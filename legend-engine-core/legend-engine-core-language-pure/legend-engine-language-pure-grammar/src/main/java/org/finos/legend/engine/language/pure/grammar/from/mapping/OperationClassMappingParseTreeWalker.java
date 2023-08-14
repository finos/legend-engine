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
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingOperation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MergeOperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.Collections;

public class OperationClassMappingParseTreeWalker
{
    public static ImmutableMap<String, MappingOperation> funcToOps;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;

    static
    {
        funcToOps = Maps.immutable.of(
                "meta::pure::router::operations::special_union_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.ROUTER_UNION,
                "meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.STORE_UNION,
                "meta::pure::router::operations::inheritance_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.INHERITANCE,
                "meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_", MappingOperation.MERGE
        );
    }

    public OperationClassMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext parserContext)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
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

    public void visitMergeOperationClassMapping(OperationClassMappingParserGrammar.OperationClassMappingContext mappingContext, MergeOperationClassMapping mergeOperationClassMapping)
    {
        //TODO mappingClass extendsClassMappingId
        if (mappingContext.functionPath() != null)
        {
            mergeOperationClassMapping.operation = funcToOps.get(mappingContext.functionPath().getText());
        }
        if (mappingContext.mergeParameters() != null)
        {
            if (mappingContext.mergeParameters().setParameter() != null && mappingContext.mergeParameters().setParameter().identifier() != null)
            {
                mergeOperationClassMapping.parameters = ListIterate.collect(mappingContext.mergeParameters().setParameter().identifier(), PureGrammarParserUtility::fromIdentifier);
            }

            if (mappingContext.mergeParameters().validationLambda() != null)
            {
                int startLine = mappingContext.start.getLine();
                int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
                // only add current walker source information column offset if this is the first line
                int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + mappingContext.getStart().getCharPositionInLine();
                ParseTreeWalkerSourceInformation combinedExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();

                String lambdaString = mappingContext.mergeParameters().validationLambda().getText();
                ValueSpecification valueSpecification = new DomainParser().parseCombinedExpression(lambdaString, combinedExpressionSourceInformation, this.parserContext);

                Lambda lambda = new Lambda();
                lambda.body = Collections.singletonList(valueSpecification);
                mergeOperationClassMapping.validationFunction = lambda;


            }


        }


    }
}
