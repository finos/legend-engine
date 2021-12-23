package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutor;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder;
import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class ServiceRunnerBuilder
{
    private String serviceRunnerClass;
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
        this.serviceRunnerClass = serviceRunnerClass;
        return this;
    }

    public ServiceRunnerBuilder withStoreExecutorConfigurations(StoreExecutorConfiguration ...storeExecutorConfigurations)
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
        List<ServiceRunner> serviceRunnerImplementations = Iterate
                .select(ServiceLoader.load(ServiceRunner.class),
                        runner -> runner.getCanonicalClassName().equals(this.serviceRunnerClass))
                .stream().collect(Collectors.toList());

        if (serviceRunnerImplementations.size() > 1)
        {
            String message = String.format("Found too many implementations of service class %s in classpath. Expected=1,Found=%d", this.serviceRunnerClass, serviceRunnerImplementations.size());
            throw new RuntimeException(message);
        }
        if (serviceRunnerImplementations.isEmpty())
        {
            String message = String.format("Service class %s not found in classpath", this.serviceRunnerClass);
            throw new RuntimeException(message);
        }

        try {
            Class<?> clazz = Class.forName(this.serviceRunnerClass);
            Constructor<?> constructor = clazz.getDeclaredConstructor(StoreExecutorConfiguration[].class);
            Object serviceRunner = constructor.newInstance((Object) this.storeExecutorConfigurations);
            return (ServiceRunner)serviceRunner;
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
