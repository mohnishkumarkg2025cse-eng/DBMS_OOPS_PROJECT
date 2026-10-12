package com.ruleweaver.model;

public enum FieldOperator {
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    CONTAINS("contains"),
    NOT_CONTAINS("not contains"),
    CONTAINS_ANY("contains any"),
    CONTAINS_ALL("contains all");

    private final String symbol;

    FieldOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
