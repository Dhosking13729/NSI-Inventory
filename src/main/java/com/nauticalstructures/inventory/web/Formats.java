package com.nauticalstructures.inventory.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Display helpers used by the page templates as ${@fmt.qty(...)} and ${@fmt.time(...)}. */
@Component("fmt")
public class Formats {
    private final DateTimeFormatter time;

    public Formats(@Value("${nsi.time-zone:America/New_York}") String zone) {
        this.time = DateTimeFormatter.ofPattern("M/d/yyyy h:mm a").withZone(ZoneId.of(zone));
    }

    public String qty(BigDecimal value) { return value == null ? "" : value.stripTrailingZeros().toPlainString(); }

    public String time(Instant value) { return value == null ? "" : time.format(value); }
}
