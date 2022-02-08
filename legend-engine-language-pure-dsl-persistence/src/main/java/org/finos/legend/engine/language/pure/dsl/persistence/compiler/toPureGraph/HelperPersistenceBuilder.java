package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromAndThruDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.EventTypeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.OpaqueEventType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.event.ScheduleFired;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ReaderVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersister;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;

public class HelperPersistenceBuilder
{
    private static final String PERSIST_PACKAGE_PREFIX = "meta::pure::persistence::metamodel";
    private static final EventTypeBuilder EVENT_TYPE_BUILDER = new EventTypeBuilder();
    private static final AuditingBuilder AUDITING_BUILDER = new AuditingBuilder();
    private static final TransactionMilestoningBuilder TRANSACTION_MILESTONING_BUILDER = new TransactionMilestoningBuilder();
    private static final ValidityMilestoningBuilder VALIDITY_MILESTONING_BUILDER = new ValidityMilestoningBuilder();

    private HelperPersistenceBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_event_EventType buildEventType(EventType eventType)
    {
        return eventType.accept(EVENT_TYPE_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_reader_Reader buildReader(Reader reader, CompileContext context)
    {
        return reader.accept(new ReaderBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_Persister buildPersister(Persister persister, CompileContext context)
    {
        return persister.accept(new PersisterBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_batch_targetspecification_TargetSpecification buildTargetSpecification(TargetSpecification specification, CompileContext context)
    {
        String targetName = specification.targetName;
        Class<?> modelClass = context.resolveClass(specification.modelClassPath);

        return specification.accept(new TargetSpecificationBuilder(targetName, modelClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy buildDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, Class<?> inputClass, CompileContext context)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode buildMilestoningMode(BatchMilestoningMode milestoningMode, Class<?> inputClass, CompileContext context)
    {
        return milestoningMode.accept(new BatchMilestoningModeBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_MergeStrategy buildMergeStrategy(MergeStrategy mergeStrategy, Class<?> inputClass, CompileContext context)
    {
        return mergeStrategy.accept(new MergeStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_batch_audit_Auditing buildAuditing(Auditing auditing)
    {
        return auditing.accept(AUDITING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning buildTransactionMilestoning(TransactionMilestoning transactionMilestoning)
    {
        return transactionMilestoning.accept(TRANSACTION_MILESTONING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_batch_validitymilestoning_ValidityMilestoning buildValidityMilestoning(ValidityMilestoning validityMilestoning)
    {
        return validityMilestoning.accept(VALIDITY_MILESTONING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_ValidityDerivation buildValidityDerivation(ValidityDerivation validityDerivation, Class<?> modelClass, CompileContext context)
    {
        return validityDerivation.accept(new ValidityDerivationBuilder(modelClass, context));
    }

    // helper methods

    private static Property<?, ?> resolveModelClassProperty(String propertyName, Class<?> inputClass, CompileContext context)
    {
        return inputClass._properties().detect(p -> p._name().equals(propertyName));
    }

    // helper visitors for class hierarchies

    private static class EventTypeBuilder implements EventTypeVisitor<Root_meta_pure_persistence_metamodel_event_EventType>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_event_EventType visit(ScheduleFired val)
        {
            return new Root_meta_pure_persistence_metamodel_event_ScheduleFired_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_event_EventType visit(OpaqueEventType val)
        {
            return new Root_meta_pure_persistence_metamodel_event_EventType_Impl("");
        }
    }

    private static class ReaderBuilder implements ReaderVisitor<Root_meta_pure_persistence_metamodel_reader_Reader>
    {
        private final CompileContext context;

        private ReaderBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_reader_Reader visit(ServiceReader val)
        {
            //TODO: ledav -- resolve service
            return new Root_meta_pure_persistence_metamodel_reader_ServiceReader_Impl("");
//                    ._service(context.pureModel.);
        }
    }

    private static class PersisterBuilder implements PersistenceVisitor<Root_meta_pure_persistence_metamodel_Persister>
    {
        private final CompileContext context;

        private PersisterBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_Persister visit(BatchPersister val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_BatchPersister_Impl("")
                    ._targetSpecification(buildTargetSpecification(val.targetSpecification, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_Persister visit(StreamingPersister val)
        {
            return new Root_meta_pure_persistence_metamodel_streaming_StreamingPersister_Impl("");
        }
    }

    private static class TargetSpecificationBuilder implements TargetSpecificationVisitor<Root_meta_pure_persistence_metamodel_batch_targetspecification_TargetSpecification>
    {
        private final String targetName;
        private final Class<?> modelClass;
        private final CompileContext context;

        private TargetSpecificationBuilder(String targetName, Class<?> modelClass, CompileContext context)
        {
            this.targetName = targetName;
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_targetspecification_TargetSpecification visit(FlatTargetSpecification val)
        {
            return buildFlatTargetSpecification(val);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_targetspecification_TargetSpecification visit(GroupedFlatTargetSpecification val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_targetspecification_GroupedFlatTargetSpecification_Impl("")
                    ._modelClass(modelClass)
                    ._transactionScope(context.resolveEnumValue(PERSIST_PACKAGE_PREFIX + "::batch::targetspecification::TransactionScope", val.transactionScope.name()))
                    ._components(ListIterate.collect(val.components, c -> resolveComponent(c, modelClass, context)));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_targetspecification_TargetSpecification visit(NestedTargetSpecification val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_targetspecification_NestedTargetSpecification_Impl("")
                    ._targetName(targetName)
                    ._modelClass(modelClass);
        }

        private Root_meta_pure_persistence_metamodel_batch_targetspecification_FlatTargetSpecification buildFlatTargetSpecification(FlatTargetSpecification specification)
        {
            return new Root_meta_pure_persistence_metamodel_batch_targetspecification_FlatTargetSpecification_Impl("")
                    ._targetName(targetName)
                    ._modelClass(modelClass)
                    ._partitionProperties(ListIterate.collect(specification.partitionPropertyPaths, p -> resolveModelClassProperty(p, modelClass, context)))
                    ._deduplicationStrategy(buildDeduplicationStrategy(specification.deduplicationStrategy, modelClass, context))
                    ._batchMilestoningMode(buildMilestoningMode(specification.milestoningMode, modelClass, context));
        }

        private Root_meta_pure_persistence_metamodel_batch_targetspecification_PropertyAndFlatTargetSpecification resolveComponent(PropertyAndFlatTargetSpecification specification, Class<?> modelClass, CompileContext context)
        {
            return new Root_meta_pure_persistence_metamodel_batch_targetspecification_PropertyAndFlatTargetSpecification_Impl("")
                    ._property(resolveModelClassProperty(specification.propertyPath, modelClass, context))
                    ._targetSpecification(buildFlatTargetSpecification(specification.targetSpecification));
        }
    }

    private static class DeduplicationStrategyBuilder implements DeduplicationStrategyVisitor<Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private DeduplicationStrategyBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy visit(AnyVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_deduplication_AnyVersionDeduplicationStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy visit(MaxVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_deduplication_MaxVersionDeduplicationStrategy_Impl("")
                    ._versionProperty(resolveModelClassProperty(val.versionProperty, modelClass, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy visit(NoDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_deduplication_NoDeduplicationStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_deduplication_DeduplicationStrategy visit(OpaqueDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_deduplication_OpaqueDeduplicationStrategy_Impl("");
        }
    }

    private static class BatchMilestoningModeBuilder implements BatchMilestoningModeVisitor<Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private BatchMilestoningModeBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(AppendOnly val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_appendonly_AppendOnly_Impl("")
                    ._auditing(buildAuditing(val.auditing))
                    ._filterDuplicates(val.filterDuplicates);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(BitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_BitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClass, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning))
                    ._validityDerivation(buildValidityDerivation(val.validityDerivation, modelClass, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(BitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_snapshot_BitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning))
                    ._validityDerivation(buildValidityDerivation(val.validityDerivation, modelClass, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(NonMilestonedDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_NonMilestonedDelta_Impl("")
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(NonMilestonedSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_snapshot_NonMilestonedSnapshot_Impl("")
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(UnitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_UnitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClass, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_BatchMilestoningMode visit(UnitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_snapshot_UnitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }
    }

    private static class MergeStrategyBuilder implements MergeStrategyVisitor<Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_MergeStrategy>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private MergeStrategyBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_MergeStrategy visit(DeleteIndicatorMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_DeleteIndicatorMergeStrategy_Impl("")
                    ._deleteProperty(resolveModelClassProperty(val.deleteProperty, modelClass, context))
                    ._deleteValues(Lists.immutable.ofAll(val.deleteValues));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_NoDeletesMergeStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_MergeStrategy visit(OpaqueMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_mode_delta_merge_OpaqueMergeStrategy_Impl("");
        }
    }

    private static class AuditingBuilder implements AuditingVisitor<Root_meta_pure_persistence_metamodel_batch_audit_Auditing>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_batch_audit_Auditing visit(BatchDateTimeAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_audit_BatchDateTimeAuditing_Impl("")
                    ._dateTimePropertyName(val.dateTimePropertyName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_audit_Auditing visit(NoAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_audit_NoAuditing_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_audit_Auditing visit(OpaqueAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_audit_OpaqueAuditing_Impl("");
        }
    }

    private static class TransactionMilestoningBuilder implements TransactionMilestoningVisitor<Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName)
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_BatchIdTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_DateTimeTransactionMilestoning_Impl("")
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_TransactionMilestoning visit(OpaqueTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_transactionmilestoning_OpaqueTransactionMilestoning_Impl("");
        }
    }

    private static class ValidityMilestoningBuilder implements ValidityMilestoningVisitor<Root_meta_pure_persistence_metamodel_batch_validitymilestoning_ValidityMilestoning>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_batch_validitymilestoning_ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_validitymilestoning_DateTimeValidityMilestoning_Impl("")
                    ._dateTimeFromName(val.dateTimeFromName)
                    ._dateTimeThruName(val.dateTimeThruName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_validitymilestoning_ValidityMilestoning visit(OpaqueValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_validitymilestoning_OpaqueValidityMilestoning_Impl("");
        }
    }

    private static class ValidityDerivationBuilder implements ValidityDerivationVisitor<Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_ValidityDerivation>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private ValidityDerivationBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromAndThruDate val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate_Impl("")
                    ._sourceDateTimeFromProperty(resolveModelClassProperty(val.sourceDateTimeFromProperty, modelClass, context))
                    ._sourceDateTimeThruProperty(resolveModelClassProperty(val.sourceDateTimeThruProperty, modelClass, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromDate val)
        {
            return new Root_meta_pure_persistence_metamodel_batch_validitymilestoning_derivation_SourceSpecifiesValidFromDate_Impl("")
                    ._sourceDateTimeFromProperty(resolveModelClassProperty(val.sourceDateTimeFromProperty, modelClass, context));
        }
    }
}
