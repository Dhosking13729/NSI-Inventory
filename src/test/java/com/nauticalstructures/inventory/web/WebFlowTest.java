package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.InventoryTransactionRepository;
import com.nauticalstructures.inventory.repository.MaterialRepository;
import com.nauticalstructures.inventory.service.MaterialService;
import com.nauticalstructures.inventory.service.StaffService;
import com.nauticalstructures.inventory.service.StaffUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests of the Version 1 screens through the full Spring context, security and database. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebFlowTest {

    @Autowired MockMvc mvc;
    @Autowired StaffService staffService;
    @Autowired MaterialService materialService;
    @Autowired MaterialRepository materials;
    @Autowired InventoryTransactionRepository transactions;

    StaffUser stockroom, purchasing, admin;
    Material bolt;

    @BeforeEach
    void data() {
        stockroom = staffService.create("Sam Stockroom", StaffRole.STOCKROOM, "sam.web", "a-long-password-1");
        purchasing = staffService.create("Pat Purchasing", StaffRole.PURCHASING, "pat.web", "a-long-password-1");
        admin = staffService.create("Ivy Manager", StaffRole.ADMIN, "ivy.web", "a-long-password-1");
        bolt = materialService.save(null, "3/8\" Marine-Grade Stainless Bolt", MaterialType.FASTENER, "each",
                new BigDecimal("20"), new BigDecimal("5"), "NSI-FST-0038");
    }

    private static RequestPostProcessor as(StaffUser u) { return user(new StaffUserDetails(u)); }

    @Test
    void checkOutByScanUpdatesStockAndRecordsTheStaffMember() throws Exception {
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "3").param("scannedCode", "nsi-fst-0038"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scan"))
                .andExpect(flash().attribute("success", containsString("Now on hand: 17")));

        assertThat(materials.findById(bolt.getMaterialId()).get().getQuantityOnHand()).isEqualByComparingTo("17");
        InventoryTransaction tx = transactions.findAll().get(0);
        assertThat(tx.getTransactionType()).isEqualTo(TransactionType.CHECK_OUT);
        assertThat(tx.getStaff().getStaffId()).isEqualTo(stockroom.getStaffId());
        assertThat(tx.getScannedCode()).isEqualTo("nsi-fst-0038");

        mvc.perform(get("/scan").with(as(stockroom)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Recent scans")))
                .andExpect(content().string(containsString("Sam Stockroom")));
    }

    @Test
    void checkInWorksAndBadScansShowAnErrorWithoutChangingStock() throws Exception {
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_IN").param("quantity", "2.5").param("scannedCode", "NSI-FST-0038"))
                .andExpect(flash().attribute("success", containsString("Check-In recorded")));
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "100").param("scannedCode", "NSI-FST-0038"))
                .andExpect(flash().attribute("error", containsString("only 22.5 on hand")));
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "1").param("scannedCode", "UNKNOWN"))
                .andExpect(flash().attribute("error", containsString("does not match")));
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "abc").param("scannedCode", "NSI-FST-0038"))
                .andExpect(flash().attribute("error", "Quantity must be a number"));
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "SOMETHING").param("quantity", "1").param("scannedCode", "NSI-FST-0038"))
                .andExpect(flash().attribute("error", "Choose Check-In or Check-Out"));

        assertThat(materials.findById(bolt.getMaterialId()).get().getQuantityOnHand()).isEqualByComparingTo("22.5");
        assertThat(transactions.count()).isEqualTo(1);
    }

    @Test
    void everyPageRequiresLogIn() throws Exception {
        for (String page : new String[]{"/", "/scan", "/materials", "/import", "/admin/users"}) {
            mvc.perform(get(page)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        }
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void rolesFollowTheUseCaseDiagram() throws Exception {
        mvc.perform(get("/import").with(as(stockroom))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(as(stockroom))).andExpect(status().isForbidden());
        mvc.perform(get("/materials/new").with(as(stockroom))).andExpect(status().isForbidden());
        mvc.perform(get("/scan").with(as(purchasing))).andExpect(status().isForbidden());
        mvc.perform(post("/scan").with(as(purchasing)).with(csrf()).param("type", "CHECK_IN")).andExpect(status().isForbidden());
        mvc.perform(get("/materials").with(as(purchasing))).andExpect(status().isOk());
        mvc.perform(get("/scan").with(as(admin))).andExpect(status().isOk());
        mvc.perform(get("/import").with(as(admin))).andExpect(status().isOk());
        mvc.perform(post("/scan").with(as(stockroom)).param("type", "CHECK_IN")).andExpect(status().isForbidden()); // no CSRF token
    }

    @Test
    void realFormLoginAndLandingPageByRole() throws Exception {
        mvc.perform(formLogin("sam.web", "a-long-password-1")).andExpect(redirectedUrl("/"));
        mvc.perform(formLogin("sam.web", "wrong-password")).andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/").with(as(stockroom))).andExpect(redirectedUrl("/scan"));
        mvc.perform(get("/").with(as(purchasing))).andExpect(redirectedUrl("/alerts"));
        mvc.perform(get("/").with(as(admin))).andExpect(redirectedUrl("/materials"));
        mvc.perform(post("/logout").with(as(stockroom)).with(csrf())).andExpect(redirectedUrl("/login?logout"));
    }

    private static org.springframework.test.web.servlet.RequestBuilder formLogin(String u, String p) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin().user(u).password(p);
    }

    @Test
    void materialPagesListDetailAddAndEdit() throws Exception {
        mvc.perform(get("/materials").with(as(purchasing)))
                .andExpect(content().string(containsString("NSI-FST-0038")));
        mvc.perform(get("/materials/" + bolt.getMaterialId()).with(as(purchasing)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Transactions")));
        mvc.perform(get("/materials/99999").with(as(purchasing))).andExpect(status().isNotFound());

        mvc.perform(get("/materials/new").with(as(admin))).andExpect(status().isOk());
        mvc.perform(post("/materials").with(as(admin)).with(csrf())
                        .param("materialName", "Marine Epoxy Primer").param("materialType", "COATING")
                        .param("unitOfMeasure", "gal").param("quantityOnHand", "7.5").param("reorderThreshold", "5")
                        .param("barcodeValue", "nsi-cot-0101"))
                .andExpect(status().is3xxRedirection());
        assertThat(materials.findByBarcodeValueIgnoreCase("NSI-COT-0101")).isPresent();

        mvc.perform(post("/materials").with(as(admin)).with(csrf())
                        .param("materialName", "Copy").param("materialType", "STEEL").param("unitOfMeasure", "ft")
                        .param("quantityOnHand", "1").param("reorderThreshold", "1").param("barcodeValue", "NSI-FST-0038"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("already used")));

        mvc.perform(get("/materials/" + bolt.getMaterialId() + "/edit").with(as(admin))).andExpect(status().isOk());
        mvc.perform(post("/materials/" + bolt.getMaterialId()).with(as(admin)).with(csrf())
                        .param("materialName", "3/8\" Marine-Grade Stainless Bolt").param("materialType", "FASTENER")
                        .param("unitOfMeasure", "each").param("quantityOnHand", "20").param("reorderThreshold", "8")
                        .param("barcodeValue", "NSI-FST-0038"))
                .andExpect(redirectedUrl("/materials/" + bolt.getMaterialId()));
        assertThat(materials.findById(bolt.getMaterialId()).get().getReorderThreshold()).isEqualByComparingTo("8");

        mvc.perform(post("/materials").with(as(admin)).with(csrf()).param("materialName", "X").param("materialType", "WOOD"))
                .andExpect(content().string(containsString("Unknown MaterialType")));
        mvc.perform(post("/materials").with(as(admin)).with(csrf()).param("materialName", "X").param("materialType", "STEEL")
                        .param("unitOfMeasure", "ft").param("quantityOnHand", "x"))
                .andExpect(content().string(containsString("must be a number")));
    }

    @Test
    void adminImportsTheSpreadsheetAndSeesTheLog() throws Exception {
        var csv = new MockMultipartFile("file", "inventory.csv", "text/csv",
                ("MaterialName,MaterialType,UnitOfMeasure,QuantityOnHand,ReorderThreshold,BarcodeValue\n"
                        + "A36 Steel Plate,Steel,sheet,9,4,NSI-STL-0250\nBad,Wood,each,1,1,NSI-X\n").getBytes());
        mvc.perform(multipart("/import").file(csv).with(as(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Completed with Errors")))
                .andExpect(content().string(containsString("Line 3")))
                .andExpect(content().string(containsString("Ivy Manager")));
        mvc.perform(multipart("/import").file(new MockMultipartFile("file", new byte[0])).with(as(admin)).with(csrf()))
                .andExpect(content().string(containsString("Choose a CSV file")));
    }

    @Test
    void adminAddsStaffUsers() throws Exception {
        mvc.perform(post("/admin/users").with(as(admin)).with(csrf())
                        .param("name", "New Hire").param("role", "STOCKROOM").param("username", "new.hire").param("password", "a-long-password-1"))
                .andExpect(redirectedUrl("/admin/users"));
        mvc.perform(get("/admin/users").with(as(admin))).andExpect(content().string(containsString("new.hire")));
        mvc.perform(post("/admin/users").with(as(admin)).with(csrf())
                        .param("name", "Dup").param("role", "STOCKROOM").param("username", "new.hire").param("password", "a-long-password-1"))
                .andExpect(content().string(containsString("already taken")));
        mvc.perform(post("/admin/users").with(as(admin)).with(csrf())
                        .param("name", "Bad role").param("role", "BOSS").param("username", "x.y").param("password", "a-long-password-1"))
                .andExpect(content().string(containsString("Choose a role")));
    }
}
