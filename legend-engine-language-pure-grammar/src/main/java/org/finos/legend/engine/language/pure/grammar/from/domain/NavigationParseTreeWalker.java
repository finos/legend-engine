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

import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.navigation.NavigationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.Path;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.path.PropertyPathElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class NavigationParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    NavigationParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation)
    {
        this.walkerSourceInformation = sourceInformation;
    }

    public ValueSpecification visitDefinition(NavigationParserGrammar.DefinitionContext ctx)
    {
        return this.visitPath(ctx);
    }

    private ValueSpecification visitPath(NavigationParserGrammar.DefinitionContext ctx)
    {
        Path path = new Path();
        path.name = ctx.name() != null ? ctx.name().VALID_STRING().getText() : null;
        path.startType = ctx.genericType() != null ? ctx.genericType().getText() : null;
        path.path = ctx.propertyWithParameters() == null ? FastList.newList() : ListIterate.collect(ctx.propertyWithParameters(), this::visitPropertyPathElement);
        path.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return DomainParseTreeWalker.wrapWithClassInstance(path, "path");
    }

    private PropertyPathElement visitPropertyPathElement(NavigationParserGrammar.PropertyWithParametersContext context)
    {
        PropertyPathElement pathElement = new PropertyPathElement();
        pathElement.property = context.VALID_STRING().getText();
        pathElement.parameters = FastList.newList();
        pathElement.sourceInformation = walkerSourceInformation.getSourceInformation(context);
        if (context.parameter() != null && !context.parameter().isEmpty())
        {
            pathElement.parameters = ListIterate.collect(context.parameter(), this::visitParameter);
        }
        return pathElement;
    }

    private ValueSpecification visitParameter(NavigationParserGrammar.ParameterContext ctx)
    {
        if (ctx.scalar() != null)
        {
            return this.visitScalar(ctx.scalar());
        }
        else
        {
            return this.visitCollection(ctx.collection());
        }
    }

    private ValueSpecification visitCollection(NavigationParserGrammar.CollectionContext collectionContext)
    {
        Collection collection = new Collection();
        collection.multiplicity = this.getMultiplicityOneOne();
        collection.values = ListIterate.collect(collectionContext.scalar(), this::visitScalar);
        return collection;
    }

    private ValueSpecification visitScalar(NavigationParserGrammar.ScalarContext ctx)
    {
        if (ctx.atomic() != null)
        {
            return this.visitAtomicExpression(ctx.atomic());
        }
        else
        {
            return this.visitEnumValue(ctx.enumStub());
        }
    }

    private EnumValue visitEnumValue(NavigationParserGrammar.EnumStubContext ctx)
    {
        EnumValue enumValue = new EnumValue();
        enumValue.fullPath = ctx.VALID_STRING(0).getSymbol().getText();
        enumValue.value = ctx.VALID_STRING(1).getSymbol().getText();
        return enumValue;
    }

    private ValueSpecification visitAtomicExpression(NavigationParserGrammar.AtomicContext ctx)
    {
        if (ctx.BOOLEAN() != null)
        {
            CBoolean instance = new CBoolean(Boolean.parseBoolean(ctx.getText()));
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return instance;
        }
        else if (ctx.STRING() != null)
        {
            CString instance = getInstanceString(ctx.getText());
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return instance;
        }
        else if (ctx.INTEGER() != null)
        {
            CInteger instance = getInstanceInteger(ctx.getText());
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return instance;
        }
        else if (ctx.FLOAT() != null)
        {
            CFloat instance = new CFloat(Double.parseDouble(ctx.getText()));
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return instance;
        }
        else if (ctx.DATE() != null)
        {
            String var = ctx.getText();
            CDateTime instance = new CDateTime(var.substring(var.lastIndexOf('%') + 1));
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return instance;
        }
        else if (ctx.LATEST_DATE() != null)
        {
            CLatestDate instance = new CLatestDate();
            instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            instance.multiplicity = getMultiplicityOneOne();
            return instance;
        }
        throw new EngineException("Unsupported scalar expression for property path: " + ctx.getText());
    }

    private CInteger getInstanceInteger(String integerString)
    {
        return new CInteger(Long.parseLong(integerString));
    }

    private CString getInstanceString(String string)
    {
        return new CString(PureGrammarParserUtility.fromGrammarString(string, true));
    }

    private Multiplicity getMultiplicityOneOne()
    {
        Multiplicity m = new Multiplicity();
        m.lowerBound = 1;
        m.setUpperBound(1);
        return m;
    }
}
