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

import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.result.ResultNormalizer;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

public class RealizedRelationalResultCSVSerializer extends CsvSerializer
{
    private RealizedRelationalResult realizedRelationalResult;
    private String databaseTimeZone;
    private boolean withHeader;
    private boolean withTransform;
    private CSVFormat csvFormat;

    public RealizedRelationalResultCSVSerializer(RealizedRelationalResult realizedRelationalResult, String databaseTimeZone)
    {
        this(realizedRelationalResult, databaseTimeZone, false, false, CSVFormat.DEFAULT);
    }

    public RealizedRelationalResultCSVSerializer(RealizedRelationalResult realizedRelationalResult, String databaseTimeZone, boolean withHeader, boolean withTransform)
    {
        this(realizedRelationalResult, databaseTimeZone, withHeader, withTransform, CSVFormat.DEFAULT);
    }

    public RealizedRelationalResultCSVSerializer(RealizedRelationalResult realizedRelationalResult, String databaseTimeZone, boolean withHeader, boolean withTransform, CSVFormat csvFormat)
    {
        this.realizedRelationalResult = realizedRelationalResult;
        this.withHeader = withHeader;
        this.withTransform = withTransform;
        this.databaseTimeZone = databaseTimeZone;
        this.csvFormat = csvFormat;
    }

    @Override
    public void stream(OutputStream targetStream)
    {
        {
            CSVPrinter csvPrinter = null;
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            List<String> columns = this.realizedRelationalResult.columns.stream().map(x -> x.label).collect(Collectors.toList());
            List<List<Object>> rows = this.withTransform ? this.realizedRelationalResult.transformedRows : this.realizedRelationalResult.resultSetRows;
            try
            {
                Writer out = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
                if (this.withHeader)
                {
                    csvPrinter = new CSVPrinter(out, this.csvFormat.withFirstRecordAsHeader());
                    csvPrinter.printRecord(columns);
                }
                else
                {
                    csvPrinter = new CSVPrinter(out, this.csvFormat);
                }
                for (List<Object> row : rows)
                {
                    List<Object> normalizedRow = row.stream().map(x -> x != null ? ResultNormalizer.normalizeToSql(x, this.databaseTimeZone) : null).collect(Collectors.toList());
                    csvPrinter.printRecord(normalizedRow);
                }

                csvPrinter.close();
                byteArrayOutputStream.writeTo(targetStream);

            }
            catch (Exception e)
            {
                throw new RuntimeException("Error creating CSV", e);
            }
            finally
            {
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
                try
                {
                    byteArrayOutputStream.close();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        return this.realizedRelationalResult.columns.stream().map(SQLResultColumn::labelTypePair).map(e -> Tuples.pair(e.getOne(), e.getTwo())).collect(Collectors.toList());
    }
}
