package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EmailNotifyee.class, name = "emailNotifyee"),
        @JsonSubTypes.Type(value = PagerDutyNotifyee.class, name = "pagerDutyNotifyee")
})
public abstract class Notifyee
{
    public SourceInformation sourceInformation;

    public abstract <T> T acceptVisitor(NotifyeeVisitor<T> visitor);
}