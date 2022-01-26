package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.backend;

public abstract class Backend {
    public abstract <T> T accept(BackendVisitor<T> visitor);
}
