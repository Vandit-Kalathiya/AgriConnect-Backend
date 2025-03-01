package com.agriconnect.Generate.Agreement.App.model;

import lombok.Data;

@Data
public class PaymentTerms {
    private String totalValue;
    private String method;
    private String advancePayment;
    private String balanceDue;
}
