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

package org.finos.legend.engine.plan.execution.stores;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The set of stores a plan can execute against.
 *
 * <p>Not an enum: extensions contribute their own store types through {@link #register(String)}.
 * Instances are interned, so {@code ==} comparisons hold. {@link #values()} is therefore "everything
 * registered so far", not a fixed set.
 *
 * <p>Serialises to its name through Jackson and is {@code Serializable}, as an enum would be, because
 * {@code StoreExecutorConfiguration.getStoreType()} is a bean property on Dropwizard-bound configuration.
 */
public final class StoreType implements Comparable<StoreType>, Serializable
{
    private static final long serialVersionUID = 1L;

    // Declared before the constants: static initialisers run in textual order, and register() needs these.
    private static final Map<String, StoreType> REGISTRY = new ConcurrentHashMap<>();
    private static final List<StoreType> ORDERED = new CopyOnWriteArrayList<>();
    private static final Object REGISTRATION_LOCK = new Object();

    public static final StoreType Relational = register("Relational");
    public static final StoreType Service = register("Service");
    public static final StoreType InMemory = register("InMemory");
    public static final StoreType ESv7 = register("ESv7");
    public static final StoreType NonRelational_MongoDB = register("NonRelational_MongoDB");
    public static final StoreType Deephaven = register("Deephaven");

    private final String name;
    private final int ordinal;

    private StoreType(String name, int ordinal)
    {
        this.name = name;
        this.ordinal = ordinal;
    }

    /**
     * Returns the store type with this name, creating and interning it if it is not yet known.
     * This is the extension point: a distribution contributing a store calls this once, typically
     * from the static initialiser of its {@code StoreState} implementation.
     */
    public static StoreType register(String name)
    {
        Objects.requireNonNull(name, "store type name may not be null");
        StoreType existing = REGISTRY.get(name);
        if (existing != null)
        {
            return existing;
        }
        synchronized (REGISTRATION_LOCK)
        {
            existing = REGISTRY.get(name);
            if (existing != null)
            {
                return existing;
            }
            StoreType created = new StoreType(name, ORDERED.size());
            ORDERED.add(created);
            REGISTRY.put(name, created);
            return created;
        }
    }

    public static StoreType valueOf(String name)
    {
        StoreType found = REGISTRY.get(Objects.requireNonNull(name, "store type name may not be null"));
        if (found == null)
        {
            throw new IllegalArgumentException("No store type '" + name + "'. Registered: " + ORDERED);
        }
        return found;
    }

    /**
     * In registration order: the built-ins first, in declaration order.
     */
    public static StoreType[] values()
    {
        return ORDERED.toArray(new StoreType[0]);
    }

    @JsonValue
    public String name()
    {
        return this.name;
    }

    /**
     * Registers rather than looks up: a configuration file can name a store type before the extension
     * that registers it has loaded, and {@link #valueOf(String)} would reject it.
     */
    @JsonCreator
    public static StoreType fromJson(String name)
    {
        return register(name);
    }

    // Java serialization bypasses the constructor, so re-intern on read or {@code ==} stops holding.
    private Object readResolve()
    {
        return register(this.name);
    }

    public int ordinal()
    {
        return this.ordinal;
    }

    @Override
    public int compareTo(StoreType other)
    {
        return Integer.compare(this.ordinal, other.ordinal);
    }

    @Override
    public String toString()
    {
        return this.name;
    }

    // equals/hashCode are deliberately identity: register() interns.
}
