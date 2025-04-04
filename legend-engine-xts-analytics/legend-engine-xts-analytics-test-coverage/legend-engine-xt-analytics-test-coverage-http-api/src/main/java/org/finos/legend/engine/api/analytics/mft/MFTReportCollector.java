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

package org.finos.legend.engine.api.analytics.mft;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.test.mft.MFTReport;
import org.finos.legend.engine.test.mft.model.MFTTestReport;
import org.finos.legend.pure.code.core.LineageM2MMFTReport;
import org.finos.legend.pure.code.core.LineageRelationalMFTReport;
import org.finos.legend.pure.code.core.M2MMFTReport;
import org.finos.legend.pure.code.core.relational.dbSpecific.RelationalMFTReportH2;
import java.util.List;

public class MFTReportCollector
{

       public static List<MFTTestReport> collectReports()
    {
        ImmutableList<MFTReport> testAnalytics = Lists.immutable.of(new LineageRelationalMFTReport(), new RelationalMFTReportH2(), new LineageM2MMFTReport(), new M2MMFTReport());
        return MFTReportBuilder.generateReport(testAnalytics);
    }

}
