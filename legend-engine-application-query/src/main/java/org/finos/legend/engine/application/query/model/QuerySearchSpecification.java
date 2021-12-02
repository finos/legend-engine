package org.finos.legend.engine.application.query.model;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;

import java.util.List;

public class QuerySearchSpecification
{
    public String searchTerm;
    public List<QueryProjectCoordinates> projectCoordinates;
    public List<TaggedValue> taggedValues;
    public List<StereotypePtr> stereotypes;
    public Integer limit;
    public Boolean showCurrentUserQueriesOnly;
}
