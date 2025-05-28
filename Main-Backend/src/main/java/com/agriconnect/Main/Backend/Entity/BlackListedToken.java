package com.agriconnect.Main.Backend.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "black_listed_token")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlackListedToken implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String token;

    private Date expiryDate;

}