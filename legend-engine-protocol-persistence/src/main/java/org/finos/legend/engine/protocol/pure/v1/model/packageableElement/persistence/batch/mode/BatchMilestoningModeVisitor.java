package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.batch.mode.snapshot.UnitemporalSnapshot;

public interface BatchMilestoningModeVisitor<T>
{
    T visit(AppendOnly val);
    T visit(BitemporalDelta val);
    T visit(BitemporalSnapshot val);
    T visit(NonMilestonedDelta val);
    T visit(NonMilestonedSnapshot val);
    T visit(UnitemporalDelta val);
    T visit(UnitemporalSnapshot val);
}