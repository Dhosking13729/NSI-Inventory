package com.nauticalstructures.inventory.service;

import java.math.BigDecimal;

/** Quantities are DECIMAL(10,2): at most 8 digits before the point and 2 after. */
final class Quantities {
    static final BigDecimal MAX = new BigDecimal("99999999.99");
    private Quantities() { }

    static BigDecimal positive(BigDecimal q, String field) {
        if (q == null || q.signum() <= 0) throw new BusinessRuleException(field + " must be greater than 0");
        return check(q, field);
    }

    static BigDecimal nonNegative(BigDecimal q, String field) {
        if (q == null || q.signum() < 0) throw new BusinessRuleException(field + " cannot be negative");
        return check(q, field);
    }

    private static BigDecimal check(BigDecimal q, String field) {
        if (q.stripTrailingZeros().scale() > 2) throw new BusinessRuleException(field + " can have at most 2 decimal places");
        if (q.compareTo(MAX) > 0) throw new BusinessRuleException(field + " is too large");
        return q.setScale(2);
    }
}
