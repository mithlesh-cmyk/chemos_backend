package chemos.chem_os.model;


import chemos.chem_os.SalesType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "sales_form")
public class Sales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private String id;

    @Column (name = "date")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type")
    private SalesType salesType;

    @Column(name = "company_to")
    private String companyTo;

    @Column(name = "company_from")
    private String companyFrom;

    @Column(name = "product")
    private String product;

    @Column(name = "quantity")
    private Double quantity;

    @Column(name = "price")
    private Double price;

    @Column(name = "payment_term")
    private String payment;

    @Column(name = "delivery_term")
    private String deliveryTerm;

    @Column(name = "port")
    private String port;

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
}
