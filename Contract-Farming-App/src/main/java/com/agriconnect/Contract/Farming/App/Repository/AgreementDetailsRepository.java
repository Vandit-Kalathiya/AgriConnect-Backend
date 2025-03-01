package com.agriconnect.Contract.Farming.App.Repository;


import com.agriconnect.Contract.Farming.App.Entity.AgreementDetails.AgreementDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgreementDetailsRepository extends JpaRepository<AgreementDetails, String> {
    // Basic CRUD operations are provided by JpaRepository
}
