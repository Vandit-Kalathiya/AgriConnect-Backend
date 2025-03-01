package com.agriconnect.Contract.Farming.App.Repository;

import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, String> {

    Optional<Agreement> findByTransactionHash(String transactionHash);
    Optional<Agreement> findByPdfHash(String pdfHash);

    Optional<Agreement> findByOrderId(String orderId);
}
