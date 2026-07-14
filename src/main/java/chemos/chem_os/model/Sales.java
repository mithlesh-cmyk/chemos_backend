package chemos.chem_os.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "sales_form", indexes = {
        @Index(name = "idx_sales_form_status_market_status_date", columnList = "status, market_status, date")
})
public class Sales {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    private LocalDate date;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "sale_type")
    private String salesType;

    @Column(name = "company_to")
    private String companyTo;

    @Column(name = "company_from")
    private String companyFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products product;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "price")
    private Double price;

    @Column(name = "payment_term")
    private String payment;

    @Column(name = "delivery_term")
    private String deliveryTerm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "port", referencedColumnName = "id")
    private Ports port;

    @Column(name = "market_price")
    private Double marketPrice;

    @Column(name = "market_status")
    private String marketStatus;

    @Column(name = "storage_days")
    private Integer storageDays;

    @Column(name = "make")
    private String make;

    @Column(name = "packaging")
    private String packaging;

    @Column(name = "origin")
    private String origin;

    @Column(name = "transit_tolerance")
    private String transitTolerance;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "vessel_name")
    private String vesselName;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_person")
    private Salespersons salesPerson;

    @Column(name = "broker_name")
    private String brokerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}
