package com.nauticalstructures.inventory.service;

/** A well-formed request that breaks an inventory rule, e.g. checking out more than is on hand. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
