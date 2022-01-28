package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend.aws;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend.Backend;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend.BackendVisitor;

public class AwsGlueBackend extends Backend
{
    public <T> T accept(BackendVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}