package org.finos.legend.engine.language.pure.dsl.service.execution;

import org.finos.legend.engine.plan.execution.stores.StoreExecutorConfiguration;
import org.finos.legend.engine.plan.platform.java.JavaSourceHelper;

import java.lang.reflect.Constructor;
import java.util.Objects;

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

    public ServiceRunnerBuilder withServicePath(String projectGroup, String projectArtifact, String servicePath)
    {
        String serviceRunnerClass = JavaSourceHelper.serviceElementPathToJavaPath(projectGroup + '.' + projectArtifact, servicePath);
        return this.withServiceRunnerClass(serviceRunnerClass, Thread.currentThread().getContextClassLoader());
    }

    public ServiceRunnerBuilder withServiceRunnerClass(String serviceRunnerClass)
    {
        return this.withServiceRunnerClass(serviceRunnerClass, Thread.currentThread().getContextClassLoader());
    }

    public ServiceRunnerBuilder withServiceRunnerClass(String serviceRunnerClass, ClassLoader classLoader)
    {
        this.serviceRunnerClass = serviceRunnerClass;
        this.classLoader = Objects.requireNonNull(classLoader);
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
        try {
            Class<?> clazz = this.classLoader.loadClass(this.serviceRunnerClass);
            Constructor<?> constructor = clazz.getDeclaredConstructor(StoreExecutorConfiguration[].class);
            Object serviceRunner = constructor.newInstance((Object) this.storeExecutorConfigurations);
            return (ServiceRunner)serviceRunner;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
