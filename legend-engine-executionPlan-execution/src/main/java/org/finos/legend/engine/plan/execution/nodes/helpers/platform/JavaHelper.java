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

import org.codehaus.commons.compiler.CompileException;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.engine.plan.compilation.ExecutionPlanDependenciesFilter;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CompiledClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaClass;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.CompositeClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.shared.javaCompiler.JavaVersion;
import org.finos.legend.engine.shared.javaCompiler.SingleFileCompiler;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaHelper
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final String DEFAULT_EXECUTION_METHOD_NAME = "execute";

    private JavaHelper()
    {
    }

    public static EngineJavaCompiler compilePlan(SingleExecutionPlan singleExecutionPlan, MutableList<CommonProfile> pm) throws JavaCompileException
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_START, "Compile Plan").toString());

            EngineJavaCompiler compiler;
            try
            {
                compiler = compilePlanFast(singleExecutionPlan);
            }
            catch (Exception ignored)
            {
                // TODO Confirm we can delete this
                compiler = compilePlanSlow(singleExecutionPlan);
            }

            LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_STOP, (double) System.currentTimeMillis() - start).toString());

            return compiler;
        }
        catch (Exception e)
        {
            LOGGER.info(new LogInfo(pm, LoggingEventType.JAVA_COMPILATION_ERROR, new ErrorResult(1, e).getMessage()).toString());
            throw e;
        }
    }

    public static StringJavaSource buildStringJavaSource(JavaClass jc)
    {
        return StringJavaSource.newStringJavaSource(jc._package, jc.name, jc.source);
    }

    private static EngineJavaCompiler createNewJavaCompiler()
    {
        List<ClassPathFilter> filters = Lists.mutable.of(new ExecutionPlanDependenciesFilter());
        ExecutionPlanJavaCompilerExtensionLoader.extensions().forEach(ext -> filters.add(ext.getExtraClassPathFilter()));
        return new EngineJavaCompiler(JavaVersion.JAVA_8, new CompositeClassPathFilter(filters));
    }

    private static EngineJavaCompiler compilePlanFast(SingleExecutionPlan singleExecutionPlan) throws JavaCompileException, IOException, CompileException
    {
        Map<JavaPlatformImplementation, List<JavaClass>> javaClassesMap = Maps.mutable.empty();
        if (singleExecutionPlan.globalImplementationSupport != null)
        {
            JavaPlatformImplementation platformImplementation = (JavaPlatformImplementation) singleExecutionPlan.globalImplementationSupport;
            List<JavaClass> globalImplementationSupportClasses = collectJavaClasses(platformImplementation);
            if (!globalImplementationSupportClasses.isEmpty())
            {
                javaClassesMap.put(platformImplementation, globalImplementationSupportClasses);
            }
        }
        collectJavaClasses(singleExecutionPlan.rootExecutionNode, javaClassesMap);
        if (javaClassesMap.isEmpty())
        {
            return null;
        }

        EngineJavaCompiler javaCompiler = createNewJavaCompiler();
        Map<JavaClass, JavaPlatformImplementation> reverseClassMap = Maps.mutable.empty();
        List<JavaClass> executeClasses = new ArrayList<>();
        List<JavaClass> nonExecuteClasses = new ArrayList<>();
        javaClassesMap.forEach((jimpl, jclasses) -> jclasses.forEach(jclass ->
        {
            reverseClassMap.put(jclass, jimpl);
            (javaClassHasFullName(jclass, jimpl.executionClassFullName) ? executeClasses : nonExecuteClasses).add(jclass);
        }));
        Map<String, JavaClass> classMap = nonExecuteClasses.stream().collect(Collectors.toMap(JavaHelper::getJavaClassFullName, x -> x, (x, y) ->
        {
            throw new RuntimeException("Conflicting classes compilation in fast mode not supported!");
        }));

        Map<String, String> classToBytecodeMap = compileJavaClasses(nonExecuteClasses, javaCompiler);

        classToBytecodeMap.forEach((name, bytecode) ->
        {
            JavaClass _class = classMap.get(name);
            if (_class == null)
            {
                // Handling Inner Classes only
                int firstDollar = name.indexOf('$');
                if (firstDollar == -1)
                {
                    throw new RuntimeException("Non Inner classes are not supported yet!");
                }

                JavaClass topClass = classMap.get(name.substring(0, firstDollar));
                JavaPlatformImplementation impl = reverseClassMap.get(topClass);
                _class = createGeneratedJavaClass(name);
                if (impl.classes == null)
                {
                    impl.classes = FastList.newListWith(_class);
                }
                else
                {
                    impl.classes.add(_class);
                }
            }
            _class.byteCode = bytecode;
        });

        ClassLoader globalClassLoader = javaCompiler.getClassLoader();

        for (JavaClass executeClass : executeClasses)
        {
            if (executeClass.byteCode == null)
            {
                Map<String, byte[]> classes = SingleFileCompiler.compileFile(buildStringJavaSource(executeClass), globalClassLoader);
                executeClass.byteCode = Base64.getEncoder().encodeToString(classes.get(getJavaClassFullName(executeClass)));
            }
        }

        return javaCompiler;
    }

    private static void collectJavaClasses(ExecutionNode executionNode, Map<JavaPlatformImplementation, List<JavaClass>> javaClassesMap)
    {
        if (executionNode.implementation != null)
        {
            JavaPlatformImplementation platformImplementation = (JavaPlatformImplementation) executionNode.implementation;
            List<JavaClass> implementationJavaClasses = collectJavaClasses(platformImplementation);
            if (!implementationJavaClasses.isEmpty())
            {
                javaClassesMap.put(platformImplementation, implementationJavaClasses);
            }
        }

        executionNode.childNodes().forEach(node -> collectJavaClasses(node, javaClassesMap));
    }

    private static List<JavaClass> collectJavaClasses(JavaPlatformImplementation platformImplementation)
    {
        if ((platformImplementation.code == null) &&
                ((platformImplementation.byteCode == null) || platformImplementation.byteCode.isEmpty()) &&
                ((platformImplementation.compiledClasses == null) || platformImplementation.compiledClasses.isEmpty()) &&
                ((platformImplementation.classes == null) || platformImplementation.classes.isEmpty()))
        {
            return Collections.emptyList();
        }

        Map<String, JavaClass> classMap = UnifiedMap.newMap();

        if (platformImplementation.code != null)
        {
            String executeClassFullName = getExecutionClassFullName(platformImplementation);
            JavaClass executeClass = newJavaClass(executeClassFullName);
            executeClass.source = platformImplementation.code;
            classMap.put(executeClassFullName, executeClass);
        }

        if (platformImplementation.byteCode != null)
        {
            platformImplementation.byteCode.forEach((fullName, byteCode) ->
            {
                JavaClass newClass = newJavaClass(fullName);
                newClass.byteCode = byteCode;
                classMap.put(fullName, newClass);
            });
        }

        if (platformImplementation.compiledClasses != null)
        {
            for (CompiledClass compiledClass : platformImplementation.compiledClasses)
            {
                JavaClass newClass = newJavaClass(compiledClass.className);
                newClass.byteCode = compiledClass.byteCode;
                classMap.put(compiledClass.className, newClass);
            }
        }

        if (platformImplementation.classes != null)
        {
            for (JavaClass localClass : platformImplementation.classes)
            {
                classMap.put(getJavaClassFullName(localClass), localClass);
            }
        }

        return FastList.newList(classMap.values());
    }

    private static EngineJavaCompiler compilePlanSlow(SingleExecutionPlan singleExecutionPlan) throws JavaCompileException
    {
        EngineJavaCompiler javaCompiler = createNewJavaCompiler();
        if (singleExecutionPlan.globalImplementationSupport != null)
        {
            JavaPlatformImplementation platformImplementation = (JavaPlatformImplementation) singleExecutionPlan.globalImplementationSupport;
            compileImplementation(platformImplementation, javaCompiler);
        }

        compileNode(singleExecutionPlan.rootExecutionNode, javaCompiler);

        return javaCompiler;
    }

    private static void compileNode(ExecutionNode executionNode, EngineJavaCompiler globalJavaCompiler) throws JavaCompileException
    {
        if (executionNode.implementation != null)
        {
            JavaPlatformImplementation platformImplementation = (JavaPlatformImplementation) executionNode.implementation;
            if (platformImplementation.classes != null && !platformImplementation.classes.isEmpty())
            {
                EngineJavaCompiler javaCompiler = new EngineJavaCompiler(globalJavaCompiler);
                JavaHelper.compileImplementation(platformImplementation, javaCompiler);
            }
        }

        for (ExecutionNode childNode : executionNode.executionNodes())
        {
            compileNode(childNode, globalJavaCompiler);
        }
    }

    private static void compileImplementation(JavaPlatformImplementation javaPlatformImplementation, EngineJavaCompiler javaCompiler) throws JavaCompileException
    {
        List<JavaClass> allJavaClasses = collectJavaClasses(javaPlatformImplementation);
        if (!allJavaClasses.isEmpty())
        {
            Map<String, String> classToBytecodeMap = compileJavaClasses(allJavaClasses, javaCompiler);

            Map<String, JavaClass> classMap = allJavaClasses.stream().collect(Collectors.toMap(JavaHelper::getJavaClassFullName, x -> x));
            classToBytecodeMap.forEach((name, bytecode) ->
            {
                JavaClass _class = classMap.get(name);
                if (_class == null)
                {
                    _class = createGeneratedJavaClass(name);
                    if (javaPlatformImplementation.classes == null)
                    {
                        javaPlatformImplementation.classes = FastList.newListWith(_class);
                    }
                    else
                    {
                        javaPlatformImplementation.classes.add(_class);
                    }
                }
                _class.byteCode = bytecode;
            });
        }
    }


    private static Map<String, String> compileJavaClasses(List<JavaClass> javaClasses, EngineJavaCompiler javaCompiler) throws JavaCompileException
    {
        Map<Boolean, List<JavaClass>> partitioned = javaClasses.stream().collect(Collectors.partitioningBy(c -> c.byteCode != null));
        List<JavaClass> javaClassesWithBytecode = partitioned.get(true);
        List<JavaClass> javaClassesToBeCompiled = partitioned.get(false);

        if (!javaClassesWithBytecode.isEmpty())
        {
            javaClassesWithBytecode.forEach(c -> javaCompiler.load(getJavaClassFullName(c), c.byteCode));
        }

        if (!javaClassesToBeCompiled.isEmpty())
        {
            javaCompiler.compile(javaClassesToBeCompiled.stream().map(c -> StringJavaSource.newStringJavaSource(c._package, c.name, c.source)).collect(Collectors.toList()));
        }

        return javaCompiler.save();
    }

    private static JavaClass createGeneratedJavaClass(String name)
    {
        JavaClass _class = newJavaClass(name);
        _class.source = "<<GENERATED>>";
        return _class;
    }


    public static String getExecutionClassFullName(JavaPlatformImplementation javaPlatformImpl)
    {
        if (javaPlatformImpl.executionClassFullName == null)
        {
            throw new IllegalStateException("Execution class missing");
        }
        return javaPlatformImpl.executionClassFullName;
    }

    public static String getExecutionMethodName(JavaPlatformImplementation javaPlatformImpl)
    {
        return (javaPlatformImpl.executionMethodName == null) ? DEFAULT_EXECUTION_METHOD_NAME : javaPlatformImpl.executionMethodName;
    }


    public static String getJavaClassFullName(JavaClass javaClass)
    {
        return javaClassHasPackage(javaClass) ? (javaClass._package + "." + javaClass.name) : javaClass.name;
    }

    public static JavaClass newJavaClass(String fullName)
    {
        int lastDot = fullName.lastIndexOf('.');
        JavaClass javaClass = new JavaClass();
        javaClass._package = (lastDot == -1) ? "" : fullName.substring(0, lastDot);
        javaClass.name = fullName.substring(lastDot + 1);
        return javaClass;
    }

    private static boolean javaClassHasPackage(JavaClass javaClass)
    {
        return (javaClass._package != null) && !javaClass._package.isEmpty();
    }

    private static boolean javaClassHasFullName(JavaClass javaClass, String fullName)
    {
        if (fullName == null)
        {
            return false;
        }
        if (!javaClassHasPackage(javaClass))
        {
            return fullName.equals(javaClass.name);
        }
        int lastDot = fullName.lastIndexOf('.');
        return (lastDot != -1) &&
                fullName.regionMatches(0, javaClass._package, 0, lastDot) &&
                fullName.regionMatches(lastDot + 1, javaClass.name, 0, fullName.length() - lastDot - 1);
    }

    public static ThreadContextClassLoaderScope withCurrentThreadContextClassLoader(ClassLoader newClassLoader)
    {
        return new ThreadContextClassLoaderScope(Thread.currentThread(), newClassLoader);
    }

    public static class ThreadContextClassLoaderScope implements AutoCloseable
    {
        private final Thread thread;
        private final ClassLoader previousClassLoader;

        private ThreadContextClassLoaderScope(Thread thread, ClassLoader newClassLoader)
        {
            this.thread = thread;
            if ((this.thread == null) || (newClassLoader == null))
            {
                this.previousClassLoader = null;
            }
            else
            {
                this.previousClassLoader = this.thread.getContextClassLoader();
                this.thread.setContextClassLoader(newClassLoader);
            }
        }

        @Override
        public void close()
        {
            if ((this.thread != null) && (this.previousClassLoader != null))
            {
                this.thread.setContextClassLoader(this.previousClassLoader);
            }
        }
    }
}
