package com.agriconnect.Main.Backend.Repository.User;

import com.agriconnect.Main.Backend.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> getUserByPhoneNumber(String phoneNumber);
}
