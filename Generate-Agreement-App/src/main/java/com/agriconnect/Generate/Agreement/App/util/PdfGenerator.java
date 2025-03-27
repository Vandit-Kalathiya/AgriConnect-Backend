package com.agriconnect.Generate.Agreement.App.util;

import com.agriconnect.Generate.Agreement.App.model.ContractRequest;
import com.agriconnect.Generate.Agreement.App.model.TermCondition;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class PdfGenerator {

    public byte[] generateContractPdf(ContractRequest contractRequest, String contractDate) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(36, 36, 36, 36);

        // Initialize fonts
        PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont fontItalic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
        DeviceRgb jewel700 = new DeviceRgb(21, 128, 61);


        // Create header
        createHeader(document, fontBold, contractDate);

        // Create party information section
        createPartyInfoSection(document, contractRequest, fontBold, fontNormal);

        // Create crop details section
        createCropDetailsSection(document, contractRequest, fontBold, fontNormal);

        // Create delivery terms section
        createDeliveryTermsSection(document, contractRequest, fontBold, fontNormal);

        // Create payment terms section
        createPaymentTermsSection(document, contractRequest, fontBold, fontNormal);

        // Create terms and conditions section
//        createTermsConditionsSection(document, contractRequest, fontBold, fontNormal);

        // Create additional notes section
        createAdditionalNotesSection(document, contractRequest, fontBold, fontNormal);

        // Create signatures section
        createSignaturesSection(document, contractRequest, fontBold, fontNormal);

        // Close document
        document.close();

        return baos.toByteArray();
    }

    private void createHeader(Document document, PdfFont fontBold, String contractDate) {
        // Company logo/name
        Paragraph companyName = new Paragraph("AgriConnect")
                .setFont(fontBold)
                .setFontSize(20)
                .setFontColor(new DeviceRgb(21, 128, 61))
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(companyName);

        // Contract title
        Paragraph title = new Paragraph("FARM PRODUCE SALES AGREEMENT")
                .setFont(fontBold)
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(title);

        // Contract date
        Paragraph date = new Paragraph("Contract Date: " + contractDate)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(date);

        // Add divider line
        document.add(new Paragraph("\n"));
        Div divider = new Div()
                .setHeight(1)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setMarginBottom(15);
        document.add(divider);
    }

    private void createPartyInfoSection(Document document, ContractRequest request,
                                        PdfFont fontBold, PdfFont fontNormal) {

        // Section title
        Paragraph sectionTitle = new Paragraph("PARTIES TO THE AGREEMENT")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(15);
        document.add(sectionTitle);

        // Create two-column table for party info
        Table table = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

        // Farmer column
        Cell farmerTitle = new Cell()
                .add(new Paragraph("FARMER").setFont(fontBold).setFontSize(12))
                .setBorder(null);
        table.addCell(farmerTitle);

        // Buyer column
        Cell buyerTitle = new Cell()
                .add(new Paragraph("BUYER").setFont(fontBold).setFontSize(12))
                .setBorder(null);
        table.addCell(buyerTitle);

        // Farmer details
        Cell farmerDetails = new Cell()
                .add(new Paragraph("Name: " + request.getFarmerInfo().getFarmerName())
                        .setFont(fontNormal).setFontSize(10))
                .add(new Paragraph("Address: " + request.getFarmerInfo().getFarmerAddress())
                        .setFont(fontNormal).setFontSize(10))
                .add(new Paragraph("Contact: " + request.getFarmerInfo().getFarmerContact())
                        .setFont(fontNormal).setFontSize(10))
                .setBorder(null);
        table.addCell(farmerDetails);

        // Buyer details
        Cell buyerDetails = new Cell()
                .add(new Paragraph("Name: " + request.getBuyerInfo().getBuyerName())
                        .setFont(fontNormal).setFontSize(10))
                .add(new Paragraph("Address: " + request.getBuyerInfo().getBuyerAddress())
                        .setFont(fontNormal).setFontSize(10))
                .add(new Paragraph("Contact: " + request.getBuyerInfo().getBuyerContact())
                        .setFont(fontNormal).setFontSize(10))
                .setBorder(null);
        table.addCell(buyerDetails);

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void createCropDetailsSection(Document document, ContractRequest request,
                                          PdfFont fontBold, PdfFont fontNormal) {

        // Section title
        Paragraph sectionTitle = new Paragraph("CROP DETAILS")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10);
        document.add(sectionTitle);

        // Create crop details grid
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Crop Type:", request.getCropDetails().getType(), fontBold, fontNormal);
        addTableRow(table, "Variety:", request.getCropDetails().getVariety(), fontBold, fontNormal);
        addTableRow(table, "Quantity:", request.getCropDetails().getQuantity(), fontBold, fontNormal);
        addTableRow(table, "Price Per Unit:", request.getCropDetails().getPricePerUnit(), fontBold, fontNormal);

        document.add(table);

        // Quality standards as bullet points
        Paragraph qualityStandardsTitle = new Paragraph("Quality Standards:")
                .setFont(fontBold)
                .setFontSize(12)
                .setMarginTop(10);
        document.add(qualityStandardsTitle);

        List qualityList = new List()
                .setSymbolIndent(12)
                .setListSymbol("•");

        for (String standard : request.getCropDetails().getQualityStandards()) {
            qualityList.add((ListItem) new ListItem(standard).setFont(fontNormal).setFontSize(10));
        }

        document.add(qualityList);
        document.add(new Paragraph("\n"));
    }

    private void createDeliveryTermsSection(Document document, ContractRequest request,
                                            PdfFont fontBold, PdfFont fontNormal) {

        // Section title
        Paragraph sectionTitle = new Paragraph("DELIVERY TERMS")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10);
        document.add(sectionTitle);

        // Create delivery terms grid
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Delivery Date:", request.getDeliveryTerms().getDate(), fontBold, fontNormal);
        addTableRow(table, "Location:", request.getDeliveryTerms().getLocation(), fontBold, fontNormal);
        addTableRow(table, "Transportation:", request.getDeliveryTerms().getTransportation(), fontBold, fontNormal);
        addTableRow(table, "Packaging:", request.getDeliveryTerms().getPackaging(), fontBold, fontNormal);

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void createPaymentTermsSection(Document document, ContractRequest request,
                                           PdfFont fontBold, PdfFont fontNormal) {

        // Section title
        Paragraph sectionTitle = new Paragraph("PAYMENT TERMS")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10);
        document.add(sectionTitle);

        // Create payment terms grid
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .setWidth(UnitValue.createPercentValue(100));

        addTableRow(table, "Total Value:", String.valueOf(Double.parseDouble(request.getCropDetails().getPricePerUnit())*Long.parseLong(request.getCropDetails().getQuantity())), fontBold, fontNormal);
        addTableRow(table, "Payment Method:", request.getPaymentTerms().getMethod(), fontBold, fontNormal);
        addTableRow(table, "Advance Payment:", request.getPaymentTerms().getAdvancePayment(), fontBold, fontNormal);
        addTableRow(table, "Balance Due:", request.getPaymentTerms().getBalanceDue(), fontBold, fontNormal);

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void createTermsConditionsSection(Document document, ContractRequest request,
                                              PdfFont fontBold, PdfFont fontNormal) {

        // Section title
        Paragraph sectionTitle = new Paragraph("TERMS AND CONDITIONS")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(10);
        document.add(sectionTitle);

        // Create numbered list for terms and conditions
        List termsList = new List().setSymbolIndent(12);

        for (TermCondition term : request.getTermsConditions()) {
            Paragraph termTitle = new Paragraph(term.getTitle())
                    .setFont(fontBold)
                    .setFontSize(11);

            Paragraph termContent = new Paragraph(term.getContent())
                    .setFont(fontNormal)
                    .setFontSize(10);

            ListItem item = new ListItem();
            item.add(termTitle);
            item.add(termContent);

            termsList.add(item);
        }

        document.add(termsList);
        document.add(new Paragraph("\n"));
    }

    private void createAdditionalNotesSection(Document document, ContractRequest request,
                                              PdfFont fontBold, PdfFont fontNormal) {

        if (request.getAdditionalNotes() != null && !request.getAdditionalNotes().isEmpty()) {
            // Section title
            Paragraph sectionTitle = new Paragraph("ADDITIONAL NOTES")
                    .setFont(fontBold)
                    .setFontSize(14)
                    .setMarginTop(10);
            document.add(sectionTitle);

            // Notes content
            Paragraph notes = new Paragraph(request.getAdditionalNotes())
                    .setFont(fontNormal)
                    .setFontSize(10);
            document.add(notes);

            document.add(new Paragraph("\n"));
        }
    }

    private void createSignaturesSection(Document document, ContractRequest request,
                                         PdfFont fontBold, PdfFont fontNormal) throws IOException {
        // Section title
        Paragraph sectionTitle = new Paragraph("SIGNATURES")
                .setFont(fontBold)
                .setFontSize(14)
                .setMarginTop(20);
        document.add(sectionTitle);

        // Create signature table
        Table sigTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100));

        // Farmer signature (image)
        Cell farmerSigCell = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.CENTER);

        if (request.getFarmerInfo().getFarmerSignature() != null && request.getFarmerInfo().getFarmerSignature().length > 0) {
            Image farmerSigImage = new Image(ImageDataFactory.create(request.getFarmerInfo().getFarmerSignature()))
                    .scaleToFit(100, 50) // Adjust size as needed
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            farmerSigCell.add(farmerSigImage);
        } else {
            farmerSigCell.add(new Paragraph("Farmer Signature Missing").setFontSize(10));
        }

        farmerSigCell
                .add(new Paragraph("Farmer: " + request.getFarmerInfo().getFarmerName()).setFontSize(10))
                .add(new Paragraph("Date: ___________________").setFontSize(10));
        sigTable.addCell(farmerSigCell);

        // Buyer signature (image)
        Cell buyerSigCell = new Cell()
                .setBorder(null)
                .setTextAlignment(TextAlignment.CENTER);

        if (request.getBuyerInfo().getBuyerSignature() != null && request.getBuyerInfo().getBuyerSignature().length > 0) {
            Image buyerSigImage = new Image(ImageDataFactory.create(request.getBuyerInfo().getBuyerSignature()))
                    .scaleToFit(100, 50) // Adjust size as needed
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);
            buyerSigCell.add(buyerSigImage);
        } else {
            buyerSigCell.add(new Paragraph("Buyer Signature Missing").setFontSize(10));
        }

        buyerSigCell
                .add(new Paragraph("Buyer: " + request.getBuyerInfo().getBuyerName()).setFontSize(10))
                .add(new Paragraph("Date: ___________________").setFontSize(10));
        sigTable.addCell(buyerSigCell);

        document.add(sigTable);

        // Footer
        Paragraph footer = new Paragraph("This is a legally binding agreement between the Farmer and Buyer.")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(50);
        document.add(footer);
    }

    private void addTableRow(Table table, String label, String value, PdfFont fontBold, PdfFont fontNormal) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(fontBold).setFontSize(10))
                .setBorder(null)
                .setPaddingBottom(5);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(fontNormal).setFontSize(10))
                .setBorder(null)
                .setPaddingBottom(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
