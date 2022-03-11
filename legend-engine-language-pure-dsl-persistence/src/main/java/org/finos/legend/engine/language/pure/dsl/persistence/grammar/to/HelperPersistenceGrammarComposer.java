package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
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

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;

public class HelperPersistenceGrammarComposer
{
    private HelperPersistenceGrammarComposer() {}

    public static String renderPersistence(Persistence persistence, int indentLevel, PureGrammarComposerContext context)
    {
        return "Persistence " + convertPath(persistence.getPath()) + "\n" +
                "{\n" +
                renderDocumentation(persistence.documentation, indentLevel) +
                renderTrigger(persistence.trigger, indentLevel) +
                renderReader(persistence.reader, indentLevel) +
                renderPersister(persistence.persister, indentLevel) +
                "}";
    }

    public static String renderDocumentation(String documentation, int indentLevel)
    {
        return getTabString(indentLevel) + "doc: " + convertString(documentation, true) + ";\n";
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

    private static String renderTargetShape(TargetShape targetShape, int indentLevel)
    {
        return targetShape.accept(new TargetShapeComposer(indentLevel));
    }

    private static String renderDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, int indentLevel)
    {
        return deduplicationStrategy.accept(new DeduplicationStrategyComposer(indentLevel));
    }

    private static String renderIngestMode(IngestMode ingestMode, int indentLevel)
    {
        return ingestMode.accept(new IngestModeComposer(indentLevel));
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
        return validityDerivation.accept(new ValidityDerivationComposer(indentLevel));
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
        public String visit(ManualTrigger val)
        {
            return getTabString(indentLevel) + "trigger: Manual;\n";
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

    private static class PersisterComposer implements PersisterVisitor<String>
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
                    renderTargetShape(val.targetShape, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }
    }

    private static class TargetShapeComposer implements TargetShapeVisitor<String>
    {
        private final int indentLevel;

        private TargetShapeComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(MultiFlatTarget val)
        {
            return getTabString(indentLevel) + "target: MultiFlat\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "modelClass: " + val.modelClass + ";\n" +
                    getTabString(indentLevel + 1) + "transactionScope: " + val.transactionScope + ";\n" +
                    getTabString(indentLevel + 1) + "parts:\n" +
                    getTabString(indentLevel + 1) + "[\n" +
                    renderParts(val, indentLevel + 2) +
                    getTabString(indentLevel + 1) + "];\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(FlatTarget val)
        {
            return getTabString(indentLevel) + "target: Flat\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderFlatTargetProperties(val, true, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueTarget val)
        {
            return getTabString(indentLevel) + "target: OpaqueTarget\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "targetName: " + convertString(val.targetName, true) + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderParts(MultiFlatTarget multiFlatTarget, int indentLevel)
        {
            StringBuilder builder = new StringBuilder();
            ListIterate.forEachWithIndex(multiFlatTarget.parts, (part, i) ->
            {
                builder.append(getTabString(indentLevel)).append("{\n");
                builder.append(renderPartProperties(part, indentLevel + 1));
                builder.append(getTabString(indentLevel)).append(i < multiFlatTarget.parts.size() - 1 ? "},\n" : "}\n");
            });
            return builder.toString();
        }

        private static String renderPartProperties(PropertyAndFlatTarget part, int indentLevel)
        {
            return getTabString(indentLevel) + "property: " + part.property + ";\n" +
                    getTabString(indentLevel) + "flatTarget:\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderFlatTargetProperties(part.flatTarget,false, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderFlatTargetProperties(FlatTarget flatTarget, boolean includeModelClass, int indentLevel)
        {
            return getTabString(indentLevel) + "targetName: " + convertString(flatTarget.targetName, true) + ";\n" +
                    (includeModelClass ? getTabString(indentLevel) + "modelClass: " + flatTarget.modelClass + ";\n" : "") +
                    renderPartitionProperties(flatTarget, indentLevel) +
                    renderDeduplicationStrategy(flatTarget.deduplicationStrategy, indentLevel) +
                    renderIngestMode(flatTarget.ingestMode, indentLevel);
        }

        private static String renderPartitionProperties(FlatTarget flatTarget, int indentLevel)
        {
            return !flatTarget.partitionProperties.isEmpty() ? getTabString(indentLevel) + "partitionProperties: " + "[" +
                    Lists.immutable.ofAll(flatTarget.partitionProperties).makeString(", ") +
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
            return getTabString(indentLevel) + "deduplicationStrategy: None;\n";
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

    private static class IngestModeComposer implements IngestModeVisitor<String>
    {
        private final int indentLevel;

        private IngestModeComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(NontemporalSnapshot val)
        {
            return getTabString(indentLevel) + "ingestMode: NontemporalSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(val.auditing, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(UnitemporalSnapshot val)
        {
            return getTabString(indentLevel) + "ingestMode: UnitemporalSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BitemporalSnapshot val)
        {
            return getTabString(indentLevel) + "ingestMode: BitemporalSnapshot\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    renderValidityMilestoning(val.validityMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(NontemporalDelta val)
        {
            return getTabString(indentLevel) + "ingestMode: NontemporalDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(val.auditing, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(UnitemporalDelta val)
        {
            return getTabString(indentLevel) + "ingestMode: UnitemporalDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderMergeStrategy(val.mergeStrategy, indentLevel + 1) +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BitemporalDelta val)
        {
            return getTabString(indentLevel) + "ingestMode: BitemporalDelta\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderMergeStrategy(val.mergeStrategy, indentLevel + 1) +
                    renderTransactionMilestoning(val.transactionMilestoning, indentLevel + 1) +
                    renderValidityMilestoning(val.validityMilestoning, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(AppendOnly val)
        {
            return getTabString(indentLevel) + "ingestMode: AppendOnly\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderAuditing(val.auditing, indentLevel + 1) +
                    getTabString(indentLevel + 1) + "filterDuplicates: " + val.filterDuplicates + ";\n" +
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
            return getTabString(indentLevel) + "auditing: None;\n";
        }

        @Override
        public String visit(DateTimeAuditing val)
        {
            return getTabString(indentLevel) + "auditing: DateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeFieldName: '" + val.dateTimeFieldName + "';\n" +
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
            return getTabString(indentLevel) + "transactionMilestoning: BatchId\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInFieldName: '" + val.batchIdInFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutFieldName: '" + val.batchIdOutFieldName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(DateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: DateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeInFieldName: '" + val.dateTimeInFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutFieldName: '" + val.dateTimeOutFieldName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: BatchIdAndDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInFieldName: '" + val.batchIdInFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutFieldName: '" + val.batchIdOutFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeInFieldName: '" + val.dateTimeInFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutFieldName: '" + val.dateTimeOutFieldName + "';\n" +
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
                    getTabString(indentLevel + 1) + "dateTimeFromFieldName: '" + val.dateTimeFromFieldName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeThruFieldName: '" + val.dateTimeThruFieldName + "';\n" +
                    renderValidityDerivation(val.derivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(OpaqueValidityMilestoning val)
        {
            return getTabString(indentLevel) + "validityMilestoning: OpaqueValidityMilestoning" + val.getClass().getSimpleName() + ";\n";
        }
    }

    private static class ValidityDerivationComposer implements ValidityDerivationVisitor<String>
    {
        private final int indentLevel;

        private ValidityDerivationComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(SourceSpecifiesFromDateTime val)
        {
            return getTabString(indentLevel) + "derivation: SourceSpecifiesFromDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromProperty: " + val.sourceDateTimeFromProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(SourceSpecifiesFromAndThruDateTime val)
        {

            return getTabString(indentLevel) + "derivation: SourceSpecifiesFromAndThruDateTime\n" +
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
