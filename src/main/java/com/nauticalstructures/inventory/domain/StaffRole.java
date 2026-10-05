package com.nauticalstructures.inventory.domain;

/** StaffUser.Role - "Stockroom", "Purchasing" or "Admin". */
public enum StaffRole implements Labeled {
    STOCKROOM("Stockroom"), PURCHASING("Purchasing"), ADMIN("Admin");

    private final String label;
    StaffRole(String label) { this.label = label; }
    @Override public String label() { return label; }

    /** Spring Security authority name, e.g. ROLE_STOCKROOM. */
    public String authority() { return "ROLE_" + name(); }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<StaffRole> {
        public Converter() { super(StaffRole.class); }
    }
}
