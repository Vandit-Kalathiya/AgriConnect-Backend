package com.agriconnect.api.gateway.Repository.User;

import com.agriconnect.api.gateway.Entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,String> {

    Optional<User> getUserByPhoneNumber(String phoneNumber);

    Optional<User> findUserByUniqueHexAddress(String uniqueHexAddress);

    Optional<User> findByEmail(String email);
}
