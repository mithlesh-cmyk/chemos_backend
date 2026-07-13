package chemos.chem_os.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private String id;

    @Column(name = "purchase_type")
    private String purchaseType;

    @Column(name = "company_to")
    private String companyTo;

    @Column(name = "company_from")
    private String companyFrom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product")
    private Products product;

    @Column(name = "vessel_name")
    private String vesselName;

    private String shipment;

    private Double quantity;

    @Column(name = "price_fc", precision = 19, scale = 4)
    private BigDecimal priceFc;

    private String currency;

    @Column(name = "offer_usd", precision = 19, scale = 4)
    private BigDecimal offerUsd;

    @Column(name = "exchange_rate", precision = 19, scale = 4)
    private BigDecimal exchangeRate;

    @Column(name = "price_inr", precision = 19, scale = 4)
    private BigDecimal priceInr;

    @Column(name = "delivery_term")
    private String deliveryTerm;

    @Column(name = "payment_days")
    private Integer paymentDays;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "port")
    private Ports port;

    @Column(name = "market_price", precision = 19, scale = 4)
    private BigDecimal marketPrice;

    @Column(name = "market_status")
    private String marketStatus;

    @Column(name = "replacement_cost", precision = 19, scale = 4)
    private BigDecimal replacementCost;

    private String make;

    private String packaging;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origin")
    private Countries origin;

    @Column(precision = 19, scale = 4)
    private BigDecimal expense;

    @Column(name = "custom_duty", precision = 19, scale = 4)
    private BigDecimal customDuty;

    @Column(precision = 19, scale = 4)
    private BigDecimal sws;

    @Column(name = "additional_charge", precision = 19, scale = 4)
    private BigDecimal add;

    @Column(name = "other_expense", precision = 19, scale = 4)
    private BigDecimal otherExpense;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "discharge_ports")
    private Ports dischargePort;

    @Column(name = "price_type")
    private String priceType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_term")
    private PaymentTerms paymentTerm;

    private LocalDate etd;

    private LocalDate eta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;
}