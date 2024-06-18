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

import java.util.Calendar;

import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.natives.essentials.date.extract.NativeDateElementFunction;

public class DayOfWeekNumber extends NativeDateElementFunction
{
    public DayOfWeekNumber(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    protected int getDateElement(PureDate date) throws InvalidDateElementException
    {
        if (!date.hasDay())
        {
            throw new InvalidDateElementException("Cannot get day of week for " + date);
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
                throw new RuntimeException("Error getting day of week for " + date);
            }
        }
    }
}
