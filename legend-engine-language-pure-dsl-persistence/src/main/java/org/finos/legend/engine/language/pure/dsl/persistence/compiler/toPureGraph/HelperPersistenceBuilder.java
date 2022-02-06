package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeSchemeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesValidFromAndThruDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesValidFromDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventTypeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.RegistryDatasetAvailable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.ScheduleTriggered;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersistence;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;

public class HelperPersistenceBuilder
{
    private static final String PERSIST_PACKAGE_PREFIX = "meta::pure::persist::metamodel";
    private static final EventTypeBuilder EVENT_TYPE_BUILDER = new EventTypeBuilder();
    private static final AuditingBuilder AUDIT_SCHEME_BUILDER = new AuditingBuilder();
    private static final TransactionMilestoningBuilder TRANSACTION_MILESTONING_SCHEME_BUILDER = new TransactionMilestoningBuilder();
    private static final ValidityMilestoningBuilder VALIDITY_MILESTONING_SCHEME_BUILDER = new ValidityMilestoningBuilder();

    private HelperPersistenceBuilder()
    {
    }

    public static Root_meta_pure_persist_metamodel_event_EventType buildEventType(EventType eventType)
    {
        return eventType.accept(EVENT_TYPE_BUILDER);
    }

    public static Root_meta_pure_persist_metamodel_Persistence buildPersistence(Persistence persistence, CompileContext context)
    {
        Enum inputShape = context.resolveEnumValue(PERSIST_PACKAGE_PREFIX + "::DataShape", persistence.inputShape.name());
        Class<?> inputClass = context.resolveClass(persistence.inputClassPath);

        return persistence.accept(new PersistenceBuilder(inputShape, inputClass, context));
    }

    public static Root_meta_pure_persist_metamodel_batch_BatchDatastoreSpecification buildDatastoreSpecification(BatchDatastoreSpecification specification, Class<?> inputClass, CompileContext context)
    {
        return new Root_meta_pure_persist_metamodel_batch_BatchDatastoreSpecification_Impl("")
                ._datastoreName(specification.datastoreName)
                ._datasets(ListIterate.collect(specification.datasets, d -> buildDatasetSpecification(d, inputClass, context)));
    }

    public static Root_meta_pure_persist_metamodel_batch_BatchDatasetSpecification buildDatasetSpecification(BatchDatasetSpecification specification, Class<?> inputClass, CompileContext context)
    {
        return new Root_meta_pure_persist_metamodel_batch_BatchDatasetSpecification_Impl("")
                ._datasetName(specification.datasetName)
                ._partitionProperties(ListIterate.collect(specification.partitionProperties, p -> resolveInputClassProperty(p, inputClass, context)))
                ._deduplicationStrategy(buildDeduplicationStrategy(specification.deduplicationStrategy, inputClass, context))
                ._milestoningMode(buildMilestoningMode(specification.milestoningMode, inputClass, context));
    }

    public static Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy buildDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, Class<?> inputClass, CompileContext context)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode buildMilestoningMode(BatchMilestoningMode milestoningMode, Class<?> inputClass, CompileContext context)
    {
        return milestoningMode.accept(new BatchMilestoningModeBuilder(inputClass, context));
    }

    public static Root_meta_pure_persist_metamodel_batch_audit_AuditScheme buildAuditScheme(Auditing auditing)
    {
        return auditing.accept(AUDIT_SCHEME_BUILDER);
    }

    public static Root_meta_pure_persist_metamodel_batch_mode_delta_merge_MergeScheme buildMergeScheme(MergeStrategy mergeStrategy, Class<?> inputClass, CompileContext context)
    {
        return mergeStrategy.accept(new MergeSchemeBuilder(inputClass, context));
    }

    // helper methods

    private static org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property<?, ?> resolveInputClassProperty(String propertyName, Class<?> inputClass, CompileContext context)
    {
        return inputClass._properties().detect(p -> p._name().equals(propertyName));
    }

    // helper visitors for class hierarchies

    private static class EventTypeBuilder implements EventTypeVisitor<Root_meta_pure_persist_metamodel_event_EventType>
    {
        @Override
        public Root_meta_pure_persist_metamodel_event_EventType visit(RegistryDatasetAvailable val)
        {
            return new Root_meta_pure_persist_metamodel_event_RegistryDatasetAvailable_Impl("");
        }

        @Override
        public Root_meta_pure_persist_metamodel_event_EventType visit(ScheduleTriggered val)
        {
            return new Root_meta_pure_persist_metamodel_event_ScheduleTriggered_Impl("");
        }
    }

    private static class PersistenceBuilder implements PersistenceVisitor<Root_meta_pure_persist_metamodel_Persistence>
    {
        private final Enum inputShape;
        private final Class<?> inputClass;
        private final CompileContext context;

        private PersistenceBuilder(Enum inputShape, Class<?> inputClass, CompileContext context)
        {
            this.inputShape = inputShape;
            this.inputClass = inputClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persist_metamodel_Persistence visit(BatchPersistence val)
        {
            return new Root_meta_pure_persist_metamodel_batch_BatchPersistence_Impl("")
                    ._inputShape(inputShape)
                    ._inputClass(inputClass)
                    ._transactionMode(context.resolveEnumValue(PERSIST_PACKAGE_PREFIX + "::batch::BatchTransactionMode", val.transactionMode.name()))
                    ._targetSpecification(buildDatastoreSpecification(val.targetSpecification, inputClass, context));
        }

        @Override
        public Root_meta_pure_persist_metamodel_Persistence visit(StreamingPersistence val)
        {
            return new Root_meta_pure_persist_metamodel_streaming_StreamingPersistence_Impl("");
        }
    }

    private static class DeduplicationStrategyBuilder implements DeduplicationStrategyVisitor<Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy>
    {
        private final Class<?> inputClass;
        private final CompileContext context;

        private DeduplicationStrategyBuilder(Class<?> inputClass, CompileContext context)
        {
            this.inputClass = inputClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy visit(AnyVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_deduplication_AnyDeduplicationStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy visit(CountDeduplicationStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_deduplication_CountDeduplicationStrategy_Impl("")
                    ._duplicateCountPropertyName(val.duplicateCountPropertyName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy visit(MaxVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_deduplication_MaxVersionDeduplicationStrategy_Impl("")
                    ._versionProperty(resolveInputClassProperty(val.versionProperty, inputClass, context));

        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_deduplication_DeduplicationStrategy visit(NoDeduplicationStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_deduplication_NoDeduplicationStrategy_Impl("");
        }
    }

    private static class BatchMilestoningModeBuilder implements BatchMilestoningModeVisitor<Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode>
    {
        private final Class<?> inputClass;
        private final CompileContext context;

        private BatchMilestoningModeBuilder(Class<?> inputClass, CompileContext context)
        {
            this.inputClass = inputClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(AppendOnly val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_appendonly_AppendOnly_Impl("")
                    ._auditScheme(buildAuditScheme(val.auditing))
                    ._filterDuplicates(val.filterDuplicates);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(BitemporalDelta val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_delta_BitemporalDelta_Impl("")
                    ._mergeScheme(val.mergeStrategy.accept(new MergeSchemeBuilder(inputClass, context)))
                    ._transactionMilestoningScheme(val.transactionMilestoning.accept(TRANSACTION_MILESTONING_SCHEME_BUILDER))
                    ._validityMilestoningScheme(val.validityMilestoning.accept(VALIDITY_MILESTONING_SCHEME_BUILDER))
                    ._validityDerivation(val.validityDerivation.accept(new ValidityDerivationBuilder(inputClass, context)));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(BitemporalSnapshot val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_snapshot_BitemporalSnapshot_Impl("")
                    ._transactionMilestoningScheme(val.transactionMilestoning.accept(TRANSACTION_MILESTONING_SCHEME_BUILDER))
                    ._validityMilestoningScheme(val.validityMilestoning.accept(VALIDITY_MILESTONING_SCHEME_BUILDER))
                    ._validityDerivation(val.validityDerivation.accept(new ValidityDerivationBuilder(inputClass, context)));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(NonMilestonedDelta val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_delta_NonMilestonedDelta_Impl("")
                    ._auditScheme(buildAuditScheme(val.auditing));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(NonMilestonedSnapshot val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_snapshot_NonMilestonedSnapshot_Impl("")
                    ._auditScheme(buildAuditScheme(val.auditing));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(UnitemporalDelta val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_delta_UnitemporalDelta_Impl("")
                    ._mergeScheme(val.mergeStrategy.accept(new MergeSchemeBuilder(inputClass, context)))
                    ._transactionMilestoningScheme(val.transactionMilestoning.accept(TRANSACTION_MILESTONING_SCHEME_BUILDER));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_BatchMilestoningMode visit(UnitemporalSnapshot val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_snapshot_UnitemporalSnapshot_Impl("")
                    ._transactionMilestoningScheme(val.transactionMilestoning.accept(TRANSACTION_MILESTONING_SCHEME_BUILDER));
        }
    }

    private static class MergeSchemeBuilder implements MergeSchemeVisitor<Root_meta_pure_persist_metamodel_batch_mode_delta_merge_MergeScheme>
    {
        private final Class<?> inputClass;
        private final CompileContext context;

        private MergeSchemeBuilder(Class<?> inputClass, CompileContext context)
        {
            this.inputClass = inputClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_delta_merge_MergeScheme visit(DeleteIndicatorMergeStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_delta_merge_DeleteIndicatorMergeScheme_Impl("")
                    ._deleteProperty(resolveInputClassProperty(val.deleteProperty, inputClass, context))
                    ._deleteValues(Lists.immutable.ofAll(val.deleteValues));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_mode_delta_merge_MergeScheme visit(NoDeletesMergeStrategy val)
        {
            return new Root_meta_pure_persist_metamodel_batch_mode_delta_merge_NoDeletesMergeScheme_Impl("");
        }
    }

    private static class AuditingBuilder implements AuditingVisitor<Root_meta_pure_persist_metamodel_batch_audit_AuditScheme>
    {
        @Override
        public Root_meta_pure_persist_metamodel_batch_audit_AuditScheme visit(BatchDateTimeAuditing val)
        {
            return new Root_meta_pure_persist_metamodel_batch_audit_BatchDateTimeAuditScheme_Impl("")
                    ._transactionDateTimePropertyName(val.dateTimePropertyName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_audit_AuditScheme visit(NoAuditing val)
        {
            return new Root_meta_pure_persist_metamodel_batch_audit_NoAuditScheme_Impl("");
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_audit_AuditScheme visit(OpaqueAuditing val)
        {
            return new Root_meta_pure_persist_metamodel_batch_audit_OpaqueAuditScheme_Impl("");
        }
    }

    private static class TransactionMilestoningBuilder implements TransactionMilestoningVisitor<Root_meta_pure_persist_metamodel_batch_transactionmilestoned_TransactionMilestoningScheme>
    {
        @Override
        public Root_meta_pure_persist_metamodel_batch_transactionmilestoned_TransactionMilestoningScheme visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_transactionmilestoned_BatchIdAndDateTimeTransactionMilestoningScheme_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName)
                    ._transactionDateTimeInName(val.dateTimeInName)
                    ._transactionDateTimeOutName(val.dateTimeOutName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_transactionmilestoned_TransactionMilestoningScheme visit(BatchIdTransactionMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_transactionmilestoned_BatchIdTransactionMilestoningScheme_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_transactionmilestoned_TransactionMilestoningScheme visit(DateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_transactionmilestoned_DateTimeTransactionMilestoningScheme_Impl("")
                    ._transactionDateTimeInName(val.dateTimeInName)
                    ._transactionDateTimeOutName(val.dateTimeOutName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_transactionmilestoned_TransactionMilestoningScheme visit(OpaqueTransactionMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_transactionmilestoned_OpaqueTransactionMilestoningScheme_Impl("");
        }
    }

    private static class ValidityMilestoningBuilder implements ValidityMilestoningVisitor<Root_meta_pure_persist_metamodel_batch_validitymilestoned_ValidityMilestoningScheme>
    {
        @Override
        public Root_meta_pure_persist_metamodel_batch_validitymilestoned_ValidityMilestoningScheme visit(DateTimeValidityMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_validitymilestoned_DateTimeValidityMilestoningScheme_Impl("")
                    ._validDateTimeFromName(val.dateTimeFromName)
                    ._validDateTimeThruName(val.dateTimeThruName);
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_validitymilestoned_ValidityMilestoningScheme visit(OpaqueValidityMilestoning val)
        {
            return new Root_meta_pure_persist_metamodel_batch_validitymilestoned_OpaqueValidityMilestoningScheme_Impl("");
        }
    }

    private static class ValidityDerivationBuilder implements ValidityDerivationVisitor<Root_meta_pure_persist_metamodel_batch_validitymilestoned_derivation_ValidityDerivation>
    {
        private final Class<?> inputClass;
        private final CompileContext context;

        private ValidityDerivationBuilder(Class<?> inputClass, CompileContext context)
        {
            this.inputClass = inputClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_validitymilestoned_derivation_ValidityDerivation visit(SourceSpecifiesValidFromAndThruDate val)
        {
            return new Root_meta_pure_persist_metamodel_batch_validitymilestoned_derivation_SourceSpecifiesValidFromAndThruDate_Impl("")
                    ._sourceValidDateTimeFromProperty(resolveInputClassProperty(val.sourceDateTimeFromProperty, inputClass, context))
                    ._sourceValidDateTimeThruProperty(resolveInputClassProperty(val.sourceDateTimeThruProperty, inputClass, context));
        }

        @Override
        public Root_meta_pure_persist_metamodel_batch_validitymilestoned_derivation_ValidityDerivation visit(SourceSpecifiesValidFromDate val)
        {
            return new Root_meta_pure_persist_metamodel_batch_validitymilestoned_derivation_SourceSpecifiesValidFromDate_Impl("")
                    ._sourceValidDateTimeFromProperty(resolveInputClassProperty(val.sourceDateTimeFromProperty, inputClass, context));
        }
    }
}
