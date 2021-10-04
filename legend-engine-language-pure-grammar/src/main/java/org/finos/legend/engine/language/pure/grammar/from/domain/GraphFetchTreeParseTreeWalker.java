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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GraphFetchTreeParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    GraphFetchTreeParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation)
    {
        this.walkerSourceInformation = sourceInformation;
    }

    public GraphFetchTree visitDefinition(GraphFetchTreeParserGrammar.DefinitionContext definitionContext)
    {
        return this.visitRootGraphDefinition(definitionContext.graphDefinition(), definitionContext);
    }

    private RootGraphFetchTree visitRootGraphDefinition(GraphFetchTreeParserGrammar.GraphDefinitionContext graphDefinitionContext, GraphFetchTreeParserGrammar.DefinitionContext definitionContext)
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
        return result;
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
            Collection result = new Collection();
            for (GraphFetchTreeParserGrammar.ScalarParameterContext scalarParameterContext : parameterContext.collectionParameter().scalarParameter())
            {
                values.add(this.visitScalarParameterContext(scalarParameterContext));
            }
            result.values = values;
            return result;
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
            Multiplicity m = this.getPureOne();
            if (ctx.STRING() != null)
            {
                List<String> values = new ArrayList<>();
                values.add(PureGrammarParserUtility.fromGrammarString(ctx.getText(), true));
                CString instance = new CString();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.INTEGER() != null)
            {
                List<Long> values = new ArrayList<>();
                values.add(Long.parseLong(ctx.getText()));
                CInteger instance = new CInteger();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.FLOAT() != null)
            {
                List<Double> values = new ArrayList<>();
                values.add(Double.parseDouble(ctx.getText()));
                CFloat instance = new CFloat();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.DATE() != null)
            {
                List<String> values = new ArrayList<>();
                values.add(ctx.getText());  // Likely wrong
                CDateTime instance = new CDateTime();
                instance.multiplicity = this.getPureOne();
                instance.values = values.stream().map(value -> value.substring(value.lastIndexOf('%') + 1)).collect(Collectors.toList());
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.BOOLEAN() != null)
            {
                List<Boolean> values = new ArrayList<>();
                values.add(Boolean.parseBoolean(ctx.getText()));
                CBoolean instance = new CBoolean();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
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
        return result;
    }

    private Multiplicity getPureOne()
    {
        Multiplicity m = new Multiplicity();
        m.lowerBound = 1;
        m.setUpperBound(1);
        return m;
    }
}
