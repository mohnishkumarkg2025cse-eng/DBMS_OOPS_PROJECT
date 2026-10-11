package com.ruleweaver.service;

import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.model.*;
import com.ruleweaver.repository.PolicyRepository;
import com.ruleweaver.repository.EvaluationResultRepository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

public class PolicyService {

    private final Object policyRepository;
    private final Object evaluationResultRepository;
    private final DecisionEngine decisionEngine;
    private final ComparisonService comparisonService;

    public PolicyService(PolicyRepository policyRepository,
                         Object evaluationResultRepository,
                         DecisionEngine decisionEngine,
                         ComparisonService comparisonService) {
        this((Object) policyRepository, evaluationResultRepository, decisionEngine, comparisonService);
    }

    public PolicyService(Object policyRepository,
                         Object evaluationResultRepository,
                         DecisionEngine decisionEngine,
                         ComparisonService comparisonService) {
        this.policyRepository = policyRepository;
        this.evaluationResultRepository = evaluationResultRepository;
        this.decisionEngine = decisionEngine;
        this.comparisonService = comparisonService;
    }

    public Policy createPolicy(String id, String name, String description, List<Condition> conditions) {
        Policy policy = new Policy(id, name, description);
        policy.setActiveVersionNumber(1);
        savePolicy(policy);
        PolicyVersion initialVersion = new PolicyVersion(id + "_v1", id, 1, description, conditions);
        savePolicyVersion(id, initialVersion);
        return findPolicyById(id);
    }

    public PolicyVersion createNewVersion(String policyId, String description, List<Condition> conditions) {
        Policy policy = findPolicyById(policyId);
        int nextVersionNumber = policy.getLatestVersion()
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
        PolicyVersion newVersion = new PolicyVersion(
                policyId + "_v" + nextVersionNumber, policyId, nextVersionNumber, description, conditions);
        newVersion.setConditions(conditions);
        savePolicyVersion(policyId, newVersion);
        savePolicy(policy);
        return newVersion;
    }

    public List<EvaluationResult> evaluateApplicants(String policyId, int versionNumber, List<Applicant> applicants) {
        return evaluateApplicants(policyId, versionNumber, applicants, ignored -> { }, () -> false);
    }

    public List<EvaluationResult> evaluateApplicants(String policyId, int versionNumber, List<Applicant> applicants,
                                                     IntConsumer progress, BooleanSupplier cancelled) {
        Policy policy = findPolicyById(policyId);
        PolicyVersion version = policy.getVersion(versionNumber)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionNumber));

        List<Applicant> pool = applicants == null ? List.of() : applicants;
        List<EvaluationResult> results = new java.util.ArrayList<>(pool.size());
        for (int i = 0; i < pool.size(); i++) {
            if (cancelled.getAsBoolean()) {
                throw new CancellationException("Evaluation cancelled.");
            }
            results.addAll(evaluateApplicantsInternal(List.of(pool.get(i)), version));
            progress.accept(i + 1);
        }
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Evaluation cancelled.");
        }
        persistEvaluationResults(results);
        return results;
    }

    public Policy setActiveVersion(String policyId, int versionNumber) {
        Policy policy = findPolicyById(policyId);
        if (policy.getVersion(versionNumber).isEmpty()) {
            throw new IllegalArgumentException("Version not found: " + versionNumber);
        }
        policy.setActiveVersionNumber(versionNumber);
        savePolicy(policy);
        return findPolicyById(policyId);
    }

    public ComparisonReport compareVersions(String policyId, int v1, int v2, List<Applicant> applicants) {
        Policy policy = findPolicyById(policyId);
        PolicyVersion version1 = policy.getVersion(v1)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + v1));
        PolicyVersion version2 = policy.getVersion(v2)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + v2));

        return compare(policyId, version1, version2, applicants);
    }

    private Policy findPolicyById(String policyId) {
        Object result = invokeMethod(policyRepository, "findById", policyId);
        if (result == null) {
            throw new IllegalArgumentException("Policy not found: " + policyId);
        }

        if (result instanceof Optional<?>) {
            Optional<?> optional = (Optional<?>) result;
            result = optional.orElse(null);
        }

        if (result instanceof Policy) {
            return (Policy) result;
        }

        Policy converted = convertPolicy(result);
        if (converted != null) {
            return converted;
        }

        throw new IllegalArgumentException("Policy not found: " + policyId);
    }

    private void savePolicy(Policy policy) {
        Object repositoryValue = convertToRepositoryValue(policy);
        invokeMethod(policyRepository, "save", repositoryValue);
    }

    private void savePolicyVersion(String policyId, PolicyVersion version) {
        if (policyRepository instanceof PolicyRepository repository) {
            repository.saveVersion(policyId, version);
            return;
        }
        invokeMethod(policyRepository, "saveVersion", policyId, version);
    }

    private void persistEvaluationResults(List<EvaluationResult> results) {
        if (results.isEmpty() || evaluationResultRepository == null) {
            return;
        }
        if (evaluationResultRepository instanceof EvaluationResultRepository repository) {
            repository.saveAll(results);
            return;
        }
        for (EvaluationResult result : results) {
            invokeMethod(evaluationResultRepository, "save", result);
        }
    }

    @SuppressWarnings("unchecked")
    private List<EvaluationResult> evaluateApplicantsInternal(List<Applicant> applicants, PolicyVersion version) {
        Object result = null;

        for (Method method : decisionEngine.getClass().getMethods()) {
            if (!"evaluateAll".equals(method.getName())) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 2) {
                continue;
            }

            Object[] args = new Object[2];
            if (paramTypes[0].isAssignableFrom(List.class) && paramTypes[1].isAssignableFrom(PolicyVersion.class)) {
                args[0] = applicants;
                args[1] = version;
            } else if (paramTypes[0].isAssignableFrom(List.class) && List.class.isAssignableFrom(paramTypes[1])) {
                args[0] = applicants;
                args[1] = version.getConditions();
            } else {
                continue;
            }

            try {
                result = method.invoke(decisionEngine, args);
                break;
            } catch (Exception ignored) {
                // retry with next compatible overload
            }
        }

        if (result == null) {
            throw new IllegalStateException("No compatible evaluateAll method found on DecisionEngine");
        }
        return (List<EvaluationResult>) result;
    }

    private ComparisonReport compare(String policyId, PolicyVersion version1, PolicyVersion version2, List<Applicant> applicants) {
        if (comparisonService != null) {
            return comparisonService.compareVersions(applicants, version1, version2);
        }
        Object result = null;

        Object[] orderedCandidates = new Object[] {
                policyId,
                version1,
                version2,
                applicants
        };
        Object[] altCandidates = new Object[] {
                version1,
                version2,
                applicants
        };
        Object[] altCandidates2 = new Object[] {
                applicants,
                version1,
                version2
        };

        for (Method method : comparisonService.getClass().getMethods()) {
            if (!"compare".equals(method.getName())) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length == 4) {
                if (isCompatible(paramTypes[0], policyId) && isCompatible(paramTypes[1], version1)
                        && isCompatible(paramTypes[2], version2) && isCompatible(paramTypes[3], applicants)) {
                    try {
                        result = method.invoke(comparisonService, orderedCandidates);
                        if (result != null) {
                            return (ComparisonReport) result;
                        }
                    } catch (Exception ignored) {
                        // continue
                    }
                }
            } else if (paramTypes.length == 3) {
                if (isCompatible(paramTypes[0], version1) && isCompatible(paramTypes[1], version2) && isCompatible(paramTypes[2], applicants)) {
                    try {
                        result = method.invoke(comparisonService, altCandidates);
                        if (result != null) {
                            return (ComparisonReport) result;
                        }
                    } catch (Exception ignored) {
                        // continue
                    }
                }
                if (isCompatible(paramTypes[0], applicants) && isCompatible(paramTypes[1], version1) && isCompatible(paramTypes[2], version2)) {
                    try {
                        result = method.invoke(comparisonService, altCandidates2);
                        if (result != null) {
                            return (ComparisonReport) result;
                        }
                    } catch (Exception ignored) {
                        // continue
                    }
                }
            }
        }

        throw new IllegalStateException("No compatible compare method found on ComparisonService");
    }

    private Object convertToRepositoryValue(Object value) {
        if (value == null) {
            return null;
        }

        for (String methodName : new String[] { "toDocument", "toMap", "asDocument" }) {
            try {
                Method method = value.getClass().getMethod(methodName);
                Object converted = method.invoke(value);
                if (converted != null) {
                    return converted;
                }
            } catch (Exception ignored) {
                // ignore and try next conversion strategy
            }
        }

        return value;
    }

    private Policy convertPolicy(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Policy) {
            return (Policy) value;
        }

        try {
            Method method = value.getClass().getMethod("toPolicy");
            Object result = method.invoke(value);
            if (result instanceof Policy) {
                return (Policy) result;
            }
        } catch (Exception ignored) {
            // ignored
        }

        try {
            Method method = Policy.class.getMethod("fromDocument", value.getClass());
            Object result = method.invoke(null, value);
            if (result instanceof Policy) {
                return (Policy) result;
            }
        } catch (Exception ignored) {
            // ignored
        }

        try {
            Constructor<?> ctor = Policy.class.getConstructor(value.getClass());
            Object result = ctor.newInstance(value);
            if (result instanceof Policy) {
                return (Policy) result;
            }
        } catch (Exception ignored) {
            // ignored
        }

        return null;
    }

    private Object invokeMethod(Object target, String methodName, Object... args) {
        if (target == null) {
            return null;
        }

        for (Method method : target.getClass().getMethods()) {
            if (!methodName.equals(method.getName()) || method.getParameterCount() != args.length) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] values = new Object[args.length];
            boolean matches = true;
            for (int i = 0; i < args.length; i++) {
                values[i] = args[i];
                if (!isCompatible(paramTypes[i], args[i])) {
                    matches = false;
                    break;
                }
            }
            if (!matches) {
                continue;
            }
            try {
                return method.invoke(target, values);
            } catch (Exception ignored) {
                // continue to next possible overload
            }
        }

        return null;
    }

    private boolean isCompatible(Class<?> targetType, Object value) {
        if (value == null) {
            return !targetType.isPrimitive();
        }

        Class<?> sourceType = value.getClass();
        if (targetType.isAssignableFrom(sourceType)) {
            return true;
        }

        if (targetType == Object.class) {
            return true;
        }

        if (targetType == String.class && value instanceof String) {
            return true;
        }

        if (targetType == int.class && value instanceof Integer) {
            return true;
        }

        if (targetType == long.class && value instanceof Long) {
            return true;
        }

        if (targetType == boolean.class && value instanceof Boolean) {
            return true;
        }

        if (targetType == double.class && value instanceof Double) {
            return true;
        }

        if (targetType == float.class && value instanceof Float) {
            return true;
        }

        if (targetType == short.class && value instanceof Short) {
            return true;
        }

        if (targetType == byte.class && value instanceof Byte) {
            return true;
        }

        if (targetType == char.class && value instanceof Character) {
            return true;
        }

        if (targetType.isAssignableFrom(List.class) && value instanceof List<?>) {
            return true;
        }

        return false;
    }
}
