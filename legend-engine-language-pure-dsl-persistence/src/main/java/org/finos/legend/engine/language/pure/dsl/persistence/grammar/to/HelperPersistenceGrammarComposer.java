package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.FlatTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.GroupedFlatTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.NestedTargetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification.PropertyAndFlatTargetSpecification;
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

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;


public class HelperPersistenceGrammarComposer
{
    public static String renderOwners(List<String> owners)
    {
        StringBuilder builder = new StringBuilder();
        if (!owners.isEmpty())
        {
            builder.append("owners: ").append("[").append(LazyIterate.collect(owners, o -> convertString(o, true)).makeString(", ")).append("];\n");
        }
        return builder.toString();
    }

    public static String renderReader(Reader reader, PureGrammarComposerContext context)
    {
        if (reader instanceof ServiceReader)
        {
            return renderServiceReader((ServiceReader) reader, context);
        }
        throw new UnsupportedOperationException();
    }

    private static String renderServiceReader(ServiceReader service, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("reader: Service\n");
        builder.append(getTabString()).append("{\n");
        builder.append(getTabString(baseIndentation)).append("service: ").append(service.service).append(";\n");
        builder.append(getTabString()).append("}");

        return builder.toString();
    }

    public static String renderPersister(Persister persister, PureGrammarComposerContext context)
    {
        if (persister instanceof BatchPersister)
        {
            return renderBatchPersistence((BatchPersister) persister, context);
        }
        else if (persister instanceof StreamingPersister)
        {
            return renderStreamingPersistence((StreamingPersister) persister, context);
        }
        throw new UnsupportedOperationException();
    }

    private static String renderStreamingPersistence(StreamingPersister streaming, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Streaming\n");
        builder.append(getTabString()).append("{\n");
        builder.append(getTabString()).append("}");

        return builder.toString();
    }

    private static String renderBatchPersistence(BatchPersister persister, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Batch\n");
        builder.append(getTabString()).append("{\n");

        if (persister.targetSpecification instanceof FlatTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderFlatTarget((FlatTargetSpecification) persister.targetSpecification, context));
        }
        else if (persister.targetSpecification instanceof GroupedFlatTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderGroupedFlatTarget((GroupedFlatTargetSpecification) persister.targetSpecification, context));
        }
        else if (persister.targetSpecification instanceof NestedTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderNestedTarget((NestedTargetSpecification) persister.targetSpecification, context));
        }
        else
        {
            return unsupported(persister.targetSpecification.getClass());
        }

        builder.append(getTabString() + 1).append("}\n");
        return builder.toString();
    }

    private static String renderFlatTarget(FlatTargetSpecification flatTarget, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString() + 1).append("target: Flat\n");
        builder.append(getTabString() + 1).append("{\n");
        builder.append(renderFlatComponent(flatTarget, baseIndentation, context));
        builder.append(getTabString() + 1).append("}\n");

        return builder.toString();
    }

    private static String renderFlatComponent(FlatTargetSpecification flatTarget, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("targetName: ").append(flatTarget.targetName).append(";\n");
        builder.append(getTabString(baseIndentation)).append("modelClass: ").append(flatTarget.modelClassPath).append(";\n");

        if (!flatTarget.partitionPropertyPaths.isEmpty())
        {
            builder.append(getTabString(baseIndentation)).append("partitionProperties: ").append("[");
            builder.append(LazyIterate.collect(flatTarget.partitionPropertyPaths, p -> convertString(p, true)).makeString(", "));
            builder.append("];\n");
        }

        DeduplicationStrategy deduplicationStrategy = flatTarget.deduplicationStrategy;
        if (deduplicationStrategy instanceof NoDeduplicationStrategy || deduplicationStrategy instanceof AnyVersionDeduplicationStrategy || deduplicationStrategy instanceof OpaqueDeduplicationStrategy)
        {
            builder.append(getTabString(baseIndentation)).append("deduplicationStrategy: ").append(deduplicationStrategy.getClass().getSimpleName()).append(";\n");
        }
        else if (deduplicationStrategy instanceof MaxVersionDeduplicationStrategy)
        {
            MaxVersionDeduplicationStrategy strategy = (MaxVersionDeduplicationStrategy) deduplicationStrategy;

            builder.append(getTabString(baseIndentation)).append("deduplicationStrategy: ").append(deduplicationStrategy).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("versionProperty: ").append(strategy.versionProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else
        {
            return unsupported(deduplicationStrategy.getClass());
        }

        if (flatTarget.milestoningMode instanceof BitemporalDelta)
        {
            builder.append(getTabString(baseIndentation)).append(renderBitemporalDelta((BitemporalDelta) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof UnitemporalDelta)
        {
            builder.append(getTabString(baseIndentation)).append(renderUnitemporalDelta((UnitemporalDelta) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof NonMilestonedDelta)
        {
            builder.append(getTabString(baseIndentation)).append(renderNonMilestonedDelta((NonMilestonedDelta) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof BitemporalSnapshot)
        {
            builder.append(getTabString(baseIndentation)).append(renderBitemporalSnapshot((BitemporalSnapshot) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof UnitemporalSnapshot)
        {
            builder.append(getTabString(baseIndentation)).append(renderUnitemporalSnapshot((UnitemporalSnapshot) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof NonMilestonedSnapshot)
        {
            builder.append(getTabString(baseIndentation)).append(renderNonMilestonedSnapshot((NonMilestonedSnapshot) flatTarget.milestoningMode, context));
        }
        else if (flatTarget.milestoningMode instanceof AppendOnly)
        {
            builder.append(getTabString(baseIndentation)).append(renderAppendOnly((AppendOnly) flatTarget.milestoningMode, context));
        }
        else
        {
            return unsupported(flatTarget.milestoningMode.getClass());
        }

        return builder.toString();
    }

    private static String renderGroupedFlatTarget(GroupedFlatTargetSpecification groupedFlatTarget, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()+1).append("target: GroupedFlat\n");
        builder.append(getTabString()+1).append("{\n");
        builder.append(getTabString(baseIndentation)).append("modelClass: ").append(groupedFlatTarget.modelClassPath).append(";\n");
        builder.append(getTabString(baseIndentation)).append("transactionScope: ").append(groupedFlatTarget.transactionScope).append(";\n");
        builder.append(getTabString(baseIndentation)).append("components:\n");
        builder.append(getTabString(baseIndentation)).append("[\n");
        ListIterate.forEachWithIndex(groupedFlatTarget.components, (component, i) ->
        {
            builder.append(getTabString(baseIndentation + 1)).append("{\n");
            builder.append(renderComponent(component, baseIndentation + 2, context));
            builder.append(getTabString(baseIndentation + 1)).append(i < groupedFlatTarget.components.size() - 1 ? "},\n" : "}\n");
        });
        builder.append(getTabString(baseIndentation)).append("];\n");

        builder.append(getTabString()+1).append("}\n");

        return builder.toString();
    }

    private static String renderComponent(PropertyAndFlatTargetSpecification component, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("property: ").append(component.propertyPath).append(";\n");
        builder.append(getTabString(baseIndentation)).append("targetSpecification:\n");
        builder.append(getTabString(baseIndentation + 1)).append("{\n");
        builder.append(renderFlatComponent(component.targetSpecification, baseIndentation + 2, context));
        builder.append(getTabString(baseIndentation + 1)).append("}\n");

        return builder.toString();
    }

    private static String renderNestedTarget(NestedTargetSpecification nestedTarget, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()+1).append("target: Nested\n");
        builder.append(getTabString()+1).append("{\n");
        builder.append(getTabString(baseIndentation)).append("targetName: ").append(nestedTarget.targetName).append(";\n");
        builder.append(getTabString(baseIndentation)).append("modelClass: ").append(nestedTarget.modelClassPath).append(";\n");

        builder.append(getTabString()+1).append("}\n");

        return builder.toString();
    }

    private static String renderBitemporalDelta(BitemporalDelta milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: BitemporalDelta\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderMergeStrategy(milestoningMode.mergeStrategy, baseIndentation + 1, context));
        builder.append(renderTransactionMilestoning(milestoningMode.transactionMilestoning, baseIndentation + 1, context));
        builder.append(renderValidityMilestoning(milestoningMode.validityMilestoning, baseIndentation + 1, context));
        builder.append(renderValidityDerivation(milestoningMode.validityDerivation, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderUnitemporalDelta(UnitemporalDelta milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: UnitemporalDelta\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderMergeStrategy(milestoningMode.mergeStrategy, baseIndentation + 1, context));
        builder.append(renderTransactionMilestoning(milestoningMode.transactionMilestoning, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderNonMilestonedDelta(NonMilestonedDelta milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: NonMilestonedDelta\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderAuditing(milestoningMode.auditing, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderAppendOnly(AppendOnly milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: NonMilestonedDelta\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderAuditing(milestoningMode.auditing, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)+1).append("filterDuplicates: ").append(milestoningMode.filterDuplicates).append(";\n");
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderUnitemporalSnapshot(UnitemporalSnapshot milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: UnitemporalSnapshot\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderTransactionMilestoning(milestoningMode.transactionMilestoning, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderBitemporalSnapshot(BitemporalSnapshot milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: BitemporalSnapshot\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderTransactionMilestoning(milestoningMode.transactionMilestoning, baseIndentation + 1, context));
        builder.append(renderValidityMilestoning(milestoningMode.validityMilestoning, baseIndentation + 1, context));
        builder.append(renderValidityDerivation(milestoningMode.validityDerivation, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderNonMilestonedSnapshot(NonMilestonedSnapshot milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: NonMilestonedSnapshot\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(renderAuditing(milestoningMode.auditing, baseIndentation + 1, context));
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

    private static String renderAuditing(Auditing auditing, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (auditing instanceof NoAuditing || auditing instanceof OpaqueAuditing)
        {
            builder.append(getTabString(baseIndentation)).append("auditing: ").append(auditing.getClass().getSimpleName()).append(";\n");
        }
        else if (auditing instanceof BatchDateTimeAuditing)
        {
            builder.append(getTabString(baseIndentation)).append("auditing: ").append(auditing).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation + 1)).append("dateTimePropertyName: ").append(((BatchDateTimeAuditing) auditing).dateTimePropertyName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }

        return builder.toString();
    }

    private static String renderTransactionMilestoning(TransactionMilestoning transactionMilestoning, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (transactionMilestoning instanceof OpaqueTransactionMilestoning)
        {
            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append(";\n");
        }
        else if (transactionMilestoning instanceof BatchIdTransactionMilestoning)
        {
            BatchIdTransactionMilestoning milestoning = (BatchIdTransactionMilestoning) transactionMilestoning;

            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdInProperty: ").append(milestoning.batchIdInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdOutProperty: ").append(milestoning.batchIdOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (transactionMilestoning instanceof BatchIdAndDateTimeTransactionMilestoning)
        {
            BatchIdAndDateTimeTransactionMilestoning milestoning = (BatchIdAndDateTimeTransactionMilestoning) transactionMilestoning;

            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation + 1)).append("batchIdInProperty: ").append(milestoning.batchIdInName).append(";\n");
            builder.append(getTabString(baseIndentation + 1)).append("batchIdOutProperty: ").append(milestoning.batchIdOutName).append(";\n");
            builder.append(getTabString(baseIndentation + 1)).append("dateTimeInProperty: ").append(milestoning.dateTimeInName).append(";\n");
            builder.append(getTabString(baseIndentation + 1)).append("dateTimeOutProperty: ").append(milestoning.dateTimeOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (transactionMilestoning instanceof DateTimeTransactionMilestoning)
        {
            DateTimeTransactionMilestoning milestoning = (DateTimeTransactionMilestoning) transactionMilestoning;

            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeInProperty: ").append(milestoning.dateTimeInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeOutProperty: ").append(milestoning.dateTimeOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else
        {
            return unsupported(transactionMilestoning.getClass());
        }

        return builder.toString();
    }

    private static String renderValidityMilestoning(ValidityMilestoning validityMilestoning, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (validityMilestoning instanceof OpaqueValidityMilestoning)
        {
            builder.append(getTabString(baseIndentation)).append("validityMilestoning: ").append(validityMilestoning).append(";\n");
        }
        else if (validityMilestoning instanceof DateTimeValidityMilestoning)
        {
            DateTimeValidityMilestoning milestoning = (DateTimeValidityMilestoning) validityMilestoning;

            builder.append(getTabString(baseIndentation)).append("validityMilestoning: ").append(validityMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeFromProperty: ").append(milestoning.dateTimeFromName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeThruProperty: ").append(milestoning.dateTimeThruName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else
        {
            return unsupported(validityMilestoning.getClass());
        }

        return builder.toString();
    }

    private static String renderValidityDerivation(ValidityDerivation validityDerivation, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (validityDerivation instanceof SourceSpecifiesFromDate)
        {
            SourceSpecifiesFromDate derivation = (SourceSpecifiesFromDate) validityDerivation;

            builder.append(getTabString(baseIndentation)).append("validityDerivation: ").append(validityDerivation).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeFromProperty: ").append(derivation.sourceDateTimeFromProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (validityDerivation instanceof SourceSpecifiesFromAndThruDate)
        {
            SourceSpecifiesFromAndThruDate derivation = (SourceSpecifiesFromAndThruDate) validityDerivation;

            builder.append(getTabString(baseIndentation)).append("validityDerivation: ").append(validityDerivation).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeFromProperty: ").append(derivation.sourceDateTimeFromProperty).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeThruProperty: ").append(derivation.sourceDateTimeThruProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else
        {
            return unsupported(validityDerivation.getClass());
        }

        return builder.toString();
    }

    private static String renderMergeStrategy(MergeStrategy mergeStrategy, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (mergeStrategy instanceof OpaqueMergeStrategy || mergeStrategy instanceof NoDeletesMergeStrategy)
        {
            builder.append(getTabString(baseIndentation)).append("mergeStrategy: ").append(mergeStrategy.getClass().getSimpleName()).append(";\n");
        }
        else if (mergeStrategy instanceof DeleteIndicatorMergeStrategy)
        {
            DeleteIndicatorMergeStrategy strategy = (DeleteIndicatorMergeStrategy) mergeStrategy;

            builder.append(getTabString(baseIndentation)).append("mergeStrategy: ").append(mergeStrategy).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation + 1)).append("deleteProperty: ").append(strategy.deleteProperty).append(";\n");
            builder.append(getTabString(baseIndentation + 1)).append("deleteValues: ");
            if (!strategy.deleteValues.isEmpty())
            {
                builder.append("[").append(LazyIterate.collect(strategy.deleteValues, d -> convertString(d, true)).makeString(", ")).append("];\n");
            }
            else {builder.append("[];\n");}
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else
        {
            return unsupported(mergeStrategy.getClass());
        };

        return builder.toString();
    }
}
