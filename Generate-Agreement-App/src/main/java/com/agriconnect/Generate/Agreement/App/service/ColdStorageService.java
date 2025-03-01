package com.agriconnect.Generate.Agreement.App.service;

import com.agriconnect.Generate.Agreement.App.model.Cold.Booking;
import com.agriconnect.Generate.Agreement.App.repository.BookingRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColdStorageService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private EmailService emailService;

    // Static map of cold storage names to owner emails (temporary solution)
    private static final Map<String, String> COLD_STORAGE_OWNERS = new HashMap<>();
    static {
        COLD_STORAGE_OWNERS.put("Storage A", "suhasikakani545@gmail.com");
        COLD_STORAGE_OWNERS.put("Storage B", "suhasikakani545@gmail.com");
        COLD_STORAGE_OWNERS.put("Cold Store XYZ", "suhasikakani545@gmail.com");
    }

    // Save a new booking with "Pending" status
    public Booking bookColdStorage(Booking booking) throws MessagingException {
        booking.setStatus("Pending");
        Booking savedBooking = bookingRepository.save(booking);

        // Send email to cold storage owner
        String ownerEmail = COLD_STORAGE_OWNERS.getOrDefault(booking.getColdStorageName(), "suhasikakani545@gmail.com");
        String farmerName = "Farmer " + booking.getFarmerId(); // Placeholder; replace with actual farmer name
        emailService.sendStorageRequestNotification(
                ownerEmail,
                farmerName,
                booking.getCropName(),
                booking.getCropQuantity(),
                booking.getStorageDuration(),
                booking.getColdStorageName()
        );

        return savedBooking;
    }

    // Approve a booking
    public Booking approveBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if ("Pending".equals(booking.getStatus())) {
            booking.setStatus("Approved");
            return bookingRepository.save(booking);
        } else {
            throw new RuntimeException("Booking is already approved or invalid.");
        }
    }

    // Get all bookings for a farmer
    public List<Booking> getFarmerBookings(String farmerId) {
        return bookingRepository.findByFarmerId(farmerId);
    }
}