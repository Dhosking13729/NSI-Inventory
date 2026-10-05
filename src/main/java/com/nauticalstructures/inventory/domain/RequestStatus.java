package com.nauticalstructures.inventory.domain;

import java.util.EnumSet;
import java.util.Set;

/** ReorderRequest.RequestStatus - "Pending", "Ordered" or "Closed"; updated in the reorder-requests view. */
public enum RequestStatus implements Labeled {
    PENDING("Pending"), ORDERED("Ordered"), CLOSED("Closed");

    private final String label;
    RequestStatus(String label) { this.label = label; }
    @Override public String label() { return label; }

    /** Allowed moves: Pending -> Ordered | Closed, Ordered -> Closed. Closed is final. */
    public Set<RequestStatus> next() {
        return switch (this) {
            case PENDING -> EnumSet.of(ORDERED, CLOSED);
            case ORDERED -> EnumSet.of(CLOSED);
            case CLOSED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<RequestStatus> {
        public Converter() { super(RequestStatus.class); }
    }
}
