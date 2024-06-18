// Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.compiled;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.ordered.ReversibleIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.lazy.AbstractLazyIterable;
import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.TreeNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Nil;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.function.FunctionDescriptor;
import org.finos.legend.pure.m3.navigation.function.InvalidFunctionDescriptorException;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.SourceRegistry;
import org.finos.legend.pure.m3.tools.StatisticsUtil;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.StrictDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.Year;
import org.finos.legend.pure.m4.coreinstance.primitive.date.YearMonth;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Bridge;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureEqualsHashingStrategy;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.extension.functions.shared.cipher.AESCipherUtil;
import org.finos.legend.pure.runtime.java.shared.hash.HashType;
import org.finos.legend.pure.runtime.java.shared.hash.HashingUtil;
import org.finos.legend.pure.runtime.java.shared.identity.IdentityManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FunctionsHelper
{
    // Crypto ----------------------------------------------------------------
    public static String encrypt(String value, String key)
    {
        return performEncryption(value, key);
    }

    public static String encrypt(Number value, String key)
    {
        return performEncryption(value.toString(), key);
    }

    public static String encrypt(Boolean value, String key)
    {
        return performEncryption(value.toString(), key);
    }

    private static String performEncryption(String value, String key)
    {
        try
        {
            return new String(AESCipherUtil.encrypt(key, value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Error ciphering value '" + value + "' with key '" + key + "'.", e);
        }
    }

    public static String decrypt(String value, String key)
    {
        try
        {
            return new String(AESCipherUtil.decrypt(key, value.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new PureExecutionException("Error deciphering value '" + value + "' with key '" + key + "'.", e);
        }
    }
    // Crypto ----------------------------------------------------------------


    // DATE-TIME --------------------------------------------------------------
    public static StrictDate today()
    {
        return DateFunctions.today();
    }

    public static DateTime now()
    {
        return DateFunctions.fromInstant(Instant.now(), 3);
    }

    public static long weekOfYear(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get week of year for " + date);
        }
        return date.getCalendar().get(Calendar.WEEK_OF_YEAR);
    }

    public static long dayOfYear(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get day of year for " + date);
        }
        return date.getCalendar().get(Calendar.DAY_OF_YEAR);
    }

    public static long dayOfWeekNumber(PureDate date, SourceInformation sourceInformation)
    {
        if (!date.hasDay())
        {
            throw new PureExecutionException(sourceInformation, "Cannot get day of week for " + date);
        }
        switch (date.getCalendar().get(Calendar.DAY_OF_WEEK))
        {
            case Calendar.MONDAY:
            {
                return 1;
            }
            case Calendar.TUESDAY:
            {
                return 2;
            }
            case Calendar.WEDNESDAY:
            {
                return 3;
            }
            case Calendar.THURSDAY:
            {
                return 4;
            }
            case Calendar.FRIDAY:
            {
                return 5;
            }
            case Calendar.SATURDAY:
            {
                return 6;
            }
            case Calendar.SUNDAY:
            {
                return 7;
            }
            default:
            {
                throw new PureExecutionException(sourceInformation, "Error getting day of week for " + date);
            }
        }
    }

    // DATE-TIME --------------------------------------------------------------


    // MATH --------------------------------------------------------------------
    public static Number stdDev(RichIterable<? extends Number> list, boolean isBiasCorrected, SourceInformation sourceInformation)
    {
        if (list == null || list.isEmpty())
        {
            throw new PureExecutionException(sourceInformation, "Unable to process empty list");
        }
        MutableList<Number> javaNumbers = Lists.mutable.withAll(list);
        double[] values = new double[javaNumbers.size()];
        for (int i = 0; i < javaNumbers.size(); i++)
        {
            values[i] = javaNumbers.get(i).doubleValue();
        }
        return StatisticsUtil.standardDeviation(values, isBiasCorrected);
    }
    // MATH --------------------------------------------------------------------


    // COLLECTION ---------------------------------------------------------------

    /**
     * Return a list consisting of element repeated n times.
     *
     * @param element element to repeat
     * @param n       number of times to repeat element
     * @param <T>     element type
     * @return element repeated n times
     */
    public static <T> RichIterable<T> repeat(T element, long n)
    {
        if (n <= 0)
        {
            return Lists.immutable.empty();
        }
        int num = (int) n;
        MutableList<T> elements = Lists.mutable.ofInitialCapacity(num);
        for (; num > 0; num--)
        {
            elements.add(element);
        }
        return elements;
    }

    public static Object get(RichIterable<?> list, String id)
    {
        return list.detect(e -> id.equals(((CoreInstance) e).getName()));
    }

    // COLLECTION ---------------------------------------------------------------


    // HASH------ ---------------------------------------------------------------
    public static String hash(String text, Object hashTypeObject)
    {
        Enum hashTypeEnum = (Enum) hashTypeObject;
        HashType hashType = HashType.valueOf(hashTypeEnum._name());

        return HashingUtil.hash(text, hashType);
    }
    // HASH------ ---------------------------------------------------------------


    // META ---------------------------------------------------------------------
    public static String functionDescriptorToId(String functionDescriptor, SourceInformation sourceInformation)
    {
        try
        {
            return FunctionDescriptor.functionDescriptorToId(functionDescriptor);
        }
        catch (InvalidFunctionDescriptorException e)
        {
            throw new PureExecutionException(sourceInformation, "Invalid function descriptor: " + functionDescriptor, e);
        }
    }

    public static boolean isValidFunctionDescriptor(String possiblyFunctionDescriptor)
    {
        return FunctionDescriptor.isValidFunctionDescriptor(possiblyFunctionDescriptor);
    }

    public static boolean isSourceReadOnly(String sourceName, ExecutionSupport es)
    {
        return isSourceReadOnly(((CompiledExecutionSupport) es).getSourceRegistry(), sourceName);
    }

    public static boolean isSourceReadOnly(SourceRegistry sourceRegistry, String sourceName)
    {
        if (sourceRegistry == null)
        {
            throw new RuntimeException("The source registry has not been defined... This function should probably not be used in your current environment.");
        }
        return sourceRegistry.getSource(sourceName).isImmutable();
    }
    // META ---------------------------------------------------------------------



    // STRING -------------------------------------------------------------------
    public static String encodeBase64(String str)
    {
        return Base64.encodeBase64URLSafeString(str.getBytes());
    }

    public static String decodeBase64(String str)
    {
        return new String(Base64.decodeBase64(str));
    }

    public static String encodeUrl(String str, String charset)
    {
        try
        {
            return URLEncoder.encode(str, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String decodeUrl(String str, String charset)
    {
        try
        {
            return URLDecoder.decode(str, charset);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static long ascii(String str)
    {
        return str.length() > 0 ? (int) str.charAt(0) : 0;
    }

    public static String character(Number number)
    {
        return String.valueOf((char) number.intValue());
    }



    public static boolean matches(String str, String regexp)
    {
        return str.matches(regexp);
    }

    public static Double jaroWinklerSimilarity(String str1, String str2)
    {
        return new JaroWinklerSimilarity().apply(str1, str2);
    }

    public static Long levenshteinDistance(String str1, String str2)
    {
        Integer integerValue = new LevenshteinDistance().apply(str1, str2);
        if (integerValue != null)
        {
            return integerValue.longValue();
        }
        else
        {
            return 0L;
        }
    }

    public static RichIterable<String> chunk(String text, long size, SourceInformation sourceInformation)
    {
        if (size < 1)
        {
            throw new PureExecutionException(sourceInformation, "Invalid chunk size: " + size);
        }
        return chunk(text, (int) size);
    }

    private static RichIterable<String> chunk(String text, int size)
    {
        int length = text.length();
        if (length == 0)
        {
            return Lists.immutable.empty();
        }

        if (size >= length)
        {
            return Lists.immutable.with(text);
        }

        return new AbstractLazyIterable<String>()
        {
            @Override
            public boolean isEmpty()
            {
                return false;
            }

            @Override
            public int size()
            {
                return (length + size - 1) / size;
            }

            @Override
            public void forEach(Consumer<? super String> consumer)
            {
                for (int i = 0; i < length; i += size)
                {
                    consumer.accept(text.substring(i, Math.min(i + size, length)));
                }
            }

            @Override
            public void each(Procedure<? super String> procedure)
            {
                forEach((Consumer<? super String>) procedure);
            }

            @Override
            public Iterator<String> iterator()
            {
                return new Iterator<String>()
                {
                    private int current = 0;

                    @Override
                    public boolean hasNext()
                    {
                        return this.current < length;
                    }

                    @Override
                    public String next()
                    {
                        if (!hasNext())
                        {
                            throw new NoSuchElementException();
                        }
                        int start = this.current;
                        int end = Math.min(start + size, length);
                        String next = text.substring(start, end);
                        this.current = end;
                        return next;
                    }
                };
            }
        };
    }

    // STRING -------------------------------------------------------------------


    // Runtime ------------------------------------------------------------------
    public static String currentUserId()
    {
        return IdentityManager.getAuthenticatedUserId();
    }

    public static boolean isOptionSet(String name, ExecutionSupport es)
    {
        return ((CompiledExecutionSupport) es).getRuntimeOptions().isOptionSet(name);
    }

    public static String guid()
    {
        return UUID.randomUUID().toString();
    }
    // Runtime ------------------------------------------------------------------



    // IO -----------------------------------------------------------------------
    public static String readFile(String path, String lineSeparator, ExecutionSupport es)
    {
        RepositoryCodeStorage codeStorage = ((CompiledExecutionSupport) es).getCodeStorage();
        if (!codeStorage.exists(path))
        {
            return null;
        }
        String content = codeStorage.getContentAsText(path);
        return (lineSeparator == null) ? content : content.replaceAll("\\R", lineSeparator);
    }
    // IO -----------------------------------------------------------------------


    // Lang ---------------------------------------------------------------------
    public static <T> T mutateAdd(T val, String property, RichIterable<? extends Object> vals, SourceInformation sourceInformation)
    {
        try
        {
            Method m = val.getClass().getMethod("_" + property);
            if (m.getReturnType() == RichIterable.class)
            {
                RichIterable l = (RichIterable) m.invoke(val);
                RichIterable newValues = Iterate.isEmpty(l) ? vals : LazyIterate.concatenate(l, vals).toList();

                m = val.getClass().getMethod("_" + property, RichIterable.class);
                m.invoke(val, newValues);
            }
            else
            {
                m = val.getClass().getMethod("_" + property, m.getReturnType());
                m.invoke(val, vals.getFirst());
            }
        }
        catch (NoSuchMethodException e)
        {
            throw new PureExecutionException(sourceInformation, "Cannot find property '" + property + "' on " + CompiledSupport.getPureGeneratedClassName(val));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return val;
    }
    // Lang ---------------------------------------------------------------------



    // Trace ---------------------------------------------------------------------
    private static final ExecutorService traceAsyncExecutor = Executors.newCachedThreadPool(new ThreadFactory()
    {
        private final ThreadGroup group = System.getSecurityManager() == null
                ? Thread.currentThread().getThreadGroup()
                : System.getSecurityManager().getThreadGroup();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r)
        {
            Thread thread = new Thread(this.group, r, "trace-async-executor-thread-" + this.threadNumber.getAndIncrement(), 0);
            if (!thread.isDaemon())
            {
                thread.setDaemon(true);
            }
            if (thread.getPriority() != Thread.NORM_PRIORITY)
            {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    });

    public static Object traceSpan(ExecutionSupport es,
                                   org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function,
                                   String operationName,
                                   org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> funcToGetTags,
                                   boolean tagsCritical,
                                   Bridge bridge)
    {
        if (!GlobalTracer.isRegistered())
        {
            return Pure.evaluate(es, function, bridge, Lists.mutable.empty());
        }

        Span span = GlobalTracer.get().buildSpan(operationName).start();
        try (Scope scope = GlobalTracer.get().scopeManager().activate(span))
        {
            if ((funcToGetTags != null) && (span != null))
            {
                try
                {
                    Future<?> future = traceAsyncExecutor.submit(() ->
                    {
                        try (Scope scope1 = GlobalTracer.get().scopeManager().activate(span))
                        {
                            MutableMap<?, ?> tags = ((PureMap) Pure.evaluate(es, funcToGetTags, bridge, Lists.mutable.empty())).getMap();
                            tags.forEachKeyValue((tag, value) -> span.setTag((String) tag, (String) value));
                        }
                    });
                    future.get(60, TimeUnit.SECONDS);
                }
                catch (TimeoutException e)
                {
                    span.setTag("Exception", "Timeout received before tags could be resolved");
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (Exception e)
                {
                    if (tagsCritical)
                    {
                        throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
                    }
                    span.setTag("Exception", "Unable to resolve tags - [" + e.getMessage() + "]");
                }
            }
            return Pure.evaluate(es, function, bridge, Lists.mutable.empty());
        }
        finally
        {
            if (span != null)
            {
                span.finish();
            }
        }
    }
    // Trace ---------------------------------------------------------------------



    public static Object alloyTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> regular, Bridge bridge)
    {
        String host = System.getProperty("alloy.test.server.host");
        long port = System.getProperty("alloy.test.server.port") == null ? -1 : Long.parseLong(System.getProperty("alloy.test.server.port"));
        if (host != null && port == -1)
        {
            throw new PureExecutionException("The system variable 'alloy.test.server.host' is set to '" + host + "' however 'alloy.test.server.port' has not been set!");
        }
        String clientVersion = System.getProperty("alloy.test.clientVersion");
        String serverVersion = System.getProperty("alloy.test.serverVersion");
        return host != null ? Pure.evaluate(es, alloyTest, bridge, clientVersion, serverVersion, host, port) : Pure.evaluate(es, regular, bridge);
    }

    public static Object legendTest(ExecutionSupport es, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> alloyTest, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> regular, Bridge bridge)
    {
        String host = System.getProperty("legend.test.server.host");
        long port = System.getProperty("legend.test.server.port") == null ? -1 : Long.parseLong(System.getProperty("legend.test.server.port"));
        String clientVersion = System.getProperty("legend.test.clientVersion");
        String serverVersion = System.getProperty("legend.test.serverVersion");
        String serializationKind = System.getProperty("legend.test.serializationKind");
        if (host != null)
        {
            if (port == -1)
            {
                throw new PureExecutionException("The system variable 'legend.test.server.host' is set to '" + host + "' however 'legend.test.server.port' has not been set!");
            }
            if (serializationKind == null || !(serializationKind.equals("text") || serializationKind.equals("json")))
            {
                serializationKind = "json";
            }
            if (clientVersion == null)
            {
                throw new PureExecutionException("The system variable 'legend.test.clientVersion' should be set");
            }
            if (serverVersion == null)
            {
                throw new PureExecutionException("The system variable 'legend.test.serverVersion' should be set");
            }
        }
        return host != null ? Pure.evaluate(es, alloyTest, bridge, clientVersion, serverVersion, serializationKind, host, port) : Pure.evaluate(es, regular, bridge);
    }
}
