package org.finos.legend.engine.protocol.persistence;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;

public class ServicePersistence extends PackageableElement
{
    public String documentation;
    public java.util.List<String> owners = java.util.Collections.<String>emptyList();
    public org.finos.legend.engine.protocol.persistence.event.EventType trigger;
    public Service service;
    public Persistence persistence;

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor) {
        return visitor.visit(this);
    }
}