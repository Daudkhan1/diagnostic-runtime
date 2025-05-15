package app.api.diagnosticruntime.userdetails.controller;

import app.api.diagnosticruntime.userdetails.dto.*;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import app.api.diagnosticruntime.userdetails.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/status/count")
    public ResponseEntity<UserCountResponse> getUserCounts() {
        UserCountResponse counts = userService.getUserCounts();
        return new ResponseEntity<>(counts, HttpStatus.OK);
    }

    @GetMapping("/role/{userRole}/count")
    public ResponseEntity<UserCountResponse> getUserCounts(@PathVariable UserRole userRole) {
        UserCountResponse counts = userService.getUserCountByStatusAndRole(userRole);
        return new ResponseEntity<>(counts, HttpStatus.OK);
    }

    @GetMapping("/role")
    public ResponseEntity<List<UserRole>> getAllUserRoles() {
        List<UserRole> roles = Arrays.asList(UserRole.values());
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody PasswordResetRequest request) {
        boolean success = userService.resetPassword(request.getEmail(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok("Password updated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with the provided email does not exist.");
        }
    }

    @GetMapping("/status")
    public ResponseEntity<List<UserStatus>> getAllUserStatuses() {
        List<UserStatus> statuses = Arrays.asList(UserStatus.values());
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    // User Registration.
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegistrationDTO userDto) {
        try {
            UserInfoDTO userInfoDTO = userService.registerUser(userDto);
            return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
        } catch (IllegalStateException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<Page<UserInfoDTO>> getAllUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            UserFilter userFilter, // UserFilter from query parameters or @RequestBody for JSON
            Pageable pageable) {
        Page<UserInfoDTO> users = userService.getAllUsers(userFilter, pageable, userDetails);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    // Get enabled user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserInfoDTO> getUserById(@PathVariable String id) {
        Optional<UserInfoDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable String id, @RequestBody UserStatusUpdateDTO userStatus) {
        userService.updateUserStatus(id, userStatus.getStatus());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<Void> updateUserRole(@PathVariable String id, @RequestBody UserRoleUpdateDTO userRole) {
        userService.updateUserRole(id, userRole.getRole());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // Update a user
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable String id, @RequestBody UserInfoDTO userToUpdate) {
        userService.updateUser(userToUpdate);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
