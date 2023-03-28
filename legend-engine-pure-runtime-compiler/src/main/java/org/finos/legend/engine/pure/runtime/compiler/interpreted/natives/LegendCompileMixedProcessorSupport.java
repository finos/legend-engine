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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

public class LegendCompileMixedProcessorSupport extends M3ProcessorSupport
{
    private final ProcessorSupport originalProcessorSupport;

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

    // Intended mainly to convert ValCoreInstance (String, Integer, etc.) into M3 equivalent instances (StringCoreInstance, IntegerCoreInstance, etc)
    private CoreInstance convertValCoreInstance(CoreInstance value)
    {
        if (value instanceof ValCoreInstance)
        {
            ValCoreInstance val = (ValCoreInstance) value;
            return this.modelRepository.newCoreInstance(val.getName(), getClassifier(val), null);
        }
        else
        {
            return value;
        }
    }

    @Override
    public void instance_addValueToProperty(CoreInstance owner, ListIterable<String> path, Iterable<? extends CoreInstance> values)
    {
        values = LazyIterate.collect(values, this::convertValCoreInstance);
        super.instance_addValueToProperty(owner, path, values);
    }

    @Override
    public void instance_setValuesForProperty(CoreInstance owner, CoreInstance property, ListIterable<? extends CoreInstance> values)
    {
        values = values.collect(this::convertValCoreInstance);
        super.instance_setValuesForProperty(owner, property, values);
    }

    @Override
    public CoreInstance instance_getValueForMetaPropertyToOneResolved(CoreInstance owner, String property)
    {
        return convertValCoreInstance(super.instance_getValueForMetaPropertyToOneResolved(owner, property));
    }

    @Override
    public ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, String propertyName)
    {
        return super.instance_getValueForMetaPropertyToMany(owner, propertyName).collect(this::convertValCoreInstance);
    }

    @Override
    public ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, CoreInstance property)
    {
        return super.instance_getValueForMetaPropertyToMany(owner, property).collect(this::convertValCoreInstance);
    }
}
