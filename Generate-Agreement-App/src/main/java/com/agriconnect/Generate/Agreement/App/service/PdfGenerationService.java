package com.agriconnect.Generate.Agreement.App.service;

import com.agriconnect.Generate.Agreement.App.model.ContractRequest;
import com.agriconnect.Generate.Agreement.App.util.PdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private final PdfGenerator pdfGenerator;

    @Autowired
    public PdfGenerationService(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public byte[] generateContractPdf(ContractRequest contractRequest) {
        try {
            // Get current date for the contract
            String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

            // Generate PDF content
            return pdfGenerator.generateContractPdf(contractRequest, currentDate);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate contract PDF", e);
        }
    }
}