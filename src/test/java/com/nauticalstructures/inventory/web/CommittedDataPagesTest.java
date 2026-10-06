package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.domain.MaterialType;
import com.nauticalstructures.inventory.domain.StaffRole;
import com.nauticalstructures.inventory.domain.StaffUser;
import com.nauticalstructures.inventory.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Not @Transactional on purpose: data is committed and each page renders after the database session has closed,
 * exactly as in a deployed environment. This catches lazy-loading errors that transactional tests hide.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommittedDataPagesTest {

    @Autowired MockMvc mvc;
    @Autowired StaffService staff;
    @Autowired MaterialService materials;
    @Autowired InventoryService inventory;
    @Autowired SpreadsheetImportService importer;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM reorder_request");
        jdbc.update("DELETE FROM low_stock_alert");
        jdbc.update("DELETE FROM inventory_transaction");
        jdbc.update("DELETE FROM import_log");
        jdbc.update("DELETE FROM material");
        jdbc.update("DELETE FROM staff_user");
    }

    @Test
    void pagesRenderCommittedRelationships() throws Exception {
        StaffUser admin = staff.create("Ivy Manager", StaffRole.ADMIN, "ivy.commit", "a-long-password-1");
        var m = materials.save(null, "Antifouling Bottom Paint", MaterialType.COATING, "gal",
                new BigDecimal("3"), new BigDecimal("4"), "NSI-COT-0202");
        inventory.record(com.nauticalstructures.inventory.domain.TransactionType.CHECK_OUT, "NSI-COT-0202", BigDecimal.ONE, admin.getStaffId());
        importer.importCsv("x.csv", new StringReader("MaterialName,MaterialType,UnitOfMeasure,QuantityOnHand,ReorderThreshold,BarcodeValue\nBolt,Fastener,each,1,1,NSI-B\n"), admin.getStaffId());

        var as = user(new StaffUserDetails(admin));
        mvc.perform(get("/scan").with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("Antifouling")));
        mvc.perform(get("/materials/" + m.getMaterialId()).with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("Ivy Manager")));
        mvc.perform(get("/import").with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("x.csv")));
        mvc.perform(get("/materials").with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("class=\"low\"")));
        // Version 2 screens: the check-out above dropped the paint below its threshold of 4
        mvc.perform(get("/alerts").with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("Antifouling")))
                .andExpect(content().string(containsString("Ivy Manager")));
        mvc.perform(get("/reorders").with(as)).andExpect(status().isOk()).andExpect(content().string(containsString("Alert #")));
    }
}
