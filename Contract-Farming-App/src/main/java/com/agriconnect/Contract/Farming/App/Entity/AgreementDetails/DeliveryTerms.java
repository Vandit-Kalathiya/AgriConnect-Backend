package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class DeliveryTerms {
    private String date;
    private String location;
    private String transportation;
    private String packaging;
}