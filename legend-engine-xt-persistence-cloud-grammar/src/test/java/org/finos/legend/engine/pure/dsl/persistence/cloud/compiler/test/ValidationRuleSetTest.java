package org.finos.legend.engine.pure.dsl.persistence.cloud.compiler.test;

import org.finos.legend.engine.language.pure.dsl.persistence.cloud.compiler.toPureGraph.PersistenceCloudCompilerExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationResult;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationRuleSet;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationRuleSetTest
{
    @Test
    public void dpuCountEqualsMinimum()
    {
        ValidationRuleSet<PersistenceContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        PersistenceContext context = createPersistenceContextWithDpuCount(2);

        assertTrue(ruleSet.validate(context).valid());
    }

    @Test
    public void dpuCountLessThanMinimum()
    {
        ValidationRuleSet<PersistenceContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        PersistenceContext context = createPersistenceContextWithDpuCount(1);

        ValidationResult result = ruleSet.validate(context);
        assertTrue(result.invalid());
        assertEquals(1, result.reasons().size());
        assertEquals("Data processing units value must be at least 2", result.reasons().get(0));
    }

    @Test
    public void dpuCountGreaterThanMinimum()
    {
        ValidationRuleSet<PersistenceContext> ruleSet = PersistenceCloudCompilerExtension.VALIDATION_RULE_SET;
        PersistenceContext context = createPersistenceContextWithDpuCount(10);

        assertTrue(ruleSet.validate(context).valid());
    }

    private static PersistenceContext createPersistenceContextWithDpuCount(int dataProcessingUnits)
    {
        AwsGluePersistencePlatform platform = new AwsGluePersistencePlatform();
        platform.dataProcessingUnits = dataProcessingUnits;
        PersistenceContext context = new PersistenceContext();
        context.platform = platform;
        return context;
    }
}
