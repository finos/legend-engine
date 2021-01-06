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

package org.finos.legend.engine.plan.compilation;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicRelativePathNode;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.GraphContext;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.RelativePathNode;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.RuleType;
import org.finos.legend.engine.plan.dependencies.domain.date.DayOfWeek;
import org.finos.legend.engine.plan.dependencies.domain.date.DurationUnit;
import org.finos.legend.engine.plan.dependencies.domain.date.Month;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.domain.date.Quarter;
import org.finos.legend.engine.plan.dependencies.domain.flatdata.ParsedFlatData;
import org.finos.legend.engine.plan.dependencies.domain.flatdata.ParsedFlatDataValue;
import org.finos.legend.engine.plan.dependencies.domain.flatdata.RawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.flatdata.RawFlatDataValue;
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IGraphFetchM2MExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;
import org.finos.legend.engine.plan.dependencies.store.platform.IGraphSerializer;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.ISerializationWriter;
import org.finos.legend.engine.plan.dependencies.store.platform.PredefinedExpressions;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.classResult.IRelationalClassInstantiationNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalChildGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalCrossRootGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalRootGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.shared.IConstantResult;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;
import org.finos.legend.engine.plan.dependencies.store.shared.IResult;
import org.finos.legend.engine.plan.dependencies.util.Library;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/*
 * Use this to generate the PURE code for integrating with this module.  At the time of writing
 * the code generated should update:
 *
 *  * main config: replace all of /core/pure/executionPlan/javaPlatform/planConventions/enginePlatformDependencies.pure
 *  * extensions: replace properties within /system/pure/router_extension.pure
 * to achieve this.
 */
public class GeneratePureConfig
{
    private static final String PURE_PACKAGE = "meta::pure::executionPlan::engine::";

    static final List<String> MAIN_PURE_CLASSES = new ArrayList<>();
    static final Map<String, List<String>> EXTENSION_PURE_CLASSES = new LinkedHashMap<>();
    static final Map<String, Class<?>> PURE_TO_JAVA_CLASSES = new LinkedHashMap<>();
    static final Map<Class<?>, String> JAVA_TO_PURE_CLASSES = new LinkedHashMap<>();

    static
    {
        mainConfig(register(PURE_PACKAGE + "IConstantResult", IConstantResult.class),
                register(PURE_PACKAGE + "EngineIResult", IResult.class),
                register(PURE_PACKAGE + "IExecutionNodeContext", IExecutionNodeContext.class),
                register(PURE_PACKAGE + "IGraphFetchM2MExecutionNodeContext", IGraphFetchM2MExecutionNodeContext.class),
                register(PURE_PACKAGE + "StoreStreamReader", IStoreStreamReader.class),
                register(PURE_PACKAGE + "DataParsingException", DataParsingException.class),
                register(PURE_PACKAGE + "PredefinedExpressions", PredefinedExpressions.class),
                register(PURE_PACKAGE + "Library", Library.class),
                register(PURE_PACKAGE + "GraphInstance", IGraphInstance.class),
                register(PURE_PACKAGE + "RelationalRootGraphNodeExecutor", IRelationalRootGraphNodeExecutor.class),
                register(PURE_PACKAGE + "RelationalCrossRootGraphNodeExecutor", IRelationalCrossRootGraphNodeExecutor.class),
                register(PURE_PACKAGE + "RelationalChildGraphNodeExecutor", IRelationalChildGraphNodeExecutor.class),
                register(PURE_PACKAGE + "RelationalClassInstantiationNodeExecutor", IRelationalClassInstantiationNodeExecutor.class),
                register(PURE_PACKAGE + "PureDate", PureDate.class),
                register("meta::pure::functions::date::DurationUnit", DurationUnit.class),
                register("meta::pure::functions::date::DayOfWeek", DayOfWeek.class),
                register("meta::pure::functions::date::Month", Month.class),
                register("meta::pure::functions::date::Quarter", Quarter.class),
                register(PURE_PACKAGE + "DataQualityBasicDefectClass", BasicDefect.class),
                register(PURE_PACKAGE + "DataQualityBasicRelativePathNodeClass", BasicRelativePathNode.class),
                register(PURE_PACKAGE + "DataQualityGraphContextClass", GraphContext.class),
                register(PURE_PACKAGE + "DataQualityConstrainedInterface", Constrained.class),
                register("meta::pure::dataQuality::Checked", IChecked.class),
                register("meta::pure::dataQuality::Defect", IDefect.class),
                register("meta::pure::dataQuality::EnforcementLevel", EnforcementLevel.class),
                register("meta::pure::dataQuality::RuleType", RuleType.class),
                register("meta::pure::dataQuality::RelativePathNode", RelativePathNode.class),
                register(PURE_PACKAGE + "IReferencedObject", IReferencedObject.class),
                register(PURE_PACKAGE + "IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics", IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics.class),
                register(PURE_PACKAGE + "IPlatformPureExpressionExecutionNodeSerializeSpecifics", IPlatformPureExpressionExecutionNodeSerializeSpecifics.class),
                register(PURE_PACKAGE + "ISerializationWriter", ISerializationWriter.class),
                register(PURE_PACKAGE + "IGraphSerializer", IGraphSerializer.class)
        );

        extensionConfig("flatData",
                register("meta::flatData::transfer::RawFlatData", RawFlatData.class),
                register("meta::flatData::transfer::RawFlatDataValue", RawFlatDataValue.class),
                register("meta::flatData::transfer::ParsedFlatData", ParsedFlatData.class),
                register("meta::flatData::transfer::ParsedFlatDataValue", ParsedFlatDataValue.class)
        );
    }

    private static String register(String pureClass, Class<?> javaClass)
    {
        PURE_TO_JAVA_CLASSES.put(pureClass, javaClass);
        JAVA_TO_PURE_CLASSES.put(javaClass, pureClass);
        return pureClass;
    }

    private static void mainConfig(String... pureClasses)
    {
        MAIN_PURE_CLASSES.addAll(Arrays.asList(pureClasses));
    }

    private static void extensionConfig(String name, String... pureClasses)
    {
        EXTENSION_PURE_CLASSES.put(name, Arrays.asList(pureClasses));
    }

    public static void main(String[] args)
    {
        System.out.println("================================================================================================================================================================\n");
        GeneratePureConfig mainConfig = new GeneratePureConfig();
        MAIN_PURE_CLASSES.forEach(mainConfig::addClass);
        System.out.println(mainConfig.generate());
        EXTENSION_PURE_CLASSES.forEach((name, pureClasses) ->
        {
            System.out.println("================================================================================================================================================================\n");
            GeneratePureConfig extension = new GeneratePureConfig(mainConfig, name);
            pureClasses.forEach(extension::addClass);
            System.out.println(extension.generate());
        });
    }

    private final GeneratePureConfig mainConfig;
    private final String extensionName;

    private final List<Consumer<Output>> classes = new ArrayList<>();
    private final List<Consumer<Output>> providedTypes = new ArrayList<>();
    private final Map<Type, JavaClass> javaClasses = new LinkedHashMap<>();
    private final Map<Type, Encoded> standardTypes = new HashMap<>();
    private final Map<Type, PredefinedJavaClass> predefinedJavaClasses = new LinkedHashMap<>();

    private GeneratePureConfig()
    {
        initStandardTypes();
        this.mainConfig = null;
        this.extensionName = null;
    }

    private GeneratePureConfig(GeneratePureConfig mainConfig, String extensionName)
    {
        initStandardTypes();
        this.mainConfig = mainConfig;
        this.extensionName = Objects.requireNonNull(extensionName);
    }

    private void initStandardTypes()
    {
        standardTypes.put(Character.TYPE, new Encoded("javaChar"));
        standardTypes.put(Integer.TYPE, new Encoded("javaInt"));
        standardTypes.put(Long.TYPE, new Encoded("javaLong"));
        standardTypes.put(Float.TYPE, new Encoded("javaFloat"));
        standardTypes.put(Double.TYPE, new Encoded("javaDouble"));
        standardTypes.put(Boolean.TYPE, new Encoded("javaBoolean"));
        standardTypes.put(Void.TYPE, new Encoded("javaVoid"));
        standardTypes.put(Character.class, new Encoded("javaCharBoxed"));
        standardTypes.put(Integer.class, new Encoded("javaIntBoxed"));
        standardTypes.put(Long.class, new Encoded("javaLongBoxed"));
        standardTypes.put(Float.class, new Encoded("javaFloatBoxed"));
        standardTypes.put(Double.class, new Encoded("javaDoubleBoxed"));
        standardTypes.put(Boolean.class, new Encoded("javaBooleanBoxed"));
        standardTypes.put(Object.class, new Encoded("javaObject"));
        standardTypes.put(String.class, new Encoded("javaString"));
        standardTypes.put(Date.class, new Encoded("javaDate"));
        standardTypes.put(java.sql.Date.class, new Encoded("javaSqlDate"));
        standardTypes.put(java.sql.Timestamp.class, new Encoded("javaSqlTimestamp"));
        standardTypes.put(Appendable.class, new Encoded("javaAppendable"));
        standardTypes.put(Number.class, new Encoded("javaNumber"));
        standardTypes.put(BigInteger.class, new Encoded("javaBigInteger"));
        standardTypes.put(BigDecimal.class, new Encoded("javaBigDecimal"));
        standardTypes.put(Calendar.class, new Encoded("javaCalendar"));
        standardTypes.put(GregorianCalendar.class, new Encoded("javaGregorianCalendar"));
        standardTypes.put(Type.class, new Encoded("javaReflectType"));
        standardTypes.put(Method.class, new Encoded("javaReflectMethod"));
        standardTypes.put(ResultSet.class, new Encoded("javaResultSet"));
        standardTypes.put(StringBuilder.class, new Encoded("javaStringBuilder"));
        standardTypes.put(URL.class, new Encoded("javaURL"));
    }

    private GeneratePureConfig addClass(String pureClassPath)
    {
        return (pureClassPath.startsWith(PURE_PACKAGE))
                ? this.defineClass(pureClassPath, PURE_TO_JAVA_CLASSES.get(pureClassPath))
                : this.mapClass(pureClassPath, PURE_TO_JAVA_CLASSES.get(pureClassPath));
    }

    private GeneratePureConfig defineClass(String pureClassPath, Class<?> clazz)
    {
        if (extensionName != null)
        {
            throw new IllegalStateException("Extensions can only map classes not define them");
        }
        classes.add((o -> o.lineOut("Class " + pureClassPath + " {}")));
        mapClass(pureClassPath, clazz);
        return this;
    }

    private GeneratePureConfig mapClass(String pureClassPath, Class<?> clazz)
    {
        JavaClass javaClass = new JavaClass(clazz);
        javaClasses.put(clazz, javaClass);
        provided(pureClassPath, javaClass.reference());
        return this;
    }

    private void provided(String pureClassPath, String as)
    {
        providedTypes.add(o ->
        {
            o.finishLine("");
            o.startLine("->addProvidedType(" + pureClassPath + ", " + as + ")");
        });
    }

    private Encoded encodeType(Type type)
    {
        if (standardTypes.containsKey(type))
        {
            return standardTypes.get(type);
        }
        else if (javaClasses.containsKey(type))
        {
            return new Encoded(javaClasses.get(type));
        }
        else if (predefinedJavaClasses.containsKey(type))
        {
            return new Encoded(predefinedJavaClasses.get(type));
        }
        else if (mainConfig != null && mainConfig.javaClasses.containsKey(type))
        {
            PredefinedJavaClass predefined = new PredefinedJavaClass(mainConfig.javaClasses.get(type));
            predefinedJavaClasses.put(type, predefined);
            return new Encoded(predefinedJavaClasses.get(type));
        }
        else if (type instanceof GenericArrayType)
        {
            return new Encoded("javaArray", encodeType(((GenericArrayType) type).getGenericComponentType()));
        }
        else if (type instanceof Class && ((Class<?>) type).isArray())
        {
            return new Encoded("javaArray", encodeType(((Class<?>) type).getComponentType()));
        }
        else if (type instanceof ParameterizedType)
        {
            Type raw = ((ParameterizedType) type).getRawType();
            Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            if (List.class.equals(raw))
            {
                return new Encoded("javaList", encodeType(typeParams[0]));
            }
            if (Collection.class.equals(raw))
            {
                return new Encoded("javaCollection", encodeType(typeParams[0]));
            }
            else if (Stream.class.equals(raw))
            {
                return new Encoded("javaStream", encodeType(typeParams[0]));
            }
            else if (Predicate.class.equals(raw))
            {
                return new Encoded("javaPredicate", encodeType(typeParams[0]));
            }
            else if (Comparator.class.equals(raw))
            {
                return new Encoded("javaComparator", encodeType(typeParams[0]));
            }
            else if (Function.class.equals(raw))
            {
                return new Encoded("javaFunction", encodeType(typeParams[0]), encodeType(typeParams[1]));
            }
            else if (BiFunction.class.equals(raw))
            {
                return new Encoded("javaBiFunction", encodeType(typeParams[0]), encodeType(typeParams[1]), encodeType(typeParams[2]));
            }
            else if (BiPredicate.class.equals(raw))
            {
                return new Encoded("javaBiPredicate", encodeType(typeParams[0]), encodeType(typeParams[1]));
            }
            else if (Supplier.class.equals(raw))
            {
                return new Encoded("javaSupplier", encodeType(typeParams[0]));
            }
            else if (Consumer.class.equals(raw))
            {
                return new Encoded("javaConsumer", encodeType(typeParams[0]));
            }
            else if (javaClasses.containsKey(raw))
            {
                List<Encoded> params = Arrays.stream(typeParams).map(this::encodeType).collect(Collectors.toList());
                return new Encoded("javaParameterizedType", new Encoded(javaClasses.get(raw)), new Encoded(params));
            }
        }
        else if (type instanceof TypeVariable)
        {
            return new Encoded("javaTypeVar", ((TypeVariable) type).getName());
        }
        else if (type instanceof WildcardType)
        {
            WildcardType w = (WildcardType) type;
            List<Encoded> lowers = Arrays.stream(w.getLowerBounds()).map(this::encodeType).collect(Collectors.toList());
            List<Encoded> uppers = Arrays.stream(w.getLowerBounds()).map(this::encodeType).collect(Collectors.toList());
            if (lowers.isEmpty() && uppers.isEmpty())
            {
                return new Encoded("javaWildcard");
            }
            else if (lowers.isEmpty())
            {
                return new Encoded("javaWildcardExtends", new Encoded(uppers));
            }
            else if (uppers.isEmpty())
            {
                return new Encoded("javaWildcardSuper", new Encoded(lowers));
            }
            else
            {
                return new Encoded("javaWildcardType", new Encoded(lowers), new Encoded(uppers));
            }
        }
        else if (type instanceof Class<?> && org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate.class.equals(type))
        {
            return new Encoded("javaClass", ((Class<?>) type).getCanonicalName());
        }
        throw new IllegalArgumentException("Cannot encode: " + type);
    }

    private String generate()
    {
        javaClasses.values().forEach(JavaClass::computeDependencies);
        Output out = new Output();
        if (extensionName == null)
        {
            mainHeader(out);
        }
        else
        {
            out.indent();
            out.indent();
            extensionHeader(out);
        }

        List<JavaClass> pending = new ArrayList<>(javaClasses.values());
        while (!pending.isEmpty())
        {
            JavaClass toDeclare = pending.stream()
                    .filter(jc -> !jc.dependsOnUndeclared())
                    .findFirst()
                    .orElse(pending.get(0));
            toDeclare.declaration(out);
            out.lineOut("");
            pending.remove(toDeclare);
        }

        if (extensionName == null)
        {
            out.startLine("let res = $conventions");
            out.indent();
            providedTypes.forEach(pt -> pt.accept(out));
            out.outdent();
            out.finishLine(";");
            out.lineOut("");
            out.lineOut("$extensions.plan_javaRuntime_enginePlatformDependencies_conventions->fold({e,b|$e->eval($b)}, $res);");
            out.outdent();
            out.lineOut("}");
        }
        else
        {
            out.startLine("$conventions");
            out.indent();
            providedTypes.forEach(pt -> pt.accept(out));
            out.outdent();
            out.finishLine(";");
            out.outdent();
            out.lineOut("},");
        }

        return out.toString();
    }

    private void mainHeader(Output out)
    {
        // Splits to avoid checkstyle error  
        out.lineOut("// Copy" + "right " + LocalDate.now().getYear() + " Goldman Sachs");
        out.lineOut("//");
        out.lineOut("// Licensed under the Apache License, Version 2.0 (the \"License\");");
        out.lineOut("// you may not use this file except in compliance with the License.");
        out.lineOut("// You may obtain a copy of the License at");
        out.lineOut("//");
        out.lineOut("//      http://www.apache.org" + "/licenses/LICENSE-2.0");
        out.lineOut("//");
        out.lineOut("// Unless required by applicable law or agreed to in writing, software");
        out.lineOut("// distributed under the License is distributed on an \"AS IS\" BASIS,");
        out.lineOut("// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.");
        out.lineOut("// See the License for the specific language governing permissions and");
        out.lineOut("// limitations under the License.");
        out.lineOut("");
        out.lineOut("/*");
        out.lineOut(" * Generated main configuration: see " + GeneratePureConfig.class.getCanonicalName());
        out.lineOut(" */");
        out.lineOut("");
        out.lineOut("import " + PURE_PACKAGE + "*;");
        out.lineOut("import meta::java::generation::convention::*;");
        out.lineOut("import meta::java::metamodel::factory::*;");
        out.lineOut("");
        classes.forEach(c -> c.accept(out));
        out.lineOut("");
        out.lineOut("function " + PURE_PACKAGE + "applyJavaEngineDependencies(conventions:Conventions[1], extensions:meta::pure::router::extension::RouterExtension[*]):Conventions[1]");
        out.lineOut("{");
        out.indent();
    }

    private void extensionHeader(Output out)
    {
        out.lineOut("plan_javaRuntime_enginePlatformDependencies_conventions =");
        out.indent();
        out.lineOut("{conventions : Conventions[1] |");
        out.indent();
        out.lineOut("/*");
        out.lineOut(" * Generated extension " + extensionName + " configuration: see " + GeneratePureConfig.class.getCanonicalName());
        out.lineOut(" */");

        if (!predefinedJavaClasses.isEmpty())
        {
            out.lineOut("");
            predefinedJavaClasses.values().forEach(p -> p.declaration(out));
        }

        out.lineOut("");
    }

    private static class Output
    {
        private final StringBuilder builder = new StringBuilder();
        private int indent = 0;

        void indent()
        {
            indent++;
        }

        void outdent()
        {
            indent--;
        }

        void lineOut(String line)
        {
            startLine(line);
            finishLine("");
        }

        void startLine(String line)
        {
            if (!line.isEmpty())
            {
                IntStream.range(0, indent).forEach(i -> builder.append("   "));
                builder.append(line);
            }
        }

        void finishLine(String remaining)
        {
            builder.append(remaining);
            builder.append("\n");
        }

        public String toString()
        {
            return builder.toString();
        }
    }

    private class JavaClass
    {
        private final Class<?> clazz;
        private final List<JavaMethod> methods = new ArrayList<>();
        private boolean declared = false;

        JavaClass(Class<?> clazz)
        {
            this.clazz = clazz;
        }

        String variable()
        {
            return 'j' + clazz.getSimpleName();
        }

        void computeDependencies()
        {
            Arrays.stream(clazz.getDeclaredMethods())
                    .filter(m -> !m.isSynthetic())
                    .map(JavaMethod::new)
                    .forEach(methods::add);
        }

        String reference()
        {
            return '$' + variable();
        }

        String referenceOrConstruction()
        {
            return this.declared
                    ? reference()
                    : construction();
        }

        void declaration(Output out)
        {
            out.startLine("let " + variable() + " = " + this.construction());
            out.indent();
            this.methods.stream()
                    .sorted()
                    .map(JavaMethod::construction)
                    .forEach(d ->
                    {
                        out.finishLine("");
                        out.startLine("->addMethod(" + d + ")");
                    });
            this.declared = true;
            out.finishLine(";");
            out.outdent();
        }

        String construction()
        {
            return new StringBuilder()
                    .append("javaClass('public', '")
                    .append(clazz.getCanonicalName())
                    .append("')")
                    .toString();
        }

        boolean dependsOnUndeclared()
        {
            return dependsOn().stream().anyMatch(JavaClass::isUndeclared);
        }

        boolean isUndeclared()
        {
            return !this.declared;
        }

        Set<JavaClass> dependsOn()
        {
            Set<JavaClass> result = new HashSet<>();
            methods.stream().map(JavaMethod::dependsOn).forEach(result::addAll);
            result.remove(this);
            return result;
        }
    }

    private class PredefinedJavaClass
    {
        private final JavaClass predefined;

        PredefinedJavaClass(JavaClass predefined)
        {
            this.predefined = predefined;
        }

        String reference()
        {
            return predefined.reference();
        }

        void declaration(Output out)
        {
            out.lineOut("let " + predefined.variable() + " = $conventions->className(" + JAVA_TO_PURE_CLASSES.get(predefined.clazz) + ");");
        }
    }

    private class JavaMethod implements Comparable<JavaMethod>
    {
        private final Method method;
        private final Encoded returnType;
        private final List<Encoded> paramTypes;

        JavaMethod(Method method)
        {
            this.method = Objects.requireNonNull(method);
            this.returnType = GeneratePureConfig.this.encodeType(method.getGenericReturnType());
            this.paramTypes = Arrays.stream(method.getGenericParameterTypes())
                    .map(GeneratePureConfig.this::encodeType)
                    .collect(Collectors.toList());
        }

        Set<JavaClass> dependsOn()
        {
            Set<JavaClass> result = new HashSet<>(returnType.dependsOn());
            this.paramTypes.stream().map(Encoded::dependsOn).forEach(result::addAll);
            return result;
        }

        String construction()
        {
            StringBuilder builder = new StringBuilder()
                    .append("javaMethod(")
                    .append("'public'")
                    .append(", ")
                    .append(this.returnType.code())
                    .append(", '")
                    .append(this.method.getName())
                    .append("', [");

            for (int i = 0; i < paramTypes.size(); i++)
            {
                String type = this.paramTypes.get(i).code();
                String name = "p" + i;
                if (i > 0)
                {
                    builder.append(", ");
                }
                builder.append("javaParam(").append(type).append(", '").append(name).append("')");
            }

            return builder
                    .append("])")
                    .toString();
        }

        @Override
        public int compareTo(JavaMethod other)
        {
            int result = this.method.getName().compareTo(other.method.getName());
            for (int i = 0; result == 0; i++)
            {
                if (this.paramTypes.size() <= i)
                {
                    result = -1;
                }
                else if (other.paramTypes.size() <= i)
                {
                    result = 1;
                }
                else
                {
                    result = this.paramTypes.get(i).code().compareTo(other.paramTypes.get(i).code());
                }
            }
            return result;
        }
    }

    private class Encoded
    {
        private final String func;
        private final JavaClass javaClass;
        private final List<Encoded> values;
        private final List<String> stringArgs;
        private final PredefinedJavaClass predefinedJavaClass;

        Encoded(String func)
        {
            this.javaClass = null;
            this.func = func;
            this.values = null;
            this.stringArgs = null;
            this.predefinedJavaClass = null;
        }

        Encoded(String func, Encoded... args)
        {
            this.javaClass = null;
            this.func = func;
            this.values = Arrays.asList(args);
            this.stringArgs = null;
            this.predefinedJavaClass = null;
        }

        Encoded(String func, String... args)
        {
            this.javaClass = null;
            this.func = func;
            this.values = null;
            this.stringArgs = Arrays.asList(args);
            this.predefinedJavaClass = null;
        }

        Encoded(List<Encoded> elements)
        {
            this.javaClass = null;
            this.func = null;
            this.values = elements;
            this.stringArgs = null;
            this.predefinedJavaClass = null;
        }

        Encoded(JavaClass javaClass)
        {
            this.javaClass = javaClass;
            this.func = null;
            this.values = null;
            this.stringArgs = null;
            this.predefinedJavaClass = null;
        }

        Encoded(PredefinedJavaClass predefinedJavaClass)
        {
            this.javaClass = null;
            this.func = null;
            this.values = null;
            this.stringArgs = null;
            this.predefinedJavaClass = predefinedJavaClass;
        }

        Set<JavaClass> dependsOn()
        {
            Set<JavaClass> result = new HashSet<>();
            if (javaClass != null)
            {
                result.add(javaClass);
            }
            if (values != null)
            {
                values.stream().map(Encoded::dependsOn).forEach(result::addAll);
            }
            return result;
        }

        String code()
        {
            String valueCodes = "";
            if (this.values != null)
            {
                valueCodes = this.values.stream().map(Encoded::code).collect(Collectors.joining(", "));
            }
            else if (this.stringArgs != null)
            {
                valueCodes = this.stringArgs.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "));
            }

            if (this.func != null)
            {
                return this.func + '(' + valueCodes + ')';
            }
            else if (this.javaClass != null)
            {
                return GeneratePureConfig.this.javaClasses.get(this.javaClass.clazz).referenceOrConstruction();
            }
            else if (this.predefinedJavaClass != null)
            {
                return this.predefinedJavaClass.reference();
            }
            else
            {
                return '[' + valueCodes + ']';
            }
        }
    }
}
