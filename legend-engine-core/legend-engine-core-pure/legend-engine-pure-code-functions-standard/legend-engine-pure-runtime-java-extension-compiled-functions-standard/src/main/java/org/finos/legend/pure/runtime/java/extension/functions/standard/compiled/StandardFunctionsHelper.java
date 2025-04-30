// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.functions.standard.compiled;

import org.eclipse.collections.api.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.runtime.java.extension.functions.standard.shared.natives.date.operation.TimeBucketShared;

public class StandardFunctionsHelper
{
    // DATE-TIME --------------------------------------------------------------
    public static DateTime timeBucket(DateTime date, long quantity, Enum unit)
    {
        try
        {
            return TimeBucketShared.time_bucket(date, quantity, unit._name());
        }
        catch (IllegalArgumentException e)
        {
            throw new PureExecutionException(e.getMessage(), e.getCause(), Stacks.mutable.empty());
        }
    }

    // MATH ---------------------------------------------------------------------
    public static double cosh(Number input)
    {
        return cosh(input.doubleValue());
    }

    public static double cosh(double input)
    {
        return Math.cosh(input);
    }

    public static double sinh(Number input)
    {
        return sinh(input.doubleValue());
    }

    public static double sinh(double input)
    {
        return Math.sinh(input);
    }

    public static double tanh(Number input)
    {
        return tanh(input.doubleValue());
    }

    public static double tanh(double input)
    {
        return Math.tanh(input);
    }
}
