package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

import java.util.List;

public class OauthSecurityScheme extends SecurityScheme
{

    public List<String> scopes;

    @Override
    public <T> T accept(SecuritySchemeVisitor<T> securitySchemeVisitor)
    {
        return securitySchemeVisitor.visit(this);
    }
}
