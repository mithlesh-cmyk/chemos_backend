package chemos.chem_os.auth.controller;

import chemos.chem_os.auth.service.PermissionOverrideService;
import chemos.chem_os.auth.service.RolePermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RolePermissionControllerTest {
    RolePermissionService roles;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        roles = mock(RolePermissionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RolePermissionController(
                roles, mock(PermissionOverrideService.class))).build();
    }

    @Test
    void putAcceptsCompleteRoleConfiguration() throws Exception {
        mvc.perform(put("/api/v1/auth/roles/pur_exe/permissions").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"parentRoleId":"pur_man","inheritanceMode":"SELECTED",
                         "permissions":["PURCHASE_CREATE"],"selectedInheritedPermissions":["PURCHASE_VIEW"]}
                        """)).andExpect(status().isOk());
        verify(roles).updatePermissions(eq("pur_exe"), argThat(request ->
                request.permissions().contains("PURCHASE_CREATE")
                        && request.selectedInheritedPermissions().contains("PURCHASE_VIEW")));
    }

    @Test
    void missingFieldsAndInvalidModeAreRejectedBeforeService() throws Exception {
        for (String body : new String[] {
                "{}", "{\"inheritanceMode\":\"TYPO\",\"permissions\":[],\"selectedInheritedPermissions\":[]}",
                "{\"inheritanceMode\":\"NONE\",\"permissions\":[\"\"],\"selectedInheritedPermissions\":[]}"
        }) {
            mvc.perform(put("/api/v1/auth/roles/pur_exe/permissions").contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(roles);
    }

    @Test
    void userPermissionDetailsAreMappedByUsername() throws Exception {
        PermissionOverrideService overrides = mock(PermissionOverrideService.class);
        MockMvc userMvc = MockMvcBuilders.standaloneSetup(new RolePermissionController(roles, overrides)).build();
        userMvc.perform(get("/api/v1/auth/users/daisy10/permissions")).andExpect(status().isOk());
        verify(overrides).getPermissions("daisy10");
    }
}