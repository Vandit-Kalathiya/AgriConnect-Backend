package com.agriconnect.Market.Access.App.Service;

import com.agriconnect.Market.Access.App.Dto.ListingRequest;
import com.agriconnect.Market.Access.App.Entity.Image;
import com.agriconnect.Market.Access.App.Entity.Listing;
import com.agriconnect.Market.Access.App.Entity.ListingStatus;
import com.agriconnect.Market.Access.App.Repository.ImageRepository;
import com.agriconnect.Market.Access.App.Repository.ListingRepository;
import com.agriconnect.Market.Access.App.exception.BadRequestException;
import com.agriconnect.Market.Access.App.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ListingRepository listingRepository;
    private final ImageRepository imageRepository;

    public ListingService(ListingRepository listingRepository, ImageRepository imageRepository) {
        this.listingRepository = listingRepository;
        this.imageRepository = imageRepository;
    }

    public Listing addListing(ListingRequest listingRequest, List<MultipartFile> images) {
        log.info("Adding new listing for product: {}", listingRequest.getProductName());

        try {
            Listing listing = new Listing();

            // Set listing details from request
            listing.setProductName(listingRequest.getProductName());
            listing.setProductDescription(listingRequest.getProductDescription());
            listing.setProductType(listingRequest.getProductType());

            // Convert and validate finalPrice
            listing.setFinalPrice(listingRequest.getFinalPrice() != null ?
                    Double.parseDouble(listingRequest.getFinalPrice()) : 0L);

            // Set AI generated price (default to 0L)
            listing.setAiGeneratedPrice(listingRequest.getAiGeneratedPrice() != null ? 
                    Double.parseDouble(listingRequest.getAiGeneratedPrice()) : 0L);

            // Convert date strings to LocalDate with custom formatter
            listing.setHarvestedDate(listingRequest.getHarvestedDate() != null && !listingRequest.getHarvestedDate().isEmpty() ?
                    LocalDate.parse(listingRequest.getHarvestedDate(), DATE_FORMATTER) : null);

            listing.setStorageCondition(listingRequest.getStorageCondition());

            // Convert and validate quantity
            listing.setQuantity(listingRequest.getQuantity() != null ?
                    Long.parseLong(listingRequest.getQuantity()) : null);

            listing.setUnitOfQuantity(listingRequest.getUnitOfQuantity());
            listing.setLocation(listingRequest.getLocation());

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
            }

            listing.setCreatedDate(LocalDate.now());
            listing.setLastUpdatedDate(LocalDate.now());
            listing.setCreatedTime(LocalTime.now());

            // Update listing with images
            listing = listingRepository.save(listing);
            
            log.info("Listing added successfully with ID: {}", listing.getId());
            return listing;

        } catch (NumberFormatException e) {
            log.error("Invalid number format in listing request", e);
            throw new BadRequestException("Invalid number format in listing request", e);
        } catch (DateTimeParseException e) {
            log.error("Invalid date format in listing request", e);
            throw new BadRequestException("Invalid date format in listing request. Expected format: yyyy-MM-dd", e);
        } catch (IOException e) {
            log.error("Failed to process image files", e);
            throw new BadRequestException("Failed to process image files", e);
        }
    }

    public Listing updateListing(String listingId, ListingRequest listingRequest, List<MultipartFile> images) {
        log.info("Updating listing with ID: {}", listingId);
        Listing existingListing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        try {
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
            if (listingRequest.getAiGeneratedPrice() != null) {
                existingListing.setAiGeneratedPrice(Double.parseDouble(listingRequest.getAiGeneratedPrice()));
            }
            if (listingRequest.getHarvestedDate() != null && !listingRequest.getHarvestedDate().isEmpty()) {
                existingListing.setHarvestedDate(LocalDate.parse(listingRequest.getHarvestedDate(), DATE_FORMATTER));
            }
            if (listingRequest.getStorageCondition() != null) {
                existingListing.setStorageCondition(listingRequest.getStorageCondition());
            }
            if (listingRequest.getQuantity() != null) {
                existingListing.setQuantity(Long.parseLong(listingRequest.getQuantity()));
            }
            if (listingRequest.getUnitOfQuantity() != null) {
                existingListing.setUnitOfQuantity(listingRequest.getUnitOfQuantity());
            }
            if (listingRequest.getLocation() != null) {
                existingListing.setLocation(listingRequest.getLocation());
            }
            if (listingRequest.getShelfLifetime() != null) {
                existingListing.setShelfLifetime(Long.parseLong(listingRequest.getShelfLifetime()));
            }
            if (listingRequest.getContactOfFarmer() != null) {
                existingListing.setContactOfFarmer(listingRequest.getContactOfFarmer());
            }

            existingListing.setLastUpdatedDate(LocalDate.now());

            // Append new images to the existing listing images
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
                                .listing(existingListing)
                                .build();
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
        log.debug("Fetching listing with ID: {}", listingId);
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
    }

    public List<byte[]> getListingImages(String listingId) {
        log.debug("Fetching images for listing ID: {}", listingId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        List<byte[]> images = new ArrayList<>();
        for (Image image : listing.getImages()) {
            images.add(image.getData());
        }
        return images;
    }

    public List<Listing> getAllListings() {
        log.debug("Fetching all listings");
        return listingRepository.findAll(); 
    }

    public void deleteListing(String listingId) {
        log.info("Deleting listing with ID: {}", listingId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        // Delete associated images
        if (!listing.getImages().isEmpty()) {
            imageRepository.deleteAll(listing.getImages());
        }

        listingRepository.deleteById(listingId);
        log.info("Listing deleted successfully with ID: {}", listingId);
    }

    public Listing updateListingStatus(String listingId, String status, String quantity) {
        log.info("Updating status for listing ID: {} to status: {}", listingId, status);
        
        Listing existingListing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));
        
        long quantityValue = Long.parseLong(quantity);
        long updatedQuantity = existingListing.getQuantity() - quantityValue;
        
        existingListing.setQuantity(Math.max(0, updatedQuantity));
        
        if (updatedQuantity <= 0) {
            if (status.equalsIgnoreCase("archived")) {
                existingListing.setStatus(String.valueOf(ListingStatus.ARCHIVED));
            } else if (status.equalsIgnoreCase("purchased")) {
                existingListing.setStatus(String.valueOf(ListingStatus.PURCHASED));
            }
        }
        
        Listing updated = listingRepository.save(existingListing);
        log.info("Listing status updated successfully for ID: {}", listingId);
        return updated;
    }

    public List<Listing> getActiveListings() {
        log.debug("Fetching active listings");
        return listingRepository.findActiveListings();
    }

    public List<Listing> getListingByFarmerContact(String farmerContact) {
        log.debug("Fetching listings for farmer contact: {}", farmerContact);
        return listingRepository.findByContactOfFarmer(farmerContact)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "farmerContact", farmerContact));
    }
}