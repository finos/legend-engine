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

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.function.Supplier;

public interface PrevalServices
{
    PrevalResult preval(Object item, PrevalState state);

    PrevalRuntime runtime();

    PrevalHooks hooks();

    Scope scope();

    GenericTypes genericTypes();

    boolean isInstanceValue(Object value, ImmutableMap<String, ImmutableList<Object>> inScopeVars);

    ImmutableList<String> openVars(Iterable<PrevalResult> results, PrevalState state);

    void trace(PrevalState state, Supplier<String> message);

    PrevalState addToScope(PrevalState state, Object function, ListIterable<? extends GenericType> resolvedTypeParameters, ListIterable<? extends ValueSpecification> parameters, boolean cleanUp);
}
