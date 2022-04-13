package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = AWS.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AWS.class, name = "AWS"),
        @JsonSubTypes.Type(value = AWS_CN.class, name = "AWS_CN"),
        @JsonSubTypes.Type(value = AWS_US_GOV.class, name = "AWS_US_GOV"),
})
public abstract class AwsPartition
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AwsPartitionVisitor<T> visitor);
}