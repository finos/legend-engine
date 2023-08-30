// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Day;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperTriggerComposer
{

    public static String renderTrigger(Trigger trigger, int indentLevel, PureGrammarComposerContext context)
    {

        if (trigger instanceof ManualTrigger)
        {
            return "Manual";
        }

        if (trigger instanceof CronTrigger)
        {
            return renderCronTrigger((CronTrigger) trigger, indentLevel, context);
        }

        return null;
    }

    private static String renderCronTrigger(CronTrigger cronTrigger, int indentLevel, PureGrammarComposerContext context)
    {
        return "Cron #{\n"
                + getTabString(indentLevel + 1) + "minute: " + cronTrigger.minute + ";\n"
                + getTabString(indentLevel + 1) + "hour: " + cronTrigger.hour + ";\n"
                + getTabString(indentLevel + 1) + "timezone: " + convertString(cronTrigger.timeZone, true) + ";\n"
                + (cronTrigger.year == null ? "" : (getTabString(indentLevel + 1) + "year: " + cronTrigger.year + ";\n"))
                + (cronTrigger.frequency == null ? "" : (getTabString(indentLevel + 1) + "frequency: " + cronTrigger.frequency.name() + ";\n"))
                + (cronTrigger.month == null ? "" : (getTabString(indentLevel + 1) + "month: " + cronTrigger.month.name() + ";\n"))
                + (cronTrigger.dayOfMonth == null ? "" : getTabString(indentLevel + 1) + "dayOfMonth: " + cronTrigger.dayOfMonth + ";\n")
                + renderDays(cronTrigger.days, indentLevel + 1)
                + getTabString(indentLevel) + "}#";
    }

    private static String renderDays(List<Day> days, int indentLevel)
    {
        if (days == null || days.isEmpty())
        {
            return "";
        }

        return getTabString(indentLevel) + "days: [ "
                + String.join(", ", ListIterate.collect(days, Enum::name)) +
               " ];\n";
    }
}
