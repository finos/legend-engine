//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.bigqueryFunc.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.BigQueryFunctionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.bigqueryFunc.metamodel.BigQueryFunction;

import java.util.function.Consumer;

public class BigQueryFunctionTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public BigQueryFunctionTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(BigQueryFunctionParserGrammar.DefinitionContext ctx)
    {
        ctx.bigQueryFunction().stream().map(this::visitBigQueryFunction).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private BigQueryFunction visitBigQueryFunction(BigQueryFunctionParserGrammar.BigQueryFunctionContext ctx)
    {
        BigQueryFunction bigQueryFunction = new BigQueryFunction();
        bigQueryFunction.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        bigQueryFunction._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        bigQueryFunction.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        BigQueryFunctionParserGrammar.FunctionNameContext functionNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.functionName(), "functionName", bigQueryFunction.sourceInformation);
        bigQueryFunction.functionName = PureGrammarParserUtility.fromGrammarString(functionNameContext.STRING().getText(), true);
        BigQueryFunctionParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", bigQueryFunction.sourceInformation);
        bigQueryFunction.function = functionContext.functionIdentifier().getText();
        BigQueryFunctionParserGrammar.OwnerContext ownerContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owner(), "owner", bigQueryFunction.sourceInformation);
        if (ownerContext != null)
        {
            bigQueryFunction.owner = PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true);
        }
        BigQueryFunctionParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", bigQueryFunction.sourceInformation);
        if (descriptionContext != null)
        {
            bigQueryFunction.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        return bigQueryFunction;
    }
}
