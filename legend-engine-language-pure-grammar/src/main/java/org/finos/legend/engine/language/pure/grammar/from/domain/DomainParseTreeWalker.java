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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.graphFetchTree.GraphFetchTreeLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.graphFetchTree.GraphFetchTreeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.navigation.NavigationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.navigation.NavigationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Constraint;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.DefaultValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Measure;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Property;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.QualifiedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Unit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedProperty;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CLatestDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.KeyExpression;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DomainParseTreeWalker
{
    private static final String TILDE = "~";

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;
    private final boolean allowPropertyBracketExpression;
    private ImportAwareCodeSection section;

    /**
     * This constructor is used for standard M3Walker when we see ###Pure.
     */
    public DomainParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext parserContext, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
        this.section = section;
        this.allowPropertyBracketExpression = false;
    }

    // TODO PropertyBracketExpression is deprecated.  Remove parameter once all use has been addressed
    public DomainParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext parserContext, boolean allowPropertyBracketExpression)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
        this.allowPropertyBracketExpression = allowPropertyBracketExpression;
    }

    public void visitDefinition(DomainParserGrammar.DefinitionContext ctx, Consumer<PackageableElement> elementConsumer)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.elementDefinition().stream().map(this::visitElement).peek(e -> this.section.elements.add(e.getPath())).forEach(elementConsumer);
    }

    private PackageableElement visitElement(DomainParserGrammar.ElementDefinitionContext ctx)
    {
        if (ctx.classDefinition() != null)
        {
            return visitClass(ctx.classDefinition());
        }
        if (ctx.association() != null)
        {
            return visitAssociation(ctx.association());
        }
        if (ctx.enumDefinition() != null)
        {
            return visitEnumeration(ctx.enumDefinition());
        }
        if (ctx.profile() != null)
        {
            return visitProfile(ctx.profile());
        }
        if (ctx.functionDefinition() != null)
        {
            return visitFunction(ctx.functionDefinition());
        }
        if (ctx.measureDefinition() != null)
        {
            return visitMeasure(ctx.measureDefinition());
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }


    // ----------------------------------------------- PROFILE -----------------------------------------------

    private Profile visitProfile(DomainParserGrammar.ProfileContext ctx)
    {
        Profile profile = new Profile();
        profile.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        profile._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        profile.stereotypes = ctx.stereotypeDefinitions() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.stereotypeDefinitions().identifier(), PureGrammarParserUtility::fromIdentifier);
        profile.tags = ctx.tagDefinitions() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.tagDefinitions().identifier(), PureGrammarParserUtility::fromIdentifier);
        profile.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return profile;
    }

    private List<TaggedValue> visitTaggedValues(DomainParserGrammar.TaggedValuesContext ctx)
    {
        return ListIterate.collect(ctx.taggedValue(), taggedValueContext ->
        {
            TaggedValue taggedValue = new TaggedValue();
            TagPtr tagPtr = new TagPtr();
            taggedValue.tag = tagPtr;
            tagPtr.profile = PureGrammarParserUtility.fromQualifiedName(taggedValueContext.qualifiedName().packagePath() == null ? Collections.emptyList() : taggedValueContext.qualifiedName().packagePath().identifier(), taggedValueContext.qualifiedName().identifier());
            tagPtr.value = PureGrammarParserUtility.fromIdentifier(taggedValueContext.identifier());
            taggedValue.value = PureGrammarParserUtility.fromGrammarString(taggedValueContext.STRING().getText(), true);
            taggedValue.tag.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.qualifiedName());
            taggedValue.tag.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.identifier());
            taggedValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext);
            return taggedValue;
        });
    }

    private List<StereotypePtr> visitStereotypes(DomainParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext);
            return stereotypePtr;
        });
    }

    // ----------------------------------------------- DEFAULT VALUE -----------------------------------------------

    private DefaultValue visitDefaultValue(DomainParserGrammar.DefaultValueExpressionContext ctx)
    {
        DefaultValue defaultValue = new DefaultValue();
        defaultValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.defaultValueExpressionsArray() != null)
        {
            List<ValueSpecification> expressions = ListIterate.collect(ctx.defaultValueExpressionsArray().defaultValueExpression(), this::visitDefaultValueExpression);
            defaultValue.value = this.collect(expressions, walkerSourceInformation.getSourceInformation(ctx));
        }
        else
        {
            defaultValue.value = visitDefaultValueExpression(ctx);
        }

        return defaultValue;
    }

    private ValueSpecification visitDefaultValueExpression(DomainParserGrammar.DefaultValueExpressionContext ctx)
    {
        ValueSpecification result = null;

        if (ctx.instanceReference() != null)
        {
            result = this.instanceReference(ctx.instanceReference(), Lists.mutable.empty(), null, " ", false);
        }
        else if (ctx.expressionInstance() != null)
        {
            result = this.newFunction(ctx.expressionInstance(), Lists.mutable.empty(), null, false, " ");
        }
        else if (ctx.instanceLiteralToken() != null)
        {
            result = this.instanceLiteralToken(ctx.instanceLiteralToken(), false);
        }

        if (ctx.propertyExpression() != null)
        {
            result = this.propertyExpression(ctx.propertyExpression(), result, Lists.mutable.empty(), null, " ", false);
        }

        return result;
    }

    // ----------------------------------------------- ENUMERATION -----------------------------------------------

    private Enumeration visitEnumeration(DomainParserGrammar.EnumDefinitionContext ctx)
    {
        Enumeration enumeration = new Enumeration();
        enumeration.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        enumeration._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        enumeration.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        enumeration.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        enumeration.values = ListIterate.collect(ctx.enumValue(), this::visitEnumValue);
        enumeration.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return enumeration;
    }

    private EnumValue visitEnumValue(DomainParserGrammar.EnumValueContext ctx)
    {
        EnumValue enumValue = new EnumValue();
        enumValue.value = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        enumValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        enumValue.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        enumValue.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        return enumValue;
    }


    // ----------------------------------------------- CLASS -----------------------------------------------

    private Class visitClass(DomainParserGrammar.ClassDefinitionContext ctx)
    {
        // TODO: break if use of generics!
        Class _class = new Class();
        _class._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        _class.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        _class.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        _class.constraints = ctx.constraints() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.constraints().constraint(), c -> this.visitConstraint(ctx.constraints().constraint(), c));
        // TODO ? add source info specific to each superType
        _class.superTypes = ctx.EXTENDS() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.type(), t -> PureGrammarParserUtility.fromQualifiedName(t.qualifiedName().packagePath() == null ? Collections.emptyList() : t.qualifiedName().packagePath().identifier(), t.qualifiedName().identifier()));
        _class.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        _class.properties = ctx.classBody().properties().property() == null ? new ArrayList<>() : ListIterate.collect(ctx.classBody().properties().property(), this::visitSimpleProperty);
        _class.qualifiedProperties = ctx.classBody().properties().qualifiedProperty() == null ? new ArrayList<>() : ListIterate.collect(ctx.classBody().properties().qualifiedProperty(), this::visitDerivedProperty);
        _class.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return _class;
    }

    private Constraint visitConstraint(List<DomainParserGrammar.ConstraintContext> constraintContexts, DomainParserGrammar.ConstraintContext ctx)
    {
        Constraint constraint = new Constraint();
        constraint.functionDefinition = new Lambda();
        List<String> typeParametersNames = new ArrayList<>();

        if (ctx.simpleConstraint() != null)
        {
            DomainParserGrammar.SimpleConstraintContext simpleConstraintContext = ctx.simpleConstraint();
            constraint.name = simpleConstraintContext.constraintId() == null ? String.valueOf(constraintContexts.indexOf((ctx))) : PureGrammarParserUtility.fromIdentifier(simpleConstraintContext.constraintId().identifier());
            DomainParseTreeWalker.LambdaContext lambdaContext = new DomainParseTreeWalker.LambdaContext(constraint.name.replace("::", "_"));
            ValueSpecification valueSpecification = this.combinedExpression(simpleConstraintContext.combinedExpression(), "line", typeParametersNames, lambdaContext, "", true, false);
            constraint.functionDefinition.body = Collections.singletonList(valueSpecification);
            constraint.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        }
        else if (ctx.complexConstraint() != null)
        {
            DomainParserGrammar.ComplexConstraintContext complexConstraintContext = ctx.complexConstraint();
            constraint.name = PureGrammarParserUtility.fromIdentifier(complexConstraintContext.identifier());
            constraint.enforcementLevel = complexConstraintContext.constraintEnforcementLevel() != null ? complexConstraintContext.constraintEnforcementLevel().constraintEnforcementLevelType().getText() : null;
            constraint.externalId = complexConstraintContext.constraintExternalId() != null ? PureGrammarParserUtility.fromGrammarString(complexConstraintContext.constraintExternalId().STRING().getText(), true) : null;
            DomainParseTreeWalker.LambdaContext lambdaContext = new DomainParseTreeWalker.LambdaContext(constraint.name.replace("::", "_"));
            constraint.functionDefinition.body = Collections.singletonList(this.combinedExpression(complexConstraintContext.constraintFunction().combinedExpression(), "constraint", typeParametersNames, lambdaContext, "", true, false));
            if (complexConstraintContext.constraintMessage() != null)
            {
                constraint.messageFunction = new Lambda();
                constraint.messageFunction.body = Collections.singletonList(this.combinedExpression(complexConstraintContext.constraintMessage().combinedExpression(), "message", typeParametersNames, lambdaContext, "", true, false));
            }
            constraint.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        }
        else
        {
            throw new UnsupportedOperationException();
        }
        return constraint;
    }

    private Property visitSimpleProperty(DomainParserGrammar.PropertyContext ctx)
    {
        Property property = new Property();
        property.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        property.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        property.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        // NOTE: here we limit the property type to only primitive type, class, or enumeration
        property.type = ctx.propertyReturnType().type().getText();
        property.multiplicity = this.buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());
        property.defaultValue = ctx.defaultValue() == null ? null : this.visitDefaultValue(ctx.defaultValue().defaultValueExpression());
        property.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        property.propertyTypeSourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.propertyReturnType().type());
        return property;
    }

    // NOTE: we renamed qualified property to derived property
    private QualifiedProperty visitDerivedProperty(DomainParserGrammar.QualifiedPropertyContext ctx)
    {
        QualifiedProperty qualifiedProperty = new QualifiedProperty();
        qualifiedProperty.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        qualifiedProperty.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        qualifiedProperty.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        DomainParseTreeWalker.LambdaContext lambdaContext = new DomainParseTreeWalker.LambdaContext(qualifiedProperty.name);
        qualifiedProperty.body = this.codeBlock(ctx.qualifiedPropertyBody().codeBlock(), null, lambdaContext, false, " ");
        qualifiedProperty.parameters = ListIterate.collect(ctx.qualifiedPropertyBody().functionVariableExpression(), functionVariableExpressionContext ->
        {
            Variable variable = new Variable();
            variable.name = PureGrammarParserUtility.fromIdentifier(functionVariableExpressionContext.identifier());
            variable._class = functionVariableExpressionContext.type().getText();
            variable.multiplicity = this.buildMultiplicity(functionVariableExpressionContext.multiplicity().multiplicityArgument());
            variable.sourceInformation = walkerSourceInformation.getSourceInformation(functionVariableExpressionContext);
            return variable;
        });
        // NOTE: we should check but here we let returned type of the derived property to be whatever
        qualifiedProperty.returnType = ctx.propertyReturnType().type().getText();
        qualifiedProperty.returnMultiplicity = this.buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());
        qualifiedProperty.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return qualifiedProperty;
    }


    // ----------------------------------------------- ASSOCIATION -----------------------------------------------

    private Association visitAssociation(DomainParserGrammar.AssociationContext ctx)
    {
        Association assoc = new Association();
        assoc._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        assoc.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        assoc.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        assoc.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        assoc.properties = ctx.associationBody().properties().property() == null ? new ArrayList<>() : ListIterate.collect(ctx.associationBody().properties().property(), this::visitSimpleProperty);
        assoc.qualifiedProperties = ctx.associationBody().properties().qualifiedProperty() == null ? new ArrayList<>() : ListIterate.collect(ctx.associationBody().properties().qualifiedProperty(), this::visitDerivedProperty);
        assoc.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return assoc;
    }


    // ----------------------------------------------- FUNCTION -----------------------------------------------

    private org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function visitFunction(DomainParserGrammar.FunctionDefinitionContext ctx)
    {
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function func = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function();
        func.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        func._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        func.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        func.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());
        DomainParseTreeWalker.LambdaContext lambdaContext = new DomainParseTreeWalker.LambdaContext(func.name);
        func.body = this.codeBlock(ctx.codeBlock(), null, lambdaContext, false, " ");
        func.parameters = ListIterate.collect(ctx.functionTypeSignature().functionVariableExpression(), functionVariableExpressionContext ->
        {
            Variable variable = new Variable();
            variable.name = PureGrammarParserUtility.fromIdentifier(functionVariableExpressionContext.identifier());
            variable._class = functionVariableExpressionContext.type().getText();
            variable.multiplicity = this.buildMultiplicity(functionVariableExpressionContext.multiplicity().multiplicityArgument());
            variable.sourceInformation = this.walkerSourceInformation.getSourceInformation(functionVariableExpressionContext);
            return variable;
        });
        func.returnType = ctx.functionTypeSignature().type().getText();
        func.returnMultiplicity = this.buildMultiplicity(ctx.functionTypeSignature().multiplicity().multiplicityArgument());
        func.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return func;
    }


    // ----------------------------------------------- MEASURE -----------------------------------------------

    private Measure visitMeasure(DomainParserGrammar.MeasureDefinitionContext ctx)
    {
        Measure measure = new Measure();
        measure._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        measure.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        if (ctx.measureBody().canonicalExpr() != null)
        {
            // traditional canonical unit pattern
            measure.canonicalUnit = this.visitCanonicalUnit(ctx.measureBody().canonicalExpr(), measure);
            measure.nonCanonicalUnits = this.visitNonCanonicalUnit(ctx.measureBody().measureExpr(), measure);
        }
        else
        {
            // non-convertible unit pattern
            MutableList<Unit> nonConvertibleUnits = FastList.newList();
            for (DomainParserGrammar.NonConvertibleMeasureExprContext ncctx : ctx.measureBody().nonConvertibleMeasureExpr())
            {
                Unit currentUnit = this.visitNonConvertibleUnit(ncctx, measure);
                nonConvertibleUnits.add(currentUnit);
            }
            measure.canonicalUnit = nonConvertibleUnits.get(0);
            if (nonConvertibleUnits.size() > 1)
            {
                measure.nonCanonicalUnits = nonConvertibleUnits.subList(1, nonConvertibleUnits.size());
            }
        }

        measure.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return measure;
    }

    private Unit visitCanonicalUnit(DomainParserGrammar.CanonicalExprContext ctx, Measure measure)
    {
        return this.visitUnit(ctx.measureExpr(), measure);
    }

    private List<Unit> visitNonCanonicalUnit(List<DomainParserGrammar.MeasureExprContext> ctxList, Measure measure)
    {
        return ctxList == null ? Lists.mutable.empty() : ListIterate.collect(ctxList, ctx -> this.visitUnit(ctx, measure));
    }

    private Unit visitUnit(DomainParserGrammar.MeasureExprContext ctx, Measure measure)
    {
        Unit unit = new Unit();
        unit._package = measure._package == null || measure._package.equals("") ? "" : measure._package;
        unit.name = measure.name.concat(TILDE).concat(PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier()));
        unit.measure = measure.getPath();
        unit.superType = (measure._package == null || measure._package.equals("") ? "" : measure._package + "::") + measure.name;

        Lambda conversionFunction = new Lambda();
        Variable variable = new Variable();
        variable.name = PureGrammarParserUtility.fromIdentifier(ctx.unitExpr().identifier());
        conversionFunction.parameters = Collections.singletonList(variable);

        DomainParseTreeWalker.LambdaContext lambdaContext = new DomainParseTreeWalker.LambdaContext(unit.name.replace("::", "_"));
        conversionFunction.body = this.codeBlock(ctx.unitExpr().codeBlock(), new ArrayList<>(), lambdaContext, false, "");

        unit.conversionFunction = conversionFunction;
        unit.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        return unit;
    }

    private Unit visitNonConvertibleUnit(DomainParserGrammar.NonConvertibleMeasureExprContext ncctx, Measure measure)
    {
        Unit unit = new Unit();
        unit._package = measure._package == null || measure._package.equals("") ? "" : measure._package;
        unit.name = measure.name.concat(TILDE).concat(PureGrammarParserUtility.fromIdentifier(ncctx.qualifiedName().identifier()));
        unit.measure = measure.getPath();
        unit.superType = (measure._package == null || measure._package.equals("") ? "" : measure._package + "::") + measure.name;

        unit.sourceInformation = this.walkerSourceInformation.getSourceInformation(ncctx);
        return unit;
    }

    // ----------------------------------------------- LAMBDA -----------------------------------------------

    public ValueSpecification concreteFunctionDefinition(DomainParserGrammar.FunctionDefinitionContext ctx)
    {
        List<String> typeParametersNames = new ArrayList<>();
        String name = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        LambdaContext lambdaContext = new LambdaContext(name.replace("::", "_"));
        List<ValueSpecification> block = this.codeBlock(ctx.codeBlock(), typeParametersNames, lambdaContext, false, " ");
        if (block.size() == 1 && block.get(0) instanceof Lambda)
        {
            return block.get(0);
        }
        else
        {
            Lambda lambda = new Lambda();
            lambda.body = block;
            lambda.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.codeBlock());
            return lambda;
        }
    }

    private List<ValueSpecification> codeBlock(DomainParserGrammar.CodeBlockContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        List<ValueSpecification> lines = new ArrayList<>();
        for (DomainParserGrammar.ProgramLineContext plCtx : ctx.programLine())
        {
            lines.add(this.programLine(plCtx, typeParametersNames, lambdaContext, addLines, space + "  "));
        }
        return lines;
    }

    private ValueSpecification programLine(DomainParserGrammar.ProgramLineContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        if (ctx.combinedExpression() != null)
        {
            return this.combinedExpression(ctx.combinedExpression(), "line", typeParametersNames, lambdaContext, space, true, addLines);
        }
        else
        {
            return this.letExpression(ctx.letExpression(), typeParametersNames, lambdaContext, addLines, space);
        }
    }

    public ValueSpecification combinedExpression(DomainParserGrammar.CombinedExpressionContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        ValueSpecification boolResult = result;
        ValueSpecification arithResult = result;

        if (ctx.expressionPart() != null)
        {
            MutableList<DomainParserGrammar.ArithmeticPartContext> arth = FastList.newList();
            MutableList<DomainParserGrammar.BooleanPartContext> bool = FastList.newList();

            //Invariant: arth and bool cannot both contains elements at the same time: either we have processed arith, or bool and have moved onto the next grouping
            for (DomainParserGrammar.ExpressionPartContext epCtx : ctx.expressionPart())
            {
                if (epCtx.arithmeticPart() != null)
                {
                    if (!bool.isEmpty())
                    {
                        boolResult = this.booleanPart(bool, arithResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
                        bool.clear();
                    }
                    arth.add(epCtx.arithmeticPart());
                }
                else if (epCtx.booleanPart() != null)
                {
                    if (!arth.isEmpty())
                    {
                        arithResult = this.arithmeticPart(arth, boolResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
                        arth.clear();
                    }
                    bool.add(epCtx.booleanPart());
                }
            }
            if (!arth.isEmpty())
            {
                result = this.arithmeticPart(arth, result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (!bool.isEmpty())
            {
                result = this.booleanPart(bool, arithResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
        }
        return result;
    }

    private AppliedFunction letExpression(DomainParserGrammar.LetExpressionContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        ValueSpecification result = this.combinedExpression(ctx.combinedExpression(), "", typeParametersNames, lambdaContext, space, true, addLines);
        result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        CString cString = new CString();
        List<String> values = new ArrayList<>();
        values.add(PureGrammarParserUtility.fromIdentifier(ctx.identifier()));
        cString.multiplicity = this.getMultiplicityOneOne();
        cString.values = values;
        AppliedFunction appliedFunction = this.createAppliedFunction(Lists.mutable.of(cString, result), "letFunction");
        appliedFunction.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return appliedFunction;
    }

    private AppliedFunction newFunction(DomainParserGrammar.ExpressionInstanceContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr newClass = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr();
        newClass.fullPath = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        List<ValueSpecification> keyExpressions = processExpressionInstanceParserPropertyAssignments(ctx.expressionInstanceParserPropertyAssignment(), typeParametersNames, lambdaContext, addLines, space);
        Collection valueAssignments = new Collection();
        valueAssignments.values = keyExpressions;
        valueAssignments.multiplicity = getMultiplicityOneOne();
        return this.createAppliedFunction(Lists.mutable.with(newClass, variableForNew(), valueAssignments), "new");
    }

    // necessary for proper compilation of new function
    private ValueSpecification variableForNew()
    {
        CString string = new CString();
        string.multiplicity = getMultiplicityOneOne();
        return string;
    }

    private MutableList<ValueSpecification> processExpressionInstanceParserPropertyAssignments(List<DomainParserGrammar.ExpressionInstanceParserPropertyAssignmentContext> keyExpressions, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        MutableList<ValueSpecification> values = FastList.newList();
        for (DomainParserGrammar.ExpressionInstanceParserPropertyAssignmentContext val : keyExpressions)
        {
            KeyExpression v = new KeyExpression();
            v.key = processKey(val.identifier(0).getText());
            v.expression = processRightSide(val.expressionInstanceRightSide(), typeParametersNames, lambdaContext, addLines, space);
            values.add(v);
        }
        return values;
    }

    public ValueSpecification processRightSide(DomainParserGrammar.ExpressionInstanceRightSideContext rightside, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        ValueSpecification ctx = null;
        if (rightside.expressionInstanceAtomicRightSide().combinedExpression() != null)
        {
            ctx = expressionOrExpressionGroup(rightside.expressionInstanceAtomicRightSide().combinedExpression().expressionOrExpressionGroup(), null, typeParametersNames, lambdaContext, space, false, addLines);
        }
        return ctx;

    }

    private ValueSpecification processKey(String name)
    {
        CString key = new CString();
        key.values = FastList.newListWith(name);
        key.multiplicity = getMultiplicityOneOne();
        return key;
    }

    private ValueSpecification expressionOrExpressionGroup(DomainParserGrammar.ExpressionOrExpressionGroupContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        return this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
    }

    private ValueSpecification expression(DomainParserGrammar.ExpressionContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification result;
        List<ValueSpecification> expressions = Lists.mutable.of();
        List<ValueSpecification> parameters;
        if (ctx.combinedExpression() != null)
        {
            return this.combinedExpression(ctx.combinedExpression(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        if (ctx.atomicExpression() != null)
        {
            result = this.atomicExpression(ctx.atomicExpression(), typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        else if (ctx.notExpression() != null)
        {
            result = this.notExpression(ctx.notExpression(), exprName, typeParametersNames, lambdaContext, space, addLines);
        }
        else if (ctx.signedExpression() != null)
        {
            result = this.signedExpression(ctx.signedExpression(), exprName, typeParametersNames, lambdaContext, space, addLines);
        }
        else if (ctx.expressionsArray() != null)
        {
            for (DomainParserGrammar.ExpressionContext eCtx : ctx.expressionsArray().expression())
            {
                expressions.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, false, addLines));
            }
            result = this.collect(expressions, walkerSourceInformation.getSourceInformation(ctx));
        }
        else
        {
            throw new EngineException(ctx.getText() + " is not supported", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

        if (ctx.propertyOrFunctionExpression() != null)
        {
            for (DomainParserGrammar.PropertyOrFunctionExpressionContext pfCtx : ctx.propertyOrFunctionExpression())
            {
                if (pfCtx.propertyExpression() != null)
                {
                    result = propertyExpression(pfCtx.propertyExpression(), result, typeParametersNames, lambdaContext, space, addLines);
                }
                // TODO PropertyBracketExpression is deprecated.  Remove else if clause once all use has been addressed
                else if (pfCtx.propertyBracketExpression() != null)
                {
                    if (!allowPropertyBracketExpression)
                    {
                        throw new EngineException("Bracket operation is not supported", walkerSourceInformation.getSourceInformation(pfCtx.propertyBracketExpression()), EngineErrorType.PARSER);
                    }
                    String getPropertyName = "oneString";
                    parameters = new ArrayList<>();
                    AppliedProperty appliedProperty = new AppliedProperty();
                    appliedProperty.property = getPropertyName;
                    appliedProperty.parameters = Lists.mutable.of(result).withAll(parameters);
                    if (pfCtx.propertyBracketExpression().STRING() != null)
                    {
                        CString instance = getInstanceString(pfCtx.propertyBracketExpression().STRING().getText());
                        instance.sourceInformation = walkerSourceInformation.getSourceInformation(pfCtx.propertyBracketExpression());
                        appliedProperty.parameters.add(instance);
                    }
                    else
                    {
                        CInteger instance = getInstanceInteger(pfCtx.propertyBracketExpression().INTEGER().getText());
                        instance.sourceInformation = walkerSourceInformation.getSourceInformation(pfCtx.propertyBracketExpression());
                        appliedProperty.parameters.add(instance);
                    }
                    appliedProperty.sourceInformation = walkerSourceInformation.getSourceInformation(pfCtx.propertyBracketExpression());
                    result = appliedProperty;
                }
                else
                {
                    for (int i = 0; i < pfCtx.functionExpression().qualifiedName().size(); i++)
                    {
                        parameters = this.functionExpressionParameters(pfCtx.functionExpression().functionExpressionParameters(i), typeParametersNames, lambdaContext, addLines, space);
                        parameters.add(0, result);
                        result = this.functionExpression(pfCtx.functionExpression().qualifiedName(i), parameters);
                    }
                }
            }
        }

        if (ctx.equalNotEqual() != null)
        {
            result = this.equalNotEqual(ctx.equalNotEqual(), result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        return result;
    }

    private ValueSpecification propertyExpression(DomainParserGrammar.PropertyExpressionContext ctx, ValueSpecification result, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean addLines)
    {
        List<ValueSpecification> parameters = new ArrayList<>();
        DomainParserGrammar.IdentifierContext property = ctx.identifier();
        ValueSpecification parameter;
        if (ctx.functionExpressionParameters() != null)
        {
            DomainParserGrammar.FunctionExpressionParametersContext fepCtx = ctx.functionExpressionParameters();
            if (fepCtx.combinedExpression() != null)
            {
                for (DomainParserGrammar.CombinedExpressionContext ceCtx : fepCtx.combinedExpression())
                {
                    parameter = this.combinedExpression(ceCtx, "param", typeParametersNames, lambdaContext, space, true, addLines);
                    parameters.add(parameter);
                }
            }
        }
        else if (ctx.functionExpressionLatestMilestoningDateParameter() != null)
        {
            CLatestDate date = new CLatestDate();
            date.multiplicity = getMultiplicityOneOne();
            parameters.add(date);
        }
        AppliedProperty appliedProperty = new AppliedProperty();
        appliedProperty.property = PureGrammarParserUtility.fromIdentifier(property);
        appliedProperty.sourceInformation = walkerSourceInformation.getSourceInformation(property);
        appliedProperty.parameters = Lists.mutable.of(result).withAll(parameters);
        return appliedProperty;
    }

    private CString getInstanceString(String string)
    {
        List<String> values = new ArrayList<>();
        values.add(PureGrammarParserUtility.fromGrammarString(string, true));
        CString instance = new CString();
        instance.multiplicity = getMultiplicityOneOne();
        instance.values = values;
        return instance;
    }

    private CInteger getInstanceInteger(String integerString)
    {
        List<Long> values = new ArrayList<>();
        values.add(Long.parseLong(integerString));
        CInteger instance = new CInteger();
        instance.multiplicity = getMultiplicityOneOne();
        instance.values = values;
        return instance;
    }

    private boolean isLowerPrecedenceBoolean(String boolop1, String boolop2)
    {
        return boolop1.equals("or") && boolop2.equals("and");
    }

    private AppliedFunction buildBoolean(DomainParserGrammar.BooleanPartContext ctx, TerminalNode terminalNode, String boolop, ValueSpecification input, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification other = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        return createAppliedFunction(Lists.mutable.of(input, other), boolop, walkerSourceInformation.getSourceInformation(terminalNode.getSymbol()));
    }


    private AppliedFunction processBooleanOp(AppliedFunction appliedFunction, DomainParserGrammar.BooleanPartContext ctx, TerminalNode terminalNode, String boolop, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        if (appliedFunction == null)
        {
            appliedFunction = buildBoolean(ctx, terminalNode, boolop, initialValue, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        else if (appliedFunction != null && isLowerPrecedenceBoolean(appliedFunction.function, boolop))
        {
            List<ValueSpecification> params = appliedFunction.parameters;
            AppliedFunction newAf = buildBoolean(ctx, terminalNode, boolop, params.get(params.size() - 1), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            MutableList<ValueSpecification> l = Lists.mutable.withAll(params.subList(0, params.size() - 1));
            appliedFunction.parameters = Lists.mutable.of(l.get(0), newAf);
        }
        else
        {
            appliedFunction = buildBoolean(ctx, terminalNode, boolop, appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }

        return appliedFunction;
    }

    private AppliedFunction booleanPart(List<DomainParserGrammar.BooleanPartContext> booleanPartContexts, ValueSpecification input, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        AppliedFunction appliedFunction = null;
        for (DomainParserGrammar.BooleanPartContext ctx : booleanPartContexts)
        {
            if (ctx.AND() != null)
            {
                appliedFunction = processBooleanOp(appliedFunction, ctx, ctx.AND(), "and", input, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (ctx.OR() != null)
            {
                appliedFunction = processBooleanOp(appliedFunction, ctx, ctx.OR(), "or", input, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else
            {
                appliedFunction = this.equalNotEqual(ctx.equalNotEqual(), appliedFunction == null ? input : appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
        }

        return appliedFunction;
    }

    private AppliedFunction equalNotEqual(DomainParserGrammar.EqualNotEqualContext ctx, ValueSpecification input, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        AppliedFunction result = null;
        ValueSpecification other;
        if (ctx.TEST_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            result = this.createAppliedFunction(Lists.mutable.of(input, other), "equal");
            result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.TEST_EQUAL().getSymbol());
        }
        else if (ctx.TEST_NOT_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            AppliedFunction inner = this.createAppliedFunction(Lists.mutable.of(input, other), "equal");
            result = this.createAppliedFunction(Lists.mutable.of(inner), "not");
            result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.TEST_NOT_EQUAL().getSymbol());
        }
        return result;
    }

    private ValueSpecification combinedArithmeticOnly(DomainParserGrammar.CombinedArithmeticOnlyContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        if (Iterate.notEmpty(ctx.arithmeticPart()))
        {
            result = this.arithmeticPart(ctx.arithmeticPart(), result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }

        return result;
    }

    private ValueSpecification notExpression(DomainParserGrammar.NotExpressionContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean addLines)
    {
        ValueSpecification negated = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, addLines);
        ValueSpecification valueSpecification = this.createAppliedFunction(Lists.mutable.of(negated), "not");
        valueSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return valueSpecification;
    }

    private ValueSpecification signedExpression(DomainParserGrammar.SignedExpressionContext ctx, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean addLines)
    {
        AppliedFunction result;
        ValueSpecification number;
        if (ctx.MINUS() != null)
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, addLines);
            result = this.createAppliedFunction(Lists.mutable.of(number), "minus");
            result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.MINUS().getSymbol());
        }
        else
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, addLines);
            result = this.createAppliedFunction(Lists.mutable.of(number), "plus");
            result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.PLUS().getSymbol());
        }
        return result;
    }

    private AppliedFunction functionExpression(DomainParserGrammar.QualifiedNameContext funcName, List<ValueSpecification> parameters)
    {
        AppliedFunction result = new AppliedFunction();
        result.function = PureGrammarParserUtility.fromQualifiedName(funcName.packagePath() == null ? Collections.emptyList() : funcName.packagePath().identifier(), funcName.identifier());
        result.parameters = parameters;
        result.sourceInformation = walkerSourceInformation.getSourceInformation(funcName);
        return result;
    }

    private List<ValueSpecification> functionExpressionParameters(DomainParserGrammar.FunctionExpressionParametersContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, boolean addLines, String space)
    {
        List<ValueSpecification> parameters = new ArrayList<>();
        for (DomainParserGrammar.CombinedExpressionContext ceCtx : ctx.combinedExpression())
        {
            parameters.add(this.combinedExpression(ceCtx, "param", typeParametersNames, lambdaContext, space, true, addLines));
        }
        return parameters;
    }

    private ValueSpecification instanceLiteralToken(DomainParserGrammar.InstanceLiteralTokenContext ctx, boolean wrapFlag)
    {
        ValueSpecification result;
        try
        {
            Multiplicity m = this.getMultiplicityOneOne();
            if (ctx.STRING() != null)
            {
                CString instance = getInstanceString(ctx.getText());
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.INTEGER() != null)
            {
                CInteger instance = getInstanceInteger(ctx.getText());
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else if (ctx.FLOAT() != null)
            {
                List<Double> values = new ArrayList<>();
                values.add(Double.parseDouble(ctx.getText()));
                CFloat instance = new CFloat();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
//            else if (ctx.DECIMAL() != null)
//            {
//                List<Double> values = new ArrayList<>();
//                values.add(Double.parseDouble(ctx.getText()));
//                CFloat instance = new CFloat();
//                instance.multiplicity = this.getPureOne();
//                instance.values = values;
//                result = instance;
//            }
            else if (ctx.DATE() != null)
            {
                result = new DateParseTreeWalker(ctx.DATE(), this.walkerSourceInformation).visitDefinition();
            }
            else if (ctx.STRICTTIME() != null)
            {
                result = new StrictTimeParseTreeWalker(ctx.STRICTTIME(), this.walkerSourceInformation).visitStrictTimeDefinition();
            }
            else if (ctx.BOOLEAN() != null)
            {
                List<Boolean> values = new ArrayList<>();
                values.add(Boolean.parseBoolean(ctx.getText()));
                CBoolean instance = new CBoolean();
                instance.multiplicity = m;
                instance.values = values;
                instance.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
                result = instance;
            }
            else
            {
                throw new RuntimeException("TODO");
            }
        }
        catch (Exception e)
        {
            throw new EngineException(ctx.getText() + " is not supported", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        return result;
    }

    private ValueSpecification atomicExpression(DomainParserGrammar.AtomicExpressionContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification result;
        ListIterable<ValueSpecification> dsl;
        Variable expr;
        List<Variable> expressions = Lists.mutable.of();
        if (ctx.instanceLiteralToken() != null)
        {
            result = this.instanceLiteralToken(ctx.instanceLiteralToken(), wrapFlag);
        }
        else if (ctx.dsl() != null)
        {
            dsl = this.visitDsl(ctx.dsl());
            assert dsl != null;
            if (dsl.size() > 1)
            {
                throw new EngineException("Only expected one graph fetch tree", walkerSourceInformation.getSourceInformation(ctx.dsl()), EngineErrorType.PARSER);
            }
            result = dsl.getFirst();
        }
        else if (ctx.expressionInstance() != null)
        {
            result = this.newFunction(ctx.expressionInstance(), typeParametersNames, lambdaContext, addLines, space);
        }
        else if (ctx.variable() != null)
        {
            result = this.variable(ctx.variable());
        }
        else if (ctx.type() != null)
        {
            if (ctx.type().unitName() != null)
            {
                result = this.unitTypeReference(ctx.type());
            }
            else
            {
                result = this.typeReference(ctx.type());
            }
        }
        else if (ctx.lambdaFunction() != null)
        {
            boolean hasLambdaParams = false;
            if (ctx.lambdaFunction().lambdaParam() != null)
            {
                for (int i = 0; i < ctx.lambdaFunction().lambdaParam().size(); i++)
                {
                    hasLambdaParams = true;
                    DomainParserGrammar.IdentifierContext idCtx = ctx.lambdaFunction().lambdaParam(i).identifier();
                    expr = this.lambdaParam(ctx.lambdaFunction().lambdaParam(i).lambdaParamType(), idCtx, typeParametersNames, space);
                    expressions.add(expr);
                }
            }
            result = this.lambdaPipe(ctx.lambdaFunction().lambdaPipe(), hasLambdaParams ? ctx.lambdaFunction().lambdaParam(0).getStart() : null, expressions, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        else if (ctx.lambdaParam() != null && ctx.lambdaPipe() != null)
        {
            expr = this.lambdaParam(ctx.lambdaParam().lambdaParamType(), ctx.lambdaParam().identifier(), typeParametersNames, space);
            expressions.add(expr);
            result = this.lambdaPipe(ctx.lambdaPipe(), ctx.lambdaParam().getStart(), expressions, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        else if (ctx.instanceReference() != null)
        {
            result = this.instanceReference(ctx.instanceReference(), typeParametersNames, lambdaContext, space, addLines);
        }
        else
        {
            // lambdaPipe
            result = this.lambdaPipe(ctx.lambdaPipe(), null, expressions, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        return result;
    }

    private ListIterable<ValueSpecification> visitDsl(DomainParserGrammar.DslContext ctx)
    {
        if (ctx.dslGraphFetch() != null)
        {
            return this.visitGraphFetchTree(ctx.dslGraphFetch());
        }
        else if (ctx.dslNavigationPath() != null)
        {
            return this.visitNavigationPath(ctx.dslNavigationPath());
        }
        throw new EngineException("Unable to parse dsl text: " + ctx.getText(), walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
    }

    private ListIterable<ValueSpecification> visitNavigationPath(DomainParserGrammar.DslNavigationPathContext ctx)
    {
        TerminalNode terminalNode = ctx.NAVIGATION_PATH_BLOCK();
        StringBuilder graphFetchStringBuilder = new StringBuilder().append(ctx.getText());
        String graphFetchString = graphFetchStringBuilder.length() > 0 ? graphFetchStringBuilder.substring(1, graphFetchStringBuilder.length() - 1) : graphFetchStringBuilder.toString();
        int startLine = terminalNode.getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + terminalNode.getSymbol().getCharPositionInLine() + terminalNode.getText().length();
        ParseTreeWalkerSourceInformation graphFetchWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(this.walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ParserErrorListener errorListener = new ParserErrorListener(graphFetchWalkerSourceInformation);
        NavigationLexerGrammar navigationLexer = new NavigationLexerGrammar(CharStreams.fromString(graphFetchString));
        navigationLexer.removeErrorListeners();
        navigationLexer.addErrorListener(errorListener);
        NavigationParserGrammar navigationParser = new NavigationParserGrammar(new CommonTokenStream(navigationLexer));
        navigationParser.removeErrorListeners();
        navigationParser.addErrorListener(errorListener);
        navigationParser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        return Lists.mutable.with(new NavigationParseTreeWalker(graphFetchWalkerSourceInformation).visitDefinition(navigationParser.definition()));
    }

    // TODO: add another island mode in M3 for this when we support path (which starts with #/)
    private ListIterable<ValueSpecification> visitGraphFetchTree(DomainParserGrammar.DslGraphFetchContext ctx)
    {
        // NOTE: we want to preserve the spacing so we can correctly produce source information in the dispatched parser
        StringBuilder graphFetchStringBuilder = new StringBuilder();
        for (DomainParserGrammar.DslContentContext fragment : ctx.dslContent())
        {
            graphFetchStringBuilder.append(fragment.getText());
        }
        String graphFetchString = graphFetchStringBuilder.length() > 0 ? graphFetchStringBuilder.substring(0, graphFetchStringBuilder.length() - 2) : graphFetchStringBuilder.toString();
        if (graphFetchString.isEmpty())
        {
            throw new EngineException("Graph fetch tree must not be empty", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        // prepare island grammar walker source information
        int startLine = ctx.ISLAND_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.ISLAND_OPEN().getSymbol().getCharPositionInLine() + ctx.ISLAND_OPEN().getText().length();
        ParseTreeWalkerSourceInformation graphFetchWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(this.walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ParserErrorListener errorListener = new ParserErrorListener(graphFetchWalkerSourceInformation);
        GraphFetchTreeLexerGrammar graphLexer = new GraphFetchTreeLexerGrammar(CharStreams.fromString(graphFetchString));
        graphLexer.removeErrorListeners();
        graphLexer.addErrorListener(errorListener);
        GraphFetchTreeParserGrammar graphParser = new GraphFetchTreeParserGrammar(new CommonTokenStream(graphLexer));
        graphParser.removeErrorListeners();
        graphParser.addErrorListener(errorListener);
        graphParser.getInterpreter().setPredictionMode(PredictionMode.SLL);
        return Lists.mutable.with(new GraphFetchTreeParseTreeWalker(graphFetchWalkerSourceInformation).visitDefinition(graphParser.definition()));
    }

    private Variable lambdaParam(DomainParserGrammar.LambdaParamTypeContext ctx, DomainParserGrammar.IdentifierContext var, List<String> typeParametersNames, String space)
    {
        Variable variable = new Variable();
        if (ctx != null)
        {
            variable.multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
            variable._class = ctx.type().getText();
            variable.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.type());
        }
        variable.name = PureGrammarParserUtility.fromIdentifier(var);
        return variable;
    }

    private ValueSpecification lambdaPipe(DomainParserGrammar.LambdaPipeContext ctx, Token firstToken, List<Variable> params, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
//        List<ValueSpecification> block = this.codeBlock(ctx.codeBlock(), typeParametersNames, lambdaContext, addLines, space);
//        ValueSpecification result;
//
////        if( wrapFlag)
////            result = block.get(0);
////        else
////        {
//        Lambda signature = new Lambda();  //   Applied Function??
//        if (Iterate.notEmpty(params))
//        {
//            signature.parameters = (List<Variable>) params;
//        }
//
//        result = signature;
//        //  }
//        return result;

        Lambda signature = new Lambda();  // Applied Function??
        signature.body = this.codeBlock(ctx.codeBlock(), typeParametersNames, lambdaContext, addLines, space);
        signature.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (Iterate.notEmpty(params))
        {
            signature.parameters = params;
        }
        return signature;
    }

    /**
     * NOTE: in Pure, we create ImportStubInstance, basically, if we have something like:
     * |ok
     * we might have no idea what okay is, so we can assume it's a type, so in this case, we can
     * create a new type of value specification that represents `type` (we call ImportStub in Pure),
     * but here in Alloy, we try to simplify this a little bit (until we decide we need to create a new
     * value specfication type)
     * <p>
     * Since might have cases like 'Person.all()', and for those we expect a class anyway instead of an enumeration,
     * so it makes sense that we turn this token into an value specification of type `class`.
     * <p>
     * Otherwise, we will try to resolve the full path for the token (remember we are treating the token as a `type`)
     * So for this one it will depend on the path resolution algorithm. If the reosolution resolve in nothing meaningful
     * we will treat this as an enumeration just because if we have `|ok.ll` it makes no sense that we treat `ok.prop1`
     * as a call to property `prop1` on class `ok`
     */
    private ValueSpecification instanceReference(DomainParserGrammar.InstanceReferenceContext ctx, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean addLines)
    {
        ValueSpecification instance = null;
        if (ctx.qualifiedName() != null)
        {
            FastList<String> primitiveTypes = FastList.newListWith("Integer", "Boolean", "Date", "Binary", "DateTime", "Float", "StrictTime", "StrictDate", "Decimal", "LatestDate", "String", "Number");
            String fullPath = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
            // Resolve the element full path, if we know it has a (all) function call after it, we will enforce that it is a class, otherwise, it is either an enumeration or a class.
            if (ctx.allOrFunction() != null)
            {
                org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr _class = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr();
                _class.fullPath = fullPath;
                _class.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                instance = _class;
            }
            else if (primitiveTypes.contains(fullPath))  // is Primitive
            {
                org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType primitiveType = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PrimitiveType();
                primitiveType.name = fullPath;
                primitiveType.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                instance = primitiveType;

            }
            else //Unknown or Enum
            {
                org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr packageElementPtr = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr();
                packageElementPtr.fullPath = fullPath;
                packageElementPtr.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                instance = packageElementPtr;
            }

        }
        else if (ctx.unitName() != null)
        {
            String fullPath = ctx.unitName().qualifiedName().getText().concat(TILDE).concat(ctx.unitName().identifier().getText());
            org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType _unit = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.UnitType();
            _unit.unitType = fullPath;
            _unit.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.unitName());
            instance = _unit;
        }
        if (ctx.allOrFunction() != null)
        {
            if (instance == null)
            {
                // NOTE: due to some weird parsing rule in Pure M3, we are allowed to have `::.all()`
                throw new EngineException("Expected a non-empty function caller", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
            }
            instance = this.allOrFunction(ctx.allOrFunction(), Lists.mutable.of(instance), ctx.qualifiedName(), typeParametersNames, lambdaContext, space, addLines);
        }
        return instance;
    }

    private HackedClass typeReference(DomainParserGrammar.TypeContext ctx)
    {
        String fullPath = ctx.getText();
        org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass hackedClass = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedClass();
        hackedClass.fullPath = fullPath;
        hackedClass.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return hackedClass;
    }

    private HackedUnit unitTypeReference(DomainParserGrammar.TypeContext ctx)
    {
        String fullPath = ctx.unitName().qualifiedName().getText().concat(TILDE).concat(ctx.unitName().identifier().getText());
        org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit hackedUnit = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.HackedUnit();
        hackedUnit.unitType = fullPath;
        hackedUnit.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return hackedUnit;
    }


    private ValueSpecification allOrFunction(DomainParserGrammar.AllOrFunctionContext ctx, List<? extends ValueSpecification> params, DomainParserGrammar.QualifiedNameContext funcName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean addLines)
    {
        AppliedFunction appliedFunction = new AppliedFunction();
        appliedFunction.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // NOTE: no matter what instance turned out to be (either enumeration or class), we will take it as class here since only class allows function calling (e.g. `Person.all()`)
        List<ValueSpecification> parameters;
        org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr cl = new org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr();
        cl.fullPath = ((org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr) params.get(0)).fullPath;
        cl.sourceInformation = params.get(0).sourceInformation;
        appliedFunction.parameters = Lists.mutable.with(cl);

        if (ctx.allFunction() != null)
        {
            appliedFunction.function = "getAll";
            return appliedFunction;
        }
        else if (ctx.allVersionsFunction() != null)
        {
            appliedFunction.function = "getAllVersions";
            return appliedFunction;
        }
        else if (ctx.allVersionsInRangeFunction() != null)
        {
            throw new EngineException(ctx.allVersionsInRangeFunction().getText() + " is not supported", walkerSourceInformation.getSourceInformation(ctx.allVersionsInRangeFunction()), EngineErrorType.PARSER);
        }
        else if (ctx.allFunctionWithMilestoning() != null)
        {
            appliedFunction.function = "getAll";
            appliedFunction.parameters.addAll(ListIterate.collect(ctx.allFunctionWithMilestoning().buildMilestoningVariableExpression(), b -> {
                if(b.variable() != null)
                {
                    return variable(b.variable());
                }
                else if(b.DATE() != null)
                {
                    return new DateParseTreeWalker(b.DATE(), this.walkerSourceInformation).visitDefinition();
                }
                else
                {
                    CLatestDate latestDate = new CLatestDate();
                    latestDate.sourceInformation = walkerSourceInformation.getSourceInformation(b);
                    latestDate.multiplicity = getMultiplicityOneOne();
                    return latestDate;
                }
            }));
            return appliedFunction;
        }
        else
        {
            parameters = this.functionExpressionParameters(ctx.functionExpressionParameters(), typeParametersNames, lambdaContext, addLines, space);
            return this.functionExpression(funcName, parameters);
        }
    }

    private Variable variable(DomainParserGrammar.VariableContext ctx)
    {
        Variable result = new Variable();
        result.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        result.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // name ?
        // class ?
        // multiplicity ?
        return result;
    }

    private boolean isAdditiveOp(String op)
    {
        return op.equals("plus") || op.equals("minus");
    }

    private boolean isProductOp(String op)
    {
        return op.equals("times") || op.equals("star") || op.equals("divide");
    }

    private boolean isRelationalComparison(String operator)
    {
        return "lessThan".equals(operator) || "lessThanEqual".equals(operator) || "greaterThan".equals(operator) || "greaterThanEqual".equals(operator);
    }

    private boolean isStrictlyLowerPrecedence(String operator1, String operator2)
    {
        return (isRelationalComparison(operator1) && (isAdditiveOp(operator2) || isProductOp(operator2)))
                || (isAdditiveOp(operator1) && isProductOp(operator2));
    }

    private List<ValueSpecification> getParams(AppliedFunction appliedFunction)
    {
        return appliedFunction.parameters;
    }

    private interface ArithmeticExpressionBuilder
    {
        AppliedFunction build(DomainParserGrammar.ArithmeticPartContext ctx, String op, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines);
    }

    AppliedFunction buildDivide(DomainParserGrammar.ArithmeticPartContext ctx, String op, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        List<ValueSpecification> others = new ArrayList<>();
        for (DomainParserGrammar.ExpressionContext eCtx : ctx.expression())
        {
            others.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines));
        }

        AppliedFunction appliedFunction = null;
        for (ValueSpecification other : others)
        {
            appliedFunction = createAppliedFunction(Lists.mutable.of(initialValue, other), "divide", walkerSourceInformation.getSourceInformation(ctx));
            initialValue = appliedFunction;
        }

        return appliedFunction;
    }

    AppliedFunction buildArithmeticOp(DomainParserGrammar.ArithmeticPartContext ctx, String op, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        List<ValueSpecification> others = new ArrayList<>();
        for (DomainParserGrammar.ExpressionContext eCtx : ctx.expression())
        {
            others.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines));
        }

        return createAppliedFunction(Lists.mutable.of(this.collect(Lists.mutable.with(initialValue).withAll(others), walkerSourceInformation.getSourceInformation(ctx))), op, walkerSourceInformation.getSourceInformation(ctx));
    }

    AppliedFunction buildComparisonOp(DomainParserGrammar.ArithmeticPartContext ctx, String op, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        ValueSpecification other = this.expression(ctx.expression(0), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        return createAppliedFunction(Lists.mutable.of(initialValue, other), op, walkerSourceInformation.getSourceInformation(ctx));
    }

    private AppliedFunction processOp(ArithmeticExpressionBuilder builder, AppliedFunction appliedFunction, DomainParserGrammar.ArithmeticPartContext ctx, String op, ValueSpecification initialValue, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        // Case where we are building from scratch
        if (appliedFunction == null)
        {
            appliedFunction = builder.build(ctx, op, initialValue, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }
        //Case where we are in the middle of an expression, and currently looking at something of higher precedence than previous expression
        //Some processing to replace the last argument of the previous expression with the current expression (where current expression
        //has the last param as it's initial parameter).
        else if (appliedFunction != null && isStrictlyLowerPrecedence(appliedFunction.function, op))
        {
            List<ValueSpecification> params = getParams(appliedFunction);

            // divide/relational functions have args as a pair
            // other arithmetic ops store as a wrapped collection (like scheme (+ 1 2 3 4))
            if (appliedFunction.function.equals("divide") || isRelationalComparison(appliedFunction.function))
            {
                MutableList<ValueSpecification> l = Lists.mutable.withAll(params.subList(0, params.size() - 1));
                AppliedFunction newAf = builder.build(ctx, op, params.get(params.size() - 1), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
                appliedFunction.parameters = Lists.mutable.of(l.get(0), newAf);
            }
            else
            {
                List<ValueSpecification> currentCollection = ((Collection) params.get(0)).values;
                MutableList<ValueSpecification> l = Lists.mutable.withAll(currentCollection.subList(0, currentCollection.size() - 1));
                AppliedFunction newAf = builder.build(ctx, op, currentCollection.get(currentCollection.size() - 1), exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
                appliedFunction.parameters = Lists.mutable.of(this.collect(l.with(newAf), walkerSourceInformation.getSourceInformation(ctx)));
            }
        }
        // Case where are in the middle of an expression, but currently looking at something of lower or equal precedence
        // Add the previously processed expression as the initial argument to this expression
        else
        {
            appliedFunction = builder.build(ctx, op, appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
        }

        return appliedFunction;
    }

    private AppliedFunction arithmeticPart(List<DomainParserGrammar.ArithmeticPartContext> aList, ValueSpecification result, String exprName, List<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, boolean addLines)
    {
        AppliedFunction appliedFunction = null;
        for (DomainParserGrammar.ArithmeticPartContext ctx : aList)
        {
            if (Iterate.notEmpty(ctx.PLUS()))
            {
                appliedFunction = processOp(this::buildArithmeticOp, appliedFunction, ctx, "plus", result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (Iterate.notEmpty(ctx.STAR()))
            {
                appliedFunction = processOp(this::buildArithmeticOp, appliedFunction, ctx, "times", result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (Iterate.notEmpty(ctx.MINUS()))
            {
                appliedFunction = processOp(this::buildArithmeticOp, appliedFunction, ctx, "minus", result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (Iterate.notEmpty(ctx.DIVIDE()))
            {
                appliedFunction = processOp(this::buildDivide, appliedFunction, ctx, "divide", result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (ctx.LESS_THAN() != null)
            {
                appliedFunction = buildComparisonOp(ctx, "lessThan", appliedFunction == null ? result : appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (ctx.LESS_OR_EQUAL() != null)
            {
                appliedFunction = buildComparisonOp(ctx, "lessThanEqual", appliedFunction == null ? result : appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (ctx.GREATER_THAN() != null)
            {
                appliedFunction = buildComparisonOp(ctx, "greaterThan", appliedFunction == null ? result : appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
            else if (ctx.GREATER_OR_EQUAL() != null)
            {
                appliedFunction = buildComparisonOp(ctx, "greaterThanEqual", appliedFunction == null ? result : appliedFunction, exprName, typeParametersNames, lambdaContext, space, wrapFlag, addLines);
            }
        }

        return appliedFunction;
    }


    public static final class LambdaContext
    {
        private int lambdaFunctionCounter = 0;
        private final String lambdaFunctionOwnerId;

        public LambdaContext(String lambdaFunctionOwnerId)
        {
            this.lambdaFunctionOwnerId = lambdaFunctionOwnerId;
        }

        public String getLambdaFunctionUniqueName()
        {
            String name = this.lambdaFunctionOwnerId + '$' + this.lambdaFunctionCounter;
            this.lambdaFunctionCounter++;
            return name;
        }
    }

    private Multiplicity buildMultiplicity(DomainParserGrammar.MultiplicityArgumentContext ctx)
    {
        Multiplicity m = new Multiplicity();
        String star = "*";
        if (ctx.fromMultiplicity() == null)
        {
            m.lowerBound = Integer.parseInt(star.equals(ctx.toMultiplicity().getText()) ? "0" : ctx.toMultiplicity().getText());
        }
        else
        {
            m.lowerBound = Integer.parseInt(ctx.fromMultiplicity().getText());
        }

        if (!star.equals(ctx.toMultiplicity().getText()))
        {
            m.setUpperBound(Integer.parseInt(ctx.toMultiplicity().getText()));
        }
        else
        {
            m.setUpperBound(null);
        }
        return m;
    }

    private Collection collect(List<ValueSpecification> values, SourceInformation si)
    {
        Collection c = new Collection();
        c.multiplicity = this.createMultiplicity(values.size(), values.size());
        c.values = values;
        c.sourceInformation = si;
        return c;
    }

    private AppliedFunction createAppliedFunction(List<ValueSpecification> parameters, String functionName)
    {
        AppliedFunction appliedFunction = new AppliedFunction();
        appliedFunction.parameters = parameters;
        appliedFunction.function = functionName;
        return (appliedFunction);
    }

    private AppliedFunction createAppliedFunction(List<ValueSpecification> parameters, String functionName, SourceInformation si)
    {
        AppliedFunction appliedFunction = createAppliedFunction(parameters, functionName);
        appliedFunction.sourceInformation = si;
        return appliedFunction;
    }

    private Multiplicity getMultiplicityOneOne()
    {
        Multiplicity m = new Multiplicity();
        m.lowerBound = 1;
        m.setUpperBound(1);
        return m;
    }

    private Multiplicity createMultiplicity(int lowerBound, int upperBound)
    {
        Multiplicity m = new Multiplicity();
        m.lowerBound = lowerBound;
        m.setUpperBound(upperBound);
        return m;
    }
}
