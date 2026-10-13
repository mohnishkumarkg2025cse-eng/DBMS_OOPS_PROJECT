package com.ruleweaver.service;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardAnalyticsTest {
    private final DashboardAnalytics analytics = new DashboardAnalytics();

    @Test
    void calculatesDecisionFunnelNearMissAndRuleDenominatorsFromRecordedResults() {
        Policy policy = policy("policy-1", "program-1", 2);
        List<EvaluationResult> results = List.of(
                evaluation("a-1", policy, 2, "run-1", true,
                        condition("GPA >= 8", true), condition("Interview >= 70", true)),
                evaluation("a-2", policy, 2, "run-1", false,
                        condition("GPA >= 8", true), condition("Interview >= 70", false)),
                evaluation("a-3", policy, 2, "run-1", false,
                        condition("GPA >= 8", false), condition("Interview >= 70", false)));

        DashboardAnalytics.Analysis analysis = analytics.analyze(results, List.of(policy),
                new DashboardAnalytics.Filters("program-1", "policy-1", 2, "run-1",
                        DashboardAnalytics.DecisionFilter.ALL));

        assertEquals(new DashboardAnalytics.Funnel(3, 1, 2, 1, 2), analysis.funnel());
        assertEquals(2, analysis.ruleImpacts().size());
        assertEquals(new DashboardAnalytics.RuleImpact("Interview >= 70", 1, 2, 3),
                analysis.ruleImpacts().get(0));
        assertEquals(new DashboardAnalytics.RuleImpact("GPA >= 8", 2, 1, 3),
                analysis.ruleImpacts().get(1));
        assertEquals("Interview >= 70", analysis.rejectionReasons().get(0).ruleName());
        assertEquals(2, analysis.rejectionReasons().get(0).failedApplicants());
    }

    @Test
    void groupsValidDatedOutcomesByDateAndSkipsUnknownUndatedAndOutOfRangeResults() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        EvaluationResult approved = new EvaluationResult();
        approved.setOverallDecision(true);
        approved.setEvaluatedOn(start.atTime(8, 0));
        EvaluationResult rejected = new EvaluationResult();
        rejected.setOverallDecision(false);
        rejected.setEvaluatedOn(start.atTime(15, 0));
        EvaluationResult nextDay = new EvaluationResult();
        nextDay.setOverallDecision(true);
        nextDay.setEvaluatedOn(start.plusDays(1).atStartOfDay());
        EvaluationResult unknown = new EvaluationResult();
        unknown.setOverallDecision(null);
        unknown.setEvaluatedOn(start.atTime(12, 0));
        EvaluationResult undated = new EvaluationResult();
        undated.setOverallDecision(false);
        undated.setEvaluatedOn(null);
        EvaluationResult beforeRange = new EvaluationResult();
        beforeRange.setOverallDecision(true);
        beforeRange.setEvaluatedOn(start.minusDays(1).atStartOfDay());

        List<DashboardAnalytics.DailyDecisionCount> daily = analytics.dailyDecisionCounts(
                List.of(approved, rejected, nextDay, unknown, undated, beforeRange),
                start, start.plusDays(1));

        assertEquals(List.of(
                new DashboardAnalytics.DailyDecisionCount(start, 1, 1),
                new DashboardAnalytics.DailyDecisionCount(start.plusDays(1), 1, 0)), daily);
        assertTrue(analytics.dailyDecisionCounts(List.of(unknown, undated), start, start).isEmpty());
    }

    @Test
    void appliesProgramPolicyVersionRunAndDecisionFiltersToLatestApplicantResults() {
        Policy internship = policy("policy-1", "program-1", 2);
        Policy scholarship = policy("policy-2", "program-2", 2);
        EvaluationResult priorRun = evaluation("a-1", internship, 2, "run-old", true);
        priorRun.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 0));
        EvaluationResult selectedRunEligible = evaluation("a-1", internship, 2, "run-selected", true);
        selectedRunEligible.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 10));
        EvaluationResult selectedRunRejected = evaluation("a-2", internship, 2, "run-selected", false);
        EvaluationResult otherProgram = evaluation("a-3", scholarship, 2, "run-selected", true);
        EvaluationResult baselineEligibleApplicant = evaluation("a-1", internship, 1, "baseline", false);
        EvaluationResult baselineRejectedApplicant = evaluation("a-2", internship, 1, "baseline", false);

        DashboardAnalytics.Analysis analysis = analytics.analyze(
                List.of(priorRun, selectedRunEligible, selectedRunRejected, otherProgram,
                        baselineEligibleApplicant, baselineRejectedApplicant),
                List.of(internship, scholarship),
                new DashboardAnalytics.Filters("program-1", "policy-1", 2, "run-selected",
                        DashboardAnalytics.DecisionFilter.REJECTED));

        assertEquals(1, analysis.funnel().evaluated());
        assertEquals(0, analysis.funnel().eligible());
        assertEquals(1, analysis.funnel().rejected());
        assertEquals("a-2", analysis.evaluations().get(0).getApplicantId());
        assertEquals(2, analysis.baseline().sharedApplicants());
        assertEquals(50.0, analysis.baseline().currentPassRate());

        DashboardAnalytics.Analysis eligibleAnalysis = analytics.analyze(
                List.of(priorRun, selectedRunEligible, selectedRunRejected, otherProgram,
                        baselineEligibleApplicant, baselineRejectedApplicant),
                List.of(internship, scholarship),
                new DashboardAnalytics.Filters("program-1", "policy-1", 2, "run-selected",
                        DashboardAnalytics.DecisionFilter.ELIGIBLE));
        assertEquals(1, eligibleAnalysis.funnel().evaluated());
        assertEquals(1, eligibleAnalysis.funnel().eligible());
    }

    @Test
    void deduplicatesByPolicyAndApplicantAndProvidesVersionBaselineOnlyForSharedApplicants() {
        Policy policy = policy("policy-1", null, 2);
        EvaluationResult oldCurrent = evaluation("a-1", policy, 2, "run-old", false);
        oldCurrent.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 0));
        EvaluationResult current = evaluation("a-1", policy, 2, "run-new", true);
        current.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 5));
        EvaluationResult previousOne = evaluation("a-1", policy, 1, "baseline-1", false);
        EvaluationResult previousTwo = evaluation("a-2", policy, 1, "baseline-1", true);

        DashboardAnalytics.Analysis analysis = analytics.analyze(
                List.of(oldCurrent, current, previousOne, previousTwo), List.of(policy),
                new DashboardAnalytics.Filters(null, "policy-1", 2, null,
                        DashboardAnalytics.DecisionFilter.ALL));

        assertEquals(1, analysis.funnel().evaluated());
        assertEquals(1, analysis.baseline().versionNumber());
        assertEquals(1, analysis.baseline().sharedApplicants());
        assertEquals(0, analysis.baseline().eligibleApplicants());
        assertEquals(1, analysis.baseline().changedDecisions());
        assertEquals(0.0, analysis.baseline().passRate());
        assertEquals(100.0, analysis.baseline().currentPassRate());
    }

    @Test
    void treatsLegacyRecordsAsUntrackedAndEmptyOrUnavailableBaselinesAsUnknown() {
        Policy policy = policy("policy-1", null, 2);
        EvaluationResult legacy = evaluation("a-1", policy, 2, null, false);
        EvaluationResult otherPolicyLegacy = evaluation("a-1", policy("policy-2", null, 2),
                2, null, true);
        List<EvaluationResult> evaluations = List.of(legacy, otherPolicyLegacy);

        assertEquals(1, analytics.findRuns(evaluations, List.of(policy),
                DashboardAnalytics.Filters.all()).size());
        assertEquals(1, analytics.findRuns(evaluations, List.of(policy),
                DashboardAnalytics.Filters.all()).get(0).applicantCount());
        assertEquals(1, analytics.analyze(evaluations, List.of(policy),
                new DashboardAnalytics.Filters(null, "policy-1", 2,
                        DashboardAnalytics.LEGACY_RUN_ID, DashboardAnalytics.DecisionFilter.ALL))
                .funnel().evaluated());
        DashboardAnalytics.Analysis empty = analytics.analyze(List.of(), List.of(policy),
                new DashboardAnalytics.Filters(null, "policy-1", 2, null,
                        DashboardAnalytics.DecisionFilter.ALL));
        assertEquals(new DashboardAnalytics.Funnel(0, 0, 0, 0, 0), empty.funnel());
        assertFalse(empty.baseline().isAvailable());
        assertNull(empty.baseline().passRate());
    }

    @Test
    void reportsRunChoicesWithDistinctApplicantSampleCounts() {
        Policy policy = policy("policy-1", null, 1);
        List<EvaluationResult> results = List.of(
                evaluation("a-1", policy, 1, "run-1", true),
                evaluation("a-1", policy, 1, "run-1", false),
                evaluation("a-2", policy, 1, "run-1", true),
                evaluation("a-3", policy, 1, "run-2", false));

        List<DashboardAnalytics.RunOption> runs = analytics.findRuns(
                results, List.of(policy), DashboardAnalytics.Filters.all());

        assertEquals(2, runs.size());
        assertEquals("run-1", runs.get(0).id());
        assertEquals(2, runs.get(0).applicantCount());
        assertEquals("run-2", runs.get(1).id());
        assertEquals(1, runs.get(1).applicantCount());
        assertTrue(runs.stream().noneMatch(DashboardAnalytics.RunOption::isLegacy));
    }

    @Test
    void aggregatesAcrossPoliciesByDistinctApplicantWhenNoPolicyIsSelected() {
        Policy firstPolicy = policy("policy-1", "program-1", 1);
        Policy secondPolicy = policy("policy-2", "program-1", 1);
        EvaluationResult first = evaluation("a-1", firstPolicy, 1, "run-1", false);
        first.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 0));
        EvaluationResult second = evaluation("a-1", secondPolicy, 1, "run-2", true);
        second.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, 5));

        DashboardAnalytics.Analysis analysis = analytics.analyze(List.of(first, second),
                List.of(firstPolicy, secondPolicy),
                new DashboardAnalytics.Filters("program-1", null, null, null,
                        DashboardAnalytics.DecisionFilter.ALL));

        assertEquals(1, analysis.funnel().evaluated());
        assertEquals(1, analysis.funnel().eligible());
        assertEquals("policy-2", analysis.evaluations().get(0).getPolicyId());
    }

    private static Policy policy(String id, String programId, int latestVersion) {
        Policy policy = new Policy(id, id, "Analytics test policy");
        policy.setProgramId(programId);
        for (int version = 1; version <= latestVersion; version++) {
            policy.addVersion(new PolicyVersion(id + "_v" + version, id, version,
                    "Version " + version, List.of()));
        }
        return policy;
    }

    private static EvaluationResult evaluation(String applicantId, Policy policy, int version,
                                               String runId, boolean eligible,
                                               ConditionResult... conditions) {
        EvaluationResult result = new EvaluationResult(applicantId, applicantId,
                policy.getId(), policy.getName(), version);
        result.setPolicyVersionId(policy.getId() + "_v" + version);
        result.setEvaluationRunId(runId);
        result.setOverallDecision(eligible);
        result.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 9, version));
        result.setConditionResults(List.of(conditions));
        return result;
    }

    private static ConditionResult condition(String name, boolean passed) {
        return new ConditionResult(name, "Rule", passed, null, null, name);
    }
}
