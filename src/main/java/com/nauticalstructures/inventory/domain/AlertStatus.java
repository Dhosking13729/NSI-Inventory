package com.nauticalstructures.inventory.domain;

import java.util.EnumSet;
import java.util.Set;

/** LowStockAlert.Status - "Open", "Acknowledged" or "Resolved"; updated by purchasing staff. */
public enum AlertStatus implements Labeled {
    OPEN("Open"), ACKNOWLEDGED("Acknowledged"), RESOLVED("Resolved");

    private final String label;
    AlertStatus(String label) { this.label = label; }
    @Override public String label() { return label; }

    /** Allowed moves: Open -> Acknowledged | Resolved, Acknowledged -> Resolved. Resolved is final. */
    public Set<AlertStatus> next() {
        return switch (this) {
            case OPEN -> EnumSet.of(ACKNOWLEDGED, RESOLVED);
            case ACKNOWLEDGED -> EnumSet.of(RESOLVED);
            case RESOLVED -> EnumSet.noneOf(AlertStatus.class);
        };
    }

    public boolean isActive() { return this != RESOLVED; }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<AlertStatus> {
        public Converter() { super(AlertStatus.class); }
    }
}
