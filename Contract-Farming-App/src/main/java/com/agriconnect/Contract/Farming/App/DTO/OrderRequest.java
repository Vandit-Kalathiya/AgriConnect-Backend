package com.agriconnect.Contract.Farming.App.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private String farmerAddress;
    private String buyerAddress;
    private String listingId;
    private Long amount;
    private String quantity;
    private String agreementId;
}
