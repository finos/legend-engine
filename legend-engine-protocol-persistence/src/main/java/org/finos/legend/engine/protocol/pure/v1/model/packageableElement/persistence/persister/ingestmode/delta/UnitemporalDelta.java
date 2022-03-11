package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoning;

public class UnitemporalDelta extends IngestMode
{
    public MergeStrategy mergeStrategy;
    public TransactionMilestoning transactionMilestoning;

    public <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}