package org.finos.legend.engine.plan.execution.stores.service.test.contentPattern;

import org.eclipse.collections.impl.factory.Lists;

import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

public class ContentPatternToWiremockPatternGeneratorExtensionLoader
{
    private static final AtomicReference<List<ContentPatternToWiremockPatternGenerator>> INSTANCE = new AtomicReference<>();

    public static List<ContentPatternToWiremockPatternGenerator> extensions()
    {
        return INSTANCE.updateAndGet(existing ->
        {
            if (existing == null)
            {
                List<ContentPatternToWiremockPatternGenerator> extensions = Lists.mutable.empty();
                for (ContentPatternToWiremockPatternGenerator extension : ServiceLoader.load(ContentPatternToWiremockPatternGenerator.class))
                {
                    extensions.add(extension);
                }
                return extensions;
            }
            else
            {
                return existing;
            }
        });
    }
}
