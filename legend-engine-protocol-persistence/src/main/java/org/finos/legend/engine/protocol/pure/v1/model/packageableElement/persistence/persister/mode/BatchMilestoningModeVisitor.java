package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.NonMilestonedDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.NonMilestonedSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.mode.snapshot.UnitemporalSnapshot;

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