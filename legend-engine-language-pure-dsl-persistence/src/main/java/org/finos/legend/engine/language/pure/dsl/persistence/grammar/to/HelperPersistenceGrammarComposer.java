package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistencePipe;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.Auditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.BatchDateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.auditing.OpaqueAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.deduplication.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.OpaqueMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoning.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.OpaqueValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromAndThruDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.SourceSpecifiesFromDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.Reader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.reader.ServiceReader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersister;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.OpaqueTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;

public class HelperPersistenceGrammarComposer
{
    private HelperPersistenceGrammarComposer() {}

    public static String renderPipe(PersistencePipe pipe, PureGrammarComposerContext context, int indentLevel)
    {
        return "PersistencePipe " + PureGrammarComposerUtility.convertPath(pipe.getPath()) + "\n" +
                "{\n" +
                renderDocumentation(pipe.documentation, indentLevel) +
                renderOwners(pipe.owners, indentLevel) +
                renderTrigger(pipe.trigger, indentLevel) +
                renderReader(pipe.reader, context, indentLevel) +
                renderPersister(pipe.persister, context, indentLevel) +
                "}";
    }

    public static String renderDocumentation(String documentation, int indentLevel)
    {
        return getTabString(indentLevel) + "doc: " + convertString(documentation, true) + ";\n";
    }

    public static String renderOwners(List<String> owners, int indentLevel)
    {
        if (owners.isEmpty())
        {
            return "";
        }
        return getTabString(indentLevel) + "owners: " + "[" + LazyIterate.collect(owners, o -> convertString(o, true)).makeString(", ") + "];\n";
    }

    private static String renderTrigger(Trigger trigger, int indentLevel)
    {
        if (trigger instanceof OpaqueTrigger)
        {
            return getTabString(indentLevel) + "trigger: " + trigger.getClass().getSimpleName() + ";\n";
        }
        return unsupported(trigger.getClass());
    }

    public static String renderReader(Reader reader, PureGrammarComposerContext context, int indentLevel)
    {
        if (reader instanceof ServiceReader)
        {
            return renderServiceReader((ServiceReader) reader, context, indentLevel);
        }
        return unsupported(reader.getClass());
    }

    private static String renderServiceReader(ServiceReader service, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "reader: Service\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "service: " + service.service + ";\n" +
                getTabString(indentLevel) + "}\n";
    }

    public static String renderPersister(Persister persister, PureGrammarComposerContext context, int indentLevel)
    {
        if (persister instanceof StreamingPersister)
        {
            return renderStreamingPersister((StreamingPersister) persister, context, indentLevel);
        }
        else if (persister instanceof BatchPersister)
        {
            return renderBatchPersister((BatchPersister) persister, context, indentLevel);
        }
        return unsupported(Persister.class);
    }

    private static String renderStreamingPersister(StreamingPersister streaming, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "persister: Streaming\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderBatchPersister(BatchPersister persister, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "persister: Batch\n" +
                getTabString(indentLevel) + "{\n" +
                renderTargetSpecification(persister.targetSpecification, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderTargetSpecification(TargetSpecification targetSpecification, PureGrammarComposerContext context, int indentLevel)
    {
        if (targetSpecification instanceof FlatTargetSpecification)
        {
            return renderFlatTarget((FlatTargetSpecification) targetSpecification, context, indentLevel);
        }
        else if (targetSpecification instanceof GroupedFlatTargetSpecification)
        {
            return renderGroupedFlatTarget((GroupedFlatTargetSpecification) targetSpecification, context, indentLevel);
        }
        else if (targetSpecification instanceof NestedTargetSpecification)
        {
            return renderNestedTarget((NestedTargetSpecification) targetSpecification, context, indentLevel);
        }
        return unsupported(targetSpecification.getClass());
    }

    private static String renderFlatTarget(FlatTargetSpecification flatTarget, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "target: Flat\n" +
                getTabString(indentLevel) + "{\n" +
                renderFlatTargetProperties(flatTarget, context, true, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderFlatTargetProperties(FlatTargetSpecification flatTarget, PureGrammarComposerContext context, boolean includeModelClass, int indentLevel)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(indentLevel)).append("targetName: ").append(flatTarget.targetName).append(";\n");

        if (includeModelClass)
        {
            builder.append(getTabString(indentLevel)).append("modelClass: ").append(flatTarget.modelClassPath).append(";\n");
        }

        if (!flatTarget.partitionPropertyPaths.isEmpty())
        {
            builder.append(getTabString(indentLevel)).append("partitionProperties: ").append("[");
            builder.append(LazyIterate.collect(flatTarget.partitionPropertyPaths, p -> convertString(p, true)).makeString(", "));
            builder.append("];\n");
        }

        builder.append(renderDeduplicationStrategy(flatTarget, context, indentLevel));
        builder.append(renderBatchMode(flatTarget, context, indentLevel));

        return builder.toString();
    }

    private static String renderDeduplicationStrategy(FlatTargetSpecification flatTarget, PureGrammarComposerContext context, int indentLevel)
    {
        DeduplicationStrategy deduplicationStrategy = flatTarget.deduplicationStrategy;
        String deduplicationStrategyName = deduplicationStrategy.getClass().getSimpleName();
        if (deduplicationStrategy instanceof NoDeduplicationStrategy || deduplicationStrategy instanceof AnyVersionDeduplicationStrategy || deduplicationStrategy instanceof OpaqueDeduplicationStrategy)
        {
            return getTabString(indentLevel) + "deduplicationStrategy: " + deduplicationStrategyName + ";\n";
        }
        else if (deduplicationStrategy instanceof MaxVersionDeduplicationStrategy)
        {
            MaxVersionDeduplicationStrategy strategy = (MaxVersionDeduplicationStrategy) deduplicationStrategy;

            return getTabString(indentLevel) + "deduplicationStrategy: " + deduplicationStrategyName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "versionProperty: " + strategy.versionProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(deduplicationStrategy.getClass());
    }

    private static String renderBatchMode(FlatTargetSpecification flatTarget, PureGrammarComposerContext context, int indentLevel)
    {
        if (flatTarget.milestoningMode instanceof NonMilestonedSnapshot)
        {
            return renderNonMilestonedSnapshot((NonMilestonedSnapshot) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof UnitemporalSnapshot)
        {
            return renderUnitemporalSnapshot((UnitemporalSnapshot) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof BitemporalSnapshot)
        {
            return renderBitemporalSnapshot((BitemporalSnapshot) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof NonMilestonedDelta)
        {
            return renderNonMilestonedDelta((NonMilestonedDelta) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof UnitemporalDelta)
        {
            return renderUnitemporalDelta((UnitemporalDelta) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof BitemporalDelta)
        {
            return renderBitemporalDelta((BitemporalDelta) flatTarget.milestoningMode, context, indentLevel);
        }
        else if (flatTarget.milestoningMode instanceof AppendOnly)
        {
            return renderAppendOnly((AppendOnly) flatTarget.milestoningMode, context, indentLevel);
        }
        return unsupported(flatTarget.milestoningMode.getClass());
    }

    private static String renderGroupedFlatTarget(GroupedFlatTargetSpecification groupedFlatTarget, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "target: GroupedFlat\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "modelClass: " + groupedFlatTarget.modelClassPath + ";\n" +
                getTabString(indentLevel + 1) + "transactionScope: " + groupedFlatTarget.transactionScope + ";\n" +
                getTabString(indentLevel + 1) + "components:\n" +
                getTabString(indentLevel + 1) + "[\n" +
                renderComponents(groupedFlatTarget, context, indentLevel + 2) +
                getTabString(indentLevel + 1) + "];\n" +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderComponents(GroupedFlatTargetSpecification groupedFlatTarget, PureGrammarComposerContext context, int indentLevel)
    {
        StringBuilder builder = new StringBuilder();
        ListIterate.forEachWithIndex(groupedFlatTarget.components, (component, i) ->
        {
            builder.append(getTabString(indentLevel)).append("{\n");
            builder.append(renderComponentProperties(component, context, indentLevel + 1));
            builder.append(getTabString(indentLevel)).append(i < groupedFlatTarget.components.size() - 1 ? "},\n" : "}\n");
        });
        return builder.toString();
    }

    private static String renderComponentProperties(PropertyAndFlatTargetSpecification component, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "property: " + component.propertyPath + ";\n" +
                getTabString(indentLevel) + "targetSpecification:\n" +
                getTabString(indentLevel) + "{\n" +
                renderFlatTargetProperties(component.targetSpecification, context,false, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderNestedTarget(NestedTargetSpecification nestedTarget, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "target: Nested\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "targetName: " + nestedTarget.targetName + ";\n" +
                getTabString(indentLevel + 1) + "modelClass: " + nestedTarget.modelClassPath + ";\n" +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderNonMilestonedSnapshot(NonMilestonedSnapshot milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: NonMilestonedSnapshot\n" +
                getTabString(indentLevel) + "{\n" +
                renderAuditing(milestoningMode.auditing, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderUnitemporalSnapshot(UnitemporalSnapshot milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: UnitemporalSnapshot\n" +
                getTabString(indentLevel) + "{\n" +
                renderTransactionMilestoning(milestoningMode.transactionMilestoning, context,indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderBitemporalSnapshot(BitemporalSnapshot milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: BitemporalSnapshot\n" +
                getTabString(indentLevel) + "{\n" +
                renderTransactionMilestoning(milestoningMode.transactionMilestoning, context, indentLevel + 1) +
                renderValidityMilestoning(milestoningMode.validityMilestoning, context, indentLevel + 1) +
                renderValidityDerivation(milestoningMode.validityDerivation, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderNonMilestonedDelta(NonMilestonedDelta milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: NonMilestonedDelta\n" +
                getTabString(indentLevel) + "{\n" +
                renderAuditing(milestoningMode.auditing, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderUnitemporalDelta(UnitemporalDelta milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: UnitemporalDelta\n" +
                getTabString(indentLevel) + "{\n" +
                renderMergeStrategy(milestoningMode.mergeStrategy, context, indentLevel + 1) +
                renderTransactionMilestoning(milestoningMode.transactionMilestoning, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderBitemporalDelta(BitemporalDelta milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: BitemporalDelta\n" +
                getTabString(indentLevel) + "{\n" +
                renderMergeStrategy(milestoningMode.mergeStrategy, context, indentLevel + 1) +
                renderTransactionMilestoning(milestoningMode.transactionMilestoning, context, indentLevel + 1) +
                renderValidityMilestoning(milestoningMode.validityMilestoning, context, indentLevel + 1) +
                renderValidityDerivation(milestoningMode.validityDerivation, context, indentLevel + 1) +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderAppendOnly(AppendOnly milestoningMode, PureGrammarComposerContext context, int indentLevel)
    {
        return getTabString(indentLevel) + "batchMode: NonMilestonedDelta\n" +
                getTabString(indentLevel) + "{\n" +
                renderAuditing(milestoningMode.auditing, context, indentLevel + 1) +
                getTabString(indentLevel + 1) + "filterDuplicates: " + milestoningMode.filterDuplicates + ";\n" +
                getTabString(indentLevel) + "}\n";
    }

    private static String renderAuditing(Auditing auditing, PureGrammarComposerContext context, int indentLevel)
    {
        String auditingName = auditing.getClass().getSimpleName();
        if (auditing instanceof NoAuditing || auditing instanceof OpaqueAuditing)
        {
            return getTabString(indentLevel) + "auditing: " + auditingName + ";\n";
        }
        else if (auditing instanceof BatchDateTimeAuditing)
        {
            return getTabString(indentLevel) + "auditing: " + auditingName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimePropertyName: " + ((BatchDateTimeAuditing) auditing).dateTimePropertyName + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(auditing.getClass());
    }

    private static String renderTransactionMilestoning(TransactionMilestoning transactionMilestoning, PureGrammarComposerContext context, int indentLevel)
    {
        String milestoningName = transactionMilestoning.getClass().getSimpleName();
        if (transactionMilestoning instanceof OpaqueTransactionMilestoning)
        {
            return getTabString(indentLevel) + "transactionMilestoning: " + milestoningName + ";\n";
        }
        else if (transactionMilestoning instanceof BatchIdTransactionMilestoning)
        {
            BatchIdTransactionMilestoning milestoning = (BatchIdTransactionMilestoning) transactionMilestoning;
            return getTabString(indentLevel) + "transactionMilestoning: " + milestoningName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInProperty: " + milestoning.batchIdInName + ";\n" +
                    getTabString(indentLevel + 1) + "batchIdOutProperty: " + milestoning.batchIdOutName + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        else if (transactionMilestoning instanceof BatchIdAndDateTimeTransactionMilestoning)
        {
            BatchIdAndDateTimeTransactionMilestoning milestoning = (BatchIdAndDateTimeTransactionMilestoning) transactionMilestoning;
            return getTabString(indentLevel) + "transactionMilestoning: " + milestoningName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "batchIdInProperty: " + milestoning.batchIdInName + ";\n" +
                    getTabString(indentLevel + 1) + "batchIdOutProperty: " + milestoning.batchIdOutName + ";\n" +
                    getTabString(indentLevel + 1) + "dateTimeInProperty: " + milestoning.dateTimeInName + ";\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutProperty: " + milestoning.dateTimeOutName + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        else if (transactionMilestoning instanceof DateTimeTransactionMilestoning)
        {
            DateTimeTransactionMilestoning milestoning = (DateTimeTransactionMilestoning) transactionMilestoning;
            return getTabString(indentLevel) + "transactionMilestoning: " + milestoningName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeInProperty: " + milestoning.dateTimeInName + ";\n" +
                    getTabString(indentLevel + 1) + "dateTimeOutProperty: " + milestoning.dateTimeOutName + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(transactionMilestoning.getClass());
    }

    private static String renderValidityMilestoning(ValidityMilestoning validityMilestoning, PureGrammarComposerContext context, int indentLevel)
    {
        if (validityMilestoning instanceof OpaqueValidityMilestoning)
        {
            return getTabString(indentLevel) + "validityMilestoning: " + validityMilestoning + ";\n";
        }
        else if (validityMilestoning instanceof DateTimeValidityMilestoning)
        {
            DateTimeValidityMilestoning milestoning = (DateTimeValidityMilestoning) validityMilestoning;
            return getTabString(indentLevel) + "validityMilestoning: " + validityMilestoning + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "dateTimeFromProperty: " + milestoning.dateTimeFromName + ";\n" +
                    getTabString(indentLevel + 1) + "dateTimeThruProperty: " + milestoning.dateTimeThruName + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(validityMilestoning.getClass());
    }

    private static String renderValidityDerivation(ValidityDerivation validityDerivation, PureGrammarComposerContext context, int indentLevel)
    {
        String derivationName = validityDerivation.getClass().getSimpleName();
        if (validityDerivation instanceof SourceSpecifiesFromDate)
        {
            SourceSpecifiesFromDate derivation = (SourceSpecifiesFromDate) validityDerivation;

            return getTabString(indentLevel) + "validityDerivation: " + derivationName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromProperty: " + derivation.sourceDateTimeFromProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        else if (validityDerivation instanceof SourceSpecifiesFromAndThruDate)
        {
            SourceSpecifiesFromAndThruDate derivation = (SourceSpecifiesFromAndThruDate) validityDerivation;

            return getTabString(indentLevel) + "validityDerivation: " + derivationName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeFromProperty: " + derivation.sourceDateTimeFromProperty + ";\n" +
                    getTabString(indentLevel + 1) + "sourceDateTimeThruProperty: " + derivation.sourceDateTimeThruProperty + ";\n" +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(validityDerivation.getClass());
    }

    private static String renderMergeStrategy(MergeStrategy mergeStrategy, PureGrammarComposerContext context, int indentLevel)
    {
        String mergeStrategyName = mergeStrategy.getClass().getSimpleName();
        if (mergeStrategy instanceof OpaqueMergeStrategy || mergeStrategy instanceof NoDeletesMergeStrategy)
        {
            return getTabString(indentLevel) + "mergeStrategy: " + mergeStrategyName + ";\n";
        }
        else if (mergeStrategy instanceof DeleteIndicatorMergeStrategy)
        {
            DeleteIndicatorMergeStrategy strategy = (DeleteIndicatorMergeStrategy) mergeStrategy;
            return getTabString(indentLevel) + "mergeStrategy: " + mergeStrategyName + "\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "deleteProperty: " + strategy.deleteProperty + ";\n" +
                    renderDeleteValues(strategy, indentLevel + 1) +
                    getTabString(indentLevel) + "}\n";
        }
        return unsupported(mergeStrategy.getClass());
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
