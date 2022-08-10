package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class BigQueryGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof BigQueryDatasourceSpecification)
            {
                BigQueryDatasourceSpecification spec = (BigQueryDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "BigQuery\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "projectId: " + convertString(spec.projectId, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "defaultDataset: " + convertString(spec.defaultDataset, true) + ";\n" +
                        (spec.proxyHost != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyHost: " + convertString(spec.proxyHost, true) + ";\n" : "") +
                        (spec.proxyPort != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyPort: " + convertString(spec.proxyPort, true) + ";\n" : "") +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
