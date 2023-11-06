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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.trigger;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MasteryConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.trigger.TriggerParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.precedence.PrecedenceRule;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Day;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Frequency;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Month;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public class TriggerParseTreeWalker
{

    private final ParseTreeWalkerSourceInformation walkerSourceInformation;

    public TriggerParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
    }

    public Trigger visitTrigger(TriggerParserGrammar ctx)
    {
        TriggerParserGrammar.DefinitionContext definitionContext =  ctx.definition();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(definitionContext);

        if (definitionContext.cronTrigger() != null)
        {
            return visitCronTrigger(definitionContext.cronTrigger());
        }

        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    private Trigger visitCronTrigger(TriggerParserGrammar.CronTriggerContext ctx)
    {

        CronTrigger cronTrigger = new CronTrigger();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // host
        TriggerParserGrammar.MinuteContext minuteContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.minute(), "minute", sourceInformation);
        cronTrigger.minute = Integer.parseInt(minuteContext.INTEGER().getText());

        // port
        TriggerParserGrammar.HourContext hourContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.hour(), "hour", sourceInformation);
        cronTrigger.hour = Integer.parseInt(hourContext.INTEGER().getText());

        // dayOfMonth
        TriggerParserGrammar.DayOfMonthContext dayOfMonthContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.dayOfMonth(), "dayOfMonth", sourceInformation);
        if (dayOfMonthContext != null)
        {
            cronTrigger.dayOfMonth = Integer.parseInt(dayOfMonthContext.INTEGER().getText());
        }
        // year
        TriggerParserGrammar.YearContext yearContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.year(), "year", sourceInformation);
        if (yearContext != null)
        {
            cronTrigger.year = Integer.parseInt(yearContext.INTEGER().getText());
        }

        // time zone
        TriggerParserGrammar.TimezoneContext timezoneContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.timezone(), "timezone", sourceInformation);
        cronTrigger.timeZone = PureGrammarParserUtility.fromGrammarString(timezoneContext.STRING().getText(), true);


        // frequency
        TriggerParserGrammar.FrequencyContext frequencyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.frequency(), "frequency", sourceInformation);
        if (frequencyContext != null)
        {
            String frequencyString = frequencyContext.frequencyValue().getText();
            cronTrigger.frequency = Frequency.valueOf(frequencyString);
        }

        // days
        TriggerParserGrammar.DaysContext daysContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.days(), "days", sourceInformation);
        if (daysContext != null)
        {
           cronTrigger.days = ListIterate.collect(daysContext.dayValue(), this::visitRunDay);
        }

        // days
        TriggerParserGrammar.MonthContext monthContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.month(), "month", sourceInformation);
        if (monthContext != null)
        {
            String monthString = PureGrammarParserUtility.fromGrammarString(monthContext.monthValue().getText(), true);
            cronTrigger.month = Month.valueOf(monthString);
        }

        return cronTrigger;

    }

    private Day visitRunDay(TriggerParserGrammar.DayValueContext ctx)
    {

        String dayStringValue = ctx.getText();
        return Day.valueOf(dayStringValue);
    }
}
