// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.pure.runtime.compiler.interpreted.natives;

import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;

public class InterpretedMetadata implements Metadata
{
    private ProcessorSupport processorSupport;

    public InterpretedMetadata(ProcessorSupport processorSupport)
    {
        this.processorSupport = processorSupport;
    }

    @Override
    public void startTransaction()
    {

    }

    @Override
    public void commitTransaction()
    {

    }

    @Override
    public void rollbackTransaction()
    {

    }

    @Override
    public CoreInstance getMetadata(String s, String s1)
    {
        if (s1.startsWith("Root::"))
        {
            s1 = s1.substring(6);
        }
        return _Package.getByUserPath(s1, processorSupport);
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String s)
    {
        throw new RuntimeException("Not supported");
    }

    @Override
    public CoreInstance getEnum(String s, String s1)
    {
        return Enumeration.findEnum(this.getMetadata(MetadataJavaPaths.Enumeration, s), s1);
    }
}
