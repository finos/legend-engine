package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class IdentifiedSecurityScheme extends SecurityScheme{

    //TODO: Refactor
    public String id;
    public SourceInformation sourceInformation;

    @Override
    public <T> T accept(SecuritySchemeVisitor<T> securitySchemeVisitor) {
        return null;
    }
}
