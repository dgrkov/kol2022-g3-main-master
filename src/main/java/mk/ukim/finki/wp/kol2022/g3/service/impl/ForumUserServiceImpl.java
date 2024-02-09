package mk.ukim.finki.wp.kol2022.g3.service.impl;

import mk.ukim.finki.wp.kol2022.g3.model.ForumUser;
import mk.ukim.finki.wp.kol2022.g3.model.ForumUserType;
import mk.ukim.finki.wp.kol2022.g3.model.Interest;
import mk.ukim.finki.wp.kol2022.g3.model.exceptions.InvalidForumUserIdException;
import mk.ukim.finki.wp.kol2022.g3.repository.ForumUserRepository;
import mk.ukim.finki.wp.kol2022.g3.repository.InterestRepository;
import mk.ukim.finki.wp.kol2022.g3.service.ForumUserService;
import mk.ukim.finki.wp.kol2022.g3.service.InterestService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class ForumUserServiceImpl implements ForumUserService, UserDetailsService {
    private final ForumUserRepository repository;
    private final PasswordEncoder encoder;
    private final InterestRepository interestRepository;
    private final InterestService interestService;

    public ForumUserServiceImpl(ForumUserRepository repository, PasswordEncoder encoder, InterestRepository interestRepository, InterestService interestService) {
        this.repository = repository;
        this.encoder = encoder;
        this.interestRepository = interestRepository;
        this.interestService = interestService;
    }

    @Override
    public List<ForumUser> listAll() {
        return repository.findAll();
    }

    @Override
    public ForumUser findById(Long id) {
        return repository.findById(id).orElseThrow(InvalidForumUserIdException::new);
    }

    @Override
    public ForumUser create(String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        List<Interest> interests = interestRepository.findAllById(interestId);
        ForumUser newFormUser = new ForumUser(name, email, encoder.encode(password), type, interests, birthday);
        return repository.save(newFormUser);
    }

    @Override
    public ForumUser update(Long id, String name, String email, String password, ForumUserType type, List<Long> interestId, LocalDate birthday) {
        List<Interest> interests = interestRepository.findAllById(interestId);
        ForumUser forumUser = findById(id);
        forumUser.setName(name);
        forumUser.setEmail(email);
        forumUser.setPassword(encoder.encode(password));
        forumUser.setType(type);
        forumUser.setInterests(interests);
        forumUser.setBirthday(birthday);
        return repository.save(forumUser);
    }

    @Override
    public ForumUser delete(Long id) {
        ForumUser delForumUser = findById(id);
        repository.delete(delForumUser);
        return delForumUser;
    }

    @Override
    public List<ForumUser> filter(Long interestId, Integer age) {
        if (interestId == null && age == null){
            return listAll();
        } else if (interestId == null) {
            return repository.findAllByBirthdayBefore(LocalDate.now().minusYears(age));
        } else if (age == null) {
            return repository.findAllByInterestsContaining(interestService.findById(interestId));
        }
        return repository.findAllByInterestsContainingAndBirthdayBefore(interestService.findById(interestId), LocalDate.now().minusYears(age));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ForumUser forumUser = repository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(forumUser.getEmail())
                .password(forumUser.getPassword())
                .authorities("ROLE_" + forumUser.getType().toString())
                .build();
    }
}
