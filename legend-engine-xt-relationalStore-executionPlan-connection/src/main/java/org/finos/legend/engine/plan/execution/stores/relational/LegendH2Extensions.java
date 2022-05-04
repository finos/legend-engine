package org.finos.legend.engine.plan.execution.stores.relational;

import java.util.*;
import org.h2.value.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public class LegendH2Extensions
{
    public static Value legend_h2_extension_json_navigate(Value json, Value property, Value arrayIndex) throws Exception
    {
        if (json == ValueNull.INSTANCE)
        {
            return ValueNull.INSTANCE;
        }

        ObjectMapper mapper = new ObjectMapper();

        Object res;
        if (arrayIndex == ValueNull.INSTANCE)
        {
            res = mapper.readValue(json.getString(), HashMap.class).get(property.getString());
        }
        else
        {
            ArrayList<?> list = mapper.readValue(json.getString(), ArrayList.class);
            res = arrayIndex.getInt() < list.size() ? list.get(arrayIndex.getInt()) : null;
        }

        if (res == null)
        {
            return ValueNull.INSTANCE;
        }
        else if (res instanceof Map || res instanceof List)
        {
            return ValueString.get(mapper.writeValueAsString(res));
        }
        else if (res instanceof String)
        {
            return ValueString.get((String) res);
        }
        else if (res instanceof Boolean)
        {
            return ValueBoolean.get((boolean) res);
        }
        else if (res instanceof Double)
        {
            return ValueDouble.get((double) res);
        }
        else if (res instanceof Float)
        {
            return ValueFloat.get((float) res);
        }
        else if (res instanceof Integer)
        {
            return ValueInt.get((int) res);
        }
        else if (res instanceof Long)
        {
            return ValueLong.get((long) res);
        }

        throw new RuntimeException("Unsupported value in H2 extension function");

    }
}
