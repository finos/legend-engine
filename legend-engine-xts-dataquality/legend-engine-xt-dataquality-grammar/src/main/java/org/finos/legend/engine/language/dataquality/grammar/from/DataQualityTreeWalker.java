//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.dataquality.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataQualityParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQuality;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityPropertyGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRootGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataProductDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataqualityRelationValidation;
import org.finos.legend.engine.protocol.dataquality.metamodel.MappingAndRuntimeDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.RelationValidation;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.SubTypeGraphFetchTree;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataQualityTreeWalker
{
    private static final String RELATION_ROW_LEVEL_VAL_TYPE = "ROW_LEVEL";
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;
    private final PureGrammarParserContext parserContext;

    public DataQualityTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section, PureGrammarParserContext parserContext)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.parserContext = parserContext;
    }

    public void visit(DataQualityParserGrammar.DefinitionContext definitionContext)
    {
        if (definitionContext.validationDefinition() != null && !definitionContext.validationDefinition().isEmpty())
        {
            definitionContext.validationDefinition().stream().map(this::visitDataQualityValidation).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private PackageableElement visitDataQualityValidation(DataQualityParserGrammar.ValidationDefinitionContext validationDefinitionContext)
    {
        if (validationDefinitionContext.classValidationDefinition() != null)
        {
            return visitClassValidation(validationDefinitionContext.classValidationDefinition());
        }
        if (validationDefinitionContext.relationValidationDefinition() != null)
        {
            return visitRelationValidation(validationDefinitionContext.relationValidationDefinition());
        }
        throw new EngineException("Unsupported syntax", this.walkerSourceInformation.getSourceInformation(validationDefinitionContext), EngineErrorType.PARSER);
    }

    private DataQuality visitClassValidation(DataQualityParserGrammar.ClassValidationDefinitionContext validationDefinitionContext)
    {
        DataQuality dataQuality = new DataQuality();
        dataQuality.name = PureGrammarParserUtility.fromIdentifier(validationDefinitionContext.qualifiedName().identifier());
        dataQuality._package = validationDefinitionContext.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(validationDefinitionContext.qualifiedName().packagePath().identifier());
        dataQuality.sourceInformation = walkerSourceInformation.getSourceInformation(validationDefinitionContext);
        dataQuality.stereotypes = validationDefinitionContext.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(validationDefinitionContext.stereotypes());
        dataQuality.taggedValues = validationDefinitionContext.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(validationDefinitionContext.taggedValues());
        dataQuality.sourceInformation = walkerSourceInformation.getSourceInformation(validationDefinitionContext);

        // context
        DataQualityParserGrammar.DqContextContext dqContextContext = PureGrammarParserUtility.validateAndExtractRequiredField(validationDefinitionContext.dqContext(),
                "context",
                dataQuality.sourceInformation);
        dataQuality.context = visitDqContext(dqContextContext, dataQuality.sourceInformation);
        // constraints
        DataQualityParserGrammar.ValidationTreeContext validationTreeContext = PureGrammarParserUtility.validateAndExtractRequiredField(validationDefinitionContext.validationTree(),
                "validationTree",
                dataQuality.sourceInformation);
        dataQuality.dataQualityRootGraphFetchTree = this.visitRootGraphDefinition(validationTreeContext.dqGraphDefinition().graphDefinition(),
                validationTreeContext.dqGraphDefinition());
        if (Objects.nonNull(validationDefinitionContext.filter()) && !validationDefinitionContext.filter().isEmpty())
        {
            dataQuality.filter = visitLambda(validationDefinitionContext.filter().get(0).combinedExpression());
        }

        return dataQuality;
    }

    private DataQualityRootGraphFetchTree visitRootGraphDefinition(DataQualityParserGrammar.GraphDefinitionContext graphDefinitionContext,
                                                   DataQualityParserGrammar.DqGraphDefinitionContext validationDefinitionContext)
    {
        List<GraphFetchTree> subTrees = new ArrayList<>();
        List<SubTypeGraphFetchTree> subTypeTrees = new ArrayList<>();
        if (Objects.nonNull(graphDefinitionContext.graphPaths()))
        {
            for (DataQualityParserGrammar.GraphPathContext graphPathContext : graphDefinitionContext.graphPaths().graphPath())
            {
                subTrees.add(this.visitGraphPathContext(graphPathContext));
            }
        }

//        for (GraphFetchTreeParserGrammar.SubTypeGraphPathContext subTypeGraphPathContext : graphDefinitionContext.graphPaths().subTypeGraphPath())
//        {
//            subTypeTrees.add(this.visitSubTypeGraphPathContext(subTypeGraphPathContext));
//        }
        DataQualityRootGraphFetchTree result = new DataQualityRootGraphFetchTree();
        result._class = PureGrammarParserUtility.fromQualifiedName(validationDefinitionContext.qualifiedName().packagePath() == null ? Collections.emptyList() : validationDefinitionContext.qualifiedName().packagePath().identifier(), validationDefinitionContext.qualifiedName().identifier());
        result.sourceInformation = walkerSourceInformation.getSourceInformation(validationDefinitionContext.qualifiedName());
        result.subTrees = subTrees;
        result.subTypeTrees = subTypeTrees;

        if (Objects.nonNull(validationDefinitionContext.constraintList()))
        {
            result.constraints = new ArrayList<>();
            for (DataQualityParserGrammar.DqConstraintNameContext dqConstraintNameContext : validationDefinitionContext.constraintList().dqConstraintName())
            {
                result.constraints.add(visitConstraintName(dqConstraintNameContext));
            }
        }
        return result;
    }

    private String visitConstraintName(DataQualityParserGrammar.DqConstraintNameContext dqConstraintNameContext)
    {
        return PureGrammarParserUtility.fromIdentifier(dqConstraintNameContext.identifier());
    }

    private PropertyGraphFetchTree visitGraphPathContext(DataQualityParserGrammar.GraphPathContext graphPathContext)
    {
        List<GraphFetchTree> subTrees = new ArrayList<>();
        if (graphPathContext.graphDefinition() != null && graphPathContext.graphDefinition().graphPaths() != null)
        {
            // validationForSubTypeTrees(graphPathContext.graphDefinition());
            for (DataQualityParserGrammar.GraphPathContext subGraphPathContext : graphPathContext.graphDefinition().graphPaths().graphPath())
            {
                subTrees.add(this.visitGraphPathContext(subGraphPathContext));
            }
        }

        List<ValueSpecification> parameters = new ArrayList<>();
//        if (graphPathContext.propertyParameters() != null)
//        {
//            for (GraphFetchTreeParserGrammar.ParameterContext parameterContext : graphPathContext.propertyParameters().parameter())
//            {
//                parameters.add(this.visitParameterContext(parameterContext));
//            }
//        }

        DataQualityPropertyGraphFetchTree result = new DataQualityPropertyGraphFetchTree();
        result.property = PureGrammarParserUtility.fromIdentifier(graphPathContext.identifier());
        if (Objects.nonNull(graphPathContext.constraintList()))
        {
            result.constraints = new ArrayList<>();
            for (DataQualityParserGrammar.DqConstraintNameContext dqConstraintNameContext : graphPathContext.constraintList().dqConstraintName())
            {
                result.constraints.add(dqConstraintNameContext.getText());
            }
        }
        result.sourceInformation = walkerSourceInformation.getSourceInformation(graphPathContext.identifier());
        result.parameters = parameters;
        result.subTrees = subTrees;
        if (graphPathContext.alias() != null)
        {
            String withQuote = graphPathContext.alias().STRING().getText();
            result.alias = withQuote.substring(1, withQuote.length() - 1);
        }
        if (graphPathContext.subtype() != null)
        {
            result.subType = graphPathContext.subtype().qualifiedName().getText();
        }
        return result;
    }


    private DataQualityExecutionContext visitDqContext(DataQualityParserGrammar.DqContextContext dqContextContext, SourceInformation sourceInformation)
    {
        if (Objects.nonNull(dqContextContext.fromMappingAndRuntime()))
        {
            PackageableElementPointer mappingPointer = new PackageableElementPointer();
            mappingPointer.type = PackageableElementType.MAPPING;
            mappingPointer.path = visitQualifiedName(dqContextContext.fromMappingAndRuntime().mapping().qualifiedName());
            mappingPointer.sourceInformation = walkerSourceInformation.getSourceInformation(dqContextContext.fromMappingAndRuntime().mapping());

            PackageableElementPointer runtimePointer = new PackageableElementPointer();
            runtimePointer.type = PackageableElementType.RUNTIME;
            runtimePointer.path = visitQualifiedName(dqContextContext.fromMappingAndRuntime().runtime().qualifiedName());
            runtimePointer.sourceInformation = walkerSourceInformation.getSourceInformation(dqContextContext.fromMappingAndRuntime().runtime());

            final MappingAndRuntimeDataQualityExecutionContext mappingAndRuntimeDataQualityExecutionContext = new MappingAndRuntimeDataQualityExecutionContext();
            mappingAndRuntimeDataQualityExecutionContext.mapping = mappingPointer;
            mappingAndRuntimeDataQualityExecutionContext.runtime = runtimePointer;
            return mappingAndRuntimeDataQualityExecutionContext;
        }
        PackageableElementPointer dataSpaceElementPointer = new PackageableElementPointer();
        dataSpaceElementPointer.type = PackageableElementType.DATASPACE;
        dataSpaceElementPointer.path = visitQualifiedName(dqContextContext.fromDataProduct().dataspace().qualifiedName());
        dataSpaceElementPointer.sourceInformation = walkerSourceInformation.getSourceInformation(dqContextContext.fromDataProduct().dataspace());

        final DataProductDataQualityExecutionContext dataSpaceDataQualityExecutionContext = new DataProductDataQualityExecutionContext();
        dataSpaceDataQualityExecutionContext.dataSpace = dataSpaceElementPointer;
        dataSpaceDataQualityExecutionContext.context = PureGrammarParserUtility.fromGrammarString(dqContextContext.fromDataProduct().contextName().STRING().getText(), true);
        return dataSpaceDataQualityExecutionContext;
    }

    private String visitQualifiedName(DataQualityParserGrammar.QualifiedNameContext qualifiedNameContext)
    {
        return PureGrammarParserUtility.fromQualifiedName(qualifiedNameContext.packagePath() == null ? Collections.emptyList() :
                qualifiedNameContext.packagePath().identifier(), qualifiedNameContext.identifier());
    }

    private List<TaggedValue> visitTaggedValues(DataQualityParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(DataQualityParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.identifier());
            return stereotypePtr;
        });
    }

    private LambdaFunction visitLambda(DataQualityParserGrammar.CombinedExpressionContext combinedExpressionContext)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = combinedExpressionContext.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + combinedExpressionContext.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        String lambdaString = this.input.getText(new Interval(combinedExpressionContext.start.getStartIndex(), combinedExpressionContext.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionSourceInformation, this.parserContext);
        if (valueSpecification instanceof LambdaFunction)
        {
            return (LambdaFunction) valueSpecification;
        }
        // NOTE: If the user just provides the body of the lambda, we will wrap a lambda around it
        // we might want to reconsider this behavior and throw error if this convenience causes any trouble
        LambdaFunction lambda = new LambdaFunction();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = new ArrayList<>();
        return lambda;
    }

    private DataqualityRelationValidation visitRelationValidation(DataQualityParserGrammar.RelationValidationDefinitionContext relationValidationDefinitionContext)
    {
        DataqualityRelationValidation dataqualityRelationValidation = new DataqualityRelationValidation();
        dataqualityRelationValidation.name = PureGrammarParserUtility.fromIdentifier(relationValidationDefinitionContext.qualifiedName().identifier());
        dataqualityRelationValidation._package = relationValidationDefinitionContext.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(relationValidationDefinitionContext.qualifiedName().packagePath().identifier());
        dataqualityRelationValidation.sourceInformation = walkerSourceInformation.getSourceInformation(relationValidationDefinitionContext);
        dataqualityRelationValidation.stereotypes = relationValidationDefinitionContext.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(relationValidationDefinitionContext.stereotypes());
        dataqualityRelationValidation.taggedValues = relationValidationDefinitionContext.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(relationValidationDefinitionContext.taggedValues());
        dataqualityRelationValidation.sourceInformation = walkerSourceInformation.getSourceInformation(relationValidationDefinitionContext);

        // query
        DataQualityParserGrammar.RelationFuncContext relationFuncContext = PureGrammarParserUtility.validateAndExtractRequiredField(relationValidationDefinitionContext.relationFunc(),
                "query",
                dataqualityRelationValidation.sourceInformation);
        dataqualityRelationValidation.query = visitLambda(relationFuncContext.combinedExpression());

        // relation validations
        DataQualityParserGrammar.ValidationsContext validationsContext = PureGrammarParserUtility.validateAndExtractRequiredField(relationValidationDefinitionContext.validations(),
                "validations",
                dataqualityRelationValidation.sourceInformation);
        dataqualityRelationValidation.validations = visitValidations(validationsContext, dataqualityRelationValidation.sourceInformation);

        // runtime
        DataQualityParserGrammar.RelationRuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractOptionalField(relationValidationDefinitionContext.relationRuntime(),
                "runtime",
                dataqualityRelationValidation.sourceInformation);
        if (Objects.nonNull(runtimeContext))
        {
            dataqualityRelationValidation.runtime = visitRuntime(runtimeContext);
        }
        return dataqualityRelationValidation;
    }

    private List<RelationValidation> visitValidations(DataQualityParserGrammar.ValidationsContext validationContexts, SourceInformation sourceInformation)
    {
        if (Objects.isNull(validationContexts.validation()))
        {
            return Collections.emptyList();
        }
        return validationContexts.validation().stream()
                .map(validationContext -> this.visitValidation(validationContext, sourceInformation))
                .collect(Collectors.toList());
    }

    private RelationValidation visitValidation(DataQualityParserGrammar.ValidationContext validationContext, SourceInformation sourceInformation)
    {
        RelationValidation relationValidation = new RelationValidation();
        DataQualityParserGrammar.ValidationNameContext validationNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(validationContext.validationName(),
                "name",
                sourceInformation);
        relationValidation.name = PureGrammarParserUtility.fromGrammarString(validationNameContext.STRING().getText(), true);

        DataQualityParserGrammar.ValidationDescContext validationDescContext = PureGrammarParserUtility.validateAndExtractOptionalField(validationContext.validationDesc(),
                "description",
                sourceInformation);
        if (Objects.nonNull(validationDescContext))
        {
            relationValidation.description = PureGrammarParserUtility.fromGrammarString(validationDescContext.STRING().getText(), true);
        }

        DataQualityParserGrammar.ValidationAssertionContext validationAssertionContext = PureGrammarParserUtility.validateAndExtractRequiredField(validationContext.validationAssertion(),
                "assertion",
                sourceInformation);
        relationValidation.assertion = this.visitLambda(validationAssertionContext.combinedExpression());

        DataQualityParserGrammar.ValidationTypeContext validationTypeContext = PureGrammarParserUtility.validateAndExtractOptionalField(validationContext.validationType(),
                "type",
                sourceInformation);
        relationValidation.type = this.visitValidationType(validationTypeContext);

        return relationValidation;
    }

    private String visitValidationType(DataQualityParserGrammar.ValidationTypeContext validationTypeContext)
    {
        if (Objects.isNull(validationTypeContext))
        {
            return RELATION_ROW_LEVEL_VAL_TYPE;
        }
        if (Objects.nonNull(validationTypeContext.validationTypeVal().VALIDATION_TYPE_ROW()))
        {
            return validationTypeContext.validationTypeVal().VALIDATION_TYPE_ROW().getText();
        }
        return validationTypeContext.validationTypeVal().VALIDATION_TYPE_AGG().getText();
    }

    private PackageableElementPointer visitRuntime(DataQualityParserGrammar.RelationRuntimeContext runtimeContext)
    {
        PackageableElementPointer runtimePointer = new PackageableElementPointer();
        runtimePointer.type = PackageableElementType.RUNTIME;
        runtimePointer.path = visitQualifiedName(runtimeContext.runtime().qualifiedName());
        runtimePointer.sourceInformation = walkerSourceInformation.getSourceInformation(runtimeContext.runtime());
        return runtimePointer;
    }
}
