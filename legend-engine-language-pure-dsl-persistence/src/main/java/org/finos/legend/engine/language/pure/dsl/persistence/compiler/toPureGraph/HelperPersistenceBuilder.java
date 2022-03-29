package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionSecondPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.*;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.TriggerVisitor;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

import java.util.ArrayDeque;
import java.util.Deque;

public class HelperPersistenceBuilder
{
    private static final String PERSISTENCE_PACKAGE_PREFIX = "meta::pure::persistence::metamodel";

    private static final TriggerBuilder TRIGGER_BUILDER = new TriggerBuilder();
    private static final AuditingBuilder AUDITING_BUILDER = new AuditingBuilder();
    private static final TransactionMilestoningBuilder TRANSACTION_MILESTONING_BUILDER = new TransactionMilestoningBuilder();

    private HelperPersistenceBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_trigger_Trigger buildTrigger(Trigger trigger)
    {
        return trigger.accept(TRIGGER_BUILDER);
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

        throw new EngineException(String.format("Persistence refers to a service '%s' that is not defined", service), persistence.sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_persistence_metamodel_persister_Persister buildPersister(Persister persister, CompileContext context)
    {
        return persister.accept(new PersisterBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_notifier_Notifier buildNotifier(Notifier notifier, CompileContext context)
    {
        return new Root_meta_pure_persistence_metamodel_notifier_Notifier_Impl("")
                ._notifyees(ListIterate.collect(notifier.notifyees, n -> n.acceptVisitor(new NotifyeeBuilder(context))));
    }

    public static Root_meta_external_shared_format_binding_Binding buildBinding(Persister persister, CompileContext context)
    {
        String binding = persister.binding;
        String bindingPath = binding.substring(0, binding.lastIndexOf("::"));
        String bindingName = binding.substring(binding.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(bindingPath)._children().detect(c -> bindingName.equals(c._name()));
        if (packageableElement instanceof Root_meta_external_shared_format_binding_Binding)
        {
            return (Root_meta_external_shared_format_binding_Binding) packageableElement;
        }

        throw new EngineException(String.format("Persister refers to a binding '%s' that is not defined", binding), persister.sourceInformation, EngineErrorType.COMPILATION);
    }

    public static org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection buildConnection(Connection connection, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection pureConnection = connection.accept(new ConnectionFirstPassBuilder(context));
        connection.accept(new ConnectionSecondPassBuilder(context, pureConnection));
        return pureConnection;
    }

    public static Root_meta_pure_persistence_metamodel_persister_targetshape_TargetShape buildTargetShape(TargetShape targetShape, CompileContext context)
    {
        return targetShape.accept(new TargetShapeBuilder(context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy buildDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, Class<?> inputClass, CompileContext context)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyBuilder(inputClass, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode buildIngestMode(IngestMode ingestMode, Iterable<String> leafModelClasses, CompileContext context)
    {
        return ingestMode.accept(new IngestModeBuilder(leafModelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_audit_Auditing buildAuditing(Auditing auditing)
    {
        return auditing.accept(AUDITING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning buildTransactionMilestoning(TransactionMilestoning transactionMilestoning)
    {
        return transactionMilestoning.accept(TRANSACTION_MILESTONING_BUILDER);
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning buildValidityMilestoning(ValidityMilestoning validityMilestoning, Iterable<String> leafModelClasses, CompileContext context)
    {
        return validityMilestoning.accept(new ValidityMilestoningBuilder(leafModelClasses, context));
    }

    public static Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation buildValidityDerivation(ValidityDerivation validityDerivation, Iterable<String> leafModelClasses, CompileContext context)
    {
        return validityDerivation.accept(new ValidityDerivationBuilder(leafModelClasses, context));
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
        Package currentPackage = type._package();
        while (!currentPackage._name().equals("Root"))
        {
            deque.push(currentPackage._name());
            currentPackage = currentPackage._package();
        }

        return Iterate.makeString(deque, "", "::", "::" + type._name());
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
        public Root_meta_pure_persistence_metamodel_trigger_Trigger visit(CronTrigger val)
        {
            return new Root_meta_pure_persistence_metamodel_trigger_CronTrigger_Impl("")
                    ._minutes(val.minutes)
                    ._hours(val.hours)
                    ._dayOfMonth(val.dayOfMonth)
                    ._month(val.month)
                    ._dayOfWeek(val.dayOfWeek);
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
            Iterable<String> leafModelClasses = val.targetShape.accept(new LeafModelClassExtractor(context));

            return new Root_meta_pure_persistence_metamodel_persister_BatchPersister_Impl("")
                    ._binding(buildBinding(val, context))
                    ._connection(buildConnection(val.connection, context))
                    ._ingestMode(buildIngestMode(val.ingestMode, leafModelClasses, context))
                    ._targetShape(buildTargetShape(val.targetShape, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_Persister visit(StreamingPersister val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_StreamingPersister_Impl("")
                    ._binding(buildBinding(val, context))
                    ._connection(buildConnection(val.connection, context));
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
            return new Root_meta_pure_persistence_metamodel_notifier_EmailNotifyee_Impl("")
                    ._emailAddress(val.address);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_notifier_Notifyee visit(PagerDutyNotifyee val)
        {
            return new Root_meta_pure_persistence_metamodel_notifier_PagerDutyNotifyee_Impl("")
                    ._url(val.url);
        }
    }

    private static class LeafModelClassExtractor implements TargetShapeVisitor<Iterable<String>>
    {
        private final CompileContext context;

        private LeafModelClassExtractor(CompileContext context)
        {
            this.context = context;
        }

        @Override
        public Iterable<String> visit(FlatTarget val)
        {
            return Sets.fixedSize.of(val.modelClass);
        }

        @Override
        public Iterable<String> visit(MultiFlatTarget val)
        {
            return Iterate.collect(val.parts, p -> {
                AbstractProperty<?> pureModelProperty = context.resolveProperty(val.modelClass, p.modelProperty);
                Type leafType = pureModelProperty._genericType()._rawType();
                Assert.assertTrue(leafType instanceof Class, () -> String.format("Target shape modelProperty '%s' must refer to a class.", p.modelProperty), p.sourceInformation, EngineErrorType.COMPILATION);

                return determineFullPath(leafType);
            });
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
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_FlatTarget_Impl("")
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
            return new Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTarget_Impl("")
                    ._modelClass(modelClass)
                    ._transactionScope(context.resolveEnumValue(TRANSACTION_SCOPE_FULL_PATH, val.transactionScope.name()))
                    ._parts(ListIterate.collect(val.parts, p -> resolvePart(p, modelClass, context)));
        }

        private Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart resolvePart(MultiFlatTargetPart part, Class<?> modelClass, CompileContext context)
        {
            Property<?, ?> property = validateAndResolveProperty(modelClass, part.modelProperty, part.sourceInformation, context);
            Type targetType = property._genericType()._rawType();
            Assert.assertTrue(targetType instanceof Class, () -> String.format("Target shape modelProperty '%s' must refer to a class.", part.modelProperty), part.sourceInformation, EngineErrorType.COMPILATION);

            return new Root_meta_pure_persistence_metamodel_persister_targetshape_MultiFlatTargetPart_Impl("")
                    ._modelProperty(property)
                    ._targetName(part.targetName)
                    ._partitionFields(ListIterate.collect(part.partitionFields, p -> validateAndResolvePropertyName(modelClass, p, part.sourceInformation, context)))
                    ._deduplicationStrategy(buildDeduplicationStrategy(part.deduplicationStrategy, modelClass, context));
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
                    ._versionField(validateAndResolvePropertyName(modelClass, val.versionField, val.sourceInformation, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_deduplication_DeduplicationStrategy visit(NoDeduplicationStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_deduplication_NoDeduplicationStrategy_Impl("");
        }
    }

    private static class IngestModeBuilder implements IngestModeVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode>
    {
        private final Iterable<String> leafModelClasses;
        private final CompileContext context;

        private IngestModeBuilder(Iterable<String> leafModelClasses, CompileContext context)
        {
            this.leafModelClasses = leafModelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_NontemporalSnapshot_Impl("")
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_UnitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalSnapshot val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_snapshot_BitemporalSnapshot_Impl("")
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning, leafModelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(NontemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_NontemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, leafModelClasses, context))
                    ._auditing(buildAuditing(val.auditing));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(UnitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_UnitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, leafModelClasses, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(BitemporalDelta val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_BitemporalDelta_Impl("")
                    ._mergeStrategy(buildMergeStrategy(val.mergeStrategy, leafModelClasses, context))
                    ._transactionMilestoning(buildTransactionMilestoning(val.transactionMilestoning))
                    ._validityMilestoning(buildValidityMilestoning(val.validityMilestoning, leafModelClasses, context));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_IngestMode visit(AppendOnly val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_appendonly_AppendOnly_Impl("")
                    ._auditing(buildAuditing(val.auditing))
                    ._filterDuplicates(val.filterDuplicates);
        }

        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy buildMergeStrategy(MergeStrategy mergeStrategy, Iterable<String> leafModelClasses, CompileContext context)
        {
            return mergeStrategy.accept(new MergeStrategyBuilder(leafModelClasses, context));
        }
    }

    private static class MergeStrategyBuilder implements MergeStrategyVisitor<Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy>
    {
        private final Iterable<String> leafModelClasses;
        private final CompileContext context;

        private MergeStrategyBuilder(Iterable<String> leafModelClasses, CompileContext context)
        {
            this.leafModelClasses = leafModelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(DeleteIndicatorMergeStrategy val)
        {
            String deleteProperty = Lists.immutable.ofAll(leafModelClasses)
                    .collect(c -> validateAndResolvePropertyName(context.resolveClass(c), val.deleteField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_DeleteIndicatorMergeStrategy_Impl("")
                    ._deleteField(deleteProperty)
                    ._deleteValues(Lists.immutable.ofAll(val.deleteValues));
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_ingestmode_delta_merge_NoDeletesMergeStrategy_Impl("");
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
        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdAndDateTimeTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName)
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_BatchIdTransactionMilestoning_Impl("")
                    ._batchIdInName(val.batchIdInName)
                    ._batchIdOutName(val.batchIdOutName);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_transactionmilestoning_DateTimeTransactionMilestoning_Impl("")
                    ._dateTimeInName(val.dateTimeInName)
                    ._dateTimeOutName(val.dateTimeOutName);
        }
    }

    private static class ValidityMilestoningBuilder implements ValidityMilestoningVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning>
    {
        private final Iterable<String> leafModelClasses;
        private final CompileContext context;

        private ValidityMilestoningBuilder(Iterable<String> leafModelClasses, CompileContext context)
        {
            this.leafModelClasses = leafModelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_DateTimeValidityMilestoning_Impl("")
                    ._derivation(buildValidityDerivation(val.derivation, leafModelClasses, context))
                    ._dateTimeFromName(val.dateTimeFromName)
                    ._dateTimeThruName(val.dateTimeThruName);
        }
    }

    private static class ValidityDerivationBuilder implements ValidityDerivationVisitor<Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation>
    {
        private final Iterable<String> leafModelClasses;
        private final CompileContext context;

        private ValidityDerivationBuilder(Iterable<String> leafModelClasses, CompileContext context)
        {
            this.leafModelClasses = leafModelClasses;
            this.context = context;
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromAndThruDateTime val)
        {
            String sourceDateTimeFromField = Lists.immutable.ofAll(leafModelClasses)
                    .collect(c -> validateAndResolvePropertyName(context.resolveClass(c), val.sourceDateTimeFromField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            String sourceDateTimeThruField = Lists.immutable.ofAll(leafModelClasses)
                    .collect(c -> validateAndResolvePropertyName(context.resolveClass(c), val.sourceDateTimeThruField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromAndThruDate_Impl("")
                    ._sourceDateTimeFromField(sourceDateTimeFromField)
                    ._sourceDateTimeThruField(sourceDateTimeThruField);
        }

        @Override
        public Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_ValidityDerivation visit(SourceSpecifiesFromDateTime val)
        {
            String sourceDateTimeFromField = Lists.immutable.ofAll(leafModelClasses)
                    .collect(c -> validateAndResolvePropertyName(context.resolveClass(c), val.sourceDateTimeFromField, val.sourceInformation, context))
                    .distinct()
                    .getOnly();

            return new Root_meta_pure_persistence_metamodel_persister_validitymilestoning_derivation_SourceSpecifiesValidFromDate_Impl("")
                    ._sourceDateTimeFromField(sourceDateTimeFromField);
        }
    }
}
