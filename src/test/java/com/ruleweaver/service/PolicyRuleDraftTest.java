package com.ruleweaver.service;

import com.ruleweaver.engine.CompositeCondition;
import com.ruleweaver.engine.Condition;
import com.ruleweaver.engine.DecisionEngine;
import com.ruleweaver.engine.LogicalOperator;
import com.ruleweaver.engine.FieldCondition;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.FieldDefinition;
import com.ruleweaver.model.FieldOperator;
import com.ruleweaver.model.FieldRegistry;
import com.ruleweaver.model.FieldType;
import com.ruleweaver.model.FieldValidationRules;
import com.ruleweaver.model.PolicyVersion;
import com.ruleweaver.model.ProgramTemplate;
import com.ruleweaver.repository.InMemoryEvaluationResultRepository;
import com.ruleweaver.repository.InMemoryPolicyRepository;
import com.ruleweaver.repository.InMemoryProgramRepository;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PolicyRuleDraftTest {
    private final FieldRegistry registry = new FieldRegistry(List.of(
            new FieldDefinition("portfolioScore", "Portfolio score", FieldType.NUMBER, true,
                    new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null),
            new FieldDefinition("status", "Application status", FieldType.TEXT, false,
                    FieldValidationRules.none(), List.of("active", "priority"), null),
            new FieldDefinition("hasReferral", "Has referral", FieldType.BOOLEAN, false,
                    FieldValidationRules.none(), List.of(), null),
            new FieldDefinition("skills", "Skills", FieldType.MULTI_SELECT, false,
                    FieldValidationRules.none(), List.of("Java", "SQL"),
                    EnumSet.of(FieldOperator.CONTAINS, FieldOperator.NOT_CONTAINS,
                            FieldOperator.CONTAINS_ANY, FieldOperator.CONTAINS_ALL))));

    @Test
    void nestedAndOrDraftProducesDecisionEngineCompositeTree() {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        draft.root().children().add(new PolicyRuleDraft.Rule(
                "portfolioScore", FieldOperator.GREATER_THAN_OR_EQUAL, "80"));
        PolicyRuleDraft.Group alternatives = new PolicyRuleDraft.Group(LogicalOperator.OR);
        alternatives.children().add(new PolicyRuleDraft.Rule("status", FieldOperator.EQUALS, "priority"));
        PolicyRuleDraft.Group conjunction = new PolicyRuleDraft.Group(LogicalOperator.AND);
        conjunction.children().add(new PolicyRuleDraft.Rule("hasReferral", FieldOperator.EQUALS, "true"));
        conjunction.children().add(new PolicyRuleDraft.Rule("skills", FieldOperator.CONTAINS, "Java"));
        alternatives.children().add(conjunction);
        draft.root().children().add(alternatives);

        List<Condition> conditions = draft.buildConditions(registry);
        assertInstanceOf(CompositeCondition.class, conditions.get(1));
        DecisionEngine engine = new DecisionEngine();
        PolicyVersion version = new PolicyVersion("policy_v1", "policy", 1, "draft", conditions);
        Applicant priorityApplicant = applicant("A-1", 90, "priority", false, List.of());
        Applicant referredApplicant = applicant("A-2", 85, "active", true, List.of("Java"));
        Applicant rejectedApplicant = applicant("A-3", 85, "active", false, List.of());

        assertTrue(engine.evaluate(priorityApplicant, version).isOverallDecision());
        assertTrue(engine.evaluate(referredApplicant, version).isOverallDecision());
        assertFalse(engine.evaluate(rejectedApplicant, version).isOverallDecision());
    }

    @Test
    void validationReportsEmptyGroupsUnknownFieldsInvalidOperatorsAndBadValues() {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        PolicyRuleDraft.Group empty = new PolicyRuleDraft.Group(LogicalOperator.AND);
        draft.root().children().add(empty);
        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("cannot be empty")));

        empty.children().add(new PolicyRuleDraft.Rule("missingField", FieldOperator.EQUALS, "x"));
        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("Unknown field")));

        empty.children().clear();
        empty.children().add(new PolicyRuleDraft.Rule("portfolioScore", FieldOperator.CONTAINS, "not-number"));
        List<String> errors = draft.validate(registry);
        assertTrue(errors.stream().anyMatch(error -> error.contains("not supported")));

        empty.children().clear();
        empty.children().add(new PolicyRuleDraft.Rule("portfolioScore",
                FieldOperator.GREATER_THAN, "not-number"));
        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("valid number")));

        empty.children().clear();
        empty.children().add(new PolicyRuleDraft.Rule("portfolioScore",
                FieldOperator.GREATER_THAN, "101"));
        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("at most 100")));
    }

    @Test
    void fieldTypesConvertToTypedConditionValuesAndValidate() {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        draft.root().children().add(new PolicyRuleDraft.Rule("status", FieldOperator.EQUALS, "priority"));
        draft.root().children().add(new PolicyRuleDraft.Rule("hasReferral", FieldOperator.EQUALS, "false"));
        draft.root().children().add(new PolicyRuleDraft.Rule("skills", FieldOperator.CONTAINS_ANY, "Java, SQL"));

        List<Condition> conditions = draft.buildConditions(registry);
        assertTrue(conditions.stream().allMatch(com.ruleweaver.engine.FieldCondition.class::isInstance));
        com.ruleweaver.engine.FieldCondition booleanCondition =
                (com.ruleweaver.engine.FieldCondition) conditions.get(1);
        assertEquals(Boolean.FALSE, booleanCondition.getExpectedValue());
        com.ruleweaver.engine.FieldCondition multiSelect =
                (com.ruleweaver.engine.FieldCondition) conditions.get(2);
        assertEquals(List.of("Java", "SQL"), multiSelect.getExpectedValue());

        draft.root().children().add(new PolicyRuleDraft.Rule("skills", FieldOperator.CONTAINS, "Kotlin"));
        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("not permitted")));
    }

    @Test
    void naturalLanguageAndReadonlyTreeRepresentSameNestedRulesAsBuiltConditions() {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        PolicyRuleDraft.Group either = new PolicyRuleDraft.Group(LogicalOperator.OR);
        either.children().add(new PolicyRuleDraft.Rule("status", FieldOperator.EQUALS, "priority"));
        either.children().add(new PolicyRuleDraft.Rule("hasReferral", FieldOperator.EQUALS, "true"));
        draft.root().children().add(either);

        String natural = draft.naturalLanguage(registry);
        String tree = draft.readOnlyTree(registry);
        List<Condition> conditions = draft.buildConditions(registry);

        assertTrue(natural.contains("Application status is priority"));
        assertTrue(natural.contains("Has referral is true"));
        assertTrue(tree.contains("AND\n  OR"));
        assertEquals("CompositeCondition", conditions.get(0).getType());
        assertEquals("OR", ((CompositeCondition) conditions.get(0)).getLogicalOperator().name());
        assertTrue(conditions.get(0).getMessage().contains("OR group"));
    }

    @Test
    void importingSavedConditionsCreatesAnIsolatedEditableDraft() {
        FieldCondition savedRule = new FieldCondition(
                "portfolioScore", FieldOperator.GREATER_THAN_OR_EQUAL, 80.0);
        CompositeCondition savedGroup = new CompositeCondition("OR group", LogicalOperator.OR,
                List.of(savedRule));
        PolicyRuleDraft draft = PolicyRuleDraft.fromConditions(List.of(savedGroup));
        PolicyRuleDraft.Group draftGroup = (PolicyRuleDraft.Group) draft.root().children().get(0);
        PolicyRuleDraft.Rule draftRule = (PolicyRuleDraft.Rule) draftGroup.children().get(0);
        draftRule.setValue("60");

        List<Condition> changed = draft.buildConditions(registry);
        CompositeCondition changedGroup = (CompositeCondition) changed.get(0);
        FieldCondition changedRule = (FieldCondition) changedGroup.getSubconditions().get(0);

        assertEquals(80.0, savedRule.getExpectedValue());
        assertEquals(60.0, changedRule.getExpectedValue());
        assertEquals(80.0, ((FieldCondition) savedGroup.getSubconditions().get(0)).getExpectedValue());
    }

    @Test
    void duplicateRulesAreRejectedAcrossNestedGroups() {
        PolicyRuleDraft draft = new PolicyRuleDraft();
        PolicyRuleDraft.Group first = new PolicyRuleDraft.Group(LogicalOperator.AND);
        first.children().add(new PolicyRuleDraft.Rule("status", FieldOperator.EQUALS, "priority"));
        PolicyRuleDraft.Group second = new PolicyRuleDraft.Group(LogicalOperator.OR);
        second.children().add(new PolicyRuleDraft.Rule("status", FieldOperator.EQUALS, "priority"));
        draft.root().children().addAll(List.of(first, second));

        assertTrue(draft.validate(registry).stream().anyMatch(error -> error.contains("duplicates another")));
    }

    @Test
    void invalidProgramDraftDoesNotPersistPolicy() {
        InMemoryPolicyRepository policies = new InMemoryPolicyRepository();
        ProgramService programs = new ProgramService(new InMemoryProgramRepository());
        programs.createProgram("program-1", "Custom", ProgramTemplate.CUSTOM, List.of(
                new FieldDefinition("portfolioScore", "Portfolio score", FieldType.NUMBER, true,
                        new FieldValidationRules(0.0, 100.0, null, null, null), List.of(), null)));
        PolicyService policyService = new PolicyService(policies, new InMemoryEvaluationResultRepository(),
                new DecisionEngine(), new ComparisonService(new DecisionEngine()));
        policyService.setProgramService(programs);

        assertThrows(IllegalArgumentException.class, () -> policyService.createPolicy("policy-1",
                "Invalid draft", "Must not be saved", "program-1",
                List.of(new com.ruleweaver.engine.FieldCondition(
                        "portfolioScore", FieldOperator.CONTAINS, 50))));
        assertTrue(policies.findById("policy-1").isEmpty());
    }

    @Test
    void draftImpactComparesBaselineAndDraftOnTheSameApplicantSample() {
        DecisionEngine engine = new DecisionEngine();
        PolicyDraftImpactService service = new PolicyDraftImpactService(engine);
        PolicyVersion baseline = new PolicyVersion("policy_v1", "policy", 1, "saved",
                List.of(new FieldCondition("portfolioScore", FieldOperator.GREATER_THAN_OR_EQUAL, 80.0)));
        List<Applicant> sample = List.of(
                applicant("A-1", 75, "active", false, List.of()),
                applicant("A-2", 50, "active", false, List.of()));

        PolicyDraftImpactService.ImpactSummary summary = service.compare(baseline,
                List.of(new FieldCondition("portfolioScore", FieldOperator.GREATER_THAN_OR_EQUAL, 60.0)),
                sample);

        assertEquals(2, summary.sampleSize());
        assertEquals(0, summary.baselineEligible());
        assertEquals(1, summary.draftEligible());
        assertEquals(1, summary.eligibleChange());
    }

    private static Applicant applicant(String id, double score, String status,
                                       boolean referral, List<String> skills) {
        Applicant applicant = new Applicant(id, id, 0, 0, skills);
        applicant.setCustomAttribute("portfolioScore", score);
        applicant.setCustomAttribute("status", status);
        applicant.setCustomAttribute("hasReferral", referral);
        return applicant;
    }
}
