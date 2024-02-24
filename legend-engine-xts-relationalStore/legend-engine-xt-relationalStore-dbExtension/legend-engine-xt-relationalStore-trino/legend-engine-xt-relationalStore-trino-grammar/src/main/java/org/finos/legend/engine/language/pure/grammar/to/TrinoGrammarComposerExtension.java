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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TrinoDelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoSSLSpecification;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class TrinoGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public List<Function2<AuthenticationStrategy, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Lists.mutable.with((_strategy, context) ->
        {
            if (_strategy instanceof TrinoDelegatedKerberosAuthenticationStrategy)
            {
                TrinoDelegatedKerberosAuthenticationStrategy auth = (TrinoDelegatedKerberosAuthenticationStrategy) _strategy;
                int baseIndentation = 1;
                return "TrinoDelegatedKerberos\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (auth.serverPrincipal == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "serverPrincipal: " + auth.serverPrincipal + ";\n") +
                        (auth.kerberosUseCanonicalHostname == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "kerberosUseCanonicalHostname: " + auth.kerberosUseCanonicalHostname + ";\n") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "kerberosRemoteServiceName: " + convertString(auth.kerberosRemoteServiceName, true) + ";\n" +
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
            if (_spec instanceof TrinoDatasourceSpecification)
            {
                TrinoDatasourceSpecification spec = (TrinoDatasourceSpecification) _spec;
                TrinoSSLSpecification sslSpec = spec.sslSpecification;

                int baseIndentation = 1;
                return "Trino\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + convertString(spec.host,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                        (spec.catalog == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "catalog: " + convertString(spec.catalog,true) + ";\n") +
                        (spec.schema == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "schema: " + convertString(spec.schema,true) + ";\n") +
                        (spec.clientTags == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "clientTags: " + convertString(spec.clientTags,true) + ";\n") +
                        (sslSpec == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 1) + "sslSpecification:\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 2) + "ssl: " + sslSpec.ssl + ";\n" +
                        (sslSpec.trustStorePathVaultReference == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 2) + "trustStorePathVaultReference: " + convertString(sslSpec.trustStorePathVaultReference,true) + ";\n") +
                        (sslSpec.trustStorePasswordVaultReference == null ? "" : context.getIndentationString() + getTabString(baseIndentation + 2) + "trustStorePasswordVaultReference: " + convertString(sslSpec.trustStorePasswordVaultReference,true) + ";\n") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "};\n") +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
