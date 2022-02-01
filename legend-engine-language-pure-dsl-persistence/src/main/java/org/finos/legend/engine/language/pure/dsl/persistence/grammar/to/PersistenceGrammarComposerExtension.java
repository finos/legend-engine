package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.ServicePersistence;

import java.util.List;

public class PersistenceGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!PersistenceParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof ServicePersistence)
                {
                    return renderServicePersistence((ServicePersistence) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<ServicePersistence> composableElements = ListIterate.selectInstancesOf(elements, ServicePersistence.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, PersistenceGrammarComposerExtension::renderServicePersistence).makeString("###" + PersistenceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderServicePersistence(ServicePersistence servicePersistence)
    {
        return null;
    }
}
