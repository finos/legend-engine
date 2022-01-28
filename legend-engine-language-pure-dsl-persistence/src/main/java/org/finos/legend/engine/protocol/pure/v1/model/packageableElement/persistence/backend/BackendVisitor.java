package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend.aws.AwsGlueBackend;

public interface BackendVisitor<T>
{
    T visit(AwsGlueBackend val);
}