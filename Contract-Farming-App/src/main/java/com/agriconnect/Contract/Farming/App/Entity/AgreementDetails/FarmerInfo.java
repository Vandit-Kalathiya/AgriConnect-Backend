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
public class FarmerInfo {
    private String farmerName;
    private String farmerAddress;
    private String farmerContact;
    @Lob
    private byte[] farmerSignature;
}
