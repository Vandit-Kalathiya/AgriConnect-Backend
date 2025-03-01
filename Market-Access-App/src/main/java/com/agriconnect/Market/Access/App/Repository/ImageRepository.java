package com.agriconnect.Market.Access.App.Repository;

import com.agriconnect.Market.Access.App.Entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {
}
