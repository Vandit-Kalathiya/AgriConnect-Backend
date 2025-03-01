package com.agriconnect.Generate.Agreement.App.Controller;


import com.agriconnect.Generate.Agreement.App.model.ContractRequest;
import com.agriconnect.Generate.Agreement.App.service.PdfGenerationService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);

    private final PdfGenerationService pdfGenerationService;

    @Autowired
    public ContractController(PdfGenerationService pdfGenerationService) {
        this.pdfGenerationService = pdfGenerationService;
    }

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<InputStreamResource> generateContract(
            @RequestBody ContractRequest contractRequest,
            HttpServletRequest request) {
        try {
            logger.info("Received contract generation request: {}", contractRequest);

            // Validate input
            if (contractRequest == null || contractRequest.getFarmerInfo() == null || contractRequest.getBuyerInfo() == null) {
                logger.warn("Invalid contract request: Missing required fields");
                throw new IllegalArgumentException("Contract request or party information cannot be null");
            }

            // Generate PDF content
            byte[] pdfContent = pdfGenerationService.generateContractPdf(contractRequest);
            if (pdfContent == null || pdfContent.length == 0) {
                logger.error("Generated PDF content is empty");
                throw new RuntimeException("Generated PDF content is empty");
            }

            // Prepare the input stream for the response
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pdfContent);

            // Sanitize filenames to avoid invalid characters
            String farmerName = contractRequest.getFarmerInfo().getFarmerName().replaceAll("[^a-zA-Z0-9]", "_");
            String buyerName = contractRequest.getBuyerInfo().getBuyerName().replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = String.format("AgriConnect_Contract_%s_%s.pdf", farmerName, buyerName);

            // Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
            headers.setContentLength(pdfContent.length);

            logger.info("Successfully generated contract PDF: {}", fileName);
            return new ResponseEntity<>(new InputStreamResource(byteArrayInputStream), headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new InputStreamResource(new ByteArrayInputStream(("Invalid request: " + e.getMessage()).getBytes())));
        } catch (Exception e) {
            logger.error("Error generating PDF", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InputStreamResource(new ByteArrayInputStream(("Error generating PDF: " + e.getMessage()).getBytes())));
        }
    }
}