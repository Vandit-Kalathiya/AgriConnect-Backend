package com.agriconnect.Market.Access.App.Service;

import com.agriconnect.Market.Access.App.Entity.Image;
import com.agriconnect.Market.Access.App.Entity.ImageData;
import com.agriconnect.Market.Access.App.Entity.Listing;
import com.agriconnect.Market.Access.App.Repository.ImageRepository;
import com.agriconnect.Market.Access.App.Repository.ImageDataRepository;
import com.agriconnect.Market.Access.App.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageDataRepository imageDataRepository;

    public ImageService(ImageRepository imageRepository, ImageDataRepository imageDataRepository) {
        this.imageRepository = imageRepository;
        this.imageDataRepository = imageDataRepository;
    }

    public byte[] getImageById(String id) {
        return imageDataRepository.findById(id)
                .map(ImageData::getData)
                .orElse(null);
    }

    public Image getImageMetadata(String id) {
        return imageRepository.findById(id).orElse(null);
    }

    @Transactional
    public void deleteImage(String imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image", "id", imageId));

        /*
         * Because Listing.images has orphanRemoval = true + cascade = ALL, deleting
         * the Image row directly while the parent still references it leads to
         * undefined JPA behaviour (Hibernate may re-insert on the next flush).
         * The correct approach is to remove the child from the parent's managed
         * collection; orphanRemoval will issue the DELETE when the transaction commits.
         */
        Listing listing = image.getListing();
        if (listing != null) {
            listing.getImages().removeIf(img -> img.getId().equals(imageId));
        } else {
            imageRepository.delete(image);
        }
    }
}
