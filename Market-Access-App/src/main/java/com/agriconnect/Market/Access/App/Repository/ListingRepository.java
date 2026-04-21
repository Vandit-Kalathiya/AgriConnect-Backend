package com.agriconnect.Market.Access.App.Repository;

import com.agriconnect.Market.Access.App.Entity.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, String> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT l FROM Listing l WHERE l.status = 'ACTIVE' ORDER BY l.createdDate DESC, l.createdTime DESC")
    List<Listing> findActiveListings();

    @Query("SELECT DISTINCT l FROM Listing l LEFT JOIN FETCH l.images WHERE l.status = 'ACTIVE' ORDER BY l.createdDate DESC, l.createdTime DESC")
    List<Listing> findActiveListingsWithImages();

    @Query("SELECT DISTINCT l FROM Listing l LEFT JOIN FETCH l.images ORDER BY l.createdDate DESC, l.createdTime DESC")
    List<Listing> findAllWithImages();

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("SELECT l FROM Listing l WHERE l.contactOfFarmer = :contact ORDER BY l.createdDate DESC")
    Optional<List<Listing>> findByContactOfFarmer(@Param("contact") String contact);

    @Query("SELECT DISTINCT l FROM Listing l LEFT JOIN FETCH l.images WHERE l.contactOfFarmer = :contact ORDER BY l.createdDate DESC")
    Optional<List<Listing>> findByContactOfFarmerWithImages(@Param("contact") String contact);

    // Native query for count - more efficient than JPQL
    @Query(value = "SELECT COUNT(*) FROM listings WHERE status = 'ACTIVE'", nativeQuery = true)
    long countActiveListings();

    // Native query for farmer listing count
    @Query(value = "SELECT COUNT(*) FROM listings WHERE contact_of_farmer = :contact", nativeQuery = true)
    long countByFarmer(@Param("contact") String contact);
}
