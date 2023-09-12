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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger;

import java.util.ArrayList;
import java.util.List;

public class CronTrigger extends Trigger
{
    public Integer minute;
    public Integer hour;
    public Month month;
    public Integer dayOfMonth;
    public String timeZone;
    public Integer year;
    public Frequency frequency;
    public List<Day> days = new ArrayList<>();

    @Override
    public <T> T accept(TriggerVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
