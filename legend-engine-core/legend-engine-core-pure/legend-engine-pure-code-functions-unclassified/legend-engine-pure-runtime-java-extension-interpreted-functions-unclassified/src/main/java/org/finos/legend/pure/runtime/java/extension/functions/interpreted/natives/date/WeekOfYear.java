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
        // Weeks are numbered as the JVM's default format locale numbers them, which is what
        // Calendar.WEEK_OF_YEAR answered: the locale says which day a week starts on, and how many
        // days of the year the first week must hold.
        return PureDateToJava.start().toLocalDate(date).get(WeekFields.of(Locale.getDefault(Locale.Category.FORMAT)).weekOfWeekBasedYear());
    }
}
