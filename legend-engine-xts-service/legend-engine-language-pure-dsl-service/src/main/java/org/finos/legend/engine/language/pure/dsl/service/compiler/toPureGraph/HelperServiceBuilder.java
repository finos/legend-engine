// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.internal.IterableIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.*;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.*;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HelperServiceBuilder
{
    public static List<ServiceCompilerExtension> getServiceCompilerExtensions(CompileContext context)
    {
        return ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), ServiceCompilerExtension.class);
    }

    public static Root_meta_legend_service_metamodel_Ownership processOwnershipModel(Ownership o)
    {
        if (o instanceof DeploymentOwnership)
        {
            return new Root_meta_legend_service_metamodel_DeploymentOwner_Impl("")._identifier(((DeploymentOwnership) o).identifier);
        }
        else if (o instanceof UserListOwnership)
        {
            return new Root_meta_legend_service_metamodel_UserListOwner_Impl("")._usersAddAll(Lists.mutable.withAll(((UserListOwnership) o).users));
        }
        else
        {
            throw new EngineException("Ownership model not supported. Type: " + o.getClass().getSimpleName(), EngineErrorType.COMPILATION);
        }
    }

    private static void inferEmbeddedRuntimeMapping(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime runtime, String mappingPath)
    {
        // If the runtime is embedded and no mapping is specified, we will take the mapping of the execution as the mapping for the runtime
        if (runtime instanceof EngineRuntime)
        {
            EngineRuntime engineRuntime = (EngineRuntime) runtime;
            if (engineRuntime.mappings.isEmpty())
            {
                PackageableElementPointer mappingPointer = new PackageableElementPointer();
                mappingPointer.sourceInformation = runtime.sourceInformation;
                mappingPointer.type = PackageableElementType.MAPPING;
                mappingPointer.path = mappingPath;
                engineRuntime.mappings.add(mappingPointer);
            }
        }
    }

    public static Root_meta_legend_service_metamodel_Execution processServiceExecution(Execution execution, Service service, CompileContext context)
    {
        if (execution instanceof PureSingleExecution)
        {
            PureSingleExecution pureSingleExecution = (PureSingleExecution) execution;
            Mapping mapping = null;
            Root_meta_core_runtime_Runtime runtime = null;
            LambdaFunction<?> lambda;
            if (pureSingleExecution.mapping != null && pureSingleExecution.runtime != null)
            {
                mapping = context.resolveMapping(pureSingleExecution.mapping, pureSingleExecution.mappingSourceInformation);
                inferEmbeddedRuntimeMapping(pureSingleExecution.runtime, pureSingleExecution.mapping);
                runtime = HelperRuntimeBuilder.buildPureRuntime(pureSingleExecution.runtime, context);
                checkMappingRuntimeCompatibility(() -> "Service '" + service.getPath() + "'", runtime, pureSingleExecution.runtime, mapping, pureSingleExecution.mapping, pureSingleExecution.runtime.sourceInformation, context.pureModel);
                lambda = HelperValueSpecificationBuilder.buildLambda(pureSingleExecution.func, context);
            }
            else
            {
                lambda = HelperValueSpecificationBuilder.buildLambda(pureSingleExecution.func, context);
            }
            return new Root_meta_legend_service_metamodel_PureSingleExecution_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::PureSingleExecution"))
                    ._func(lambda)
                    ._mapping(mapping)
                    ._runtime(runtime);
        }
        else if (execution instanceof PureMultiExecution)
        {
            PureMultiExecution pureMultiExecution = (PureMultiExecution) execution;
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(pureMultiExecution.func, context);
            //TODO: a more robust validation
            if ((pureMultiExecution.executionParameters != null && pureMultiExecution.executionParameters.isEmpty()) || (pureMultiExecution.executionParameters == null && !org.finos.legend.pure.generated.core_service_service_helperFunctions.Root_meta_legend_service_isFromFunctionPresent_FunctionDefinition_1__Boolean_1_(lambda, context.getExecutionSupport())))
            {
                throw new EngineException("Service multi execution must not be empty", pureMultiExecution.sourceInformation, EngineErrorType.COMPILATION);
            }
            Set<String> executionKeyValues = new HashSet<>();
            if (pureMultiExecution.executionParameters != null && !pureMultiExecution.executionParameters.isEmpty())
            {
                return new Root_meta_legend_service_metamodel_PureMultiExecution_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::PureMultiExecution"))
                        ._executionKey(pureMultiExecution.executionKey)
                        ._func(lambda)
                        ._executionParameters(ListIterate.collect(pureMultiExecution.executionParameters, executionParameter -> processServiceKeyedExecutionParameter(executionParameter, service, context, executionKeyValues)));
            }
            else
            {
                return new Root_meta_legend_service_metamodel_PureMultiExecution_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::PureMultiExecution"))
                        ._executionKey(org.finos.legend.pure.generated.core_service_service_helperFunctions.Root_meta_legend_service_getKeyFromFunctionDefinition_FunctionDefinition_1__String_1_(lambda, context.getExecutionSupport()))
                        ._func(lambda);
            }
        }
        return getServiceCompilerExtensions(context).stream().flatMap(extension -> extension.getExtraServiceExecutionProcessors().stream()).map(processor -> processor.value(execution, context)).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported service execution type '" + execution.getClass().getSimpleName() + "'"));
    }

    private static Root_meta_legend_service_metamodel_KeyedExecutionParameter processServiceKeyedExecutionParameter(KeyedExecutionParameter keyedExecutionParameter, Service service, CompileContext context, Set<String> executionKeyValues)
    {
        Mapping mapping = context.resolveMapping(keyedExecutionParameter.mapping, keyedExecutionParameter.mappingSourceInformation);
        inferEmbeddedRuntimeMapping(keyedExecutionParameter.runtime, keyedExecutionParameter.mapping);
        Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(keyedExecutionParameter.runtime, context);

        checkMappingRuntimeCompatibility(() -> "Service '" + service.getPath() + "', key: '" + keyedExecutionParameter.key + "',", runtime, keyedExecutionParameter.runtime, mapping, keyedExecutionParameter.mapping, keyedExecutionParameter.runtime.sourceInformation, context.pureModel);
        if (!executionKeyValues.add(keyedExecutionParameter.key))
        {
            throw new EngineException("Execution parameter with key '" + keyedExecutionParameter.key + "' already existed", keyedExecutionParameter.sourceInformation, EngineErrorType.COMPILATION);
        }
        return new Root_meta_legend_service_metamodel_KeyedExecutionParameter_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::KeyedExecutionParameter"))
                ._key(keyedExecutionParameter.key)
                ._mapping(mapping)
                ._runtime(runtime);
    }

    public static Root_meta_legend_service_metamodel_ServiceTestData processServiceTestSuiteData(TestData testData, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_legend_service_metamodel_ServiceTestData pureTestData = new Root_meta_legend_service_metamodel_ServiceTestData_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::ServiceTestData"));

        if (testData.connectionsTestData != null && !testData.connectionsTestData.isEmpty())
        {
            pureTestData._connectionsTestData(ListIterate.collect(testData.connectionsTestData, data -> HelperServiceBuilder.processServiceConnectionData(data, context, processingContext)));
        }

        return pureTestData;
    }

    private static Root_meta_legend_service_metamodel_ConnectionTestData processServiceConnectionData(ConnectionTestData connectionData, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_legend_service_metamodel_ConnectionTestData pureConnectionData = new Root_meta_legend_service_metamodel_ConnectionTestData_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::ConnectionTestData"));

        pureConnectionData._connectionId(connectionData.id);
        pureConnectionData._testData(connectionData.data.accept(new EmbeddedDataFirstPassBuilder(context, processingContext)));

        return pureConnectionData;
    }

    public static Root_meta_legend_service_metamodel_ParameterValue processServiceTestParameterValue(ParameterValue parameterValue, CompileContext context)
    {
        Root_meta_legend_service_metamodel_ParameterValue pureParameterValue = new Root_meta_legend_service_metamodel_ParameterValue_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::ParameterValue"));

        pureParameterValue._name(parameterValue.name);
        pureParameterValue._value(Lists.immutable.with(parameterValue.value.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("")))));

        return pureParameterValue;
    }

    public static void validateServiceTestParameterValues(CompileContext context, List<Root_meta_legend_service_metamodel_ParameterValue> parameterValues, RichIterable<? extends VariableExpression> parameters, SourceInformation sourceInformation)
    {
        for (VariableExpression param : parameters)
        {
            Optional<Root_meta_legend_service_metamodel_ParameterValue> parameterValue = ListIterate.detectOptional(parameterValues, p -> p._name().equals(param._name()));

            if (parameterValue.isPresent())
            {
                InstanceValue paramValue = (InstanceValue) parameterValue.get()._value().getOnly();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity paramMultiplicity = param._multiplicity();
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity paramValueMultiplicity = paramValue._multiplicity();
                if (!"Nil".equals(paramValue._genericType()._rawType()))
                {
                    HelperModelBuilder.checkCompatibility(context, paramValue._genericType()._rawType(), paramValueMultiplicity, param._genericType(), paramMultiplicity, "Parameter value type does not match with parameter type for parameter: '" + param._name() + "'", sourceInformation);
                }
            }
            else
            {
                if (param._multiplicity()._lowerBound() != null && param._multiplicity()._lowerBound()._value() != null && param._multiplicity()._lowerBound()._value() != 0)
                {
                    throw new EngineException("Parameter value required for parameter: '" + param._name() + "'", sourceInformation, EngineErrorType.COMPILATION);
                }
            }
        }
    }

    public static Root_meta_legend_service_metamodel_Test processServiceTest(ServiceTest_Legacy serviceTest, CompileContext context, Execution execution)
    {
        if (serviceTest instanceof SingleExecutionTest)
        {
            if (!(execution instanceof PureSingleExecution))
            {
                throw new EngineException("Test does not match execution type", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            SingleExecutionTest singleExecutionTest = (SingleExecutionTest) serviceTest;
            return new Root_meta_legend_service_metamodel_SingleExecutionTest_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::SingleExecutionTest"))
                    ._data(singleExecutionTest.data)
                    ._asserts(ListIterate.collect(singleExecutionTest.asserts, assertion -> processTestContainer(assertion, context)));
        }
        else if (serviceTest instanceof MultiExecutionTest)
        {
            if (!(execution instanceof PureMultiExecution))
            {
                throw new EngineException("Test does not match execution type", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            Set<String> executionKeyValues = ((PureMultiExecution) execution).executionParameters.stream().map(ep -> ep.key).collect(Collectors.toSet());
            Set<String> testKeyValues = new HashSet<>();
            MultiExecutionTest multiExecutionTest = (MultiExecutionTest) serviceTest;
            if (multiExecutionTest.tests.isEmpty())
            {
                throw new EngineException("Service multi execution test must not be empty", multiExecutionTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            Root_meta_legend_service_metamodel_MultiExecutionTest multiTest = new Root_meta_legend_service_metamodel_MultiExecutionTest_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::MultiExecutionTest"))
                    ._tests(ListIterate.collect(multiExecutionTest.tests, test -> processServiceKeyedSingleExecutionTest(test, context, testKeyValues)));
            /**
             * Here, we verify matching key values between multi execution and multi test
             * NOTE: since test depends on execution, we definitely want to throw when no execution is found for a test.
             * The other direction is debatable, on one hand it makes sense to have a test for each execution, but a lot of time
             * (and majorly for backward compatibility reasons) we have executions that only differ in connection information like credentials, etc.
             * as such it is immaterial to have test for these executions.
             */
            List<String> testWithoutExecutionKeys = testKeyValues.stream().filter(testKeyValue -> !executionKeyValues.contains(testKeyValue)).collect(Collectors.toList());
            executionKeyValues.removeAll(testKeyValues);
            if (!testWithoutExecutionKeys.isEmpty())
            {
                throw new EngineException("Test(s) with key '" + StringUtils.join(testWithoutExecutionKeys, "', '") + "' do not have a corresponding execution", multiExecutionTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            return multiTest;
        }
        return getServiceCompilerExtensions(context).stream().flatMap(extension -> extension.getExtraServiceTestProcessors().stream()).map(processor -> processor.value(serviceTest, execution, context)).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported service test type '" + serviceTest.getClass().getSimpleName() + "'"));
    }

    private static Root_meta_legend_service_metamodel_KeyedSingleExecutionTest processServiceKeyedSingleExecutionTest(KeyedSingleExecutionTest keyedSingleExecutionTest, CompileContext context, Set<String> testKeyValues)
    {
        if (!testKeyValues.add(keyedSingleExecutionTest.key))
        {
            throw new EngineException("Service test with key '" + keyedSingleExecutionTest.key + "' already existed", keyedSingleExecutionTest.sourceInformation, EngineErrorType.COMPILATION);
        }
        return new Root_meta_legend_service_metamodel_KeyedSingleExecutionTest_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::KeyedSingleExecutionTest"))
                ._key(keyedSingleExecutionTest.key)
                ._data(keyedSingleExecutionTest.data)
                ._asserts(ListIterate.collect(keyedSingleExecutionTest.asserts, assertion -> processTestContainer(assertion, context)));
    }

    public static Root_meta_legend_service_metamodel_TestContainer processTestContainer(TestContainer testContainer, CompileContext context)
    {
        // todo hack to support legacy test flow
        // this is to ensure generics are set and prevent NPE
        // this assume a cache for types, and this prime the value
        context.resolveGenericType("meta::pure::mapping::Result")
                ._typeArguments(Lists.fixedSize.of(context.resolveGenericType("meta::pure::metamodel::type::Any")))
                ._multiplicityArguments(Lists.fixedSize.of(context.pureModel.getMultiplicity("zeromany")));

        return new Root_meta_legend_service_metamodel_TestContainer_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::TestContainer"))
                ._parametersValues(ListIterate.collect(testContainer.parametersValues, parameterValue -> parameterValue.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("")))))
                ._assert(HelperValueSpecificationBuilder.buildLambda(testContainer._assert, context));
    }

    public static Root_meta_legend_service_metamodel_ExecutionParameters processExecutionParameters(ExecutionParameters params, CompileContext context)
    {
        if (params instanceof SingleExecutionParameters)
        {
            SingleExecutionParameters execParams = (SingleExecutionParameters) params;
            Mapping mapping = context.resolveMapping(execParams.mapping, execParams.mappingSourceInformation);
            Root_meta_legend_service_metamodel_SingleExecutionParameters param = new Root_meta_legend_service_metamodel_SingleExecutionParameters_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::SingleExecutionParameters"))
                    ._key(execParams.key)
                    ._mapping(mapping);
            if (execParams.runtime != null && execParams.runtimeComponents != null)
            {
                throw new EngineException("Cannot use both runtime and runtime components", execParams.sourceInformation, EngineErrorType.COMPILATION);
            }
            if (execParams.runtime != null)
            {
                inferEmbeddedRuntimeMapping(execParams.runtime, execParams.mapping);
                Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(execParams.runtime, context);

                param._runtime(runtime);
            }
            else
            {
                Assert.assertTrue(execParams.runtimeComponents != null, () -> "Runtime components must be specified when runtime isn't");
                RuntimeComponents c = execParams.runtimeComponents;
                PackageableElement binding;
                try
                {
                    binding = platform_pure_essential_meta_graph_pathToElement.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(c.binding.path, context.getExecutionSupport());
                    Assert.assertTrue(binding instanceof Root_meta_external_format_shared_binding_Binding, () -> "provide a valid Binding");
                }
                catch (Exception e)
                {
                    throw new EngineException("Cannot resolve binding. Error: " + e.getMessage(), execParams.runtimeComponents.sourceInformation, EngineErrorType.COMPILATION);
                }

                Root_meta_legend_service_metamodel_RuntimeComponents components = new Root_meta_legend_service_metamodel_RuntimeComponents_Impl("")
                        ._runtime(HelperRuntimeBuilder.buildPureRuntime(c.runtime, context))
                        ._class(context.resolveClass(c.clazz.path))
                        ._binding((Root_meta_external_format_shared_binding_Binding)binding);
                param._runtimeComponents(components);
            }
            return param;
        }
        else if (params instanceof MultiExecutionParameters)
        {
            MultiExecutionParameters execParams = (MultiExecutionParameters) params;
            return new Root_meta_legend_service_metamodel_MultiExecutionParameters_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::MultiExecutionParameters"))
                    ._masterKey(execParams.masterKey)
                    ._singleExecutionParameters(ListIterate.collect(execParams.singleExecutionParameters,
                            param -> (Root_meta_legend_service_metamodel_SingleExecutionParameters) processExecutionParameters(param, context)));
        }
        throw new UnsupportedOperationException("Unsupported service execution type '" + params.getClass().getSimpleName() + "'");
    }

    private static void checkMappingRuntimeCompatibility(Supplier<String> context, Root_meta_core_runtime_Runtime pureRuntime, Runtime runtime, Mapping pureMapping, String mapping, SourceInformation sourceInformation, PureModel pureModel)
    {
        boolean compatible = !(pureRuntime instanceof Root_meta_core_runtime_EngineRuntime) || HelperRuntimeBuilder.isRuntimeCompatibleWithMapping((Root_meta_core_runtime_EngineRuntime) pureRuntime, pureMapping);

        if (!compatible)
        {
            String runtimeName = runtime instanceof RuntimePointer ? " '" + ((RuntimePointer) runtime).runtime + "'" : "";
            pureModel.addWarnings(Lists.mutable.with(new Warning(sourceInformation, context.get() + " Runtime" + runtimeName + " does not cover mapping '" + mapping + "'")));
        }
    }

    public static void validate(ExecutionEnvironmentInstance execEnv, Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv, CompileContext compileContext)
    {
        checkRuntimeMappingCompatibility(execEnv, pureExecEnv, compileContext);
    }

    public static void checkRuntimeMappingCompatibility(ExecutionEnvironmentInstance execEnv, Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv, CompileContext compileContext)
    {
        Map<String, SingleExecutionParameters> params = IterableIterate.flatCollect(execEnv.executionParameters, p ->
        {
            if (p instanceof SingleExecutionParameters)
            {
                return FastList.newListWith((SingleExecutionParameters) p);
            }
            else if (p instanceof MultiExecutionParameters)
            {
                return FastList.newList(((MultiExecutionParameters) p).singleExecutionParameters);
            }
            return FastList.newList();
        }).groupByUniqueKey(s -> s.key);

        IterableIterate.forEach(pureExecEnv._executionParameters(), e ->
        {
            if (e instanceof Root_meta_legend_service_metamodel_MultiExecutionParameters)
            {
                IterableIterate.forEach(((Root_meta_legend_service_metamodel_MultiExecutionParameters) e)._singleExecutionParameters(),
                        s -> checkMappingRuntimeCompatibility(execEnv, s._key(), s, params.get(s._key()), compileContext)
                );
            }
            else if (e instanceof Root_meta_legend_service_metamodel_SingleExecutionParameters)
            {
                Root_meta_legend_service_metamodel_SingleExecutionParameters param = (Root_meta_legend_service_metamodel_SingleExecutionParameters) e;
                checkMappingRuntimeCompatibility(execEnv, param._key(), param, params.get(param._key()), compileContext);
            }
            else
            {
                throw new UnsupportedOperationException("Unsupported service execution type '" + execEnv.getClass().getSimpleName() + "'");
            }
        });
    }

    private static void checkMappingRuntimeCompatibility(ExecutionEnvironmentInstance execEnv, String key, Root_meta_legend_service_metamodel_SingleExecutionParameters pureParam, SingleExecutionParameters param, CompileContext compileContext)
    {
        Root_meta_core_runtime_Runtime pureRuntime = pureParam._runtime() == null ? pureParam._runtimeComponents()._runtime() : pureParam._runtime();
        Runtime runtime = param.runtime == null ? param.runtimeComponents.runtime : param.runtime;
        checkMappingRuntimeCompatibility(() -> "Execution Environment '" + execEnv.getPath() + "', key: '" + key + "',", pureRuntime, runtime, pureParam._mapping(), param.mapping, runtime.sourceInformation, compileContext.pureModel);
    }
}
