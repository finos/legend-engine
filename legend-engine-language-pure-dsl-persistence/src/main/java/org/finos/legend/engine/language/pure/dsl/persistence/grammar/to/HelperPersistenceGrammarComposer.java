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
    public static String renderOwners(List<String> owners)
    {
        StringBuilder builder = new StringBuilder();
        if (!owners.isEmpty())
        {
            builder.append("owners: ").append("[").append(LazyIterate.collect(owners, o -> convertString(o, true)).makeString(", ")).append("];\n");
        }
        return builder.toString();
    }

    public static String renderPersistence(Persistence persistence, PureGrammarComposerContext context)
    {
        if (persistence instanceof BatchPersistence)
        {
            return renderBatchPersistence((BatchPersistence) persistence, context)
        }
        else if (persistence instanceof StreamingPersistence)
        {
            return renderStreamingPersistence((StreamingPersistence) persistence, context)
        }
        throw new UnsupportedOperationException();
    }

    private static String renderStreamingPersistence(StreamingPersistence streaming, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Streaming\n");
        builder.append(getTabString()).append("{\n");
        builder.append(getTabString(baseIndentation)).append("inputShape: ").append(streaming.inputShape).append(";\n");
        builder.append(getTabString(baseIndentation)).append("inputClass: ").append(streaming.inputClass).append(";\n");
        builder.append(getTabString()).append("}");

        return builder.toString();
    }

    private static String renderBatchPersistence(BatchPersistence batch, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("persistence: Batch\n");
        builder.append(getTabString()).append("{\n");
        builder.append(getTabString(baseIndentation)).append("inputShape: ").append(batch.inputShape).append(";\n");
        builder.append(getTabString(baseIndentation)).append("inputClass: ").append(batch.inputClass).append(";\n");
        builder.append(getTabString(baseIndentation)).append("transactionMode: ").append(batch.transactionMode).append(";\n");

        if (batch.targetSpecification instanceof BatchDatastoreSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderDatastore((BatchDatastoreSpecification) persistence.targetSpecification, context));
        }

        builder.append(getTabString()).append("}");

        return builder.toString();
    }

    private static String renderDatastore(BatchDatastoreSpecification datastore, PureGrammarComposerContext context)
    {
        int baseIndentation = 3;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()+1).append("target: Datastore\n");
        builder.append(getTabString()+1).append("{\n");
        builder.append(getTabString(baseIndentation)).append("datastoreName: ").append(datastore.datastoreName).append(";\n");
        builder.append(getTabString(baseIndentation)).append("datasets:").append("\n");
        builder.append(getTabString(baseIndentation)).append("[\n");
        ListIterate.forEachWithIndex(datastore.datasets, (dataset, i) ->
        {
            builder.append(getTabString(baseIndentation + 1)).append("{\n");
            builder.append(renderDataset(dataset, baseIndentation + 2, context));
            builder.append(getTabString(baseIndentation + 1)).append(i < datastore.datasets.size() - 1 ? "},\n" : "}\n");
        });
        builder.append(getTabString(baseIndentation)).append("];\n");

        builder.append(getTabString()+1).append("}\n");

        return builder.toString();
    }

    private static String renderDataset(BatchDatasetSpecification dataset, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("datasetName: ").append(dataset.datasetName).append(";\n");
        builder.append(getTabString(baseIndentation)).append("partitionProperties: ").append("[");
        if (!dataset.partitionProperties.isEmpty())
        {
            builder.append(LazyIterate.collect(partitionProperties, p -> convertString(p, true)).makeString(", "));
        }
        builder.append("];\n");
        builder.append(getTabString(baseIndentation)).append("deduplicationStrategy: ").append(dataset.deduplicationStrategy).append(";\n");

        if (dataset.milestoningMode instanceof BitemporalDelta)
        {
            builder.append(getTabString(baseIndentation)).append(renderBitemporalDelta((BitemporalDelta) dataset.milestoningMode, context));
        }
        // to add other modes

        return builder.toString();
    }

    private static String renderBitemporalDelta(BitemporalDelta milestoningMode, PureGrammarComposerContext context)
    {
        int baseIndentation = 5;
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString(baseIndentation)).append("batchMode: BitemporalDelta\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(getTabString(baseIndentation)+1).append("mergeScheme: ").append(milestoningMode.mergeScheme).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("transactionMilestoning: ").append(milestoningMode.transactionMilestoningScheme).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("validityMilestoning: ").append(milestoningMode.validityMilestoningScheme).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("validityDerivation: ").append(milestoningMode.validityDerivation).append(";\n");
        builder.append(getTabString(baseIndentation)).append("}\n");

        return builder.toString();
    }

}
