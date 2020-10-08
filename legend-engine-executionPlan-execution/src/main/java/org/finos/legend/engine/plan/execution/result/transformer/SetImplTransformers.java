// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.transformer;

import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class SetImplTransformers
{
    public static final Function<Object, Object> TEMPORARY_DATATYPE_TRANSFORMER = (Function<Object, Object>) o ->
    {
        if (o instanceof Timestamp)
        {
            return DateFunctions.fromSQLTimestamp((Timestamp) o);
        }
        if (o instanceof java.sql.Date)
        {
            return StrictDate.fromSQLDate((java.sql.Date) o);
        }
        if (o instanceof Date)
        {
            PureDate pureDate = PureDate.fromDate((Date) o);
            return pureDate.toString();
        }
        return o;
    };

    public MutableList<Function<Object, Object>> transformers;

    public <T> SetImplTransformers(List<TransformerInput<T>> transformerInputs)
    {
        transformers = ListIterate.collect(transformerInputs, this::buildTransformer);
    }

    public SetImplTransformers()
    {
        transformers = Lists.mutable.empty();
    }

    private <T> Function<Object, Object> buildTransformer(TransformerInput<T> transformerInput)
    {
        if (transformerInput.type != null && transformerInput.test.valueOf(transformerInput.identifier))
        {
            return transformerInput.transformer.valueOf(transformerInput.identifier);
        }
        else if (transformerInput.type != null && transformerInput.type.equals("Boolean"))
        {
            return o -> o == null ? null : o instanceof Boolean ? o : Boolean.parseBoolean((String) o);
        }
        else if (transformerInput.type != null && (transformerInput.type.equals("StrictDate") || transformerInput.type.equals("DateTime") || transformerInput.type.equals("Date")))
        {
            return o -> o instanceof Date ? DateFunctions.fromDate((Date) o) : o;
        }
        else
        {
            return o -> o;
        }
    }

}
