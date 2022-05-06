package org.finos.legend.engine.external.format.json.read;

import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;

import java.io.InputStream;

public interface IJsonDeserializeExecutionNodeSpecifics
{
    IStoreStreamReader streamReader(InputStream context);
}
