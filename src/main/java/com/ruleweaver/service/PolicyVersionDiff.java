package com.ruleweaver.service;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.ConditionDocumentMapper;
import com.ruleweaver.model.PolicyVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Compares persisted policy-version conditions by their serialized rule configuration. */
public final class PolicyVersionDiff {

    public enum ChangeType {
        UNCHANGED,
        ADDED,
        REMOVED,
        MODIFIED
    }

    public record Change(ChangeType type, String baseline, String target) {
    }

    private PolicyVersionDiff() {
    }

    public static List<Change> compare(PolicyVersion baseline, PolicyVersion target) {
        if (baseline == null || target == null) {
            throw new IllegalArgumentException("Both policy versions are required.");
        }
        if (!Objects.equals(baseline.getPolicyId(), target.getPolicyId())) {
            throw new IllegalArgumentException("Policy versions must belong to the same policy.");
        }

        List<Condition> before = baseline.getConditions();
        List<Condition> after = target.getConditions();
        int sharedCount = Math.min(before.size(), after.size());
        List<Change> changes = new ArrayList<>();
        for (int index = 0; index < sharedCount; index++) {
            Condition oldCondition = before.get(index);
            Condition newCondition = after.get(index);
            if (ConditionDocumentMapper.toDocument(oldCondition)
                    .equals(ConditionDocumentMapper.toDocument(newCondition))) {
                changes.add(new Change(ChangeType.UNCHANGED,
                        describe(oldCondition), describe(newCondition)));
            } else {
                changes.add(new Change(ChangeType.MODIFIED,
                        describe(oldCondition), describe(newCondition)));
            }
        }
        for (int index = sharedCount; index < before.size(); index++) {
            changes.add(new Change(ChangeType.REMOVED, describe(before.get(index)), ""));
        }
        for (int index = sharedCount; index < after.size(); index++) {
            changes.add(new Change(ChangeType.ADDED, "", describe(after.get(index))));
        }
        return List.copyOf(changes);
    }

    private static String describe(Condition condition) {
        String message = condition.getMessage();
        return message == null || message.isBlank()
                ? condition.getClass().getSimpleName() : message;
    }
}
