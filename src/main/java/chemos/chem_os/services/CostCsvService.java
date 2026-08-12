package chemos.chem_os.services;

import chemos.chem_os.dto.CostCsvEntryResponse;
import chemos.chem_os.dto.CostCsvTotalResponse;
import chemos.chem_os.dto.CostCsvUploadResponse;
import chemos.chem_os.model.CostCsvEntry;
import chemos.chem_os.model.CostCsvUpload;
import chemos.chem_os.repository.CostCsvEntryRepository;
import chemos.chem_os.repository.CostCsvUploadRepository;
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
public class CostCsvService {

    private final CostCsvUploadRepository costCsvUploadRepository;
    private final CostCsvEntryRepository costCsvEntryRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public CostCsvUploadResponse uploadCsv(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        List<CostCsvEntry> entries = parseCsv(file);
        String currentUser = currentUserService.getUsername();

        CostCsvUpload upload = costCsvUploadRepository.save(
                CostCsvUpload.builder()
                        .uploadedBy(currentUser)
                        .rowCount(entries.size())
                        .build()
        );

        entries.forEach(e -> {
            e.setUpload(upload);
            e.setCreatedBy(currentUser);
        });
        costCsvEntryRepository.saveAll(entries);

        return new CostCsvUploadResponse(upload.getId(), upload.getUploadedBy(), upload.getUploadedAt(), upload.getRowCount());
    }

    public List<CostCsvUploadResponse> getAllUploads() {
        return costCsvUploadRepository.findAll().stream()
                .map(u -> new CostCsvUploadResponse(u.getId(), u.getUploadedBy(), u.getUploadedAt(), u.getRowCount()))
                .toList();
    }

    public List<CostCsvEntryResponse> getEntriesByUpload(Long uploadId) {
        if (!costCsvUploadRepository.existsById(uploadId)) {
            throw new IllegalArgumentException("No upload found with id: " + uploadId);
        }
        return costCsvEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new CostCsvEntryResponse(e.getId(), e.getParticular(), e.getDirectCost(), e.getIndirectCost(), e.getCreatedBy()))
                .toList();
    }

    public List<CostCsvEntryResponse> getEntriesByUploadAndDate(Long uploadId, LocalDate date) {
        CostCsvUpload upload = costCsvUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("No upload found with id: " + uploadId));

        if (upload.getUploadedAt() == null || !upload.getUploadedAt().toLocalDate().equals(date)) {
            throw new IllegalArgumentException(
                    "Upload " + uploadId + " was not created on " + date);
        }

        return costCsvEntryRepository.findByUploadId(uploadId).stream()
                .map(e -> new CostCsvEntryResponse(e.getId(), e.getParticular(), e.getDirectCost(), e.getIndirectCost(), e.getCreatedBy()))
                .toList();
    }

    public CostCsvTotalResponse getTotalCost() {
        CostCsvUpload latestUpload = costCsvUploadRepository.findTopByOrderByUploadedAtDesc()
                .orElseThrow(() -> new IllegalArgumentException("No CSV uploads found"));

        BigDecimal totalDirectCost = costCsvEntryRepository.sumDirectCostByUploadId(latestUpload.getId());
        BigDecimal totalIndirectCost = costCsvEntryRepository.sumIndirectCostByUploadId(latestUpload.getId());
        return new CostCsvTotalResponse(totalDirectCost, totalIndirectCost);
    }

    private List<CostCsvEntry> parseCsv(MultipartFile file) {
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
            List<CostCsvEntry> entries = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                String particular = record.get("particular");
                String directCostStr = record.get("direct_cost");
                String indirectCostStr = record.get("indirect_cost");

                if (particular == null || particular.isBlank()) {
                    continue;
                }

                BigDecimal directCost;
                try {
                    directCost = new BigDecimal(directCostStr.replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid direct_cost \"" + directCostStr + "\" at row " + record.getRecordNumber());
                }

                BigDecimal indirectCost;
                try {
                    indirectCost = new BigDecimal(indirectCostStr.replace(",", ""));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Invalid indirect_cost \"" + indirectCostStr + "\" at row " + record.getRecordNumber());
                }

                entries.add(CostCsvEntry.builder()
                        .particular(particular)
                        .directCost(directCost)
                        .indirectCost(indirectCost)
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
