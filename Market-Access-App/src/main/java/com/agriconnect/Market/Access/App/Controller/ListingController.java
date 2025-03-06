package com.agriconnect.Market.Access.App.Controller;

import com.agriconnect.Market.Access.App.Dto.ListingRequest;
import com.agriconnect.Market.Access.App.Entity.Listing;
import com.agriconnect.Market.Access.App.Service.ListingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/listings")
public class ListingController {
    private final ListingService listingService;

    @Autowired
    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createListing(
            @RequestPart("images") List<MultipartFile> images,
            @ModelAttribute ListingRequest listingRequest) {
        try {
            Listing listing = listingService.addListing(listingRequest, images);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateListing(
            @PathVariable String id,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @ModelAttribute ListingRequest listingRequest) {
        try {
            Listing listing = listingService.updateListing(id, listingRequest, images);
            return ResponseEntity.ok(listing);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getListingById(@PathVariable String id) {
        try {
            Listing listing = listingService.getListingById(id);
            return ResponseEntity.ok(listing);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{listingId}/image")
    public ResponseEntity<List<byte[]>> getListingImages(@PathVariable String listingId) {
        return ResponseEntity.ok().body(listingService.getListingImages(listingId));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllListings() {
        try {
            List<Listing> listings = listingService.getAllListings();
            return ResponseEntity.ok(listings);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteListing(@PathVariable String id) {
        try {
            listingService.deleteListing(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}/{status}")
    public ResponseEntity<?> updateListingStatus(@PathVariable String id, @PathVariable String status) {
        try {
            Listing updatedListing = listingService.updateListingStatus(id, status);
            return ResponseEntity.ok(updatedListing);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/all/active")
    public ResponseEntity<?> getAllActiveListings() {
        try {
            List<Listing> activeListings = listingService.getActiveListings();
            return ResponseEntity.ok(activeListings);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/user/{userContact}")
    public ResponseEntity<?> getListingByUserContact(@PathVariable String userContact) {
        try {
            List<Listing> listing = listingService.getListingByFarmerContact(userContact);
            return ResponseEntity.ok(listing);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Unexpected error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}