package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

public class PackageableElementPtr extends ValueSpecification
{
    public String fullPath;

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
