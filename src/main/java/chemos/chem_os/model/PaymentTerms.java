package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "payment_term_master")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PaymentTerms {

    @Id
    private Integer id;

    @Column(name = "payment_term", nullable = false, columnDefinition = "text")
    private String paymentTerm;

    @Column(name = "payment_code", columnDefinition = "text")
    private String paymentCode;

    @Column(name = "credit_days")
    private Integer creditDays;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
