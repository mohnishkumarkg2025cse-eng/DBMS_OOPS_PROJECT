package com.ruleweaver.demo;

import com.ruleweaver.engine.*;
import com.ruleweaver.model.*;
import com.ruleweaver.service.ComparisonService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Standalone Automated Test Runner.
 * Executes all core test suites defined in Section J of the PDF specification
 * and verifies assertions programmatically without requiring external test harnesses.
 */
public class RuleWeaverTestRunner {

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println(">>> RUNNING RULEWEAVER AUTOMATED TEST SUITE (Section J Specs)...\n");

        runNumericConditionTests();
        runSkillConditionTests();
        runCompositeConditionTests();
        runDecisionEngineTests();
        runComparisonServiceTests();

        System.out.println("\n------------------------------------------------------------");
        System.out.printf("TEST RESULTS: Total: %d | Passed: %d | Failed: %d\n", testsRun, testsPassed, testsFailed);
        System.out.println("------------------------------------------------------------");

        if (testsFailed > 0) {
            System.err.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! ✓");
        }
    }

    private static void runNumericConditionTests() {
        System.out.println("[Suite: NumericCondition]");
        Applicant app = new Applicant("T1", "Student", 8.0, 75, Collections.singletonList("Java"));

        // >= 8.0 should be true
        NumericCondition c1 = new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0);
        assertCondition(c1.test(app) == true, "GPA 8.0 satisfies >= 8.0");

        // > 8.0 should be false
        NumericCondition c2 = new NumericCondition("gpa", Operator.GREATER_THAN, 8.0);
        assertCondition(c2.test(app) == false, "GPA 8.0 fails > 8.0");

        // Missing field returns false safely
        NumericCondition c3 = new NumericCondition("nonExistent", Operator.GREATER_THAN_OR_EQUAL, 10.0);
        assertCondition(c3.test(app) == false, "Missing field handled safely as false");

        // Null applicant returns false safely
        assertCondition(c1.test(null) == false, "Null applicant handled safely as false");
    }

    private static void runSkillConditionTests() {
        System.out.println("\n[Suite: SkillCondition]");
        Applicant alice = new Applicant("T2", "Alice", 8.5, 80, Arrays.asList("Java", "SQL"));

        SkillCondition s1 = new SkillCondition("java"); // Case insensitive
        assertCondition(s1.test(alice) == true, "Case-insensitive skill 'java' passes");

        SkillCondition s2 = new SkillCondition("Python");
        assertCondition(s2.test(alice) == false, "Missing skill 'Python' fails");

        Applicant emptySkills = new Applicant("T3", "Empty", 8.0, 70, Collections.emptyList());
        assertCondition(s1.test(emptySkills) == false, "Empty skill set fails safely");
    }

    private static void runCompositeConditionTests() {
        System.out.println("\n[Suite: CompositeCondition - Composite Pattern]");
        Applicant applicant = new Applicant("T4", "Candidate", 8.5, 75, Arrays.asList("Java", "Docker"));

        // AND group
        CompositeCondition andGroup = new CompositeCondition("AND Group", LogicalOperator.AND);
        andGroup.addSubcondition(new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0));
        andGroup.addSubcondition(new SkillCondition("Java"));
        assertCondition(andGroup.test(applicant) == true, "AND group passes when all sub-conditions pass");

        andGroup.addSubcondition(new SkillCondition("Rust")); // Fails
        assertCondition(andGroup.test(applicant) == false, "AND group fails when one sub-condition fails");

        // OR group
        CompositeCondition orGroup = new CompositeCondition("OR Group", LogicalOperator.OR);
        orGroup.addSubcondition(new SkillCondition("Rust")); // Fails
        orGroup.addSubcondition(new SkillCondition("Java")); // Passes
        assertCondition(orGroup.test(applicant) == true, "OR group passes when at least one sub-condition passes");
    }

    private static void runDecisionEngineTests() {
        System.out.println("\n[Suite: DecisionEngine - Polymorphic Rule Evaluation]");
        DecisionEngine engine = new DecisionEngine();

        PolicyVersion pv1 = new PolicyVersion("PV-1", "POL-TEST", 1, "Internship Criteria", Arrays.asList(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                new SkillCondition("Java")
        ));

        Applicant carol = new Applicant("T5", "Carol", 8.5, 72, Arrays.asList("Java", "C++"));
        EvaluationResult resCarol = engine.evaluate(carol, pv1);
        assertCondition(resCarol.isOverallDecision() == true, "Carol passes all criteria -> Eligible");

        Applicant alice = new Applicant("T6", "Alice", 8.2, 65, Collections.singletonList("Java"));
        EvaluationResult resAlice = engine.evaluate(alice, pv1);
        assertCondition(resAlice.isOverallDecision() == false, "Alice fails interview score -> Not Eligible");
        assertCondition(resAlice.getFailedConditions().size() == 1, "Alice has exactly 1 failed condition");
    }

    private static void runComparisonServiceTests() {
        System.out.println("\n[Suite: ComparisonService - Decision Time Machine]");
        DecisionEngine engine = new DecisionEngine();
        ComparisonService service = new ComparisonService(engine);

        PolicyVersion v1 = new PolicyVersion("PV-1", "POL-TEST", 1, "v1", Arrays.asList(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                new SkillCondition("Java")
        ));

        PolicyVersion v2 = new PolicyVersion("PV-2", "POL-TEST", 2, "v2", Arrays.asList(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.5), // Increased threshold
                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                new SkillCondition("Java")
        ));

        Applicant dave = new Applicant("T7", "Dave", 8.3, 85, Collections.singletonList("Java"));
        Applicant carol = new Applicant("T8", "Carol", 8.5, 72, Collections.singletonList("Java"));
        List<Applicant> pool = Arrays.asList(dave, carol);

        ComparisonReport report = service.compareVersions(pool, v1, v2);

        assertCondition(report.getV1PassedCount() == 2, "v1 pass count is 2 (Dave & Carol)");
        assertCondition(report.getV2PassedCount() == 1, "v2 pass count is 1 (Carol only)");
        assertCondition(report.getChangedApplicants().size() == 1, "Exactly 1 applicant changed decision");

        ApplicantDiff daveDiff = report.getChangedApplicants().get(0);
        assertCondition(daveDiff.getApplicantName().equals("Dave"), "Dave is the applicant whose decision changed");
        assertCondition(daveDiff.isV1Decision() == true && daveDiff.isV2Decision() == false, "Dave passed v1 and failed v2");
        assertCondition(report.getLostEligibilityCount() == 1, "Lost eligibility metric is 1");
    }

    private static void assertCondition(boolean condition, String description) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  ✓ " + description);
        } else {
            testsFailed++;
            System.err.println("  ✗ FAILED: " + description);
        }
    }
}
