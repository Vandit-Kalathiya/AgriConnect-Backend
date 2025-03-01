package com.agriconnect.Generate.Agreement.App.model;

import jakarta.persistence.Lob;
import lombok.Data;

@Data
public class BuyerInfo {
    private String buyerName;
    private String buyerAddress;
    private String buyerContact;
    @Lob
    private byte[] buyerSignature;
}
