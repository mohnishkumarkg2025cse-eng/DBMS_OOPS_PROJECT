package com.ruleweaver.service;

import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PipelineCalculatorTest {

    private static final String POLICY_ID = "policy-1";
    private static final int VERSION = 2;
    private final PipelineCalculator calculator = new PipelineCalculator();

    @Test
    void noApplicantsProducesAvailableZeroCounts() {
        PipelineCalculator.PipelineSummary summary = calculate(List.of(), List.of(), goodVersion());

        assertEquals(0L, summary.totalApplicants().count());
        assertEquals(0L, summary.awaitingEvaluation().count());
        assertEquals(0L, summary.evaluationsCompleted().count());
        assertEquals(0L, summary.approved().count());
        assertEquals(0L, summary.rejected().count());
        assertTrue(summary.needsAttention().isEmpty());
    }

    @Test
    void applicantsWithoutResultsAreAwaitingAndNeverRejected() {
        List<Applicant> applicants = List.of(applicant("a-1", "Avery"), applicant("a-2", "Blake"));

        PipelineCalculator.PipelineSummary summary = calculate(applicants, List.of(), goodVersion());

        assertEquals(2L, summary.totalApplicants().count());
        assertEquals(2L, summary.awaitingEvaluation().count());
        assertEquals(0L, summary.evaluationsCompleted().count());
        assertEquals(0L, summary.approved().count());
        assertEquals(0L, summary.rejected().count());
        assertEquals(List.of("Avery", "Blake"),
                summary.needsAttention().stream().map(PipelineCalculator.AttentionApplicant::displayName).toList());
    }

    @Test
    void allApprovedAndAllRejectedUseLatestSavedDecision() {
        List<Applicant> applicants = List.of(applicant("a-1", "Avery"), applicant("a-2", "Blake"));
        PipelineCalculator.PipelineSummary approved = calculate(applicants,
                List.of(result("a-1", true, 1), result("a-2", true, 2)), goodVersion());
        PipelineCalculator.PipelineSummary rejected = calculate(applicants,
                List.of(result("a-1", false, 1), result("a-2", false, 2)), goodVersion());

        assertEquals(2L, approved.approved().count());
        assertEquals(0L, approved.rejected().count());
        assertEquals(0L, rejected.approved().count());
        assertEquals(2L, rejected.rejected().count());
    }

    @Test
    void mixedOutcomesSatisfyPipelineCountInvariants() {
        List<Applicant> applicants = List.of(
                applicant("a-1", "Avery"), applicant("a-2", "Blake"), applicant("a-3", "Casey"));

        PipelineCalculator.PipelineSummary summary = calculate(applicants,
                List.of(result("a-1", true, 1), result("a-2", false, 2)), goodVersion());

        assertEquals(3L, summary.totalApplicants().count());
        assertEquals(1L, summary.awaitingEvaluation().count());
        assertEquals(2L, summary.evaluationsCompleted().count());
        assertEquals(1L, summary.approved().count());
        assertEquals(1L, summary.rejected().count());
        assertEquals(summary.evaluationsCompleted().count(),
                summary.approved().count() + summary.rejected().count());
        assertEquals(summary.totalApplicants().count() - summary.evaluationsCompleted().count(),
                summary.awaitingEvaluation().count());
    }

    @Test
    void repeatedRunsCountApplicantOnceAndUseLatestTimestamp() {
        List<Applicant> applicants = List.of(applicant("a-1", "Avery"));
        EvaluationResult earlier = result("a-1", false, 1);
        earlier.setEvaluatedOn(LocalDateTime.parse("2026-10-10T10:00:00"));
        EvaluationResult latest = result("a-1", true, 2);
        latest.setEvaluatedOn(LocalDateTime.parse("2026-10-11T10:00:00"));

        PipelineCalculator.PipelineSummary summary = calculate(applicants,
                List.of(earlier, latest), goodVersion());

        assertEquals(1L, summary.evaluationsCompleted().count());
        assertEquals(1L, summary.approved().count());
        assertEquals(0L, summary.rejected().count());
    }

    @Test
    void ignoresResultsFromOtherPolicyVersions() {
        EvaluationResult otherVersion = result("a-1", true, 1);
        otherVersion.setPolicyVersionNumber(VERSION - 1);

        PipelineCalculator.PipelineSummary summary = calculate(
                List.of(applicant("a-1", "Avery")), List.of(otherVersion), goodVersion());

        assertEquals(1L, summary.awaitingEvaluation().count());
        assertEquals(0L, summary.evaluationsCompleted().count());
        assertEquals(0L, summary.rejected().count());
    }

    @Test
    void supportsLegacyResultsThatOnlyHaveTheVersionId() {
        EvaluationResult legacy = new EvaluationResult();
        legacy.setApplicantId("a-1");
        legacy.setPolicyVersionId(POLICY_ID + "_v" + VERSION);
        legacy.setOverallDecision(true);
        legacy.setEvaluatedOn(LocalDateTime.parse("2026-10-12T10:00:00"));

        PipelineCalculator.PipelineSummary summary = calculate(
                List.of(applicant("a-1", "Avery")), List.of(legacy), goodVersion());

        assertEquals(1L, summary.evaluationsCompleted().count());
        assertEquals(1L, summary.approved().count());
        assertEquals(0L, summary.awaitingEvaluation().count());
    }

    @Test
    void zeroConditionVersionWarnsAndSuppressesEveryMetric() {
        PolicyVersion emptyVersion = new PolicyVersion();
        emptyVersion.setPolicyId(POLICY_ID);
        emptyVersion.setVersionNumber(VERSION);

        PipelineCalculator.PipelineSummary summary = calculate(
                List.of(applicant("a-1", "Avery")),
                List.of(result("a-1", true, 1)), emptyVersion);

        assertTrue(summary.misconfiguredVersion());
        assertTrue(summary.warnings().get(0).contains("no conditions"));
        assertEquals(PipelineCalculator.Availability.UNAVAILABLE,
                summary.totalApplicants().availability());
        assertEquals(PipelineCalculator.Availability.UNAVAILABLE,
                summary.evaluationsCompleted().availability());
        assertTrue(summary.needsAttention().isEmpty());
    }

    @Test
    void failedOrIncompleteMetricIsUnavailableWhenModelHasNoSuchState() {
        PipelineCalculator.PipelineSummary summary = calculate(
                List.of(applicant("a-1", "Avery")), List.of(), goodVersion());

        assertEquals(PipelineCalculator.Availability.UNAVAILABLE,
                summary.failedOrIncomplete().availability());
        assertTrue(summary.failedOrIncomplete().explanation().contains("no failed or incomplete"));
    }

    @Test
    void unknownLatestOutcomeMakesOutcomeTotalsUnavailableRatherThanGuessing() {
        EvaluationResult unknown = result("a-1", true, 1);
        unknown.setOverallDecision(null);

        PipelineCalculator.PipelineSummary summary = calculate(
                List.of(applicant("a-1", "Avery")), List.of(unknown), goodVersion());

        assertEquals(PipelineCalculator.Availability.UNAVAILABLE,
                summary.evaluationsCompleted().availability());
        assertEquals(PipelineCalculator.Availability.UNAVAILABLE, summary.approved().availability());
        assertEquals(PipelineCalculator.Availability.UNAVAILABLE, summary.rejected().availability());
        assertEquals(0L, summary.awaitingEvaluation().count());
    }

    private PipelineCalculator.PipelineSummary calculate(List<Applicant> applicants,
                                                          List<EvaluationResult> results,
                                                          PolicyVersion version) {
        return calculator.calculate(applicants, results, POLICY_ID, VERSION, version);
    }

    private static PolicyVersion goodVersion() {
        PolicyVersion version = new PolicyVersion();
        version.setPolicyId(POLICY_ID);
        version.setVersionNumber(VERSION);
        version.setConditions(List.of(new NumericCondition("gpa", Operator.GREATER_THAN, 2.0)));
        return version;
    }

    private static Applicant applicant(String id, String name) {
        return new Applicant(id, name, 3.0, 80, List.of());
    }

    private static EvaluationResult result(String applicantId, boolean approved, int minute) {
        EvaluationResult result = new EvaluationResult(
                applicantId, applicantId, POLICY_ID, "Policy", VERSION);
        result.setOverallDecision(approved);
        result.setEvaluatedOn(LocalDateTime.of(2026, 10, 12, 10, minute));
        return result;
    }
}
