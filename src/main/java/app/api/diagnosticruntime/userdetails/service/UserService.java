package app.api.diagnosticruntime.userdetails.service;

import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.userdetails.dto.UserCountResponse;
import app.api.diagnosticruntime.userdetails.dto.UserFilter;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.dto.UserRegistrationDTO;
import app.api.diagnosticruntime.userdetails.mapper.UserMapper;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import app.api.diagnosticruntime.userdetails.repository.UserRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static app.api.diagnosticruntime.userdetails.mapper.UserMapper.toUserInfoDTO;

@Transactional
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       MongoTemplate mongoTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mongoTemplate = mongoTemplate;
    }

    public UserCountResponse getUserCounts() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countByStatus(UserStatus.INACTIVE);
        long rejectedUsers = userRepository.countByStatus(UserStatus.REJECTED);
        long pendingUsers = userRepository.countByStatus(UserStatus.NEW);

        return new UserCountResponse(totalUsers, activeUsers, inactiveUsers, rejectedUsers, pendingUsers);
    }

    public UserCountResponse getUserCountByStatusAndRole(UserRole userRole) {
        long totalUsers = userRepository.countAllByRole(userRole);
        long pendingUsers = userRepository.countAllByRoleAndStatus(userRole, UserStatus.NEW);
        long activeUsers = userRepository.countAllByRoleAndStatus(userRole, UserStatus.ACTIVE);
        long inactiveUsers = userRepository.countAllByRoleAndStatus(userRole, UserStatus.INACTIVE);
        long rejectedUsers = userRepository.countAllByRoleAndStatus(userRole, UserStatus.REJECTED);
        return new UserCountResponse(totalUsers, activeUsers, inactiveUsers, rejectedUsers, pendingUsers);
    }

    public boolean resetPassword(String email, String newPassword) {
        return userRepository.findByEmail(email).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    public Optional<UserInfoDTO> getUserById(String id) {
        return userRepository.findByIdAndStatusIs(id, UserStatus.ACTIVE)
                .map(UserMapper::toUserInfoDTO);
    }

    public Optional<UserInfoDTO> getUserByEmail(String id) {
        return userRepository.findByEmailAndStatusIs(id, UserStatus.ACTIVE)
                .map(UserMapper::toUserInfoDTO);
    }


    public Page<UserInfoDTO> getAllUsers(UserFilter userFilter, Pageable pageable, UserDetails userDetails) {
        Query query = new Query().with(pageable);

        // Build criteria based on filter parameters
        Optional.ofNullable(userFilter.getStatus())
                .ifPresent(status -> query.addCriteria(Criteria.where("status").is(status)));

        Optional.ofNullable(userFilter.getRole())
                .ifPresent(role -> query.addCriteria(Criteria.where("role").is(role)));

        // Add fullname filter (case-insensitive partial match)
        Optional.ofNullable(userFilter.getFullname())
                .ifPresent(fullname -> query.addCriteria(Criteria.where("fullname").regex(fullname, "i")));

        query.addCriteria(Criteria.where("email").ne(userDetails.getUsername()));

        // Execute query with pagination
        List<User> users = mongoTemplate.find(query, User.class);
        long count = mongoTemplate.count(query.skip(-1).limit(-1), User.class); // Reset pagination to get total count

        // Convert to DTOs
        List<UserInfoDTO> userDtos = users.stream()
                .map(UserMapper::toUserInfoDTO)
                .collect(Collectors.toList());

        return new PageImpl<>(userDtos, pageable, count);
    }

    public void updateUserRole(String id, String userRole) {
        UserRole role = UserRole.valueOf(userRole);
        userRepository.findById(id).ifPresent(user -> {
            user.setRole(role);
            user.setAuthorities(new HashSet<>(Collections.singleton(new SimpleGrantedAuthority(role.toString()))));
            userRepository.save(user);
        });
    }

    public void updateUserStatus(String id, String userStatus) {
        UserStatus status = UserStatus.valueOf(userStatus);
        userRepository.findById(id).ifPresent(user -> {
            if(user.getStatus().equals(UserStatus.REJECTED)) {
                throw new IllegalArgumentException("Rejected user cannot be activated");
            } else {
                user.setStatus(status);
            }
            userRepository.save(user);
        });
    }

    public void updateUser(UserInfoDTO userToUpdate) {

        if (Strings.isEmpty(userToUpdate.getRole())) {
            throw new IllegalStateException("User must have a role");
        }

        // Encode the password
        String encodedPassword = passwordEncoder.encode(userToUpdate.getPassword());

        UserRole role = Optional.ofNullable(userToUpdate.getRole())
                .map(roleString -> {
                    try {
                        return UserRole.valueOf(roleString);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("This role doesn't exist: " + roleString);
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException("Role cannot be null"));


        userRepository.findById(userToUpdate.getId()).ifPresent(user -> {

            if(!user.getEmail().equals(userToUpdate.getEmail())){
                if (userRepository.existsByEmail(userToUpdate.getEmail())) {
                    throw new IllegalStateException("Email is already taken");
                }
            }



            user.setFullName(userToUpdate.getFullName());
            user.setEmail(userToUpdate.getEmail());
            if(!user.getPassword().equals(userToUpdate.getPassword())) {
                user.setPassword(encodedPassword);
            }
            user.setStatus(UserStatus.valueOf(userToUpdate.getStatus()));
            user.setPhoneNumber(userToUpdate.getPhoneNumber());
            user.setRegistrationDate(userToUpdate.getRegistrationDate());
            user.setRole(role);
            user.setAuthorities(new HashSet<>(Collections.singleton(new SimpleGrantedAuthority(role.toString()))));
            userRepository.save(user);
        });
    }

    public User getUserByUsername(String username) {
        Optional<User> userRecord = userRepository.findByEmailAndStatusIs(username, UserStatus.ACTIVE);

        if (userRecord.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }
        return userRecord.get();
    }

    public String getUserIdByUsername(String username) {
        User user = getUserByUsername(username);
        return user.getId();
    }

    private int getReferredCaseCount(String userId) {
        Query query = new Query(Criteria.where("referred_to").is(userId).and("is_deleted").is(false));
        return (int) mongoTemplate.count(query, Case.class);
    }


    public UserInfoDTO registerUser(UserRegistrationDTO userToRegister) {
        if (userRepository.existsByEmail(userToRegister.getEmail())) {
            throw new IllegalStateException("Email is already taken");
        }

        if (Strings.isEmpty(userToRegister.getRole())) {
            throw new IllegalStateException("User must have a role");
        }

        // Encode the password
        String encodedPassword = passwordEncoder.encode(userToRegister.getPassword());

        // Find or create the role
        UserRole role = Optional.ofNullable(userToRegister.getRole())
                .map(roleString -> {
                    try {
                        return UserRole.valueOf(roleString);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("This role doesn't exist: " + roleString);
                    }
                }).orElseThrow(() -> new IllegalArgumentException("Role cannot be null"));

        // Create and save the user
        User newUser = new User();
        newUser.setFullName(userToRegister.getFullName());
        newUser.setEmail(userToRegister.getEmail());
        newUser.setPassword(encodedPassword);
        newUser.setPhoneNumber(userToRegister.getPhoneNumber());
        newUser.setStatus(UserStatus.NEW);
        newUser.setRegistrationDate(LocalDate.now());
        newUser.setRole(role);

        User savedUser = userRepository.save(newUser);

        // Return the DTO
        return toUserInfoDTO(savedUser);
    }


}
