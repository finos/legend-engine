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
import org.apache.commons.lang3.ClassUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.CsvSerializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.TempTableStreamingResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.TempTableColumnMetaData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamingTempTableResultCSVSerializer extends CsvSerializer
{
    private TempTableStreamingResult tempTableStreamingResult;
    private boolean withHeader;
    private CSVFormat csvFormat;

    private Class objectClass;
    private List<Pair<Method, Object[]>> methodWithParametersList = Lists.mutable.empty();
    private List<String> columnLabels;
    private List<String> columnTypes;

    private String timeZone;

    public StreamingTempTableResultCSVSerializer(TempTableStreamingResult tempTableStreamingResult, boolean withHeader)
    {
        this.tempTableStreamingResult = tempTableStreamingResult;
        this.withHeader = withHeader;
        this.csvFormat = this.withHeader ? CSVFormat.DEFAULT.withFirstRecordAsHeader() : CSVFormat.DEFAULT;
    }

    public StreamingTempTableResultCSVSerializer(TempTableStreamingResult tempTableStreamingResult, boolean withHeader, CSVFormat csvFormat)
    {
        this.tempTableStreamingResult = tempTableStreamingResult;
        this.withHeader = withHeader;
        this.csvFormat = csvFormat;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        final Stream<?> inputStream = this.tempTableStreamingResult.inputStream;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Writer out = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(out, this.csvFormat);)
        {
            String connectionTimeZone = this.tempTableStreamingResult.getRelationalDatabaseTimeZone();
            timeZone = connectionTimeZone == null ? TimeZone.getTimeZone("GMT").toString() : connectionTimeZone;

            final List<TempTableColumnMetaData> columns = this.tempTableStreamingResult.tempTableColumnMetaData;
            columnLabels = columns.stream().map(col -> col.column.label).collect(Collectors.toList());
            columnTypes = columns.stream().map(col -> col.column.dataType.toUpperCase()).collect(Collectors.toList());

            final Iterator<?> streamIterator = inputStream.iterator();

            if (this.withHeader)
            {
                csvPrinter.printRecord(columnLabels);
            }
            try
            {
                if (streamIterator.hasNext())
                {
                    Object obj = streamIterator.next();
                    objectClass = obj.getClass();
                    if (!(ClassUtils.isPrimitiveOrWrapper(objectClass) || (objectClass == String.class)))
                    {
                        for (TempTableColumnMetaData key : columns)
                        {
                            try
                            {
                                methodWithParametersList.add(Tuples.pair(objectClass.getMethod(key.identifierForGetter, key.parametersForGetter.keySet().stream().map(String::getClass).toArray(Class[]::new)), key.parametersForGetter.values().toArray()));
                            }
                            catch (Exception e)
                            {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    csvPrinter.printRecord(processRow(obj));
                }

                while (streamIterator.hasNext())
                {
                    Object obj = streamIterator.next();
                    csvPrinter.printRecord(processRow(obj));
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

            csvPrinter.close();
            byteArrayOutputStream.writeTo(targetStream);
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating CSV", e);
        }
        finally
        {
            try
            {
                // We are explicitly closing inputStream here instead of resource block because ideally steam should be closed when iterator finishes
                // but in rare case we are explicitly closing to avoid resource leak
                inputStream.close();
            }
            catch (Exception ignored)
            {

            }
        }
    }

    private List<Object> processRow(Object obj)
    {
        List<Object> valList = Lists.mutable.empty();

        if (ClassUtils.isPrimitiveOrWrapper(objectClass) || (objectClass == String.class))
        {
            if (obj == null)
            {
                valList.add("");
            }
            else
            {
                if (columnTypes.get(0).startsWith("TIMESTAMP"))
                {
                    valList.add(processDateTimeConstantForTimeZone(obj.toString()));
                }
                else
                {
                    valList.add(obj);
                }
            }
        }
        else
        {
            int index = 0;
            for (Pair<Method, Object[]> methodParamPair : methodWithParametersList)
            {
                Object value;
                try
                {
                    value = methodParamPair.getOne().invoke(obj, methodParamPair.getTwo());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                if (value == null)
                {
                    valList.add("");
                }
                else
                {
                    if (columnTypes.get(index).startsWith("TIMESTAMP"))
                    {
                        valList.add(processDateTimeConstantForTimeZone(value.toString()));
                    }
                    else
                    {
                        valList.add(value);
                    }
                }
                index++;
            }
        }
        return valList;
    }

    private String processDateTimeConstantForTimeZone(String date)
    {
        Map var = Maps.mutable.with("dt", date);
        String template = "${GMTtoTZ(\"[" + this.timeZone + "]\" dt )}";
        String freeMarkerAlloyDateFunction = "<#function GMTtoTZ tz paramDate><#return (tz+\" \"+paramDate)?date.@alloyDate></#function>";
        return FreeMarkerExecutor.processRecursively(template, var, freeMarkerAlloyDateFunction);
    }

    @Override
    public List<Pair<String, String>> getHeaderColumnsAndTypes()
    {
        return this.tempTableStreamingResult.tempTableColumnMetaData.stream().map(t -> t.column).map(SQLResultColumn::labelTypePair).map(e -> Tuples.pair(e.getOne(), e.getTwo())).collect(Collectors.toList());
    }
}
