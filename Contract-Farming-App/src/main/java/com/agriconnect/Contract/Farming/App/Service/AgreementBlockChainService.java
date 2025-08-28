//package com.agriconnect.Contract.Farming.App.Service;
//
//import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.web3j.abi.datatypes.DynamicArray;
//import org.web3j.abi.datatypes.Type;
//import org.web3j.abi.datatypes.Utf8String;
//import org.web3j.abi.datatypes.generated.Uint256;
//import org.web3j.abi.datatypes.generated.Uint8;
//import org.web3j.protocol.core.methods.response.TransactionReceipt;
//
//import java.math.BigInteger;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//public class AgreementBlockChainService {
//    private static final Logger logger = LoggerFactory.getLogger(AgreementBlockChainService.class);
//
//    @Autowired
//    private AgreementRegistry agreementRegistry;
//
//    @Autowired
//    private AgreementService agreementService; // Assuming this exists for file management
//
//    public String addAgreement(String orderId, String farmerAddress, String buyerAddress) throws Exception {
//        TransactionReceipt receipt = agreementRegistry.addAgreement(orderId, farmerAddress, buyerAddress).send();
//        logger.info("Agreement added with txHash: {}", receipt.getTransactionHash());
//        return receipt.getTransactionHash();
//    }
//
//    public AgreementRegistry.Agreement getAgreement(String orderId) throws Exception {
//        List<Type> result = agreementRegistry.getAgreement(orderId).send();
//        return new AgreementRegistry.Agreement(
//                orderId,
//                result.get(0).getValue().toString(),          // farmer (Address)
//                result.get(1).getValue().toString(),          // buyer (Address)
//                statusToString(((Uint8) result.get(2)).getValue()), // status (Uint8)
//                ((Uint256) result.get(3)).getValue(),         // timestamp (Uint256)
//                result.get(4).getValue().toString(),          // paymentId (Utf8String)
//                ((Uint256) result.get(5)).getValue(),         // amount (Uint256)
//                result.get(6).getValue().toString()           // refundId (Utf8String)
//        );
//    }
//
//    public List<AgreementRegistry.Agreement> getAllAgreements() throws Exception {
//        List<Type> result = agreementRegistry.getAllAgreementHashes().send();
//        DynamicArray<Utf8String> hashes = (DynamicArray<Utf8String>) result.get(0);
//        List<String> orderIds = hashes.getValue().stream()
//                .map(Utf8String::getValue)
//                .collect(Collectors.toList());
//
//        return orderIds.stream()
//                .map(orderId -> {
//                    try {
//                        return getAgreement(orderId);
//                    } catch (Exception e) {
//                        logger.error("Failed to fetch agreement for orderId: {} - {}", orderId, e.getMessage());
//                        return null;
//                    }
//                })
//                .filter(agreement -> agreement != null)
//                .collect(Collectors.toList());
//    }
//
//    public String getTotalAgreements() throws Exception {
//        try {
////            Uint256 result = agreementRegistry.getTotalAgreements().send();
////            logger.info("Raw response from getTotalAgreements: {}", result);
////            if (result == null) {
////                throw new Exception("Received null response from getTotalAgreements");
////            }
//            BigInteger value = agreementRegistry.getTotalAgreements().send().getValue();
//            logger.info("Converted value: {}", value);
//            return value.toString();
//        } catch (Exception e) {
//            logger.error("Error in getTotalAgreements: {}", e.getMessage(), e);
//            throw new Exception("Unable to fetch total agreements: " + e.getMessage());
//        }
//    }
//
//    public String updateAgreementStatus(String orderId, int status) throws Exception {
//        if (status < 0 || status > 6) {
//            throw new IllegalArgumentException("Status must be between 0 and 6");
//        }
//        TransactionReceipt receipt = agreementRegistry.updateAgreementStatus(orderId, BigInteger.valueOf(status)).send();
//        logger.info("Status updated for orderId: {} with txHash: {}", orderId, receipt.getTransactionHash());
//        return receipt.getTransactionHash();
//    }
//
//    public String deleteAgreement(String orderId) throws Exception {
//        TransactionReceipt receipt = agreementRegistry.deleteAgreement(orderId).send();
//        logger.info("Agreement deleted for orderId: {} with txHash: {}", orderId, receipt.getTransactionHash());
//        agreementService.deleteAgreement(orderId); // Assuming this deletes local files
//        return receipt.getTransactionHash();
//    }
//
//    public String deleteAllAgreements() throws Exception {
//        List<AgreementRegistry.Agreement> agreements = getAllAgreements();
//        TransactionReceipt receipt = agreementRegistry.deleteAllAgreements().send();
//        logger.info("All agreements deleted with txHash: {}", receipt.getTransactionHash());
//        agreementService.deleteAllAgreements(); // Assuming this deletes all local files
//        return receipt.getTransactionHash();
//    }
//
//    private String statusToString(BigInteger status) {
//        switch (status.intValue()) {
//            case 0: return "Pending";
//            case 1: return "Signed";
//            case 2: return "Completed";
//            case 3: return "Cancelled";
//            case 4: return "Refunded";
//            case 5: return "ReturnRequested";
//            case 6: return "ReturnConfirmed";
//            default: return "Unknown";
//        }
//    }
//}