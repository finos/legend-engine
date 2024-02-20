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
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionExpressionBuilderRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerDispatchBuilderInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.FunctionHandlerRegistrationInfo;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.Handlers;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.TestFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph.HelperServiceBuilder.processOwnershipModel;

public class ServiceCompilerExtensionImpl implements ServiceCompilerExtension
{
    @Override
    public CompilerExtension build()
    {
        return new ServiceCompilerExtensionImpl();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.with(
                Processor.newProcessor(
                        Service.class,
                        Lists.fixedSize.with(PackageableConnection.class, PackageableRuntime.class, DataElement.class, ExecutionEnvironmentInstance.class),
                        (service, context) -> processserviceFirstPass(service, context),
                        (service, context) ->
                        {
                            Root_meta_legend_service_metamodel_Service pureService = (Root_meta_legend_service_metamodel_Service) context.pureModel.getOrCreatePackage(service._package)._children().detect(c -> service.name.equals(c._name()));

                            pureService._execution(HelperServiceBuilder.processServiceExecution(service.execution, context));

                            // Legacy flow
                            if (service.test != null)
                            {
                                pureService._test(HelperServiceBuilder.processServiceTest(service.test, context, service.execution));
                            }
                            // Strategic flow
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
                                    pureServiceTestSuite._testable(pureService);
                                    for (Root_meta_pure_test_AtomicTest pureTest : pureServiceTestSuite._tests())
                                    {
                                        Root_meta_legend_service_metamodel_ServiceTest pureServiceTest = (Root_meta_legend_service_metamodel_ServiceTest) pureTest;

                                        if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution_Impl && pureServiceTest._keys().size() != 0)
                                        {
                                            throw new EngineException("Service Test cannot have keys for SingleExecution Tests", service.sourceInformation, EngineErrorType.COMPILATION);
                                        }

                                        HelperServiceBuilder.validateServiceTestParameterValues(context, (List<Root_meta_legend_service_metamodel_ParameterValue>) pureServiceTest._parameters().toList(), parameters, ListIterate.detect(suite.tests, t -> t.id.equals(pureServiceTest._id())).sourceInformation);
                                        pureServiceTest._testable(pureService);
                                    }
                                    return pureServiceTestSuite;
                                }));
                            }
                            if (service.testSuites != null && service.test != null)
                            {
                                context.pureModel.addWarnings(Collections.singletonList(new Warning(service.sourceInformation, "Service uses both legacy and strategic test flow. please shift to strategic test flow using test suites.")));
                            }
                            //post validation
                            if (service.postValidations != null)
                            {
                                List<String> validationAssertionIds = ListIterate.flatCollect(service.postValidations, postValidation -> postValidation.assertions).collect(postValidationAssertion -> postValidationAssertion.id);
                                List<String> duplicateValidationAssertionIds = validationAssertionIds.stream().filter(e -> Collections.frequency(validationAssertionIds, e) > 1).distinct().collect(Collectors.toList());

                                if (!duplicateValidationAssertionIds.isEmpty())
                                {
                                    throw new EngineException("Multiple post validation assertions found with ids : '" + String.join(",", duplicateValidationAssertionIds) + "'", service.sourceInformation, EngineErrorType.COMPILATION);
                                }

                                pureService._postValidations(ListIterate.collect(service.postValidations, constraint ->
                                        new Root_meta_legend_service_metamodel_PostValidation_Impl<>("", null, context.pureModel.getClass("meta::legend::service::metamodel::PostValidation"))
                                                ._description(constraint.description)
                                                ._parameters(ListIterate.collect(constraint.parameters, parameter -> HelperValueSpecificationBuilder.buildLambda(parameter, context)))
                                                ._assertions(ListIterate.collect(constraint.assertions, assertion ->
                                                        new Root_meta_legend_service_metamodel_PostValidationAssertion_Impl<>("", null, context.pureModel.getClass("meta::legend::service::metamodel::PostValidationAssertion"))
                                                                ._id(assertion.id)
                                                                ._assertion(HelperValueSpecificationBuilder.buildLambda(assertion.assertion, context))))
                                ));

                                if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureSingleExecution_Impl)
                                {
                                    FunctionType executionFunctionType = (FunctionType) ((Root_meta_legend_service_metamodel_PureSingleExecution_Impl) pureService._execution())._func()._classifierGenericType()._typeArguments().getFirst()._rawType();
                                    pureService._postValidations().forEach(validation ->
                                    {
                                        Assert.assertTrue(executionFunctionType._parameters().size() == validation._parameters().size(),
                                                () -> "Post validation parameter count '" + validation._parameters().size() + "' does not match with service parameter count '" + executionFunctionType._parameters().size() + "'");
                                    });

                                }

                                if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureMultiExecution_Impl)
                                {
                                    FunctionType executionFunctionType = (FunctionType) ((Root_meta_legend_service_metamodel_PureMultiExecution_Impl) pureService._execution())._func()._classifierGenericType()._typeArguments().getFirst()._rawType();
                                    int executionFunctionParamCountWithExecutionKey = executionFunctionType._parameters().size() + 1;

                                    pureService._postValidations().forEach(validation ->
                                    {
                                        Assert.assertTrue(executionFunctionParamCountWithExecutionKey == validation._parameters().size(),
                                                () -> "Post validation parameter count '" + validation._parameters().size() + "' does not match with service parameter count '" + executionFunctionParamCountWithExecutionKey + "'");
                                    });
                                }

                                //validate post validation function parameter type
                                if (pureService._execution() instanceof Root_meta_legend_service_metamodel_PureExecution_Impl)
                                {
                                    FunctionType executionFunctionType = (FunctionType) ((Root_meta_legend_service_metamodel_PureExecution_Impl) pureService._execution())._func()._classifierGenericType()._typeArguments().getFirst()._rawType();
                                    pureService._postValidations().flatCollect(Root_meta_legend_service_metamodel_PostValidation::_assertions).collect(Root_meta_legend_service_metamodel_PostValidationAssertion::_assertion).forEach(assertion ->
                                    {
                                        FunctionType fType = (FunctionType) assertion._classifierGenericType()._typeArguments().getFirst()._rawType();
                                        Assert.assertTrue(fType._parameters() != null && fType._parameters().size() == 1, () -> "Post validation assertion function expects 1 parameter");
                                        Assert.assertTrue(executionFunctionType._returnType().equals(fType._parameters().getFirst()._genericType()) && executionFunctionType._returnMultiplicity().equals(fType._parameters().getAny()._multiplicity()),
                                                () -> "Post validation assertion function parameter type '" +
                                                        fType._parameters().getFirst()._genericType()._rawType()._name() +
                                                        Multiplicity.print(fType._parameters().getFirst()._multiplicity()) +
                                                        "' does not match with service execution return type '" +
                                                        executionFunctionType._returnType()._rawType()._name() +
                                                        Multiplicity.print(executionFunctionType._returnMultiplicity()) +
                                                        "'"
                                        );
                                    });
                                }
                            }
                        }),
                Processor.newProcessor(
                        ExecutionEnvironmentInstance.class,
                        Lists.fixedSize.with(PackageableConnection.class, PackageableRuntime.class),
                        (execEnv, context) -> new Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance_Impl(execEnv.name, null, context.pureModel.getClass("meta::legend::service::metamodel::ExecutionEnvironmentInstance"))
                                ._name(execEnv.name),
                        (execEnv, context) ->
                        {
                            Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = (Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance) context.pureModel.getOrCreatePackage(execEnv._package)._children().detect(c -> execEnv.name.equals(c._name()));
                            pureExecEnv._executionParameters(ListIterate.collect(execEnv.executionParameters, params -> HelperServiceBuilder.processExecutionParameters(params, context)));
                        })
        );
    }

    public Root_meta_legend_service_metamodel_Service processserviceFirstPass(Service service, CompileContext context)
    {
        if (!service.owners.isEmpty() && service.ownership != null)
        {
            throw new EngineException("Cannot use both ownership model and explicit owners list.", service.sourceInformation, EngineErrorType.COMPILATION);
        }
//        if (service.owners == null && service.ownership == null)
//        {
//            throw new EngineException("Must use either ownership model or explicit owners list.", service.sourceInformation, EngineErrorType.COMPILATION);
//        }
        return new Root_meta_legend_service_metamodel_Service_Impl(service.name, null, context.pureModel.getClass("meta::legend::service::metamodel::Service"))
                ._name(service.name)
                ._stereotypes(ListIterate.collect(service.stereotypes, s -> context.resolveStereotype(s.profile, s.value, s.profileSourceInformation, s.sourceInformation)))
                ._taggedValues(ListIterate.collect(service.taggedValues, t -> new Root_meta_pure_metamodel_extension_TaggedValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::extension::TaggedValue"))._tag(context.resolveTag(t.tag.profile, t.tag.value, t.tag.profileSourceInformation, t.tag.sourceInformation))._value(t.value)))
                ._pattern(service.pattern)
                ._owners(Lists.mutable.withAll(service.owners))
                ._ownership(service.ownership != null ? processOwnershipModel(service.ownership) : null)
                ._documentation(service.documentation);
    }

    @Override
    public List<Function3<Test, CompileContext, ProcessingContext, org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test>> getExtraTestProcessors()
    {
        return Collections.singletonList((test, context, processingContext) ->
        {
            if (test instanceof ServiceTestSuite)
            {
                ServiceTestSuite serviceTestSuite = (ServiceTestSuite) test;
                Root_meta_legend_service_metamodel_ServiceTestSuite pureServiceTestSuite = new Root_meta_legend_service_metamodel_ServiceTestSuite_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::ServiceTestSuite"));

                if (serviceTestSuite.tests == null || serviceTestSuite.tests.isEmpty())
                {
                    throw new EngineException("Service TestSuites should have at least 1 test", serviceTestSuite.sourceInformation, EngineErrorType.COMPILATION);
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
                    pureServiceTestSuite._serviceTestData(HelperServiceBuilder.processServiceTestSuiteData(serviceTestSuite.testData, context, processingContext));
                }
                pureServiceTestSuite._tests(tests);

                return pureServiceTestSuite;
            }
            else if (test instanceof ServiceTest)
            {
                ServiceTest serviceTest = (ServiceTest) test;
                Root_meta_legend_service_metamodel_ServiceTest pureServiceTest = new Root_meta_legend_service_metamodel_ServiceTest_Impl("", null, context.pureModel.getClass("meta::legend::service::metamodel::ServiceTest"));

                pureServiceTest._id(serviceTest.id);
                if (serviceTest.parameters != null && !serviceTest.parameters.isEmpty())
                {
                    pureServiceTest._parameters(ListIterate.collect(serviceTest.parameters, param -> HelperServiceBuilder.processServiceTestParameterValue(param, context)));
                }
                if (serviceTest.serializationFormat != null)
                {
                    pureServiceTest._serializationFormat(serviceTest.serializationFormat);
                }
                pureServiceTest._keysAddAll(Lists.mutable.withAll(serviceTest.keys));
                if (serviceTest.assertions == null || serviceTest.assertions.isEmpty())
                {
                    throw new EngineException("Service Tests should have at least 1 assert", serviceTest.sourceInformation, EngineErrorType.COMPILATION);
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

    @Override
    public List<Function<Handlers, List<FunctionHandlerRegistrationInfo>>> getExtraFunctionHandlerRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                org.eclipse.collections.api.factory.Lists.mutable.with(
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::legend::service::validation::assertCollectionEmpty_Any_MANY__String_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"))
                        ),
                        new FunctionHandlerRegistrationInfo(null,
                                handlers.h("meta::legend::service::validation::assertTabularDataSetEmpty_TabularDataSet_1__String_1__Boolean_1_", false, ps -> handlers.res("Boolean", "one"))
                        )
                ));
    }

    @Override
    public Map<String, Function3<Object, CompileContext, ProcessingContext, ValueSpecification>> getExtraClassInstanceProcessors()
    {
        return Maps.mutable.with("executionEnvironmentInstance", (obj, context, processingContext) ->
                {
                    ExecutionEnvironmentInstance execEnv = (ExecutionEnvironmentInstance) obj;
                    Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance pureExecEnv = (Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance) context.pureModel.getOrCreatePackage(execEnv._package)._children().detect(c -> execEnv.name.equals(c._name()));
                    pureExecEnv._executionParameters(ListIterate.collect(execEnv.executionParameters, params -> HelperServiceBuilder.processExecutionParameters(params, context)));
                    GenericType execEnvGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::legend::service::metamodel::ExecutionEnvironmentInstance"));
                    return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                            ._genericType(execEnvGenericType)
                            ._multiplicity(context.pureModel.getMultiplicity("one"))
                            ._values(processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(pureExecEnv));
                }
        );
    }

    @Override
    public List<Function3<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, CompileContext, ProcessingContext, InstanceValue>> getExtraValueSpecificationBuilderForFuncExpr()
    {
        return Lists.mutable.with((packageableElement, context, processingContext) ->
        {
            if (packageableElement instanceof Root_meta_legend_service_metamodel_ExecutionEnvironmentInstance)
            {
                GenericType execEnvGenericType = new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(context.pureModel.getType("meta::legend::service::metamodel::ExecutionEnvironmentInstance"));
                return new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::valuespecification::InstanceValue"))
                        ._genericType(execEnvGenericType)
                        ._multiplicity(context.pureModel.getMultiplicity("one"))
                        ._values(processingContext.peek().equals("Applying new") ? FastList.newList() : FastList.newListWith(packageableElement));
            }
            return null;
        });
    }

    @Override
    public List<Function<Handlers, List<FunctionHandlerDispatchBuilderInfo>>> getExtraFunctionHandlerDispatchBuilderInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                org.eclipse.collections.api.factory.Lists.mutable.with(
                        new FunctionHandlerDispatchBuilderInfo("meta::pure::mapping::from_T_m__SingleExecutionParameters_1__T_m_", (List<ValueSpecification> ps) -> ps.size() == 2 && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "SingleExecutionParameters".equals(ps.get(1)._genericType()._rawType()._name()))),
                        new FunctionHandlerDispatchBuilderInfo("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__SingleExecutionParameters_1_", (List<ValueSpecification> ps) -> ps.size() == 2 && handlers.isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "ExecutionEnvironmentInstance".equals(ps.get(0)._genericType()._rawType()._name())) && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name()))),
                        new FunctionHandlerDispatchBuilderInfo("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__String_1__SingleExecutionParameters_1_", (List<ValueSpecification> ps) -> ps.size() == 3 && handlers.isOne(ps.get(0)._multiplicity()) && ("Nil".equals(ps.get(0)._genericType()._rawType()._name()) || "ExecutionEnvironmentInstance".equals(ps.get(0)._genericType()._rawType()._name())) && handlers.isOne(ps.get(1)._multiplicity()) && ("Nil".equals(ps.get(1)._genericType()._rawType()._name()) || "String".equals(ps.get(1)._genericType()._rawType()._name())) && handlers.isOne(ps.get(2)._multiplicity()) && ("Nil".equals(ps.get(2)._genericType()._rawType()._name()) || "String".equals(ps.get(2)._genericType()._rawType()._name())))
                ));
    }

    @Override
    public List<Function<Handlers, List<FunctionExpressionBuilderRegistrationInfo>>> getExtraFunctionExpressionBuilderRegistrationInfoCollectors()
    {
        return Collections.singletonList((handlers) ->
                org.eclipse.collections.api.factory.Lists.mutable.with(
                        new FunctionExpressionBuilderRegistrationInfo(Lists.mutable.with(0),
                                handlers.m(handlers.h("meta::pure::mapping::from_T_m__SingleExecutionParameters_1__T_m_", false, ps -> handlers.res(ps.get(0)._genericType(), ps.get(0)._multiplicity()), ps -> ps.size() == 2))
                        ),
                        // getter for execution parameters from execution environment
                        new FunctionExpressionBuilderRegistrationInfo(null, handlers.m(
                                handlers.m(handlers.h("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__SingleExecutionParameters_1_", false, ps -> handlers.res("meta::legend::service::metamodel::SingleExecutionParameters", "one"), ps -> ps.size() == 2)),
                                handlers.m(handlers.h("meta::legend::service::get_ExecutionEnvironmentInstance_1__String_1__String_1__SingleExecutionParameters_1_", false, ps -> handlers.res("meta::legend::service::metamodel::SingleExecutionParameters", "one"), ps -> ps.size() == 3))))

                ));
    }


}
