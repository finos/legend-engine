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

package org.finos.legend.engine.language.pure.grammar.from.domain;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.graphFetchTree.GraphFetchTreeParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphFetchTreeParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    GraphFetchTreeParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation)
    {
        this.walkerSourceInformation = sourceInformation;
    }

    public ClassInstance visitDefinition(GraphFetchTreeParserGrammar.DefinitionContext definitionContext)
    {
        return this.visitRootGraphDefinition(definitionContext.graphDefinition(), definitionContext);
    }

    private ClassInstance visitRootGraphDefinition(GraphFetchTreeParserGrammar.GraphDefinitionContext graphDefinitionContext, GraphFetchTreeParserGrammar.DefinitionContext definitionContext)
    {
        List<GraphFetchTree> subTrees = new ArrayList<>();
        for (GraphFetchTreeParserGrammar.GraphPathContext graphPathContext : graphDefinitionContext.graphPaths().graphPath())
        {
            subTrees.add(this.visitGraphPathContext(graphPathContext));
        }
        RootGraphFetchTree result = new RootGraphFetchTree();
        result._class = PureGrammarParserUtility.fromQualifiedName(definitionContext.qualifiedName().packagePath() == null ? Collections.emptyList() : definitionContext.qualifiedName().packagePath().identifier(), definitionContext.qualifiedName().identifier());
        result.sourceInformation = walkerSourceInformation.getSourceInformation(definitionContext.qualifiedName());
        result.subTrees = subTrees;
        return DomainParseTreeWalker.wrapWithClassInstance(result, "rootGraphFetchTree");
    }

    private PropertyGraphFetchTree visitGraphPathContext(GraphFetchTreeParserGrammar.GraphPathContext graphPathContext)
    {
        List<GraphFetchTree> subTrees = new ArrayList<>();
        if (graphPathContext.graphDefinition() != null)
        {
            for (GraphFetchTreeParserGrammar.GraphPathContext subGraphPathContext : graphPathContext.graphDefinition().graphPaths().graphPath())
            {
                subTrees.add(this.visitGraphPathContext(subGraphPathContext));
            }
        }

        List<ValueSpecification> parameters = new ArrayList<>();
        if (graphPathContext.propertyParameters() != null)
        {
            for (GraphFetchTreeParserGrammar.ParameterContext parameterContext : graphPathContext.propertyParameters().parameter())
            {
                parameters.add(this.visitParameterContext(parameterContext));
            }
        }

        PropertyGraphFetchTree result = new PropertyGraphFetchTree();
        result.property = PureGrammarParserUtility.fromIdentifier(graphPathContext.identifier());
        result.sourceInformation = walkerSourceInformation.getSourceInformation(graphPathContext.identifier());
        result.parameters = parameters;
        result.subTrees = subTrees;
        if (graphPathContext.alias() != null)
        {
            String withQuote = graphPathContext.alias().STRING().getText();
            result.alias = withQuote.substring(1, withQuote.length() - 1);
        }
        if (graphPathContext.subtype() != null)
        {
            result.subType = graphPathContext.subtype().qualifiedName().getText();
        }
        return result;
    }

    private ValueSpecification visitParameterContext(GraphFetchTreeParserGrammar.ParameterContext parameterContext)
    {
        List<ValueSpecification> values = new ArrayList<>();
        if (parameterContext.scalarParameter() != null)
        {
            return this.visitScalarParameterContext(parameterContext.scalarParameter());
        }
        else
        {
            for (GraphFetchTreeParserGrammar.ScalarParameterContext scalarParameterContext : parameterContext.collectionParameter().scalarParameter())
            {
                values.add(this.visitScalarParameterContext(scalarParameterContext));
            }
            return new Collection(values);
        }
    }

    private ValueSpecification visitScalarParameterContext(GraphFetchTreeParserGrammar.ScalarParameterContext scalarParameterContext)
    {
        if (scalarParameterContext.enumReference() != null)
        {
            EnumValue result = new EnumValue();
            result.sourceInformation = walkerSourceInformation.getSourceInformation(scalarParameterContext.enumReference());
            result.fullPath = PureGrammarParserUtility.fromQualifiedName(scalarParameterContext.enumReference().qualifiedName().packagePath() == null ? Collections.emptyList() : scalarParameterContext.enumReference().qualifiedName().packagePath().identifier(), scalarParameterContext.enumReference().qualifiedName().identifier());
            result.value = PureGrammarParserUtility.fromIdentifier(scalarParameterContext.enumReference().identifier());
            return result;
        }
        if (scalarParameterContext.variable() != null)
        {
            Variable ve = new Variable();
            ve.sourceInformation = walkerSourceInformation.getSourceInformation(scalarParameterContext.variable().identifier());
            ve.name = PureGrammarParserUtility.fromIdentifier(scalarParameterContext.variable().identifier());
            return ve;
        }
        return instanceLiteralToken(scalarParameterContext.instanceLiteral().instanceLiteralToken());
    }

    private ValueSpecification instanceLiteralToken(GraphFetchTreeParserGrammar.InstanceLiteralTokenContext ctx)
    {
        ValueSpecification result;
        try
        {
            if (ctx.STRING() != null)
            {
                result = new CString(PureGrammarParserUtility.fromGrammarString(ctx.getText(), true));
            }
            else if (ctx.INTEGER() != null)
            {
                result = new CInteger(Long.parseLong(ctx.getText()));
            }
            else if (ctx.FLOAT() != null)
            {
                result = new CFloat(Double.parseDouble(ctx.getText()));
            }
            else if (ctx.DATE() != null)
            {
                result = new CDateTime(ctx.getText());
            }
            else if (ctx.BOOLEAN() != null)
            {
                result = new CBoolean(Boolean.parseBoolean(ctx.getText()));
            }
            else
            {
                // TODO
                throw new UnsupportedOperationException();
            }
        }
        catch (Exception e)
        {
            throw new UnsupportedOperationException(ctx.getText());
        }
        result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return result;
    }
}
