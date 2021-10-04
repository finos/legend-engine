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

package org.finos.legend.engine.external.shared.runtime.write;

import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.io.IOException;
import java.io.OutputStream;

public class ExternalFormatDefaultSerializer extends Serializer
{
    private final ExternalFormatWriter externalFormatWriter;

    public ExternalFormatDefaultSerializer(ExternalFormatWriter externalFormatWriter)
    {
        this.externalFormatWriter = externalFormatWriter;
    }

    @Override
    public void stream(OutputStream targetStream) throws IOException
    {
        externalFormatWriter.writeData(targetStream);
    }
}
