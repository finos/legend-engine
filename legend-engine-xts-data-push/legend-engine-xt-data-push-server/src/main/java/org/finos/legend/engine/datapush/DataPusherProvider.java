package org.finos.legend.engine.datapush;

import org.finos.legend.connection.StoreInstance;

public interface DataPusherProvider
{
    DataPusher getDataPusher(StoreInstance connectionInstance);
}
