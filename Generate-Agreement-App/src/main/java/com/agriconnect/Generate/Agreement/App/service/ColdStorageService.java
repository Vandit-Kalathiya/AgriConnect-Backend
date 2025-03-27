package com.agriconnect.Generate.Agreement.App.service;

import com.agriconnect.Generate.Agreement.App.model.Cold.Booking;
import com.agriconnect.Generate.Agreement.App.model.ColdStorage;
import com.agriconnect.Generate.Agreement.App.repository.BookingRepository;
import com.agriconnect.Generate.Agreement.App.repository.ColdStorageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ColdStorageService {

    private final BookingRepository bookingRepository;
    private final EmailService emailService;
    private final ColdStorageRepository coldStorageRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.maps.api.key}")
    private String googleMapsApiKey;

    private static final Map<String, String> COLD_STORAGE_OWNERS = new HashMap<>();

    static {
        COLD_STORAGE_OWNERS.put("Storage A", "suhasikakani545@gmail.com");
        COLD_STORAGE_OWNERS.put("Storage B", "suhasikakani545@gmail.com");
        COLD_STORAGE_OWNERS.put("Cold Store XYZ", "suhasikakani545@gmail.com");
    }

    @Autowired
    public ColdStorageService(BookingRepository bookingRepository, EmailService emailService,
                              ColdStorageRepository coldStorageRepository, RestTemplate restTemplate,
                              ObjectMapper objectMapper) {
        this.bookingRepository = bookingRepository;
        this.emailService = emailService;
        this.coldStorageRepository = coldStorageRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Booking bookColdStorage(Booking booking) throws MessagingException {
        booking.setStatus("Pending");
        Booking savedBooking = bookingRepository.save(booking);

        String ownerEmail = COLD_STORAGE_OWNERS.getOrDefault(booking.getColdStorageName(), "suhasikakani545@gmail.com");
        String farmerName = "Farmer " + booking.getFarmerId();
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

    public Booking approveBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if ("Pending".equals(booking.getStatus())) {
            booking.setStatus("Approved");
            return bookingRepository.save(booking);
        } else {
            throw new RuntimeException("Booking is already approved or invalid.");
        }
    }

    public List<Booking> getFarmerBookings(String farmerId) {
        return bookingRepository.findByFarmerId(farmerId);
    }

    public List<ColdStorage> fetchNearbyColdStorages(double lat, double lon) throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
                .queryParam("location", lat + "," + lon)
                .queryParam("radius", 100000) // 50 km
                .queryParam("keyword", "cold storage") // Broaden search
                .queryParam("key", googleMapsApiKey)
                .toUriString();

        System.out.println("Fetching from URL: " + url);
        String response = restTemplate.getForObject(url, String.class);
        if (response == null) {
            throw new IOException("Failed to fetch data from Google Places API");
        }

        JsonNode root = objectMapper.readTree(response);
        String status = root.get("status").asText();
        System.out.println("API Response Status: " + status);

        if ("ZERO_RESULTS".equals(status)) {
            System.out.println("No cold storages found near " + lat + "," + lon);
            return new ArrayList<>(); // Return empty list instead of throwing exception
        } else if (!"OK".equals(status)) {
            throw new IOException("API Error: " + status + " - " + root.get("error_message").asText("Unknown error"));
        }

        JsonNode results = root.get("results");
        if (results == null || !results.isArray()) {
            throw new IOException("Invalid response format: No results array");
        }

        List<ColdStorage> storages = new ArrayList<>();
        for (JsonNode place : results) {
            ColdStorage storage = parseColdStorageFromJson(place, lat, lon);
            // Check if already exists in DB to avoid duplicates
            ColdStorage existing = coldStorageRepository.findByPlaceId(storage.getPlaceId()).orElse(null);
            if (existing == null) {
                coldStorageRepository.save(storage);
                storages.add(storage);
            } else {
                storages.add(existing); // Use existing record
            }
        }
        return storages;
    }

    public List<ColdStorage> fetchNearbyColdStoragesByDistAndState(String district, String state, double lat, double lon) throws IOException {
        // Construct the Google Places API URL
//        String url = UriComponentsBuilder.fromHttpUrl("https://maps.googleapis.com/maps/api/place/textsearch/json")
//                .queryParam("query", "cold storage in " + district + ", " + state)
//                .queryParam("location", lat + "," + lon)  // Biasing search results towards the provided location
//                .queryParam("radius", 50000)  // 50 km radius
//                .queryParam("key", googleMapsApiKey)
//                .toUriString();

        String encodedQuery = URLEncoder.encode("cold storage in " + district + ", " + state, StandardCharsets.UTF_8);
        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json?query=" + encodedQuery +"&type=storage&keyword=cold+storage,refrigeration+warehouse,frozen+storage"+ "&key=" + googleMapsApiKey;


        System.out.println("Fetching from URL: " + url);
        System.out.println(district+" "+ state);

        // Make API request
        String response = restTemplate.getForObject(url, String.class);
        if (response == null) {
            throw new IOException("Failed to fetch data from Google Places API");
        }

        // Parse JSON response
        JsonNode root = objectMapper.readTree(response);
        String status = root.get("status").asText();
        System.out.println("API Response Status: " + status);
        System.out.println(response);

        // Handle API errors
        if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
            throw new IOException("API Error: " + status + " - " + root.get("error_message").asText("Unknown error"));
        }

        JsonNode results = root.get("results");
        if (results == null || !results.isArray() || results.isEmpty()) {
            System.out.println("No cold storages found for " + district + ", " + state + ". Returning default response.");
//            return fetchDefaultColdStorages(district, state); // Fallback mechanism
        }

        List<ColdStorage> storages = new ArrayList<>();
        for (JsonNode place : results) {
            String name = place.get("name").asText().toLowerCase();
            ColdStorage storage = null;
            if (!name.contains("ice cream") && !name.contains("cold drink") && !name.contains("dairy")) {
                storage = parseColdStorageFromJson(place, lat, lon);
//                storages.add(storage);
            }

            // Check if already exists in DB to avoid duplicates
            if (storage != null) {
                ColdStorage existing = coldStorageRepository.findByPlaceId(storage.getPlaceId()).orElse(null);
                if (existing == null) {
                    coldStorageRepository.save(storage);
                    storages.add(storage);
                } else {
                    storages.add(existing);
                }
            }
        }

        return storages;
    }

//    private List<ColdStorage> fetchDefaultColdStorages(String district, String state) {
//        // Fetch from database or return predefined locations
//        List<ColdStorage> defaultStorages = coldStorageRepository.findByDistrictAndState(district, state);
//
//        if (defaultStorages.isEmpty()) {
//            System.out.println("No default cold storages found in DB. Returning empty list.");
//            return new ArrayList<>();
//        }
//
//        return defaultStorages;
//    }


    private ColdStorage parseColdStorageFromJson(JsonNode place, double userLat, double userLon) {
        ColdStorage storage = new ColdStorage();

        storage.setPlaceId(place.has("place_id") ? place.get("place_id").asText() : null);
        storage.setName(place.has("name") ? place.get("name").asText() : null);
        storage.setBusinessStatus(place.has("business_status") ? place.get("business_status").asText() : null);

        JsonNode geometry = place.get("geometry");
        if (geometry != null) {
            JsonNode location = geometry.get("location");
            if (location != null) {
                storage.setLat(location.has("lat") ? location.get("lat").asDouble() : 0.0);
                storage.setLon(location.has("lng") ? location.get("lng").asDouble() : 0.0);
            }

            JsonNode viewport = geometry.get("viewport");
            if (viewport != null) {
                JsonNode northeast = viewport.get("northeast");
                if (northeast != null) {
                    storage.setViewportNortheastLat(northeast.has("lat") ? northeast.get("lat").asDouble() : null);
                    storage.setViewportNortheastLng(northeast.has("lng") ? northeast.get("lng").asDouble() : null);
                }
                JsonNode southwest = viewport.get("southwest");
                if (southwest != null) {
                    storage.setViewportSouthwestLat(southwest.has("lat") ? southwest.get("lat").asDouble() : null);
                    storage.setViewportSouthwestLng(southwest.has("lng") ? southwest.get("lng").asDouble() : null);
                }
            }
        }

        storage.setIcon(place.has("icon") ? place.get("icon").asText() : null);
        storage.setIconBackgroundColor(place.has("icon_background_color") ? place.get("icon_background_color").asText() : null);
        storage.setIconMaskBaseUri(place.has("icon_mask_base_uri") ? place.get("icon_mask_base_uri").asText() : null);

        JsonNode openingHours = place.get("opening_hours");
        if (openingHours != null && openingHours.has("open_now")) {
            storage.setOpenNow(openingHours.get("open_now").asBoolean());
        }

        if (place.has("photos")) {
            storage.setPhotos(place.get("photos").toString());
        }

        storage.setRating(place.has("rating") ? place.get("rating").asDouble() : null);
        storage.setUserRatingsTotal(place.has("user_ratings_total") ? place.get("user_ratings_total").asInt() : null);

        JsonNode plusCode = place.get("plus_code");
        if (plusCode != null) {
            storage.setCompoundCode(plusCode.has("compound_code") ? plusCode.get("compound_code").asText() : null);
            storage.setGlobalCode(plusCode.has("global_code") ? plusCode.get("global_code").asText() : null);
        }

        storage.setVicinity(place.has("vicinity") ? place.get("vicinity").asText() : null);

        if (place.has("types")) {
            List<String> typeList = new ArrayList<>();
            place.get("types").forEach(type -> typeList.add(type.asText()));
            storage.setTypes(String.join(",", typeList));
        }

        storage.setPhoneNumber(place.has("formatted_phone_number") ? place.get("formatted_phone_number").asText() : "N/A");
        storage.setTemperature("Unknown");
        storage.setSpecialty("General");
        storage.setCapacity("Unknown");

        storage.setDistance(calculateDistance(userLat, userLon, storage.getLat(), storage.getLon()));

        return storage;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public ColdStorage getColdStorageDetails(String placeId) {
        return coldStorageRepository.findByPlaceId(placeId)
                .orElseThrow(() -> new RuntimeException("Cold storage not found"));
    }

    public ColdStorage updateColdStorageDetails(String placeId, ColdStorage updatedStorage) {
        ColdStorage existing = getColdStorageDetails(placeId);
        existing.setPhoneNumber(updatedStorage.getPhoneNumber());
        existing.setTemperature(updatedStorage.getTemperature());
        existing.setSpecialty(updatedStorage.getSpecialty());
        existing.setCapacity(updatedStorage.getCapacity());
        return coldStorageRepository.save(existing);
    }

    public ColdStorage bookColdStorage(String placeId) {
        ColdStorage storage = getColdStorageDetails(placeId);
        return coldStorageRepository.save(storage);
    }
}