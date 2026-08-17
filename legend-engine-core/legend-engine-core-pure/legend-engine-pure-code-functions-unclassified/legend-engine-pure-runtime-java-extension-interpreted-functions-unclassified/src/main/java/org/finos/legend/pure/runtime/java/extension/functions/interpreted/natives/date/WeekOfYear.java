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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.date;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDateToJava;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.NativeDateElementFunction;

import java.time.temporal.WeekFields;
import java.util.Locale;

public class WeekOfYear extends NativeDateElementFunction
{
    public WeekOfYear(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    protected int getDateElement(PureDate date) throws InvalidDateElementException
    {
        if (!date.hasDay())
        {
            throw new InvalidDateElementException("Cannot get week of year for " + date);
        }
        // The week a date falls in depends on which day starts the week and how many days January must
        // contribute for the first week to count as week 1. Both come from the format locale, as they
        // did when this was a GregorianCalendar, so the answer stays locale dependent.
        WeekFields weekFields = WeekFields.of(Locale.getDefault(Locale.Category.FORMAT));
        return PureDateToJava.start().toLocalDate(date).get(weekFields.weekOfWeekBasedYear());
    }
}
