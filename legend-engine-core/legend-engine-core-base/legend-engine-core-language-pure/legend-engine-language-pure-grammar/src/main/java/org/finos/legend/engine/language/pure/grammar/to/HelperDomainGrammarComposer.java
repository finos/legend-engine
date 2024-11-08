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
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.AggregationKind;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Constraint;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.QualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
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
    private static final String DEFAULT_TESTABLE_ID = "default";
    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_XML = "application/xml";

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
        return PureGrammarComposerUtility.convertIdentifier(unit.name) + (unit.conversionFunction == null ? ";" : ": " + renderUnitLambda(unit.conversionFunction, transformer) + ";");
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
        return renderAnnotations(property.stereotypes, property.taggedValues) + renderAggregation(property.aggregation) + PureGrammarComposerUtility.convertIdentifier(property.name) + ": " + HelperValueSpecificationGrammarComposer.printGenericType(property.genericType, transformer) + "[" + renderMultiplicity(property.multiplicity) + "]" + (property.defaultValue != null ? " = " + property.defaultValue.value.accept(transformer) : "");
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
                + HelperValueSpecificationGrammarComposer.printGenericType(qualifiedProperty.returnGenericType, transformer) + "[" + renderMultiplicity(qualifiedProperty.returnMultiplicity) + "]";
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
        if (function.tests.isEmpty())
        {
            return stringBuilder.toString();
        }
        stringBuilder.append("\n{\n");
        stringBuilder.append(String.join("\n" + (function.tests.size() > 1 ? "\n" : ""), ListIterate.collect(function.tests, suite -> renderFunctionTestSuite(function, suite, context))));
        stringBuilder.append("\n}");
        return stringBuilder.toString();
    }

    public static String renderFunctionTestSuite(Function function, FunctionTestSuite functionTestSuite, PureGrammarComposerContext context)
    {
        int baseIndentation = 1;
        StringBuilder str = new StringBuilder();
        if (!functionTestSuite.id.equals(DEFAULT_TESTABLE_ID))
        {
            str.append(getTabString(baseIndentation)).append(functionTestSuite.id).append("\n").append(getTabString(baseIndentation)).append("(\n");
            if (functionTestSuite.testData != null && !functionTestSuite.testData.isEmpty())
            {
                str.append(String.join("\n", ListIterate.collect(functionTestSuite.testData, test -> renderFunctionTestData(test, baseIndentation + 1, context)))).append("\n");
            }
            if (functionTestSuite.tests != null)
            {
                str.append(String.join("\n", ListIterate.collect(functionTestSuite.tests, test -> renderFunctionTest(function, (FunctionTest) test, baseIndentation + 1, context))));
            }
            str.append("\n").append(getTabString(baseIndentation)).append(")");
        }
        else
        {
            if (functionTestSuite.testData != null && !functionTestSuite.testData.isEmpty())
            {
                str.append(String.join("\n", ListIterate.collect(functionTestSuite.testData, test -> renderFunctionTestData(test, baseIndentation, context)))).append("\n");
            }
            if (functionTestSuite.tests != null)
            {
                str.append(String.join("\n", ListIterate.collect(functionTestSuite.tests, test -> renderFunctionTest(function, (FunctionTest) test, baseIndentation, context))));
            }
        }
        return str.toString();
    }

    public static String renderFunctionTestData(StoreTestData storeTestData, int currentInt, PureGrammarComposerContext context)
    {
        StringBuilder dataStrBuilder = new StringBuilder();
        dataStrBuilder.append(getTabString(currentInt));
        dataStrBuilder.append(HelperRuntimeGrammarComposer.renderStoreProviderPointer(storeTestData.store)).append(":");
        EmbeddedData embeddedData = storeTestData.data;
        if (embeddedData instanceof DataElementReference)
        {
            dataStrBuilder.append(" ");
            dataStrBuilder.append(((DataElementReference) embeddedData).dataElement.path);
        }
        else if (embeddedData instanceof ExternalFormatData)
        {
            dataStrBuilder.append(" ");
            dataStrBuilder.append(renderSimpleExternalFormat(((ExternalFormatData) embeddedData)));
        }
        else
        {
            dataStrBuilder.append("\n");
            dataStrBuilder.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(storeTestData.data, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(currentInt + 2)).build()));
        }
        dataStrBuilder.append(";");
        return dataStrBuilder.toString();
    }


    public static String renderFunctionTest(Function function, FunctionTest functionTest, int currentInt, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(currentInt)).append(functionTest.id);
        if (functionTest.doc != null)
        {
            str.append(" ").append(PureGrammarComposerUtility.convertString(functionTest.doc, true));
        }
        str.append(" | ").append(HelperValueSpecificationGrammarComposer.getFunctionNameWithNoPackage(function)).append("(");
        if (functionTest.parameters != null && !functionTest.parameters.isEmpty())
        {
            str.append(LazyIterate.collect(functionTest.parameters, parameterValue -> parameterValue.value.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build())).makeString(","));
        }
        str.append(") => ");
        if (functionTest.assertions.size() > 1)
        {
            throw new EngineException("Unable to generate grammar for function tests with more than one assertion", functionTest.sourceInformation, EngineErrorType.COMPOSER);
        }
        if (functionTest.assertions.size() == 1)
        {
            str.append(renderTestAssertion(functionTest.assertions.get(0), context));
        }
        str.append(";");
        return str.toString();
    }

    private static String renderTestAssertion(TestAssertion testAssertion, PureGrammarComposerContext context)
    {
        if (testAssertion instanceof EqualTo)
        {
            return ((EqualTo) testAssertion).expected.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
        }
        else if (testAssertion instanceof EqualToJson)
        {
            EqualToJson equalToJson = (EqualToJson) testAssertion;
            ExternalFormatData externalFormatData = equalToJson.expected;
            return renderSimpleExternalFormat(externalFormatData);
        }
        else
        {
            throw new EngineException("Unknown test assertion type: " + testAssertion.toString(), testAssertion.sourceInformation, EngineErrorType.COMPOSER);
        }
    }

    private static String renderSimpleExternalFormat(ExternalFormatData externalFormatData)
    {
        if (externalFormatData.contentType.equals(APPLICATION_JSON))
        {
            return "(JSON) " + PureGrammarComposerUtility.convertString(externalFormatData.data, true);
        }
        else if (externalFormatData.contentType.equals(APPLICATION_XML))
        {
            return "(XML) " + PureGrammarComposerUtility.convertString(externalFormatData.data, true);
        }
        else
        {
            return "(" + externalFormatData.contentType + ") " + PureGrammarComposerUtility.convertString(externalFormatData.data, true);
        }
    }
}
