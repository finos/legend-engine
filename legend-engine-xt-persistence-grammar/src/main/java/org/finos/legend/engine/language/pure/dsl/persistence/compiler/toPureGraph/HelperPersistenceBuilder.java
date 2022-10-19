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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.TestAssertionFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.EmailNotifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.Notifier;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.NotifyeeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.PagerDutyNotifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.PersisterVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.AnyVersionDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DuplicateCountDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.MaxVersionDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.NoDeduplicationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.ObjectStorageSink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.RelationalSink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.Sink;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.sink.SinkVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.FlatTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.MultiFlatTarget;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.MultiFlatTargetPart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.TargetShape;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.TargetShapeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.SourceSpecifiesInAndOutDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.SourceSpecifiesInDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.TransactionDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.derivation.TransactionDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.PersistenceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_ConnectionTestData;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_ConnectionTestData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTest;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTestBatch;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTestBatch_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_PersistenceTest_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_TestData;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_TestData_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_Notifier;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_Notifier_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_Notifyee;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_BatchPersister_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_Persister;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_StreamingPersister_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_audit_Auditing;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_audit_DateTimeAuditing_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_audit_NoAuditing_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_deduplication_AnyVersionDeduplicationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_deduplication_DuplicateCountDeduplicationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_deduplication_NoDeduplicationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_appendonly_AppendOnly_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_NontemporalDelta_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_NoDeletesMergeStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_BitemporalSnapshot_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_NontemporalSnapshot_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_UnitemporalSnapshot_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_sink_ObjectStorageSink_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_sink_Sink;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdTransactionMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInAndOutDate_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInDate_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromDate_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation;
import org.finos.legend.pure.generated.Root_meta_pure_persistence_metamodel_trigger_Trigger;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HelperPersistenceBuilder
{
    private static final String PERSISTENCE_PACKAGE_PREFIX = "meta::pure::persistence::metamodel";

    private static final AuditingBuilder AUDITING_BUILDER = new AuditingBuilder();

    private HelperPersistenceBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_trigger_Trigger buildTrigger(Trigger trigger, CompileContext context)
    {
        return IPersistenceCompilerExtension.process(trigger, ListIterate.flatCollect(IPersistenceCompilerExtension.getExtensions(), IPersistenceCompilerExtension::getExtraTriggerProcessors), context);
    }

    public static Root_meta_legend_service_metamodel_Service buildService(Persistence persistence, CompileContext context)
    {
        String service = persistence.service;
        String servicePath = service.substring(0, service.lastIndexOf("::"));
        String serviceName = service.substring(service.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(servicePath)._children().detect(c -> serviceName.equals(c._name()));
        if (packageableElement instanceof Root_meta_legend_service_metamodel_Service)
        {
            return (Root_meta_legend_service_metamodel_Service) packageableElement;
        }

        throw new EngineException(String.format("Service '%s' is not defined", service), persistence.sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_persistence_metamodel_persister_Persister buildPersister(Persister persister, CompileContext context)
    {
        return persister.accept(new PersisterBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_notifier_Notifier buildNotifier(Notifier notifier, CompileContext context)
    {
        return new Root_meta_pure_persistence_metamodel_notifier_Notifier_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::notifier::Notifier"))
                ._notifyees(ListIterate.collect(notifier.notifyees, n -> n.acceptVisitor(new NotifyeeBuilder(context))));
    }

    public static Root_meta_pure_persistence_metamodel_persister_sink_Sink buildSink(Sink sink, CompileContext context)
    {
        return sink.accept(new SinkBuilder(context));
    }

    public static RichIterable<? extends Test> buildTests(Persistence persistence, CompileContext context)
    {
        if (persistence.tests == null)
        {
            return Lists.immutable.empty();
        }

        List<String> testIds = ListIterate.collect(persistence.tests, test -> test.id);
        List<String> duplicateTestIds = testIds.stream().filter(e -> Collections.frequency(testIds, e) > 1).distinct().collect(Collectors.toList());

        if (!duplicateTestIds.isEmpty())
        {
            throw new EngineException("Multiple persistenceTest found with ids : '" + String.join(",", duplicateTestIds) + "'", persistence.sourceInformation, EngineErrorType.COMPILATION);
        }

        return ListIterate.collect(persistence.tests, test -> buildPersistenceTest(test, persistence, context));
    }

    private static Root_meta_pure_persistence_metamodel_PersistenceTest buildPersistenceTest(PersistenceTest test, Persistence persistence, CompileContext context)
    {
        Root_meta_pure_persistence_metamodel_PersistenceTest purePersistenceTest = new Root_meta_pure_persistence_metamodel_PersistenceTest_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::PersistenceTest"));
        purePersistenceTest._id(test.id);
        purePersistenceTest._isTestDataFromServiceOutput(test.isTestDataFromServiceOutput);

        if (test.testBatches != null)
        {
            List<String> testBatchIds = ListIterate.collect(test.testBatches, testBatch -> testBatch.id);
            List<String> duplicateTestBatchIds = testBatchIds.stream().filter(e -> Collections.frequency(testBatchIds, e) > 1).distinct().collect(Collectors.toList());
            if (!duplicateTestBatchIds.isEmpty())
            {
                throw new EngineException("Multiple testBatches found with ids : '" + String.join(",", duplicateTestBatchIds) + "'", test.sourceInformation, EngineErrorType.COMPILATION);
            }
            purePersistenceTest._testBatches(ListIterate.collect(test.testBatches, testBatch ->
            {
                Root_meta_pure_persistence_metamodel_PersistenceTestBatch pureTestBatch = new Root_meta_pure_persistence_metamodel_PersistenceTestBatch_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::PersistenceTestBatch"));
                pureTestBatch._id(testBatch.id);
                pureTestBatch._batchId(test.testBatches.indexOf(testBatch));
                pureTestBatch._testData(HelperPersistenceBuilder.processPersistenceTestBatchData(testBatch.testData, context, new ProcessingContext("Persistence '" + context.pureModel.buildPackageString(persistence._package, persistence.name) + "' First Pass")));
                pureTestBatch._assertions(ListIterate.collect(testBatch.assertions, assertion -> assertion.accept(new TestAssertionFirstPassBuilder(context, new ProcessingContext("Persistence '" + context.pureModel.buildPackageString(persistence._package, persistence.name) + "' First Pass")))));
                return pureTestBatch;
            }));
        }
        return purePersistenceTest;
    }

    public static Database buildDatabase(String database, SourceInformation sourceInformation, CompileContext context)
    {
        String databasePath = database.substring(0, database.lastIndexOf("::"));
        String databaseName = database.substring(database.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(databasePath)._children().detect(c -> databaseName.equals(c._name()));
        if (packageableElement instanceof Database)
        {
            return (Database) packageableElement;
        }

        throw new EngineException(String.format("Database '%s' is not defined", database), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_external_shared_format_binding_Binding buildBinding(String binding, SourceInformation sourceInformation, CompileContext context)
    {
        String bindingPath = binding.substring(0, binding.lastIndexOf("::"));
        String bindingName = binding.substring(binding.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(bindingPath)._children().detect(c -> bindingName.equals(c._name()));
        if (packageableElement instanceof Root_meta_external_shared_format_binding_Binding)
        {
            return (Root_meta_external_shared_format_binding_Binding) packageableElement;
        }

        throw new EngineException(String.format("Binding '%s' is not defined", binding), sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape buildTargetShape(TargetShape targetShape, Map<String, Class<?>> modelClassByProperty, CompileContext context)
    {
        return targetShape.accept(new TargetShapeBuilder(modelClassByProperty, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy buildDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, Class<?> inputClass, CompileContext context)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode buildIngestMode(IngestMode ingestMode, Collection<Class<?>> modelClasses, CompileContext context)
    {
        return ingestMode.accept(new IngestModeBuilder(modelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_audit_Auditing buildAuditing(Auditing auditing)
    {
        return auditing.accept(AUDITING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning buildTransactionMilestoning(TransactionMilestoning transactionMilestoning, Collection<Class<?>> modelClasses, CompileContext context)
    {
        return transactionMilestoning.accept(new TransactionMilestoningBuilder(modelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation buildTransactionDerivation(TransactionDerivation transactionDerivation, Collection<Class<?>> modelClasses, CompileContext context)
    {
        return transactionDerivation.accept(new TransactionDerivationBuilder(modelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning buildValidityMilestoning(ValidityMilestoning validityMilestoning, Collection<Class<?>> modelClasses, CompileContext context)
    {
        return validityMilestoning.accept(new ValidityMilestoningBuilder(modelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation buildValidityDerivation(ValidityDerivation validityDerivation, Collection<Class<?>> modelClasses, CompileContext context)
    {
        return validityDerivation.accept(new ValidityDerivationBuilder(modelClasses, context));
    }

    // helper methods

    private static Property<?, ?> validateAndResolveProperty(Class<?> modelClass, String propertyName, SourceInformation sourceInformation, CompileContext context)
    {
        Property<?, ?> property = modelClass._properties().detect(p -> p._name().equals(propertyName));
        Assert.assertTrue(property != null, () -> String.format("Property '%s' must exist in class '%s'", propertyName, determineFullPath(modelClass)), sourceInformation, EngineErrorType.COMPILATION);
        return property;
    }

    private static String validateAndResolvePropertyName(Class<?> modelClass, String propertyName, SourceInformation sourceInformation, CompileContext context)
    {
        return validateAndResolveProperty(modelClass, propertyName, sourceInformation, context)._name();
    }

    private static String determineFullPath(Type type)
    {
        Deque<String> deque = new ArrayDeque<>();
        Package currentPackage = ((PackageableElement) type)._package();
        while (!currentPackage._name().equals("Root"))
        {
            deque.push(currentPackage._name());
            currentPackage = currentPackage._package();
        }

        return Iterate.makeString(deque, "", "::", "::" + type._name());
    }

    // helper visitors for class hierarchies

    private static class PersisterBuilder implements PersisterVisitor<Root_meta_pure_persistence_metamodel_persister_Persister>
    {
        private final CompileContext context;

        private PersisterBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_Persister visit(BatchPersister val)
        {
            Map<String, Class<?>> modelClassByProperty = val.targetShape.accept(new ModelClassByPropertyExtractor(context));
            Collection<Class<?>> modelClasses = modelClassByProperty.values();

            return new Root_meta_pure_persistence_metamodel_persister_BatchPersister_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::BatchPersister"))
                    ._sink(buildSink(val.sink, context))
                    ._ingestMode(buildIngestMode(val.ingestMode, modelClasses, context))
                    ._targetShape(buildTargetShape(val.targetShape, modelClassByProperty, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_Persister visit(StreamingPersister val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_StreamingPersister_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::StreamingPersister"))
                    ._sink(buildSink(val.sink, context));
        }
    }

    private static class NotifyeeBuilder implements NotifyeeVisitor<Root_meta_pure_persistence_metamodel_notifier_Notifyee>
    {
        private final CompileContext context;

        private NotifyeeBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_notifier_Notifyee visit(EmailNotifyee val)
        {
            return new Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::notifier::EmailNotifyee"))
                    ._emailAddress(val.address);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_notifier_Notifyee visit(PagerDutyNotifyee val)
        {
            return new Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::notifier::PagerDutyNotifyee"))
                    ._url(val.url);
        }
    }

    public static Root_meta_pure_persistence_metamodel_TestData processPersistenceTestBatchData(TestData testData, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_pure_persistence_metamodel_TestData pureTestBatchData = new Root_meta_pure_persistence_metamodel_TestData_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::TestData"));

        if (testData.connection != null)
        {
            pureTestBatchData._connection(HelperPersistenceBuilder.processPersistenceConnectionData(testData.connection, context, processingContext));
        }

        return pureTestBatchData;
    }

    private static Root_meta_pure_persistence_metamodel_ConnectionTestData processPersistenceConnectionData(ConnectionTestData connectionData, CompileContext context, ProcessingContext processingContext)
    {
        Root_meta_pure_persistence_metamodel_ConnectionTestData pureConnectionData = new Root_meta_pure_persistence_metamodel_ConnectionTestData_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::ConnectionTestData"));

        pureConnectionData._data(connectionData.data.accept(new EmbeddedDataFirstPassBuilder(context, processingContext)));

        return pureConnectionData;
    }

    private static class SinkBuilder implements SinkVisitor<Root_meta_pure_persistence_metamodel_persister_sink_Sink>
    {
        private final CompileContext context;

        private SinkBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_sink_Sink visit(RelationalSink val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_sink_RelationalSink_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::sink::RelationalSink"))
                    ._database(buildDatabase(val.database, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_sink_Sink visit(ObjectStorageSink val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_sink_ObjectStorageSink_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::sink::ObjectStorageSink"))
                    ._binding(buildBinding(val.binding, val.sourceInformation, context));
        }
    }

    private static class ModelClassByPropertyExtractor implements TargetShapeVisitor<Map<String, Class<?>>>
    {
        private final CompileContext context;

        private ModelClassByPropertyExtractor(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Map<String, Class<?>> visit(FlatTarget val)
        {
            return Maps.fixedSize.of(null, context.resolveClass(val.modelClass));
        }

        @Override
        public Map<String, Class<?>> visit(MultiFlatTarget val)
        {
            return Iterate.injectInto(Maps.mutable.<String, Class<?>>of(), val.parts, (map, p) ->
            {
                AbstractProperty<?> pureModelProperty = context.resolveProperty(val.modelClass, p.modelProperty);
                Type leafType = pureModelProperty._genericType()._rawType();
                Assert.assertTrue(leafType instanceof Class, () -> String.format("Target shape modelProperty '%s' must refer to a class.", p.modelProperty), p.sourceInformation, EngineErrorType.COMPILATION);

                Class<?> modelClass = context.resolveClass(determineFullPath(leafType));
                map.put(p.modelProperty, modelClass);
                return map;
            }).asUnmodifiable();
        }
    }

    private static class TargetShapeBuilder implements TargetShapeVisitor<Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape>
    {
        Map<String, Class<?>> modelClassByProperty;
        private final CompileContext context;

        private TargetShapeBuilder(Map<String, Class<?>> modelClassByProperty, CompileContext context)
        {
            this.modelClassByProperty = modelClassByProperty;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape visit(FlatTarget val)
        {
            Class<?> modelClass = context.resolveClass(val.modelClass, val.sourceInformation);
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::targetshape::FlatTarget"))
                    ._targetName(val.targetName)
                    ._modelClass(modelClass)
                    ._partitionFields(ListIterate.collect(val.partitionFields, p -> validateAndResolvePropertyName(modelClass, p, val.sourceInformation, context)))
                    ._deduplicationStrategy(buildDeduplicationStrategy(val.deduplicationStrategy, modelClass, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape visit(MultiFlatTarget val)
        {
            Class<?> modelClass = context.resolveClass(val.modelClass, val.sourceInformation);
            String TRANSACTION_SCOPE_FULL_PATH = PERSISTENCE_PACKAGE_PREFIX + "::persister::targetshape::TransactionScope";
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::targetshape::MultiFlatTarget"))
                    ._modelClass(modelClass)
                    ._transactionScope(context.resolveEnumValue(TRANSACTION_SCOPE_FULL_PATH, val.transactionScope.name()))
                    ._parts(ListIterate.collect(val.parts, p -> resolvePart(p, modelClass, context)));
        }

        private Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart resolvePart(MultiFlatTargetPart part, Class<?> modelClass, CompileContext context)
        {
            Property<?, ?> property = validateAndResolveProperty(modelClass, part.modelProperty, part.sourceInformation, context);
            Class<?> nestedModelClass = modelClassByProperty.get(property._name());

            return new Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::targetshape::MultiFlatTargetPart"))
                    ._modelProperty(property)
                    ._targetName(part.targetName)
                    ._partitionFields(ListIterate.collect(part.partitionFields, p -> validateAndResolvePropertyName(nestedModelClass, p, part.sourceInformation, context)))
                    ._deduplicationStrategy(buildDeduplicationStrategy(part.deduplicationStrategy, nestedModelClass, context));
        }
    }

    private static class DeduplicationStrategyBuilder implements DeduplicationStrategyVisitor<Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private DeduplicationStrategyBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(NoDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_NoDeduplicationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::deduplication::NoDeduplicationStrategy"));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(AnyVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_AnyVersionDeduplicationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::deduplication::AnyVersionDeduplicationStrategy"));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(MaxVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::deduplication::MaxVersionDeduplicationStrategy"))
                    ._versionField(validateAndResolvePropertyName(modelClass, val.versionField, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(DuplicateCountDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_DuplicateCountDeduplicationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::deduplication::DuplicateCountDeduplicationStrategy"))
                    ._duplicateCountName(val.duplicateCountName);
        }
    }

    private static class IngestModeBuilder implements IngestModeVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private IngestModeBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_NontemporalSnapshot_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::snapshot::NontemporalSnapshot"))
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_UnitemporalSnapshot_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::snapshot::UnitemporalSnapshot"))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning, modelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_BitemporalSnapshot_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::snapshot::BitemporalSnapshot"))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning, modelClasses, context))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning, modelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_NontemporalDelta_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::delta::NontemporalDelta"))
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClasses, context))
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::delta::UnitemporalDelta"))
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClasses, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning, modelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::delta::BitemporalDelta"))
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClasses, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning, modelClasses, context))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning, modelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(AppendOnly val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_appendonly_AppendOnly_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::appendonly::AppendOnly"))
                    ._auditing(buildAuditing(val.auditing))
                    ._filterDuplicates(val.filterDuplicates);
        }

        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy buildMergeStrategy(MergeStrategy mergeStrategy, Collection<Class<?>> modelClasses, CompileContext context)
        {
            return mergeStrategy.accept(new MergeStrategyBuilder(modelClasses, context));
        }
    }

    private static class MergeStrategyBuilder implements MergeStrategyVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private MergeStrategyBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(DeleteIndicatorMergeStrategy val)
        {
            String deleteProperty = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.deleteField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::delta::merge::DeleteIndicatorMergeStrategy"))
                    ._deleteField(deleteProperty)
                    ._deleteValues(Lists.immutable.ofAll(val.deleteValues));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_NoDeletesMergeStrategy_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::ingestmode::delta::merge::NoDeletesMergeStrategy"));
        }
    }

    private static class AuditingBuilder implements AuditingVisitor<Root_meta_pure_persistence_metamodel_persister_audit_Auditing>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_persister_audit_Auditing visit(DateTimeAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_audit_DateTimeAuditing_Impl("")
                    ._dateTimeName(val.dateTimeName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_audit_Auditing visit(NoAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_audit_NoAuditing_Impl("");
        }
    }

    private static class TransactionMilestoningBuilder implements TransactionMilestoningVisitor<Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private TransactionMilestoningBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::transactionmilestoning::BatchIdAndDateTimeTransactionMilestoning"))
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName)
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName)
                    ._derivation(val.derivation == null ? null : buildTransactionDerivation(val.derivation, modelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdTransactionMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::transactionmilestoning::BatchIdTransactionMilestoning"))
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::transactionmilestoning::DateTimeTransactionMilestoning"))
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName)
                    ._derivation(val.derivation == null ? null : buildTransactionDerivation(val.derivation, modelClasses, context));
        }
    }

    private static class TransactionDerivationBuilder implements TransactionDerivationVisitor<Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private TransactionDerivationBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation visit(SourceSpecifiesInAndOutDateTime val)
        {
            String sourceDateTimeInField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeInField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            String sourceDateTimeOutField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeOutField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInAndOutDate_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::transactionmilestoning::derivation::SourceSpecifiesTransactionInAndOutDate"))
                    ._sourceDateTimeInField(sourceDateTimeInField)
                    ._sourceDateTimeOutField(sourceDateTimeOutField);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_TransactionDerivation visit(SourceSpecifiesInDateTime val)
        {
            String sourceDateTimeInField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeInField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_derivation_SourceSpecifiesTransactionInDate_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::transactionmilestoning::derivation::SourceSpecifiesTransactionInDate"))
                    ._sourceDateTimeInField(sourceDateTimeInField);
        }
    }

    private static class ValidityMilestoningBuilder implements ValidityMilestoningVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private ValidityMilestoningBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::validitymilestoning::DateTimeValidityMilestoning"))
                    ._dateTimeFromName(val.dateTimeFromName)
                    ._dateTimeThruName(val.dateTimeThruName)
                    ._derivation(buildValidityDerivation(val.derivation, modelClasses, context));
        }
    }

    private static class ValidityDerivationBuilder implements ValidityDerivationVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation>
    {
        private final Collection<Class<?>> modelClasses;
        private final CompileContext context;

        private ValidityDerivationBuilder(Collection<Class<?>> modelClasses, CompileContext context)
        {
            this.modelClasses = modelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromAndThruDateTime val)
        {
            String sourceDateTimeFromField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeFromField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            String sourceDateTimeThruField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeThruField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::validitymilestoning::derivation::SourceSpecifiesValidFromAndThruDate"))
                    ._sourceDateTimeFromField(sourceDateTimeFromField)
                    ._sourceDateTimeThruField(sourceDateTimeThruField);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromDateTime val)
        {
            String sourceDateTimeFromField = Lists.immutable.ofAll(modelClasses)
                    .collect(c -> validateAndResolvePropertyName(c, val.sourceDateTimeFromField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromDate_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::persister::validitymilestoning::derivation::SourceSpecifiesValidFromDate"))
                    ._sourceDateTimeFromField(sourceDateTimeFromField);
        }
    }
}