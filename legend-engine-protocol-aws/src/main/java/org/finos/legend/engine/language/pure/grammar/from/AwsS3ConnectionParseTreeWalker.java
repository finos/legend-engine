package org.finos.legend.engine.language.pure.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.antlr4.AwsS3ConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AwsPartition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AWS;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AWS_US_GOV;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AWS_CN;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.AwsPartitionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

public class AwsS3ConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public AwsS3ConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    /**********
     * s3Connection
     **********/

    public void visitAwsS3ConnectionValue(AwsS3ConnectionParserGrammar.DefinitionContext ctx, S3Connection awsS3Connection)
    {
        // store (to change to not applicable?)
        AwsS3ConnectionParserGrammar.ConnectionStoreContext storeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.connectionStore(), "store", awsS3Connection.sourceInformation);
        if (storeContext != null)
        {
            awsS3Connection.element = PureGrammarParserUtility.fromQualifiedName(storeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : storeContext.qualifiedName().packagePath().identifier(), storeContext.qualifiedName().identifier());
            awsS3Connection.elementSourceInformation = this.walkerSourceInformation.getSourceInformation(storeContext.qualifiedName());
        }

        // partition
        AwsS3ConnectionParserGrammar.PartitionContext partitionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.partition(), "partition", awsS3Connection.sourceInformation);
        awsS3Connection.partition = visitPartition(partitionContext);

        // region
        AwsS3ConnectionParserGrammar.RegionContext regionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.region(), "region", awsS3Connection.sourceInformation);
        awsS3Connection.region = PureGrammarParserUtility.fromGrammarString(regionContext.STRING().getText(), true);

        // bucket
        AwsS3ConnectionParserGrammar.BucketContext bucketContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.bucket(), "bucket", awsS3Connection.sourceInformation);
        awsS3Connection.bucket = PureGrammarParserUtility.fromGrammarString(bucketContext.STRING().getText(), true);

    }

    /**********
     * partition
     **********/

    private AwsPartition visitPartition(AwsS3ConnectionParserGrammar.PartitionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        if (ctx.AWS() != null)
        {
            return new AWS();
        }
        else if (ctx.AWS_CN() != null)
        {
            return new AWS_CN();
        }
        else if (ctx.AWS_US_GOV() != null)
        {
            return new AWS_US_GOV();
        }
        throw new EngineException("Unrecognized partition", sourceInformation, EngineErrorType.PARSER);
    }

}
