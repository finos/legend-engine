//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is added to help users to migrate to standard format and will be removed in upcoming releases.
 * TODO: Remove this.
 */
@Deprecated
public class RelationalResultToCSVSerializerWithoutISODateTransformations extends CsvSerializer
{
    private final RelationalResult relationalResult;
    private final CSVFormat csvFormat;

    public RelationalResultToCSVSerializerWithoutISODateTransformations(RelationalResult relationalResult)
    {
        this(relationalResult, false);
    }

    public RelationalResultToCSVSerializerWithoutISODateTransformations(RelationalResult relationalResult, boolean withHeader)
    {
        this(relationalResult, (withHeader ? CSVFormat.DEFAULT.withHeader(relationalResult.getColumnListForSerializer().toArray(new String[0])) : CSVFormat.DEFAULT));
    }

    public RelationalResultToCSVSerializerWithoutISODateTransformations(RelationalResult relationalResult, CSVFormat csvFormat)
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
            this.printRecords(relationalResult.resultSet, csvPrinter);
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

    private void printRecords(ResultSet resultSet, CSVPrinter csvPrinter) throws SQLException, IOException
    {
        MutableList<Function<Object, Object>> transformers = relationalResult.getTransformersWithoutDateTransformations();

        int columnCount = resultSet.getMetaData().getColumnCount();

        while (resultSet.next())
        {
            for (int i = 1; i <= columnCount; ++i)
            {
                csvPrinter.print(transformers.get(i - 1).valueOf(relationalResult.getValue(i)));
            }
            csvPrinter.println();
        }
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        return relationalResult.getSQLResultColumns().stream().map(SQLResultColumn::labelTypePair).map(e -> Tuples.pair(e.getOne(), e.getTwo())).collect(Collectors.toList());
    }
}
