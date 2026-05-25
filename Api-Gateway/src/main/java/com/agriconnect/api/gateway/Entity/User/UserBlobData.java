package com.agriconnect.api.gateway.Entity.User;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBlobData {

    @Id
    private String id;

    @Column(columnDefinition = "bytea")
    private byte[] profilePicture;

    @Column(columnDefinition = "bytea")
    private byte[] signature;
}
