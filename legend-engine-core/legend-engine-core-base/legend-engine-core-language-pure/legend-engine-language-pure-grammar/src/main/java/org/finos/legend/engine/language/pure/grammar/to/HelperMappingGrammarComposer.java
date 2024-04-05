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

import java.util.List;
import java.util.Optional;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.test.assertion.HelperTestAssertionGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingEnumSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingIntegerSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumValueMappingStringSourceValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.ExpectedOutputMappingTestAssert;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestAssert;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStoreAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;

import java.util.Objects;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperMappingGrammarComposer
{
    public static String renderMappingInclude(MappingInclude mappingInclude, PureGrammarComposerContext context)
    {
        Optional<String> renderedMappingInclude = context.extraMappingIncludeComposers.stream().map(func -> func.apply(mappingInclude)).filter(Objects::nonNull).findFirst();
        if (renderedMappingInclude.isPresent())
        {
            return renderedMappingInclude.get();
        }
        else
        {
            throw new EngineException("Cannot find service to compose provided MappingInclude class type.", EngineErrorType.COMPOSER);
        }
    }

    public static String renderEnumerationMapping(EnumerationMapping enumerationMapping)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(PureGrammarComposerUtility.convertPath(enumerationMapping.enumeration)).append(": EnumerationMapping").append(enumerationMapping.id != null ? (" " + PureGrammarComposerUtility.convertIdentifier(enumerationMapping.id)) : "").append("\n").append(getTabString()).append("{\n");
        if (!enumerationMapping.enumValueMappings.isEmpty())
        {
            builder.append(LazyIterate.collect(enumerationMapping.enumValueMappings, enumValueMapping -> getTabString(2) + renderEnumValueMapping(enumValueMapping)).makeString(",\n"));
            builder.append("\n");
        }
        return builder.append(getTabString()).append("}").toString();
    }

    private static String renderEnumValueMapping(EnumValueMapping enumValueMapping)
    {
        return enumValueMapping.sourceValues.isEmpty() ? "" : PureGrammarComposerUtility.convertIdentifier(enumValueMapping.enumValue)
                + ": ["
                + ListIterate.collect(enumValueMapping.sourceValues,
                sourceValue ->
                {
                    if (sourceValue instanceof EnumValueMappingStringSourceValue)
                    {
                        return convertString(((EnumValueMappingStringSourceValue) sourceValue).value, true);
                    }
                    else if (sourceValue instanceof EnumValueMappingIntegerSourceValue)
                    {
                        return ((EnumValueMappingIntegerSourceValue) sourceValue).value.toString();
                    }
                    else if (sourceValue instanceof EnumValueMappingEnumSourceValue)
                    {
                        return PureGrammarComposerUtility.convertPath(((EnumValueMappingEnumSourceValue) sourceValue).enumeration) + "." + PureGrammarComposerUtility.convertIdentifier(((EnumValueMappingEnumSourceValue) sourceValue).value);
                    }
                    else if (sourceValue instanceof String)
                    {
                        return convertString((String) sourceValue, true);
                    }
                    return sourceValue.toString();
                }).makeString(", ")
                + "]";
    }

    public static String renderAssociationMapping(AssociationMapping associationMapping, PureGrammarComposerContext context)
    {
        if (associationMapping instanceof XStoreAssociationMapping)
        {
            return renderXStoreAssociationMapping((XStoreAssociationMapping) associationMapping, context);
        }
        return context.extraAssociationMappingComposers.stream().map(composer -> composer.value(associationMapping, context)).filter(Objects::nonNull).findFirst().orElseGet(() -> unsupported(associationMapping.getClass()));
    }

    private static String renderXStoreAssociationMapping(XStoreAssociationMapping xStoreAssociationMapping, PureGrammarComposerContext context)
    {
        return xStoreAssociationMapping.association + renderMappingId(xStoreAssociationMapping.id) + ": " + "XStore\n" +
                getTabString() + "{\n" +
                LazyIterate.collect(xStoreAssociationMapping.propertyMappings, p -> getTabString(2) + HelperMappingGrammarComposer.renderXStorePropertyMapping((XStorePropertyMapping) p, context)).makeString(",\n") + (xStoreAssociationMapping.propertyMappings.isEmpty() ? "" : "\n") +
                getTabString() + "}";
    }

    private static String renderXStorePropertyMapping(XStorePropertyMapping xStorePropertyMapping, PureGrammarComposerContext context)
    {
        return PureGrammarComposerUtility.convertIdentifier(xStorePropertyMapping.property.property) +
                (xStorePropertyMapping.source == null || xStorePropertyMapping.source.isEmpty() ? "" : "[" + PureGrammarComposerUtility.convertIdentifier(xStorePropertyMapping.source) + ", " + PureGrammarComposerUtility.convertIdentifier(xStorePropertyMapping.target) + "]") +
                ": " + xStorePropertyMapping.crossExpression.body.get(0).accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
    }

    public static String renderMappingTest(MappingTest_Legacy mappingTestLegacy, DEPRECATED_PureGrammarComposerCore transformer)
    {
        return "  " + mappingTestLegacy.name + "\n" +
                getTabString(2) + "(\n" +
                getTabString(3) + "query: " + mappingTestLegacy.query.accept(transformer) + ";\n" +
                getTabString(3) + "data:\n" +
                getTabString(3) + "[\n" +
                LazyIterate.collect(mappingTestLegacy.inputData, inputData -> getTabString(4) + renderMappingTestInputData(inputData, transformer.toContext())).makeString(",\n") + (mappingTestLegacy.inputData.isEmpty() ? "" : "\n") +
                getTabString(3) + "];\n" +
                getTabString(3) + "assert: " + renderMappingTestAssert(mappingTestLegacy._assert) + ";\n" +
                getTabString(2) + ")";
    }

    private static String renderMappingTestAssert(MappingTestAssert mappingTestAssert)
    {
        if (mappingTestAssert instanceof ExpectedOutputMappingTestAssert)
        {
            return convertString(((ExpectedOutputMappingTestAssert) mappingTestAssert).expectedOutput, false);
        }
        return unsupported(mappingTestAssert.getClass());
    }

    private static String renderMappingTestInputData(InputData inputData, PureGrammarComposerContext context)
    {
        if (inputData instanceof ObjectInputData)
        {
            ObjectInputData objectInputData = (ObjectInputData) inputData;
            return "<Object, " + objectInputData.inputType + ", " + PureGrammarComposerUtility.convertPath(objectInputData.sourceClass) + ", " + convertString(objectInputData.data, false) + ">";
        }
        return context.extraMappingTestInputDataComposers.stream().map(composer -> composer.value(inputData, context)).filter(Objects::nonNull).findFirst().orElseGet(() -> unsupported(inputData.getClass()));
    }

    public static String renderClassMappingId(ClassMapping cm)
    {
        return renderMappingId(cm.id);
    }

    public static String renderMappingId(String id)
    {
        return (id != null ? ("[" + PureGrammarComposerUtility.convertIdentifier(id) + "]") : "");
    }

    public static String renderAggregateSetImplementationContainer(AggregateSetImplementationContainer agg, DEPRECATED_PureGrammarComposerCore transformer)
    {
        String aggregateMapping = "";
        if (agg.setImplementation != null)
        {
            transformer.setBaseTabLevel(4);
            aggregateMapping = "~aggregateMapping" + agg.setImplementation.accept(transformer);
            transformer.setBaseTabLevel(1);
        }
        return "(\n" +
                getTabString(4) + "~modelOperation" + ":" + " {\n" +
                getTabString(5) + "~canAggregate " + (agg.aggregateSpecification.canAggregate ? "true" : "false") + ",\n" +
                getTabString(5) + "~groupByFunctions (\n" +
                LazyIterate.collect(agg.aggregateSpecification.groupByFunctions, groupByFunction -> getTabString(6) + groupByFunction.groupByFn.body.get(0).accept(transformer)).makeString(",\n") +
                "\n" + getTabString(5) + "),\n" +
                getTabString(5) + "~aggregateValues (\n" +
                LazyIterate.collect(agg.aggregateSpecification.aggregateValues, aggregateFunction -> getTabString(6) + "( " + "~mapFn:" + aggregateFunction.mapFn.body.get(0).accept(transformer)
                        + " ," + " ~aggregateFn: " + aggregateFunction.aggregateFn.body.get(0).accept(transformer) + " )").makeString(",\n") +
                "\n" + getTabString(5) + ")\n" +
                getTabString(4) + "},\n" +
                getTabString(4) + aggregateMapping +
                "\n" + getTabString(3) + ")\n";
    }

    public static String renderMappingTestSuite(MappingTestSuite mappingTestSuite, DEPRECATED_PureGrammarComposerCore transformer)
    {
        int baseIndentation = 2;
        StringBuilder str = new StringBuilder();

        str.append(getTabString(1)).append(mappingTestSuite.id).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");
        if (mappingTestSuite.doc != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("doc: ").append(convertString(mappingTestSuite.doc, true)  + ";\n");
        }

        str.append(getTabString(baseIndentation + 1)).append("function: ").append(mappingTestSuite.func.accept(transformer)).append(";\n");
        if (mappingTestSuite.tests != null && !mappingTestSuite.tests.isEmpty())
        {
            str.append(getTabString(baseIndentation + 1)).append("tests:\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(mappingTestSuite.tests, test -> renderMappingTests((MappingTest) test, transformer)))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("];\n");
        }
        str.append(getTabString(baseIndentation)).append("}");
        return str.toString();
    }

    private static String renderMappingTests(MappingTest test, DEPRECATED_PureGrammarComposerCore transformer)
    {
        int baseIndentation = 4;
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation)).append(test.id).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");
        if (test.doc != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("doc: ").append(convertString(test.doc, true)  + ";\n");
        }
        str.append(renderStoreTestData(test.storeTestData, transformer, 4));
        str.append(getTabString(baseIndentation + 1)).append("asserts:\n").append(getTabString(baseIndentation + 1)).append("[\n");
        if (test.assertions != null || test.assertions.isEmpty())
        {
            str.append(String.join(",\n", ListIterate.collect(test.assertions, testAssertion -> HelperTestAssertionGrammarComposer.composeTestAssertion(testAssertion, PureGrammarComposerContext.Builder.newInstance(transformer.toContext()).withIndentationString(getTabString(baseIndentation + 2)).build())))).append("\n");
        }
        str.append(getTabString(baseIndentation + 1)).append("];\n").append(getTabString(baseIndentation)).append("}");
        return str.toString();
    }

    public static String renderStoreTestData(List<StoreTestData> storeTestData, DEPRECATED_PureGrammarComposerCore transformer, int baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation + 1)).append("data").append(":\n");
        str.append(getTabString(baseIndentation + 1)).append("[\n");
        if (!storeTestData.isEmpty())
        {
            str.append(String.join(",\n", ListIterate.collect(storeTestData, data -> renderStoreElementTestData(data, transformer, baseIndentation + 2)))).append("\n");
        }
        str.append(getTabString(baseIndentation + 1)).append("];\n");
        return str.toString();
    }

    public static String renderStoreElementTestData(StoreTestData mappingStoreTestData, DEPRECATED_PureGrammarComposerCore transformer, int baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation)).append(mappingStoreTestData.store).append(":\n");
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(mappingStoreTestData.data, PureGrammarComposerContext.Builder.newInstance(transformer.toContext()).withIndentationString(getTabString(baseIndentation + 1)).build()));
        return str.toString();
    }

}
