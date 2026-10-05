package com.nauticalstructures.inventory.domain;

/** Material.MaterialType - the five categories in the Submission 3 data dictionary. */
public enum MaterialType implements Labeled {
    STEEL("Steel"), ALUMINUM("Aluminum"), FASTENER("Fastener"), COATING("Coating"), CONSUMABLE("Consumable");

    private final String label;
    MaterialType(String label) { this.label = label; }
    @Override public String label() { return label; }

    public static MaterialType fromLabel(String value) { return Labeled.parse(MaterialType.class, value); }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter extends LabelConverter<MaterialType> {
        public Converter() { super(MaterialType.class); }
    }
}
