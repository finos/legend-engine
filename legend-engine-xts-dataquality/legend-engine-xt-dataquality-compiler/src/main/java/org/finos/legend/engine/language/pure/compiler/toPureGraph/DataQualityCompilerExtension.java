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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQuality;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityPropertyGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRootGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataSpaceDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataqualityRelationValidation;
import org.finos.legend.engine.protocol.dataquality.metamodel.MappingAndRuntimeDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.RelationValidation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.SubTypeGraphFetchTree;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityExecutionContext;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityPropertyGraphFetchTree;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityPropertyGraphFetchTree_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidation_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRootGraphFetchTree;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRootGraphFetchTree_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQuality_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataSpaceDataQualityExecutionContext;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataSpaceDataQualityExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_MappingAndRuntimeDataQualityExecutionContext;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_MappingAndRuntimeDataQualityExecutionContext_Impl;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_RelationValidation;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_RelationValidation_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_constraint_Constraint_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataQualityCompilerExtension implements CompilerExtension
{

    private static final String RELATION_ROW_LEVEL_VAL_TYPE = "ROW_LEVEL";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataQualityValidation");
    }

    @Override
    public CompilerExtension build()
    {
        return new DataQualityCompilerExtension();
    }


    private GenericType genericType(CompileContext context, String _class)
    {
        GenericType gt = context.resolveGenericType("meta::external::dataquality::DataQuality");

        return gt._typeArguments(org.eclipse.collections.impl.factory.Lists.fixedSize.of(
                context.resolveGenericType(_class)
        ));
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                getDataQualityModelContraintProcessor(),
                getDataQualityRelationValidationProcessor()
        );
    }

    private Processor<DataQuality> getDataQualityModelContraintProcessor()
    {
        return Processor.newProcessor(
                DataQuality.class,
                org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableRuntime.class, Mapping.class, org.finos.legend.engine.protocol.pure.m3.type.Class.class, DataSpace.class),
                (dataquality, compileContext) ->
                {
                    Root_meta_external_dataquality_DataQuality<Object> metamodel = new Root_meta_external_dataquality_DataQuality_Impl<>(
                            dataquality.name,
                            SourceInformationHelper.toM3SourceInformation(dataquality.sourceInformation),
                            compileContext.pureModel.getClass("meta::external::dataquality::DataQuality")
                    )._classifierGenericType(genericType(compileContext, dataquality.dataQualityRootGraphFetchTree._class));
                    return metamodel;
                },
                (dataquality, compileContext) ->
                {

                },
                (dataquality, compileContext) ->
                {
                    Root_meta_external_dataquality_DataQuality<Object> metamodel = (Root_meta_external_dataquality_DataQuality<Object>) compileContext.pureModel.getPackageableElement(compileContext.pureModel.buildPackageString(dataquality._package, dataquality.name));
                    metamodel._context(buildDataQualityExecutionContext(dataquality, compileContext))
                            ._filter(getFilterLambda(dataquality, compileContext))
                            ._validationTree(buildRootGraphFetchTree(dataquality.dataQualityRootGraphFetchTree, compileContext, compileContext.pureModel.getClass(dataquality.dataQualityRootGraphFetchTree._class), null, new ProcessingContext("DataQuality")));
                    metamodel._validate(true, SourceInformationHelper.toM3SourceInformation(dataquality.sourceInformation), compileContext.getExecutionSupport());
                }
        );
    }

    private Processor<DataqualityRelationValidation> getDataQualityRelationValidationProcessor()
    {
        return Processor.newProcessor(
                DataqualityRelationValidation.class,
                org.eclipse.collections.impl.factory.Lists.fixedSize.with(PackageableRuntime.class, Mapping.class, org.finos.legend.engine.protocol.pure.m3.type.Class.class, DataSpace.class),
                (dataqualityRelationValidation, compileContext) ->
                {
                    Root_meta_external_dataquality_DataQualityRelationValidation_Impl metamodel = new Root_meta_external_dataquality_DataQualityRelationValidation_Impl(
                            dataqualityRelationValidation.name,
                            SourceInformationHelper.toM3SourceInformation(dataqualityRelationValidation.sourceInformation),
                            compileContext.pureModel.getClass("meta::external::dataquality::DataQualityRelationValidation")
                    );
                    return metamodel;
                },
                (dataqualityRelationValidation, compileContext) ->
                {

                },
                (dataqualityRelationValidation, compileContext) ->
                {
                    Root_meta_external_dataquality_DataQualityRelationValidation_Impl metamodel = (Root_meta_external_dataquality_DataQualityRelationValidation_Impl) compileContext.pureModel.getPackageableElement(compileContext.pureModel.buildPackageString(dataqualityRelationValidation._package, dataqualityRelationValidation.name));
                    LambdaFunction<?> relationValidationQuery = buildDataqualityRelationValidationQuery(dataqualityRelationValidation, compileContext);
                    metamodel._query(relationValidationQuery);
                    metamodel._validations(buildDataQualityRelationValidations(dataqualityRelationValidation.validations, relationValidationQuery, SourceInformationHelper.toM3SourceInformation(dataqualityRelationValidation.sourceInformation), compileContext));
                    metamodel._runtime(buildRelationValidationRuntime(dataqualityRelationValidation.runtime, relationValidationQuery, SourceInformationHelper.toM3SourceInformation(dataqualityRelationValidation.sourceInformation), compileContext));
                    metamodel._validate(true, SourceInformationHelper.toM3SourceInformation(dataqualityRelationValidation.sourceInformation), compileContext.getExecutionSupport());
                }
        );
    }

    private LambdaFunction<?> buildDataqualityRelationValidationQuery(DataqualityRelationValidation dataqualityRelationValidation, CompileContext compileContext)
    {
        LambdaFunction<?> relationQuery = HelperValueSpecificationBuilder.buildLambda(dataqualityRelationValidation.query, compileContext);
        String relationQueryReturnType = Compiler.getLambdaReturnType(dataqualityRelationValidation.query, compileContext.pureModel);
        Assert.assertTrue("meta::pure::metamodel::relation::Relation".equals(relationQueryReturnType), () -> "Relation expected from lambda");
        return relationQuery;
    }

    private RichIterable<Root_meta_external_dataquality_RelationValidation> buildDataQualityRelationValidations(List<RelationValidation> relationValidations, LambdaFunction<?> relationQuery, SourceInformation sourceInformation, CompileContext compileContext)
    {
        Set<String> allValidationNames = new HashSet<>();
        return Lists.mutable.withAll(relationValidations.stream()
                .map(relationValidation -> buildRelationValidation(relationValidation, relationQuery, allValidationNames, sourceInformation, compileContext))
                .collect(Collectors.toList()));
    }

    private Root_meta_external_dataquality_RelationValidation_Impl buildRelationValidation(RelationValidation relationalValidation, LambdaFunction<?> relationQuery, Set<String> allValidationNames, SourceInformation sourceInformation, CompileContext compileContext)
    {
        Assert.assertTrue(relationalValidation.assertion.parameters.size() == 1, () -> "Assertion should have one input param!");
        Root_meta_external_dataquality_RelationValidation_Impl relationValidation = new Root_meta_external_dataquality_RelationValidation_Impl("", sourceInformation, compileContext.pureModel.getClass("meta::external::dataquality::RelationValidation"));

        if (!allValidationNames.add(relationalValidation.name))
        {
            throw new EngineException("Duplicated validation '" + relationalValidation.name + "'", EngineErrorType.COMPILATION);
        }
        relationValidation._name(relationalValidation.name);
        relationValidation._description(relationalValidation.description);
        relationValidation._type(relationalValidation.type);
        Variable assertionInputParam = relationalValidation.assertion.parameters.get(0);
        assertionInputParam.multiplicity = Multiplicity.PURE_ONE;
        assertionInputParam.genericType = getAssertionInputParamGenericType(relationQuery, relationalValidation.type);

        LambdaFunction<?> assertion = HelperValueSpecificationBuilder.buildLambda(relationalValidation.assertion, compileContext);
        relationValidation._assertion(assertion);
        return relationValidation;
    }

    private org.finos.legend.engine.protocol.pure.m3.type.generics.GenericType getAssertionInputParamGenericType(LambdaFunction<?> relationQuery, String relationValidationType)
    {
        FunctionType functionType = (FunctionType) relationQuery._classifierGenericType()._typeArguments().getLast()._rawType();
        if (Objects.isNull(relationValidationType) || RELATION_ROW_LEVEL_VAL_TYPE.equals(relationValidationType))
        {
            return CompileContext.convertGenericType(functionType._returnType()._typeArguments().getLast());
        }
        return CompileContext.convertGenericType(functionType._returnType());
    }

    private LambdaFunction<?> getFilterLambda(DataQuality app, CompileContext compileContext)
    {
        if (Objects.isNull(app.filter))
        {
            return null;
        }
        return HelperValueSpecificationBuilder.buildLambda(app.filter, compileContext);
    }

    private Root_meta_external_dataquality_DataQualityExecutionContext buildDataQualityExecutionContext(DataQuality app, CompileContext context)
    {
        if (app.context instanceof MappingAndRuntimeDataQualityExecutionContext)
        {
            return buildMappingAndRuntimeExecutionContext(((MappingAndRuntimeDataQualityExecutionContext) app.context), context);
        }
        else if (app.context instanceof DataSpaceDataQualityExecutionContext)
        {
            return buildDataSpaceExecutionContext(((DataSpaceDataQualityExecutionContext) app.context), context);
        }
        throw new EngineException("Unsupported DataQuality ExecutionContext");
    }

    private Root_meta_external_dataquality_MappingAndRuntimeDataQualityExecutionContext buildMappingAndRuntimeExecutionContext(MappingAndRuntimeDataQualityExecutionContext mappingAndRuntimeDataQualityExecutionContext, CompileContext compileContext)
    {
        return new Root_meta_external_dataquality_MappingAndRuntimeDataQualityExecutionContext_Impl(
                null,
                SourceInformationHelper.toM3SourceInformation(mappingAndRuntimeDataQualityExecutionContext.sourceInformation),
                compileContext.pureModel.getClass("meta::external::dataquality::MappingAndRuntimeDataQualityExecutionContext")
        )
                ._mapping(compileContext.resolveMapping(mappingAndRuntimeDataQualityExecutionContext.mapping.path, mappingAndRuntimeDataQualityExecutionContext.mapping.sourceInformation))
                ._runtime(compileContext.resolveRuntime(mappingAndRuntimeDataQualityExecutionContext.runtime.path, mappingAndRuntimeDataQualityExecutionContext.runtime.sourceInformation));

    }

    private Root_meta_external_dataquality_DataSpaceDataQualityExecutionContext buildDataSpaceExecutionContext(DataSpaceDataQualityExecutionContext dataSpaceDataQualityExecutionContext, CompileContext compileContext)
    {
        return new Root_meta_external_dataquality_DataSpaceDataQualityExecutionContext_Impl(
                null,
                SourceInformationHelper.toM3SourceInformation(dataSpaceDataQualityExecutionContext.sourceInformation),
                compileContext.pureModel.getClass("meta::external::dataquality::DataSpaceDataQualityExecutionContext")
        )
                ._dataSpace(HelperDataSpaceBuilder.resolveDataSpace(dataSpaceDataQualityExecutionContext.dataSpace.path, dataSpaceDataQualityExecutionContext.dataSpace.sourceInformation, compileContext))
                ._contextName(dataSpaceDataQualityExecutionContext.context);

    }

    private static GraphFetchTree buildPropertyGraphFetchTree(DataQualityPropertyGraphFetchTree propertyGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        AbstractProperty<?> property;
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> pureParameters = org.eclipse.collections.impl.factory.Lists.mutable.empty();

        Variable thisVariable = new Variable("this", HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport()), new Multiplicity(1, 1));
        MutableList<ValueSpecification> originalParams = org.eclipse.collections.impl.factory.Lists.mutable.<ValueSpecification>with(thisVariable).withAll(propertyGraphFetchTree.parameters);
        property = HelperModelBuilder.getAppliedProperty(context, parentClass, Optional.of(originalParams), propertyGraphFetchTree.property, false, propertyGraphFetchTree.sourceInformation);
        processingContext.push("PropertyTree");
        processingContext.addInferredVariables("this", HelperModelBuilder.createThisVariableForClass(context, HelperModelBuilder.getElementFullPath(parentClass, context.pureModel.getExecutionSupport())));
        Class<?> subType = propertyGraphFetchTree.subType == null ? null : context.resolveClass(propertyGraphFetchTree.subType, propertyGraphFetchTree.sourceInformation);
        Type returnType = subType == null ? property._genericType()._rawType() : subType;

        ListIterable<GraphFetchTree> children = ListIterate.collect(propertyGraphFetchTree.subTrees, subTree -> buildPropertyGraphFetchTree((DataQualityPropertyGraphFetchTree) subTree, context, (Class<?>) returnType, openVariables, processingContext));
        Root_meta_external_dataquality_DataQualityPropertyGraphFetchTree root_meta_external_dataquality_dataQualityPropertyGraphFetchTree = new Root_meta_external_dataquality_DataQualityPropertyGraphFetchTree_Impl("", SourceInformationHelper.toM3SourceInformation(propertyGraphFetchTree.sourceInformation), context.pureModel.getClass("meta::external::dataquality::DataQualityPropertyGraphFetchTree"))
                ._property(property)
                ._parameters(pureParameters)
                ._alias(propertyGraphFetchTree.alias)
                ._subType(subType)
                ._subTrees(children);
        if (returnType instanceof Root_meta_pure_metamodel_type_Class_Impl)
        {
            root_meta_external_dataquality_dataQualityPropertyGraphFetchTree._constraints(resolveNodeConstraints(context, (Class<?>) returnType, propertyGraphFetchTree.constraints));
        }
        return root_meta_external_dataquality_dataQualityPropertyGraphFetchTree; // returnType - current property type - for constraints
    }

    private static Root_meta_external_dataquality_DataQualityRootGraphFetchTree buildRootGraphFetchTree(DataQualityRootGraphFetchTree rootGraphFetchTree, CompileContext context, Class<?> parentClass, MutableList<String> openVariables, ProcessingContext processingContext)
    {
        HashSet<String> subTypeClasses = new HashSet<String>();
        HashSet<String> propertieIdentifiersAtRootLevel = new HashSet<String>();
        for (org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.GraphFetchTree propertyGraphFetchTree : rootGraphFetchTree.subTrees)
        {
            propertieIdentifiersAtRootLevel.add(getPropertyIdentifier((PropertyGraphFetchTree) propertyGraphFetchTree));
        }
        for (SubTypeGraphFetchTree subTypeGraphFetchTree : rootGraphFetchTree.subTypeTrees)
        {
            if (!subTypeClasses.add(subTypeGraphFetchTree.subTypeClass))
            {
                throw new EngineException("There are multiple subTypeTrees having subType " + subTypeGraphFetchTree.subTypeClass + ", Only one is allowed", subTypeGraphFetchTree.sourceInformation, EngineErrorType.COMPILATION);
            }
            for (org.finos.legend.engine.protocol.pure.dsl.graph.valuespecification.constant.classInstance.GraphFetchTree propertyGraphFetchTree : subTypeGraphFetchTree.subTrees)
            {
                String propertyIdentifier = getPropertyIdentifier((PropertyGraphFetchTree) propertyGraphFetchTree);
                if (propertieIdentifiersAtRootLevel.contains(propertyIdentifier))
                {
                    throw new EngineException("Property \"" + propertyIdentifier + "\" is present at root level hence should not be specified at subType level", subTypeGraphFetchTree.sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        }
        ListIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.graphFetch.GraphFetchTree> children = ListIterate.collect(rootGraphFetchTree.subTrees, subTree -> buildPropertyGraphFetchTree((DataQualityPropertyGraphFetchTree) subTree, context, parentClass, openVariables, processingContext));
        Class<?> classifier = context.pureModel.getClass("meta::external::dataquality::DataQualityRootGraphFetchTree");
        GenericType genericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))
                ._rawType(classifier)
                ._typeArguments(org.eclipse.collections.impl.factory.Lists.fixedSize.of(context.pureModel.getGenericType(parentClass)));

        return new Root_meta_external_dataquality_DataQualityRootGraphFetchTree_Impl<>("", SourceInformationHelper.toM3SourceInformation(rootGraphFetchTree.sourceInformation), classifier)
                ._class(parentClass)
                ._classifierGenericType(genericType)
                ._subTrees(children)
                ._constraints(resolveNodeConstraints(context, parentClass, rootGraphFetchTree.constraints));
    }

    /**
     * Recursively go through hierarchical/generalization chain and find the property.
     */
    public static FastList<Constraint> resolveNodeConstraints(CompileContext context, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, List<String> treeConstraintNames)
    {
        FastList<Constraint> constraints = FastList.newList();
        for (CoreInstance c_type : _Class.computeConstraintsInHierarchy(_class, context.pureModel.getExecutionSupport().getProcessorSupport()))
        {
            Root_meta_pure_metamodel_constraint_Constraint_Impl constraint = (Root_meta_pure_metamodel_constraint_Constraint_Impl) c_type;
            if (treeConstraintNames.contains(constraint._name))
            {
                constraints.add(constraint);
            }
        }
        return constraints;
    }

    private static String getPropertyIdentifier(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        return propertyGraphFetchTree.alias != null ? propertyGraphFetchTree.alias : propertyGraphFetchTree.property;
    }


    private RichIterable<? extends Root_meta_core_runtime_Runtime> buildRelationValidationRuntime(PackageableElementPointer runtime, LambdaFunction<?> relationValidationQuery, SourceInformation toM3SourceInformation, CompileContext compileContext)
    {
        if (Objects.isNull(runtime))
        {
            return Lists.immutable.empty();
        }
        return Lists.immutable.of(compileContext.resolveRuntime(runtime.path, runtime.sourceInformation));
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::relationEmpty_Relation_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 1 && handlers.typeOne(ps.get(0), "Relation")))),
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::relationNotEmpty_Relation_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 1 && handlers.typeOne(ps.get(0), "Relation")))),
                        // row count
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::rowCountGreaterThan_Relation_1__Number_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 2 && handlers.typeOne(ps.get(0), "Relation") && handlers.typeOne(ps.get(1), Sets.immutable.with("Number", "Integer", "Float", "Decimal"))))),
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::rowCountGreaterThanEqual_Relation_1__Number_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 2 && handlers.typeOne(ps.get(0), "Relation") && handlers.typeOne(ps.get(1), Sets.immutable.with("Number", "Integer", "Float", "Decimal"))))),
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::rowCountLowerThan_Relation_1__Number_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 2 && handlers.typeOne(ps.get(0), "Relation") && handlers.typeOne(ps.get(1), Sets.immutable.with("Number", "Integer", "Float", "Decimal"))))),
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::rowCountLowerThanEqual_Relation_1__Number_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 2 && handlers.typeOne(ps.get(0), "Relation") && handlers.typeOne(ps.get(1), Sets.immutable.with("Number", "Integer", "Float", "Decimal"))))),
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(handlers.h("meta::external::dataquality::rowCountEqual_Relation_1__Number_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"), ps -> ps.size() == 2 && handlers.typeOne(ps.get(0), "Relation") && handlers.typeOne(ps.get(1), Sets.immutable.with("Number", "Integer", "Float", "Decimal")))))
                ));
    }

}
