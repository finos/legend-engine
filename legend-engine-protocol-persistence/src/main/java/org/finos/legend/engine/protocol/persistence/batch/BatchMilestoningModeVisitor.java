package org.finos.legend.engine.protocol.persistence.batch;

public interface BatchMilestoningModeVisitor<T>
{
    T visit(org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.AppendOnlyNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.BitemporalDeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.snapshot.BitemporalSnapshotMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.DeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.DeltaNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.Milestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.NonMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.snapshot.SnapshotMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.nonmilestoned.SnapshotNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.delta.UnitemporalDeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.persistence.batch.milestoned.snapshot.UnitemporalSnapshotMilestoned val);
}