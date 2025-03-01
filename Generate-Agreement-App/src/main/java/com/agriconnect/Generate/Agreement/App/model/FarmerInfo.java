package com.agriconnect.Generate.Agreement.App.model;

import jakarta.persistence.Lob;
import lombok.Data;

@Data
public class FarmerInfo {
    private String farmerName;
    private String farmerAddress;
    private String farmerContact;
    @Lob
    private byte[] farmerSignature;
}
