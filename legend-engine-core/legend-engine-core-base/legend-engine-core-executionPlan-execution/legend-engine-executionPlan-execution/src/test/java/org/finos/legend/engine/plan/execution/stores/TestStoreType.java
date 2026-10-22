// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestStoreType
{
    private static final String[] BUILT_INS =
            {"Relational", "Service", "InMemory", "ESv7", "NonRelational_MongoDB", "Deephaven"};

    @Test
    public void builtInsKeepTheirNamesAndDeclarationOrder()
    {
        StoreType[] all = StoreType.values();
        assertTrue("values() must start with the built-ins", all.length >= BUILT_INS.length);

        String[] firstNames = Arrays.stream(all)
                .limit(BUILT_INS.length)
                .map(StoreType::name)
                .toArray(String[]::new);
        assertArrayEquals(BUILT_INS, firstNames);
    }

    @Test
    public void builtInOrdinalsAreUnchanged()
    {
        for (int i = 0; i < BUILT_INS.length; i++)
        {
            assertEquals(BUILT_INS[i], i, StoreType.valueOf(BUILT_INS[i]).ordinal());
        }
    }

    @Test
    public void constantFieldsAndValueOfAreTheSameInstances()
    {
        assertSame(StoreType.Relational, StoreType.valueOf("Relational"));
        assertSame(StoreType.Service, StoreType.valueOf("Service"));
        assertSame(StoreType.InMemory, StoreType.valueOf("InMemory"));
        assertSame(StoreType.ESv7, StoreType.valueOf("ESv7"));
        assertSame(StoreType.NonRelational_MongoDB, StoreType.valueOf("NonRelational_MongoDB"));
        assertSame(StoreType.Deephaven, StoreType.valueOf("Deephaven"));
    }

    @Test
    public void valueOfRejectsAnUnknownNameLikeTheEnumDid()
    {
        assertThrows(IllegalArgumentException.class, () -> StoreType.valueOf("NoSuchStore"));
    }

    @Test
    public void valueOfRejectsNull()
    {
        assertThrows(NullPointerException.class, () -> StoreType.valueOf(null));
    }

    @Test
    public void registerRejectsNull()
    {
        assertThrows(NullPointerException.class, () -> StoreType.register(null));
    }

    @Test
    public void registerIsIdempotentAndInterns()
    {
        StoreType first = StoreType.register("TestStore_Idempotent");
        StoreType second = StoreType.register("TestStore_Idempotent");

        assertSame(first, second);
        assertSame(first, StoreType.valueOf("TestStore_Idempotent"));
        assertEquals("TestStore_Idempotent", first.name());
        assertEquals("TestStore_Idempotent", first.toString());
    }

    @Test
    public void registeringTwiceDoesNotGrowValues()
    {
        StoreType.register("TestStore_NoDuplicate");
        int afterFirst = StoreType.values().length;
        StoreType.register("TestStore_NoDuplicate");

        assertEquals("a repeat registration must not add a second entry",
                afterFirst, StoreType.values().length);
    }

    @Test
    public void registeredTypeAppearsInValuesExactlyOnce()
    {
        StoreType registered = StoreType.register("TestStore_InValues");

        long occurrences = Arrays.stream(StoreType.values()).filter(t -> t == registered).count();
        assertEquals(1, occurrences);
    }

    @Test
    public void internedInstancesWorkAsMapKeys()
    {
        StoreType registered = StoreType.register("TestStore_MapKey");
        Map<StoreType, String> map = new HashMap<>();
        map.put(registered, "value");

        assertEquals("value", map.get(StoreType.valueOf("TestStore_MapKey")));
        assertEquals("value", map.get(StoreType.register("TestStore_MapKey")));
    }

    @Test
    public void compareToFollowsRegistrationOrder()
    {
        assertTrue(StoreType.Relational.compareTo(StoreType.Service) < 0);
        assertTrue(StoreType.Service.compareTo(StoreType.Relational) > 0);
        assertEquals(0, StoreType.Relational.compareTo(StoreType.Relational));

        StoreType late = StoreType.register("TestStore_Ordering");
        assertTrue("an extension registers after the built-ins",
                StoreType.Deephaven.compareTo(late) < 0);
    }

    @Test
    public void valuesReturnsADefensiveCopy()
    {
        StoreType[] values = StoreType.values();
        int size = values.length;
        values[0] = null;

        assertNotNull("mutating the returned array must not corrupt the registry",
                StoreType.values()[0]);
        assertEquals(size, StoreType.values().length);
    }

    @Test
    public void concurrentFirstRegistrationYieldsOneInstance() throws Exception
    {
        final int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try
        {
            CyclicBarrier startTogether = new CyclicBarrier(threads);
            List<Callable<StoreType>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++)
            {
                tasks.add(() ->
                {
                    startTogether.await(10, TimeUnit.SECONDS);
                    return StoreType.register("TestStore_Concurrent");
                });
            }

            List<Future<StoreType>> futures = pool.invokeAll(tasks);
            List<StoreType> results = new ArrayList<>();
            for (Future<StoreType> future : futures)
            {
                results.add(future.get(30, TimeUnit.SECONDS));
            }

            assertEquals("every thread must observe the same interned instance",
                    1, results.stream().distinct().count());
            assertEquals("the racing registration must add exactly one entry",
                    1, Arrays.stream(StoreType.values())
                            .filter(t -> "TestStore_Concurrent".equals(t.name()))
                            .count());
        }
        finally
        {
            pool.shutdownNow();
        }
    }

    @Test
    public void namesAreUniqueAcrossTheRegistry()
    {
        List<String> names = Arrays.stream(StoreType.values()).map(StoreType::name).collect(Collectors.toList());
        assertEquals("no two registered store types may share a name",
                names.size(), names.stream().distinct().count());
    }

    @Test
    public void serialisesAsItsNameExactlyAsTheEnumDid() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("\"Relational\"", mapper.writeValueAsString(StoreType.Relational));
        assertSame(StoreType.Relational, mapper.readValue("\"Relational\"", StoreType.class));
    }

    @Test
    public void deserialisingAnUnregisteredNameRegistersItRatherThanFailing() throws Exception
    {
        StoreType parsed = new ObjectMapper().readValue("\"TestStore_FromJson\"", StoreType.class);
        assertSame(parsed, StoreType.valueOf("TestStore_FromJson"));
    }

    @Test
    public void serialisesAsABeanPropertyOfAStoreExecutorConfiguration() throws Exception
    {
        assertEquals("{\"storeType\":\"Relational\"}",
                new ObjectMapper().writeValueAsString(new RelationalishConfiguration()));
    }

    @Test
    public void javaSerializationRoundTripsToTheSameInstance() throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes))
        {
            out.writeObject(StoreType.Deephaven);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
        {
            assertSame("readResolve must re-intern, or == stops holding after a round trip",
                    StoreType.Deephaven, in.readObject());
        }
    }

    public static class RelationalishConfiguration implements StoreExecutorConfiguration
    {
        @Override
        public StoreType getStoreType()
        {
            return StoreType.Relational;
        }
    }
}
