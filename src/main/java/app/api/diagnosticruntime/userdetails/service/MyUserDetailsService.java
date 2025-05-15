package app.api.diagnosticruntime.userdetails.service;


import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import app.api.diagnosticruntime.userdetails.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Service
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public MyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> userRecord = userRepository.findByEmailAndStatusIs(email, UserStatus.ACTIVE);

        if (userRecord.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        User loadedUser = userRecord.get();

        return new User(
                loadedUser.getId(),
                loadedUser.getFullName(),
                loadedUser.getEmail(),
                loadedUser.getPassword(),
                loadedUser.getPhoneNumber(),
                loadedUser.getStatus(),
                loadedUser.getRole(),
                loadedUser.getRegistrationDate()
        );
    }
}
