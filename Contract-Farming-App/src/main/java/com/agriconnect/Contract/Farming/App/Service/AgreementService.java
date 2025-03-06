package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.DTO.ResponseData;
import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import com.agriconnect.Contract.Farming.App.Repository.AgreementRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class AgreementService {

    @Autowired
    private AgreementRepository agreementRepository;

    public Agreement uploadAgreement(MultipartFile file, String transactionHash, String pdfHash, String orderId, String farmerAddress, String buyerAddress) throws Exception {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        try {
            if (fileName.contains("..")) {
                throw new Exception("Filename contains invalid path sequence "
                        + fileName);
            }

            Agreement agreement
                    = new Agreement(farmerAddress, buyerAddress, orderId, file.getSize(), fileName,
                    file.getContentType(),
                    file.getBytes(), LocalDate.now(), LocalTime.now(), transactionHash, pdfHash);

            Agreement savedAgreement = agreementRepository.save(agreement);

            String downloadURl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/download/")
                    .path(savedAgreement.getPdfHash())
                    .toUriString();

            savedAgreement.setDownloadUrl(downloadURl);

            return agreementRepository.save(savedAgreement);

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    public Agreement getAgreementByTransactionHash(String transactionHash) {
        return agreementRepository.findByTransactionHash(transactionHash).orElseThrow(() -> new EntityNotFoundException("Could not find agreement with transaction hash " + transactionHash));
    }

    public Agreement getAgreementByPdfHash(String pdfHash) {
        return agreementRepository.findByPdfHash(pdfHash).orElseThrow(() -> new EntityNotFoundException("Could not find agreement with pdf hash " + pdfHash));
    }

    public Agreement getAgreementByOrderId(String orderId) {
        return agreementRepository.findByOrderId(orderId).orElseThrow(() -> new EntityNotFoundException("Could not find agreement with pdf hash " + orderId));
    }

    public Agreement saveAgreement(Agreement agreement) {
        return agreementRepository.save(agreement);
    }

    public String deleteAgreement(String pdfHash) {
        Agreement agreement = getAgreementByPdfHash(pdfHash);
        agreementRepository.delete(agreement);
        return "Agreement with pdf hash " + pdfHash + " has been deleted successfully";
    }

    public String deleteAllAgreements() {
        agreementRepository.deleteAll();
        return "All agreements have been deleted successfully";
    }

    public List<Agreement> getAgreementsByAddress(String address) {
        return agreementRepository.findAgreementsByAddress(address);
    }
}
