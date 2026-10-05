package com.nauticalstructures.inventory.service;

import com.nauticalstructures.inventory.domain.ImportLog;

import java.util.List;

public record ImportResult(ImportLog log, int created, int updated, List<String> errors, int alertsRaised) { }
