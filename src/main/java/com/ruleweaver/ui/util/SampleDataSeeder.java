package com.ruleweaver.ui.util;

import com.ruleweaver.engine.NumericCondition;
import com.ruleweaver.engine.Operator;
import com.ruleweaver.engine.SkillCondition;
import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.Policy;
import com.ruleweaver.repository.ApplicantRepository;
import com.ruleweaver.service.PolicyService;
import com.ruleweaver.ui.AppContext;
import com.ruleweaver.ui.adapter.ApplicantDocumentMapper;
import com.ruleweaver.ui.components.Toast;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds and clears the standard demonstration dataset.
 */
public class SampleDataSeeder {

    private static final String DEMO_POLICY_ID = "POL-INTERN";
    private static final List<String> DEMO_APPLICANT_IDS =
            List.of("APP-101", "APP-102", "APP-103", "APP-104", "APP-105");

    public static void seedDemoData(Runnable onComplete) {
        AppContext ctx = AppContext.getInstance();
        ctx.runAsync(() -> {
            ApplicantRepository applicantRepository = ctx.getApplicantRepository();
            if (applicantRepository != null) {
                List<Applicant> applicants = Arrays.asList(
                        createApplicant("APP-101", "Alice Smith", "alice@example.com",
                                8.2, 65, Arrays.asList("Java", "SQL")),
                        createApplicant("APP-102", "Bob Johnson", "bob@example.com",
                                7.9, 75, Arrays.asList("Python", "Java")),
                        createApplicant("APP-103", "Carol White", "carol@example.com",
                                8.5, 72, Arrays.asList("C++", "Java")),
                        createApplicant("APP-104", "Dave Miller", "dave@example.com",
                                8.3, 85, Arrays.asList("Java", "Spring", "Docker")),
                        createApplicant("APP-105", "Eve Taylor", "eve@example.com",
                                9.1, 90, Arrays.asList("Python", "Go", "Kubernetes"))
                );
                for (Applicant applicant : applicants) {
                    Document document = ApplicantDocumentMapper.toDocument(applicant);
                    applicantRepository.upsert(document);
                }
            }

            PolicyService policyService = ctx.getPolicyService();
            Policy policy = ctx.getPolicyRepository().findById(DEMO_POLICY_ID).orElse(null);
            if (policy == null) {
                policy = policyService.createPolicy(
                        DEMO_POLICY_ID,
                        "Software Engineering Internship Eligibility",
                        "Screening criteria for student summer internship pool",
                        Arrays.asList(
                                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                                new SkillCondition("Java")));
            }
            if (policy.getVersion(2).isEmpty()) {
                policyService.createNewVersion(
                        DEMO_POLICY_ID,
                        "Tightened GPA threshold (GPA >= 8.5) due to high candidate volume",
                        Arrays.asList(
                                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.5),
                                new NumericCondition("interviewScore", Operator.GREATER_THAN_OR_EQUAL, 70.0),
                                new SkillCondition("Java")));
            }

            Policy selectedPolicy = policy;
            javafx.application.Platform.runLater(() -> {
                ctx.selectedPolicyProperty().set(selectedPolicy);
                ctx.selectedVersionProperty().set(selectedPolicy.getVersion(1).orElse(null));
            });
            ctx.loadApplicantsAsync(() -> ctx.loadPoliciesAsync(onComplete), error ->
                    ctx.showToast(Toast.error(
                            "Applicant Load Failed",
                            error.getMessage() == null ? "Could not load applicants." : error.getMessage(),
                            null)));
        });
    }

    public static void clearDemoData(Runnable onComplete) {
        AppContext ctx = AppContext.getInstance();
        ctx.runAsync(() -> {
            ApplicantRepository applicantRepository = ctx.getApplicantRepository();
            if (applicantRepository != null) {
                DEMO_APPLICANT_IDS.forEach(applicantRepository::delete);
            }
            if (ctx.getEvaluationRepository() != null) {
                ctx.getEvaluationRepository().deleteDemoEvaluations(DEMO_APPLICANT_IDS, DEMO_POLICY_ID);
            }
            ctx.getPolicyRepository().deleteById(DEMO_POLICY_ID);
            ctx.loadApplicantsAsync(() -> ctx.loadPoliciesAsync(onComplete), error ->
                    ctx.showToast(Toast.error(
                            "Clear Demo Data Failed",
                            error.getMessage() == null ? "Could not reload applicant data." : error.getMessage(),
                            null)));
        });
    }

    private static Applicant createApplicant(
            String id, String name, String email, double gpa, int interviewScore, List<String> skills) {
        Applicant applicant = new Applicant(id, name, gpa, interviewScore, skills);
        applicant.setEmail(email);
        return applicant;
    }
}
