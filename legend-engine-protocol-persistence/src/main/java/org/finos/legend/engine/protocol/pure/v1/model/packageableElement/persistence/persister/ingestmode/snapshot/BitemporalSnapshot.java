package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoning;

public class BitemporalSnapshot extends IngestMode
{
    public TransactionMilestoning transactionMilestoning;
    public ValidityMilestoning validityMilestoning;

    public <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}