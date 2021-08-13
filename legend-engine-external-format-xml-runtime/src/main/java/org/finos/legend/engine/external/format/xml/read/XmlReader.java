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

package org.finos.legend.engine.external.format.xml.read;

import org.finos.legend.engine.external.shared.runtime.read.ExternalFormatReader;

import java.io.InputStream;
import java.util.function.Consumer;

public class XmlReader<T> extends ExternalFormatReader<T>
{
    private final IXmlDeserializeExecutionNodeSpecifics specifics;
    private final InputStream stream;
    private final String location;

    public XmlReader(IXmlDeserializeExecutionNodeSpecifics specifics, InputStream stream, String location)
    {
        this.specifics = specifics;
        this.stream = stream;
        this.location = location;
    }

    @Override
    protected void readData(Consumer consumer)
    {
        org.finos.legend.engine.external.format.xml.shared.XmlReader reader = org.finos.legend.engine.external.format.xml.shared.XmlReader.newReader(stream, location);
        DeserializeContext<?> context = new DeserializeContext<>(reader, consumer);
        specifics.read(context);
    }
}

