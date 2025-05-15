package app.api.diagnosticruntime.userdetails.model;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Setter
@Getter
@Document(collection = "users")
public class User implements UserDetails {
    @Id
    private String id;

    @Field("fullname")
    private String fullName;

    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("status")
    private UserStatus status;

    @Field("phone_number")
    private String phoneNumber;

    @Field("registration_date")
    private LocalDate registrationDate;

    @Field("role")
    private UserRole role;

    private Set<GrantedAuthority> authorities;

    public User(String id, String fullName, String email, String password, String phoneNumber, UserStatus status, UserRole role, LocalDate registrationDate) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.role = role;
        this.registrationDate = registrationDate;
        this.authorities = new HashSet<>(Collections.singleton(new SimpleGrantedAuthority(role.toString())));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
