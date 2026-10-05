package com.nauticalstructures.inventory.domain;

import jakarta.persistence.AttributeConverter;

public abstract class LabelConverter<E extends Enum<E> & Labeled> implements AttributeConverter<E, String> {
    private final Class<E> type;
    protected LabelConverter(Class<E> type) { this.type = type; }

    @Override public String convertToDatabaseColumn(E value) { return value == null ? null : value.label(); }
    @Override public E convertToEntityAttribute(String db) { return db == null ? null : Labeled.parse(type, db); }
}
