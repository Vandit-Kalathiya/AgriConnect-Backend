package com.agriconnect.Contract.Farming.App.AgreementRegistry;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.agriconnect.Contract.Farming.App.AgreementRegistry.Helper.BINARY;

public class AgreementRegistry extends Contract {

    protected AgreementRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        super(BINARY, contractAddress, web3j, credentials, gasProvider);
    }

    public RemoteCall<TransactionReceipt> addAgreement(String pdfHash, String farmer, String buyer) {
        return executeRemoteCallTransaction(
                new Function(
                        "addAgreement",
                        Arrays.asList(new Utf8String(pdfHash), new Address(farmer), new Address(buyer)),
                        Collections.emptyList()
                )
        );
    }

    public RemoteCall<List<Type>> getAgreement(String pdfHash) {
        return executeRemoteCallMultipleValueReturn(
                new Function(
                        "getAgreement",
                        Arrays.asList(new Utf8String(pdfHash)),
                        Arrays.asList(
                                new TypeReference<Utf8String>() {},
                                new TypeReference<Address>() {},
                                new TypeReference<Address>() {},
                                new TypeReference<Uint8>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Utf8String>() {},
                                new TypeReference<Uint256>() {},
                                new TypeReference<Utf8String>() {}
                        )
                )
        );
    }

    public RemoteCall<TransactionReceipt> addPaymentDetails(String pdfHash, String paymentId, BigInteger amount) {
        return executeRemoteCallTransaction(
                new Function("addPaymentDetails", Arrays.asList(new Utf8String(pdfHash), new Utf8String(paymentId), new Uint256(amount)), Collections.emptyList())
        );
    }

    public RemoteCall<TransactionReceipt> recordRefund(String pdfHash, String refundId) {
        return executeRemoteCallTransaction(
                new Function("recordRefund", Arrays.asList(new Utf8String(pdfHash), new Utf8String(refundId)), Collections.emptyList())
        );
    }

    public RemoteCall<TransactionReceipt> requestReturn(String pdfHash) {
        return executeRemoteCallTransaction(
                new Function("requestReturn", Arrays.asList(new Utf8String(pdfHash)), Collections.emptyList())
        );
    }

    public RemoteCall<TransactionReceipt> confirmReturn(String pdfHash) {
        return executeRemoteCallTransaction(
                new Function("confirmReturn", Arrays.asList(new Utf8String(pdfHash)), Collections.emptyList())
        );
    }

    public RemoteCall<List<Type>> getAllAgreementHashes() {
        return executeRemoteCallMultipleValueReturn(
                new Function(
                        "getAllAgreementHashes",
                        Collections.emptyList(),
                        Arrays.asList(new TypeReference<DynamicArray<Utf8String>>() {})
                )
        );
    }

    public RemoteCall<Uint256> getTotalAgreements() {
        return executeRemoteCallSingleValueReturn(
                new Function("getTotalAgreements", Collections.emptyList(),
                        Arrays.asList(new TypeReference<Uint256>() {})),
                Uint256.class
        );
    }

    public RemoteCall<TransactionReceipt> updateAgreementStatus(String pdfHash, BigInteger status) {
        return executeRemoteCallTransaction(
                new Function(
                        "updateAgreementStatus",
                        Arrays.asList(new Utf8String(pdfHash), new Uint8(status)),
                        Collections.emptyList()
                )
        );
    }

    public RemoteCall<TransactionReceipt> deleteAgreement(String pdfHash) {
        return executeRemoteCallTransaction(
                new Function(
                        "deleteAgreement",
                        Arrays.asList(new Utf8String(pdfHash)),
                        Collections.emptyList()
                )
        );
    }

    public RemoteCall<TransactionReceipt> deleteAllAgreements() {
        return executeRemoteCallTransaction(
                new Function(
                        "deleteAllAgreements",
                        Collections.emptyList(),
                        Collections.emptyList()
                )
        );
    }

    public static AgreementRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        return new AgreementRegistry(contractAddress, web3j, credentials, gasProvider);
    }

    public static class Agreement {
        public String pdfHash;
        public String farmer;
        public String buyer;
        public String status;
        public BigInteger timestamp;
        public String paymentId;
        public BigInteger amount;
        public String refundId;

        public Agreement(String pdfHash, String farmer, String buyer, String status, BigInteger timestamp,
                         String paymentId, BigInteger amount, String refundId) {
            this.pdfHash = pdfHash;
            this.farmer = farmer;
            this.buyer = buyer;
            this.status = status;
            this.timestamp = timestamp;
            this.paymentId = paymentId;
            this.amount = amount;
            this.refundId = refundId;
        }
    }
}
