package com.agriconnect.Generate.Agreement.App.model.Cold;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Booking", indexes = {
        @Index(name = "idx_booking_farmer_id", columnList = "farmerId"),
        @Index(name = "idx_booking_status", columnList = "status"),
        @Index(name = "idx_booking_farmer_status", columnList = "farmerId,status")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String farmerId; // Assuming farmer authentication
    private double cropQuantity; // In tons
    private int storageDuration; // In days
    private String cropType;
    private String cropName;
    private String coldStorageName; // Store only the name of the cold storage
    private String status = "Pending"; // Default status
}
