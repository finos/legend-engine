package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

public class ApiKeySecurityScheme extends SecurityScheme{

    public String location;
    public String keyName;

    @Override
    public <T> T accept(SecuritySchemeVisitor<T> securitySchemeVisitor)
    {
        return securitySchemeVisitor.visit(this);
    }
}
