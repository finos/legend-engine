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

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.tuple.Pair;
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
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.inMemory.DataParsingException;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IGraphFetchM2MExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReadingExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryPropertyGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryRootGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryRootGraphFetchMergeExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IStoreStreamReadingExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.IGraphSerializer;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeGraphFetchMergeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeGraphFetchUnionSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.ISerializationWriter;
import org.finos.legend.engine.plan.dependencies.store.platform.PredefinedExpressions;
import org.finos.legend.engine.plan.dependencies.store.relational.IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.classResult.IRelationalClassInstantiationNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalChildGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalClassQueryTempTableGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalCrossRootGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalCrossRootQueryTempTableGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalPrimitiveQueryGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalRootGraphNodeExecutor;
import org.finos.legend.engine.plan.dependencies.store.relational.graphFetch.IRelationalRootQueryTempTableGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.serviceStore.IServiceParametersResolutionExecutionNodeSpecifics;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
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
 * Use this to generate the PURE code for integrating with this module or equivalent extension modules.
 *
 * Run the main() in this class to generate the main configuration and use the output to replace
 * all of /core/pure/executionPlan/javaPlatform/planConventions/enginePlatformDependencies.pure
 *
 * Extensions will use their own mains to generate a plan_javaRuntime_enginePlatformDependencies_conventions
 * value to be added to a router extension.
 */
public class GeneratePureConfig
{
    private static final String PURE_PACKAGE = "meta::pure::executionPlan::engine::";

    static final Map<String, Class<?>> MAIN_DEPENDENCIES = new LinkedHashMap<>();

    static
    {
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IConstantResult", IConstantResult.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "EngineIResult", IResult.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IExecutionNodeContext", IExecutionNodeContext.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IGraphFetchM2MExecutionNodeContext", IGraphFetchM2MExecutionNodeContext.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "StoreStreamReader", IStoreStreamReader.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "DataParsingException", DataParsingException.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "PredefinedExpressions", PredefinedExpressions.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "Library", Library.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "GraphInstance", IGraphInstance.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "RelationalRootGraphNodeExecutor", IRelationalRootGraphNodeExecutor.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "RelationalCrossRootGraphNodeExecutor", IRelationalCrossRootGraphNodeExecutor.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "RelationalChildGraphNodeExecutor", IRelationalChildGraphNodeExecutor.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "RelationalClassInstantiationNodeExecutor", IRelationalClassInstantiationNodeExecutor.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "PureDate", PureDate.class);
        MAIN_DEPENDENCIES.put("meta::pure::functions::date::DurationUnit", DurationUnit.class);
        MAIN_DEPENDENCIES.put("meta::pure::functions::date::DayOfWeek", DayOfWeek.class);
        MAIN_DEPENDENCIES.put("meta::pure::functions::date::Month", Month.class);
        MAIN_DEPENDENCIES.put("meta::pure::functions::date::Quarter", Quarter.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "DataQualityBasicDefectClass", BasicDefect.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "DataQualityBasicRelativePathNodeClass", BasicRelativePathNode.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "DataQualityGraphContextClass", GraphContext.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "DataQualityConstrainedInterface", Constrained.class);
        MAIN_DEPENDENCIES.put("meta::pure::dataQuality::Checked", IChecked.class);
        MAIN_DEPENDENCIES.put("meta::pure::dataQuality::Defect", IDefect.class);
        MAIN_DEPENDENCIES.put("meta::pure::dataQuality::EnforcementLevel", EnforcementLevel.class);
        MAIN_DEPENDENCIES.put("meta::pure::dataQuality::RuleType", RuleType.class);
        MAIN_DEPENDENCIES.put("meta::pure::dataQuality::RelativePathNode", RelativePathNode.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IReferencedObject", IReferencedObject.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics", IRelationalCreateAndPopulateTempTableExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IPlatformPureExpressionExecutionNodeSerializeSpecifics", IPlatformPureExpressionExecutionNodeSerializeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IPlatformPureExpressionExecutionNodeGraphFetchUnionSpecifics", IPlatformPureExpressionExecutionNodeGraphFetchUnionSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IPlatformPureExpressionExecutionNodeGraphFetchMergeSpecifics", IPlatformPureExpressionExecutionNodeGraphFetchMergeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "ISerializationWriter", ISerializationWriter.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IGraphSerializer", IGraphSerializer.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IStoreStreamReadingExecutionNodeSpecifics", IStoreStreamReadingExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IStoreStreamReadingExecutionNodeContext", IStoreStreamReadingExecutionNodeContext.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IInMemoryRootGraphFetchExecutionNodeSpecifics", IInMemoryRootGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IInMemoryRootGraphFetchMergeExecutionNodeSpecifics", IInMemoryRootGraphFetchMergeExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IInMemoryPropertyGraphFetchExecutionNodeSpecifics", IInMemoryPropertyGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IRelationalRootQueryTempTableGraphFetchExecutionNodeSpecifics", IRelationalRootQueryTempTableGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IRelationalCrossRootQueryTempTableGraphFetchExecutionNodeSpecifics", IRelationalCrossRootQueryTempTableGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics", IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IRelationalClassQueryTempTableGraphFetchExecutionNodeSpecifics", IRelationalClassQueryTempTableGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IRelationalPrimitiveQueryGraphFetchExecutionNodeSpecifics", IRelationalPrimitiveQueryGraphFetchExecutionNodeSpecifics.class);
        MAIN_DEPENDENCIES.put(PURE_PACKAGE + "IServiceParametersResolutionExecutionNodeSpecifics", IServiceParametersResolutionExecutionNodeSpecifics.class);
    }

    private final Class<?> extensionClass;

    public static void main(String[] args)
    {
        System.out.println(new GeneratePureConfig().generate());
    }

    private final Map<String, Class<?>> pureToJavaClasses = new LinkedHashMap<>();
    private final GeneratePureConfig mainConfig;
    private final String extensionName;
    private final String purePackage;

    private final List<Consumer<Output>> classes = new ArrayList<>();
    private final List<Consumer<Output>> providedTypes = new ArrayList<>();
    private final Map<Type, JavaClass> javaClasses = new LinkedHashMap<>();
    private final Map<Type, EncodeableType> standardTypes = new HashMap<>();
    private final Map<Type, PredefinedJavaClass> predefinedJavaClasses = new LinkedHashMap<>();

    private GeneratePureConfig()
    {
        this.mainConfig = null;
        this.extensionClass = null;
        this.purePackage = PURE_PACKAGE;
        initStandardTypes();
        MAIN_DEPENDENCIES.forEach(this::addClass);
        this.extensionName = null;
    }

    public GeneratePureConfig(String extensionName, Class<?> extensionClass, String purePackage)
    {
        this.mainConfig = new GeneratePureConfig();
        this.extensionClass = extensionClass;
        this.purePackage = purePackage;
        initStandardTypes();
        this.extensionName = Objects.requireNonNull(extensionName);
    }

    private void initStandardTypes()
    {
        standardTypes.put(Character.TYPE, new FactoryType("javaChar"));
        standardTypes.put(Integer.TYPE, new FactoryType("javaInt"));
        standardTypes.put(Long.TYPE, new FactoryType("javaLong"));
        standardTypes.put(Float.TYPE, new FactoryType("javaFloat"));
        standardTypes.put(Double.TYPE, new FactoryType("javaDouble"));
        standardTypes.put(Boolean.TYPE, new FactoryType("javaBoolean"));
        standardTypes.put(Void.TYPE, new FactoryType("javaVoid"));
        standardTypes.put(Character.class, new FactoryType("javaCharBoxed"));
        standardTypes.put(Integer.class, new FactoryType("javaIntBoxed"));
        standardTypes.put(Long.class, new FactoryType("javaLongBoxed"));
        standardTypes.put(Float.class, new FactoryType("javaFloatBoxed"));
        standardTypes.put(Double.class, new FactoryType("javaDoubleBoxed"));
        standardTypes.put(Boolean.class, new FactoryType("javaBooleanBoxed"));
        standardTypes.put(Object.class, new FactoryType("javaObject"));
        standardTypes.put(String.class, new FactoryType("javaString"));
        standardTypes.put(Date.class, new FactoryType("javaDate"));
        standardTypes.put(Instant.class, new FactoryType("javaInstant"));
        standardTypes.put(LocalDate.class, new FactoryType("javaLocalDate"));
        standardTypes.put(Temporal.class, new FactoryType("javaTemporal"));
        standardTypes.put(java.sql.Date.class, new FactoryType("javaSqlDate"));
        standardTypes.put(java.sql.Timestamp.class, new FactoryType("javaSqlTimestamp"));
        standardTypes.put(Appendable.class, new FactoryType("javaAppendable"));
        standardTypes.put(Number.class, new FactoryType("javaNumber"));
        standardTypes.put(BigInteger.class, new FactoryType("javaBigInteger"));
        standardTypes.put(BigDecimal.class, new FactoryType("javaBigDecimal"));
        standardTypes.put(Calendar.class, new FactoryType("javaCalendar"));
        standardTypes.put(GregorianCalendar.class, new FactoryType("javaGregorianCalendar"));
        standardTypes.put(Type.class, new FactoryType("javaReflectType"));
        standardTypes.put(Method.class, new FactoryType("javaReflectMethod"));
        standardTypes.put(ResultSet.class, new FactoryType("javaResultSet"));
        standardTypes.put(StringBuilder.class, new FactoryType("javaStringBuilder"));
        standardTypes.put(URL.class, new FactoryType("javaURL"));
    }

    public GeneratePureConfig addClass(String pureClassPath, Class<?> clazz)
    {
        pureToJavaClasses.put(pureClassPath, clazz);
        return (pureClassPath.startsWith(purePackage))
                ? this.defineClass(pureClassPath, clazz)
                : this.mapClass(pureClassPath, clazz);
    }

    public String pureClassFor(Class<?> clazz)
    {
        return pureToJavaClasses.entrySet().stream().filter(kv -> kv.getValue().equals(clazz)).findFirst().map(Map.Entry::getKey)
                .orElseGet(() ->
                {
                    if (mainConfig == null)
                    {
                        throw new IllegalArgumentException("No mapping for " + clazz.getName());
                    }
                    return mainConfig.pureClassFor(clazz);
                });
    }

    private GeneratePureConfig defineClass(String pureClassPath, Class<?> clazz)
    {
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

    private EncodeableType encodeType(Type type)
    {
        if (standardTypes.containsKey(type))
        {
            return standardTypes.get(type);
        }
        else if (javaClasses.containsKey(type))
        {
            return javaClasses.get(type);
        }
        else if (predefinedJavaClasses.containsKey(type))
        {
            return predefinedJavaClasses.get(type);
        }
        else if (mainConfig != null && mainConfig.javaClasses.containsKey(type))
        {
            PredefinedJavaClass predefined = new PredefinedJavaClass(mainConfig.javaClasses.get(type));
            predefinedJavaClasses.put(type, predefined);
            return predefined;
        }
        else if (type instanceof GenericArrayType)
        {
            return new FactoryType("javaArray", encodeType(((GenericArrayType) type).getGenericComponentType()));
        }
        else if (type instanceof Class && ((Class<?>) type).isArray())
        {
            return new FactoryType("javaArray", encodeType(((Class<?>) type).getComponentType()));
        }
        else if (type instanceof ParameterizedType)
        {
            Type raw = ((ParameterizedType) type).getRawType();
            Type[] typeParams = ((ParameterizedType) type).getActualTypeArguments();
            if (List.class.equals(raw))
            {
                return new FactoryType("javaList", encodeType(typeParams[0]));
            }
            if (Collection.class.equals(raw))
            {
                return new FactoryType("javaCollection", encodeType(typeParams[0]));
            }
            else if (Stream.class.equals(raw))
            {
                return new FactoryType("javaStream", encodeType(typeParams[0]));
            }
            else if (Predicate.class.equals(raw))
            {
                return new FactoryType("javaPredicate", encodeType(typeParams[0]));
            }
            else if (Comparator.class.equals(raw))
            {
                return new FactoryType("javaComparator", encodeType(typeParams[0]));
            }
            else if (Function.class.equals(raw))
            {
                return new FactoryType("javaFunction", encodeType(typeParams[0]), encodeType(typeParams[1]));
            }
            else if (BiFunction.class.equals(raw))
            {
                return new FactoryType("javaBiFunction", encodeType(typeParams[0]), encodeType(typeParams[1]), encodeType(typeParams[2]));
            }
            else if (BiPredicate.class.equals(raw))
            {
                return new FactoryType("javaBiPredicate", encodeType(typeParams[0]), encodeType(typeParams[1]));
            }
            else if (Supplier.class.equals(raw))
            {
                return new FactoryType("javaSupplier", encodeType(typeParams[0]));
            }
            else if (Consumer.class.equals(raw))
            {
                return new FactoryType("javaConsumer", encodeType(typeParams[0]));
            }
            else if (Pair.class.equals(raw))
            {
                List<EncodeableType> params = Arrays.stream(typeParams).map(this::encodeType).collect(Collectors.toList());
                return new FactoryType("javaParameterizedType", new FactoryType("javaClass", Pair.class.getCanonicalName()), params);
            }
            else if (Map.class.equals(raw))
            {
                List<EncodeableType> params = Arrays.stream(typeParams).map(this::encodeType).collect(Collectors.toList());
                return new FactoryType("javaParameterizedType", new FactoryType("javaClass", Map.class.getCanonicalName()), params);
            }
            else if (javaClasses.containsKey(raw))
            {
                List<EncodeableType> params = Arrays.stream(typeParams).map(this::encodeType).collect(Collectors.toList());
                return new FactoryType("javaParameterizedType", javaClasses.get(raw), params);
            }
            else if (raw instanceof Class<?>)
            {
                List<EncodeableType> params = Arrays.stream(typeParams).map(this::encodeType).collect(Collectors.toList());
                return new FactoryType("javaParameterizedType", encodeType(raw), params);
            }
        }
        else if (type instanceof TypeVariable)
        {
            return new FactoryType("javaTypeVar", ((TypeVariable) type).getName());
        }
        else if (type instanceof WildcardType)
        {
            WildcardType w = (WildcardType) type;
            List<EncodeableType> lowers = Arrays.stream(w.getLowerBounds()).map(this::encodeType).collect(Collectors.toList());
            List<EncodeableType> uppers = Arrays.stream(w.getLowerBounds()).map(this::encodeType).collect(Collectors.toList());
            if (lowers.isEmpty() && uppers.isEmpty())
            {
                return new FactoryType("javaWildcard");
            }
            else if (lowers.isEmpty())
            {
                return new FactoryType("javaWildcardExtends", uppers);
            }
            else if (uppers.isEmpty())
            {
                return new FactoryType("javaWildcardSuper", lowers);
            }
            else
            {
                return new FactoryType("javaWildcardType", lowers, uppers);
            }
        }
        else if (type instanceof Class<?>)
        {
            return new FactoryType("javaClass", ((Class<?>) type).getCanonicalName());
        }
        throw new IllegalArgumentException("Cannot encode: " + type);
    }

    public String generate()
    {
        javaClasses.values().forEach(JavaClass::computeDependencies);
        Output out = new Output();
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

        if (extensionName == null)
        {
            out.lineOut("/*");
            out.lineOut(" * Generated main configuration: see " + GeneratePureConfig.class.getCanonicalName());
            out.lineOut(" */");
        }
        else
        {
            out.lineOut("/*");
            out.lineOut(" * Generated extension " + extensionName + " class definitions: see " + extensionClass.getCanonicalName());
            out.lineOut(" */");
        }

        out.lineOut("");
        out.lineOut("import " + purePackage + "*;");
        out.lineOut("import meta::external::language::java::factory::*;");
        out.lineOut("import meta::external::language::java::transform::*;");
        out.lineOut("");

        if (!classes.isEmpty())
        {
            classes.forEach(c -> c.accept(out));
            out.lineOut("");
        }

        if (extensionName == null)
        {
            out.lineOut("function " + purePackage + "applyJavaEngineDependencies(conventions:Conventions[1], extensions:meta::pure::extension::Extension[*]):Conventions[1]");
        }
        else
        {
            out.lineOut("/*");
            out.lineOut(" * This function should be assigned to the router extension:");
            out.lineOut(" *");
            out.lineOut(" *     plan_javaRuntime_enginePlatformDependencies_conventions = " + purePackage + "extendJavaEngineDependencies_Conventions_1__Conventions_1_");
            out.lineOut(" */");
            out.lineOut("function " + purePackage + "extendJavaEngineDependencies(conventions:Conventions[1]):Conventions[1]");
        }
        out.lineOut("{");
        out.indent();

        if (!predefinedJavaClasses.isEmpty())
        {
            out.lineOut("");
            predefinedJavaClasses.values().forEach(p -> p.declaration(out));
        }

        out.lineOut("");

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
        }
        else
        {
            out.startLine("$conventions");
            out.indent();
            providedTypes.forEach(pt -> pt.accept(out));
            out.outdent();
            out.finishLine(";");
        }

        out.outdent();
        out.lineOut("}");

        return out.toString();
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

    private class JavaMethod implements Comparable<JavaMethod>
    {

        private final Method method;
        private final EncodeableType returnType;
        private final List<EncodeableType> paramTypes;

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
            Set<JavaClass> result = new HashSet<>();
            addDependency(result, returnType);
            this.paramTypes.forEach(t -> addDependency(result, t));
            return result;
        }

        private void addDependency(Set<JavaClass> set, EncodeableType type)
        {
            if (type instanceof JavaClass)
            {
                set.add((JavaClass) type);
            }
            else
            {
                set.addAll(type.dependsOn());
            }
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

    private abstract class EncodeableType
    {
        abstract String code();

        Set<JavaClass> dependsOn()
        {
            return Collections.emptySet();
        }
    }

    private class FactoryType extends EncodeableType
    {
        private final String func;
        private final List<Object> args;

        FactoryType(String func, Object... args)
        {
            this.func = func;
            this.args = Arrays.asList(args);
        }

        @Override
        Set<JavaClass> dependsOn()
        {
            return argumentsDependencies(args);
        }

        private Set<JavaClass> argumentsDependencies(List<?> arguments)
        {
            Set<JavaClass> result = Sets.mutable.empty();
            for (Object a : arguments)
            {
                if (a instanceof JavaClass)
                {
                    result.add((JavaClass) a);
                }
                else if (a instanceof EncodeableType)
                {
                    result.addAll(((EncodeableType) a).dependsOn());
                }
                else if (a instanceof List)
                {
                    result.addAll(argumentsDependencies((List<Objects>) a));
                }
            }
            return result;
        }

        @Override
        String code()
        {
            return this.func + "(" + codeArguments(args) + ")";
        }

        private String codeArguments(List<?> arguments)
        {
            return arguments.stream().map(a ->
            {
                if (a instanceof String)
                {
                    return "'" + a + "'";
                }
                else if (a instanceof EncodeableType)
                {
                    return ((EncodeableType) a).code();
                }
                else if (a instanceof List)
                {
                    return '[' + codeArguments((List<Objects>) a) + ']';
                }
                else
                {
                    throw new IllegalStateException("Invaliid argument value");
                }
            }).collect(Collectors.joining(", "));
        }
    }

    private class JavaClass extends EncodeableType
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
            Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getDeclaringClass() != Object.class)
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

        @Override
        String code()
        {
            return GeneratePureConfig.this.javaClasses.get(this.clazz).referenceOrConstruction();
        }

        @Override
        Set<JavaClass> dependsOn()
        {
            Set<JavaClass> result = new HashSet<>();
            methods.stream().map(JavaMethod::dependsOn).forEach(result::addAll);
            result.remove(this);
            return result;
        }
    }

    private class PredefinedJavaClass extends EncodeableType
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
            out.lineOut("let " + predefined.variable() + " = $conventions->className(" + pureClassFor(predefined.clazz) + ");");
        }

        @Override
        String code()
        {
            return reference();
        }
    }
}
