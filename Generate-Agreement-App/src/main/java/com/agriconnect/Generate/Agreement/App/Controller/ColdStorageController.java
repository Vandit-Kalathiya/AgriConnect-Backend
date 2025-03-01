package com.agriconnect.Generate.Agreement.App.Controller;

import com.agriconnect.Generate.Agreement.App.model.Cold.Booking;
import com.agriconnect.Generate.Agreement.App.service.ColdStorageService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coldstorage")
@CrossOrigin(origins = "http://localhost:5173")
public class ColdStorageController {
    @Autowired
    private ColdStorageService coldStorageService;

    @PostMapping("/book")
    public ResponseEntity<Booking> bookColdStorage(@RequestBody Booking booking) throws MessagingException {
        Booking savedBooking = coldStorageService.bookColdStorage(booking);
        return ResponseEntity.ok(savedBooking);
    }

    @PutMapping("/approve/{bookingId}")
    public ResponseEntity<Booking> approveBooking(@PathVariable Long bookingId) {
        Booking approvedBooking = coldStorageService.approveBooking(bookingId);
        return ResponseEntity.ok(approvedBooking);
    }

    // Get bookings for a farmer
    @GetMapping("/bookings")
    public List<Booking> getFarmerBookings(@RequestParam String farmerId) {
        return coldStorageService.getFarmerBookings(farmerId);
    }
}