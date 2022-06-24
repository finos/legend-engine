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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.HelperConnectionGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.EmailNotifyee;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.Notifier;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier.Notifyee;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.BusinessDateTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.TriggerVisitor;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperPersistenceGrammarComposer
{
    private HelperPersistenceGrammarComposer()
    {
    }

    public static String renderPersistence(Persistence persistence, int indentLevel, PureGrammarComposerContext context)
    {
        return "Persistence " + convertPath(persistence.getPath()) + "\n" +
                "{\n" +
                renderDocumentation(persistence.documentation, indentLevel) +
                renderTrigger(persistence.trigger, indentLevel) +
                renderService(persistence.service, indentLevel) +
                renderPersister(persistence.persister, indentLevel, context) +
                renderNotifier(persistence.notifier, indentLevel) +
                "}";
    }

    private static String renderDocumentation(String documentation, int indentLevel)
    {
        return getTabString(indentLevel) + "doc: " + convertString(documentation, true) + ";\n";
    }

    private static String renderTrigger(Trigger trigger, int indentLevel)
    {
        return trigger.accept(new TriggerComposer(indentLevel));
    }

    private static String renderService(String service, int indentLevel)
    {
        return getTabString(indentLevel) + "service: " + service + ";\n";
    }

    private static String renderPersister(Persister persister, int indentLevel, PureGrammarComposerContext context)
    {
        return persister.accept(new PersisterComposer(indentLevel, context));
    }

    private static String renderNotifier(Notifier notifier, int indentLevel)
    {
        if (notifier.notifyees.isEmpty())
        {
            return "";
        }
        return getTabString(indentLevel) + "notifier:\n" +
                getTabString(indentLevel) + "{\n" +
                renderNotifyees(notifier.notifyees, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderNotifyees(List<Notifyee> notifyees, int indentLevel)
    {
        NotifyeeComposer visitor = new NotifyeeComposer(indentLevel + 1);
        return getTabString(indentLevel) + "notifyees:\n" +
                getTabString(indentLevel) + "[\n" +
                Iterate.makeString(ListIterate.collect(notifyees, n -> n.acceptVisitor(visitor)), ",\n") + "\n" +
                getTabString(indentLevel) + "]\n";
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
        public String visit(CronTrigger val)
        {
            //TODO: ledav -- implement cron trigger
            throw new UnsupportedOperationException("Cron trigger is not yet supported.");
        }

        @Override
        public String visit(BusinessDateTrigger val)
        {
            return getTabString(indentLevel) + "trigger: BusinessDate\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "pattern: '" + val.pattern + "';\n" +
                    getTabString(indentLevel + 1) + "startDate: '" + val.startDate + "';\n" +
                    getTabString(indentLevel + 1) + "maxDuration: '" + val.maxDuration + "';\n" +
                    getTabString(indentLevel + 1) + "repeatCount: '" + val.repeatCount + "';\n" +
                    getTabString(indentLevel + 1) + "repeatInterval: '" + val.repeatInterval + "';\n" +
                    getTabString(indentLevel) + "}";
        }
    }

    private static class NotifyeeComposer implements NotifyeeVisitor<String>
    {
        private final int indentLevel;

        private NotifyeeComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(EmailNotifyee val)
        {
            return getTabString(indentLevel) + "Email\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "address: '" + val.address + "';\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(PagerDutyNotifyee val)
        {
            return getTabString(indentLevel) + "PagerDuty\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "url: '" + val.url + "';\n" +
                    getTabString(indentLevel) + "}";
        }
    }

    private static class PersisterComposer implements PersisterVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;

        private PersisterComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(StreamingPersister val)
        {
            return getTabString(indentLevel) + "persister: Streaming\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderSink(val.sink, indentLevel + 1, context) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BatchPersister val)
        {
            return getTabString(indentLevel) + "persister: Batch\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderSink(val.sink, indentLevel + 1, context) +
                    renderIngestMode(val.ingestMode, indentLevel + 1) +
                    renderTargetShape(val.targetShape, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderSink(Sink sink, int indentLevel, PureGrammarComposerContext context)
        {
            return sink.accept(new SinkComposer(indentLevel, context));
        }

        private static String renderIngestMode(IngestMode ingestMode, int indentLevel)
        {
            return ingestMode.accept(new IngestModeComposer(indentLevel));
        }

        private static String renderTargetShape(TargetShape targetShape, int indentLevel)
        {
            return targetShape.accept(new TargetShapeComposer(indentLevel));
        }
    }

    private static class SinkComposer implements SinkVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;

        private SinkComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(RelationalSink val)
        {
            return getTabString(indentLevel) + "sink: Relational\n" +
                    getTabString(indentLevel) + "{\n" +
                    (val.connection == null ? "" : renderConnection(val.connection, indentLevel + 1, context)) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(ObjectStorageSink val)
        {
            return getTabString(indentLevel) + "sink: ObjectStorage\n" +
                    getTabString(indentLevel) + "{\n" +
                    renderBinding(val.binding, indentLevel + 1) +
                    renderConnection(val.connection, indentLevel + 1, context) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderBinding(String binding, int indentLevel)
        {
            return getTabString(indentLevel) + "binding: " + binding + ";\n";
        }

        private static String renderConnection(Connection connection, int indentLevel, PureGrammarComposerContext context)
        {
            DEPRECATED_PureGrammarComposerCore composerCore = DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build();
            if (connection instanceof ConnectionPointer)
            {
                return getTabString(indentLevel) + "connection: " + PureGrammarComposerUtility.convertPath(connection.accept(composerCore)) + ";\n";
            }
            return getTabString(indentLevel) + "connection:\n" +
                    getTabString(indentLevel) + "#{\n" +
                    getTabString(indentLevel + 1) + HelperConnectionGrammarComposer.getConnectionValueName(connection, composerCore.toContext()) + "\n" +
                    connection.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(composerCore).withIndentation(getTabSize(indentLevel + 1), true).build()) + "\n" +
                    getTabString(indentLevel) + "}#\n";
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
        public String visit(FlatTarget val)
        {
            return getTabString(indentLevel) + "targetShape: Flat\n" +
                    getTabString(indentLevel) + "{\n" +
                    (val.modelClass == null ? "" : getTabString(indentLevel + 1) + "modelClass: " + val.modelClass + ";\n") +
                    getTabString(indentLevel + 1) + "targetName: " + convertString(val.targetName, true) + ";\n" +
                    renderPartitionFields(val.partitionFields, indentLevel + 1) +
                    renderDeduplicationStrategy(val.deduplicationStrategy, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(MultiFlatTarget val)
        {
            return getTabString(indentLevel) + "targetShape: MultiFlat\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "modelClass: " + val.modelClass + ";\n" +
                    getTabString(indentLevel + 1) + "transactionScope: " + val.transactionScope + ";\n" +
                    getTabString(indentLevel + 1) + "parts:\n" +
                    getTabString(indentLevel + 1) + "[\n" +
                    renderParts(val, indentLevel + 2) +
                    getTabString(indentLevel + 1) + "];\n" +
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

        private static String renderPartProperties(MultiFlatTargetPart part, int indentLevel)
        {
            return getTabString(indentLevel) + "modelProperty: " + part.modelProperty + ";\n" +
                    getTabString(indentLevel) + "targetName: " + convertString(part.targetName, true) + ";\n" +
                    renderPartitionFields(part.partitionFields, indentLevel) +
                    renderDeduplicationStrategy(part.deduplicationStrategy, indentLevel);
        }

        private static String renderPartitionFields(List<String> partitionFields, int indentLevel)
        {
            return !partitionFields.isEmpty() ? getTabString(indentLevel) + "partitionFields: " + "[" +
                    Lists.immutable.ofAll(partitionFields).makeString(", ") +
                    "];\n" : "";
        }

        private static String renderDeduplicationStrategy(DeduplicationStrategy deduplicationStrategy, int indentLevel)
        {
            return deduplicationStrategy.accept(new DeduplicationStrategyComposer(indentLevel));
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
            return "";
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
                    getTabString(indentLevel + 1) + "versionField: " + val.versionField + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(DuplicateCountDeduplicationStrategy val)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: DuplicateCount\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "duplicateCountName: '" + val.duplicateCountName + "';\n" +
                    getTabString(indentLevel) + "}\n";
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
                    renderMergeStrategy(val.mergeStrategy, indentLevel + 1) +
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

        private static String renderMergeStrategy(MergeStrategy mergeStrategy, int indentLevel)
        {
            return mergeStrategy.accept(new MergeStrategyComposer(indentLevel));
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
                    getTabString(indentLevel + 1) + "dateTimeName: '" + val.dateTimeName + "';\n" +
                    getTabString(indentLevel) + "}\n";
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
                    getTabString(indentLevel + 1) + "batchIdInName: '" + val.batchIdInName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutName: '" + val.batchIdOutName + "';\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(DateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: DateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeInName: '" + val.dateTimeInName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutName: '" + val.dateTimeOutName + "';\n" +
                    renderTransactionDerivation(val.derivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return getTabString(indentLevel) + "transactionMilestoning: BatchIdAndDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInName: '" + val.batchIdInName + "';\n" +
                    getTabString(indentLevel + 1) + "batchIdOutName: '" + val.batchIdOutName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeInName: '" + val.dateTimeInName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutName: '" + val.dateTimeOutName + "';\n" +
                    renderTransactionDerivation(val.derivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderTransactionDerivation(TransactionDerivation transactionDerivation, int indentLevel)
        {
            return transactionDerivation == null ? "" : transactionDerivation.accept(new TransactionDerivationComposer(indentLevel));
        }
    }

    private static class TransactionDerivationComposer implements TransactionDerivationVisitor<String>
    {
        private final int indentLevel;

        private TransactionDerivationComposer(int indentLevel)
        {
            this.indentLevel = indentLevel;
        }

        @Override
        public String visit(SourceSpecifiesInDateTime val)
        {
            return getTabString(indentLevel) + "derivation: SourceSpecifiesInDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeInField: " + val.sourceDateTimeInField + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(SourceSpecifiesInAndOutDateTime val)
        {
            return getTabString(indentLevel) + "derivation: SourceSpecifiesInAndOutDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeInField: " + val.sourceDateTimeInField + ";\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeOutField: " + val.sourceDateTimeOutField + ";\n" +
                    getTabString(indentLevel) + "}\n";
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
                    getTabString(indentLevel + 1) + "dateTimeFromName: '" + val.dateTimeFromName + "';\n" +
                    getTabString(indentLevel + 1) + "dateTimeThruName: '" + val.dateTimeThruName + "';\n" +
                    renderValidityDerivation(val.derivation, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }

        private static String renderValidityDerivation(ValidityDerivation validityDerivation, int indentLevel)
        {
            return validityDerivation.accept(new ValidityDerivationComposer(indentLevel));
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
                    getTabString(indentLevel + 1) + "sourceDateTimeFromField: " + val.sourceDateTimeFromField + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }

        @Override
        public String visit(SourceSpecifiesFromAndThruDateTime val)
        {

            return getTabString(indentLevel) + "derivation: SourceSpecifiesFromAndThruDateTime\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromField: " + val.sourceDateTimeFromField + ";\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeThruField: " + val.sourceDateTimeThruField + ";\n" +
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
                    getTabString(indentLevel + 1) + "deleteField: " + val.deleteField + ";\n" +
                    renderDeleteValues(val, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
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
