// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to.data;

import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.AggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ExecutionContextInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Pair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.RuntimeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.SerializationConfig;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSAggregateValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSColumnInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TDSSortInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapAggregation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.TdsOlapRank;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.RootGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.path.Path;

import java.util.List;
import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ModelStoreDataGrammarComposer implements ValueSpecificationVisitor<String>
{
    private static final String PARENTHESES = "()";
    private static final String BRACKETS = "[]";

    private final PureGrammarComposerContext context;
    private final Stack<String> collectionStyle = new Stack<>();
    private int indentLevel = 1;

    public ModelStoreDataGrammarComposer(PureGrammarComposerContext context)
    {
        this.context = context;
        this.collectionStyle.push(BRACKETS);
    }

    public String compose(ModelStoreData data)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(
                data.instances.keySet().stream().map(type ->
                {
                    String indentString = context.getIndentationString() + PureGrammarComposerUtility.getTabString();

                    StringBuilder str = new StringBuilder();
                    str.append(context.getIndentationString());
                    str.append(type).append(":\n");

                    if (!(data.instances.get(type) instanceof Pair))
                    {
                        if (((Collection) data.instances.get(type)).values.size() == 1)
                        {
                            str.append(indentString).append("[\n");

                            indentLevel++;
                            str.append(indentString).append(PureGrammarComposerUtility.getTabString());
                            str.append(data.instances.get(type).accept(this));
                            str.append("\n");
                            indentLevel--;

                            str.append(indentString).append("]");
                        }
                        else
                        {
                            str.append(indentString);
                            str.append(data.instances.get(type).accept(this));
                        }
                    }
                    else
                    {
                        DataElementReference reference = new DataElementReference();
                        reference.dataElement = ((PackageableElementPtr)((Pair) data.instances.get(type)).second).fullPath;
                        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(reference, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentString).build()));
                    }
                    return str.toString();
                }).collect(Collectors.joining(",\n")));

        return builder.toString();
    }

    @Override
    public String visit(ValueSpecification valueSpecification)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(PackageableElementPtr packageableElementPtr)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Whatever whatever)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CString cString)
    {
        return formatCollection(cString.values.stream().map(v -> (Supplier<String>) () -> PureGrammarComposerUtility.convertString(v, true)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(CDateTime cDateTime)
    {
        return formatCollection(cDateTime.values.stream().map(v -> (Supplier<String>) () -> HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(CLatestDate cLatestDate)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CStrictDate cStrictDate)
    {
        return formatCollection(cStrictDate.values.stream().map(v -> (Supplier<String>) () -> HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(CStrictTime cStrictTime)
    {
        return formatCollection(cStrictTime.values.stream().map(v -> (Supplier<String>) () -> HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(AggregateValue aggregateValue)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Class aClass)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CBoolean cBoolean)
    {
        return formatCollection(cBoolean.values.stream().map(v -> (Supplier<String>) () -> String.valueOf(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(UnknownAppliedFunction unknownAppliedFunction)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Enum anEnum)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(EnumValue enumValue)
    {
        return PureGrammarComposerUtility.convertPath(enumValue.fullPath) +
                "." +
                PureGrammarComposerUtility.convertIdentifier(enumValue.value);
    }

    @Override
    public String visit(RuntimeInstance runtimeInstance)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Path path)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CInteger cInteger)
    {
        return formatCollection(cInteger.values.stream().map(v -> (Supplier<String>) () -> String.valueOf(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(CDecimal cDecimal)
    {
        return formatCollection(cDecimal.values.stream().map(v -> (Supplier<String>) () -> v.toPlainString() + 'D').collect(Collectors.toList()), true);
    }

    @Override
    public String visit(Lambda lambda)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(ExecutionContextInstance executionContextInstance)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Pair pair)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(PureList pureList)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Variable variable)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CFloat cFloat)
    {
        return formatCollection(cFloat.values.stream().map(v -> (Supplier<String>) () -> String.valueOf(v)).collect(Collectors.toList()), true);
    }

    @Override
    public String visit(MappingInstance mappingInstance)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(HackedClass hackedClass)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(Collection collection)
    {
        List<ValueSpecification> values = collection.values;
        boolean onOneLine = values.size() <= 1 || isPrimitiveValue(values.get(0));
        List<Supplier<String>> texts = values.stream().map(vs -> (Supplier<String>) () -> vs.accept(this)).collect(Collectors.toList());
        return formatCollection(texts, onOneLine);
    }

    public String formatCollection(List<Supplier<String>> values, boolean onOneLine)
    {
        String style = collectionStyle.peek();

        if (values.isEmpty())
        {
            return style;
        }

        if (values.size() == 1 && style.equals(BRACKETS))
        {
            return values.get(0).get();
        }

        StringBuilder builder = new StringBuilder();
        builder.append(style.charAt(0));
        indentLevel++;
        collectionStyle.push(BRACKETS);
        builder.append(onOneLine ? "" : "\n" + context.getIndentationString() + PureGrammarComposerUtility.getTabString(indentLevel));
        String glue = onOneLine ? ", " : ",\n" + context.getIndentationString() + PureGrammarComposerUtility.getTabString(indentLevel);
        for (int i = 0; i < values.size(); i++)
        {
            if (i > 0)
            {
                builder.append(glue);
            }
            builder.append(values.get(i).get());
        }
        collectionStyle.pop();
        indentLevel--;
        builder.append(onOneLine ? "" : "\n" + context.getIndentationString() + PureGrammarComposerUtility.getTabString(indentLevel));
        builder.append(style.charAt(1));
        return builder.toString();
    }

    @Override
    public String visit(AppliedFunction appliedFunction)
    {
        if (appliedFunction.function.equals("new"))
        {
            String _class = ((PackageableElementPtr) appliedFunction.parameters.get(0)).fullPath;
            StringBuilder builder = new StringBuilder();
            builder.append("^" + _class);
            collectionStyle.push(PARENTHESES);
            builder.append(appliedFunction.parameters.get(2).accept(this));
            collectionStyle.pop();
            return builder.toString();
        }
        else
        {
            throw new UnsupportedOperationException("Not implemented for ModelStoreData");
        }
    }

    @Override
    public String visit(AppliedQualifiedProperty appliedQualifiedProperty)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(RootGraphFetchTree rootGraphFetchTree)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(SerializationConfig serializationConfig)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(AppliedProperty appliedProperty)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(TdsOlapAggregation tdsOlapAggregation)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(TDSAggregateValue tdsAggregateValue)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(TDSSortInformation tdsSortInformation)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(TDSColumnInformation tdsColumnInformation)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(TdsOlapRank tdsOlapRank)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(HackedUnit hackedUnit)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(UnitInstance unitInstance)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(UnitType unitType)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(KeyExpression keyExpression)
    {
        String key = ((CString) keyExpression.key).values.get(0);
        collectionStyle.push(BRACKETS);
        String value = keyExpression.expression.accept(this);
        collectionStyle.pop();
        return key + " = " + value;
    }

    @Override
    public String visit(PrimitiveType primitiveType)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    private static boolean isPrimitiveValue(ValueSpecification valueSpecification)
    {
        return HelperValueSpecificationGrammarComposer.isPrimitiveValue(valueSpecification);
    }
}
