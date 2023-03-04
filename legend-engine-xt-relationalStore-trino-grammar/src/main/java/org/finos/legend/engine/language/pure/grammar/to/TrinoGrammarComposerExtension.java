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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class TrinoGrammarComposerExtension implements IRelationalGrammarComposerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, PureGrammarComposerContext, String>> getExtraDataSourceSpecificationComposers()
    {
        return Lists.mutable.with((_spec, context) ->
        {
            if (_spec instanceof TrinoDatasourceSpecification)
            {
                TrinoDatasourceSpecification spec = (TrinoDatasourceSpecification) _spec;
                int baseIndentation = 1;
                return "Trino\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "host: " + PureGrammarComposerUtility.convertString(spec.host,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "port: " + spec.port + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "catalog: " + PureGrammarComposerUtility.convertString(spec.catalog,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "schema: " + PureGrammarComposerUtility.convertString(spec.schema,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "clientTags: " + PureGrammarComposerUtility.convertString(spec.clientTags,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "ssl: " + spec.ssl + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "trustStorePathVaultReference: " + PureGrammarComposerUtility.convertString(spec.trustStorePathVaultReference,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "trustStorePasswordVaultReference: " + PureGrammarComposerUtility.convertString(spec.trustStorePasswordVaultReference,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "kerberosRemoteServiceName: " + PureGrammarComposerUtility.convertString(spec.kerberosRemoteServiceName,true) + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "kerberosUseCanonicalHostname: " + spec.kerberosUseCanonicalHostname + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation) + "}";
            }
            return null;
        });
    }
}
