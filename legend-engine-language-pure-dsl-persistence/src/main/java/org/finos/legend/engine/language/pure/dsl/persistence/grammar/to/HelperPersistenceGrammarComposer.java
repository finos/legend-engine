package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.ServicePersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.StreamingPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.BatchPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.BatchDatastoreSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.BatchDatasetSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.BitemporalDelta;


import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;


public class HelperPersistenceGrammarComposer
{
    /*
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

    public static String renderPersistence(Persistence persistence, PureGrammarComposerContext context)
    {
        if (persistence instanceof BatchPersistence)
        {
            return renderBatchPersistence((BatchPersistence) persistence, context);
        }
        else if (persistence instanceof StreamingPersistence)
        {
            return renderStreamingPersistence((StreamingPersistence) persistence, context);
        }
        throw new UnsupportedOperationException();
    }

    private static String renderStreamingPersistence(StreamingPersistence streaming, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Streaming\n");
        builder.append(getTabString()).append("{\n");
        builder.append(getTabString()).append("}");

        return builder.toString();
    }

    private static String renderBatchPersistence(BatchPersistence batch, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Batch\n");
        builder.append(getTabString()).append("{\n");

        if (batch.targetSpecification instanceof FlatTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderFlatTarget((FlatTargetSpecification) persistence.targetSpecification, context));
        }
        else if (batch.targetSpecification instanceof GroupedFlatTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderGroupedFlatTarget((GroupedFlatTargetSpecification) persistence.targetSpecification, context));
        }
        else if (batch.targetSpecification instanceof NestedTargetSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderNestedTarget((NestedTargetSpecification) persistence.targetSpecification, context));
        }
        throw new UnsupportedOperationException();

        builder.append(getTabString()+1).append("}\n");

        return builder.toString();
    }

    private static String renderFlatTarget(FlatTargetSpecification flatTarget, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()+1).append("target: Flat\n");
        builder.append(getTabString()+1).append("{\n");
        builder.append(renderFlatComponenet(flatTarget, baseIndentation, context));
        builder.append(getTabString()+1).append("}\n");

        return builder.toString();
    }

    private static String renderFlatComponent(FlatTargetSpecification flatTarget, int baseIndentation, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();

        builder.append(getTabString(baseIndentation)).append("targetName: ").append(flatTarget.targetName).append(";\n");
        builder.append(getTabString(baseIndentation)).append("modelClass: ").append(flatTarget.modelClassPath).append(";\n");

        if (!flatTarget.partitionPropertyPaths.isEmpty())
        {
        builder.append(getTabString(baseIndentation)).append("partitionProperties: ").append("[");
            builder.append(LazyIterate.collect(partitionPropertyPaths, p -> convertString(p, true)).makeString(", "));
        builder.append("];\n");
        }

        if (flatTarget.deduplicationStrategy instanceof (NoDeduplicationStrategy || AnyVersionDeduplicationStrategy || OpaqueDeduplicationStrategy))
        {
            builder.append(getTabString(baseIndentation)).append("deduplicationStrategy: ").append(flatTarget.deduplicationStrategy).append(";\n");
        }
        else if (flatTarget.deduplicationStrategy instanceof MaxVersionDeduplicationStrategy)
        {
            builder.append(getTabString(baseIndentation)).append("deduplicationStrategy: ").append(flatTarget.deduplicationStrategy).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("versionProperty: ").append(flatTarget.deduplicationStrategy.versionProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (flatTarget.deduplicationStrategy.isEmpty())
        {}

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
        else throw new UnsupportedOperationException();

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

        if (auditing instanceof (NoAuditing || OpaqueAuditing))
        {
            builder.append(getTabString(baseIndentation)).append("auditing: ").append(auditing).append(";\n");
        }
        else if (auditing instanceof BatchDateTimeAuditing)
        {
            builder.append(getTabString(baseIndentation)).append("auditing: ").append(auditing).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimePropertyName: ").append(auditing.dateTimePropertyName).append(";\n");
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
            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdInProperty: ").append(transactionMilestoning.batchIdInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdOutProperty: ").append(transactionMilestoning.batchIdOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (transactionMilestoning instanceof BatchIdAndDateTimeTransactionMilestoning)
        {
            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdInProperty: ").append(transactionMilestoning.batchIdInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("batchIdOutProperty: ").append(transactionMilestoning.batchIdOutName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeInProperty: ").append(transactionMilestoning.dateTimeInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeOutProperty: ").append(transactionMilestoning.dateTimeOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (transactionMilestoning instanceof DateTimeTransactionMilestoning)
        {
            builder.append(getTabString(baseIndentation)).append("transactionMilestoning: ").append(transactionMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeInProperty: ").append(transactionMilestoning.dateTimeInName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeOutProperty: ").append(transactionMilestoning.dateTimeOutName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else throw UnsupportedOperationException();

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
            builder.append(getTabString(baseIndentation)).append("validityMilestoning: ").append(validityMilestoning).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeFromProperty: ").append(validityMilestoning.dateTimeFromName).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("dateTimeThruProperty: ").append(validityMilestoning.dateTimeThruName).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else throw UnsupportedOperationException();

        return builder.toString();
    }

    private static String renderValidityDerivation(ValidityDerivation validityDerivation, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (validityDerivation instanceof SourceSpecifiesFromDate)
        {
            builder.append(getTabString(baseIndentation)).append("validityDerivation: ").append(validityDerivation).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeFromProperty: ").append(validityMilestoning.sourceDateTimeFromProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else if (validityDerivation instanceof SourceSpecifiesFromAndThruDate)
        {
            builder.append(getTabString(baseIndentation)).append("validityDerivation: ").append(validityDerivation).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeFromProperty: ").append(validityMilestoning.sourceDateTimeFromProperty).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("sourceDateTimeThruProperty: ").append(validityMilestoning.sourceDateTimeThruProperty).append(";\n");
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else throw UnsupportedOperationException();

        return builder.toString();
    }

    private static String renderMergeStrategy(MergeStrategy mergeStrategy, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();

        if (mergeStrategy instanceof (OpaqueMergeStrategy || NoDeletesMergeStrategy))
        {
            builder.append(getTabString(baseIndentation)).append("mergeStrategy: ").append(mergeStrategy).append(";\n");
        }
        else if (mergeStrategy instanceof DeleteIndicatorMergeStrategy)
        {
            builder.append(getTabString(baseIndentation)).append("mergeStrategy: ").append(mergeStrategy).append("\n");
            builder.append(getTabString(baseIndentation)).append("{\n");
            builder.append(getTabString(baseIndentation+1)).append("deleteProperty: ").append(mergeStrategy.deleteProperty).append(";\n");
            builder.append(getTabString(baseIndentation+1)).append("deleteValues: ");
            if (!mergeStrategy.deleteValues.isEmpty())
            {
                builder.append("[").append(LazyIterate.collect(mergeStrategy.deleteValues, d -> convertString(d, true)).makeString(", ")).append("];\n");
            }
            else {builder.append("[];\n");}
            builder.append(getTabString(baseIndentation)).append("}\n");
        }
        else throw UnsupportedOperationException();

        return builder.toString();
    }
    */
}
