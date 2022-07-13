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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.Interval;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    @Deprecated
    public static final Function<Object, Object> TEMPORARY_DATATYPE_TRANSFORMER_WITHOUT_ISO_FORMAT = (Function<Object, Object>) o ->
    {
        if (o instanceof Timestamp)
        {
            return formatTimestamp((Timestamp) o);
        }
        return o;
    };

    public MutableList<Function<Object, Object>> transformers;

    public SetImplTransformers()
    {
        transformers = Lists.mutable.empty();
    }

    public <T> SetImplTransformers(List<TransformerInput<T>> transformerInputs)
    {
        transformers = ListIterate.collect(transformerInputs, this::buildTransformer);
    }

    /**
     * This is added to help users to migrate to standard format and will be removed in upcoming releases.
     * TODO: Remove this.
     */
    @Deprecated
    public <T> SetImplTransformers(List<TransformerInput<T>> transformerInputs, boolean useDateTransformations)
    {
        transformers = ListIterate.collect(transformerInputs, useDateTransformations ? this::buildTransformer : this::buildTransformerWithoutDateTransformations);
    }

    private Boolean toBoolean(Object o)
    {
        if (o == null)
        {
            return null;
        }
        else if (o instanceof Boolean)
        {
            return (Boolean) o;
        }
        else if (o instanceof String)
        {
            return Boolean.parseBoolean((String) o);
        }
        else if (o instanceof Number)
        {
            return ((Number) o).intValue() != 0;
        }
        else
        {
            throw new IllegalArgumentException("Transformer Error: Could not convert to Boolean");
        }
    }

    private <T> Function<Object, Object> buildTransformer(TransformerInput<T> transformerInput)
    {
        if (transformerInput.type != null && transformerInput.test.valueOf(transformerInput.identifier))
        {
            return transformerInput.transformer.valueOf(transformerInput.identifier);
        }
        else if (transformerInput.type != null && transformerInput.type.equals("Boolean"))
        {
            return o -> toBoolean(o);
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

    /**
     * This is added to help users to migrate to standard format and will be removed in upcoming releases.
     * TODO: Remove this.
     */
    @Deprecated
    private <T> Function<Object, Object> buildTransformerWithoutDateTransformations(TransformerInput<T> transformerInput)
    {
        if (transformerInput.type != null && transformerInput.test.valueOf(transformerInput.identifier))
        {
            return transformerInput.transformer.valueOf(transformerInput.identifier);
        }
        else if (transformerInput.type != null && transformerInput.type.equals("Boolean"))
        {
            return o -> toBoolean(o);
        }
        else if (transformerInput.type != null && (transformerInput.type.equals("StrictDate") || transformerInput.type.equals("DateTime") || transformerInput.type.equals("Date")))
        {
            return o -> o instanceof Timestamp ? formatTimestamp((Timestamp) o) : o;
        }
        else
        {
            return o -> o;
        }
    }

    private static final List<DateTimeFormatter> formatters = Interval.fromTo(1, 9).stream().map(i -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss." + StringUtils.repeat('S', i)).withZone(ZoneId.of("UTC"))).collect(Collectors.toCollection(ArrayList::new));

    @Deprecated
    private static String formatTimestamp(Timestamp timestamp)
    {
        /**
         * logic to find trailingZeros copied over from java.sql.Timestamp toString() function
         */

        int trailingZeros = 0;
        int tmpNanos = timestamp.getNanos();

        if (tmpNanos == 0)
        {
            trailingZeros = 8;
        }
        else
        {
            while (tmpNanos % 10 == 0)
            {
                tmpNanos /= 10;
                trailingZeros++;
            }
        }

        int decimals = 9 - trailingZeros;

        return formatters.get(decimals - 1).format(timestamp.toInstant());
    }
}
