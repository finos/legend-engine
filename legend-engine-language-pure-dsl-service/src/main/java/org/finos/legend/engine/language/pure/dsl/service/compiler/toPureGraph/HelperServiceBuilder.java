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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.*;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.Authorizer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer.AuthorizerPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedSingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Execution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_KeyedExecutionParameter;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_KeyedExecutionParameter_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_KeyedSingleExecutionTest;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_KeyedSingleExecutionTest_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionTest;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_MultiExecutionTest_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureMultiExecution_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureSingleExecution_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionTest_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Test;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_TestContainer;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_TestContainer_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Authorizer;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class HelperServiceBuilder
{
    public static List<ServiceCompilerExtension> getServiceCompilerExtensions(CompileContext context)
    {
        return ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), ServiceCompilerExtension.class);
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

    public static Root_meta_legend_service_metamodel_Execution processServiceExecution(Execution execution, CompileContext context)
    {
        if (execution instanceof PureSingleExecution)
        {
            PureSingleExecution pureSingleExecution = (PureSingleExecution) execution;
            Mapping mapping = context.resolveMapping(pureSingleExecution.mapping, pureSingleExecution.mappingSourceInformation);
            inferEmbeddedRuntimeMapping(pureSingleExecution.runtime, pureSingleExecution.mapping);
            org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(pureSingleExecution.runtime, context);
            HelperRuntimeBuilder.checkRuntimeMappingCoverage(runtime, Lists.fixedSize.of(mapping), context, pureSingleExecution.runtime.sourceInformation);
            return new Root_meta_legend_service_metamodel_PureSingleExecution_Impl("")
                    ._func(HelperValueSpecificationBuilder.buildLambda(pureSingleExecution.func, context))
                    ._mapping(mapping)
                    ._runtime(runtime);
        }
        else if (execution instanceof PureMultiExecution)
        {
            PureMultiExecution pureMultiExecution = (PureMultiExecution) execution;
            if (pureMultiExecution.executionParameters.isEmpty())
            {
                throw new EngineException("Service multi execution must not be empty", pureMultiExecution.sourceInformation, EngineErrorType.COMPILATION);
            }
            Set<String> executionKeyValues = new HashSet<>();
            return new Root_meta_legend_service_metamodel_PureMultiExecution_Impl("")
                    ._executionKey(pureMultiExecution.executionKey)
                    ._func(HelperValueSpecificationBuilder.buildLambda(pureMultiExecution.func, context))
                    ._executionParameters(ListIterate.collect(pureMultiExecution.executionParameters, executionParameter -> processServiceKeyedExecutionParameter(executionParameter, context, executionKeyValues)));
        }
        return getServiceCompilerExtensions(context).stream().flatMap(extension -> extension.getExtraServiceExecutionProcessors().stream()).map(processor -> processor.value(execution, context)).filter(Objects::nonNull).findFirst()
                                                    .orElseThrow(() -> new UnsupportedOperationException("Unsupported service execution type '" + execution.getClass().getSimpleName() + "'"));
    }

    private static Root_meta_legend_service_metamodel_KeyedExecutionParameter processServiceKeyedExecutionParameter(KeyedExecutionParameter keyedExecutionParameter, CompileContext context, Set<String> executionKeyValues)
    {
        Mapping mapping = context.resolveMapping(keyedExecutionParameter.mapping, keyedExecutionParameter.mappingSourceInformation);
        inferEmbeddedRuntimeMapping(keyedExecutionParameter.runtime, keyedExecutionParameter.mapping);
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(keyedExecutionParameter.runtime, context);
        HelperRuntimeBuilder.checkRuntimeMappingCoverage(runtime, Lists.fixedSize.of(mapping), context, keyedExecutionParameter.runtime.sourceInformation);
        if (!executionKeyValues.add(keyedExecutionParameter.key))
        {
            throw new EngineException("Execution parameter with key '" + keyedExecutionParameter.key + "' already existed", keyedExecutionParameter.sourceInformation, EngineErrorType.COMPILATION);
        }
        return new Root_meta_legend_service_metamodel_KeyedExecutionParameter_Impl("")
                ._key(keyedExecutionParameter.key)
                ._mapping(mapping)
                ._runtime(runtime);
    }
    public static Root_meta_legend_service_metamodel_Test processServiceTest(ServiceTest serviceTest, CompileContext context, Execution execution)
    {
        if (serviceTest instanceof SingleExecutionTest)
        {
            if (!(execution instanceof PureSingleExecution))
            {
                throw new EngineException("Test does not match execution type", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
            }
            SingleExecutionTest singleExecutionTest = (SingleExecutionTest) serviceTest;
            return new Root_meta_legend_service_metamodel_SingleExecutionTest_Impl("")
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
            Root_meta_legend_service_metamodel_MultiExecutionTest multiTest = new Root_meta_legend_service_metamodel_MultiExecutionTest_Impl("")
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
        return new Root_meta_legend_service_metamodel_KeyedSingleExecutionTest_Impl("")
                ._key(keyedSingleExecutionTest.key)
                ._data(keyedSingleExecutionTest.data)
                ._asserts(ListIterate.collect(keyedSingleExecutionTest.asserts, assertion -> processTestContainer(assertion, context)));
    }

    public static Root_meta_legend_service_metamodel_TestContainer processTestContainer(TestContainer testContainer, CompileContext context)
    {
        return new Root_meta_legend_service_metamodel_TestContainer_Impl("")
                ._parametersValues(ListIterate.collect(testContainer.parametersValues, parameterValue -> parameterValue.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext("")))))
                ._assert(HelperValueSpecificationBuilder.buildLambda(testContainer._assert, context));
    }

    public static Root_meta_legend_service_metamodel_Authorizer processServiceAuthorizer(Authorizer authorizer, CompileContext context) {
        if (authorizer == null)
        {
            return null;
        }
        if (authorizer instanceof AuthorizerPointer) {
            return context.resolveAuthorizer(((AuthorizerPointer) authorizer).authorizer, authorizer.sourceInformation);
        }

        throw new UnsupportedOperationException("Unsupported service authorizer type '" + authorizer.getClass().getSimpleName() + "'");
    }
}
