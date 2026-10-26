package chemos.chem_os.controller;

import chemos.chem_os.model.VesselDishPortSummary;
import chemos.chem_os.model.Products;
import chemos.chem_os.model.Ports;
import chemos.chem_os.repository.VesselDishPortSummaryRepository;
import chemos.chem_os.services.VesselDishPortSummaryService;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VesselDishPortSummaryControllerTest {

    private SessionFactory factory;

    @BeforeEach
    void setUp() {
        factory = new Configuration()
                .addAnnotatedClass(VesselDishPortSummary.class)
                .addAnnotatedClass(Products.class)
                .addAnnotatedClass(Ports.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url",
                        "jdbc:h2:mem:vessel_summary_" + UUID.randomUUID() + ";MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "none")
                .buildSessionFactory();
        factory.inTransaction(session -> session.doWork(connection -> {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        CREATE TABLE public.products (
                            id VARCHAR(255) PRIMARY KEY, name VARCHAR(255) NOT NULL,
                            hs_code VARCHAR(255), cas_no VARCHAR(255)
                        )
                        """);
                statement.execute("""
                        CREATE TABLE public.ports (
                            id VARCHAR(255) PRIMARY KEY, display_name VARCHAR(255) NOT NULL,
                            search_key VARCHAR(255) NOT NULL UNIQUE, locode VARCHAR(255),
                            is_indian BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP, updated_at TIMESTAMP
                        )
                        """);
                statement.execute("""
                        CREATE TABLE public.vessel_dish_port_summary_csv (
                            id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            product VARCHAR(255),
                            "vessel name" VARCHAR(255),
                            "dish port clean" VARCHAR(255),
                            "total qty" NUMERIC(18,3),
                            "shipment count" INTEGER,
                            "avg waiting days" NUMERIC(10,2),
                            "avg berth days" NUMERIC(10,2),
                            "dish port share" NUMERIC(8,4),
                            "ship id" BIGINT,
                            "product id" VARCHAR(255),
                            "port id" VARCHAR(50),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO public.products (id, name) VALUES ('PROD-1', 'Methanol')
                        """);
                statement.executeUpdate("""
                        INSERT INTO public.ports (id, display_name, search_key)
                        VALUES ('PORT-1', 'Kandla', 'kandla')
                        """);
                statement.execute("""
                        ALTER TABLE public.vessel_dish_port_summary_csv
                            ADD CONSTRAINT fk_vessel_dish_port_summary_product
                                FOREIGN KEY ("product id") REFERENCES public.products(id)
                                ON DELETE NO ACTION ON UPDATE NO ACTION
                        """);
                statement.execute("""
                        ALTER TABLE public.vessel_dish_port_summary_csv
                            ADD CONSTRAINT fk_vessel_dish_port_summary_port
                                FOREIGN KEY ("port id") REFERENCES public.ports(id)
                                ON DELETE NO ACTION ON UPDATE NO ACTION
                        """);
            }
        }));
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void returnsAllColumnsAndRowsIncludingNullsFromExistingTable() throws Exception {
        factory.inTransaction(session -> session.doWork(connection -> {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv
                        (product, "vessel name", "dish port clean", "total qty", "shipment count",
                         "avg waiting days", "avg berth days", "dish port share", "ship id",
                         "product id", "port id", created_at)
                        VALUES ('Methanol', 'Example Vessel', 'Kandla', 12345.678, 3,
                                2.25, 4.50, 0.3750, 9876543210,
                                'PROD-1', 'PORT-1', TIMESTAMP '2026-10-26 12:00:00')
                        """);
                statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv (product, created_at)
                        VALUES (NULL, NULL)
                        """);
            }
        }));

        try (var session = factory.openSession()) {
            var summary = session.find(VesselDishPortSummary.class, 1L);
            assertThat(summary.getProductReference().getName()).isEqualTo("Methanol");
            assertThat(summary.getPortReference().getDisplayName()).isEqualTo("Kandla");
            var nullableSummary = session.find(VesselDishPortSummary.class, 2L);
            assertThat(nullableSummary.getProductReference()).isNull();
            assertThat(nullableSummary.getPortReference()).isNull();
            var repository = new JpaRepositoryFactory(session)
                    .getRepository(VesselDishPortSummaryRepository.class);
            var mvc = MockMvcBuilders.standaloneSetup(new VesselDishPortSummaryController(
                    new VesselDishPortSummaryService(repository))).build();

            mvc.perform(get("/api/v1/vessel-dish-port-summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].*", hasSize(13)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].product").value("Methanol"))
                    .andExpect(jsonPath("$[0].vessel_name").value("Example Vessel"))
                    .andExpect(jsonPath("$[0].dish_port_clean").value("Kandla"))
                    .andExpect(jsonPath("$[0].total_qty").value(12345.678))
                    .andExpect(jsonPath("$[0].shipment_count").value(3))
                    .andExpect(jsonPath("$[0].avg_waiting_days").value(2.25))
                    .andExpect(jsonPath("$[0].avg_berth_days").value(4.50))
                    .andExpect(jsonPath("$[0].dish_port_share").value(0.3750))
                    .andExpect(jsonPath("$[0].ship_id").value(9876543210L))
                    .andExpect(jsonPath("$[0].product_id").value("PROD-1"))
                    .andExpect(jsonPath("$[0].port_id").value("PORT-1"))
                    .andExpect(jsonPath("$[0].created_at").value("2026-10-26T12:00:00"))
                    .andExpect(jsonPath("$[1].id").value(2))
                    .andExpect(jsonPath("$[1].*", hasSize(13)))
                    .andExpect(jsonPath("$[1].product").value(nullValue()))
                    .andExpect(jsonPath("$[1].total_qty").value(nullValue()))
                    .andExpect(jsonPath("$[1].created_at").value(nullValue()));
        }
    }

    @Test
    void foreignKeysRejectUnknownMetadataAndDeletionOfReferencedMetadata() {
        factory.inTransaction(session -> session.doWork(connection -> {
            try (var statement = connection.createStatement()) {
                assertThatThrownBy(() -> statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv ("product id")
                        VALUES ('UNKNOWN')
                        """)).isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv ("port id")
                        VALUES ('UNKNOWN')
                        """)).isInstanceOf(SQLException.class);
                statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv ("product id", "port id")
                        VALUES ('PROD-1', 'PORT-1')
                        """);
                assertThatThrownBy(() -> statement.executeUpdate(
                        "DELETE FROM public.products WHERE id = 'PROD-1'"))
                        .isInstanceOf(SQLException.class);
                assertThatThrownBy(() -> statement.executeUpdate(
                        "DELETE FROM public.ports WHERE id = 'PORT-1'"))
                        .isInstanceOf(SQLException.class);
            }
        }));
    }

    @Test
    void filtersByExactProductIdAndKeepsAllRowsWhenOmitted() throws Exception {
        factory.inTransaction(session -> session.doWork(connection -> {
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        INSERT INTO public.products (id, name) VALUES ('PROD-2', 'Methanol')
                        """);
                statement.executeUpdate("""
                        INSERT INTO public.vessel_dish_port_summary_csv (product, "product id")
                        VALUES ('Methanol', 'PROD-1'), ('Methanol', 'PROD-2'),
                               ('Methanol', 'PROD-1'), ('Methanol', NULL)
                        """);
            }
        }));
        try (var session = factory.openSession()) {
            var repository = new JpaRepositoryFactory(session)
                    .getRepository(VesselDishPortSummaryRepository.class);
            var mvc = MockMvcBuilders.standaloneSetup(new VesselDishPortSummaryController(
                    new VesselDishPortSummaryService(repository))).build();

            mvc.perform(get("/api/v1/vessel-dish-port-summary").param("product_id", "PROD-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[1].id").value(3))
                    .andExpect(jsonPath("$[0].product_id").value("PROD-1"))
                    .andExpect(jsonPath("$[1].product_id").value("PROD-1"));
            mvc.perform(get("/api/v1/vessel-dish-port-summary").param("product_id", "PROD-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(2));
            mvc.perform(get("/api/v1/vessel-dish-port-summary").param("product_id", "UNKNOWN"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
            mvc.perform(get("/api/v1/vessel-dish-port-summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)));
        }
    }

    @Test
    void rejectsBlankAndOversizedProductIds() throws Exception {
        try (var session = factory.openSession()) {
            var repository = new JpaRepositoryFactory(session)
                    .getRepository(VesselDishPortSummaryRepository.class);
            var mvc = MockMvcBuilders.standaloneSetup(new VesselDishPortSummaryController(
                    new VesselDishPortSummaryService(repository))).build();

            for (String invalidId : new String[]{"", "   ", "x".repeat(256)}) {
                mvc.perform(get("/api/v1/vessel-dish-port-summary").param("product_id", invalidId))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Test
    void returnsEmptyArrayWhenTableIsEmpty() throws Exception {
        try (var session = factory.openSession()) {
            var repository = new JpaRepositoryFactory(session)
                    .getRepository(VesselDishPortSummaryRepository.class);
            var mvc = MockMvcBuilders.standaloneSetup(new VesselDishPortSummaryController(
                    new VesselDishPortSummaryService(repository))).build();

            mvc.perform(get("/api/v1/vessel-dish-port-summary"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("[]"));
        }
    }
}
