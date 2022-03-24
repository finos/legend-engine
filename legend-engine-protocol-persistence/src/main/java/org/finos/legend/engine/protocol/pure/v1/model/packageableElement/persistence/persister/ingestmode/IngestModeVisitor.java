package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;

public interface IngestModeVisitor<T>
{
    T visit(AppendOnly val);
    T visit(BitemporalDelta val);
    T visit(BitemporalSnapshot val);
    T visit(NontemporalDelta val);
    T visit(NontemporalSnapshot val);
    T visit(UnitemporalDelta val);
    T visit(UnitemporalSnapshot val);
}