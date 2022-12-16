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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CByteStream;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Class;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Enum;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.MappingInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Whatever;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.Pair;

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

                    ValueSpecification vs = data.instances.get(type);
                    if ((vs instanceof ClassInstance && ((ClassInstance) vs).value instanceof Pair))
                    {
                        DataElementReference reference = new DataElementReference();
                        reference.dataElement = ((PackageableElementPtr) ((Pair) ((ClassInstance) data.instances.get(type)).value).second).fullPath;
                        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(reference, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentString).build()));
                    }
                    else if (vs instanceof Collection && ((Collection) vs).values.size() == 1)
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
        return PureGrammarComposerUtility.convertString(cString.value, true);
    }

    @Override
    public String visit(CDateTime cDateTime)
    {
        return HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(cDateTime.value);
    }

    @Override
    public String visit(CLatestDate cLatestDate)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CStrictDate cStrictDate)
    {
        return HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(cStrictDate.value);
    }

    @Override
    public String visit(CStrictTime cStrictTime)
    {
        return HelperValueSpecificationGrammarComposer.generateValidDateValueContainingPercent(cStrictTime.value);
    }

    @Override
    public String visit(CByteStream cByteStream)
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
        return String.valueOf(cBoolean.value);
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

    public String visit(ClassInstance vs)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(CInteger cInteger)
    {
        return String.valueOf(cInteger.value);
    }

    @Override
    public String visit(CDecimal cDecimal)
    {
        return cDecimal.value.toPlainString() + "D";
    }

    @Override
    public String visit(Lambda lambda)
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
        return String.valueOf(cFloat.value);
    }

    @Override
    public String visit(MappingInstance mappingInstance)
    {
        throw new UnsupportedOperationException("Not implemented for ModelStoreData");
    }

    @Override
    public String visit(GenericTypeInstance genericTypeInstance)
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
    public String visit(AppliedProperty appliedProperty)
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
        String key = ((CString) keyExpression.key).value;
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
