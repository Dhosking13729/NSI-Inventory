package com.nauticalstructures.inventory.domain;

/** InventoryTransaction.TransactionType - limited to "Check-In" or "Check-Out" by a CHECK constraint. */
public enum TransactionType implements Labeled {
    CHECK_IN("Check-In"), CHECK_OUT("Check-Out");

    private final String label;
    TransactionType(String label) { this.label = label; }
    @Override public String label() { return label; }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<TransactionType> {
        public Converter() { super(TransactionType.class); }
    }
}
