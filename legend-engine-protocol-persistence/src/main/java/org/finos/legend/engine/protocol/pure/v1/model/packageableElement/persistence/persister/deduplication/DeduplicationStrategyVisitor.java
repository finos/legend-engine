package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication;

public interface DeduplicationStrategyVisitor<T>
{
    T visit(AnyVersionDeduplicationStrategy val);
    T visit(MaxVersionDeduplicationStrategy val);
    T visit(NoDeduplicationStrategy val);
    T visit(OpaqueDeduplicationStrategy val);
}