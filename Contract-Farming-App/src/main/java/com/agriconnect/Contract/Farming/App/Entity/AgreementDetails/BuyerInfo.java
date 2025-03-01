package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;


import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class BuyerInfo {
    private String buyerName;
    private String buyerAddress;
    private String buyerContact;
    @Lob
    private byte[] buyerSignature;
}