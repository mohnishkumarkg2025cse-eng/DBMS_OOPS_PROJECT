package com.ruleweaver.service;

import com.ruleweaver.model.ConditionResult;
import com.ruleweaver.model.EvaluationResult;
import com.ruleweaver.model.Policy;
import com.ruleweaver.model.PolicyVersion;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class DashboardAnalytics {
    public static final String LEGACY_RUN_ID = "__legacy_untracked__";

    public enum DecisionFilter {
        ALL,
        ELIGIBLE,
        REJECTED
    }

    public record Filters(String programId, String policyId, Integer versionNumber,
                          String evaluationRunId, DecisionFilter decision) {
        public Filters {
            decision = decision == null ? DecisionFilter.ALL : decision;
        }

        public static Filters all() {
            return new Filters(null, null, null, null, DecisionFilter.ALL);
        }
    }

    public record RunOption(String id, LocalDateTime latestEvaluation, int applicantCount) {
        public boolean isLegacy() {
            return LEGACY_RUN_ID.equals(id);
        }
    }

    public record Funnel(int evaluated, int eligible, int rejected, int nearMisses,
                         int rejectedWithRuleDetails) { }

    public record RuleImpact(String ruleName, int passedApplicants,
                             int failedApplicants, int assessedApplicants) { }

    public record DailyDecisionCount(LocalDate date, int approved, int rejected) { }

    public record Baseline(Integer versionNumber, int sharedApplicants, int eligibleApplicants,
                           int changedDecisions, Double passRate,
                           int currentEligibleApplicants, Double currentPassRate) {
        public boolean isAvailable() {
            return versionNumber != null && sharedApplicants > 0;
        }
    }

    public record Analysis(List<EvaluationResult> evaluations, Funnel funnel,
                           List<RuleImpact> ruleImpacts, List<RuleImpact> rejectionReasons,
                           Baseline baseline) {
        public Analysis {
            evaluations = List.copyOf(evaluations);
            ruleImpacts = List.copyOf(ruleImpacts);
            rejectionReasons = List.copyOf(rejectionReasons);
        }
    }

    public List<RunOption> findRuns(List<EvaluationResult> evaluations, List<Policy> policies,
                                   Filters filters) {
        filters = filters == null ? Filters.all() : filters;
        List<EvaluationResult> matching = filterScope(evaluations, policies, filters, false);
        Map<String, List<EvaluationResult>> byRun = matching.stream().collect(Collectors.groupingBy(
                result -> result.getEvaluationRunId() == null
                        ? LEGACY_RUN_ID : result.getEvaluationRunId(), LinkedHashMap::new, Collectors.toList()));
        return byRun.entrySet().stream()
                .map(entry -> new RunOption(entry.getKey(),
                        entry.getValue().stream().map(EvaluationResult::getEvaluatedOn)
                                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null),
                        latestApplicants(entry.getValue(), !LEGACY_RUN_ID.equals(entry.getKey())).size()))
                .sorted(Comparator.comparing(RunOption::latestEvaluation,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RunOption::id))
                .toList();
    }

    public Analysis analyze(List<EvaluationResult> evaluations, List<Policy> policies, Filters filters) {
        Filters selected = filters == null ? Filters.all() : filters;
        List<EvaluationResult> selectedCohort = latestApplicants(
                filterScope(evaluations, policies, selected, true),
                selected.policyId() != null || selected.evaluationRunId() != null);
        List<EvaluationResult> current = selectedCohort;
        if (selected.decision() == DecisionFilter.ELIGIBLE) {
            current = current.stream().filter(EvaluationResult::isOverallDecision).toList();
        } else if (selected.decision() == DecisionFilter.REJECTED) {
            current = current.stream().filter(result -> !result.isOverallDecision()).toList();
        }

        int eligible = (int) current.stream().filter(EvaluationResult::isOverallDecision).count();
        int rejected = current.size() - eligible;
        int rejectedWithDetails = (int) current.stream()
                .filter(result -> !result.isOverallDecision() && !result.getConditionResults().isEmpty())
                .count();
        int nearMisses = (int) current.stream()
                .filter(result -> !result.isOverallDecision() && failedConditionCount(result) == 1)
                .count();
        Funnel funnel = new Funnel(current.size(), eligible, rejected, nearMisses, rejectedWithDetails);
        List<EvaluationResult> rejectedResults = current.stream()
                .filter(result -> !result.isOverallDecision()).toList();
        return new Analysis(current, funnel, buildRuleImpacts(current), buildRuleImpacts(rejectedResults),
                buildBaseline(evaluations, policies, selected, selectedCohort));
    }

    public List<DailyDecisionCount> dailyDecisionCounts(List<EvaluationResult> evaluations,
                                                        LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)
                || evaluations == null || evaluations.isEmpty()) {
            return List.of();
        }
        Map<LocalDate, int[]> countsByDate = new TreeMap<>();
        for (EvaluationResult result : evaluations) {
            if (result == null || !result.hasDecisionOutcome() || result.getEvaluatedOn() == null) {
                continue;
            }
            LocalDate date = result.getEvaluatedOn().toLocalDate();
            if (date.isBefore(startDate) || date.isAfter(endDate)) {
                continue;
            }
            int[] counts = countsByDate.computeIfAbsent(date, ignored -> new int[2]);
            counts[result.isOverallDecision() ? 0 : 1]++;
        }
        return countsByDate.entrySet().stream()
                .map(entry -> new DailyDecisionCount(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private static List<EvaluationResult> filterScope(List<EvaluationResult> source, List<Policy> policies,
                                                       Filters filters, boolean includeRun) {
        if (source == null || source.isEmpty()) return List.of();
        Map<String, Policy> byId = new HashMap<>();
        if (policies != null) {
            policies.stream().filter(Objects::nonNull)
                    .forEach(policy -> byId.put(policy.getId(), policy));
        }
        return source.stream().filter(Objects::nonNull)
                .filter(result -> filters.policyId() == null
                        || Objects.equals(filters.policyId(), result.getPolicyId()))
                .filter(result -> filters.versionNumber() == null
                        || filters.versionNumber() == result.getPolicyVersionNumber())
                .filter(result -> filters.programId() == null
                        || byId.containsKey(result.getPolicyId())
                        && Objects.equals(filters.programId(), byId.get(result.getPolicyId()).getProgramId()))
                .filter(result -> matchesRun(result, filters, includeRun))
                .toList();
    }

    private static boolean matchesRun(EvaluationResult result, Filters filters, boolean includeRun) {
        if (!includeRun || filters.evaluationRunId() == null) return true;
        if (LEGACY_RUN_ID.equals(filters.evaluationRunId())) {
            return result.getEvaluationRunId() == null;
        }
        return Objects.equals(filters.evaluationRunId(), result.getEvaluationRunId());
    }

    private static List<EvaluationResult> latestApplicants(List<EvaluationResult> source, boolean policyScoped) {
        Map<ApplicantKey, EvaluationResult> latest = new LinkedHashMap<>();
        int unkeyed = 0;
        for (EvaluationResult result : source) {
            String applicantId = result.getApplicantId();
            ApplicantKey key = applicantId == null
                    ? new ApplicantKey(policyScoped ? result.getPolicyId() : null, null, result.getId() == null
                            ? "unkeyed-" + unkeyed++ : result.getId())
                    : new ApplicantKey(policyScoped ? result.getPolicyId() : null, applicantId, null);
            latest.merge(key, result, DashboardAnalytics::laterEvaluation);
        }
        return List.copyOf(latest.values());
    }

    private static EvaluationResult laterEvaluation(EvaluationResult current, EvaluationResult candidate) {
        LocalDateTime currentTime = current.getEvaluatedOn();
        LocalDateTime candidateTime = candidate.getEvaluatedOn();
        if (currentTime == null) return candidate;
        return candidateTime == null || candidateTime.isBefore(currentTime) ? current : candidate;
    }

    private static List<RuleImpact> buildRuleImpacts(List<EvaluationResult> results) {
        Map<String, Map<String, Boolean>> outcomesByRuleAndApplicant = new HashMap<>();
        int unkeyed = 0;
        for (EvaluationResult result : results) {
            String applicantKey = result.getPolicyId() + "\u0000"
                    + (result.getApplicantId() == null
                    ? "unkeyed-" + unkeyed++ : result.getApplicantId());
            for (ConditionResult condition : result.getConditionResults()) {
                String ruleName = Objects.requireNonNullElse(condition.getConditionName(), "Unnamed rule");
                outcomesByRuleAndApplicant.computeIfAbsent(ruleName, ignored -> new HashMap<>())
                        .merge(applicantKey, condition.isPassed(), (previous, next) -> previous && next);
            }
        }
        return outcomesByRuleAndApplicant.entrySet().stream()
                .map(entry -> {
                    int failed = (int) entry.getValue().values().stream().filter(passed -> !passed).count();
                    int assessed = entry.getValue().size();
                    return new RuleImpact(entry.getKey(), assessed - failed, failed, assessed);
                })
                .sorted(Comparator.comparingInt(RuleImpact::failedApplicants).reversed()
                        .thenComparing(RuleImpact::ruleName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static Baseline buildBaseline(List<EvaluationResult> allResults, List<Policy> policies,
                                          Filters filters, List<EvaluationResult> current) {
        if (filters.policyId() == null || filters.versionNumber() == null || policies == null) {
            return new Baseline(null, 0, 0, 0, null, 0, null);
        }
        Policy policy = policies.stream().filter(Objects::nonNull)
                .filter(candidate -> Objects.equals(candidate.getId(), filters.policyId()))
                .findFirst().orElse(null);
        if (policy == null || policy.getVersion(filters.versionNumber()).isEmpty()) {
            return new Baseline(null, 0, 0, 0, null, 0, null);
        }
        PolicyVersion previousVersion = policy.getVersions().stream()
                .filter(version -> version.getVersionNumber() < filters.versionNumber())
                .max(Comparator.comparingInt(PolicyVersion::getVersionNumber)).orElse(null);
        if (previousVersion == null) return new Baseline(null, 0, 0, 0, null, 0, null);

        Set<String> currentApplicants = current.stream().map(EvaluationResult::getApplicantId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        List<EvaluationResult> previous = allResults == null ? List.of() : allResults.stream()
                .filter(result -> Objects.equals(filters.policyId(), result.getPolicyId()))
                .filter(result -> result.getPolicyVersionNumber() == previousVersion.getVersionNumber())
                .filter(result -> currentApplicants.contains(result.getApplicantId()))
                .toList();
        Map<String, EvaluationResult> previousByApplicant = latestApplicants(previous, true).stream()
                .filter(result -> result.getApplicantId() != null)
                .collect(Collectors.toMap(EvaluationResult::getApplicantId, result -> result, (left, right) -> left));
        int shared = 0;
        int eligible = 0;
        int currentEligible = 0;
        int changed = 0;
        Map<String, EvaluationResult> currentByApplicant = current.stream()
                .filter(result -> result.getApplicantId() != null)
                .collect(Collectors.toMap(EvaluationResult::getApplicantId, result -> result, (left, right) -> left));
        for (Map.Entry<String, EvaluationResult> entry : currentByApplicant.entrySet()) {
            EvaluationResult prior = previousByApplicant.get(entry.getKey());
            if (prior == null) continue;
            shared++;
            if (prior.isOverallDecision()) eligible++;
            if (entry.getValue().isOverallDecision()) currentEligible++;
            if (prior.isOverallDecision() != entry.getValue().isOverallDecision()) changed++;
        }
        Double rate = shared == 0 ? null : eligible * 100.0 / shared;
        Double currentRate = shared == 0 ? null : currentEligible * 100.0 / shared;
        return new Baseline(previousVersion.getVersionNumber(), shared, eligible,
                changed, rate, currentEligible, currentRate);
    }

    private static int failedConditionCount(EvaluationResult result) {
        return (int) result.getConditionResults().stream().filter(condition -> !condition.isPassed()).count();
    }

    private record ApplicantKey(String policyId, String applicantId, String fallbackId) { }
}
