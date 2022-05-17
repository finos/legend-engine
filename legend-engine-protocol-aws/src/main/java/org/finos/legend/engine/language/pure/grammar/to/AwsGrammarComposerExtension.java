package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.AwsFinCloudGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.AwsGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.*;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AwsGrammarComposerExtension implements IAwsGrammarComposerExtension
{


    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof S3Connection) {
                PureGrammarComposerContext ctx = PureGrammarComposerContext.Builder.newInstance(context).build();
                S3Connection s3Connection = (S3Connection) connectionValue;
                int baseIndentation = 0;

                return Tuples.pair(AwsGrammarParserExtension.AWS_S3_CONNECTION_TYPE, context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (s3Connection.element != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "store: " + s3Connection.element + ";\n") : "") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "partition: " + s3Connection.partition + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "region: '" + s3Connection.region + "';\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "bucket: '" + s3Connection.bucket + "';\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "key: '" + s3Connection.key + "';\n" +
                        context.getIndentationString() + "}");
            }
            else if (connectionValue instanceof FinCloudConnection) {
                PureGrammarComposerContext ctx = PureGrammarComposerContext.Builder.newInstance(context).build();
                FinCloudConnection finCloudConnection = (FinCloudConnection) connectionValue;
                int baseIndentation = 0;

                List<IAwsGrammarComposerExtension> extensions = IAwsGrammarComposerExtension.getExtensions(context);

                String authenticationStrategy = IAwsGrammarComposerExtension.process(finCloudConnection.authenticationStrategy,
                        ListIterate.flatCollect(extensions, IAwsGrammarComposerExtension::getExtraAuthenticationStrategyComposers),
                        context);

                //String specification = IAwsGrammarComposerExtension.process(finCloudConnection.targetSpecification,
                //        ListIterate.flatCollect(extensions, IAwsGrammarComposerExtension::getExtraDataSourceSpecificationComposers),
                //        context);

                return Tuples.pair(AwsFinCloudGrammarParserExtension.AWS_FIN_CLOUD_CONNECTION_TYPE, context.getIndentationString() + getTabString(baseIndentation) + "{\n" +
                        (finCloudConnection.element != null ? (context.getIndentationString() + getTabString(baseIndentation + 1) + "store: " + finCloudConnection.element + ";\n") : "") +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "datasetId: '" + finCloudConnection.datasetId + "';\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "authenticationStrategy: " + authenticationStrategy + ";\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "apiUrl: '" + finCloudConnection.apiUrl + "';\n" +
                        context.getIndentationString() + getTabString(baseIndentation + 1) + "queryInfo: '" + finCloudConnection.queryInfo + "';\n" +
                        context.getIndentationString() + "}");
            }
            return null;
        });
    }

}