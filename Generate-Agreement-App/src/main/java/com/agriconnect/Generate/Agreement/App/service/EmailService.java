package com.agriconnect.Generate.Agreement.App.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendStorageRequestNotification(String toEmail, String farmerName, String cropName, double cropQuantity, int storageDuration, String coldStorageName) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlContent = "<h1>New Storage Request Received</h1>"
                + "<p>A farmer has submitted a storage request with the following details:</p>"
                + "<ul>"
                + "<li><strong>Farmer Name:</strong> " + farmerName + "</li>"
                + "<li><strong>Crop Name:</strong> " + cropName + "</li>"
                + "<li><strong>Crop Quantity:</strong> " + cropQuantity + " tons</li>"
                + "<li><strong>Storage Duration:</strong> " + storageDuration + " days</li>"
                + "<li><strong>Cold Storage:</strong> " + coldStorageName + "</li>"
                + "</ul>"
                + "<p>Please review and approve this request at your earliest convenience.</p>";

        helper.setText(htmlContent, true); // Set to true for HTML content
        helper.setTo(toEmail);
        helper.setSubject("New Storage Request from Farmer");
        helper.setFrom("vanditkaj266@gmail.com");

        mailSender.send(mimeMessage);
    }
}
