package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch;

public interface BatchMilestoningModeVisitor<T>
{
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.AppendOnlyNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.BitemporalDeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.snapshot.BitemporalSnapshotMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.DeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.DeltaNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.Milestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.NonMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.snapshot.SnapshotMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.nonmilestoned.SnapshotNonMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.delta.UnitemporalDeltaMilestoned val);
    T visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.milestoned.snapshot.UnitemporalSnapshotMilestoned val);
}