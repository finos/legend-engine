package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.TestDataSource;

public class RelationalInputData extends InputData
{
    public String database;
    @Deprecated
    public String data;
    public TestDataSource testDataSource;
    public RelationalInputType inputType;
}
