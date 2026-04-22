package com.agriconnect.api.gateway.Entity.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "users", indexes = {
        @Index(name = "idx_user_phone", columnList = "phoneNumber", unique = true),
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_unique_hex", columnList = "uniqueHexAddress", unique = true),
        @Index(name = "idx_user_username", columnList = "username")
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String uniqueHexAddress;
    private String username;
    private String password;
    private String phoneNumber;
    private String address;

    @Column(unique = true)
    private String email;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(columnDefinition = "bytea")
    @JsonIgnore
    private byte[] profilePicture;

    @Column(columnDefinition = "bytea")
    @JsonIgnore
    private byte[] signature;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    // List<Object> wishlist = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
