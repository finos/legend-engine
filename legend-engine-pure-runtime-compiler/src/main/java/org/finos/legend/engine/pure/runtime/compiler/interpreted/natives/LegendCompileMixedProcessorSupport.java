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

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

public class LegendCompileMixedProcessorSupport extends M3ProcessorSupport
{
    private ProcessorSupport originalProcessorSupport;

    public LegendCompileMixedProcessorSupport(Context context, ModelRepository modelRepository, ProcessorSupport original)
    {
        super(context, modelRepository);
        this.originalProcessorSupport = original;
    }

    // Intended mainly to Provide a classifier for ValCoreInstance (String, Integer, etc.)
    @Override
    public CoreInstance getClassifier(CoreInstance instance)
    {
        if (instance instanceof ValCoreInstance)
        {
            return _Package.getByUserPath(((ValCoreInstance) instance).getType(), this);
        }
        if (instance instanceof Any)
        {
            Any any = (Any) instance;
            GenericType genericType = any._classifierGenericType();
            if (genericType != null)
            {
                Type type = genericType._rawType();
                if (type != null)
                {
                    return type;
                }
            }
            return any.getClassifier();
        }
        return instance.getClassifier();
    }

    @Override
    public CoreInstance newGenericType(SourceInformation sourceInformation, CoreInstance source, boolean inferred)
    {
        CoreInstance coreInstance = _Package.getByUserPath("meta::pure::metamodel::type::generics::GenericType", this.originalProcessorSupport);
        return this.modelRepository.newCoreInstance("", coreInstance, null);
    }
}
