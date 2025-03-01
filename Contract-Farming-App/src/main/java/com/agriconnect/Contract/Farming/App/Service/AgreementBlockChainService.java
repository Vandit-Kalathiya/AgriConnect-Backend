package com.agriconnect.Contract.Farming.App.Service;

import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
import com.agriconnect.Contract.Farming.App.Entity.Order;
import com.agriconnect.Contract.Farming.App.Repository.OrderRepository;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AgreementBlockChainService {
    private static final Logger logger = LoggerFactory.getLogger(AgreementBlockChainService.class);

    @Autowired
    private AgreementRegistry agreementRegistry;
    @Autowired
    private AgreementService agreementService;

    public String addAgreement(String pdfHash, String farmerAddress, String buyerAddress) throws Exception {
        TransactionReceipt receipt = agreementRegistry.addAgreement(pdfHash, farmerAddress, buyerAddress).send();
        return receipt.getTransactionHash();
    }

    // Updated to return AgreementDTO with string status
    public AgreementRegistry.Agreement getAgreement(String pdfHash) throws Exception {
        List<Type> result = agreementRegistry.getAgreement(pdfHash).send();
        return new AgreementRegistry.Agreement(
                result.get(0).getValue().toString(),           // pdfHash
                result.get(1).getValue().toString(),           // farmer
                result.get(2).getValue().toString(),           // buyer
                statusToString(((Uint8) result.get(3)).getValue()), // Convert status to string
                ((Uint256) result.get(4)).getValue(),          // timestamp
                result.get(5).getValue().toString(),
                ((Uint256) result.get(6)).getValue(),
                result.get(7).getValue().toString()
        );
    }

    public List<String> getAllAgreementHashes() throws Exception {
        List<Type> result = agreementRegistry.getAllAgreementHashes().send();
        DynamicArray<Utf8String> hashes = (DynamicArray<Utf8String>) result.get(0);
        return hashes.getValue().stream()
                .map(Utf8String::getValue)
                .collect(Collectors.toList());
    }

    public BigInteger getTotalAgreements() throws Exception {
        return agreementRegistry.getTotalAgreements().send().getValue();
    }

    public String updateAgreementStatus(String pdfHash, int status) throws Exception {
        TransactionReceipt receipt = agreementRegistry.updateAgreementStatus(
                pdfHash,
                BigInteger.valueOf(status)
        ).send();
        return receipt.getTransactionHash();
    }

    public String deleteAgreement(String pdfHash) throws Exception {
        logger.info("Deleting agreement with PDF hash: {}", pdfHash);

        // Delete from blockchain
        TransactionReceipt receipt = agreementRegistry.deleteAgreement(pdfHash).send();

        // Delete the local PDF file
        agreementService.deleteAgreement(pdfHash);

        logger.info("Transaction hash: {}", receipt.getTransactionHash());
        return receipt.getTransactionHash();
    }

    public String deleteAllAgreements() throws Exception {
        logger.info("Deleting all agreements");

        // Get all hashes before deletion for file cleanup
        List<String> hashes = getAllAgreementHashes();

        // Delete from blockchain
        TransactionReceipt receipt = agreementRegistry.deleteAllAgreements().send();

        // Delete all local PDF files
        agreementService.deleteAllAgreements();

        logger.info("Transaction hash: {}", receipt.getTransactionHash());
        return receipt.getTransactionHash();
    }

    // Helper method to convert numeric status to string
    private String statusToString(BigInteger status) {
        switch (status.intValue()) {
            case 0: return "Pending";
            case 1: return "Signed";
            case 2: return "Completed";
            case 3: return "Cancelled";
            case 4: return "Refunded";
            case 5: return "ReturnRequested";
            case 6: return "ReturnConfirmed";
            default: return "Unknown";
        }
    }
}