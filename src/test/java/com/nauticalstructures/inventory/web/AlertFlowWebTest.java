package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.*;
import com.nauticalstructures.inventory.repository.LowStockAlertRepository;
import com.nauticalstructures.inventory.repository.ReorderRequestRepository;
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

/** End-to-end Version 2 flows: alert engine, alerts screen, reorder-requests view, thresholds, roles. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlertFlowWebTest {

    @Autowired MockMvc mvc;
    @Autowired StaffService staffService;
    @Autowired MaterialService materialService;
    @Autowired LowStockAlertRepository alerts;
    @Autowired ReorderRequestRepository reorders;

    StaffUser stockroom, purchasing, admin;
    Material paint;

    @BeforeEach
    void data() {
        stockroom = staffService.create("Sam Stockroom", StaffRole.STOCKROOM, "sam.v2", "a-long-password-1");
        purchasing = staffService.create("Pat Purchasing", StaffRole.PURCHASING, "pat.v2", "a-long-password-1");
        admin = staffService.create("Ivy Manager", StaffRole.ADMIN, "ivy.v2", "a-long-password-1");
        paint = materialService.save(null, "Antifouling Bottom Paint", MaterialType.COATING, "gal",
                new BigDecimal("6"), new BigDecimal("4"), "NSI-COT-0202");
    }

    private static RequestPostProcessor as(StaffUser u) { return user(new StaffUserDetails(u)); }

    private void checkOut(String qty) throws Exception {
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                .param("type", "CHECK_OUT").param("quantity", qty).param("scannedCode", "NSI-COT-0202"));
    }

    @Test
    void checkOutBelowThresholdAlertsPurchasingAndCreatesAReorderRequest() throws Exception {
        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "2").param("scannedCode", "NSI-COT-0202"))
                .andExpect(flash().attribute("success", containsString("Now on hand: 4")))
                .andExpect(flash().attributeCount(2));                       // type + success: 4 is not below 4
        assertThat(alerts.count()).isZero();

        mvc.perform(post("/scan").with(as(stockroom)).with(csrf())
                        .param("type", "CHECK_OUT").param("quantity", "3").param("scannedCode", "NSI-COT-0202"))
                .andExpect(flash().attribute("warning", containsString("Purchasing has been alerted")));

        LowStockAlert a = alerts.findAll().get(0);
        assertThat(a.getStatus()).isEqualTo(AlertStatus.OPEN);
        assertThat(a.getNotifiedStaff().getStaffId()).isEqualTo(purchasing.getStaffId());
        assertThat(a.getTriggeredThreshold()).isEqualByComparingTo("4");
        ReorderRequest r = reorders.findByAlertAlertId(a.getAlertId()).orElseThrow();
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(r.getRequestedQuantity()).isEqualByComparingTo("7");   // 2 x 4 - 1

        checkOut("1");                                                      // still below: no second alert
        assertThat(alerts.count()).isEqualTo(1);

        mvc.perform(get("/materials").with(as(purchasing)))
                .andExpect(content().string(containsString("class=\"badge\">1<")));
        mvc.perform(get("/alerts").with(as(purchasing)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Antifouling Bottom Paint")))
                .andExpect(content().string(containsString("Pat Purchasing")));
    }

    @Test
    void purchasingAcknowledgesAndResolvesAlerts() throws Exception {
        checkOut("5");
        Integer id = alerts.findAll().get(0).getAlertId();

        mvc.perform(post("/alerts/" + id + "/status").with(as(purchasing)).with(csrf()).param("status", "ACKNOWLEDGED"))
                .andExpect(redirectedUrl("/alerts")).andExpect(flash().attribute("success", containsString("Acknowledged")));
        mvc.perform(post("/alerts/" + id + "/status").with(as(purchasing)).with(csrf()).param("status", "OPEN"))
                .andExpect(flash().attribute("error", containsString("can't be changed")));
        mvc.perform(post("/alerts/" + id + "/status").with(as(purchasing)).with(csrf()).param("status", "RESOLVED"))
                .andExpect(flash().attribute("success", containsString("Resolved")));
        mvc.perform(post("/alerts/" + id + "/status").with(as(purchasing)).with(csrf()).param("status", "BOGUS"))
                .andExpect(flash().attribute("error", "Choose Acknowledged or Resolved"));
        mvc.perform(post("/alerts/9999/status").with(as(purchasing)).with(csrf()).param("status", "RESOLVED"))
                .andExpect(flash().attribute("error", containsString("No alert")));
        assertThat(alerts.findById(id).get().getStatus()).isEqualTo(AlertStatus.RESOLVED);

        checkOut("1");                                                      // a resolved alert allows a new one
        assertThat(alerts.count()).isEqualTo(2);
    }

    @Test
    void reorderRequestsMoveThroughTheirStatusesAndRecordTheReviewer() throws Exception {
        checkOut("5");
        Integer rid = reorders.findAll().get(0).getRequestId();

        mvc.perform(get("/reorders").with(as(purchasing))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Alert #")));
        mvc.perform(post("/reorders/" + rid + "/status").with(as(purchasing)).with(csrf()).param("status", "ORDERED"))
                .andExpect(flash().attribute("success", containsString("Ordered")));
        mvc.perform(post("/reorders/" + rid + "/status").with(as(purchasing)).with(csrf()).param("status", "PENDING"))
                .andExpect(flash().attribute("error", containsString("can't be changed")));
        mvc.perform(post("/reorders/" + rid + "/status").with(as(purchasing)).with(csrf()).param("status", "CLOSED"))
                .andExpect(flash().attribute("success", containsString("Closed")));
        mvc.perform(post("/reorders/" + rid + "/status").with(as(purchasing)).with(csrf()).param("status", "x"))
                .andExpect(flash().attribute("error", "Choose Ordered or Closed"));
        ReorderRequest r = reorders.findById(rid).orElseThrow();
        assertThat(r.getRequestStatus()).isEqualTo(RequestStatus.CLOSED);
        assertThat(r.getReviewedBy().getStaffId()).isEqualTo(purchasing.getStaffId());
    }

    @Test
    void purchasingCreatesManualRequests() throws Exception {
        mvc.perform(post("/reorders").with(as(purchasing)).with(csrf())
                        .param("materialId", paint.getMaterialId().toString()).param("quantity", "12.5"))
                .andExpect(flash().attribute("success", containsString("created for Antifouling")));
        assertThat(reorders.findAll()).singleElement().satisfies(r -> assertThat(r.getAlert()).isNull());

        mvc.perform(post("/reorders").with(as(purchasing)).with(csrf()).param("quantity", "1"))
                .andExpect(flash().attribute("error", "Choose a material"));
        mvc.perform(post("/reorders").with(as(purchasing)).with(csrf())
                        .param("materialId", paint.getMaterialId().toString()).param("quantity", "lots"))
                .andExpect(flash().attribute("error", "Quantity must be a number"));
    }

    @Test
    void purchasingConfiguresTheThresholdAndAHigherOneAlertsImmediately() throws Exception {
        mvc.perform(post("/materials/" + paint.getMaterialId() + "/threshold").with(as(purchasing)).with(csrf())
                        .param("reorderThreshold", "5"))
                .andExpect(flash().attribute("success", containsString("updated to 5")));
        assertThat(alerts.count()).isZero();                                // 6 on hand is not below 5

        mvc.perform(post("/materials/" + paint.getMaterialId() + "/threshold").with(as(purchasing)).with(csrf())
                        .param("reorderThreshold", "10"))
                .andExpect(flash().attribute("warning", containsString("below the new threshold")));
        assertThat(alerts.count()).isEqualTo(1);

        mvc.perform(post("/materials/" + paint.getMaterialId() + "/threshold").with(as(purchasing)).with(csrf())
                        .param("reorderThreshold", "-1"))
                .andExpect(flash().attribute("error", containsString("cannot be negative")));
        mvc.perform(post("/materials/" + paint.getMaterialId() + "/threshold").with(as(purchasing)).with(csrf())
                        .param("reorderThreshold", "ten"))
                .andExpect(flash().attribute("error", containsString("must be a number")));
        mvc.perform(get("/materials/" + paint.getMaterialId()).with(as(purchasing)))
                .andExpect(content().string(containsString("Update threshold")))
                .andExpect(content().string(containsString("Low-stock alerts")));
    }

    @Test
    void stockroomStaffCannotReachPurchasingScreens() throws Exception {
        mvc.perform(get("/alerts").with(as(stockroom))).andExpect(status().isForbidden());
        mvc.perform(get("/reorders").with(as(stockroom))).andExpect(status().isForbidden());
        mvc.perform(post("/materials/" + paint.getMaterialId() + "/threshold").with(as(stockroom)).with(csrf())
                .param("reorderThreshold", "1")).andExpect(status().isForbidden());
        mvc.perform(get("/materials/" + paint.getMaterialId()).with(as(stockroom)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Update threshold"))));
        mvc.perform(get("/alerts").with(as(admin))).andExpect(status().isOk());
    }

    @Test
    void forbiddenPageOffersAWayBack() throws Exception {
        org.springframework.test.web.servlet.MvcResult denied = mvc.perform(get("/reorders").with(as(stockroom)))
                .andExpect(status().isForbidden()).andReturn();
        mvc.perform(get("/error").with(as(stockroom)).accept(org.springframework.http.MediaType.TEXT_HTML)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_STATUS_CODE, 403)
                        .requestAttr(jakarta.servlet.RequestDispatcher.ERROR_REQUEST_URI, "/reorders"))
                .andExpect(content().string(containsString("isn't available to your role")));
        assertThat(denied.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void importedStockAlreadyBelowThresholdRaisesAlerts() throws Exception {
        var csv = new MockMultipartFile("file", "inventory.csv", "text/csv",
                ("MaterialName,MaterialType,UnitOfMeasure,QuantityOnHand,ReorderThreshold,BarcodeValue\n"
                        + "Argon Cylinder,Consumable,each,1,2,NSI-CON-0075\nBolt,Fastener,each,500,150,NSI-FST-0038\n").getBytes());
        mvc.perform(multipart("/import").file(csv).with(as(admin)).with(csrf()))
                .andExpect(content().string(containsString("1 material(s) are below their reorder threshold")));
        assertThat(alerts.findAll()).singleElement()
                .satisfies(a -> assertThat(a.getMaterial().getBarcodeValue()).isEqualTo("NSI-CON-0075"));
    }

    @Test
    void adminEditThatDropsStockBelowThresholdAlsoAlerts() throws Exception {
        mvc.perform(post("/materials/" + paint.getMaterialId()).with(as(admin)).with(csrf())
                        .param("materialName", "Antifouling Bottom Paint").param("materialType", "COATING")
                        .param("unitOfMeasure", "gal").param("quantityOnHand", "2").param("reorderThreshold", "4")
                        .param("barcodeValue", "NSI-COT-0202"))
                .andExpect(status().is3xxRedirection());
        assertThat(alerts.count()).isEqualTo(1);
    }
}
