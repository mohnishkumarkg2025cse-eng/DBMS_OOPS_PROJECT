package com.ruleweaver.service;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PolicyVersionDiffTest {

    @Test
    void comparesUnchangedModifiedAddedAndRemovedConditions() {
        PolicyVersion baseline = version(1, List.of(
                new NumericCondition("gpa", ">=", 3.0),
                new SkillCondition("Java"),
                new SkillCondition("Python")));
        PolicyVersion target = version(2, List.of(
                new NumericCondition("gpa", ">=", 3.0),
                new SkillCondition("Kotlin"),
                new NumericCondition("interviewScore", ">=", 80)));

        List<PolicyVersionDiff.Change> changes = PolicyVersionDiff.compare(baseline, target);

        assertEquals(List.of(
                PolicyVersionDiff.ChangeType.UNCHANGED,
                PolicyVersionDiff.ChangeType.MODIFIED,
                PolicyVersionDiff.ChangeType.MODIFIED),
                changes.stream().map(PolicyVersionDiff.Change::type).toList());
        assertEquals("Skill 'Java' required", changes.get(1).baseline());
        assertEquals("Skill 'Kotlin' required", changes.get(1).target());
        assertEquals("Skill 'Python' required", changes.get(2).baseline());
        assertEquals("InterviewScore >= 80", changes.get(2).target());
    }

    @Test
    void reportsTrailingAddedAndRemovedConditions() {
        PolicyVersion baseline = version(1, List.of(new SkillCondition("Java")));
        PolicyVersion target = version(2, List.of(
                new SkillCondition("Java"),
                new NumericCondition("gpa", ">=", 3.0)));

        List<PolicyVersionDiff.Change> changes = PolicyVersionDiff.compare(baseline, target);

        assertEquals(PolicyVersionDiff.ChangeType.UNCHANGED, changes.get(0).type());
        assertEquals(PolicyVersionDiff.ChangeType.ADDED, changes.get(1).type());
        assertEquals("Gpa >= 3", changes.get(1).target());
    }

    @Test
    void rejectsVersionsFromDifferentPolicies() {
        PolicyVersion baseline = version(1, List.of());
        PolicyVersion target = new PolicyVersion("other", 2, List.of());

        assertThrows(IllegalArgumentException.class, () -> PolicyVersionDiff.compare(baseline, target));
    }

    private static PolicyVersion version(int number, List<Condition> conditions) {
        return new PolicyVersion("policy-1", number, conditions);
    }
}
