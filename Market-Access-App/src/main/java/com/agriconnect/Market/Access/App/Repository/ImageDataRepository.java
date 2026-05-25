package com.agriconnect.Market.Access.App.Repository;

import com.agriconnect.Market.Access.App.Entity.ImageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageDataRepository extends JpaRepository<ImageData, String> {
}
