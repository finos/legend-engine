package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.targetshape;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.deduplication.DeduplicationStrategy;

import java.util.List;

public class MultiFlatTargetPart
{
    public String modelProperty;
    public String targetName;
    public List<String> partitionFields;
    public DeduplicationStrategy deduplicationStrategy;
    public SourceInformation sourceInformation;
}