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

package org.finos.legend.engine.external.format.xml.read.valueProcessors;

import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.read.ValueProcessor;
import org.finos.legend.engine.external.format.xml.shared.XmlUtils;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AddEnumToObject<T, V extends Enum<?>> implements ValueProcessor<T>
{
    private final SimpleTypeHandler<String> handler;
    private final Class<V> clazz;
    private final ExternalDataObjectAdder<T, V> dataAdder;
    private final Method getNameMethod;
    private final V[] enumConstants;
    private String typePath;

    public AddEnumToObject(ExternalDataAdder<T> dataAdder, SimpleTypeHandler<String> handler, Class<V> clazz)
    {
        this(dataAdder, handler, clazz, "");
    }

    public AddEnumToObject(ExternalDataAdder<T> dataAdder, SimpleTypeHandler<String> handler, Class<V> clazz, String typePath)
    {
        this.dataAdder = (ExternalDataObjectAdder<T, V>) dataAdder;
        this.handler = handler;
        this.clazz = clazz;
        try
        {
            this.getNameMethod = clazz.getMethod("getName");
        }
        catch (NoSuchMethodException e)
        {
            throw new RuntimeException("getName does not exist in : " + clazz.getSimpleName(), e);
        }
        this.enumConstants = clazz.getEnumConstants();
        this.typePath = typePath;
    }

    @Override
    public void process(DeserializeContext<?> context, String rawValue)
    {
        String text = handler.parse(rawValue);

        String textWithoutPath = text.startsWith(typePath) ? text.substring(typePath.length()) : text;
        V value = null;
        for (V v : enumConstants)
        {
            try
            {
                if (XmlUtils.lenientMatch((String) getNameMethod.invoke(v), textWithoutPath))
                {
                    value = v;
                }
            }
            catch (IllegalAccessException | InvocationTargetException e)
            {
                throw new RuntimeException("Not able to execute getName() on Enum " + clazz.getSimpleName(), e);
            }
        }
        if (value == null)
        {
            throw new IllegalArgumentException("No enum value matches " + text);
        }
        context.addValue(dataAdder, value);
    }
}
