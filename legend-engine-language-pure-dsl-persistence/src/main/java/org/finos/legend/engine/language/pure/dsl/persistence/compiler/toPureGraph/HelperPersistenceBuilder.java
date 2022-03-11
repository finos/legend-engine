package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.PersisterVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ReaderVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.targetshape.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.OpaqueTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.TriggerVisitor;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

public class HelperPersistenceBuilder
{
    private static final String PERSISTENCE_PACKAGE_PREFIX = "meta::pure::persistence::metamodel";

    private static final TriggerBuilder TRIGGER_BUILDER = new TriggerBuilder();
    private static final AuditingBuilder AUDITING_BUILDER = new AuditingBuilder();
    private static final TransactionMilestoningBuilder TRANSACTION_MILESTONING_BUILDER = new TransactionMilestoningBuilder();
    private static final ValidityMilestoningBuilder VALIDITY_MILESTONING_BUILDER = new ValidityMilestoningBuilder();

    private HelperPersistenceBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_trigger_Trigger buildTrigger(Trigger trigger)
    {
        return trigger.accept(TRIGGER_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_reader_Reader buildReader(Reader reader, CompileContext context)
    {
        return reader.accept(new ReaderBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_Persister buildPersister(Persister persister, CompileContext context)
    {
        return persister.accept(new PersisterBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape buildTargetShape(TargetShape targetShape, CompileContext context)
    {
        return targetShape.accept(new TargetShapeBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy buildDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, Class<?> inputClass, CompileContext context)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode buildMilestoningMode(IngestMode milestoningMode, Class<?> inputClass, CompileContext context)
    {
        return milestoningMode.accept(new IngestModeBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy buildMergeStrategy(MergeStrategy mergeStrategy, Class<?> inputClass, CompileContext context)
    {
        return mergeStrategy.accept(new MergeStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_audit_Auditing buildAuditing(Auditing auditing)
    {
        return auditing.accept(AUDITING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning buildTransactionMilestoning(TransactionMilestoning transactionMilestoning)
    {
        return transactionMilestoning.accept(TRANSACTION_MILESTONING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning buildValidityMilestoning(ValidityMilestoning validityMilestoning)
    {
        return validityMilestoning.accept(VALIDITY_MILESTONING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation buildValidityDerivation(ValidityDerivation validityDerivation, Class<?> modelClass, CompileContext context)
    {
        return validityDerivation.accept(new ValidityDerivationBuilder(modelClass, context));
    }

    // helper methods

    private static Property<?, ?> validateAndResolveProperty(Class<?> modelClass, String propertyName, SourceInformation sourceInformation, CompileContext context)
    {
        Property<?, ?> property = modelClass._properties().detect(p -> p._name().equals(propertyName));
        Assert.assertTrue(property != null, () -> String.format("Property '%s' must exist in class '%s::%s'", propertyName, modelClass._package(), modelClass._name()), sourceInformation, EngineErrorType.COMPILATION);
        return property;
    }

    // helper visitors for class hierarchies

    private static class TriggerBuilder implements TriggerVisitor<Root_meta_pure_persistence_metamodel_trigger_Trigger>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_trigger_Trigger visit(ManualTrigger val)
        {
            return new Root_meta_pure_persistence_metamodel_trigger_ManualTrigger_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_trigger_Trigger visit(OpaqueTrigger val)
        {
            return new Root_meta_pure_persistence_metamodel_trigger_OpaqueTrigger_Impl("");
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
            String servicePath = val.service.substring(0, val.service.lastIndexOf("::"));
            String serviceName = val.service.substring(val.service.lastIndexOf("::") + 2);
            PackageableElement packageableElement = context.pureModel.getOrCreatePackage(servicePath)._children().detect(c -> serviceName.equals(c._name()));

            if (packageableElement instanceof Root_meta_legend_service_metamodel_Service)
            {
                return new Root_meta_pure_persistence_metamodel_reader_ServiceReader_Impl("")
                        ._service((Root_meta_legend_service_metamodel_Service) packageableElement);
            }
            throw new EngineException(String.format("Persistence refers to a service '%s' that is not defined", val.service), val.sourceInformation, EngineErrorType.COMPILATION);
        }
    }

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
            return new Root_meta_pure_persistence_metamodel_persister_BatchPersister_Impl("")
                    ._targetShape(buildTargetShape(val.targetShape, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_Persister visit(StreamingPersister val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_StreamingPersister_Impl("");
        }
    }

    private static class TargetShapeBuilder implements TargetShapeVisitor<Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape>
    {
        private final CompileContext context;

        private TargetShapeBuilder(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape visit(FlatTarget val)
        {
            Class<?> modelClass = context.resolveClass(val.modelClass, val.sourceInformation);
            return buildFlatTarget(val, modelClass, context);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape visit(MultiFlatTarget val)
        {
            Class<?> modelClass = context.resolveClass(val.modelClass, val.sourceInformation);
            String TRANSACTION_SCOPE_FULL_PATH = PERSISTENCE_PACKAGE_PREFIX + "::persister::targetshape::TransactionScope";
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget_Impl("")
                    ._modelClass(modelClass)
                    ._transactionScope(context.resolveEnumValue(TRANSACTION_SCOPE_FULL_PATH, val.transactionScope.name()))
                    ._parts(ListIterate.collect(val.parts, p -> resolvePart(p, modelClass, context)));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape visit(OpaqueTarget val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_OpaqueTarget_Impl("")
                    ._targetName(val.targetName);
        }

        private Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget buildFlatTarget(FlatTarget flatTarget, Class<?> modelClass, CompileContext context)
        {
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget_Impl("")
                    ._targetName(flatTarget.targetName)
                    ._modelClass(modelClass)
                    ._partitionProperties(ListIterate.collect(flatTarget.partitionProperties, p -> validateAndResolveProperty(modelClass, p, flatTarget.sourceInformation, context)))
                    ._deduplicationStrategy(buildDeduplicationStrategy(flatTarget.deduplicationStrategy, modelClass, context))
                    ._ingestMode(buildMilestoningMode(flatTarget.ingestMode, modelClass, context));
        }

        private Root_meta_pure_persistence_metamodel_persister_targetshape_PropertyAndFlatTarget resolvePart(PropertyAndFlatTarget propertyAndFlatTarget, Class<?> modelClass, CompileContext context)
        {
            Property<?, ?> property = validateAndResolveProperty(modelClass, propertyAndFlatTarget.property, propertyAndFlatTarget.sourceInformation, context);
            Type targetType = property._genericType()._rawType();
            Assert.assertTrue(targetType instanceof Class, () -> String.format("Target part property must refer to a Class. The property '%s' refers to a %s", propertyAndFlatTarget.property, targetType._name()), propertyAndFlatTarget.sourceInformation, EngineErrorType.COMPILATION);

            return new Root_meta_pure_persistence_metamodel_persister_targetshape_PropertyAndFlatTarget_Impl("")
                    ._property(property)
                    ._flatTarget(buildFlatTarget(propertyAndFlatTarget.flatTarget, (Class<?>) targetType, context));
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
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(AnyVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_AnyVersionDeduplicationStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(MaxVersionDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_MaxVersionDeduplicationStrategy_Impl("")
                    ._versionProperty(validateAndResolveProperty(modelClass, val.versionProperty, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(NoDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_NoDeduplicationStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(OpaqueDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_OpaqueDeduplicationStrategy_Impl("");
        }
    }

    private static class IngestModeBuilder implements IngestModeVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private IngestModeBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(AppendOnly val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_appendonly_AppendOnly_Impl("")
                    ._auditing(buildAuditing(val.auditing))
                    ._filterDuplicates(val.filterDuplicates);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClass, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_BitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_NontemporalDelta_Impl("")
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_NontemporalSnapshot_Impl("")
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, modelClass, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_UnitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }
    }

    private static class MergeStrategyBuilder implements MergeStrategyVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private MergeStrategyBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(DeleteIndicatorMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy_Impl("")
                    ._deleteProperty(validateAndResolveProperty(modelClass, val.deleteProperty, val.sourceInformation, context))
                    ._deleteValues(Lists.immutable.ofAll(val.deleteValues));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_NoDeletesMergeStrategy_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(OpaqueMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_OpaqueMergeStrategy_Impl("");
        }
    }

    private static class AuditingBuilder implements AuditingVisitor<Root_meta_pure_persistence_metamodel_persister_audit_Auditing>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_persister_audit_Auditing visit(DateTimeAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_audit_DateTimeAuditing_Impl("")
                    ._dateTimePropertyName(val.dateTimeFieldName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_audit_Auditing visit(NoAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_audit_NoAuditing_Impl("");
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_audit_Auditing visit(OpaqueAuditing val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_audit_OpaqueAuditing_Impl("");
        }
    }

    private static class TransactionMilestoningBuilder implements TransactionMilestoningVisitor<Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInFieldName)
                    ._batchIdOutName(val.batchIdOutFieldName)
                    ._dateTimeInName(val.dateTimeInFieldName)
                    ._dateTimeOutName(val.dateTimeOutFieldName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInFieldName)
                    ._batchIdOutName(val.batchIdOutFieldName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning_Impl("")
                    ._dateTimeInName(val.dateTimeInFieldName)
                    ._dateTimeOutName(val.dateTimeOutFieldName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(OpaqueTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_OpaqueTransactionMilestoning_Impl("");
        }
    }

    private static class ValidityMilestoningBuilder implements ValidityMilestoningVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning>
    {
        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning_Impl("")
                    ._dateTimeFromName(val.dateTimeFromFieldName)
                    ._dateTimeThruName(val.dateTimeThruFieldName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning visit(OpaqueValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_OpaqueValidityMilestoning_Impl("");
        }
    }

    private static class ValidityDerivationBuilder implements ValidityDerivationVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation>
    {
        private final Class<?> modelClass;
        private final CompileContext context;

        private ValidityDerivationBuilder(Class<?> modelClass, CompileContext context)
        {
            this.modelClass = modelClass;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromAndThruDateTime val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate_Impl("")
                    ._sourceDateTimeFromProperty(validateAndResolveProperty(modelClass, val.sourceDateTimeFromProperty, val.sourceInformation, context))
                    ._sourceDateTimeThruProperty(validateAndResolveProperty(modelClass, val.sourceDateTimeThruProperty, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromDateTime val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromDate_Impl("")
                    ._sourceDateTimeFromProperty(validateAndResolveProperty(modelClass, val.sourceDateTimeFromProperty, val.sourceInformation, context));
        }
    }
}
