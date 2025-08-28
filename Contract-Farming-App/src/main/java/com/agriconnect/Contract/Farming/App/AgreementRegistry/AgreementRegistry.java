//package com.agriconnect.Contract.Farming.App.AgreementRegistry;
//
//import org.web3j.abi.TypeReference;
//import org.web3j.abi.datatypes.*;
//import org.web3j.abi.datatypes.generated.Uint256;
//import org.web3j.abi.datatypes.generated.Uint8;
//import org.web3j.crypto.Credentials;
//import org.web3j.protocol.Web3j;
//import org.web3j.protocol.core.RemoteCall;
//import org.web3j.protocol.core.methods.response.TransactionReceipt;
//import org.web3j.tx.Contract;
//import org.web3j.tx.gas.ContractGasProvider;
//
//import java.math.BigInteger;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static com.agriconnect.Contract.Farming.App.AgreementRegistry.Helper.BINARY;
//
//public class AgreementRegistry extends Contract {
//
//    protected AgreementRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
//        super(BINARY, contractAddress, web3j, credentials, gasProvider);
//    }
//
//    public RemoteCall<TransactionReceipt> addAgreement(String orderId, String farmer, String buyer) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "addAgreement",
//                        Arrays.asList(new Utf8String(orderId), new Address(farmer), new Address(buyer)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> addPaymentDetails(String orderId, String paymentId, BigInteger amount) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "addPaymentDetails",
//                        Arrays.asList(new Utf8String(orderId), new Utf8String(paymentId), new Uint256(amount)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> requestReturn(String orderId) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "requestReturn",
//                        Arrays.asList(new Utf8String(orderId)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> confirmReturn(String orderId) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "confirmReturn",
//                        Arrays.asList(new Utf8String(orderId)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> recordRefund(String orderId, String refundId) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "recordRefund",
//                        Arrays.asList(new Utf8String(orderId), new Utf8String(refundId)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<List<Type>> getAgreement(String orderId) {
//        return executeRemoteCallMultipleValueReturn(
//                new Function(
//                        "getAgreement",
//                        Arrays.asList(new Utf8String(orderId)),
//                        Arrays.asList(
//                                new TypeReference<Address>() {},    // farmer
//                                new TypeReference<Address>() {},    // buyer
//                                new TypeReference<Uint8>() {},      // status
//                                new TypeReference<Uint256>() {},    // timestamp
//                                new TypeReference<Utf8String>() {}, // paymentId
//                                new TypeReference<Uint256>() {},    // amount
//                                new TypeReference<Utf8String>() {}  // refundId
//                        )
//                )
//        );
//    }
//
//    public RemoteCall<List<Type>> getAllAgreementHashes() {
//        return executeRemoteCallMultipleValueReturn(
//                new Function(
//                        "getAllAgreementHashes",
//                        Collections.emptyList(),
//                        Arrays.asList(new TypeReference<DynamicArray<Utf8String>>() {})
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> updateAgreementStatus(String orderId, BigInteger status) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "updateAgreementStatus",
//                        Arrays.asList(new Utf8String(orderId), new Uint8(status)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> deleteAgreement(String orderId) {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "deleteAgreement",
//                        Arrays.asList(new Utf8String(orderId)),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<TransactionReceipt> deleteAllAgreements() {
//        return executeRemoteCallTransaction(
//                new Function(
//                        "deleteAllAgreements",
//                        Collections.emptyList(),
//                        Collections.emptyList()
//                )
//        );
//    }
//
//    public RemoteCall<Uint256> getTotalAgreements() {
//        return executeRemoteCallSingleValueReturn(
//                new Function(
//                        "getTotalAgreements",
//                        Collections.emptyList(),
//                        Arrays.asList(new TypeReference<Uint256>() {})
//                ),
//                Uint256.class
//        );
//    }
//
//    public RemoteCall<Bool> agreementExists(String orderId) {
//        return executeRemoteCallSingleValueReturn(
//                new Function(
//                        "agreementExists",
//                        Arrays.asList(new Utf8String(orderId)),
//                        Arrays.asList(new TypeReference<Bool>() {})
//                ),
//                Bool.class
//        );
//    }
//
//    public static AgreementRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
//        return new AgreementRegistry(contractAddress, web3j, credentials, gasProvider);
//    }
//
//    public static class Agreement {
//        public String orderId;
//        public String farmer;
//        public String buyer;
//        public String status; // Changed to String for readability
//        public BigInteger timestamp;
//        public String paymentId;
//        public BigInteger amount;
//        public String refundId;
//
//        public Agreement(String orderId, String farmer, String buyer, String status, BigInteger timestamp,
//                         String paymentId, BigInteger amount, String refundId) {
//            this.orderId = orderId;
//            this.farmer = farmer;
//            this.buyer = buyer;
//            this.status = status;
//            this.timestamp = timestamp;
//            this.paymentId = paymentId;
//            this.amount = amount;
//            this.refundId = refundId;
//        }
//    }
//}