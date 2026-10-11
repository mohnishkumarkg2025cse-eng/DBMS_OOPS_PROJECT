package com.ruleweaver.engine;

import com.ruleweaver.model.Applicant;
import com.ruleweaver.model.ConditionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConditionDocumentMapperTest {

    @Test
    void roundTripsNestedConditions() {
        CompositeCondition skills = new CompositeCondition("Any language", LogicalOperator.OR, List.of(
                new SkillCondition("Java"),
                new SkillCondition("Python")));
        CompositeCondition root = new CompositeCondition("Internship rules", LogicalOperator.AND, List.of(
                new NumericCondition("gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0),
                skills));

        Condition restored = ConditionDocumentMapper.fromDocument(ConditionDocumentMapper.toDocument(root));
        Applicant applicant = new Applicant("A-1", "Test Applicant", 8.5, 80, List.of("Python"));

        assertInstanceOf(CompositeCondition.class, restored);
        CompositeCondition restoredRoot = (CompositeCondition) restored;
        assertEquals(LogicalOperator.AND, restoredRoot.getLogicalOperator());
        assertInstanceOf(CompositeCondition.class, restoredRoot.getSubconditions().get(1));
        CompositeCondition restoredSkills = (CompositeCondition) restoredRoot.getSubconditions().get(1);
        assertEquals(LogicalOperator.OR, restoredSkills.getLogicalOperator());
        assertTrue(restored.test(applicant));
    }

    @Test
    void evaluationResultsContainConditionRequirements() {
        Applicant applicant = new Applicant("A-2", "Test Applicant", 7.5, 80, List.of("Python"));
        ConditionResult numeric = new NumericCondition(
                "gpa", Operator.GREATER_THAN_OR_EQUAL, 8.0).evaluate(applicant);
        ConditionResult skill = new SkillCondition("Java").evaluate(applicant);
        ConditionResult composite = new CompositeCondition(
                "Rules", LogicalOperator.AND, List.of(new SkillCondition("Java"))).evaluate(applicant);

        assertEquals(8.0, numeric.getRequiredValue());
        assertEquals("Java", skill.getRequiredValue());
        assertNull(composite.getRequiredValue());
    }

    @Test
    void numericConditionFailsSafelyWhenTheFieldIsMissing() {
        Applicant applicant = new Applicant("A-3", "Test Applicant", 8.5, 80, List.of("Java"));

        ConditionResult result = new NumericCondition(
                "unknownNumericField", Operator.GREATER_THAN_OR_EQUAL, 1.0).evaluate(applicant);

        assertFalse(result.isPassed());
        assertNull(result.getActualValue());
        assertEquals(1.0, result.getRequiredValue());
        assertTrue(result.getExplanation().contains("missing or invalid"));
    }

    @Test
    void defaultsLegacyCompositeDocumentsToAnd() {
        Condition restored = ConditionDocumentMapper.fromDocument(
                new org.bson.Document("type", "CompositeCondition")
                        .append("name", "Legacy rules")
                        .append("subconditions", List.of()));

        assertInstanceOf(CompositeCondition.class, restored);
        assertEquals(LogicalOperator.AND, ((CompositeCondition) restored).getLogicalOperator());
    }
}
