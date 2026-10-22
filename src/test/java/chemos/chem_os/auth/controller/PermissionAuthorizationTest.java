package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.dto.PermissionOverrideRequest;
import chemos.chem_os.auth.model.UserPermissionRestriction.OverrideEffect;
import chemos.chem_os.auth.service.PermissionOverrideService;
import chemos.chem_os.auth.service.RolePermissionService;
import chemos.chem_os.controller.PurchaseController;
import chemos.chem_os.controller.SalesController;
import chemos.chem_os.services.PurchaseService;
import chemos.chem_os.services.SalesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionAuthorizationTest {
    AnnotationConfigApplicationContext context;
    PurchaseService purchases;
    SalesService sales;
    PermissionOverrideService overrides;
    RolePermissionService roles;

    @Configuration
    @EnableMethodSecurity
    static class MethodSecurity {}

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        purchases = mock(PurchaseService.class);
        sales = mock(SalesService.class);
        overrides = mock(PermissionOverrideService.class);
        roles = mock(RolePermissionService.class);
        context.register(MethodSecurity.class, PurchaseController.class, SalesController.class,
                RolePermissionController.class, PermissionOverrideController.class);
        context.registerBean(PurchaseService.class, () -> purchases);
        context.registerBean(SalesService.class, () -> sales);
        context.registerBean(PermissionOverrideService.class, () -> overrides);
        context.registerBean(RolePermissionService.class, () -> roles);
        context.refresh();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"confirmed-order", "unconfirmed-order"})
    void viewAndCreateCannotEditOrApproveAnyPurchase(String orderId) {
        authenticate("PURCHASE_VIEW", "PURCHASE_CREATE");
        PurchaseController controller = context.getBean(PurchaseController.class);
        assertThatThrownBy(() -> controller.updatePurchase(orderId, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.confirmPurchase(orderId)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(purchases);
    }

    @ParameterizedTest
    @ValueSource(strings = {"confirmed-order", "unconfirmed-order"})
    void sameRulesApplyToSales(String orderId) {
        authenticate("SALE_VIEW", "SALE_CREATE");
        SalesController controller = context.getBean(SalesController.class);
        assertThatThrownBy(() -> controller.updateSale(orderId, null)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.confirmSale(orderId)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(sales);
    }

    @Test
    void holdingBusinessPermissionDoesNotAllowChangingOverrides() {
        authenticate("PURCHASE_EDIT");
        PermissionOverrideController controller = context.getBean(PermissionOverrideController.class);
        assertThatThrownBy(() -> controller.setOverride("other", "PURCHASE_EDIT",
                new PermissionOverrideRequest(OverrideEffect.ALLOW, null))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.removeOverride("other", "PURCHASE_EDIT"))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.listOverrides("other")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> context.getBean(RolePermissionController.class).listPermissions())
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> context.getBean(RolePermissionController.class).updateRolePermissions("pur_exe", null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> context.getBean(RolePermissionController.class).getUserPermissions("other"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(overrides, roles);
    }

    @Test
    void roleManagementCanAdministerPermissionsNotHeldPersonally() {
        authenticate("ROLE_MANAGEMENT");
        PermissionOverrideRequest request = new PermissionOverrideRequest(OverrideEffect.ALLOW, "Approved");
        context.getBean(PermissionOverrideController.class).setOverride("other", "PURCHASE_EDIT", request);
        context.getBean(RolePermissionController.class).listPermissions();
        verify(overrides).setOverride("other", "PURCHASE_EDIT", request);
        verify(roles).listPermissions();
    }

    @Test
    void explicitEditPermissionAllowsEdit() {
        authenticate("PURCHASE_EDIT", "SALE_EDIT");
        context.getBean(PurchaseController.class).updatePurchase("order", null);
        context.getBean(SalesController.class).updateSale("order", null);
        verify(purchases).updatePurchase("order", null);
        verify(sales).updateSale("order", null);
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-user", null, AuthorityUtils.createAuthorityList(authorities)));
    }
}