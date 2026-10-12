package com.ruleweaver.service;

import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.ConditionDocumentMapper;
import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.ProgramConfiguration;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * Runs a threshold experiment against a cloned policy version without persisting results.
 */
public final class WhatIfSimulator {
    private final DecisionEngine decisionEngine;

    public WhatIfSimulator(DecisionEngine decisionEngine) {
        if (decisionEngine == null) {
            throw new IllegalArgumentException("Decision engine is required.");
        }
        this.decisionEngine = decisionEngine;
    }

    public List<NumericRule> findNumericRules(PolicyVersion version) {
        return findNumericRules(version, null);
    }

    public List<NumericRule> findNumericRules(PolicyVersion version, ProgramConfiguration program) {
        if (version == null) {
            return List.of();
        }
        List<NumericRule> rules = new ArrayList<>();
        for (int i = 0; i < version.getConditions().size(); i++) {
            collectNumericRules(version.getConditions().get(i), List.of(i), rules, program);
        }
        return List.copyOf(rules);
    }

    public List<BooleanRule> findBooleanRules(PolicyVersion version, ProgramConfiguration program) {
        if (version == null) return List.of();
        List<BooleanRule> rules = new ArrayList<>();
        for (int i = 0; i < version.getConditions().size(); i++) {
            collectBooleanRules(version.getConditions().get(i), List.of(i), rules, program);
        }
        return List.copyOf(rules);
    }

    public List<MultiSelectRule> findMultiSelectRules(PolicyVersion version, ProgramConfiguration program) {
        if (version == null) return List.of();
        List<MultiSelectRule> rules = new ArrayList<>();
        for (int i = 0; i < version.getConditions().size(); i++) {
            collectMultiSelectRules(version.getConditions().get(i), List.of(i), rules, program);
        }
        return List.copyOf(rules);
    }

    public SimulationResult simulate(PolicyVersion baseVersion,
                                     List<RuleAdjustment> adjustments,
                                     List<Applicant> applicants,
                                     IntConsumer progress,
                                     BooleanSupplier cancelled) {
        return simulate(baseVersion, adjustments, null, applicants, progress, cancelled);
    }

    public SimulationResult simulate(PolicyVersion baseVersion,
                                     List<RuleAdjustment> adjustments,
                                     ProgramConfiguration program,
                                     List<Applicant> applicants,
                                     IntConsumer progress,
                                     BooleanSupplier cancelled) {
        if (baseVersion == null) {
            throw new IllegalArgumentException("A base policy version is required.");
        }
        if (adjustments == null || applicants == null) {
            throw new IllegalArgumentException("Rule adjustments and applicants are required.");
        }
        IntConsumer reportProgress = progress == null ? ignored -> { } : progress;
        BooleanSupplier isCancelled = cancelled == null ? () -> false : cancelled;

        Set<List<Integer>> adjustedPaths = new HashSet<>();
        int numericAdjustments = 0;
        for (RuleAdjustment adjustment : adjustments) {
            if (adjustment == null || !adjustedPaths.add(adjustment.path())) {
                throw new IllegalArgumentException("Each rule can be adjusted only once per simulation.");
            }
            Condition target = conditionAtPath(baseVersion.getConditions(), adjustment.path());
            if (target instanceof NumericCondition) {
                numericAdjustments++;
                if (!(adjustment.value() instanceof Number threshold)
                        || !Double.isFinite(threshold.doubleValue())) {
                    throw new IllegalArgumentException("Numeric rule adjustments must be finite numbers.");
                }
                NumericRule numericRule = findNumericRules(baseVersion, program).stream()
                        .filter(rule -> rule.path().equals(adjustment.path()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                "The selected numeric rule is not compatible with the program schema."));
                double thresholdValue = ((Number) adjustment.value()).doubleValue();
                if (thresholdValue < numericRule.minimum() || thresholdValue > numericRule.maximum()) {
                    throw new IllegalArgumentException("Threshold for " + numericRule.label() + " must be between "
                            + numericRule.minimum() + " and " + numericRule.maximum() + ".");
                }
            } else if (target instanceof FieldCondition field) {
                validateFieldAdjustment(field, adjustment.value(), program);
            } else {
                throw new IllegalArgumentException(
                        "Only numeric, Boolean, and multi-select field rules can be adjusted.");
            }
        }
        if (numericAdjustments > 3) {
            throw new IllegalArgumentException("At most three numeric thresholds can be adjusted at once.");
        }

        List<Condition> simulatedConditions = cloneConditions(baseVersion.getConditions());
        for (RuleAdjustment adjustment : adjustments) {
            int rootIndex = adjustment.path().get(0);
            Condition updated = adjustedCondition(simulatedConditions.get(rootIndex),
                    adjustment.path(), 1, adjustment.value());
            simulatedConditions.set(rootIndex, updated);
        }
        PolicyVersion simulatedVersion = new PolicyVersion(
                baseVersion.getId(), baseVersion.getPolicyId(), baseVersion.getVersionNumber(),
                baseVersion.getDescription(), simulatedConditions);

        int baselinePassCount = 0;
        int simulatedPassCount = 0;
        int comparedCount = 0;
        List<ApplicantFlip> flips = new ArrayList<>();
        for (int i = 0; i < applicants.size(); i++) {
            if (isCancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                throw new CancellationException("Impact Lab simulation cancelled.");
            }
            Applicant applicant = applicants.get(i);
            if (applicant != null) {
                EvaluationResult baseline = decisionEngine.evaluate(applicant, baseVersion);
                EvaluationResult simulated = decisionEngine.evaluate(applicant, simulatedVersion);
                comparedCount++;
                if (baseline.isOverallDecision()) baselinePassCount++;
                if (simulated.isOverallDecision()) simulatedPassCount++;
                if (baseline.isOverallDecision() != simulated.isOverallDecision()) {
                    flips.add(new ApplicantFlip(applicant.getId(), applicant.getName(),
                            baseline.isOverallDecision(), simulated.isOverallDecision(),
                            explainChangedConditions(baseline, simulated)));
                }
            }
            reportProgress.accept(i + 1);
        }
        return new SimulationResult(simulatedVersion, simulatedPassCount, baselinePassCount,
                simulatedPassCount - baselinePassCount, comparedCount, flips);
    }

    public SimulationResult simulate(PolicyVersion realVersion,
                                     NumericRule selectedRule,
                                     double threshold,
                                     List<Applicant> applicants,
                                     List<EvaluationResult> storedResults) {
        if (realVersion == null || selectedRule == null) {
            throw new IllegalArgumentException("A policy version and numeric rule are required.");
        }
        if (applicants == null || storedResults == null) {
            throw new IllegalArgumentException("Applicants and stored evaluation results are required.");
        }

        List<Condition> clonedConditions = realVersion.getConditions().stream()
                .map(condition -> ConditionDocumentMapper.fromDocument(
                        ConditionDocumentMapper.toDocument(condition)))
                .toList();
        NumericCondition clonedRule = numericConditionAt(clonedConditions, selectedRule.path());
        if (!clonedRule.getFieldName().equals(selectedRule.fieldName())) {
            throw new IllegalArgumentException("The selected numeric rule does not belong to this version.");
        }
        clonedRule.setThreshold(threshold);

        PolicyVersion simulatedVersion = new PolicyVersion(
                realVersion.getId(),
                realVersion.getPolicyId(),
                realVersion.getVersionNumber(),
                realVersion.getDescription(),
                clonedConditions);

        Map<String, EvaluationResult> baselineByApplicant = new HashMap<>();
        for (EvaluationResult result : storedResults) {
            if (result.getApplicantId() != null) {
                baselineByApplicant.merge(result.getApplicantId(), result, (previous, current) -> {
                    if (previous.getEvaluatedOn() == null) {
                        return current;
                    }
                    if (current.getEvaluatedOn() == null) {
                        return previous;
                    }
                    return current.getEvaluatedOn().isAfter(previous.getEvaluatedOn())
                            ? current : previous;
                });
            }
        }

        List<ApplicantFlip> flips = new ArrayList<>();
        int simulatedPassCount = 0;
        int baselinePassCount = 0;
        int comparedCount = 0;
        for (Applicant applicant : applicants) {
            EvaluationResult baseline = applicant.getId() == null
                    ? null : baselineByApplicant.get(applicant.getId());
            if (baseline == null) {
                continue;
            }

            EvaluationResult simulated = decisionEngine.evaluate(applicant, simulatedVersion);
            comparedCount++;
            if (simulated.isOverallDecision()) {
                simulatedPassCount++;
            }
            if (baseline.isOverallDecision()) {
                baselinePassCount++;
            }
            if (simulated.isOverallDecision() != baseline.isOverallDecision()) {
                List<ConditionResult> failedConditions = simulated.isOverallDecision()
                        ? baseline.getFailedConditions() : simulated.getFailedConditions();
                String ruleDetails = failedConditions.stream()
                        .map(ConditionResult::getConditionName)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("No failed-rule details are available");
                String failingRules = simulated.isOverallDecision()
                        ? "Previously failed (real version): " + ruleDetails
                        : "Fails what-if version: " + ruleDetails;
                flips.add(new ApplicantFlip(applicant.getId(), applicant.getName(),
                        baseline.isOverallDecision(), simulated.isOverallDecision(), failingRules));
            }
        }
        if (comparedCount == 0) {
            throw new IllegalStateException(
                    "No stored results match the current applicants for this policy version. Evaluate this version first.");
        }

        return new SimulationResult(simulatedVersion, simulatedPassCount, baselinePassCount,
                simulatedPassCount - baselinePassCount, comparedCount, flips);
    }

    private static void collectNumericRules(Condition condition, List<Integer> path,
                                            List<NumericRule> rules, ProgramConfiguration program) {
        if (condition instanceof NumericCondition numeric) {
            FieldDefinition definition = findField(program, numeric.getFieldName());
            if (program == null || definition != null && definition.getType() == FieldType.NUMBER) {
                Double minimum = definition == null ? null : definition.getValidationRules().minimum();
                Double maximum = definition == null ? null : definition.getValidationRules().maximum();
                String field = normalizeField(numeric.getFieldName());
                if (minimum == null && "gpa".equals(field)) minimum = 0.0;
                if (maximum == null) {
                    if ("gpa".equals(field)) maximum = 10.0;
                    else if ("interviewscore".equals(field) || "interview".equals(field)) maximum = 100.0;
                }
                if (minimum == null) minimum = Math.min(0.0, numeric.getThreshold());
                if (maximum == null) {
                    maximum = Math.max(100.0,
                            numeric.getThreshold() + Math.max(10.0, Math.abs(numeric.getThreshold()) * 0.5));
                }
                double step = "gpa".equals(field) || numeric.getThreshold() % 1 != 0 ? 0.1 : 1.0;
                if (definition == null || definition.getSupportedOperators().contains(
                        FieldOperator.valueOf(numeric.getOperator().name()))
                        && numeric.getThreshold() >= minimum && numeric.getThreshold() <= maximum) {
                    rules.add(new NumericRule(path, numeric.getMessage(), numeric.getFieldName(),
                            numeric.getThreshold(), minimum, maximum, step));
                }
            }
        } else if (condition instanceof CompositeCondition composite) {
            for (int i = 0; i < composite.getSubconditions().size(); i++) {
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                collectNumericRules(composite.getSubconditions().get(i), childPath, rules, program);
            }
        }
    }

    private static void collectBooleanRules(Condition condition, List<Integer> path,
                                            List<BooleanRule> rules, ProgramConfiguration program) {
        if (condition instanceof FieldCondition field && field.getExpectedValue() instanceof Boolean value
                && (field.getOperator() == FieldOperator.EQUALS || field.getOperator() == FieldOperator.NOT_EQUALS)
                && isFieldType(program, field.getFieldKey(), FieldType.BOOLEAN)
                && supportsOperator(program, field.getFieldKey(), field.getOperator())) {
            rules.add(new BooleanRule(path, field.getMessage(), field.getFieldKey(), value));
        } else if (condition instanceof CompositeCondition composite) {
            for (int i = 0; i < composite.getSubconditions().size(); i++) {
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                collectBooleanRules(composite.getSubconditions().get(i), childPath, rules, program);
            }
        }
    }

    private static void collectMultiSelectRules(Condition condition, List<Integer> path,
                                                List<MultiSelectRule> rules, ProgramConfiguration program) {
        if (condition instanceof FieldCondition field && isMultiSelectField(field, program)) {
            FieldDefinition definition = findField(program, field.getFieldKey());
            List<String> expected = stringValues(field.getExpectedValue());
            List<String> options = definition == null ? List.of() : definition.getPermittedValues();
            rules.add(new MultiSelectRule(path, field.getMessage(), field.getFieldKey(),
                    field.getOperator(), expected, options, options.isEmpty()));
        } else if (condition instanceof CompositeCondition composite) {
            for (int i = 0; i < composite.getSubconditions().size(); i++) {
                List<Integer> childPath = new ArrayList<>(path);
                childPath.add(i);
                collectMultiSelectRules(composite.getSubconditions().get(i), childPath, rules, program);
            }
        }
    }

    private static boolean isMultiSelectField(FieldCondition field, ProgramConfiguration program) {
        FieldDefinition definition = findField(program, field.getFieldKey());
        if (definition != null) {
            return definition.getType() == FieldType.MULTI_SELECT
                    && definition.getSupportedOperators().contains(field.getOperator());
        }
        if (program != null) return false;
        return field.getExpectedValue() instanceof List<?>
                || field.getOperator() == FieldOperator.CONTAINS_ANY
                || field.getOperator() == FieldOperator.CONTAINS_ALL;
    }

    private static boolean isFieldType(ProgramConfiguration program, String key, FieldType type) {
        FieldDefinition definition = findField(program, key);
        return program == null || definition != null && definition.getType() == type;
    }

    private static boolean supportsOperator(ProgramConfiguration program, String key, FieldOperator operator) {
        FieldDefinition definition = findField(program, key);
        return program == null || definition != null && definition.getSupportedOperators().contains(operator);
    }

    private static FieldDefinition findField(ProgramConfiguration program, String key) {
        if (program == null) return null;
        String normalized = normalizeField(key);
        return program.getFields().stream()
                .filter(field -> normalizeField(field.getKey()).equals(normalized))
                .findFirst().orElse(null);
    }

    private static String normalizeField(String field) {
        if (field == null) return "";
        String normalized = field.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        return "interview".equals(normalized) ? "interviewscore" : normalized;
    }

    private static List<String> stringValues(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        }
        return value instanceof String text ? List.of(text) : List.of();
    }

    private static void validateFieldAdjustment(FieldCondition field, Object value,
                                                ProgramConfiguration program) {
        if (value instanceof Boolean && field.getExpectedValue() instanceof Boolean
                && (field.getOperator() == FieldOperator.EQUALS
                || field.getOperator() == FieldOperator.NOT_EQUALS)
                && isFieldType(program, field.getFieldKey(), FieldType.BOOLEAN)
                && supportsOperator(program, field.getFieldKey(), field.getOperator())) {
            return;
        }
        if (value instanceof List<?> values
                && (field.getOperator() == FieldOperator.CONTAINS_ANY
                || field.getOperator() == FieldOperator.CONTAINS_ALL)
                && !values.isEmpty() && values.stream().allMatch(String.class::isInstance)
                && isMultiSelectField(field, program)) {
            validatePermittedValues(field, values, program);
            return;
        }
        if (value instanceof String && (field.getOperator() == FieldOperator.CONTAINS
                || field.getOperator() == FieldOperator.NOT_CONTAINS)
                && isMultiSelectField(field, program)) {
            validatePermittedValues(field, List.of(value), program);
            return;
        }
        throw new IllegalArgumentException("Rule adjustment value is incompatible with field rule '"
                + field.getMessage() + "'.");
    }

    private static void validatePermittedValues(FieldCondition field, List<?> values,
                                                ProgramConfiguration program) {
        FieldDefinition definition = findField(program, field.getFieldKey());
        if (definition == null || definition.getPermittedValues().isEmpty()) return;
        for (Object value : values) {
            if (!definition.getPermittedValues().contains(value)) {
                throw new IllegalArgumentException("Value '" + value + "' is not permitted for "
                        + definition.getLabel() + ".");
            }
        }
    }

    private static List<Condition> cloneConditions(List<Condition> conditions) {
        return new ArrayList<>(conditions.stream().map(condition ->
                ConditionDocumentMapper.fromDocument(ConditionDocumentMapper.toDocument(condition))).toList());
    }

    private static Condition conditionAtPath(List<Condition> roots, List<Integer> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("A rule path is required.");
        }
        try {
            Condition current = roots.get(path.get(0));
            for (int i = 1; i < path.size(); i++) {
                if (!(current instanceof CompositeCondition composite)) {
                    throw new IllegalArgumentException("Rule path does not point to a nested condition.");
                }
                current = composite.getSubconditions().get(path.get(i));
            }
            return current;
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalArgumentException("Rule path does not exist in the selected policy version.", exception);
        }
    }

    private static Condition adjustedCondition(Condition condition, List<Integer> path,
                                               int depth, Object value) {
        if (depth == path.size()) {
            if (condition instanceof NumericCondition numeric) {
                numeric.setThreshold(((Number) value).doubleValue());
                return numeric;
            }
            if (condition instanceof FieldCondition field) {
                return new FieldCondition(field.getFieldKey(), field.getOperator(), value);
            }
            throw new IllegalArgumentException("The selected rule cannot be adjusted.");
        }
        if (!(condition instanceof CompositeCondition composite)) {
            throw new IllegalArgumentException("Rule path does not point to a nested condition.");
        }
        int childIndex = path.get(depth);
        Condition child = composite.getSubconditions().get(childIndex);
        composite.setSubcondition(childIndex, adjustedCondition(child, path, depth + 1, value));
        return composite;
    }

    private static String explainChangedConditions(EvaluationResult baseline, EvaluationResult simulated) {
        List<String> changed = new ArrayList<>();
        int count = Math.min(baseline.getConditionResults().size(), simulated.getConditionResults().size());
        for (int i = 0; i < count; i++) {
            ConditionResult before = baseline.getConditionResults().get(i);
            ConditionResult after = simulated.getConditionResults().get(i);
            if (before.isPassed() != after.isPassed()) changed.add(after.getConditionName());
        }
        if (changed.isEmpty()) {
            return "Decision changed; inspect the condition explanations for nested rules.";
        }
        return (simulated.isOverallDecision() ? "Now passes: " : "Now fails: ") + String.join(", ", changed);
    }

    private static NumericCondition numericConditionAt(List<Condition> conditions, List<Integer> path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("A numeric rule path is required.");
        }
        Condition current = conditions.get(path.get(0));
        for (int i = 1; i < path.size(); i++) {
            if (!(current instanceof CompositeCondition composite)) {
                throw new IllegalArgumentException("The selected numeric rule path is invalid.");
            }
            current = composite.getSubconditions().get(path.get(i));
        }
        if (!(current instanceof NumericCondition numeric)) {
            throw new IllegalArgumentException("The selected rule is not numeric.");
        }
        return numeric;
    }

    public record NumericRule(List<Integer> path, String label, String fieldName, double threshold,
                              double minimum, double maximum, double step) {
        public NumericRule {
            path = List.copyOf(path);
        }

        public NumericRule(List<Integer> path, String label, String fieldName, double threshold) {
            this(path, label, fieldName, threshold,
                    defaultMinimum(fieldName, threshold), defaultMaximum(fieldName, threshold),
                    defaultStep(fieldName, threshold));
        }

        @Override
        public String toString() {
            return label;
        }

        private static double defaultMinimum(String fieldName, double threshold) {
            return "gpa".equals(normalizeField(fieldName)) ? 0.0 : Math.min(0.0, threshold);
        }

        private static double defaultMaximum(String fieldName, double threshold) {
            String field = normalizeField(fieldName);
            if ("gpa".equals(field)) return 10.0;
            if ("interviewscore".equals(field)) return 100.0;
            return Math.max(100.0, threshold + Math.max(10.0, Math.abs(threshold) * 0.5));
        }

        private static double defaultStep(String fieldName, double threshold) {
            return "gpa".equals(normalizeField(fieldName)) || threshold % 1 != 0 ? 0.1 : 1.0;
        }
    }

    public record BooleanRule(List<Integer> path, String label, String fieldKey, boolean expectedValue) {
        public BooleanRule { path = List.copyOf(path); }
    }

    public record MultiSelectRule(List<Integer> path, String label, String fieldKey, FieldOperator operator,
                                  List<String> expectedValues, List<String> options, boolean openValueSet) {
        public MultiSelectRule {
            path = List.copyOf(path);
            expectedValues = List.copyOf(expectedValues);
            options = List.copyOf(options);
        }
    }

    public record RuleAdjustment(List<Integer> path, Object value) {
        public RuleAdjustment {
            if (path == null || path.isEmpty()) throw new IllegalArgumentException("A rule path is required.");
            path = List.copyOf(path);
            value = value instanceof List<?> values ? List.copyOf(values) : value;
        }
    }

    public record ApplicantFlip(String applicantId, String applicantName, boolean baselineEligible,
                                boolean simulatedEligible, String failingRule) {
        public ApplicantFlip(String applicantName, String failingRule) {
            this(null, applicantName, false, false, failingRule);
        }
    }

    public record SimulationResult(PolicyVersion simulatedVersion,
                                  int passCount,
                                  int baselinePassCount,
                                  int delta,
                                  int comparedCount,
                                  List<ApplicantFlip> flips) {
        public SimulationResult {
            flips = List.copyOf(flips);
        }
    }
}
