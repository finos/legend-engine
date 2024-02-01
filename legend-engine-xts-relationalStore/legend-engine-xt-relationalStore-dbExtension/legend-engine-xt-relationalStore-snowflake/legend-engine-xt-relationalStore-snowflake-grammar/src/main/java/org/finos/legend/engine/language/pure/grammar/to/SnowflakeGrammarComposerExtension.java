// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class SnowflakeGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Snowflake");
    }

    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((_strategy, context) ->
        {
            if (_strategy instanceof SnowflakePublicAuthenticationStrategy)
            {
                SnowflakePublicAuthenticationStrategy auth = (SnowflakePublicAuthenticationStrategy) _strategy;
                int baseIndentation = 1;
                return "SnowflakePublic" +
                        "\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "publicUserName: " + convertString(auth.publicUserName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "privateKeyVaultReference: " + convertString(auth.privateKeyVaultReference, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "passPhraseVaultReference: " + convertString(auth.passPhraseVaultReference, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";

            }
            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof SnowflakeDatasourceSpecification)
            {
                SnowflakeDatasourceSpecification spec = (SnowflakeDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "Snowflake\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "name: " + convertString(spec.databaseName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "account: " + convertString(spec.accountName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "warehouse: " + convertString(spec.warehouseName, true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "region: " + convertString(spec.region, true) + ";\n" +
                        (spec.cloudType != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "cloudType: " + convertString(spec.cloudType, true) + ";\n" : "") +
                        (spec.quotedIdentifiersIgnoreCase != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "quotedIdentifiersIgnoreCase: " + spec.quotedIdentifiersIgnoreCase + ";\n" : "") +
                        (spec.enableQueryTags != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "enableQueryTags: " + spec.enableQueryTags + ";\n" : "") +
                        (spec.proxyHost != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyHost: " + convertString(spec.proxyHost, true) + ";\n" : "") +
                        (spec.proxyPort != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "proxyPort: " + convertString(spec.proxyPort, true) + ";\n" : "") +
                        (spec.nonProxyHosts != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "nonProxyHosts: " + convertString(spec.nonProxyHosts, true) + ";\n" : "") +
                        (spec.nonProxyHosts != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "nonProxyHosts: " + convertString(spec.nonProxyHosts, true) + ";\n" : "") +
                        (spec.nonProxyHosts != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "nonProxyHosts: " + convertString(spec.nonProxyHosts, true) + ";\n" : "") +
                        (spec.accountType != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "accountType: " + spec.accountType + ";\n" : "") +
                        (spec.organization != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "organization: " + convertString(spec.organization, true) + ";\n" : "") +

                        (spec.role != null ? context.getIndentationString() + getTabString(baseIndentation + 1) + "role: " + convertString(spec.role, true) + ";\n" : "") +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
