package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

public class SimpleHttpSecurityScheme extends SecurityScheme{

    public String scheme;

    @Override
    public <T> T accept(SecuritySchemeVisitor<T> securitySchemeVisitor)
    {
        return securitySchemeVisitor.visit(this);
    }
}
