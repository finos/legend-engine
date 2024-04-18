// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler;

import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class MetadataWrapper implements Metadata
{
    private final Metadata lazy;
    private final Package root;
    private final PureModel pureModel;

    public MetadataWrapper(Package root, Metadata lazy)
    {
        this(root, lazy, null);
    }

    public MetadataWrapper(Package root, Metadata lazy, PureModel pureModel)
    {
        this.root = root;
        this.lazy = lazy;
        this.pureModel = pureModel;
    }

    @Override
    public void startTransaction()
    {
        this.lazy.startTransaction();
    }

    @Override
    public void commitTransaction()
    {
        this.lazy.commitTransaction();
    }

    @Override
    public void rollbackTransaction()
    {
        this.lazy.rollbackTransaction();
    }

    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        if ((M3Paths.Package.equals(classifier) && M3Paths.Root.equals(id)))
        {
            return this.root;
        }
        try
        {
            return this.lazy.getMetadata(classifier, id);
        }
        catch (RuntimeException e)
        {
            if (this.pureModel != null)
            {
                CoreInstance type = this.pureModel.getTypeFromIndex(id.substring(M3Paths.Root.length() + 2));
                if (type != null)
                {
                    return type;
                }
            }
            throw new PureExecutionException(e);
        }
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        return this.lazy.getMetadata(classifier);
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        return this.lazy.getEnum(enumerationName, enumName);
    }
}
