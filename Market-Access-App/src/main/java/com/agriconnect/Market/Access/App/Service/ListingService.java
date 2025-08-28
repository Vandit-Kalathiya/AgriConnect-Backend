package com.agriconnect.Market.Access.App.Service;

import com.agriconnect.Market.Access.App.Dto.ListingRequest;
import com.agriconnect.Market.Access.App.Entity.Image;
import com.agriconnect.Market.Access.App.Entity.Listing;
import com.agriconnect.Market.Access.App.Entity.ListingStatus;
import com.agriconnect.Market.Access.App.Repository.ImageRepository;
import com.agriconnect.Market.Access.App.Repository.ListingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListingService {

    private static final Logger logger = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final ImageRepository imageRepository; // Added ImageRepository

    public ListingService(ListingRepository listingRepository, ImageRepository imageRepository) {
        this.listingRepository = listingRepository;
        this.imageRepository = imageRepository;
    }

    public Listing addListing(ListingRequest listingRequest, List<MultipartFile> images) {
        Listing listing = new Listing();

        // Define a DateTimeFormatter for the expected date format
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Adjust this pattern as needed

        try {
            // Set listing details from request
            listing.setProductName(listingRequest.getProductName());
            listing.setProductDescription(listingRequest.getProductDescription());
            listing.setProductType(listingRequest.getProductType());

            // Convert and validate finalPrice
            listing.setFinalPrice(listingRequest.getFinalPrice() != null ?
                    Double.parseDouble(listingRequest.getFinalPrice()) : 0L);

            // Set AI generated price (default to 0L)
            listing.setAiGeneratedPrice(listingRequest.getAiGeneratedPrice() != null ? Double.parseDouble(listingRequest.getAiGeneratedPrice()) : 0L);

            // Convert date strings to LocalDate with custom formatter
            listing.setHarvestedDate(listingRequest.getHarvestedDate() != null && !listingRequest.getHarvestedDate().isEmpty() ?
                    LocalDate.parse(listingRequest.getHarvestedDate(), dateFormatter) : null);
//            listing.setAvailabilityDate(listingRequest.getAvailabilityDate() != null && !listingRequest.getAvailabilityDate().isEmpty() ?
//                    LocalDate.parse(listingRequest.getAvailabilityDate(), dateFormatter) : null);

//            listing.setQualityGrade(listingRequest.getQualityGrade());
            listing.setStorageCondition(listingRequest.getStorageCondition());

            // Convert and validate quantity
            listing.setQuantity(listingRequest.getQuantity() != null ?
                    Long.parseLong(listingRequest.getQuantity()) : null);

            listing.setUnitOfQuantity(listingRequest.getUnitOfQuantity());
            listing.setLocation(listingRequest.getLocation());
//            listing.setCertifications(listingRequest.getCertifications());

            // Convert and validate shelfLifetime
            listing.setShelfLifetime(listingRequest.getShelfLifetime() != null ?
                    Long.parseLong(listingRequest.getShelfLifetime()) : null);

            listing.setContactOfFarmer(listingRequest.getContactOfFarmer());

            // Initialize images list
            listing.setImages(new ArrayList<>());

            // Save listing first to get ID
            listing = listingRepository.save(listing);

            // Handle image uploads
            if (images != null && !images.isEmpty()) {
                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        Image image = Image.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .size(file.getSize())
                                .data(file.getBytes())
                                .createDate(LocalDate.now())
                                .createTime(LocalTime.now())
                                .listing(listing)
                                .build();

                        imageRepository.save(image);
                        listing.getImages().add(image);
                    }
                }

                listing.setCreatedDate(LocalDate.now());
                listing.setLastUpdatedDate(LocalDate.now());
                listing.setCreatedTime(LocalTime.now());

                // Update listing with images
                listing = listingRepository.save(listing);
            }

            return listing;

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in listing request", e);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format in listing request. Expected format: yyyy-MM-dd", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image files", e);
        }
    }

    public Listing updateListing(String listingId, ListingRequest listingRequest, List<MultipartFile> images) {
        Listing existingListing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + listingId));

        try {
            // Update fields from request
            if (listingRequest.getProductName() != null) {
                existingListing.setProductName(listingRequest.getProductName());
            }
            if (listingRequest.getProductDescription() != null) {
                existingListing.setProductDescription(listingRequest.getProductDescription());
            }
            if (listingRequest.getProductType() != null) {
                existingListing.setProductType(listingRequest.getProductType());
            }
            if (listingRequest.getFinalPrice() != null) {
                existingListing.setFinalPrice(Double.parseDouble(listingRequest.getFinalPrice()));
            }
            if (listingRequest.getHarvestedDate() != null) {
                existingListing.setHarvestedDate(LocalDate.parse(listingRequest.getHarvestedDate()));
            }
//            if (listingRequest.getAvailabilityDate() != null) {
//                existingListing.setAvailabilityDate(LocalDate.parse(listingRequest.getAvailabilityDate()));
//            }
//            if (listingRequest.getQualityGrade() != null) {
//                existingListing.setQualityGrade(listingRequest.getQualityGrade());
//            }
            if (listingRequest.getStorageCondition() != null) {
                existingListing.setStorageCondition(listingRequest.getStorageCondition());
            }
            if (listingRequest.getQuantity() != null) {
                long existingQty = Long.parseLong(existingListing.getQuantity().toString());
                long requestedQty = Long.parseLong(listingRequest.getQuantity());
                existingListing.setQuantity(Long.parseLong(String.valueOf(Math.max(0, existingQty - requestedQty))));
            }
            if (listingRequest.getUnitOfQuantity() != null) {
                existingListing.setUnitOfQuantity(listingRequest.getUnitOfQuantity());
            }
            if (listingRequest.getLocation() != null) {
                existingListing.setLocation(listingRequest.getLocation());
            }
//            if (listingRequest.getCertifications() != null) {
//                existingListing.setCertifications(listingRequest.getCertifications());
//            }
            if (listingRequest.getShelfLifetime() != null) {
                existingListing.setShelfLifetime(Long.parseLong(listingRequest.getShelfLifetime()));
            }
            if (listingRequest.getContactOfFarmer() != null) {
                existingListing.setContactOfFarmer(listingRequest.getContactOfFarmer());
            }

            // Handle image updates
            if (images != null && !images.isEmpty()) {
                // Optional: Remove existing images if you want to replace them
                // existingListing.getImages().clear();
                // imageRepository.deleteAll(existingListing.getImages());

                for (MultipartFile file : images) {
                    if (!file.isEmpty()) {
                        Image image = Image.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .size(file.getSize())
                                .data(file.getBytes())
                                .createDate(LocalDate.now())
                                .createTime(LocalTime.now())
                                .listing(existingListing)
                                .build();

//                        image.setDownloadUrl("/images/" + image.getId());
                        imageRepository.save(image);
                        existingListing.getImages().add(image);
                    }
                }
            }

            return listingRepository.save(existingListing);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in listing request", e);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format in listing request", e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process image files", e);
        }
    }

    public Listing getListingById(String listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + listingId));
    }

    public List<byte[]> getListingImages(String listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + listingId));

        List<byte[]> images = new ArrayList<>();
        for (Image image : listing.getImages()) {
            images.add(image.getData());
        }
        return images;
    }

    public List<Listing> getAllListings() {
        List<Listing> listings = listingRepository.findAll();
        if (listings.isEmpty()) {
            return new ArrayList<>(); // Return empty list instead of null
        }
        return listings;
    }

    public void deleteListing(String listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + listingId));

        // Delete associated images
        if (!listing.getImages().isEmpty()) {
            imageRepository.deleteAll(listing.getImages());
        }

        listingRepository.deleteById(listingId);
    }

    public Listing updateListingStatus(String listingId, String status, String quantity) {
        Listing existingListing = listingRepository.findById(listingId)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found with id: " + listingId));
        long updatedQuantity = existingListing.getQuantity() - Long.parseLong(quantity);
        existingListing.setQuantity(updatedQuantity < 0 ? 0 : existingListing.getQuantity() - Long.parseLong(quantity));
        if (updatedQuantity == 0) {
            if (status.equalsIgnoreCase("archived")) {
                existingListing.setStatus(String.valueOf(ListingStatus.ARCHIVED));
            } else if (status.equalsIgnoreCase("purchased")) {
                existingListing.setStatus(String.valueOf(ListingStatus.PURCHASED));
            }
        }
        return listingRepository.save(existingListing);
    }

    public List<Listing> getActiveListings() {
        List<Listing> listings = listingRepository.findActiveListings();
        if (listings.isEmpty()) {
            return new ArrayList<>(); // Return empty list instead of null
        }
        return listings;
    }

    public List<Listing> getListingByFarmerContact(String farmerContact) {
        return listingRepository.findByContactOfFarmer(farmerContact)
                .orElseThrow(() -> new EntityNotFoundException("Listing not found for farmer contact: " + farmerContact));
    }
}