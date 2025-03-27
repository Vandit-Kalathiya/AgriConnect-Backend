package com.agriconnect.Generate.Agreement.App.repository;

import com.agriconnect.Generate.Agreement.App.model.ColdStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColdStorageRepository extends JpaRepository<ColdStorage, String> {

    Optional<ColdStorage> findByPlaceId(String placeId);
}
