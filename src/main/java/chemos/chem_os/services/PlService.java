package chemos.chem_os.services;

import chemos.chem_os.dto.PlEntryResponse;
import chemos.chem_os.dto.PlUploadResponse;
import chemos.chem_os.model.PlEntry;
import chemos.chem_os.model.PlUpload;
import chemos.chem_os.repository.PlEntryRepository;
import chemos.chem_os.repository.PlUploadRepository;
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
public class PlService {

    private final PlUploadRepository plUploadRepository;
    private final PlEntryRepository plEntryRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public PlUploadResponse uploadCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<PlEntry> entries = parseCsv(file);
        String currentUser = currentUserService.getUsername();

        PlUpload upload = plUploadRepository.save(
                PlUpload.builder()
                        .uploadedBy(currentUser)
                        .rowCount(entries.size())
                        .build()
        );

        entries.forEach(e -> {
            e.setUpload(upload);
            e.setCreatedBy(currentUser);
        });
        plEntryRepository.saveAll(entries);

        return new PlUploadResponse(upload.getId(), upload.getUploadedBy(), upload.getUploadedAt(), upload.getRowCount());
    }

    public List<PlUploadResponse> getAllUploads() {
        return plUploadRepository.findAll().stream()
                .map(u -> new PlUploadResponse(u.getId(), u.getUploadedBy(), u.getUploadedAt(), u.getRowCount()))
                .toList();
    }

    public List<PlEntryResponse> getEntriesByUpload(Long uploadId) {
        if (!plUploadRepository.existsById(uploadId)) {
            throw new IllegalArgumentException("No upload found with id: " + uploadId);
        }
        return plEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new PlEntryResponse(e.getId(), e.getParticular(), e.getAmount(), e.getCreatedBy()))
                .toList();
    }

    public List<PlEntryResponse> getEntriesByUploadAndDate(Long uploadId, LocalDate date) {
        PlUpload upload = plUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("No upload found with id: " + uploadId));

        if (upload.getUploadedAt() == null || !upload.getUploadedAt().toLocalDate().equals(date)) {
            throw new IllegalArgumentException(
                    "Upload " + uploadId + " was not created on " + date);
        }

        return plEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new PlEntryResponse(e.getId(), e.getParticular(), e.getAmount(), e.getCreatedBy()))
                .toList();
    }

    private List<PlEntry> parseCsv(MultipartFile file) {
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
            List<PlEntry> entries = new ArrayList<>();

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

                entries.add(PlEntry.builder()
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
