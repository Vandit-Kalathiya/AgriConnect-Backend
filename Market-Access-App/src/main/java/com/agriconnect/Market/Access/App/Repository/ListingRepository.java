package com.agriconnect.Market.Access.App.Repository;

import com.agriconnect.Market.Access.App.Entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, String> {
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE'")
    List<Listing> findActiveListings();

    Optional<List<Listing>> findByContactOfFarmer(String contact);
}
