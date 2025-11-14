package org.finos.legend.engine.query.sql.api.schema;

import java.util.List;

public class SchemaResult
{
    public List<AddressableRelation> addressableRelations;
    public List<C3LinearizationUnitReturn> typeInheritances;

    public SchemaResult()
    {
    }

    public SchemaResult(List<AddressableRelation> addressableRelations, List<C3LinearizationUnitReturn> typeInheritances)
    {
        this.addressableRelations = addressableRelations;
        this.typeInheritances = typeInheritances;
    }
}
