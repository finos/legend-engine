package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.targetspecification;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public abstract class TargetSpecification
{
    public String targetName;
    public String modelClassPath;
    public SourceInformation sourceInformation;

    public abstract <T> T accept(TargetSpecificationVisitor<T> visitor);
}