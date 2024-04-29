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

import java.util.Set;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.modelStore.ModelStoreDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DateParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.domain.StrictTimeParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelInstanceTestData;
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
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ModelStoreDataParseTreeWalker
{
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
        Set<String> _classes = Sets.mutable.empty();
        List<ModelTestData> modelStoreModelData = Lists.mutable.empty();

        for (ModelStoreDataParserGrammar.TypeIndexedInstancesContext typeIndexedInstancesContext : ctx.typeIndexedInstances())
        {
            String fullPath = PureGrammarParserUtility.fromQualifiedName(typeIndexedInstancesContext.qualifiedName().packagePath() == null ? Collections.emptyList() : typeIndexedInstancesContext.qualifiedName().packagePath().identifier(), typeIndexedInstancesContext.qualifiedName().identifier());
            ModelTestData modelTestData;
            if (typeIndexedInstancesContext.embeddedData() != null)
            {
                EmbeddedData embeddedData = HelperEmbeddedDataGrammarParser.parseEmbeddedData(typeIndexedInstancesContext.embeddedData(), this.walkerSourceInformation, extensions);
                ModelEmbeddedTestData modelEmbeddedData = new ModelEmbeddedTestData();
                modelEmbeddedData.data = embeddedData;
                modelEmbeddedData.model = fullPath;
                modelTestData = modelEmbeddedData;
            }
            else if (typeIndexedInstancesContext.instance() != null)
            {
                ModelInstanceTestData modelInstanceData = new ModelInstanceTestData();
                modelInstanceData.model = fullPath;
                ValueSpecification instances = collection(typeIndexedInstancesContext.instance().stream().map(this::visitInstance).collect(Collectors.toList()));
                modelInstanceData.instances = instances;
                modelTestData = modelInstanceData;
            }
            else
            {
                throw new EngineException("No valid model store data provided for type '" + fullPath + "'", walkerSourceInformation.getSourceInformation(typeIndexedInstancesContext), EngineErrorType.PARSER);
            }
            if (_classes.contains(fullPath))
            {
                throw new EngineException("Multiple entries found for type: '" + fullPath + "'", walkerSourceInformation.getSourceInformation(typeIndexedInstancesContext), EngineErrorType.PARSER);
            }
            modelTestData.sourceInformation = walkerSourceInformation.getSourceInformation(typeIndexedInstancesContext);
            modelStoreModelData.add(modelTestData);
            _classes.add(fullPath);
        }
        if (!modelStoreModelData.isEmpty())
        {
            result.modelData = modelStoreModelData;
        }
        return result;
    }

    private AppliedFunction visitInstance(ModelStoreDataParserGrammar.InstanceContext ctx)
    {
        ctx.instancePropertyAssignment().stream().map(this::visitPropertyAssignment).collect(Collectors.toList());

        PackageableElementPtr newClass = new PackageableElementPtr();
        newClass.fullPath = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());

        List<ValueSpecification> keyExpressions = ctx.instancePropertyAssignment().stream().map(this::visitPropertyAssignment).collect(Collectors.toList());
        Collection valueAssignments = new Collection(keyExpressions);

        AppliedFunction appliedFunction = new AppliedFunction();
        appliedFunction.parameters = Lists.mutable.with(newClass, new CString("dummy"), valueAssignments);
        appliedFunction.function = "new";
        return appliedFunction;
    }

    private KeyExpression visitPropertyAssignment(ModelStoreDataParserGrammar.InstancePropertyAssignmentContext ctx)
    {
        KeyExpression result = new KeyExpression();
        result.add = false;
        String property = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        result.key = new CString(property);
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
                result = new CString(PureGrammarParserUtility.fromGrammarString(literalToken.STRING().getText(), true));
            }
            else if (literalToken.INTEGER() != null)
            {
                result = new CInteger(Long.parseLong(literalToken.INTEGER().getText()));
            }
            else if (literalToken.FLOAT() != null)
            {
                result = new CFloat(Double.parseDouble(literalToken.FLOAT().getText()));
            }
            else if (literalToken.DECIMAL() != null)
            {
                String text = literalToken.DECIMAL().getText();
                result = new CDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
            }
            else if (literalToken.DATE() != null)
            {
                result = new DateParseTreeWalker(literalToken.DATE(), this.walkerSourceInformation).visitDefinition();
            }
            else if (literalToken.BOOLEAN() != null)
            {
                result = new CBoolean(Boolean.parseBoolean(literalToken.BOOLEAN().getText()));
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
            result = new CInteger(Long.parseLong(ctx.MINUS().getText() + ctx.INTEGER().getText()));
        }
        else if (ctx.FLOAT() != null && ctx.MINUS() != null)
        {
            result = new CFloat(Double.parseDouble(ctx.MINUS().getText() + ctx.FLOAT().getText()));
        }
        else if (ctx.DECIMAL() != null && ctx.MINUS() != null)
        {
            String text = ctx.MINUS().getText() + ctx.DECIMAL().getText();
            result = new CDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
        }
        else if (ctx.INTEGER() != null && ctx.PLUS() != null)
        {
            result = new CInteger(Long.parseLong(ctx.PLUS().getText() + ctx.INTEGER().getText()));
        }
        else if (ctx.FLOAT() != null && ctx.PLUS() != null)
        {
            result = new CFloat(Double.parseDouble(ctx.PLUS().getText() + ctx.FLOAT().getText()));
        }
        else if (ctx.DECIMAL() != null && ctx.PLUS() != null)
        {
            String text = ctx.PLUS().getText() + ctx.DECIMAL().getText();
            result = new CDecimal(new BigDecimal(text.substring(0, text.length() - 1)));
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
        return new Collection(values);
    }
}
