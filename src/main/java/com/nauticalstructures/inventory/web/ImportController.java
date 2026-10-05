package com.nauticalstructures.inventory.web;

import com.nauticalstructures.inventory.service.ImportResult;
import com.nauticalstructures.inventory.service.SpreadsheetImportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/** Import Spreadsheet Data (Admin only). */
@Controller
@RequestMapping("/import")
public class ImportController {

    private final SpreadsheetImportService importer;

    public ImportController(SpreadsheetImportService importer) { this.importer = importer; }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("logs", importer.history());
        return "import";
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, Authentication auth, Model model) throws IOException {
        if (file.isEmpty()) {
            model.addAttribute("error", "Choose a CSV file to upload");
        } else {
            try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
                ImportResult result = importer.importCsv(file.getOriginalFilename(), reader, CurrentStaff.id(auth));
                model.addAttribute("result", result);
            }
        }
        model.addAttribute("logs", importer.history());
        return "import";
    }
}
