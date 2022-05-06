package org.finos.legend.engine.external.format.flatdata.shared.validation;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;

public interface FlatDataValidator
{
    List<FlatDataDefect> validate(FlatData store, FlatDataSection section);
}
