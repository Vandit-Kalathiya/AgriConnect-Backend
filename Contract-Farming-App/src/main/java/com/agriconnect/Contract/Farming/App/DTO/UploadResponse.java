package com.agriconnect.Contract.Farming.App.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadResponse {
    private String txHash;
    private String pdfHash;
    private String downloadUrl;
}
