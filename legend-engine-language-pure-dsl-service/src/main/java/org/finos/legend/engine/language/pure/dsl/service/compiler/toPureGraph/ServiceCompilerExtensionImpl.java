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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.TestFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ParameterValue;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTest;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTestSuite;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTestSuite_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_ServiceTest_Impl;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_extension_TaggedValue_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ServiceCompilerExtensionImpl implements ServiceCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                Service.class,
                Lists.fixedSize.with(PackageableConnection.class, PackageableRuntime.class, DataElement.class),
                (service, context) -> new Root_meta_legend_service_metamodel_Service_Impl("")
                        ._name(service.name)
                        ._stereotypes(ListIterate.collect(service.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                        ._taggedValues(ListIterate.collect(service.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("")._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                        ._pattern(service.pattern)
                        ._owners(Lists.mutable.withAll(service.owners))
                        ._documentation(service.documentation),
                (service, context) ->
                {
                    Root_meta_legend_service_metamodel_Service pureService = (Root_meta_legend_service_metamodel_Service) context.pureModel.getOrCreatePackage(service._package)._children().detect(c -> service.name.equals(c._name()));

                    pureService._execution(HelperServiceBuilder.processServiceExecution(service.execution, context));

                    // Legacy flow
                    if (service.test != null)
                    {
                        pureService._test(HelperServiceBuilder.processServiceTest(service.test, context, service.execution));
                    }
                    //
                    if (service.testSuites != null)
                    {
                        RichIterable<? extends VariableExpression> parameters = ((FunctionType) ((Root_meta_legend_service_metamodel_PureExecution) pureService._execution())._func()._classifierGenericType()._typeArguments().getOnly()._rawType())._parameters();

                        List<String> testSuiteIds = ListIterate.collect(service.testSuites, suite -> suite.id);
                        List<String> duplicateTestSuiteIds = testSuiteIds.stream().filter(e -> Collections.frequency(testSuiteIds, e) > 1).distinct().collect(Collectors.toList());

                        if (!duplicateTestSuiteIds.isEmpty())
                        {
                            throw new EngineException("Multiple testSuites found with ids : '" + String.join(",", duplicateTestSuiteIds) + "'", service.sourceInformation, EngineErrorType.COMPILATION);
                        }
                        pureService._tests(ListIterate.collect(service.testSuites, suite ->
                        {
                            Root_meta_legend_service_metamodel_ServiceTestSuite pureServiceTestSuite = (Root_meta_legend_service_metamodel_ServiceTestSuite) suite.accept(new TestFirstPassBuilder(context, new ProcessingContext("Service '" + context.pureModel.buildPackageString(service._package, service.name) + "' Second Pass")));

                            for (Root_meta_pure_test_AtomicTest pureTest : pureServiceTestSuite._tests())
                            {
                                Root_meta_legend_service_metamodel_ServiceTest pureServiceTest = (Root_meta_legend_service_metamodel_ServiceTest) pureTest;
                                HelperServiceBuilder.validateServiceTestParameterValues((List<Root_meta_legend_service_metamodel_ParameterValue>) pureServiceTest._parameters().toList(), parameters, ListIterate.detect(suite.tests, t -> t.id.equals(pureServiceTest._id())).sourceInformation);
                            }
                            return pureServiceTestSuite;
                        }));
                    }
                }));
    }

    @Override
    public List<Function3<Test, CompileContext, ProcessingContext, org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test>> getExtraTestProcessors()
    {
        return Collections.singletonList((test, context, processingContext) ->
        {
            if (test instanceof ServiceTestSuite)
            {
                ServiceTestSuite serviceTestSuite = (ServiceTestSuite) test;
                Root_meta_legend_service_metamodel_ServiceTestSuite pureServiceTestSuite = new Root_meta_legend_service_metamodel_ServiceTestSuite_Impl("");

                if (serviceTestSuite.tests == null || serviceTestSuite.tests.isEmpty())
                {
                    throw new EngineException("Service TestSuites should have atleast 1 test", serviceTestSuite.sourceInformation, EngineErrorType.COMPILATION);
                }

                List<String> testIds = ListIterate.collect(serviceTestSuite.tests, t -> t.id);
                List<String> duplicateTestIds = testIds.stream().filter(e -> Collections.frequency(testIds, e) > 1).distinct().collect(Collectors.toList());

                if (!duplicateTestIds.isEmpty())
                {
                    throw new EngineException("Multiple tests found with ids : '" + String.join(",", duplicateTestIds) + "'", serviceTestSuite.sourceInformation, EngineErrorType.COMPILATION);
                }

                RichIterable<? extends Root_meta_pure_test_AtomicTest> tests = ListIterate.collect(serviceTestSuite.tests, unitTest -> (Root_meta_pure_test_AtomicTest) unitTest.accept(new TestFirstPassBuilder(context, processingContext)));

                pureServiceTestSuite._id(test.id);
                if (serviceTestSuite.testData != null)
                {
                    pureServiceTestSuite._testData(HelperServiceBuilder.processServiceTestSuiteData(serviceTestSuite.testData, context, processingContext));
                }
                pureServiceTestSuite._tests(tests);

                return pureServiceTestSuite;
            }
            else if (test instanceof ServiceTest)
            {
                ServiceTest serviceTest = (ServiceTest) test;
                Root_meta_legend_service_metamodel_ServiceTest pureServiceTest = new Root_meta_legend_service_metamodel_ServiceTest_Impl("");

                pureServiceTest._id(serviceTest.id);
                if (serviceTest.parameters != null && !serviceTest.parameters.isEmpty())
                {
                    pureServiceTest._parameters(ListIterate.collect(serviceTest.parameters, param -> HelperServiceBuilder.processServiceTestParameterValue(param, context)));
                }

                if (serviceTest.assertions == null || serviceTest.assertions.isEmpty())
                {
                    throw new EngineException("Service Tests should have atleast 1 assert", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
                }

                List<String> assertionIds = ListIterate.collect(serviceTest.assertions, a -> a.id);
                List<String> duplicateAssertionIds = assertionIds.stream().filter(e -> Collections.frequency(assertionIds, e) > 1).distinct().collect(Collectors.toList());

                if (!duplicateAssertionIds.isEmpty())
                {
                    throw new EngineException("Multiple assertions found with ids : '" + String.join(",", duplicateAssertionIds) + "'", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
                }

                pureServiceTest._assertions(ListIterate.collect(serviceTest.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, processingContext))));

                return pureServiceTest;
            }
            else
            {
                return null;
            }
        });
    }
}
