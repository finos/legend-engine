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
import org.finos.legend.engine.external.format.xml.shared.datatypes.DoubleSimpleTypeHandler;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;

public class AddDoubleToObject<T>  implements ValueProcessor<T>
{
    private final ExternalDataDoubleAdder<T> dataAdder;
    private final DoubleSimpleTypeHandler handler;

    public AddDoubleToObject(ExternalDataAdder<T> dataAdder, DoubleSimpleTypeHandler handler)
    {
        this.dataAdder = (ExternalDataDoubleAdder<T>) dataAdder;
        this.handler = handler;
    }

    @Override
    public void process(DeserializeContext<?> context, String rawValue)
    {
        context.addValue(dataAdder, handler.parseDouble(rawValue));
    }
}
