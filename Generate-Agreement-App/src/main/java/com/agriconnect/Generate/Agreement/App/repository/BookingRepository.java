package com.agriconnect.Generate.Agreement.App.repository;

import com.agriconnect.Generate.Agreement.App.model.Cold.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByFarmerId(String farmerId);
}