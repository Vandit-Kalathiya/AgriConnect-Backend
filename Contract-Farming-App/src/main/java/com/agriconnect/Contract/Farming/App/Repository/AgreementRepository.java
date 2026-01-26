package com.agriconnect.Contract.Farming.App.Repository;

import com.agriconnect.Contract.Farming.App.Entity.Agreement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, String> {

    Optional<Agreement> findByTransactionHash(String transactionHash);
    Optional<Agreement> findByPdfHash(String pdfHash);
    Optional<Agreement> findByOrderId(String orderId);

    @Query("SELECT a FROM Agreement a WHERE a.farmerAddress = :address OR a.buyerAddress = :address")
    List<Agreement> findAgreementsByAddress(String address);

    // ==================== CURSOR-BASED PAGINATION ====================
    
    // Get agreements with cursor pagination (descending - newest first)
    @Query("SELECT a FROM Agreement a WHERE " +
           "(a.createDate < :cursorDate) OR " +
           "(a.createDate = :cursorDate AND a.createTime < :cursorTime) OR " +
           "(a.createDate = :cursorDate AND a.createTime = :cursorTime AND a.id < :cursorId) " +
           "ORDER BY a.createDate DESC, a.createTime DESC, a.id DESC")
    List<Agreement> findAgreementsAfterCursorDesc(
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    // First page descending
    @Query("SELECT a FROM Agreement a ORDER BY a.createDate DESC, a.createTime DESC, a.id DESC")
    List<Agreement> findAgreementsFirstPageDesc(Pageable pageable);

    // By User Address with cursor pagination
    @Query("SELECT a FROM Agreement a WHERE (a.farmerAddress = :address OR a.buyerAddress = :address) AND " +
           "((a.createDate < :cursorDate) OR " +
           "(a.createDate = :cursorDate AND a.createTime < :cursorTime) OR " +
           "(a.createDate = :cursorDate AND a.createTime = :cursorTime AND a.id < :cursorId)) " +
           "ORDER BY a.createDate DESC, a.createTime DESC, a.id DESC")
    List<Agreement> findAgreementsByAddressAfterCursor(
            @Param("address") String address,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorTime") LocalTime cursorTime,
            @Param("cursorId") String cursorId,
            Pageable pageable);

    @Query("SELECT a FROM Agreement a WHERE (a.farmerAddress = :address OR a.buyerAddress = :address) " +
           "ORDER BY a.createDate DESC, a.createTime DESC, a.id DESC")
    List<Agreement> findAgreementsByAddressFirstPage(@Param("address") String address, Pageable pageable);
}
