// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.to;

import java.util.List;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SpannerDatasourceSpecification;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class SpannerGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof SpannerDatasourceSpecification)
            {
                SpannerDatasourceSpecification spec = (SpannerDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "Spanner\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "projectId: " + convertString(spec.projectId, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "instanceId: " + convertString(spec.instanceId, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "databaseId: " + convertString(spec.databaseId, true) + ";\n" +
                        (spec.proxyHost != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.proxyHost, true) + ";\n" : "") +
                        (spec.proxyPort != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.proxyPort + ";\n" : "") +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
