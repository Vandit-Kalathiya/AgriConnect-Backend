package com.agriconnect.Contract.Farming.App.Repository;

import com.agriconnect.Contract.Farming.App.Entity.AgreementDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgreementDocumentRepository extends JpaRepository<AgreementDocument, String> {
}
