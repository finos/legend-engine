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

package org.finos.legend.engine.api.analytics.fct;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.model.FCTTestReport;
import org.finos.legend.pure.code.core.LineageM2MFCTReport;
import org.finos.legend.pure.code.core.LineageRelationalFCTReport;
import org.finos.legend.pure.code.core.M2MFCTReport;
import org.finos.legend.pure.code.core.relational.dbSpecific.RelationalFCTReportH2;
import java.util.List;

public class FCTReportCollector
{
     public static List<FCTTestReport> collectReports()
    {
        ImmutableList<FCTReport> testAnalytics = Lists.immutable.of(new LineageRelationalFCTReport(), new RelationalFCTReportH2(), new LineageM2MFCTReport(), new M2MFCTReport());
        return FCTReportBuilder.generateReport(testAnalytics);
    }

}
