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
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelInstanceTestData;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.AppliedQualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.UnknownAppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CBoolean;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CByteArray;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDateTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDecimal;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CFloat;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CInteger;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CLatestDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Class;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Enum;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.EnumValue;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.GenericTypeInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.MappingInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.PrimitiveType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.UnitInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.deprecated.Whatever;

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
        if (data.modelData != null && !data.modelData.isEmpty())
        {
           String modelDataString =  data.modelData.stream().map(this::compose).collect(Collectors.joining(",\n"));
           builder.append(modelDataString);
        }
        return builder.toString();
    }


    public String compose(ModelTestData data)
    {
        StringBuilder str = new StringBuilder();
        String indentString = context.getIndentationString() + PureGrammarComposerUtility.getTabString();
        String type = data.model;
        str.append(context.getIndentationString());
        str.append(type).append(":\n");
        if (data instanceof ModelInstanceTestData)
        {
            ModelInstanceTestData modelInstanceData = (ModelInstanceTestData)  data;
            ValueSpecification vs = modelInstanceData.instances;
            if (vs instanceof PackageableElementPtr)
            {
                DataElementReference reference = new DataElementReference();
                reference.dataElement = new PackageableElementPointer(
                        PackageableElementType.DATA,
                        ((PackageableElementPtr) vs).fullPath,
                        data.sourceInformation
                );
                str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(reference, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentString).build()));
            }
            else if (vs instanceof Collection && ((Collection) vs).values.size() == 1)
            {
                str.append(indentString).append("[\n");

                indentLevel++;
                str.append(indentString).append(PureGrammarComposerUtility.getTabString());
                str.append(vs.accept(this));
                str.append("\n");
                indentLevel--;

                str.append(indentString).append("]");
            }
            else
            {
                str.append(indentString);
                str.append(vs.accept(this));
            }
            return str.toString();
        }
        else if (data instanceof ModelEmbeddedTestData)
        {
            ModelEmbeddedTestData modelEmbeddedData = (ModelEmbeddedTestData) data;
            str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(modelEmbeddedData.data, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentString).build()));
            return str.toString();
        }
        throw new UnsupportedOperationException("Model Data class '" + data.getClass().getName() + "' not supported");

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
    public String visit(CByteArray cByteArray)
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
    public String visit(LambdaFunction lambda)
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
