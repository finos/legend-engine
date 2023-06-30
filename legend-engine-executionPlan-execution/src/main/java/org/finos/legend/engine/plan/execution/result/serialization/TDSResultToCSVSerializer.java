//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.serialization;

import java.io.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.TDSResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;

public class TDSResultToCSVSerializer extends CsvSerializer
{
    private final TDSResult tdsResult;
    private final CSVFormat csvFormat;
    private final List<TDSResultToCSVSerializer.TDSColumnWithCSVExtractor> csvValueExtractors;

    public TDSResultToCSVSerializer(TDSResult tdsResult, boolean withHeader)
    {
        this(tdsResult, (withHeader ? CSVFormat.DEFAULT.withHeader(tdsResult.getResultBuilder().columns.stream().map(x -> x.name).toArray(String[]::new)) : CSVFormat.DEFAULT));
    }

    public TDSResultToCSVSerializer(TDSResult tdsResult, CSVFormat csvFormat)
    {
        this.tdsResult = tdsResult;
        this.csvFormat = csvFormat;
        this.csvValueExtractors = this.tdsResult.getResultBuilder().columns.stream().map(TDSResultToCSVSerializer.TDSColumnWithCSVExtractor::new).collect(Collectors.toList());
    }

    @Override
    public void stream(OutputStream targetStream)
    {
        try (Stream<Object[]> rows = tdsResult.rowsStream();
             CSVPrinter csvPrinter = new CSVPrinter(new BufferedWriter(new OutputStreamWriter(targetStream)), this.csvFormat)
        )
        {
            this.printRecords(rows, csvPrinter);
            csvPrinter.flush();
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating CSV", e);
        }
    }

    private void printRecords(Stream<Object[]> rows, CSVPrinter csvPrinter)
    {
        rows.forEach(x ->
        {
            try
            {
                for (int i = 0; i < this.csvValueExtractors.size(); i++)
                {
                    csvPrinter.print(this.csvValueExtractors.get(i).valueOf(x[i]));
                }
                csvPrinter.println();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        return this.tdsResult.getResultBuilder().columns.stream().map(x -> Tuples.pair(x.name, x.type)).collect(Collectors.toList());
    }

    private static class TDSColumnWithCSVExtractor
    {
        private final Function<Object, Object> csvValueExtractor;

        public TDSColumnWithCSVExtractor(TDSColumn tdsColumn)
        {
            this.csvValueExtractor = csvValueExtractor(tdsColumn.type);
        }

        private Object valueOf(Object value)
        {
            if (value == null)
            {
                return null;
            }
            else
            {
                return this.csvValueExtractor.apply(value);
            }
        }

        private static Function<Object, Object> csvValueExtractor(String type)
        {
            switch (type)
            {
                case "String":
                    return String.class::cast;
                case "Integer":
                    return ((Function<Object, Number>) Number.class::cast).andThen(Number::longValue);
                case "Float":
                case "Number":
                    return ((Function<Object, Number>) Number.class::cast).andThen(Number::doubleValue);
                case "Decimal":
                    return BigDecimal.class::cast;
                case "Boolean":
                    return Boolean.class::cast;
                case "Date":
                case "DateTime":
                    return ((Function<Object, PureDate>) PureDate.class::cast).andThen(PureDate::toInstant).andThen(Instant::toString);
                case "StrictDate":
                    return ((Function<Object, PureDate>) PureDate.class::cast).andThen(PureDate::toLocalDate).andThen(LocalDate::toString);
                default:
                    throw new UnsupportedOperationException("TDS type not supported: " + type);
            }
        }
    }
}
