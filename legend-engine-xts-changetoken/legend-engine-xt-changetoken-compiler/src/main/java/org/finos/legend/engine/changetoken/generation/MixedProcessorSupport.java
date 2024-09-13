// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;

public class MixedProcessorSupport implements ProcessorSupport
{
    CompiledProcessorSupport compiledProcessorSupport;

    public MixedProcessorSupport(CompiledProcessorSupport processorSupport)
    {
        this.compiledProcessorSupport = processorSupport;
    }

    @Override
    public boolean instance_instanceOf(CoreInstance object, String typeName)
    {
        return compiledProcessorSupport.instance_instanceOf(object, typeName);
    }

    @Override
    public boolean type_isPrimitiveType(CoreInstance type)
    {
        return compiledProcessorSupport.type_isPrimitiveType(type);
    }

    @Override
    public boolean valueSpecification_instanceOf(CoreInstance valueSpecification, String type)
    {
        try
        {
            return compiledProcessorSupport.valueSpecification_instanceOf(valueSpecification, type);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public CoreInstance type_wrapGenericType(CoreInstance aClass)
    {
        return compiledProcessorSupport.type_wrapGenericType(aClass);
    }

    @Override
    public CoreInstance function_getFunctionType(CoreInstance function)
    {
        return compiledProcessorSupport.function_getFunctionType(function);
    }

    @Override
    public CoreInstance newGenericType(SourceInformation sourceInformation, CoreInstance source, boolean inferred)
    {
        return compiledProcessorSupport.newGenericType(sourceInformation, source, inferred);
    }

    @Override
    public CoreInstance package_getByUserPath(String path)
    {
        return compiledProcessorSupport.package_getByUserPath(path);
    }

    @Override
    public CoreInstance repository_getTopLevel(String name)
    {
        return compiledProcessorSupport.repository_getTopLevel(name);
    }

    @Override
    public CoreInstance newEphemeralAnonymousCoreInstance(String type)
    {
        return compiledProcessorSupport.newEphemeralAnonymousCoreInstance(type);
    }

    @Override
    public CoreInstance newCoreInstance(String name, String typeName, SourceInformation sourceInformation)
    {
        return compiledProcessorSupport.newCoreInstance(name, typeName, sourceInformation);
    }

    @Override
    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return compiledProcessorSupport.newCoreInstance(name, classifier, sourceInformation);
    }

    @Override
    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, String classifier)
    {
        return compiledProcessorSupport.newAnonymousCoreInstance(sourceInformation, classifier);
    }

    @Override
    public SetIterable<CoreInstance> function_getFunctionsForName(String functionName)
    {
        return compiledProcessorSupport.function_getFunctionsForName(functionName);
    }

    @Override
    public ImmutableList<CoreInstance> type_getTypeGeneralizations(CoreInstance type, Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator)
    {
        return compiledProcessorSupport.type_getTypeGeneralizations(type, generator);
    }

    @Override
    public CoreInstance class_findPropertyUsingGeneralization(CoreInstance classifier, String propertyName)
    {
        return compiledProcessorSupport.class_findPropertyUsingGeneralization(classifier, propertyName);
    }

    @Override
    public CoreInstance class_findPropertyOrQualifiedPropertyUsingGeneralization(CoreInstance classifier, String propertyName)
    {
        return compiledProcessorSupport.class_findPropertyOrQualifiedPropertyUsingGeneralization(classifier, propertyName);
    }

    @Override
    public RichIterable<CoreInstance> class_getSimpleProperties(CoreInstance classifier)
    {
        return compiledProcessorSupport.class_getSimpleProperties(classifier);
    }

    @Override
    public MapIterable<String, CoreInstance> class_getSimplePropertiesByName(CoreInstance classifier)
    {
        return compiledProcessorSupport.class_getSimplePropertiesByName(classifier);
    }

    @Override
    public RichIterable<CoreInstance> class_getQualifiedProperties(CoreInstance classifier)
    {
        return compiledProcessorSupport.class_getQualifiedProperties(classifier);
    }

    @Override
    public MapIterable<String, CoreInstance> class_getQualifiedPropertiesByName(CoreInstance classifier)
    {
        return compiledProcessorSupport.class_getQualifiedPropertiesByName(classifier);
    }

    @Override
    public ListIterable<String> property_getPath(CoreInstance property)
    {
        return compiledProcessorSupport.property_getPath(property);
    }

    @Override
    public CoreInstance getClassifier(CoreInstance instance)
    {
        return compiledProcessorSupport.getClassifier(instance);
    }

    @Override
    public boolean type_subTypeOf(CoreInstance type, CoreInstance possibleSuperType)
    {
        return compiledProcessorSupport.type_subTypeOf(type, possibleSuperType);
    }

    @Override
    public CoreInstance type_BottomType()
    {
        return compiledProcessorSupport.type_BottomType();
    }

    @Override
    public CoreInstance type_TopType()
    {
        return compiledProcessorSupport.type_TopType();
    }

    public Metadata getMetadata()
    {
        return compiledProcessorSupport.getMetadata();
    }

    @Override
    public void instance_addValueToProperty(CoreInstance owner, ListIterable<String> path, Iterable<? extends CoreInstance> values)
    {
        compiledProcessorSupport.instance_addValueToProperty(owner, path, values);
    }

    @Override
    public void instance_setValuesForProperty(CoreInstance owner, CoreInstance property, ListIterable<? extends CoreInstance> values)
    {
        compiledProcessorSupport.instance_setValuesForProperty(owner, property, values);
    }

    @Override
    public CoreInstance instance_getValueForMetaPropertyToOneResolved(CoreInstance owner, String property)
    {
        return compiledProcessorSupport.instance_getValueForMetaPropertyToOneResolved(owner, property);
    }

    @Override
    public ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, String propertyName)
    {
        return compiledProcessorSupport.instance_getValueForMetaPropertyToMany(owner, propertyName);
    }

    @Override
    public ListIterable<? extends CoreInstance> instance_getValueForMetaPropertyToMany(CoreInstance owner, CoreInstance property)
    {
        return compiledProcessorSupport.instance_getValueForMetaPropertyToMany(owner, property);
    }
}
