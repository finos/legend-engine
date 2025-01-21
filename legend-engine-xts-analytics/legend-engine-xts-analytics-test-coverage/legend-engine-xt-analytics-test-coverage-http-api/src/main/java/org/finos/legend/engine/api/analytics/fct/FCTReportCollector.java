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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.FCTReportBuilder;
import org.finos.legend.engine.test.fct.model.FCTTestReport;
import org.finos.legend.pure.code.core.Test_Analytics_Lineage_FCT;
import org.finos.legend.pure.code.core.relational.RelationalFCTReport;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.util.List;

public class FCTReportCollector
{


    public static void main(String[] args)
    {
        PureModel pureModel = new PureModel(PureModelContextData.newPureModelContextData(), null, DeploymentMode.PROD);
        MutableList<? extends Root_meta_pure_extension_Extension> extensions  = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        collectReports(extensions);
    }

    public static List<FCTTestReport> collectReports(MutableList<? extends Root_meta_pure_extension_Extension> extensions)
    {
        ImmutableList<FCTReport> testAnalytics = Lists.immutable.of(new Test_Analytics_Lineage_FCT(), new RelationalFCTReport());
        return FCTReportBuilder.generateReport(testAnalytics, extensions);
    }





}
