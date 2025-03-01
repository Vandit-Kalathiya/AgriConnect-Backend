package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PaymentTerms {
    private String totalValue;
    private String method;
    private String advancePayment;
    private String balanceDue;
}