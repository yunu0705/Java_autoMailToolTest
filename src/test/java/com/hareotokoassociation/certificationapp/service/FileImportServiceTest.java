package com.hareotokoassociation.certificationapp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.hareotokoassociation.certificationapp.model.Applicant;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.main.web-application-type=none")
class FileImportServiceTest {

    @Autowired
    private FileImportService fileImportService;

    @Test
    void testReadApplicantsFromFile_validRowsOnly() throws Exception {
        String filePath = "src/main/resources/test_applicants.csv";

        List<Applicant> result = fileImportService.readApplicantsFromFile(filePath);

        assertEquals(3, result.size());
        List<String> emails = result.stream().map(Applicant::getEmail).toList();
        assertTrue(emails.contains("yamada@example.com"));
        assertTrue(emails.contains("sato@example.com"));
        assertTrue(emails.contains("tanaka@example.com"));
    }
}
