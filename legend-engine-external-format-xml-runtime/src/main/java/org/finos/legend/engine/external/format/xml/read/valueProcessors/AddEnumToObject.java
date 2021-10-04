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

public class AddEnumToObject<T, V extends Enum<?>> implements ValueProcessor<T>
{
    private final SimpleTypeHandler<String> handler;
    private final Class<V> clazz;
    private final ExternalDataObjectAdder<T, V> dataAdder;

    public AddEnumToObject(ExternalDataAdder<T> dataAdder, SimpleTypeHandler<String> handler, Class<V> clazz)
    {
        this.dataAdder = (ExternalDataObjectAdder<T, V>) dataAdder;
        this.handler = handler;
        this.clazz = clazz;
    }

    @Override
    public void process(DeserializeContext<?> context, String rawValue)
    {
        String text = handler.parse(rawValue);
        V value = null;
        for (V v: clazz.getEnumConstants())
        {
            if (XmlUtils.lenientMatch(v.name(), text))
            {
                value = v;
            }
        }
        if (value == null)
        {
            throw new IllegalArgumentException("No enum value matches " + text);
        }
        context.addValue(dataAdder, value);
    }
}
