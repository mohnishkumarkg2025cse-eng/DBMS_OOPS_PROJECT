package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implements the Composite Design Pattern for conditions.
 * Allows treating single criteria and grouped logical criteria (AND/OR) uniformly.
 * 
 * Demonstrates:
 * - Composition: Holds a collection of other Condition instances
 * - Polymorphism: Evaluates any Condition subtype without concrete coupling
 * - Recursion: Can nest composites within composites
 */
public class CompositeCondition extends AbstractCondition {
    private LogicalOperator logicalOperator;
    private List<Condition> subconditions;

    public CompositeCondition(String name, LogicalOperator logicalOperator) {
        super(name);
        this.logicalOperator = logicalOperator != null ? logicalOperator : LogicalOperator.AND;
        this.subconditions = new ArrayList<>();
    }

    public CompositeCondition(String name, LogicalOperator logicalOperator, List<Condition> subconditions) {
        super(name);
        this.logicalOperator = logicalOperator != null ? logicalOperator : LogicalOperator.AND;
        this.subconditions = subconditions != null ? new ArrayList<>(subconditions) : new ArrayList<>();
    }

    public void addSubcondition(Condition condition) {
        if (condition != null) {
            this.subconditions.add(condition);
        }
    }

    public void removeSubcondition(Condition condition) {
        this.subconditions.remove(condition);
    }

    public void setSubcondition(int index, Condition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Composite subcondition is required.");
        }
        subconditions.set(index, condition);
    }

    public List<Condition> getSubconditions() {
        return Collections.unmodifiableList(subconditions);
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    @Override
    public boolean test(Applicant applicant) {
        if (subconditions.isEmpty()) {
            return true; // Empty condition group vacuously passes
        }

        if (logicalOperator == LogicalOperator.AND) {
            for (Condition cond : subconditions) {
                if (!cond.test(applicant)) {
                    return false;
                }
            }
            return true;
        } else { // OR
            for (Condition cond : subconditions) {
                if (cond.test(applicant)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public String explain(Applicant applicant) {
        if (subconditions.isEmpty()) {
            return getMessage() + ": Passed (no sub-criteria configured)";
        }

        boolean passed = test(applicant);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[%s Group '%s' (%s)]: %s\n", 
                logicalOperator, getMessage(), subconditions.size() + " rules", 
                passed ? "Passed" : "Failed"));

        for (int i = 0; i < subconditions.size(); i++) {
            Condition sub = subconditions.get(i);
            sb.append("   ├── ").append(sub.explain(applicant));
            if (i < subconditions.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    protected Object resolveActualValue(Applicant applicant) {
        return subconditions.stream()
                .map(c -> c.getMessage() + "=" + c.test(applicant))
                .collect(Collectors.toList());
    }

    @Override
    public String getType() {
        return "CompositeCondition";
    }
}
