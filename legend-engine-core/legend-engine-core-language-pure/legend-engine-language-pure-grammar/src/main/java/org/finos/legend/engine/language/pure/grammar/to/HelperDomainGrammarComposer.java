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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.test.assertion.HelperTestAssertionGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.AggregationKind;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Constraint;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.QualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperDomainGrammarComposer
{
    public static String renderStereotypePointer(StereotypePtr stereotypePtr)
    {
        return PureGrammarComposerUtility.convertPath(stereotypePtr.profile) + "." + PureGrammarComposerUtility.convertIdentifier(stereotypePtr.value);
    }

    public static String renderTaggedValue(TaggedValue taggedValue)
    {
        return PureGrammarComposerUtility.convertPath(taggedValue.tag.profile) + "." + PureGrammarComposerUtility.convertIdentifier(taggedValue.tag.value) + " = " + convertString(taggedValue.value, true);
    }

    public static String renderAnnotations(List<StereotypePtr> stereotypes, List<TaggedValue> taggedValues)
    {
        return (stereotypes == null || stereotypes.isEmpty() ? "" : "<<" + LazyIterate.collect(stereotypes, HelperDomainGrammarComposer::renderStereotypePointer).makeString(", ") + ">> ")
                + (taggedValues == null || taggedValues.isEmpty() ? "" : "{" + LazyIterate.collect(taggedValues, HelperDomainGrammarComposer::renderTaggedValue).makeString(", ") + "} ");
    }

    public static String renderEnumValue(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.EnumValue enumValue)
    {
        return renderAnnotations(enumValue.stereotypes, enumValue.taggedValues) + PureGrammarComposerUtility.convertIdentifier(enumValue.value);
    }

    public static String renderUnit(Unit unit, DEPRECATED_PureGrammarComposerCore transformer)
    {
        String unitName = PureGrammarComposerUtility.convertIdentifier(unit.name);
        return (unitName.startsWith("'") ? "'" : "") + unitName.substring(unitName.lastIndexOf("~") + 1) + (unit.conversionFunction == null ? ";" : ": " + renderUnitLambda(unit.conversionFunction, transformer) + ";");
    }

    public static String renderUnitLambda(Lambda conversionFunction, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return (conversionFunction.parameters.isEmpty() ? "" : LazyIterate.collect(conversionFunction.parameters, variable -> variable.name).makeString(","))
                + " -> " + LazyIterate.collect(conversionFunction.body, valueSpecification -> valueSpecification.accept(transformer)).makeString(";");
    }

    public static String renderMultiplicity(Multiplicity multiplicity)
    {
        return multiplicity.lowerBound == 0 && multiplicity.getUpperBoundInt() == Integer.MAX_VALUE ? "*" : multiplicity.lowerBound == multiplicity.getUpperBoundInt() ? String.valueOf(multiplicity.lowerBound) : multiplicity.lowerBound + ".." + (multiplicity.getUpperBoundInt() == Integer.MAX_VALUE ? "*" : multiplicity.getUpperBoundInt());
    }

    public static String renderProperty(Property property, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return renderAnnotations(property.stereotypes, property.taggedValues) + renderAggregation(property.aggregation) + PureGrammarComposerUtility.convertIdentifier(property.name) + ": " + property.type + "[" + renderMultiplicity(property.multiplicity) + "]" + (property.defaultValue != null ? " = " + property.defaultValue.value.accept(transformer) : "");
    }

    private static String renderAggregation(AggregationKind aggregationKind)
    {
        if (aggregationKind == null)
        {
            return "";
        }
        switch (aggregationKind)
        {
            case NONE:
            {
                return "(none) ";
            }
            case SHARED:
            {
                return "(shared) ";
            }
            case COMPOSITE:
            {
                return "(composite) ";
            }
            default:
            {
                throw new EngineException("Unknown aggregation kind '" + aggregationKind + "'", EngineErrorType.COMPOSER);
            }
        }
    }

    public static String renderDerivedProperty(QualifiedProperty qualifiedProperty, DEPRECATED_PureGrammarComposerCore transformer)
    {
        List<Variable> functionParameters = qualifiedProperty.parameters.stream().filter(p -> !p.name.equals("this")).collect(Collectors.toList());
        return renderAnnotations(qualifiedProperty.stereotypes, qualifiedProperty.taggedValues)
                + PureGrammarComposerUtility.convertIdentifier(qualifiedProperty.name) + "("
                + LazyIterate.collect(functionParameters, p -> p.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(transformer).withVariableInFunctionSignature().build())).makeString(",")
                + ") {"
                + (qualifiedProperty.body.size() <= 1
                ? LazyIterate.collect(qualifiedProperty.body, b -> b.accept(transformer)).makeString("\n")
                : LazyIterate
                .collect(qualifiedProperty.body, b -> b.accept(transformer))
                .makeString("\n" + getTabString(2), ";\n" + getTabString(2), ";\n" + getTabString()))
                + "}: "
                + qualifiedProperty.returnType + "[" + renderMultiplicity(qualifiedProperty.returnMultiplicity) + "]";
    }

    public static String renderConstraint(Constraint constraint, List<Constraint> allConstraints, DEPRECATED_PureGrammarComposerCore transformer)
    {
        constraint.functionDefinition.parameters = Collections.emptyList();
        String lambdaString = constraint.functionDefinition.accept(transformer).replaceFirst("\\|", "");
        if (constraint.enforcementLevel == null && constraint.externalId == null && constraint.messageFunction == null)
        {
            return (String.valueOf(allConstraints.indexOf(constraint)).equals(constraint.name)
                    ? ""
                    : PureGrammarComposerUtility.convertIdentifier(constraint.name) + ": ") + lambdaString;
        }
        else
        {
            StringBuilder builder = new StringBuilder().append(constraint.name).append('\n');
            appendTabString(builder, 1);
            builder.append("(").append('\n');
            if (constraint.externalId != null)
            {
                appendTabString(builder, 2);
                builder.append("~externalId: ").append(convertString(constraint.externalId, true)).append('\n');
            }
            appendTabString(builder, 2);
            builder.append("~function: ").append(lambdaString).append('\n');
            if (constraint.enforcementLevel != null)
            {
                appendTabString(builder, 2);
                builder.append("~enforcementLevel: ").append(constraint.enforcementLevel).append('\n');
            }
            if (constraint.messageFunction != null)
            {
                appendTabString(builder, 2);
                String messageString = constraint.messageFunction.accept(transformer).replaceFirst("\\|", "");
                builder.append("~message: ").append(messageString).append('\n');
            }
            appendTabString(builder, 1);
            builder.append(")");
            return builder.toString();
        }
    }


    public static String renderFunctionTestSuites(Function function, PureGrammarComposerContext context)
    {
        StringBuilder stringBuilder = new StringBuilder();
        if (function.tests == null)
        {
            return stringBuilder.toString();
        }
        stringBuilder.append("\n[\n");
        stringBuilder.append(String.join(",\n", ListIterate.collect(function.tests, suite -> renderFunctionTestSuite(suite, context)))).append("\n");
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public static String renderFunctionTestSuite(FunctionTestSuite functionTestSuite, PureGrammarComposerContext context)
    {
        int baseIndentation = 1;
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation)).append(functionTestSuite.id).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");
        if (functionTestSuite.testData != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("data").append(":\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(functionTestSuite.testData, storeData -> renderFunctionTestData(storeData, baseIndentation + 2, context)))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("];\n");
        }
        // tests
        if (functionTestSuite.tests != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("tests").append(":\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(functionTestSuite.tests, test -> renderFunctionTest((FunctionTest) test, baseIndentation + 2, context)))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("]\n");
        }
        str.append(getTabString(baseIndentation)).append("}");
        return str.toString();
    }

    public static String renderFunctionTestData(StoreTestData storeTestData, int currentInt, PureGrammarComposerContext context)
    {
        StringBuilder dataStrBuilder = new StringBuilder();
        dataStrBuilder.append(getTabString(currentInt)).append("{\n");
        dataStrBuilder.append(getTabString(currentInt + 1)).append("store").append(": ").append(storeTestData.store).append(";\n");
        dataStrBuilder.append(getTabString(currentInt + 1)).append("data").append(":\n");
        dataStrBuilder.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(storeTestData.data, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(currentInt + 2)).build()));
        dataStrBuilder.append(";\n");
        dataStrBuilder.append(getTabString(currentInt)).append("}");
        return dataStrBuilder.toString();
    }


    public static String renderFunctionTest(FunctionTest functionTest, int baseInd, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseInd)).append(functionTest.id).append(":\n");
        str.append(getTabString(baseInd)).append("{\n");

        if (functionTest.doc != null)
        {
            str.append(getTabString(baseInd + 1)).append("doc: ").append(convertString(functionTest.doc, true)  + ";\n");
        }
        // Parameters
        if (functionTest.parameters != null && !functionTest.parameters.isEmpty())
        {
            str.append(getTabString(baseInd + 1)).append("parameters:\n");
            str.append(getTabString(baseInd + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(functionTest.parameters, param -> renderFunctionTestParam(param, baseInd + 2, context)))).append("\n");
            str.append(getTabString(baseInd + 1)).append("]\n");
        }
        // Assertions
        if (functionTest.assertions != null && !functionTest.assertions.isEmpty())
        {
            str.append(getTabString(baseInd + 1)).append("asserts:\n");
            str.append(getTabString(baseInd + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(functionTest.assertions, testAssertion -> HelperTestAssertionGrammarComposer.composeTestAssertion(testAssertion, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(baseInd + 2)).build())))).append("\n");
            str.append(getTabString(baseInd + 1)).append("]\n");
        }
        str.append(getTabString(baseInd)).append("}");
        return str.toString();
    }


    private static String renderFunctionTestParam(ParameterValue parameterValue, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append(parameterValue.name);
        str.append(" = ");
        str.append(parameterValue.value.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()));

        return str.toString();
    }

}
