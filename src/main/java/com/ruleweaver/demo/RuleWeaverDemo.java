package com.ruleweaver.demo;

import com.ruleweaver.engine.*;
import com.ruleweaver.model.*;
import com.ruleweaver.repository.*;
import com.ruleweaver.service.ComparisonService;
import com.ruleweaver.service.PolicyService;
import java.util.*;

/**
 * End-to-end Demonstration of RuleWeaver X — The Decision Time Machine.
 * 
 * Demonstrates:
 * 1. Java OOP Principles:
 *    - Encapsulation: Applicant, Policy, EvaluationResult private fields & accessors
 *    - Abstraction: Condition interface
 *    - Inheritance: AbstractCondition -> NumericCondition, SkillCondition, CompositeCondition
 *    - Polymorphism: DecisionEngine evaluating mixed Condition lists uniformly
 *    - Composition: Policy -> PolicyVersion -> Conditions; Composite Pattern
 * 2. Algorithmic Decision Engine & Explainability:
 *    - Detailed breakdown of pass/fail criteria per applicant
 * 3. The Decision Time Machine (Policy Versioning & Comparison):
 *    - Evaluating candidates across Version 1 vs Version 2
 *    - Pinpointing the exact rule changes that flipped decisions (e.g. Dave Miller)
 *    - Statistical impact reporting
 */
public class RuleWeaverDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("     RULEWEAVER X — THE DECISION TIME MACHINE & EXPLAINABLE DECISION ENGINE     ");
        System.out.println("================================================================================\n");

        // 1. Initialize Repositories and Services
        var applicantRepo = new InMemoryApplicantRepository();
        var policyRepo = new InMemoryPolicyRepository();
        var evalRepo = new InMemoryEvaluationResultRepository();
        DecisionEngine engine = new DecisionEngine();
        ComparisonService comparisonService = new ComparisonService(engine);
        PolicyService policyService = new PolicyService(policyRepo, evalRepo, engine, comparisonService);

        // 2. Populate Candidate Database (matching PDF Case Study + Extension)
        List<Applicant> applicants = Arrays.asList(
                new Applicant("APP-101", "Alice Smith", 8.2, 65, Arrays.asList("Java", "SQL", "Communication")),
                new Applicant("APP-102", "Bob Jones", 7.9, 75, Arrays.asList("Python", "Java")),
                new Applicant("APP-103", "Carol Davis", 8.5, 72, Arrays.asList("C++", "Java")),
                new Applicant("APP-104", "Dave Miller", 8.3, 85, Arrays.asList("Java", "Spring", "Docker")),
                new Applicant("APP-105", "Eve Taylor", 9.1, 90, Arrays.asList("Python", "Go", "Kubernetes"))
        );
        applicants.forEach(applicantRepo::save);

        System.out.println(">>> 1. Registered Applicants:");
        for (Object rawApplicant : applicantRepo.findAll()) {
            Applicant a = (Applicant) rawApplicant;
            System.out.printf(" • [%s] %-12s | GPA: %.2f | Interview: %2d | Skills: %s\n",
                    a.getId(), a.getName(), a.getGpa(), a.getInterviewScore(), a.getSkills());
        }
        System.out.println();

        // 3. Define Policy v1: Internship Eligibility (GPA >= 8.0, Interview >= 70, Java skill)
        List<Condition> v1Conditions = new ArrayList<>();
        v1Conditions.add(new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0));
        v1Conditions.add(new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0));
        v1Conditions.add(new SkillCondition("Java"));

        Policy policy = policyService.createPolicy(
                "POL-INTERN",
                "Software Engineering Internship Eligibility",
                "Evaluates student qualification for summer internship",
                v1Conditions
        );

        System.out.println(">>> 2. Created Policy: " + policy.getName() + " (v1)");
        System.out.println("Criteria defined for v1:");
        for (Condition c : v1Conditions) {
            System.out.println(" • " + c.getMessage() + " [" + c.getType() + "]");
        }
        System.out.println();

        // 4. Run Decision Engine on Version 1
        System.out.println(">>> 3. Executing Decision Engine on Policy v1 (Explainable Results):\n");
        List<EvaluationResult> v1Results = policyService.evaluateApplicants("POL-INTERN", 1, applicants);
        for (EvaluationResult res : v1Results) {
            System.out.println(res.getSummaryExplanation());
        }

        // 5. Versioning: College raises GPA threshold to 8.5 in Version 2
        System.out.println(">>> 4. Creating Policy Version 2 ('Decision Time Machine' Simulation):");
        System.out.println("Scenario: College raises GPA requirement from 8.0 to 8.5 due to high applicant volume.\n");

        List<Condition> v2Conditions = new ArrayList<>();
        v2Conditions.add(new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.5)); // Tightened threshold
        v2Conditions.add(new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0));
        v2Conditions.add(new SkillCondition("Java"));

        PolicyVersion v2 = policyService.createNewVersion(
                "POL-INTERN",
                "Tightened GPA threshold (GPA >= 8.5) for high volume screening",
                v2Conditions
        );

        // 6. Run Decision Engine on Version 2
        List<EvaluationResult> v2Results = policyService.evaluateApplicants("POL-INTERN", 2, applicants);

        // 7. Policy Comparison ("Decision Time Machine")
        System.out.println(">>> 5. Generating Policy Comparison Report (v1 vs v2):");
        ComparisonReport report = policyService.compareVersions("POL-INTERN", 1, 2, applicants);
        System.out.println(report.getSummaryText());

        // 8. Demonstrate Composite Condition (Composite Pattern)
        System.out.println(">>> 6. Demonstrating Composite Pattern (Advanced Group Criteria):");
        CompositeCondition technicalExcellence = new CompositeCondition("Technical Excellence Track", LogicalOperator.AND);
        technicalExcellence.addSubcondition(new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.5));
        technicalExcellence.addSubcondition(new SkillCondition("Docker"));

        System.out.println("Evaluating candidate Dave Miller against Composite Track:");
        Applicant dave = applicantRepo.findById("APP-104").orElseThrow();
        System.out.println(technicalExcellence.explain(dave));
        System.out.println();

        System.out.println("================================================================================");
        System.out.println("            DEMO COMPLETE: ALL OOP & DECISION ENGINE GOALS SATISFIED            ");
        System.out.println("================================================================================");
    }
}
