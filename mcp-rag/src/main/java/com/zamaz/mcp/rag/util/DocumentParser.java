package com.zamaz.mcp.rag.util;

import com.zamaz.mcp.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for parsing various document formats.
 */
@Slf4j
@Component
public class DocumentParser {
    
    public String parseDocument(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        
        log.info("Parsing document: {} with content type: {}", fileName, contentType);
        
        try (InputStream inputStream = file.getInputStream()) {
            if (isPdf(contentType, fileName)) {
                return parsePdf(inputStream);
            } else if (isWord(contentType, fileName)) {
                return parseWord(inputStream, fileName);
            } else if (isText(contentType, fileName)) {
                return parseText(inputStream);
            } else if (isMarkdown(contentType, fileName)) {
                return parseMarkdown(inputStream);
            } else {
                throw new BusinessException("Unsupported file type: " + contentType, "UNSUPPORTED_FILE_TYPE")
                        .withDetail("contentType", contentType)
                        .withDetail("fileName", fileName);
            }
        }
    }
    
    private boolean isPdf(String contentType, String fileName) {
        return "application/pdf".equalsIgnoreCase(contentType) ||
               (fileName != null && fileName.toLowerCase().endsWith(".pdf"));
    }
    
    private boolean isWord(String contentType, String fileName) {
        return "application/msword".equalsIgnoreCase(contentType) ||
               "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(contentType) ||
               (fileName != null && (fileName.toLowerCase().endsWith(".doc") || 
                                   fileName.toLowerCase().endsWith(".docx")));
    }
    
    private boolean isText(String contentType, String fileName) {
        return contentType != null && contentType.startsWith("text/") ||
               (fileName != null && fileName.toLowerCase().endsWith(".txt"));
    }
    
    private boolean isMarkdown(String contentType, String fileName) {
        return "text/markdown".equalsIgnoreCase(contentType) ||
               (fileName != null && (fileName.toLowerCase().endsWith(".md") || 
                                   fileName.toLowerCase().endsWith(".markdown")));
    }
    
    private String parsePdf(InputStream inputStream) throws IOException {
        byte[] pdfBytes = IOUtils.toByteArray(inputStream);
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    private String parseWord(InputStream inputStream, String fileName) throws IOException {
        if (fileName != null && fileName.toLowerCase().endsWith(".docx")) {
            return parseDocx(inputStream);
        } else {
            return parseDoc(inputStream);
        }
    }
    
    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }
    
    private String parseDoc(InputStream inputStream) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream)) {
            WordExtractor extractor = new WordExtractor(document);
            return extractor.getText();
        }
    }
    
    private String parseText(InputStream inputStream) throws IOException {
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
    
    private String parseMarkdown(InputStream inputStream) throws IOException {
        // For now, treat markdown as plain text
        // In a production system, you might want to parse and extract specific elements
        return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    }
}