package com.ruleweaver.service;

import com.ruleweaver.model.EvaluationResult;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EvaluationAnalytics {

    private EvaluationAnalytics() {
    }

    public static List<EvaluationResult> latestPerApplicant(List<EvaluationResult> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return List.of();
        }
        Map<String, EvaluationResult> latest = new LinkedHashMap<>();
        for (EvaluationResult candidate : evaluations) {
            latest.merge(candidate.getApplicantId(), candidate, EvaluationAnalytics::laterEvaluation);
        }
        return latest.values().stream()
                .sorted(Comparator.comparing(EvaluationResult::getEvaluatedOn,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
    }

    private static EvaluationResult laterEvaluation(EvaluationResult current, EvaluationResult candidate) {
        LocalDateTime currentTime = current.getEvaluatedOn();
        LocalDateTime candidateTime = candidate.getEvaluatedOn();
        if (currentTime == null) {
            return candidate;
        }
        return candidateTime == null || candidateTime.isBefore(currentTime) ? current : candidate;
    }
}
