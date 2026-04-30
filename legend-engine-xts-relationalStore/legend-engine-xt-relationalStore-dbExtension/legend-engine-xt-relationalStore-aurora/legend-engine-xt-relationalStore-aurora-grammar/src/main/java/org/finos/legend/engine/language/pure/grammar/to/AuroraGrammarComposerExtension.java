// Copyright 2026 Goldman Sachs
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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.AuroraDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.GlobalAuroraDatasourceSpecification;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AuroraGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Aurora");
    }

    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((_strategy, context) -> null);
    }

    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof AuroraDatasourceSpecification)
            {
                AuroraDatasourceSpecification spec = (AuroraDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "Aurora\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.name, true) + ";\n" +
                        (spec.clusterInstanceHostPattern == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "clusterInstanceHostPattern: " + convertString(spec.clusterInstanceHostPattern, true) + ";\n") +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            if (_spec instanceof GlobalAuroraDatasourceSpecification)
            {
                GlobalAuroraDatasourceSpecification spec = (GlobalAuroraDatasourceSpecification) _spec;
                int baseIndentation = 1;
                String patterns = spec.globalClusterInstanceHostPatterns.stream()
                        .map(p -> convertString(p, true))
                        .collect(Collectors.joining(", "));
                return "GlobalAurora\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.name, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "region: " + convertString(spec.region, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "globalClusterInstanceHostPatterns: [" + patterns + "];\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
