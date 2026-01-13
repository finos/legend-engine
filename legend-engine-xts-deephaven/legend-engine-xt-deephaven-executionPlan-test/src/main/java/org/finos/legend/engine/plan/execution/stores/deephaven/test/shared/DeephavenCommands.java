// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.deephaven.test.shared;

import io.deephaven.client.impl.BarrageSnapshot;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.csv.util.CsvReaderException;
import io.deephaven.engine.table.Table;
import io.deephaven.engine.util.TableTools;
import io.deephaven.extensions.barrage.util.BarrageUtil;
import io.deephaven.qst.table.TicketTable;
import org.apache.arrow.flight.FlightStream;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL;
import org.finos.legend.pure.generated.Root_meta_pure_functions_io_http_URL_Impl;

import java.util.concurrent.ExecutionException;

import static org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer.LOGGER;
import static org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer.bufferAllocator;
import static org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer.deephavenSession;

public class DeephavenCommands
{
    private static final int DEEPHAVEN_PORT = 10000;
    public static final String START_SERVER_FUNCTION = "startDeephaven_String_1__URL_1_";
    public static final String START_SERVER_FOR_PCT_FUNCTION = "startDeephavenForPCT_String_1__URL_1_";
    public static final String STOP_SERVER_FUNCTION = "stopDeephaven_String_1__Nil_0_";
    public static final String CREATE_TABLE_FROM_CSV_FUNCTION = "createTableFromCSV_String_1__String_1__Nil_0_";

    public static Root_meta_pure_functions_io_http_URL startServer(String imageTag)
    {
        if (!DeephavenTestContainer.startDeephaven(imageTag))
        {
            throw new RuntimeException("Failed to start Deephaven container");
        }

        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("deephavenUrl");
        String host = DeephavenTestContainer.deephavenContainer.getHost();
        int mappedPort = DeephavenTestContainer.deephavenContainer.getMappedPort(DEEPHAVEN_PORT);
        url._host(host);
        url._port(mappedPort);
        url._path("/");
        return url;
    }

    public static Root_meta_pure_functions_io_http_URL startServerForPCT(String imageTag)
    {
        if (!DeephavenTestContainer.startDeephavenForPCT(imageTag))
        {
            throw new RuntimeException("Failed to start Deephaven container");
        }

        Root_meta_pure_functions_io_http_URL_Impl url = new Root_meta_pure_functions_io_http_URL_Impl("deephavenUrl");
        String host = DeephavenTestContainer.deephavenContainer.getHost();
        int mappedPort = DeephavenTestContainer.deephavenContainer.getMappedPort(DEEPHAVEN_PORT);
        url._host(host);
        url._port(mappedPort);
        url._path("/");
        return url;
    }

    public static void stopServer(String imageTag)
    {
        DeephavenTestContainer.stopDeephaven();
    }

    public static void createTableFromCSV(String tableName, String csv)
    {
        CsvToNewTable csvToNewTable = null;
        try
        {
            csvToNewTable = new CsvToNewTable(csv);
        }
        catch (CsvReaderException e)
        {
            throw new RuntimeException(e);
        }
        try
        {
            csvToNewTable.publish(tableName, bufferAllocator, deephavenSession);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        TableHandle queryTableHandle = deephavenSession.session().batch().of(TicketTable.fromQueryScopeField(tableName));
        FlightStream stream = deephavenSession.stream(queryTableHandle);

        while (stream.next())
        {
            LOGGER.info("row count: " + stream.getRoot().getRowCount());
        }

//            stream.next()
//            stream.getRoot().getRowCount();

        BarrageSnapshot snapshot = deephavenSession.snapshot(queryTableHandle, BarrageUtil.DEFAULT_SNAPSHOT_OPTIONS);
//            snapshot.partialTable()
        Table tableFuture = null;
        try
        {
            tableFuture = snapshot.entireTable().get();
        }
        catch (InterruptedException | ExecutionException e)
        {
            throw new RuntimeException(e);
        }

        TableTools.show(tableFuture);
    }
}
