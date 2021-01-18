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

package org.finos.legend.engine.plan.execution.nodes.helpers.platform;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.engine.shared.javaCompiler.StringJavaSource;
import org.finos.legend.engine.plan.execution.nodes.ExecutionNodeExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CompiledClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.slf4j.Logger;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionNodeJavaPlatformHelper
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    public static Result executeJavaImplementation(ExecutionNode node, ExecutionNodeContextFactory contextFactory, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        Result childResult = node.executionNodes().isEmpty() ? null : node.executionNodes().getFirst().accept(new ExecutionNodeExecutor(profiles, executionState));
        ExecutionNodeContext context = contextFactory.create(executionState, childResult);
        Subject subject = ProfileManagerHelper.extractSubject(profiles);
        return subject == null
                ? callJavaExecute(node, context, executionState, null)
                : Subject.doAs(subject, (PrivilegedAction<Result>) () -> callJavaExecute(node, context, executionState, profiles));
    }

    public static <T> T getNodeSpecificsInstance(ExecutionNode node, ExecutionState executionState, MutableList<CommonProfile> profiles)
    {
        if (!(node.implementation instanceof JavaPlatformImplementation))
        {
            throw new RuntimeException("Only Java implementations are currently supported, found: " + node.implementation);
        }

        Class<?> specificsClass = getClassToExecute(node, JavaHelper.getExecutionClassFullName((JavaPlatformImplementation) node.implementation), executionState, profiles);

        try
        {
            return (T) specificsClass.getConstructor().newInstance();
        }
        catch (NoSuchMethodException | IllegalAccessException | InstantiationException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Result callJavaExecute(ExecutionNode node, ExecutionNodeContext context, ExecutionState executionState, MutableList<CommonProfile> pm)
    {
        if (!(node.implementation instanceof JavaPlatformImplementation))
        {
            throw new RuntimeException("Only Java implementations are currently supported, found: " + node.implementation);
        }
        JavaPlatformImplementation javaPlatformImpl = (JavaPlatformImplementation) node.implementation;
        String className = JavaHelper.getExecutionClassFullName(javaPlatformImpl);
        String methodName = JavaHelper.getExecutionMethodName(javaPlatformImpl);

        Class<?> executionClass = getClassToExecute(node, className, executionState, pm);
        for (Method method : executionClass.getDeclaredMethods())
        {
            if (methodName.equals(method.getName()))
            {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0)
                {
                    return toResult(executeStaticJavaMethod(method));
                }
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(context))
                {
                    return toResult(executeStaticJavaMethod(method, context));
                }
                if (parameterTypes.length == 1 && parameterTypes[0].isInstance(context.getChildResult()))
                {
                    return toResult(executeStaticJavaMethod(method, context.getChildResult()));
                }
                if (parameterTypes.length == 2 && parameterTypes[0].isInstance(context.getChildResult()) && parameterTypes[1].isInstance(context))
                {
                    return toResult(executeStaticJavaMethod(method, context.getChildResult(), context));
                }
                if (parameterTypes.length == 2 && parameterTypes[0].isInstance(context.getChildResult()) && parameterTypes[1].isInstance(executionState))
                {
                    return toResult(executeStaticJavaMethod(method, context.getChildResult(), executionState));
                }
            }
        }
        throw new RuntimeException("Could not find appropriate execution method named '" + methodName + "' on class " + className);
    }

    private static Result toResult(Object result)
    {
        return (result instanceof Result) ? (Result) result : new ConstantResult(result);
    }

    private static Object executeStaticJavaMethod(Method toExecuteMethod, Object... parameters)
    {
        try
        {
            return toExecuteMethod.invoke(null, parameters);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
            {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error)
            {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    public static <T> T executeStaticJavaMethod(ExecutionNode node, String className, String methodName, List<? extends Class<?>> parameterTypes, List<?> parameters, ExecutionState executionState, MutableList<CommonProfile> pm)
    {
        return executeStaticJavaMethod(node, className, methodName, Collections.singletonList(Tuples.pair(parameterTypes, parameters)), executionState, pm);
    }

    public static <T> T executeStaticJavaMethod(ExecutionNode node, String className, String methodName, List<? extends Pair<? extends List<? extends Class<?>>, ? extends List<?>>> parameterTypesAndParametersAlternatives, ExecutionState executionState, MutableList<CommonProfile> pm)
    {
        Class<?> toExecuteClass = getClassToExecute(node, className, executionState, pm);

        List<NoSuchMethodException> noSuchMethodExceptions = new ArrayList<>();
        for (Pair<? extends List<? extends Class<?>>, ? extends List<?>> pair : parameterTypesAndParametersAlternatives)
        {
            try
            {
                Method method = toExecuteClass.getMethod(methodName, pair.getOne().toArray(new Class[0]));
                return (T) method.invoke(null, pair.getTwo().toArray());
            }
            catch (NoSuchMethodException e)
            {
                noSuchMethodExceptions.add(e);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException)
                {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error)
                {
                    throw (Error) cause;
                }
                throw new RuntimeException(cause);
            }
        }
        if (!noSuchMethodExceptions.isEmpty())
        {
            NoSuchMethodException lastException = noSuchMethodExceptions.get(noSuchMethodExceptions.size() - 1);
            noSuchMethodExceptions.subList(0, noSuchMethodExceptions.size() - 1).forEach(lastException::addSuppressed);
            throw new RuntimeException(lastException);
        }
        return null;
    }

    private static List<JavaClass> getLocalImplementationSupportClasses(ExecutionNode node)
    {
        JavaPlatformImplementation j = (JavaPlatformImplementation) node.implementation;
        return j.classes == null ? FastList.newList() : j.classes;
    }

    public static Class<?> getClassToExecute(ExecutionNode node, String _class, ExecutionState executionState, MutableList<CommonProfile> pm)
    {
        if (executionState.isJavaCompilationForbidden())
        {
            try
            {
                return Thread.currentThread().getContextClassLoader().loadClass(_class);
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }

        JavaPlatformImplementation j = (JavaPlatformImplementation) node.implementation;
        List<JavaClass> javaClasses = FastList.newList();
        if (j.code != null)
        {
            JavaClass executeClass = JavaHelper.newJavaClass(JavaHelper.getExecutionClassFullName(j));
            executeClass.source = j.code;
            javaClasses.add(executeClass);
        }
        javaClasses.addAll(getLocalImplementationSupportClasses(node));
        if (javaClasses.isEmpty())
        {
            ClassLoader classLoader = executionState.hasJavaCompiler() ? executionState.getJavaCompiler().getClassLoader() : Thread.currentThread().getContextClassLoader();
            try
            {
                return classLoader.loadClass(_class);
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }

        try
        {
            // TODO Confirm we can delete this
            MutableMap<String, String> compiledClassesWithByteCode = getCompiledClasses(node);
            List<StringJavaSource> classesToCompile = javaClasses.stream()
                    .filter(c -> !compiledClassesWithByteCode.containsKey(JavaHelper.getJavaClassFullName(c)))
                    .map(JavaHelper::buildStringJavaSource)
                    .collect(Collectors.toList());

            EngineJavaCompiler compiler = new EngineJavaCompiler(executionState.getJavaCompiler());

            long start = System.currentTimeMillis();

            try
            {
                if (!compiledClassesWithByteCode.isEmpty())
                {
                    compiler.load(compiledClassesWithByteCode);
                }

                if (!classesToCompile.isEmpty())
                {
                    LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_START, "Node: " + node.getClass().getName()).toString());
                    compiler.compile(classesToCompile);
                    LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_STOP, (double)System.currentTimeMillis() - start).toString());
                }
            }
            catch (Exception jce)
            {
                LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_ERROR, new ErrorResult(1, jce).getMessage()).toString());
                throw jce;
            }

            return compiler.getClassLoader().loadClass(_class);
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static MutableMap<String, String> getCompiledClasses(ExecutionNode node)
    {
        JavaPlatformImplementation platformImplementation = (JavaPlatformImplementation) node.implementation;
        if (platformImplementation.byteCode != null)
        {
            return UnifiedMap.newMap(platformImplementation.byteCode);
        }
        else if (platformImplementation.compiledClasses != null)
        {
            return UnifiedMap.newMapWith(org.eclipse.collections.impl.list.mutable.ListAdapter.adapt(platformImplementation.compiledClasses).collect((Function<CompiledClass, Pair<String, String>>) c -> Tuples.pair(c.className, c.byteCode)));
        }
        else if (platformImplementation.classes != null)
        {
            return UnifiedMap.newMapWith(platformImplementation.classes.stream().filter(c -> c.byteCode != null).map(c -> Tuples.pair(c._package + '.' + c.name, c.byteCode)).collect(Collectors.toList()));
        }
        else
        {
            return UnifiedMap.newMap();
        }
    }

    public interface ExecutionNodeContextFactory
    {
        ExecutionNodeContext create(ExecutionState state, Result childResult);
    }
}
