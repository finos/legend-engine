package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model;

public interface SecuritySchemeVisitor<T>
{
    T visit(SecurityScheme securityScheme);
}

