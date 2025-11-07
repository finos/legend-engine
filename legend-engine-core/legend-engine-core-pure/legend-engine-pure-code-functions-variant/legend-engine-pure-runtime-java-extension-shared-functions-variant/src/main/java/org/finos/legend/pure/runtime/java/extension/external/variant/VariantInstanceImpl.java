// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.variant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.Variant;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.variant.VariantCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public class VariantInstanceImpl extends VariantCoreInstanceWrapper
{
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JsonNode jsonNode;

    private VariantInstanceImpl(JsonNode jsonNode, Variant wrapped)
    {
        super(wrapped);
        this.jsonNode = jsonNode;
    }

    public JsonNode getJsonNode()
    {
        return this.jsonNode;
    }

    @Override
    public String toString()
    {
        return super.getName();
    }

    @Override
    public Variant copy()
    {
        return new VariantInstanceImpl(this.jsonNode.deepCopy(), ((Variant) this.instance).copy());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof VariantInstanceImpl))
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }
        VariantInstanceImpl that = (VariantInstanceImpl) o;
        return this.getName().equals(that.getName());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), this.getName());
    }

    public static VariantInstanceImpl newVariant(String json, ProcessorSupport processorSupport)
    {
        return newVariant(json, (SourceInformation) null, processorSupport);
    }

    public static VariantInstanceImpl newVariant(String json, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        try
        {
            JsonNode parsed = OBJECT_MAPPER.readTree(json);
            return newVariant(parsed, sourceInformation, processorSupport);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public static VariantInstanceImpl newVariant(JsonNode node, ProcessorSupport processorSupport)
    {
        return newVariant(node, (SourceInformation) null, processorSupport);
    }

    public static VariantInstanceImpl newVariant(JsonNode node, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        Variant variant = (Variant) processorSupport.newCoreInstance(node.toString(), M3Paths.Variant, sourceInformation);
        return new VariantInstanceImpl(node, variant);
    }

    @Deprecated
    public static VariantInstanceImpl newVariant(String json, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        return newVariant(json, processorSupport);
    }

    @Deprecated
    public static VariantInstanceImpl newVariant(JsonNode node, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        return newVariant(node, processorSupport);
    }
}
