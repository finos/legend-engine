package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.BatchPersistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.streaming.StreamingPersistence;

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
        builder.append(getTabString(baseIndentation)).append("inputShape: ").append(streaming.inputShape).append(";\n");
        builder.append(getTabString(baseIndentation)).append("inputClass: ").append(streaming.inputClassPath).append(";\n");
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
        builder.append(getTabString(baseIndentation)).append("inputClass: ").append(batch.inputClassPath).append(";\n");
        builder.append(getTabString(baseIndentation)).append("transactionMode: ").append(batch.transactionMode).append(";\n");

        if (batch.targetSpecification instanceof BatchDatastoreSpecification)
        {
            builder.append(getTabString(baseIndentation)).append(renderDatastore((BatchDatastoreSpecification) batch.targetSpecification, context));
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
            builder.append(LazyIterate.collect(dataset.partitionProperties, p -> convertString(p, true)).makeString(", "));
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
        builder.append(getTabString(baseIndentation)+1).append("mergeScheme: ").append(milestoningMode.mergeStrategy).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("transactionMilestoning: ").append(milestoningMode.transactionMilestoning).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("validityMilestoning: ").append(milestoningMode.validityMilestoning).append(";\n");
        builder.append(getTabString(baseIndentation)+1).append("validityDerivation: ").append(milestoningMode.validityDerivation).append(";\n");
        builder.append(getTabString(baseIndentation)).append("}\n");
        return builder.toString();
    }
    */
}
