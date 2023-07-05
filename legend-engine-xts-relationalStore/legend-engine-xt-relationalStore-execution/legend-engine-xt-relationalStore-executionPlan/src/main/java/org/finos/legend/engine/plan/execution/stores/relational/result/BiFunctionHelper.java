// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import java.util.function.BiFunction;

@FunctionalInterface
public interface BiFunctionHelper<T1, T2, R, E extends Exception>
{
    R apply(T1 t1, T2 t2) throws E;

    static <T1, T2, R, E extends Exception> BiFunction<T1, T2, R> unchecked(BiFunctionHelper<T1, T2, R, E> f)
    {
        return (t1, t2) ->
        {
            try
            {
                return f.apply(t1, t2);
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException)
                {
                    throw (RuntimeException) e;
                }
                else
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
