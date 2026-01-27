// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.server.core.pct;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.reports.model.AdapterKey;
import org.finos.legend.pure.m3.pct.reports.model.FunctionTestResults;
import org.finos.legend.pure.m3.pct.shared.generation.Shared;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GeneratePCTFiles
{
    public static void main(String[] args) throws Exception
    {
        Shared.writeStringToTarget("./target/pct", "PCT_Report_Compatibility.html", IOUtils.resourceToString("pct/PCT_Report_Compatibility.html", StandardCharsets.UTF_8, GeneratePCTFiles.class.getClassLoader()));
        Shared.writeStringToTarget("./target/pct", "pct-docs.json", getDocumentationAsJson());
        Shared.writeStringToTarget("./target/pct", "git-info.json", DeploymentStateAndVersions.sdlcJSON);
    }

    public static String getDocumentationAsJson() throws JsonProcessingException
    {
        return new ObjectMapper().addMixIn(FunctionDocumentation.class, FunctionDocumentationMixIn.class).writeValueAsString(DocumentationGeneration.buildDocumentation());
    }

    public static class FunctionDocumentationMixIn
    {
        @JsonSerialize(keyUsing = AdapterKeyToString.class)
        public Map<AdapterKey, FunctionTestResults> functionTestResults = Maps.mutable.empty();
    }

    public static class AdapterKeyToString extends StdScalarSerializer<AdapterKey>
    {
        protected AdapterKeyToString()
        {
            super(AdapterKey.class);
        }

        @Override
        public void serialize(AdapterKey key, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeFieldName(key.platform + ":" + key.adapter.group + ":" + key.adapter.name);
        }
    }
}
