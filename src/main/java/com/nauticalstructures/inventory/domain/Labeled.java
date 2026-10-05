package com.nauticalstructures.inventory.domain;

/** An enum stored in the database by its human-readable label (as written in the data dictionary). */
public interface Labeled {
    String label();

    static <E extends Enum<E> & Labeled> E parse(Class<E> type, String value) {
        if (value != null) {
            for (E e : type.getEnumConstants()) {
                if (e.label().equalsIgnoreCase(value.trim()) || e.name().equalsIgnoreCase(value.trim())) {
                    return e;
                }
            }
        }
        throw new IllegalArgumentException("'" + value + "' is not a valid " + type.getSimpleName());
    }
}
