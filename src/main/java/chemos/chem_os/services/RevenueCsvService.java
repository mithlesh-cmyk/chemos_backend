package chemos.chem_os.services;

import chemos.chem_os.dto.RevenueCsvEntryResponse;
import chemos.chem_os.dto.RevenueCsvTotalResponse;
import chemos.chem_os.dto.RevenueCsvUploadResponse;
import chemos.chem_os.model.RevenueCsvEntry;
import chemos.chem_os.model.RevenueCsvUpload;
import chemos.chem_os.repository.RevenueCsvEntryRepository;
import chemos.chem_os.repository.RevenueCsvUploadRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RevenueCsvService {

    private final RevenueCsvUploadRepository revenueCsvUploadRepository;
    private final RevenueCsvEntryRepository revenueCsvEntryRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public RevenueCsvUploadResponse uploadCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<RevenueCsvEntry> entries = parseCsv(file);
        String currentUser = currentUserService.getUsername();

        RevenueCsvUpload upload = revenueCsvUploadRepository.save(
                RevenueCsvUpload.builder()
                        .uploadedBy(currentUser)
                        .rowCount(entries.size())
                        .build()
        );

        entries.forEach(e -> {
            e.setUpload(upload);
            e.setCreatedBy(currentUser);
        });
        revenueCsvEntryRepository.saveAll(entries);

        return new RevenueCsvUploadResponse(upload.getId(), upload.getUploadedBy(), upload.getUploadedAt(), upload.getRowCount());
    }

    public List<RevenueCsvUploadResponse> getAllUploads() {
        return revenueCsvUploadRepository.findAll().stream()
                .map(u -> new RevenueCsvUploadResponse(u.getId(), u.getUploadedBy(), u.getUploadedAt(), u.getRowCount()))
                .toList();
    }

    public List<RevenueCsvEntryResponse> getEntriesByUpload(Long uploadId) {
        if (!revenueCsvUploadRepository.existsById(uploadId)) {
            throw new IllegalArgumentException("No upload found with id: " + uploadId);
        }
        return revenueCsvEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new RevenueCsvEntryResponse(e.getId(), e.getParticular(), e.getAmount(), e.getCreatedBy()))
                .toList();
    }

    public List<RevenueCsvEntryResponse> getEntriesByUploadAndDate(Long uploadId, LocalDate date) {
        RevenueCsvUpload upload = revenueCsvUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("No upload found with id: " + uploadId));

        if (upload.getUploadedAt() == null || !upload.getUploadedAt().toLocalDate().equals(date)) {
            throw new IllegalArgumentException(
                    "Upload " + uploadId + " was not created on " + date);
        }

        return revenueCsvEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new RevenueCsvEntryResponse(e.getId(), e.getParticular(), e.getAmount(), e.getCreatedBy()))
                .toList();
    }

    public RevenueCsvTotalResponse getTotalAmount() {
        RevenueCsvUpload latestUpload = revenueCsvUploadRepository.findTopByOrderByUploadedAtDesc()
                .orElseThrow(() -> new IllegalArgumentException("No CSV uploads found"));

        BigDecimal total = revenueCsvEntryRepository.sumAmountsByUploadId(latestUpload.getId());
        return new RevenueCsvTotalResponse(total);
    }

    private List<RevenueCsvEntry> parseCsv(MultipartFile file) {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
                CSVParser csvParser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder()
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreHeaderCase(true)
                                .setTrim(true)
                                .build())
        ) {
            List<RevenueCsvEntry> entries = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                String particular = record.get("particular");
                String amountStr = record.get("amount");

                if (particular == null || particular.isBlank()) {
                    continue;
                }

                BigDecimal amount;
                try {
                    amount = new BigDecimal(amountStr.replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid amount \"" + amountStr + "\" at row " + record.getRecordNumber());
                }

                entries.add(RevenueCsvEntry.builder()
                        .particular(particular)
                        .amount(amount)
                        .build());
            }

            if (entries.isEmpty()) {
                throw new IllegalArgumentException("CSV file contains no valid data rows");
            }

            return entries;
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }
    }
}
