package com.agriconnect.api.gateway.Repository.User;

import com.agriconnect.api.gateway.Entity.User.UserBlobData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBlobDataRepository extends JpaRepository<UserBlobData, String> {
}
