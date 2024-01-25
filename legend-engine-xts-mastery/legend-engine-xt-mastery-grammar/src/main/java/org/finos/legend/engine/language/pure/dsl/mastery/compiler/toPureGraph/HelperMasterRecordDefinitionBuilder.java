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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.language.pure.dsl.mastery.extension.IMasteryModelGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.Profile;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordService;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.RecordSourceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.CollectionEquality;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.IdentityResolutionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.identity.ResolutionQuery;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

public class HelperMasterRecordDefinitionBuilder
{
    private static final String MASTERY_PACKAGE_PREFIX = "meta::pure::mastery::metamodel";
    private static final String ROOT = "Root";
    private static final Set<String> CONDITIONAL_BLOCK_RULE_PREDICATE_INPUT = newHashSet("incoming", "current");

    private HelperMasterRecordDefinitionBuilder()
    {
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class buildModelClass(MasterRecordDefinition val, CompileContext context)
    {
        Class<?> modelClass = context.resolveClass(val.modelClass);
        return modelClass;
    }

    public static RichIterable<Root_meta_pure_mastery_metamodel_identity_CollectionEquality> buildCollectionEqualities(List<CollectionEquality> collectionEqualities, CompileContext context)
    {
        if (collectionEqualities == null)
        {
            return null;
        }
        return ListIterate.collect(collectionEqualities, collectionEquality ->
                new Root_meta_pure_mastery_metamodel_identity_CollectionEquality_Impl("")
                        ._modelClass(context.resolveClass(collectionEquality.modelClass))
                        ._equalityFunction(BuilderUtil.buildService(collectionEquality.equalityFunction, context, collectionEquality.sourceInformation))
        );
    }

    private static String determineFullPath(Type type)
    {
        Deque<String> deque = new ArrayDeque<>();
        Package currentPackage = ((PackageableElement) type)._package();
        while (!ROOT.equals(currentPackage._name()))
        {
            deque.push(currentPackage._name());
            currentPackage = currentPackage._package();
        }

        return Iterate.makeString(deque, "", "::", "::" + type._name());
    }

    public static Root_meta_pure_mastery_metamodel_identity_IdentityResolution buildIdentityResolution(IdentityResolution identityResolution, String modelClass, CompileContext context)
    {
        return identityResolution.accept(new IdentityResolutionBuilder(context, modelClass));
    }

    private static class IdentityResolutionBuilder implements IdentityResolutionVisitor<Root_meta_pure_mastery_metamodel_identity_IdentityResolution>
    {
        private final CompileContext context;
        private final String modelClass;

        public IdentityResolutionBuilder(CompileContext context, String modelClass)
        {
            this.context = context;
            this.modelClass = modelClass;
        }

        @Override
        public Root_meta_pure_mastery_metamodel_identity_IdentityResolution visit(IdentityResolution protocolVal)
        {
            Root_meta_pure_mastery_metamodel_identity_IdentityResolution_Impl resImpl = new Root_meta_pure_mastery_metamodel_identity_IdentityResolution_Impl("");
            resImpl._resolutionQueriesAddAll(ListIterate.flatCollect(protocolVal.resolutionQueries, this::visitResolutionQuery));
            return resImpl;
        }

        private Iterable<Root_meta_pure_mastery_metamodel_identity_ResolutionQuery> visitResolutionQuery(ResolutionQuery protocolQuery)
        {
            ArrayList<Root_meta_pure_mastery_metamodel_identity_ResolutionQuery> list = new ArrayList<>();
            Root_meta_pure_mastery_metamodel_identity_ResolutionQuery resQuery = new Root_meta_pure_mastery_metamodel_identity_ResolutionQuery_Impl("");

            String KEY_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::identity::ResolutionKeyType";
            resQuery._keyType(protocolQuery.keyType == null ? null : context.resolveEnumValue(KEY_TYPE_FULL_PATH, protocolQuery.keyType.name()));
            resQuery._optional(protocolQuery.optional);
            resQuery._precedence(protocolQuery.precedence);
            if (protocolQuery.filter != null)
            {
                validateFilterInput(protocolQuery.filter);
                resQuery._filter(HelperValueSpecificationBuilder.buildLambda(protocolQuery.filter, context));
            }

            ListIterate.forEachWithIndex(protocolQuery.queries, (lambda, i) ->
                    resQuery._queriesAdd(HelperValueSpecificationBuilder.buildLambda(lambda, context)));
            list.add(resQuery);
            return list;
        }

        private void validateFilterInput(Lambda predicate)
        {
            List<Variable> parameters = predicate.parameters;
            if (parameters.size() != 1)
            {
                throw new EngineException(format("The resolution query filter must have exactly one parameter specified in the lambda function - found %s", parameters.size()), EngineErrorType.COMPILATION);
            }
            Variable parameter = parameters.get(0);
            String parameterClass = parameter._class;
            if (!modelClass.equals(parameterClass))
            {
                throw new EngineException(format("Input Class for the resolution key filter should be %s, however found %s", modelClass, parameterClass), EngineErrorType.COMPILATION);
            }
            if (parameter.multiplicity.lowerBound != 1 || !parameter.multiplicity.isUpperBoundEqualTo(1))
            {
                throw new EngineException("Expected input for resolution key filter to have multiplicity 1", EngineErrorType.COMPILATION);
            }
        }
    }

    public static RichIterable<Root_meta_pure_mastery_metamodel_RecordSource> buildRecordSources(List<RecordSource> recordSources, CompileContext context)
    {
        return ListIterate.collect(recordSources, n -> n.accept(new RecordSourceBuilder(context)));
    }

    public static RichIterable<Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule> buildPrecedenceRules(MasterRecordDefinition masterRecordDefinition, CompileContext context, Set<String> dataProviderTypes)
    {
        Set<String> recordSourceIds = masterRecordDefinition.sources.stream().map(recordSource -> recordSource.id).collect(Collectors.toSet());
        return ListIterate.collect(masterRecordDefinition.precedenceRules, n -> n.accept(new PrecedenceRuleBuilder(context, recordSourceIds, masterRecordDefinition.modelClass, dataProviderTypes)));
    }

    private static class PrecedenceRuleBuilder implements PrecedenceRuleVisitor<Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule>
    {
        private final CompileContext context;
        private final Set<String> recordSourceIds;
        private final String modelClass;
        private final Set<String> validDataProviderTypes;

        public PrecedenceRuleBuilder(CompileContext context, Set<String> recordSourceIds, String modelClass, Set<String> validProviderTypes)
        {
            this.context = context;
            this.recordSourceIds = recordSourceIds;
            this.modelClass = modelClass;
            this.validDataProviderTypes = validProviderTypes;
        }

        @Override
        public Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule visit(PrecedenceRule precedenceRule)
        {
            Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule purePrecedenceRule;
            if (precedenceRule instanceof SourcePrecedenceRule)
            {
                purePrecedenceRule = visitSourcePrecedenceRule(precedenceRule);
            }
            else if (precedenceRule instanceof DeleteRule)
            {
                validateNoDataProviderScope((DeleteRule) precedenceRule);
                purePrecedenceRule = new Root_meta_pure_mastery_metamodel_precedence_DeleteRule_Impl("");
            }
            else if (precedenceRule instanceof CreateRule)
            {
                purePrecedenceRule = new Root_meta_pure_mastery_metamodel_precedence_CreateRule_Impl("");
            }
            else if (precedenceRule instanceof ConditionalRule)
            {
                purePrecedenceRule = visitConditionalRule((ConditionalRule) precedenceRule);
            }
            else
            {
                throw new EngineException("Unrecognized precedence rule", EngineErrorType.COMPILATION);
            }
            return visitPrecedenceRuleBase(precedenceRule, purePrecedenceRule);
        }

        private Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule visitPrecedenceRuleBase(PrecedenceRule precedenceRule, Root_meta_pure_mastery_metamodel_precedence_PrecedenceRule purePrecedenceRule)
        {
            purePrecedenceRule._scope(ListIterate.collect(precedenceRule.scopes, n -> this.visitScopes(n, recordSourceIds)));
            List<PropertyPath> propertyPaths = precedenceRule.paths;
            Class<?> masterRecordClass = context.resolveClass(modelClass);
            purePrecedenceRule._masterRecordFilter(validateAndSetMasterRecordLambda(precedenceRule.masterRecordFilter));
            Class<?> parentClass = masterRecordClass;
            for (PropertyPath propertyPath : propertyPaths)
            {
                Root_meta_pure_mastery_metamodel_precedence_PropertyPath purePropertyPath = visitPath(propertyPath, parentClass);
                purePrecedenceRule._pathsAdd(purePropertyPath);
                Type parentType = purePropertyPath._property()._genericType()._rawType();
                if (parentType instanceof Class)
                {
                    parentClass = (Class<?>) parentType;
                }
                else
                {
                    parentClass = null;
                }
            }
            return purePrecedenceRule;
        }

        private Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule visitSourcePrecedenceRule(PrecedenceRule precedenceRule)
        {
            SourcePrecedenceRule sourcePrecedenceRule = (SourcePrecedenceRule) precedenceRule;
            Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule_Impl pureSourcePrecedenceRule = new Root_meta_pure_mastery_metamodel_precedence_SourcePrecedenceRule_Impl("");
            pureSourcePrecedenceRule._precedence(sourcePrecedenceRule.precedence);
            String ACTION_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::precedence::RuleAction";
            pureSourcePrecedenceRule._action(context.resolveEnumValue(ACTION_TYPE_FULL_PATH, sourcePrecedenceRule.action.name()));
            return pureSourcePrecedenceRule;
        }

        private Root_meta_pure_mastery_metamodel_precedence_ConditionalRule visitConditionalRule(ConditionalRule conditionalRule)
        {
            validateNoScopeSet(conditionalRule);
            Root_meta_pure_mastery_metamodel_precedence_ConditionalRule pureConditionalRule = new Root_meta_pure_mastery_metamodel_precedence_ConditionalRule_Impl("");
            validatePredicateInput(conditionalRule.predicate);
            pureConditionalRule._predicate(HelperValueSpecificationBuilder.buildLambda(conditionalRule.predicate, context));
            return pureConditionalRule;
        }

        private void validatePredicateInput(Lambda predicate)
        {
            validateInputVariableNames(predicate);
            validateInputVariableType(predicate);
            validateInputMultiplicity(predicate);
        }

        private void validateNoScopeSet(ConditionalRule conditionalRule)
        {
            if (!isEmpty(conditionalRule.scopes))
            {
                throw new EngineException(
                       "ConditionalRule with ruleScope is currently unsupported", conditionalRule.sourceInformation,
                        EngineErrorType.COMPILATION);
            }
        }

        private void validateNoDataProviderScope(DeleteRule deleteRule)
        {
            if (!isEmpty(deleteRule.scopes) && deleteRule.scopes.stream().anyMatch(scope -> scope instanceof DataProviderTypeScope))
            {
                throw new EngineException(
                        "DataProviderTypeScope is not allowed on DeleteRule", deleteRule.sourceInformation,
                        EngineErrorType.COMPILATION);
            }
        }

        private void validateInputVariableNames(Lambda predicate)
        {
            Set<String> actualNames = predicate.parameters.stream().map(variable -> variable.name).collect(Collectors.toSet());
            if (!CONDITIONAL_BLOCK_RULE_PREDICATE_INPUT.equals(actualNames))
            {
                throw new EngineException(
                        format("Incorrect input variable names, expect %s but received %s", CONDITIONAL_BLOCK_RULE_PREDICATE_INPUT, actualNames),
                        EngineErrorType.COMPILATION);
            }
        }

        private void validateInputVariableType(Lambda predicate)
        {
            Set<String> inputVariableTypes = predicate.parameters.stream().map(variable -> variable._class).collect(Collectors.toSet());
            if (inputVariableTypes.size() != 1)
            {
                throw new EngineException(format("Two of the same Master Record Classes are expected, received: %s", inputVariableTypes), EngineErrorType.COMPILATION);
            }
            String actualModelClass = inputVariableTypes.stream().findFirst().get();
            if (!modelClass.equals(actualModelClass))
            {
                throw new EngineException(format("Input Class is not expected Master Record class %s, instead was %s", modelClass, actualModelClass), EngineErrorType.COMPILATION);
            }
        }

        private void validateInputMultiplicity(Lambda predicate)
        {
            List<Multiplicity> inputClassMultiplicity = predicate.parameters.stream().map(variable -> variable.multiplicity).collect(Collectors.toList());
            Multiplicity masterRecord1 = inputClassMultiplicity.get(0);
            Multiplicity masterRecord2 = inputClassMultiplicity.get(1);
            if ((masterRecord1.lowerBound != masterRecord2.lowerBound && masterRecord1.lowerBound == 1)
                || !(masterRecord1.getUpperBound().equals(masterRecord2.lowerBound) && masterRecord1.isUpperBoundEqualTo(1)))
            {
                throw new EngineException("Expected both Master Record classes defined with multiplicity 1", EngineErrorType.COMPILATION);
            }
        }

        private Root_meta_pure_mastery_metamodel_precedence_PropertyPath visitPath(PropertyPath propertyPath, Class<?> parentClass)
        {
            Property<?,?> property = (Property<?,?>) context.resolveProperty(determineFullPath(parentClass), propertyPath.property);
            Type propertyClass = property._genericType()._rawType();
            String propertyClassName;
            if ((propertyClass instanceof Class) || (propertyClass instanceof Enumeration))
            {
                propertyClassName = determineFullPath(propertyClass);
            }
            else
            {
                propertyClassName = propertyClass.getName();
            }
            Root_meta_pure_mastery_metamodel_precedence_PropertyPath purePropertyPath = visitRootPath(propertyPath.filter, propertyClassName);
            purePropertyPath._property(property);
            return purePropertyPath;
        }

        private Root_meta_pure_mastery_metamodel_precedence_PropertyPath visitRootPath(Lambda filter, String propertyPathClass)
        {
            Root_meta_pure_mastery_metamodel_precedence_PropertyPath purePropertyPath = new Root_meta_pure_mastery_metamodel_precedence_PropertyPath_Impl("");
            filter.parameters.get(0)._class = propertyPathClass;
            purePropertyPath._filter(HelperValueSpecificationBuilder.buildLambda(filter, context));
            return purePropertyPath;
        }

        private LambdaFunction<?> validateAndSetMasterRecordLambda(Lambda filter)
        {
            String actualMasterRecordQualifiedName = filter.parameters.get(0)._class;
            if (modelClass.equals(actualMasterRecordQualifiedName))
            {
                return HelperValueSpecificationBuilder.buildLambda(filter, context);
            }
            throw new EngineException(format("Path, %s does not match Master Record Class %s", actualMasterRecordQualifiedName, modelClass), EngineErrorType.COMPILATION);
        }

        private Root_meta_pure_mastery_metamodel_precedence_RuleScope visitScopes(RuleScope ruleScope, Set<String> recordSourceIds)
        {
            if (ruleScope instanceof RecordSourceScope)
            {
                RecordSourceScope recordSourceScope = (RecordSourceScope) ruleScope;
                Root_meta_pure_mastery_metamodel_precedence_RecordSourceScope pureRecordSourceScope = new Root_meta_pure_mastery_metamodel_precedence_RecordSourceScope_Impl("");
                if (!recordSourceIds.contains(recordSourceScope.recordSourceId))
                {
                    throw new EngineException(format("Record Source: %s not defined", recordSourceScope.recordSourceId));
                }
                pureRecordSourceScope._recordSourceId(recordSourceScope.recordSourceId);
                return pureRecordSourceScope;
            }
            else if (ruleScope instanceof DataProviderTypeScope)
            {
                DataProviderTypeScope dataProviderTypeScope = (DataProviderTypeScope) ruleScope;

                if (!validDataProviderTypes.contains(dataProviderTypeScope.dataProviderType))
                {
                    throw new EngineException(format("Unrecognized Data Provider Type: %s", dataProviderTypeScope.dataProviderType), ruleScope.sourceInformation, EngineErrorType.COMPILATION);
                }
                Root_meta_pure_mastery_metamodel_precedence_DataProviderTypeScope pureDataProviderTypeScope = new Root_meta_pure_mastery_metamodel_precedence_DataProviderTypeScope_Impl("");
                pureDataProviderTypeScope._dataProviderType(dataProviderTypeScope.dataProviderType);
                return pureDataProviderTypeScope;
            }
            else if (ruleScope instanceof DataProviderIdScope)
            {
                DataProviderIdScope dataProviderIdScope = (DataProviderIdScope) ruleScope;
                getAndValidateDataProvider(dataProviderIdScope.dataProviderId, dataProviderIdScope.sourceInformation, context);
                Root_meta_pure_mastery_metamodel_precedence_DataProviderIdScope pureDataProviderIdScope = new Root_meta_pure_mastery_metamodel_precedence_DataProviderIdScope_Impl("");
                pureDataProviderIdScope._dataProviderId(dataProviderIdScope.dataProviderId.replaceAll("::", "_"));
                return pureDataProviderIdScope;
            }
            else
            {
                throw new EngineException("Invalid Scope defined");
            }
        }
    }

    private static class RecordSourceBuilder implements RecordSourceVisitor<Root_meta_pure_mastery_metamodel_RecordSource>
    {
        private final CompileContext context;

        public RecordSourceBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_mastery_metamodel_RecordSource visit(RecordSource protocolSource)
        {
            validateRecordSource(protocolSource);
            List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
            List<Function2<Authorization, CompileContext, Root_meta_pure_mastery_metamodel_authorization_Authorization>> processors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraAuthorizationProcessors);
            List<Function2<Trigger, CompileContext, Root_meta_pure_mastery_metamodel_trigger_Trigger>> triggerProcessors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraTriggerProcessors);

            String RECORD_SOURCE_STATUS_KEY_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::RecordSourceStatus";
            String PROFILE_KEY_TYPE_FULL_PATH = MASTERY_PACKAGE_PREFIX + "::Profile";
            Root_meta_pure_mastery_metamodel_RecordSource pureSource = new Root_meta_pure_mastery_metamodel_RecordSource_Impl("");
            pureSource._id(protocolSource.id);
            pureSource._description(protocolSource.description);
            pureSource._status(context.resolveEnumValue(RECORD_SOURCE_STATUS_KEY_TYPE_FULL_PATH, protocolSource.status.name()));
            pureSource._sequentialData(protocolSource.sequentialData);
            pureSource._stagedLoad(protocolSource.stagedLoad);
            pureSource._createPermitted(protocolSource.createPermitted);
            pureSource._createBlockedException(protocolSource.createBlockedException);
            pureSource._dataProvider(buildDataProvider(protocolSource, context));
            pureSource._recordService(protocolSource.recordService == null ? null : buildRecordService(protocolSource.recordService, context));
            pureSource._allowFieldDelete(protocolSource.allowFieldDelete);
            pureSource._timeoutInMinutes(protocolSource.timeoutInMinutes == null ? null : Long.valueOf(protocolSource.timeoutInMinutes));
            pureSource._authorization(protocolSource.authorization == null ? null : IMasteryCompilerExtension.process(protocolSource.authorization, processors, context));
            pureSource._trigger(protocolSource.trigger == null ? null : IMasteryCompilerExtension.process(protocolSource.trigger, triggerProcessors, context));
            pureSource._dependencies(buildDependencies(protocolSource));
            pureSource._runProfile(protocolSource.runProfile == null ? null : context.resolveEnumValue(PROFILE_KEY_TYPE_FULL_PATH, protocolSource.runProfile.name()));
            pureSource._raiseExceptionWorkflow(protocolSource.raiseExceptionWorkflow);
            return pureSource;
        }

        private static Root_meta_pure_mastery_metamodel_DataProvider buildDataProvider(RecordSource recordSource, CompileContext context)
        {
            if (recordSource.dataProvider != null)
            {
              return getAndValidateDataProvider(recordSource.dataProvider, recordSource.sourceInformation, context);
            }
            return null;
        }

        private static Root_meta_pure_mastery_metamodel_RecordService buildRecordService(RecordService recordService, CompileContext context)
        {
            List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
            List<Function2<AcquisitionProtocol, CompileContext, Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol>> processors = ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraAcquisitionProtocolProcessors);

            return new Root_meta_pure_mastery_metamodel_RecordService_Impl("")
                    ._parseService(BuilderUtil.buildService(recordService.parseService, context, recordService.sourceInformation))
                    ._transformService(BuilderUtil.buildService(recordService.transformService, context, recordService.sourceInformation))
                    ._acquisitionProtocol(recordService.acquisitionProtocol == null ? null : IMasteryCompilerExtension.process(recordService.acquisitionProtocol, processors, context));
        }

        private static RichIterable<Root_meta_pure_mastery_metamodel_RecordSourceDependency> buildDependencies(RecordSource recordSource)
        {
            if (recordSource.dependencies == null)
            {
                return null;
            }
            return ListIterate.collect(recordSource.dependencies, dependency ->
                   new Root_meta_pure_mastery_metamodel_RecordSourceDependency_Impl("")._dependentRecordSourceId(dependency.dependentRecordSourceId)
            );
        }

        private static void validateRecordSource(RecordSource recordSource)
        {
            if (recordSource.id.length() > 31)
            {
                throw new EngineException(format("Invalid record source id '%s'; id must not be longer than 31 characters.", recordSource.id), recordSource.sourceInformation, EngineErrorType.COMPILATION);
            }

            boolean kafkaSource = nonNull(recordSource.recordService) && nonNull(recordSource.recordService.acquisitionProtocol) && recordSource.recordService.acquisitionProtocol.isKafkaAcquisitionProtocol();

            if (isTrue(recordSource.sequentialData) && kafkaSource && nonNull(recordSource.runProfile) && recordSource.runProfile != Profile.ExtraSmall)
            {
                throw new EngineException("'runProfile' can only be set to ExtraSmall for Delta kafka sources", recordSource.sourceInformation, EngineErrorType.COMPILATION);
            }
            if (kafkaSource && nonNull(recordSource.runProfile) && recordSource.runProfile != Profile.Small)
            {
                throw new EngineException("'runProfile' can only be set to Small for Full Universe kafka sources", recordSource.sourceInformation, EngineErrorType.COMPILATION);
            }
        }
    }

    private static Root_meta_pure_mastery_metamodel_DataProvider getAndValidateDataProvider(String path, SourceInformation sourceInformation, CompileContext context)
    {
        PackageableElement packageableElement = context.resolvePackageableElement(path, sourceInformation);
        if (packageableElement instanceof Root_meta_pure_mastery_metamodel_DataProvider)
        {
            return (Root_meta_pure_mastery_metamodel_DataProvider) packageableElement;
        }
        throw new EngineException(format("DataProvider '%s' is not defined", path), sourceInformation, EngineErrorType.COMPILATION);

    }

    public static PureModelContextData buildMasterRecordDefinitionGeneratedElements(Root_meta_pure_mastery_metamodel_MasterRecordDefinition masterRecordDefinition, CompileContext compileContext, String version)
    {
        return IMasteryModelGenerationExtension.generate(masterRecordDefinition, ListIterate.flatCollect(IMasteryModelGenerationExtension.getExtensions(), IMasteryModelGenerationExtension::getExtraMasteryModelGenerators), compileContext, version);
    }
}