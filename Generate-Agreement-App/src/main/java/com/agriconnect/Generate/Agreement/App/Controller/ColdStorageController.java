package com.agriconnect.Generate.Agreement.App.Controller;

import com.agriconnect.Generate.Agreement.App.model.Cold.Booking;
import com.agriconnect.Generate.Agreement.App.model.ColdStorage;
import com.agriconnect.Generate.Agreement.App.service.ColdStorageService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/coldStorage")
@CrossOrigin(origins = "http://localhost:5173")
public class ColdStorageController {
    @Autowired
    private ColdStorageService coldStorageService;

    @PostMapping("/book")
    public ResponseEntity<?> bookColdStorage(@RequestBody Booking booking) {
        try {
            Booking savedBooking = coldStorageService.bookColdStorage(booking);
            return ResponseEntity.ok(savedBooking);
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send email confirmation: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Booking failed: " + e.getMessage());
        }
    }

    @PutMapping("/approve/{bookingId}")
    public ResponseEntity<?> approveBooking(@PathVariable String bookingId) {
        try {
            Booking approvedBooking = coldStorageService.approveBooking(bookingId);
            return ResponseEntity.ok(approvedBooking);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to approve booking: " + e.getMessage());
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getFarmerBookings(@RequestParam String farmerId) {
        try {
            List<Booking> bookings = coldStorageService.getFarmerBookings(farmerId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch farmer bookings: " + e.getMessage());
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyColdStorages(
            @RequestParam double lat,
            @RequestParam double lon) {
        try {
            List<ColdStorage> storages = coldStorageService.fetchNearbyColdStorages(lat, lon);
            return ResponseEntity.ok(storages);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch nearby storages due to IO error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to fetch nearby storages: " + e.getMessage());
        }
    }

    @GetMapping("/nearby/d/s")
    public ResponseEntity<?> getNearbyColdStoragesByDistAndState(
            @RequestParam String district,
            @RequestParam String state,
            @RequestParam double lat,
            @RequestParam double lon) {
        try {
            System.out.println(district);
            List<ColdStorage> storages = coldStorageService.fetchNearbyColdStoragesByDistAndState(district, state, lat, lon);
            return ResponseEntity.ok(storages);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch nearby storages due to IO error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to fetch nearby storages: " + e.getMessage());
        }
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<?> getColdStorageDetails(@PathVariable String placeId) {
        try {
            ColdStorage storage = coldStorageService.getColdStorageDetails(placeId);
            return ResponseEntity.ok(storage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch storage details: " + e.getMessage());
        }
    }

    @PutMapping("/{placeId}")
    public ResponseEntity<?> updateColdStorageDetails(
            @PathVariable String placeId,
            @RequestBody ColdStorage storage) {
        try {
            ColdStorage updated = coldStorageService.updateColdStorageDetails(placeId, storage);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update storage details: " + e.getMessage());
        }
    }

    @PostMapping("/{placeId}/book")
    public ResponseEntity<?> bookColdStorage(@PathVariable String placeId) {
        try {
            ColdStorage booked = coldStorageService.bookColdStorage(placeId);
            return ResponseEntity.ok(booked);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to book storage: " + e.getMessage());
        }
    }
}