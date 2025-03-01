package com.agriconnect.Market.Access.App.Service;

import com.agriconnect.Market.Access.App.Repository.ImageRepository;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public byte[] getImageById(String id) {
        return imageRepository.findById(id).get().getData();
    }
}
