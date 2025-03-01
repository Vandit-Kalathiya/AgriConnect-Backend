package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agreement_details")
@Getter
@Setter
@NoArgsConstructor
public class AgreementDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Embedded
    private FarmerInfo farmerInfo;

    @Embedded
    private BuyerInfo buyerInfo;

    @Embedded
    private CropDetails cropDetails;

    @Embedded
    private DeliveryTerms deliveryTerms;

    @Embedded
    private PaymentTerms paymentTerms;

    @ElementCollection
    @CollectionTable(name = "agreement_terms_conditions", joinColumns = @JoinColumn(name = "agreement_details_id"))
    @Column(name = "term_condition")
    private List<TermCondition> termConditions = new ArrayList<>();

    @Column(length = 1000)
    private String additionalNotes;
}