package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend;

public interface BackendVisitor<T> {
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend.aws.AwsGlueBackend val);
}
