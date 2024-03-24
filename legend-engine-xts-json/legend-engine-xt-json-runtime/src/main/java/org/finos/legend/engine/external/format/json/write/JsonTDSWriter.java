//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.write;

import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatWriter;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;

import java.io.OutputStream;

public class JsonTDSWriter extends ExternalFormatWriter
{
    private final RelationalResultToJsonDefaultSerializer serializer;

    public JsonTDSWriter(RelationalResult result)
    {
        this.serializer = new RelationalResultToJsonDefaultSerializer(result);
    }

    @Override
    public void writeData(OutputStream outputStream)
    {
        this.serializer.stream(outputStream);
    }
}
