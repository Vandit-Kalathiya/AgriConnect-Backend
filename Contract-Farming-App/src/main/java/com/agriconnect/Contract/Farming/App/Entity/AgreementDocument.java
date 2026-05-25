package com.agriconnect.Contract.Farming.App.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "agreement")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgreementDocument {

    @Id
    private String id;

    @Lob
    private byte[] data;
}
