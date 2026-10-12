package com.ruleweaver.model;

public record FieldValidationRules(
        Double minimum,
        Double maximum,
        Integer minimumLength,
        Integer maximumLength,
        String pattern) {

    public FieldValidationRules {
        if (minimum != null && !Double.isFinite(minimum)
                || maximum != null && !Double.isFinite(maximum)) {
            throw new IllegalArgumentException("Field numeric bounds must be finite.");
        }
        if (minimum != null && maximum != null && minimum > maximum) {
            throw new IllegalArgumentException("Field minimum cannot be greater than its maximum.");
        }
        if (minimumLength != null && minimumLength < 0
                || maximumLength != null && maximumLength < 0
                || minimumLength != null && maximumLength != null && minimumLength > maximumLength) {
            throw new IllegalArgumentException("Field length limits are invalid.");
        }
        if (pattern != null) {
            try {
                java.util.regex.Pattern.compile(pattern);
            } catch (java.util.regex.PatternSyntaxException exception) {
                throw new IllegalArgumentException("Field pattern is not a valid regular expression.", exception);
            }
        }
    }

    public static FieldValidationRules none() {
        return new FieldValidationRules(null, null, null, null, null);
    }
}
