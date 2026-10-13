package com.ruleweaver.service;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.PolicyVersion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public final class PipelineCalculator {

    public enum Availability {
        AVAILABLE,
        UNAVAILABLE
    }

    public record Metric(Long count, Availability availability, String explanation) {
        public Metric {
            Objects.requireNonNull(availability);
            if (availability == Availability.AVAILABLE && (count == null || count < 0)) {
                throw new IllegalArgumentException("An available metric requires a non-negative count.");
            }
            if (availability == Availability.UNAVAILABLE && count != null) {
                throw new IllegalArgumentException("An unavailable metric cannot expose a count.");
            }
        }

        public static Metric available(long count) {
            return new Metric(count, Availability.AVAILABLE, null);
        }

        public static Metric unavailable(String explanation) {
            return new Metric(null, Availability.UNAVAILABLE, explanation);
        }

        public String displayValue() {
            return count == null ? "Unavailable" : Long.toString(count);
        }

        public boolean isAvailable() {
            return availability == Availability.AVAILABLE;
        }
    }

    public record AttentionApplicant(String applicantId, String displayName) { }

    public record PipelineSummary(
            Metric totalApplicants,
            Metric awaitingEvaluation,
            Metric evaluationsCompleted,
            Metric approved,
            Metric rejected,
            Metric failedOrIncomplete,
            boolean misconfiguredVersion,
            List<String> warnings,
            List<AttentionApplicant> needsAttention) {
        public PipelineSummary {
            Objects.requireNonNull(totalApplicants);
            Objects.requireNonNull(awaitingEvaluation);
            Objects.requireNonNull(evaluationsCompleted);
            Objects.requireNonNull(approved);
            Objects.requireNonNull(rejected);
            Objects.requireNonNull(failedOrIncomplete);
            warnings = List.copyOf(warnings);
            needsAttention = List.copyOf(needsAttention);
        }
    }

    public PipelineSummary calculate(List<Applicant> applicants,
                                     List<EvaluationResult> storedResults,
                                     String policyId,
                                     int versionNumber,
                                     PolicyVersion selectedVersion) {
        Objects.requireNonNull(applicants, "Applicants must be loaded before calculating the pipeline.");
        Objects.requireNonNull(storedResults, "Evaluation results must be loaded before calculating the pipeline.");
        if (policyId == null || policyId.isBlank() || versionNumber <= 0
                || selectedVersion == null
                || !Objects.equals(policyId, selectedVersion.getPolicyId())
                || versionNumber != selectedVersion.getVersionNumber()) {
            return unavailableSummary("Select a saved policy version to view its decision pipeline.", false);
        }
        if (selectedVersion.getConditions().isEmpty()) {
            return unavailableSummary("This policy version has no conditions. Add at least one condition before evaluating applicants.",
                    true);
        }

        Map<String, Applicant> applicantsById = new TreeMap<>();
        List<Applicant> applicantsWithoutId = new ArrayList<>();
        for (Applicant applicant : applicants) {
            if (applicant == null) {
                continue;
            }
            if (applicant.getId() == null || applicant.getId().isBlank()) {
                applicantsWithoutId.add(applicant);
            } else {
                applicantsById.putIfAbsent(applicant.getId(), applicant);
            }
        }
        long total = applicantsById.size() + applicantsWithoutId.size();
        Set<String> knownApplicantIds = new HashSet<>(applicantsById.keySet());

        Map<String, List<EvaluationResult>> resultsByApplicant = new HashMap<>();
        for (EvaluationResult result : storedResults) {
            if (result == null || result.getApplicantId() == null
                    || !knownApplicantIds.contains(result.getApplicantId())
                    || !belongsToVersion(result, policyId, versionNumber)) {
                continue;
            }
            resultsByApplicant.computeIfAbsent(result.getApplicantId(), ignored -> new ArrayList<>())
                    .add(result);
        }

        long completed = resultsByApplicant.size();
        long awaiting = total - completed;
        long approvedCount = 0;
        long rejectedCount = 0;
        boolean allLatestDecisionsKnown = true;
        for (List<EvaluationResult> applicantResults : resultsByApplicant.values()) {
            Boolean decision = latestDecision(applicantResults);
            if (decision == null) {
                allLatestDecisionsKnown = false;
            } else if (decision) {
                approvedCount++;
            } else {
                rejectedCount++;
            }
        }

        List<AttentionApplicant> attention = applicantsById.values().stream()
                .filter(applicant -> !resultsByApplicant.containsKey(applicant.getId()))
                .map(applicant -> new AttentionApplicant(applicant.getId(),
                        applicant.getName() == null || applicant.getName().isBlank()
                                ? applicant.getId() : applicant.getName().trim()))
                .sorted(Comparator.comparing(AttentionApplicant::displayName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AttentionApplicant::applicantId))
                .toList();
        List<AttentionApplicant> anonymousAttention = new ArrayList<>();
        for (Applicant applicant : applicantsWithoutId) {
            String name = applicant.getName() == null || applicant.getName().isBlank()
                    ? "Applicant" : applicant.getName().trim();
            anonymousAttention.add(new AttentionApplicant(null, name));
        }
        attention = new ArrayList<>(attention);
        attention.addAll(anonymousAttention);
        attention.sort(Comparator.comparing(AttentionApplicant::displayName,
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(item -> Objects.requireNonNullElse(item.applicantId(), "")));

        Metric completedMetric = allLatestDecisionsKnown
                ? Metric.available(completed)
                : Metric.unavailable("At least one latest saved result has no unambiguous decision outcome.");
        Metric approvedMetric = allLatestDecisionsKnown
                ? Metric.available(approvedCount) : completedMetric;
        Metric rejectedMetric = allLatestDecisionsKnown
                ? Metric.available(rejectedCount) : completedMetric;

        return new PipelineSummary(
                Metric.available(total),
                Metric.available(awaiting),
                completedMetric,
                approvedMetric,
                rejectedMetric,
                Metric.unavailable("EvaluationResult has no failed or incomplete evaluation state."),
                false,
                allLatestDecisionsKnown ? List.of()
                        : List.of("Some saved results have an unknown or ambiguous outcome; outcome counts are unavailable."),
                attention);
    }

    private static PipelineSummary unavailableSummary(String warning, boolean misconfigured) {
        Metric unavailable = Metric.unavailable(warning);
        return new PipelineSummary(unavailable, unavailable, unavailable, unavailable, unavailable, unavailable,
                misconfigured, List.of(warning), List.of());
    }

    private static boolean belongsToVersion(EvaluationResult result, String policyId, int versionNumber) {
        if (result.getPolicyId() != null && result.getPolicyVersionNumber() > 0) {
            return Objects.equals(policyId, result.getPolicyId())
                    && result.getPolicyVersionNumber() == versionNumber;
        }
        return Objects.equals(policyId + "_v" + versionNumber, result.getPolicyVersionId());
    }

    private static Boolean latestDecision(List<EvaluationResult> results) {
        LocalDateTime latestTimestamp = results.stream()
                .map(EvaluationResult::getEvaluatedOn)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        List<EvaluationResult> possibleLatest = results.stream()
                .filter(result -> latestTimestamp == null
                        || result.getEvaluatedOn() == null
                        || latestTimestamp.equals(result.getEvaluatedOn()))
                .toList();
        Boolean decision = null;
        for (EvaluationResult result : possibleLatest) {
            Boolean candidate = result.getDecisionOutcome();
            if (candidate == null || decision != null && !decision.equals(candidate)) {
                return null;
            }
            decision = candidate;
        }
        return decision;
    }
}
