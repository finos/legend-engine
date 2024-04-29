//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServiceRunnerBuilder
{
    private String serviceRunnerClass;
    private ClassLoader classLoader;
    private boolean allowJavaCompilation;
    private StoreExecutorConfiguration[] storeExecutorConfigurations;

    private ServiceRunnerBuilder()
    {
    }

    public static ServiceRunnerBuilder newInstance()
    {
        return new ServiceRunnerBuilder();
    }

    public ServiceRunnerBuilder withServiceRunnerClass(String serviceRunnerClass)
    {
        return this.withServiceRunnerClass(serviceRunnerClass, Thread.currentThread().getContextClassLoader());
    }

    public ServiceRunnerBuilder withServiceRunnerClass(String serviceRunnerClass, ClassLoader classLoader)
    {
        this.serviceRunnerClass = serviceRunnerClass;
        Objects.requireNonNull(classLoader);
        this.classLoader = classLoader;
        return this;
    }

    public ServiceRunnerBuilder withStoreExecutorConfigurations(StoreExecutorConfiguration... storeExecutorConfigurations)
    {
        this.storeExecutorConfigurations = storeExecutorConfigurations;
        return this;
    }

    public ServiceRunnerBuilder withAllowJavaCompilation(boolean allowJavaCompilation)
    {
        this.allowJavaCompilation = allowJavaCompilation;
        return this;
    }

    public ServiceRunner build()
    {
        try
        {
            Class<?> clazz = this.classLoader.loadClass(this.serviceRunnerClass);
            Constructor<?> constructor = clazz.getDeclaredConstructor(StoreExecutorConfiguration[].class);
            Object serviceRunner = constructor.newInstance((Object) this.storeExecutorConfigurations);
            return (ServiceRunner) serviceRunner;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static StoreExecutor buildStoreExecutor(MutableList<StoreExecutorBuilder> storeExecutorBuilders, Class<? extends StoreExecutorBuilder> storeExecutorBuilderClass, StoreExecutorConfiguration configuration)
    {
        StoreExecutorBuilder builder = filter(storeExecutorBuilders, storeExecutorBuilderClass);
        if (configuration != null)
        {
            return builder.build(configuration);
        }
        else
        {
            return builder.build();
        }
    }

    private static StoreExecutorBuilder filter(MutableList<StoreExecutorBuilder> storeExecutorBuilders, Class<? extends StoreExecutorBuilder> clazz)
    {
        MutableList<StoreExecutorBuilder> builders = storeExecutorBuilders.select(builder -> clazz.isAssignableFrom(builder.getClass())).toList();
        if (builders.size() == 0)
        {
            String message = String.format("Attempt to configure a builder of type %s but none was found in the classpath", clazz.getCanonicalName());
            throw new RuntimeException(message);
        }
        if (builders.size() > 1)
        {
            String builderImplementations = builders.stream().map(b -> b.getClass().getCanonicalName()).collect(Collectors.joining(","));
            String message = String.format("Attempt to configure a builder of type %s but found more than one in the classpath. Implementations=%s", clazz.getCanonicalName(), builderImplementations);
            throw new RuntimeException(message);
        }
        return builders.get(0);
    }
}
