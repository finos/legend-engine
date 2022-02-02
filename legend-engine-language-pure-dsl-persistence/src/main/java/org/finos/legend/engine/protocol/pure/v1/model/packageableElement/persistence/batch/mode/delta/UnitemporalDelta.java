package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.merge.MergeScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned.TransactionMilestoningScheme;

public class UnitemporalDelta extends BatchMilestoningMode
{
    public MergeScheme mergeScheme;
    public TransactionMilestoningScheme transactionMilestoningScheme;
    public SourceInformation sourceInformation;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}