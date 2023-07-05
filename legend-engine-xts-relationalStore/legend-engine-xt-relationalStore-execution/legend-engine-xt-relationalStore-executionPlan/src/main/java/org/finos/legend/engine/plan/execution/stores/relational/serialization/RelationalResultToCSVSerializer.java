// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class RelationalResultToCSVSerializer extends CsvSerializer
{
    private final RelationalResult relationalResult;
    private final CSVFormat csvFormat;

    public RelationalResultToCSVSerializer(RelationalResult relationalResult)
    {
        this(relationalResult, false);
    }

    public RelationalResultToCSVSerializer(RelationalResult relationalResult, boolean withHeader)
    {
        this.relationalResult = relationalResult;
        if (withHeader)
        {
            this.csvFormat = CSVFormat.DEFAULT.withHeader(relationalResult.getColumnListForSerializer().toArray(new String[0]));
        }
        else
        {
            this.csvFormat = CSVFormat.DEFAULT;
        }
    }

    public RelationalResultToCSVSerializer(RelationalResult relationalResult, CSVFormat csvFormat)
    {
        this.relationalResult = relationalResult;
        this.csvFormat = csvFormat;
    }

    @Override
    public void stream(OutputStream targetStream)
    {
        CSVPrinter csvPrinter = null;
        try
        {
            Writer out = new BufferedWriter(new OutputStreamWriter(targetStream));
            csvPrinter = new CSVPrinter(out, this.csvFormat);
            csvPrinter.printRecords(relationalResult.resultSet);
            csvPrinter.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating CSV", e);
        }
        finally
        {
            relationalResult.close();
            try
            {
                if (csvPrinter != null)
                {
                    csvPrinter.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        return relationalResult.getSQLResultColumns().stream().map(SQLResultColumn::labelTypePair).map(e -> Tuples.pair(e.getOne(), e.getTwo())).collect(Collectors.toList());
    }
}
