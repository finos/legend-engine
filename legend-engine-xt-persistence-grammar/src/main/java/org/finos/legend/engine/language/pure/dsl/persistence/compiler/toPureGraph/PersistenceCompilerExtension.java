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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationResult;
import org.finos.legend.engine.language.pure.dsl.persistence.compiler.validation.ValidationRuleSet;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.DefaultPersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTestBatch;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceContext_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTest;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTestBatch;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTestBatch_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTest_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_Persistence_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_context_PersistencePlatform_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_CronTrigger_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_ManualTrigger_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_Trigger;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_validation_ValidationResult;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_validation_ValidationRuleSet;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.pure.generated.core_persistence_persistence_validation.Root_meta_pure_persistence_validation_validate_T_1__ValidationRuleSet_1__ValidationResult_1_;
import static org.finos.legend.pure.generated.core_persistence_persistence_validations_rules.Root_meta_pure_persistence_validation_commonRules__ValidationRuleSet_1_;

public class PersistenceCompilerExtension implements IPersistenceCompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.fixedSize.of(
                Processor.newProcessor(
                        Persistence.class,
                        Lists.fixedSize.of(Service.class, Mapping.class, Binding.class, PackageableConnection.class, Database.class),
                        (persistence, context) -> new Root_meta_pure_persistence_metamodel_Persistence_Impl(persistence.name, null, context.pureModel.getClass("meta::pure::persistence::metamodel::Persistence"))
                                ._name(persistence.name)
                                ._documentation(persistence.documentation),
                        (persistence, context) ->
                        {
                            Root_meta_pure_persistence_metamodel_Persistence purePersistence = (Root_meta_pure_persistence_metamodel_Persistence) context.pureModel.getOrCreatePackage(persistence._package)._children().detect(c -> persistence.name.equals(c._name()));
                            purePersistence._trigger(HelperPersistenceBuilder.buildTrigger(persistence.trigger, context));
                            purePersistence._service(HelperPersistenceBuilder.buildService(persistence, context));
                            purePersistence._persister(HelperPersistenceBuilder.buildPersister(persistence.persister, context));
                            purePersistence._notifier(HelperPersistenceBuilder.buildNotifier(persistence.notifier, context));
                            purePersistence._tests(HelperPersistenceBuilder.buildTests(persistence, context));
                        }
                ),
                Processor.newProcessor(
                        PersistenceContext.class,
                        Lists.fixedSize.of(Persistence.class, PackageableConnection.class),
                        (persistenceContext, context) -> new Root_meta_pure_persistence_metamodel_PersistenceContext_Impl(persistenceContext.name, null, context.pureModel.getClass("meta::pure::persistence::metamodel::PersistenceContext"))._name(persistenceContext.name),
                        (persistenceContext, context) ->
                        {
                            Root_meta_pure_persistence_metamodel_PersistenceContext purePersistenceContext = (Root_meta_pure_persistence_metamodel_PersistenceContext) context.pureModel.getOrCreatePackage(persistenceContext._package)._children().detect(c -> persistenceContext.name.equals(c._name()));
                            purePersistenceContext._persistence(HelperPersistenceContextBuilder.buildPersistence(persistenceContext, context));
                            purePersistenceContext._platform(HelperPersistenceContextBuilder.buildPersistencePlatform(persistenceContext.platform, context));
                            purePersistenceContext._serviceParameters(ListIterate.collect(persistenceContext.serviceParameters, sp -> HelperPersistenceContextBuilder.buildServiceParameter(sp, context)));
                            purePersistenceContext._sinkConnection(HelperPersistenceContextBuilder.buildConnection(persistenceContext.sinkConnection, context));
                        },
                        (persistenceContext, context) ->
                        {
                        },
                        (persistenceContext, context) ->
                        {
                        },
                        // use the final compiler pass to invoke validation extensions
                        (persistenceContext, context) ->
                        {
                            Root_meta_pure_persistence_metamodel_PersistenceContext purePersistenceContext = (Root_meta_pure_persistence_metamodel_PersistenceContext) context.pureModel.getOrCreatePackage(persistenceContext._package)._children().detect(c -> persistenceContext.name.equals(c._name()));

                            // execute common validations
                            Root_meta_pure_persistence_validation_ValidationRuleSet<? extends Root_meta_pure_persistence_metamodel_PersistenceContext> pureValidationRuleSet = Root_meta_pure_persistence_validation_commonRules__ValidationRuleSet_1_(context.getExecutionSupport());
                            Root_meta_pure_persistence_validation_ValidationResult pureValidationResult = Root_meta_pure_persistence_validation_validate_T_1__ValidationRuleSet_1__ValidationResult_1_(purePersistenceContext, pureValidationRuleSet, context.getExecutionSupport());
                            if (pureValidationResult.invalid(context.getExecutionSupport()))
                            {
                                RichIterable<? extends String> failureReasons = pureValidationResult.reasons(context.getExecutionSupport());
                                throw new EngineException(Iterate.makeString(failureReasons, "Core validation error(s) for persistence context [" + persistenceContext._package + "::" + persistenceContext.name + "]:\n", "\n", ""), persistenceContext.sourceInformation, EngineErrorType.COMPILATION);
                            }

                            // discover and execute platform-specific validations
                            List<ValidationRuleSet<PersistenceContext>> validationRuleSets = ListIterate.collect(IPersistenceCompilerExtension.getExtensions(), IPersistenceCompilerExtension::getExtraValidationRuleset);
                            validationRuleSets.forEach(validationRuleSet ->
                            {
                                ValidationResult result = validationRuleSet.validate(persistenceContext);
                                if (result.invalid())
                                {
                                    throw new EngineException(
                                            String.format("[" + validationRuleSet.name() + "] platform validation error(s) for persistence context [%s]: %s", persistenceContext._package + "::" + persistenceContext.name, result.reasons()),
                                            persistenceContext.sourceInformation,
                                            EngineErrorType.COMPILATION);
                                }
                            });
                        }
                ));
    }

    @Override
    public List<Function2<PersistencePlatform, CompileContext, Root_meta_pure_persistence_metamodel_context_PersistencePlatform>> getExtraPersistencePlatformProcessors()
    {
        return Collections.singletonList((persistencePlatform, compileContext) ->
                persistencePlatform instanceof DefaultPersistencePlatform
                        ? new Root_meta_pure_persistence_metamodel_context_PersistencePlatform_Impl("", null, compileContext.pureModel.getClass("meta::pure::persistence::metamodel::context::PersistencePlatform"))
                        : null);
    }

    @Override
    public List<Function2<Trigger, CompileContext, Root_meta_pure_persistence_metamodel_trigger_Trigger>> getExtraTriggerProcessors()
    {
        return Collections.singletonList((trigger, compileContext) ->
        {
            if (trigger instanceof ManualTrigger)
            {
                return new Root_meta_pure_persistence_metamodel_trigger_ManualTrigger_Impl("", null, compileContext.pureModel.getClass("meta::pure::persistence::metamodel::trigger::ManualTrigger"));
            }
            else if (trigger instanceof CronTrigger)
            {
                CronTrigger cronTrigger = (CronTrigger) trigger;
                return new Root_meta_pure_persistence_metamodel_trigger_CronTrigger_Impl("", null, compileContext.pureModel.getClass("meta::pure::persistence::metamodel::trigger::CronTrigger"))
                    ._minutes(cronTrigger.minutes)
                    ._hours(cronTrigger.hours)
                    ._dayOfMonth(cronTrigger.dayOfMonth)
                    ._month(cronTrigger.month)
                    ._dayOfWeek(cronTrigger.dayOfWeek);
            }
            return null;
        });
    }

    @Override
    public List<Function3<Test, CompileContext, ProcessingContext, org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test>> getExtraTestProcessors()
    {
        return Collections.singletonList((test, context, processingContext) ->
        {
            if (test instanceof PersistenceTest)
            {
                return persistenceTest((PersistenceTest) test, context, processingContext);
            }
            return null;
        });
    }

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test persistenceTest(PersistenceTest persistenceTest, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_pure_persistence_metamodel_PersistenceTest purePersistenceTest = new Root_meta_pure_persistence_metamodel_PersistenceTest_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::PersistenceTest"));

        if (persistenceTest.testBatches == null || persistenceTest.testBatches.isEmpty())
        {
            throw new EngineException("PersistenceTest should have at least 1 testBatch", persistenceTest.sourceInformation, EngineErrorType.COMPILATION);
        }

        List<String> batchIds = ListIterate.collect(persistenceTest.testBatches, t -> t.id);
        List<String> duplicateBatchIds = batchIds.stream().filter(e -> Collections.frequency(batchIds, e) > 1).distinct().collect(Collectors.toList());

        if (!duplicateBatchIds.isEmpty())
        {
            throw new EngineException("Multiple testBatches found with ids : '" + String.join(",", duplicateBatchIds) + "'", persistenceTest.sourceInformation, EngineErrorType.COMPILATION);
        }

        RichIterable<? extends Root_meta_pure_persistence_metamodel_PersistenceTestBatch> testBatches = ListIterate.collect(
                persistenceTest.testBatches,
                batch -> persistenceTestBatch(batch, context, processingContext));

        purePersistenceTest._testBatches(testBatches);
        return purePersistenceTest;
    }

    private static Root_meta_pure_persistence_metamodel_PersistenceTestBatch persistenceTestBatch(PersistenceTestBatch batch, CompileContext context, ProcessingContext processingContext)
    {
        if (batch == null)
        {
            return null;
        }

        Root_meta_pure_persistence_metamodel_PersistenceTestBatch pureTestBatch = new Root_meta_pure_persistence_metamodel_PersistenceTestBatch_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::PersistenceTestBatch"));

        if (batch.assertions == null)
        {
            throw new EngineException("Persistence TestBatch shouldn't have null assert", batch.sourceInformation, EngineErrorType.COMPILATION);
        }
        pureTestBatch._assertions(ListIterate.collect(batch.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, processingContext))));

        if (batch.testData == null)
        {
            throw new EngineException("Persistence TestBatch shouldn't have null test data", batch.sourceInformation, EngineErrorType.COMPILATION);
        }
        pureTestBatch._testData(HelperPersistenceBuilder.processPersistenceTestBatchData(batch.testData, context, processingContext));

        return pureTestBatch;
    }
}
