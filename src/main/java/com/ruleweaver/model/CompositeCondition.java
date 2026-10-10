package com.ruleweaver.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * *** TEMPORARY STUB - owned by Rizwan. ***
 * Groups several conditions (Composite pattern). This stub supports AND only, as the PDF suggests.
 */
public class CompositeCondition implements Condition {

    private final List<Condition> subconditions;

    public CompositeCondition(List<Condition> subconditions) {
        if (subconditions == null || subconditions.isEmpty()) {
            throw new IllegalArgumentException("A CompositeCondition needs at least one sub-condition");
        }
        this.subconditions = Collections.unmodifiableList(new ArrayList<>(subconditions));
    }

    public List<Condition> getSubconditions() { return subconditions; }

    @Override
    public boolean test(Applicant a) {
        for (Condition c : subconditions) {
            if (!c.test(a)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getMessage() {
        List<String> parts = new ArrayList<>();
        for (Condition c : subconditions) {
            parts.add(c.getMessage());
        }
        return "(" + String.join(" AND ", parts) + ")";
    }

    @Override
    public String explain(Applicant a) {
        List<String> parts = new ArrayList<>();
        for (Condition c : subconditions) {
            parts.add(c.explain(a));
        }
        return String.join("; ", parts);
    }
}
