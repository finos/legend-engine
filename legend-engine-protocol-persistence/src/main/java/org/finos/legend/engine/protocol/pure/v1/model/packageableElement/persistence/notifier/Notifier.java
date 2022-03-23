package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.notifier;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.util.Collections;
import java.util.List;

public class Notifier
{
    public List<Notifyee> notifyees = Collections.emptyList();
    public SourceInformation sourceInformation;
}