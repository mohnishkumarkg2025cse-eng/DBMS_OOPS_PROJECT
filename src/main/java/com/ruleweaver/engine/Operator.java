package com.ruleweaver.engine;

/**
 * Enumeration of supported comparison operators for numeric and attribute conditions.
 */
public enum Operator {
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    EQUALS("=="),
    NOT_EQUALS("!=");

    private final String symbol;

    Operator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static Operator fromString(String symbol) {
        if (symbol == null) {
            throw new IllegalArgumentException("Operator symbol cannot be null");
        }
        String clean = symbol.trim();
        if (clean.equals("=")) {
            return EQUALS;
        }
        for (Operator op : values()) {
            if (op.symbol.equalsIgnoreCase(clean)) {
                return op;
            }
        }
        throw new IllegalArgumentException("Unknown operator symbol: " + symbol);
    }

    public boolean apply(double actual, double threshold) {
        switch (this) {
            case GREATER_THAN_OR_EQUAL:
                return actual >= threshold;
            case LESS_THAN_OR_EQUAL:
                return actual <= threshold;
            case GREATER_THAN:
                return actual > threshold;
            case LESS_THAN:
                return actual < threshold;
            case EQUALS:
                return Math.abs(actual - threshold) < 0.000001;
            case NOT_EQUALS:
                return Math.abs(actual - threshold) >= 0.000001;
            default:
                throw new UnsupportedOperationException("Unsupported operator: " + this);
        }
    }
}
