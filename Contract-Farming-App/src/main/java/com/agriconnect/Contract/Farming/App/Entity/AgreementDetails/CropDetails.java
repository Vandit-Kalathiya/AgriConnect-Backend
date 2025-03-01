package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;


import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CropDetails {
    private String type;
    private String variety;
    private String quantity;
    private String pricePerUnit;

    @ElementCollection
    @Column(name = "quality_standard")
    private List<String> qualityStandards = new ArrayList<>();
}