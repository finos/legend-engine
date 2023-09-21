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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_CronTrigger;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_CronTrigger_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_ManualTrigger_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_Trigger;

public class HelperTriggerBuilder
{

    public static Root_meta_pure_mastery_metamodel_trigger_Trigger buildTrigger(Trigger trigger, CompileContext context)
    {

        if (trigger instanceof ManualTrigger)
        {
            return new Root_meta_pure_mastery_metamodel_trigger_ManualTrigger_Impl("", null, context.pureModel.getClass("meta::pure::mastery::metamodel::trigger::Trigger"));
        }

        if (trigger instanceof CronTrigger)
        {
            return buildCronTrigger((CronTrigger) trigger, context);
        }

        return null;
    }

    private static Root_meta_pure_mastery_metamodel_trigger_CronTrigger buildCronTrigger(CronTrigger cronTrigger, CompileContext context)
    {
        return new Root_meta_pure_mastery_metamodel_trigger_CronTrigger_Impl("")
                ._minute(cronTrigger.minute)
                ._hour(cronTrigger.hour)
                ._dayOfMonth(cronTrigger.dayOfMonth == null ? null : Long.valueOf(cronTrigger.dayOfMonth))
                ._year(cronTrigger.year == null ? null : Long.valueOf(cronTrigger.year))
                ._timezone(cronTrigger.timeZone)
                ._frequency(context.resolveEnumValue("meta::pure::mastery::metamodel::trigger::Frequency", cronTrigger.frequency.name()))
                ._month(cronTrigger.year == null ? null : context.resolveEnumValue("meta::pure::mastery::metamodel::trigger::Month", cronTrigger.month.name()))
                ._days(ListIterate.collect(cronTrigger.days, day -> context.resolveEnumValue("meta::pure::mastery::metamodel::trigger::Day", day.name())));
    }
}
