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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql.mapping;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.mapping.GraphQLMappingParserGrammar;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;

public class GraphQLMappingParseTreeWalker
{
    public GraphQLMappingParseTreeWalker()
    {
    }

    public void visitGraphQLClassMapping(GraphQLMappingParserGrammar.ClassMappingContext ctx, GraphQLClassMapping classMapping)
    {
        GraphQLMappingParserGrammar.MappingTypeNameContext typeNameContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.mappingTypeName(), "typeName", classMapping.sourceInformation);
        if (typeNameContext != null)
        {
            classMapping.typeName = PureGrammarParserUtility.fromIdentifier(typeNameContext.identifier());
            classMapping.storePath = typeNameContext.storePointer().qualifiedName().getText();
        }
        GraphQLMappingParserGrammar.MappingBindingContext bindingContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.mappingBinding(), "binding", classMapping.sourceInformation);
        if (bindingContext != null)
        {
            classMapping.bindingPath = bindingContext.qualifiedName().getText();
        }
        GraphQLMappingParserGrammar.MappingJoinAccessorContext joinAccessorContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.mappingJoinAccessor(), "joinAccessor", classMapping.sourceInformation);
        if (joinAccessorContext != null)
        {
            classMapping.joinAccessorQualifiedProperty = PureGrammarParserUtility.fromIdentifier(joinAccessorContext.identifier());
        }
    }
}
