package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.BatchMilestoningModeVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.transactionmilestoned.TransactionMilestoningScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.ValidityMilestoningScheme;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.validitymilestoned.derivation.ValidityDerivation;

public class BitemporalSnapshot extends BatchMilestoningMode
{
    public TransactionMilestoningScheme transactionMilestoningScheme;
    public ValidityMilestoningScheme validityMilestoningScheme;
    public ValidityDerivation validityDerivation;
    public SourceInformation sourceInformation;

    public <T> T accept(BatchMilestoningModeVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}