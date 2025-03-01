package com.agriconnect.Contract.Farming.App.Entity.AgreementDetails;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TermCondition {

    private String tId;

    private String title;

    @Column(length = 1000)
    private String content;
}