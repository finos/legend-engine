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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Constraint;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.QualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameterComplexValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.functionTest.FunctionTestParameterPrimitiveValue;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
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
        return renderAnnotations(property.stereotypes, property.taggedValues) + PureGrammarComposerUtility.convertIdentifier(property.name) + ": " + property.type + "[" + renderMultiplicity(property.multiplicity) + "]" + (property.defaultValue != null ? " = " + property.defaultValue.value.accept(transformer) : "");
    }

    public static String renderDerivedProperty(QualifiedProperty qualifiedProperty, DEPRECATED_PureGrammarComposerCore transformer)
    {
        List<Variable> functionParameters = qualifiedProperty.parameters.stream().filter(p -> !p.name.equals("this")).collect(Collectors.toList());
        return renderAnnotations(qualifiedProperty.stereotypes, qualifiedProperty.taggedValues)
                + PureGrammarComposerUtility.convertIdentifier(qualifiedProperty.name) + "("
                + LazyIterate.collect(functionParameters, p -> p.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(transformer).withVariableInFunctionSignature().build())).makeString(",")
                + ") {"
                + LazyIterate.collect(qualifiedProperty.body, b -> b.accept(transformer)).makeString("\n")
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

//---------------------------------------------------------- Function Tests ----------------------------------------------------------
    public static String renderFunctionTests(List<FunctionTest> functionTests, PureGrammarComposerContext context)
    {
        if (!functionTests.isEmpty())
        {
            int baseIndentation = 0;
            StringBuilder builder = new StringBuilder().append("<\n");

            builder.append(String.join(",\n", ListIterate.collect(functionTests, test -> renderFunctionTest((FunctionTest) test, baseIndentation + 1, context)))).append("\n");

            builder.append(">\n");
            return builder.toString();
        }
        else
        {
            return "";
        }
    }

    public static String renderFunctionTest(FunctionTest functionTest, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append(" ").append(functionTest.id).append(": ");
        builder.append(getTabString(0)).append("{");
        // Parameters
        if (functionTest.parameters != null && !functionTest.parameters.isEmpty())
        {
            builder.append(getTabString(0)).append("[");

            if (functionTest.parameters.get(0).name != null)
            {
                builder.append(String.join(", ", ListIterate.collect(functionTest.parameters, param -> renderFunctionTestParameterWithName(param, 0, context)))).append("");
            }
            else
            {
                builder.append(String.join(", ", ListIterate.collect(functionTest.parameters, param -> renderFunctionTestParameterWithoutName(param, 0, context)))).append("");
            }
            builder.append(getTabString(0)).append("]");
        }
        else
        {
            builder.append(getTabString(0)).append("[]");
        }
        builder.append(", ");
        // Assert
        if (functionTest.assertions != null && !functionTest.assertions.isEmpty())
        {
            TestAssertion testAssertion = functionTest.assertions.get(0);
            if (testAssertion instanceof EqualTo)
            {
                EqualTo functionTestAssertion = (EqualTo) testAssertion;
                builder.append(functionTestAssertion.expected.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()));
            }
            else if (testAssertion instanceof EqualToJson)
            {
                EqualToJson functionTestAssertion = (EqualToJson) testAssertion;
                builder.append("#{");
                builder.append(functionTestAssertion.expected.data);
                builder.append("}#");
            }
        }
        builder.append(getTabString(0)).append("}");

        return builder.toString();
    }

    public static String renderFunctionTestParameterWithName(FunctionTestParameter parameter, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        if (parameter instanceof FunctionTestParameterPrimitiveValue)
        {
            FunctionTestParameterPrimitiveValue functionValueParameter = (FunctionTestParameterPrimitiveValue) parameter;
            str.append(getTabString(baseIndentation)).append(functionValueParameter.name);
            str.append(" = ");
            str.append(functionValueParameter.value.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()));
        }
        else if (parameter instanceof FunctionTestParameterComplexValue)
        {
            FunctionTestParameterComplexValue functionTestParameterComplexValue = (FunctionTestParameterComplexValue) parameter;
            str.append(getTabString(baseIndentation)).append(functionTestParameterComplexValue.name);
            str.append(" = ");
            str.append("#{");
            str.append(functionTestParameterComplexValue.externalFormatData.data);
            str.append("}#");
        }

        return str.toString();
    }

    public static String renderFunctionTestParameterWithoutName(FunctionTestParameter parameter, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        if (parameter instanceof FunctionTestParameterPrimitiveValue)
        {
            FunctionTestParameterPrimitiveValue functionValueParameter = (FunctionTestParameterPrimitiveValue) parameter;
            str.append(functionValueParameter.value.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()));
        }
        else if (parameter instanceof FunctionTestParameterComplexValue)
        {
            FunctionTestParameterComplexValue functionTestParameterComplexValue = (FunctionTestParameterComplexValue) parameter;
            str.append("#{");
            str.append(functionTestParameterComplexValue.externalFormatData.data);
            str.append("}#");
        }

        return str.toString();
    }



}
