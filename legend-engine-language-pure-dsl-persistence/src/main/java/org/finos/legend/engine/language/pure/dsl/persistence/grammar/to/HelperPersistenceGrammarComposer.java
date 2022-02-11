package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePipe;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ReaderVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.OpaqueTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.TriggerVisitor;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;

public class HelperPersistenceGrammarComposer
{
    private HelperPersistenceGrammarComposer() {}

    public static String renderPipe(PersistencePipe pipe, int indentLevel, PureGrammarComposerContext context)
    {
        return "PersistencePipe " + convertPath(pipe.getPath()) + "\n" +
                "{\n" +
                renderDocumentation(pipe.documentation, indentLevel) +
                renderOwners(pipe.owners, indentLevel) +
                renderTrigger(pipe.trigger, indentLevel) +
                renderReader(pipe.reader, indentLevel) +
                renderPersister(pipe.persister, indentLevel) +
                "}";
    }

    public static String renderDocumentation(String documentation, int indentLevel)
    {
        return getTabString(indentLevel) + "doc: " + convertString(documentation, true) + ";\n";
    }

    public static String renderOwners(List<String> owners, int indentLevel)
    {
        return owners.isEmpty() ? "" : getTabString(indentLevel) + "owners: " + "[" + LazyIterate.collect(owners, o -> convertString(o, true)).makeString(", ") + "];\n";
    }

    private static String renderTrigger(Trigger trigger, int indentLevel)
    {
        return trigger.accept(new TriggerComposer(indentLevel));
    }

    public static String renderReader(Reader reader, int indentLevel)
    {
        return reader.accept(new ReaderComposer(indentLevel));
    }

    public static String renderPersister(Persister persister, int indentLevel)
    {
        return persister.accept(new PersisterComposer(indentLevel));
    }

    private static String renderTargetSpecification(TargetSpecification targetSpecification, int indentLevel)
    {
        return targetSpecification.accept(new TargetSpecificationComposer(indentLevel));
    }

    private static String renderDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, int indentLevel)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyComposer(indentLevel));
    }

    private static String renderBatchMode(BatchMilestoningMode batchMode, int indentLevel)
    {
        return batchMode.accept(new BatchModeComposer(indentLevel));
    }

    private static String renderAuditing(Auditing auditing, int indentLevel)
    {
        return auditing.accept(new AuditingComposer(indentLevel));
    }

    private static String renderTransactionMilestoning(TransactionMilestoning transactionMilestoning, int indentLevel)
    {
        return transactionMilestoning.accept(new TransactionMilestoningComposer(indentLevel));
    }

    private static String renderValidityMilestoning(ValidityMilestoning validityMilestoning, int indentLevel)
    {
        return validityMilestoning.accept(new ValidityMilestoningComposer(indentLevel));
    }

    private static String renderValidityDerivation(ValidityDerivation validityDerivation, int indentLevel)
    {
        return validityDerivation.accept(new ValidiityDerivationComposer(indentLevel));
    }

    private static String renderMergeStrategy(MergeStrategy mergeStrategy, int indentLevel)
    {
        return mergeStrategy.accept(new MergeStrategyComposer(indentLevel));
    }

    // helper visitors for class hierarchies

    private static class TriggerComposer implements TriggerVisitor<String>
    {
        private final int indentLevel;

        private TriggerComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(OpaqueTrigger val)
        {
            return getTabString(indentLevel) + "trigger: " + val.getClass().getSimpleName() + ";\n";
        }
    }

    private static class ReaderComposer implements ReaderVisitor<String>
    {
        private final int indentLevel;

        private ReaderComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(ServiceReader val)
        {
            return getTabString(indentLevel) + "reader: Service\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "service: " + val.service + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static class PersisterComposer implements PersistenceVisitor<String>
    {
        private final int indentLevel;

        private PersisterComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(StreamingPersister val)
        {
            return getTabString(indentLevel) + "persister: Streaming\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BatchPersister val)
        {
            return getTabString(indentLevel) + "persister: Batch\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderTargetSpecification(val.targetSpecification, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static class TargetSpecificationComposer implements TargetSpecificationVisitor<String>
    {
        private final int indentLevel;

        private TargetSpecificationComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(GroupedFlatTargetSpecification val)
        {
            return getTabString(indentLevel) + "target: GroupedFlat\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "modelClass: " + val.modelClassPath + ";\n" +
                    getTabString(indentLevel + 1) + "transactionScope: " + val.transactionScope + ";\n" +
                    getTabString(indentLevel + 1) + "components:\n" +
                    getTabString(indentLevel + 1) + "[\n" +
                    renderComponents(val, indentLevel + 2) +
                    getTabString(indentLevel + 1) + "];\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(FlatTargetSpecification val)
        {
            return getTabString(indentLevel) + "target: Flat\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderFlatTargetProperties(val, true, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(NestedTargetSpecification val)
        {
            return getTabString(indentLevel) + "target: Nested\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "targetName: " + convertString(val.targetName, true) + ";\n" +
                    getTabString(indentLevel + 1) + "modelClass: " + val.modelClassPath + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderComponents(GroupedFlatTargetSpecification groupedFlatTarget, int indentLevel)
        {
            StringBuilder builder = new StringBuilder();
            ListIterate.forEachWithIndex(groupedFlatTarget.components, (component, i) ->
            {
                builder.append(getTabString(indentLevel)).append("{\n");
                builder.append(renderComponentProperties(component, indentLevel + 1));
                builder.append(getTabString(indentLevel)).append(i < groupedFlatTarget.components.size() - 1 ? "},\n" : "}\n");
            });
            return builder.toString();
        }

        private static String renderComponentProperties(PropertyAndFlatTargetSpecification component, int indentLevel)
        {
            return getTabString(indentLevel) + "property: " + component.propertyPath + ";\n" +
                    getTabString(indentLevel) + "targetSpecification:\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderFlatTargetProperties(component.targetSpecification,false, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderFlatTargetProperties(FlatTargetSpecification flatTarget, boolean includeModelClass, int indentLevel)
        {
            return getTabString(indentLevel) + "targetName: " + convertString(flatTarget.targetName, true) + ";\n" +
                    (includeModelClass ? getTabString(indentLevel) + "modelClass: " + flatTarget.modelClassPath + ";\n" : "") +
                    renderPartitionProperties(flatTarget, indentLevel) +
                    renderDeduplicationStrategy(flatTarget.deduplicationStrategy, indentLevel) +
                    renderBatchMode(flatTarget.milestoningMode, indentLevel);
        }

        private static String renderPartitionProperties(FlatTargetSpecification flatTarget, int indentLevel)
        {
            return !flatTarget.partitionPropertyPaths.isEmpty() ? getTabString(indentLevel) + "partitionProperties: " + "[" +
                    Lists.immutable.ofAll(flatTarget.partitionPropertyPaths).makeString(", ") +
                    "];\n" : "";
        }
    }

    private static class DeduplicationStrategyComposer implements DeduplicationStrategyVisitor<String>
    {
        private final int indentLevel;

        private DeduplicationStrategyComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(NoDeduplicationStrategy val)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: NoDeduplication\n;";
        }

        @Override
        public String visit(AnyVersionDeduplicationStrategy val)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: AnyVersion;\n";
        }

        @Override
        public String visit(MaxVersionDeduplicationStrategy val)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: MaxVersion\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "versionProperty: " + val.versionProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueDeduplicationStrategy val)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: OpaqueDeduplication;\n";
        }
    }

    private static class BatchModeComposer implements BatchMilestoningModeVisitor<String>
    {
        private final int indentLevel;

        private BatchModeComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(NonMilestonedSnapshot val)
        {
            return getTabString(indentLevel) + "batchMode: NonMilestonedSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(val.auditing, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(UnitemporalSnapshot val)
        {
            return getTabString(indentLevel) + "batchMode: UnitemporalSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BitemporalSnapshot val)
        {
            return getTabString(indentLevel) + "batchMode: BitemporalSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    renderValidityMilestoning(val.validityMilestoning, indentLevel + 1) +
                    renderValidityDerivation(val.validityDerivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(NonMilestonedDelta val)
        {
            return getTabString(indentLevel) + "batchMode: NonMilestonedDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(val.auditing, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(UnitemporalDelta val)
        {
            return getTabString(indentLevel) + "batchMode: UnitemporalDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderMergeStrategy(val.mergeStrategy, indentLevel + 1) +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BitemporalDelta val)
        {
            return getTabString(indentLevel) + "batchMode: BitemporalDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderMergeStrategy(val.mergeStrategy, indentLevel + 1) +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    renderValidityMilestoning(val.validityMilestoning, indentLevel + 1) +
                    renderValidityDerivation(val.validityDerivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(AppendOnly val)
        {
            return getTabString(indentLevel) + "batchMode: NonMilestonedDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(((AppendOnly) val).auditing, indentLevel + 1) +
                    getTabString(indentLevel + 1) + "filterDuplicates: " + ((AppendOnly) val).filterDuplicates + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static class AuditingComposer implements AuditingVisitor<String>
    {
        private final int indentLevel;

        private AuditingComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(NoAuditing val)
        {
            return getTabString(indentLevel) + "auditing: NoAuditing;\n";
        }

        @Override
        public String visit(BatchDateTimeAuditing val)
        {
            return getTabString(indentLevel) + "auditing: BatchDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchDateTimeFieldName: '" + val.dateTimeFieldName + "';\n" +
                    getTabString(indentLevel) + "}\n";        }

        @Override
        public String visit(OpaqueAuditing val)
        {
            return getTabString(indentLevel) + "auditing: " + val.getClass().getSimpleName() + ";\n";
        }
    }

    private static class TransactionMilestoningComposer implements TransactionMilestoningVisitor<String>
    {
        private final int indentLevel;

        private TransactionMilestoningComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(BatchIdTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: BatchIdOnly\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInFieldName: '" + val.batchIdInName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutFieldName: '" + val.batchIdOutName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(DateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: DateTimeOnly\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeInFieldName: '" + val.dateTimeInName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutFieldName: '" + val.dateTimeOutName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: BatchIdAndDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInFieldName: '" + val.batchIdInName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutFieldName: '" + val.batchIdOutName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeInFieldName: '" + val.dateTimeInName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutFieldName: '" + val.dateTimeOutName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: " + val.getClass().getSimpleName() + ";\n";
        }
    }

    private static class ValidityMilestoningComposer implements ValidityMilestoningVisitor<String>
    {
        private final int indentLevel;

        private ValidityMilestoningComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(DateTimeValidityMilestoning val)
        {
            return getTabString(indentLevel) + "validityMilestoning: DateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeFromFieldName: '" + val.dateTimeFromName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeThruFieldName: '" + val.dateTimeThruName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueValidityMilestoning val)
        {
            return getTabString(indentLevel) + "validityMilestoning: " + val.getClass().getSimpleName() + ";\n";
        }
    }

    private static class ValidiityDerivationComposer implements ValidityDerivationVisitor<String>
    {
        private final int indentLevel;

        private ValidiityDerivationComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(SourceSpecifiesFromDateTime val)
        {
            return getTabString(indentLevel) + "validityDerivation: SourceSpecifiesFromDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromProperty: " + val.sourceDateTimeFromProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(SourceSpecifiesFromAndThruDateTime val)
        {

            return getTabString(indentLevel) + "validityDerivation: SourceSpecifiesFromAndThruDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromProperty: " + val.sourceDateTimeFromProperty + ";\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeThruProperty: " + val.sourceDateTimeThruProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static class MergeStrategyComposer implements MergeStrategyVisitor<String>
    {
        private final int indentLevel;

        private MergeStrategyComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(NoDeletesMergeStrategy val)
        {
            return getTabString(indentLevel) + "mergeStrategy: NoDeletes;\n";
        }

        @Override
        public String visit(DeleteIndicatorMergeStrategy val)
        {
            return getTabString(indentLevel) + "mergeStrategy: DeleteIndicator\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "deleteProperty: " + val.deleteProperty + ";\n" +
                    renderDeleteValues(val, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueMergeStrategy val)
        {
            return getTabString(indentLevel) + "mergeStrategy: " + val.getClass().getSimpleName() + ";\n";
        }

        private static String renderDeleteValues(DeleteIndicatorMergeStrategy strategy, int indentLevel)
        {
            StringBuilder builder = new StringBuilder();
            builder.append(getTabString(indentLevel)).append("deleteValues: ");
            if (!strategy.deleteValues.isEmpty())
            {
                builder.append("[").append(LazyIterate.collect(strategy.deleteValues, d -> convertString(d, true)).makeString(", ")).append("];\n");
            }
            else
            {
                builder.append("[];\n");
            }
            return builder.toString();
        }
    }
}
