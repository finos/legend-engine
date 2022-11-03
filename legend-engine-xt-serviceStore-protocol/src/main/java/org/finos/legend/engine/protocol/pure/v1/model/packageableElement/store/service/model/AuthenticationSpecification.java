package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
            @JsonSubTypes.Type(value = OAuthAuthentication.class, name = "oauth"),
            @JsonSubTypes.Type(value = UsernamePasswordAuthentication.class, name = "basic" ),
    })
public class AuthenticationSpecification {

    public SourceInformation sourceInformation;
}
