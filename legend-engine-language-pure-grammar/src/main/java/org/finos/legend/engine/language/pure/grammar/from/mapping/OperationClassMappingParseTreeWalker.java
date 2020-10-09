// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingOperation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;

public class OperationClassMappingParseTreeWalker
{
    public static ImmutableMap<String, MappingOperation> funcToOps;

    static
    {
        funcToOps = Maps.immutable.of(
                "meta::pure::router::operations::special_union_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.ROUTER_UNION,
                "meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.STORE_UNION,
                "meta::pure::router::operations::inheritance_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.INHERITANCE,
                "meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.MERGE
        );
    }

    public void visitOperationClassMapping(OperationClassMappingParserGrammar.OperationClassMappingContext mappingContext, OperationClassMapping operationClassMapping)
    {
        //TODO mappingClass extendsClassMappingId
        if (mappingContext.functionPath() != null)
        {
            operationClassMapping.operation = funcToOps.get(mappingContext.functionPath().getText());
        }
        if (mappingContext.parameters() != null && mappingContext.parameters().identifier() != null)
        {
            operationClassMapping.parameters = ListIterate.collect(mappingContext.parameters().identifier(), PureGrammarParserUtility::fromIdentifier);
        }
    }
}
