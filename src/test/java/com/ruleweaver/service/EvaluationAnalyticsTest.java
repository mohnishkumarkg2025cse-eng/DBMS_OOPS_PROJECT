package com.ruleweaver.service;

import com.ruleweaver.model.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EvaluationAnalyticsTest {

    @Test
    void analyticsUseLatestEvaluationPerApplicantAndCompareOnlySharedApplicants() {
        EvaluationResult oldV1 = result("a-1", 1, true, 1);
        EvaluationResult latestV1 = result("a-1", 1, false, 3);
        EvaluationResult otherV1 = result("a-2", 1, true, 2);
        EvaluationResult latestV2 = result("a-1", 2, true, 4);
        ComparisonService service = new ComparisonService(null);

        List<EvaluationResult> latest = EvaluationAnalytics.latestPerApplicant(
                List.of(oldV1, latestV1, otherV1));
        var report = service.compareEvaluations(List.of(oldV1, latestV1, otherV1), List.of(latestV2));

        assertEquals(2, latest.size());
        assertSame(latestV1, latest.get(1));
        assertEquals(1, report.getTotalApplicants());
        assertEquals(0, report.getV1PassedCount());
        assertEquals(1, report.getV2PassedCount());
        assertEquals(1, report.getGainedEligibilityCount());
    }

    private static EvaluationResult result(String applicantId, int version, boolean passed, int minute) {
        EvaluationResult result = new EvaluationResult(applicantId, applicantId, "policy-1", "Policy", version);
        result.setOverallDecision(passed);
        result.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, minute));
        return result;
    }
}
