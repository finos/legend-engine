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

package org.finos.legend.engine.test.runner.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.MultiResult;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.result.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Result;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TDSColumn_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TDSNull;
import org.finos.legend.pure.generated.Root_meta_pure_tds_TDSNull_Impl;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSet;

public class ResultToPureResultVisitor implements RelationalResultVisitor<Root_meta_pure_mapping_Result<Object>>
{
    @Override
    public Root_meta_pure_mapping_Result<Object> visit(ErrorResult errorResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(VoidRelationalResult voidRelationalResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(RealizedRelationalResult realizedRelationalResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(RelationalResult relationalResult)
    {
        // Build Pure Result
        if (relationalResult.builder instanceof TDSBuilder)
        {
            Root_meta_pure_mapping_Result<Object> res = new org.finos.legend.pure.generated.Root_meta_pure_mapping_Result_Impl<Object>("Res");
            try
            {
                org.finos.legend.pure.generated.Root_meta_pure_tds_TabularDataSet set = new org.finos.legend.pure.generated.Root_meta_pure_tds_TabularDataSet_Impl("TDS");
                ResultSet rset = relationalResult.resultSet;
                MutableList<Function<Object, Object>> transformers = relationalResult.getTransformers();
                Root_meta_pure_tds_TDSNull tdsNull = new Root_meta_pure_tds_TDSNull_Impl("");
                while (rset.next())
                {
                    org.finos.legend.pure.generated.Root_meta_pure_tds_TDSRow row = new org.finos.legend.pure.generated.Root_meta_pure_tds_TDSRow_Impl("");
                    for (int i = 1; i <= relationalResult.columnCount - 1; i++)
                    {
                        Object obj = relationalResult.getValue(i);
                        row._valuesAdd(obj == null ? tdsNull : transformers.get(i - 1).valueOf(obj));
                    }
                    Object obj = relationalResult.getValue(relationalResult.columnCount);
                    row._valuesAdd(obj == null ? tdsNull : transformers.get(relationalResult.columnCount - 1).valueOf(obj));

                    row._parent(set);
                    set._rowsAdd(row);
                }
                for (int i = 0; i < relationalResult.columnCount; i++)
                {
                    TDSColumn col = relationalResult.getTdsColumns().get(i);
                    set._columnsAdd(new Root_meta_pure_tds_TDSColumn_Impl(col.name)._offset((long) i)._name(col.name));
                }
                res._valuesAdd(set);
                return res;
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            throw new RuntimeException("To Code");
        }
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(StreamingObjectResult tStreamingObjectResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(JsonStreamingResult jsonStreamingResult)
    {
        Root_meta_pure_mapping_Result<Object> res = new org.finos.legend.pure.generated.Root_meta_pure_mapping_Result_Impl<Object>("Res");
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024))
        {
            JsonFactory factory = new JsonFactory();
            try (JsonGenerator generator = factory.createGenerator(byteArrayOutputStream))
            {
                jsonStreamingResult.getJsonStream().accept(generator);
            }
            finally
            {
                jsonStreamingResult.close();
            }
            res._valuesAdd(byteArrayOutputStream.toString());
            return res;
        }
        catch (Exception e)
        {
            jsonStreamingResult.close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(TempTableStreamingResult tempTableStreamingResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(ConstantResult constantResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(MultiResult multiResult)
    {
        throw new RuntimeException("Not supported!");
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(SQLExecutionResult sqlExecutionResult)
    {
        return null;
    }

    @Override
    public Root_meta_pure_mapping_Result<Object> visit(StreamingResult streamingResult)
    {
        Root_meta_pure_mapping_Result<Object> res = new org.finos.legend.pure.generated.Root_meta_pure_mapping_Result_Impl<Object>("Res");
        String output = streamingResult.flush(streamingResult.getSerializer(SerializationFormat.DEFAULT));
        res._valuesAdd(output);
        return res;
    }
}
