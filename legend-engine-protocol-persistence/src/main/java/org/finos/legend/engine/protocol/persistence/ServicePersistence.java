package org.finos.legend.engine.protocol.persistence;

public class ServicePersistence
{
    public String documentation;
    public java.util.List<String> owners = java.util.Collections.<String>emptyList();
    public org.finos.legend.engine.protocol.persistence.event.EventType trigger;
    public Persistence persistence;
}