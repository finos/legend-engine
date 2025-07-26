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

package org.finos.legend.engine.language.pure.grammar.from.milestoning;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.BusinessSnapshotMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingMilestoning;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDate;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CDateTime;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype.primitive.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.ProcessingSnapshotMilestoning;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MilestoningParseTreeWalker
{
    public static Milestoning visitBusinessMilestoning(MilestoningSpecificationSourceCode code, RelationalParserGrammar.BusinessMilestoningContext milestoningCtx)
    {
        if (milestoningCtx.businessMilestoningFromThru() != null)
        {
            BusinessMilestoning milestoning = new BusinessMilestoning();
            RelationalParserGrammar.BusinessMilestoningFromThruContext businessMilestoningCtx = milestoningCtx.businessMilestoningFromThru();

            milestoning.sourceInformation = code.getWalkerSourceInformation().getSourceInformation(businessMilestoningCtx);
            milestoning.from = businessMilestoningCtx.identifier(0).getText();
            milestoning.thru = businessMilestoningCtx.identifier(1).getText();
            milestoning.thruIsInclusive = businessMilestoningCtx.THRU_IS_INCLUSIVE() != null && Boolean.parseBoolean(businessMilestoningCtx.BOOLEAN().getText());
            if (businessMilestoningCtx.INFINITY_DATE() != null)
            {
                milestoning.infinityDate = visitDate(businessMilestoningCtx.DATE().getText(), code.getWalkerSourceInformation().getSourceInformation(businessMilestoningCtx.DATE().getSymbol()));
            }
            return milestoning;
        }
        else if (milestoningCtx.businessSnapshotDate() != null)
        {
            RelationalParserGrammar.BusinessSnapshotDateContext businessMilestoningCtx = milestoningCtx.businessSnapshotDate();
            BusinessSnapshotMilestoning milestoning = new BusinessSnapshotMilestoning();
            milestoning.sourceInformation = code.getWalkerSourceInformation().getSourceInformation(businessMilestoningCtx);
            milestoning.snapshotDate = PureGrammarParserUtility.fromIdentifier(businessMilestoningCtx.identifier());
            return milestoning;
        }
        throw new EngineException("Unsupported syntax", code.getWalkerSourceInformation().getSourceInformation(milestoningCtx), EngineErrorType.PARSER);
    }

    public static Milestoning visitProcessingMilestoning(MilestoningSpecificationSourceCode code, RelationalParserGrammar.ProcessingMilestoningContext milestoningCtx)
    {
        if (milestoningCtx.processingMilestoningInOut() != null)
        {
            ProcessingMilestoning milestoning = new ProcessingMilestoning();
            RelationalParserGrammar.ProcessingMilestoningInOutContext processingMilestoningCtx = milestoningCtx.processingMilestoningInOut();
            milestoning.sourceInformation = code.getWalkerSourceInformation().getSourceInformation(milestoningCtx);
            milestoning.in = processingMilestoningCtx.identifier(0).getText();
            milestoning.out = processingMilestoningCtx.identifier(1).getText();
            milestoning.outIsInclusive = processingMilestoningCtx.OUT_IS_INCLUSIVE() != null && Boolean.parseBoolean(processingMilestoningCtx.BOOLEAN().getText());
            if (processingMilestoningCtx.INFINITY_DATE() != null)
            {
                milestoning.infinityDate = visitDate(processingMilestoningCtx.DATE().getText(), code.getWalkerSourceInformation().getSourceInformation(processingMilestoningCtx.DATE().getSymbol()));
            }
            return milestoning;
        }
        else if (milestoningCtx.processingSnapshotDate() != null)
        {
            ProcessingSnapshotMilestoning milestoning = new ProcessingSnapshotMilestoning();
            RelationalParserGrammar.ProcessingSnapshotDateContext processingMilestoningCtx = milestoningCtx.processingSnapshotDate();
            milestoning.sourceInformation = code.getWalkerSourceInformation().getSourceInformation(processingMilestoningCtx);
            milestoning.snapshotDate = PureGrammarParserUtility.fromIdentifier(processingMilestoningCtx.identifier());
            return milestoning;
        }
        throw new EngineException("Unsupported syntax", code.getWalkerSourceInformation().getSourceInformation(milestoningCtx), EngineErrorType.PARSER);
    }

    private static CDate visitDate(String val, SourceInformation sourceInformation)
    {
        Pattern strictDatePattern = Pattern.compile("%([0-9]{4})-([0-9]{2})-([0-9]{2})");
        Matcher strictDateMatcher = strictDatePattern.matcher(val);

        CDate date = strictDateMatcher.matches() ? visitStrictDate(new CStrictDate(), val) : visitDateTime(new CDateTime(), val);

        date.sourceInformation = sourceInformation;
        date.multiplicity = Multiplicity.PURE_ONE;
        return date;
    }

    private static CDate visitStrictDate(CStrictDate strictDate, String val)
    {
        strictDate.value = val.substring(val.lastIndexOf('%') + 1);
        return strictDate;
    }

    private static CDate visitDateTime(CDateTime dateTime, String val)
    {
        dateTime.value = val.substring(val.lastIndexOf('%') + 1);
        return dateTime;
    }
}
