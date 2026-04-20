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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedProperty;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMappingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionEmbeddedPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.relationFunction.RelationFunctionPropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_SetImplementationContainer_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_function_LambdaFunction_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.OperationSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;

import java.util.Collections;
import java.util.stream.Collectors;

public class ClassMappingSecondPassBuilder implements ClassMappingVisitor<SetImplementation>
{
    private final CompileContext context;
    private final Mapping parentMapping;

    public ClassMappingSecondPassBuilder(CompileContext context, Mapping parentMapping)
    {
        this.context = context;
        this.parentMapping = parentMapping;
    }

    // NOTE: when we remove this visitor, we can return "void"
    @Override
    public SetImplementation visit(ClassMapping classMapping)
    {
        if (classMapping.extendsClassMappingId != null)
        {
            String superSetId = classMapping.extendsClassMappingId;
            ImmutableSet<SetImplementation> superSets = HelperMappingBuilder.getAllClassMappings(this.parentMapping).select(c -> c._id().equals(superSetId));
            if (superSets.isEmpty())
            {
                throw new EngineException("Can't find extends class mapping '" + superSetId + "' in mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            if (superSets.size() > 1)
            {
                String parents = superSets.stream().map(superSet -> "'" + HelperModelBuilder.getElementFullPath(superSet._parent(), this.context.pureModel.getExecutionSupport()) + "'").sorted().collect(Collectors.joining(", "));
                throw new EngineException("Duplicated class mappings found with ID '" + superSetId + "' in mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "'; parent mapping for duplicated: " + parents, classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
        this.context.getCompilerExtensions().getExtraClassMappingSecondPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));
        return null;
    }

    @Override
    public SetImplementation visit(OperationClassMapping classMapping)
    {
        OperationSetImplementation operationSetImplementation = (OperationSetImplementation) this.parentMapping._classMappings().detect(c -> c._id().equals(HelperMappingBuilder.getClassMappingId(classMapping, this.context)));
        return operationSetImplementation._parameters(ListIterate.collect(classMapping.parameters, classMappingId ->
        {
            SetImplementation match = HelperMappingBuilder.getAllClassMappings(this.parentMapping).detect(c -> c._id().equals(classMappingId));
            if (match == null)
            {
                throw new EngineException("Can't find class mapping '" + classMappingId + "' in mapping '" + HelperModelBuilder.getElementFullPath(this.parentMapping, this.context.pureModel.getExecutionSupport()) + "'", classMapping.sourceInformation, EngineErrorType.COMPILATION);
            }
            return new Root_meta_pure_mapping_SetImplementationContainer_Impl("", null, context.pureModel.getClass("meta::pure::mapping::SetImplementationContainer"))._id(classMappingId)._setImplementation(match);
        }));
    }

    @Override
    public SetImplementation visit(PureInstanceClassMapping classMapping)
    {
        return this.visit((ClassMapping)classMapping);
    }

    @Override
    public SetImplementation visit(AggregationAwareClassMapping classMapping)
    {
        this.context.getCompilerExtensions().getExtraAggregationAwareClassMappingSecondPassProcessors().forEach(processor -> processor.value(classMapping, this.parentMapping, this.context));
        return null;
    }

    @Override
    public SetImplementation visit(RelationFunctionClassMapping classMapping)
    {
        // Protocol invariant: exactly one of ~func / ~src must be set. Enforce here rather than
        // trusting the wire format so a malformed JSON payload fails with a clear error instead
        // of silently favouring one branch and dropping the other.
        if (classMapping.relationFunction != null && classMapping.sourceLambda != null)
        {
            throw new EngineException("Relation class mapping must specify exactly one of '~func' or '~src', not both.", classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }

        RelationFunctionInstanceSetImplementation setImpl = (RelationFunctionInstanceSetImplementation) parentMapping._classMappings().detect(c -> c._id().equals(HelperMappingBuilder.getClassMappingId(classMapping, context)));

        // Resolve the source relation from either form of the grammar: a named-function
        // reference (~func) or an inline expression (~src). Both paths yield a function
        // whose last expression carries the row type the property mappings will be typed
        // against, so downstream code can treat them uniformly.
        FunctionDefinition<?> relationFunction;
        if (classMapping.relationFunction != null)
        {
            String functionPath = classMapping.relationFunction.path;
            String functionId;
            try
            {
                functionId = FunctionDescriptor.isValidFunctionDescriptor(functionPath) ? FunctionDescriptor.functionDescriptorToId(functionPath) : functionPath;
            }
            catch (InvalidFunctionDescriptorException e)
            {
                throw new EngineException("Invalid function descriptor specified!", classMapping.relationFunction.sourceInformation, EngineErrorType.COMPILATION, e);
            }
            relationFunction = (FunctionDefinition<?>) context.resolvePackageableElement(functionId, classMapping.sourceInformation);
        }
        else if (classMapping.sourceLambda != null)
        {
            // Inline-expression form: wrap the user expression as a zero-arg lambda so
            // the rest of this method sees the same shape as the named-function form.
            relationFunction = HelperValueSpecificationBuilder.buildLambdaWithContext(
                    classMapping.sourceLambda.body,
                    classMapping.sourceLambda.parameters == null ? Lists.fixedSize.empty() : classMapping.sourceLambda.parameters,
                    classMapping.sourceLambda.sourceInformation,
                    context,
                    new ProcessingContext("Building ~src relation source lambda for mapping '" + HelperMappingBuilder.getClassMappingId(classMapping, context) + "'"));
        }
        else
        {
            throw new EngineException("Relation class mapping must specify either '~func' or '~src'.", classMapping.sourceInformation, EngineErrorType.COMPILATION);
        }
        setImpl._relationFunction(relationFunction);

        // Fail fast if the resolved source doesn't return a relation. Doing this before
        // building any property value functions avoids compiling those against an
        // absent or nonsensical row type and producing misleading downstream errors.
        org.finos.legend.pure.m3.navigation.ProcessorSupport processorSupport = context.pureModel.getExecutionSupport().getProcessorSupport();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType resolvedFnType =
                (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType) processorSupport.function_getFunctionType(relationFunction);
        if (!processorSupport.type_subTypeOf(resolvedFnType._returnType()._rawType(), processorSupport.package_getByUserPath(M3Paths.Relation)))
        {
            throw new EngineException(
                    "Relation mapping function should return a Relation! Found a "
                            + org.finos.legend.pure.m3.navigation.generictype.GenericType.print(resolvedFnType._returnType(), processorSupport)
                            + " instead.",
                    SourceInformationHelper.fromM3SourceInformation(relationFunction.getSourceInformation()),
                    EngineErrorType.COMPILATION);
        }

        // Extract the row type from the source's last expression. Deeper structural
        // checks (e.g. column existence per property) happen in the mapping validation
        // stage; here we just need enough type information to bind `$src` in property
        // value lambdas.
        GenericType lastExprType = relationFunction._expressionSequence().toList().getLast()._genericType();
        MutableList<? extends GenericType> typeArgs = Lists.mutable.withAll(lastExprType._typeArguments());
        GenericType srcType = typeArgs.isEmpty() ? null : typeArgs.getFirst();

        // Attach a value function to each property mapping. Embedded property mappings
        // inherit the parent's source relation and row type, since an embedded set shares
        // the parent's row shape.
        buildValueFunctionsForPropertyMappings(classMapping.propertyMappings, setImpl, relationFunction, srcType);

        return setImpl;
    }

    /**
     * Walks the protocol property mappings alongside their already-built compiled
     * counterparts on {@code parent} and attaches a value-function lambda to each leaf
     * property mapping. Embedded property mappings recurse with the same source relation
     * and row type.
     *
     * <p>Pairing by index is safe here because the first-pass builder emits compiled
     * property mappings in the same order as the protocol input.</p>
     */
    private void buildValueFunctionsForPropertyMappings(java.util.List<PropertyMapping> protocolPropertyMappings, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation parent, FunctionDefinition<?> relationFunction, GenericType srcType)
    {
        MutableList<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping> m3PropertyMappings = Lists.mutable.withAll(parent._propertyMappings());
        for (int i = 0; i < protocolPropertyMappings.size(); i++)
        {
            PropertyMapping protocolPm = protocolPropertyMappings.get(i);
            org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping m3Pm = m3PropertyMappings.get(i);
            if (protocolPm instanceof RelationFunctionPropertyMapping && m3Pm instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping)
            {
                RelationFunctionPropertyMapping pPm = (RelationFunctionPropertyMapping) protocolPm;
                org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping mPm = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping) m3Pm;
                LambdaFunction<?> valueFn = buildPropertyValueFn(pPm, srcType, parent._id());
                if (valueFn != null)
                {
                    mPm._valueFn(valueFn);
                }
            }
            else if (protocolPm instanceof RelationFunctionEmbeddedPropertyMapping && m3Pm instanceof EmbeddedRelationFunctionSetImplementation)
            {
                RelationFunctionEmbeddedPropertyMapping pEmb = (RelationFunctionEmbeddedPropertyMapping) protocolPm;
                EmbeddedRelationFunctionSetImplementation mEmb = (EmbeddedRelationFunctionSetImplementation) m3Pm;
                mEmb._relationFunction(relationFunction);
                // Can be null for Inline embedded
                if (pEmb.propertyMappings != null && !pEmb.propertyMappings.isEmpty())
                {
                    buildValueFunctionsForPropertyMappings(pEmb.propertyMappings, mEmb, relationFunction, srcType);
                }
            }
        }
    }

    /**
     * Synthesises the value-function lambda for a single property mapping. Two protocol
     * shapes are supported:
     * <ul>
     *   <li>Bare column ({@code propName: COL}) — normalised to a property access on
     *       {@code $src} so downstream consumers see a single, uniform shape.</li>
     *   <li>Inline expression ({@code propName: $src.A + $src.B}) — compiled with
     *       {@code src} typed at the source's row type.</li>
     * </ul>
     * Returns {@code null} when neither shape is set; the missing definition is reported
     * later by the mapping validation stage rather than aborting compilation here.
     */
    private LambdaFunction<?> buildPropertyValueFn(RelationFunctionPropertyMapping pm, GenericType srcType, String parentId)
    {
        // Choose the body to compile: the bare-column form is normalised to a property
        // access on `$src`; the inline-expression form is used verbatim.
        java.util.List<ValueSpecification> body;
        if (pm.valueFn != null && pm.valueFn.body != null && !pm.valueFn.body.isEmpty())
        {
            body = pm.valueFn.body;
        }
        else if (pm.column != null && !pm.column.isEmpty())
        {
            Variable srcRef = new Variable();
            srcRef.name = "src";
            srcRef.sourceInformation = pm.sourceInformation;
            AppliedProperty colAccess = new AppliedProperty();
            colAccess.property = pm.column;
            colAccess.parameters = Collections.singletonList(srcRef);
            colAccess.sourceInformation = pm.sourceInformation;
            body = Collections.singletonList(colAccess);
        }
        else
        {
            return null;
        }
        return compileRelationPropertyLambda(body, srcType, parentId + "." + (pm.property == null ? "" : pm.property.property), pm.sourceInformation);
    }

    /**
     * Compiles a body of expressions as a single-arg lambda whose only parameter is
     * {@code src}, bound to the given row type. Follows the same shape used elsewhere
     * for model-to-model transform lambdas.
     */
    private LambdaFunction<?> compileRelationPropertyLambda(java.util.List<ValueSpecification> body, GenericType srcType, String lambdaId, SourceInformation sourceInformation)
    {
        VariableExpression srcVar = new Root_meta_pure_metamodel_valuespecification_VariableExpression_Impl("", null, context.pureModel.getClass(M3Paths.VariableExpression))
                ._name("src")
                ._multiplicity(context.pureModel.getMultiplicity("one"))
                ._genericType(srcType == null
                        // Fallback when the row type can't be inferred (e.g. the source doesn't
                        // actually return a relation). A separate validator surfaces that as
                        // its own error; degrading gracefully here lets compilation continue
                        // and report all problems together.
                        ? context.newGenericType(context.pureModel.getType(M3Paths.Any))
                        : (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(srcType, context.pureModel.getExecutionSupport().getProcessorSupport()));

        MutableList<VariableExpression> pureParameters = Lists.mutable.with(srcVar);
        ProcessingContext ctx = new ProcessingContext("Building relation property valueFn for '" + lambdaId + "'");
        ctx.addVariableLevel();
        ctx.addInferredVariables("src", srcVar);
        MutableList<String> openVariables = Lists.mutable.empty();
        MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification> valueSpecifications =
                ListIterate.collect(body, p -> p.accept(new ValueSpecificationBuilder(context, openVariables, ctx)));
        MutableList<String> cleanedOpenVariables = openVariables.distinct();
        cleanedOpenVariables.removeAll(pureParameters.collect(VariableExpression::_name));
        GenericType functionType = PureModel.buildFunctionType(pureParameters, valueSpecifications.getLast()._genericType(), valueSpecifications.getLast()._multiplicity(), context.pureModel);
        ctx.flushVariable("src");
        ctx.removeLastVariableLevel();

        return new Root_meta_pure_metamodel_function_LambdaFunction_Impl<>(lambdaId, SourceInformationHelper.toM3SourceInformation(sourceInformation), null)
                ._classifierGenericType(context.newGenericType(context.pureModel.getType(M3Paths.LambdaFunction), Lists.mutable.with(functionType)))
                ._openVariables(cleanedOpenVariables)
                ._expressionSequence(valueSpecifications);
    }
}
