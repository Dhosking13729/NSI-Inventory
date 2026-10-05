package com.nauticalstructures.inventory.domain;

/** ImportLog.ImportStatus - "Completed", "Completed with Errors" or "Failed". */
public enum ImportStatus implements Labeled {
    COMPLETED("Completed"), COMPLETED_WITH_ERRORS("Completed with Errors"), FAILED("Failed");

    private final String label;
    ImportStatus(String label) { this.label = label; }
    @Override public String label() { return label; }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<ImportStatus> {
        public Converter() { super(ImportStatus.class); }
    }
}
