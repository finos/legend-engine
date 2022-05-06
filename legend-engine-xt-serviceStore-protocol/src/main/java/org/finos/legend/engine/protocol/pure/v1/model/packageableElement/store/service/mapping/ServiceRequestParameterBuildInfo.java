package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

public class ServiceRequestParameterBuildInfo
{
    public String serviceParameter;
    public Lambda transform;

    public SourceInformation sourceInformation;
}
