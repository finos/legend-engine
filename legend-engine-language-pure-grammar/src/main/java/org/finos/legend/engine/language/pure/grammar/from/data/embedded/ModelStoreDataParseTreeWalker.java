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

package org.finos.legend.engine.language.pure.grammar.from.data.embedded;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.modelStore.ModelStoreDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DateParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.domain.StrictTimeParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Pair;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelStoreDataParseTreeWalker
{
    private static final Multiplicity PURE_ONE = multiplicity(1, 1);

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final SourceInformation sourceInformation;
    private final PureGrammarParserExtensions extensions;

    public ModelStoreDataParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.sourceInformation = sourceInformation;
        this.extensions = extensions;
    }

    public ModelStoreData visit(ModelStoreDataParserGrammar.DefinitionContext ctx)
    {
        ModelStoreData result = new ModelStoreData();
        result.sourceInformation = sourceInformation;
        result.instances = Maps.mutable.empty();

        for (ModelStoreDataParserGrammar.TypeIndexedInstancesContext typeIndexedInstancesContext : ctx.typeIndexedInstances())
        {
            String fullPath = PureGrammarParserUtility.fromQualifiedName(typeIndexedInstancesContext.qualifiedName().packagePath() == null ? Collections.emptyList() : typeIndexedInstancesContext.qualifiedName().packagePath().identifier(), typeIndexedInstancesContext.qualifiedName().identifier());
            ValueSpecification instances = null;
            if (typeIndexedInstancesContext.instance() != null)
            {
                instances = collection(typeIndexedInstancesContext.instance().stream().map(this::visitInstance).collect(Collectors.toList()));
            }
            if (typeIndexedInstancesContext.embeddedData() != null)
            {
                EmbeddedData embeddedData = HelperEmbeddedDataGrammarParser.parseEmbeddedData(typeIndexedInstancesContext.embeddedData(), this.walkerSourceInformation, extensions);
                if (embeddedData instanceof DataElementReference)
                {
                    PackageableElementPtr ptr = new PackageableElementPtr();
                    ptr.fullPath = ((DataElementReference) embeddedData).dataElement;
                    PackageableElementPtr bindingPtr = new PackageableElementPtr();
                    bindingPtr.fullPath = "default::m2m::$binding";
                    Pair pair = new Pair();
                    pair.first = bindingPtr;
                    pair.second = ptr;
                    instances = pair;
                }
            }
            if (result.instances.containsKey(fullPath))
            {
                throw new EngineException("Multiple entries found for type: '" + fullPath + "'", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
            }

            result.instances.put(fullPath, instances);
        }

        return result;
    }

    private AppliedFunction visitInstance(ModelStoreDataParserGrammar.InstanceContext ctx)
    {
        ctx.instancePropertyAssignment().stream().map(this::visitPropertyAssignment).collect(Collectors.toList());

        PackageableElementPtr newClass = new PackageableElementPtr();
        newClass.fullPath = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());

        List<ValueSpecification> keyExpressions = ctx.instancePropertyAssignment().stream().map(this::visitPropertyAssignment).collect(Collectors.toList());
        Collection valueAssignments = new Collection();
        valueAssignments.values = keyExpressions;
        valueAssignments.multiplicity = PURE_ONE;

        AppliedFunction appliedFunction = new AppliedFunction();
        appliedFunction.parameters = Lists.mutable.with(newClass, cString("dummy"), valueAssignments);
        appliedFunction.function = "new";
        return appliedFunction;
    }

    private KeyExpression visitPropertyAssignment(ModelStoreDataParserGrammar.InstancePropertyAssignmentContext ctx)
    {
        KeyExpression result = new KeyExpression();
        result.add = false;

        String property = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        result.key = cString(property);
        List<ValueSpecification> values = ctx.instanceRightSide().instanceAtomicRightSide().stream().map(this::visitAtomicRightSide).collect(Collectors.toList());
        result.expression = collection(values);
        return result;
    }

    private ValueSpecification visitAtomicRightSide(ModelStoreDataParserGrammar.InstanceAtomicRightSideContext ctx)
    {
        if (ctx.instanceLiteral() != null)
        {
            return visitInstanceLiteral(ctx.instanceLiteral());
        }
        else if (ctx.enumReference() != null)
        {
            EnumValue result = new EnumValue();
            result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.enumReference());
            result.fullPath = PureGrammarParserUtility.fromQualifiedName(ctx.enumReference().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.enumReference().qualifiedName().packagePath().identifier(), ctx.enumReference().qualifiedName().identifier());
            result.value = PureGrammarParserUtility.fromIdentifier(ctx.enumReference().identifier());
            return result;
        }
        else if (ctx.instance() != null)
        {
            return visitInstance(ctx.instance());
        }
        else
        {
            throw new IllegalStateException("Unhandled atomicRightSide");
        }
    }

    private ValueSpecification visitInstanceLiteral(ModelStoreDataParserGrammar.InstanceLiteralContext ctx)
    {
        ValueSpecification result;
        if (ctx.instanceLiteralToken() != null)
        {
            ModelStoreDataParserGrammar.InstanceLiteralTokenContext literalToken = ctx.instanceLiteralToken();
            if (literalToken.STRING() != null)
            {
                result = cString(PureGrammarParserUtility.fromGrammarString(literalToken.STRING().getText(), true));
            }
            else if (literalToken.INTEGER() != null)
            {
                result = cInteger(Long.parseLong(literalToken.INTEGER().getText()));
            }
            else if (literalToken.FLOAT() != null)
            {
                result = cFloat(Double.parseDouble(literalToken.FLOAT().getText()));
            }
            else if (literalToken.DECIMAL() != null)
            {
                String text = literalToken.DECIMAL().getText();
                result = cDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
            }
            else if (literalToken.DATE() != null)
            {
                result = new DateParseTreeWalker(literalToken.DATE(), this.walkerSourceInformation).visitDefinition();
            }
            else if (literalToken.BOOLEAN() != null)
            {
                result = cBoolean(Boolean.parseBoolean(literalToken.BOOLEAN().getText()));
            }
            else if (literalToken.STRICTTIME() != null)
            {
                result = new StrictTimeParseTreeWalker(literalToken.STRICTTIME(), this.walkerSourceInformation).visitStrictTimeDefinition();
            }
            else
            {
                throw new IllegalStateException("Unhandled instanceLiteralToken");
            }
        }
        else if (ctx.INTEGER() != null && ctx.MINUS() != null)
        {
            result = cInteger(Long.parseLong(ctx.MINUS().getText() + ctx.INTEGER().getText()));
        }
        else if (ctx.FLOAT() != null && ctx.MINUS() != null)
        {
            result = cFloat(Double.parseDouble(ctx.MINUS().getText() + ctx.FLOAT().getText()));
        }
        else if (ctx.DECIMAL() != null && ctx.MINUS() != null)
        {
            String text = ctx.MINUS().getText() + ctx.DECIMAL().getText();
            result = cDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
        }
        else if (ctx.INTEGER() != null && ctx.PLUS() != null)
        {
            result = cInteger(Long.parseLong(ctx.PLUS().getText() + ctx.INTEGER().getText()));
        }
        else if (ctx.FLOAT() != null && ctx.PLUS() != null)
        {
            result = cFloat(Double.parseDouble(ctx.PLUS().getText() + ctx.FLOAT().getText()));
        }
        else if (ctx.DECIMAL() != null && ctx.PLUS() != null)
        {
            String text = ctx.PLUS().getText() + ctx.DECIMAL().getText();
            result = cDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
        }
        else
        {
            throw new IllegalStateException("Unhandled instanceLiteral");
        }
        result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return result;
    }

    private static Collection collection(List<ValueSpecification> values)
    {
        Collection result = new Collection();
        result.values = values;
        result.multiplicity = multiplicity(values.size(), values.size());
        return result;
    }

    private static CString cString(String value)
    {
        return cString(Collections.singletonList(value), PURE_ONE);
    }

    private static CString cString(List<String> values)
    {
        return cString(values, multiplicity(values.size(), values.size()));
    }

    private static CString cString(List<String> values, Multiplicity multiplicity)
    {
        CString result = new CString();
        result.multiplicity = multiplicity;
        result.values = values;
        return result;
    }

    private static CInteger cInteger(long value)
    {
        CInteger result = new CInteger();
        result.multiplicity = PURE_ONE;
        result.values = Collections.singletonList(value);
        return result;
    }

    private static CFloat cFloat(double value)
    {
        CFloat result = new CFloat();
        result.multiplicity = PURE_ONE;
        result.values = Collections.singletonList(value);
        return result;
    }

    private static CDecimal cDecimal(BigDecimal value)
    {
        CDecimal result = new CDecimal();
        result.multiplicity = PURE_ONE;
        result.values = Collections.singletonList(value);
        return result;
    }

    private static CBoolean cBoolean(boolean value)
    {
        CBoolean result = new CBoolean();
        result.multiplicity = PURE_ONE;
        result.values = Collections.singletonList(value);
        return result;
    }

    private static Multiplicity multiplicity(int lowerBound, int upperBound)
    {
        Multiplicity m = new Multiplicity();
        m.lowerBound = lowerBound;
        m.setUpperBound(upperBound);
        return m;
    }
}
