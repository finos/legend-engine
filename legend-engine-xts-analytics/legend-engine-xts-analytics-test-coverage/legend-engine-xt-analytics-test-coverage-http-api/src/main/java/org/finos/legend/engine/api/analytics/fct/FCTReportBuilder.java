/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */


package org.finos.legend.engine.api.analytics.fct;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.model.FCTTestReport;
import org.finos.legend.engine.test.fct.model.FCTTestResult;
import org.finos.legend.engine.test.fct.model.FeatureTest;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static org.finos.legend.pure.generated.core_analytics_test_coverage_modelCoverage_analytics.Root_meta_analytics_testCoverage_featureMatrix_buildStoreReportJSON_ConcreteFunctionDefinition_MANY__String_1__String_1__String_1_;
import static org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled.getClassLoaderExecutionSupport;

public class FCTReportBuilder
{


    public static List<FCTTestReport> generateReport(ImmutableList<FCTReport> reports, MutableList<? extends Root_meta_pure_extension_Extension> extensions)
    {
       ObjectMapper mapper = new ObjectMapper();
       FastList<FCTTestReport> results = FastList.newList();
       reports.forEach(report ->
               {

                   MutableMap<String, String> explodedExpectedFailures = PCTReportConfiguration.explodeExpectedFailures(report.expectedFailures(), getClassLoaderExecutionSupport().getProcessorSupport());

                   RichIterable<? extends ConcreteFunctionDefinition<? extends Object>> testfunctions = (RichIterable<? extends ConcreteFunctionDefinition<? extends Object>>) (RichIterable<?>) Lists.mutable.withAll(report.getTestCollection().stream()
                           .flatMap(c -> c.getAllTestFunctions().stream()).map(c -> (ConcreteFunctionDefinition<? extends Object>) c).collect(Collectors.toList()));
                   String json = Root_meta_analytics_testCoverage_featureMatrix_buildStoreReportJSON_ConcreteFunctionDefinition_MANY__String_1__String_1__String_1_(testfunctions, report.getReportID(), report.getStoreID(), getClassLoaderExecutionSupport());
                   try
                   {
                       FCTTestResult[] fctTestResults = mapper.readValue(json, FCTTestResult[].class);
                       for (FCTTestResult testResult : fctTestResults)
                       {
                           FCTTestReport fctTestReport = new FCTTestReport(testResult);
                           if (testResult.featureTests != null)
                           {
                               for (FeatureTest featureTest : testResult.featureTests)
                               {
                                   fctTestReport.functionName = featureTest.functionName;
                                   String error = explodedExpectedFailures.get(featureTest.functionName);
                                   fctTestReport.success = (error == null  && featureTest.expectedError == null   && Objects.equals(featureTest.assertionType, "assertion"));
                                   fctTestReport.errorMessage = featureTest.expectedError != null ? featureTest.expectedError : error;
                                   if (Objects.equals(featureTest.assertionType, "assertion"))
                                   {
                                       fctTestReport.assertionType = Objects.equals(testResult.testType, "LineageFCT") ? "Lineage" : "Execution";
                                   }
                                   else
                                   {
                                       fctTestReport.assertionType = featureTest.assertionType;
                                   }
                               }
                           }
                           else
                           {
                               fctTestReport.assertionType = "UnTested";
                           }
                           results.add(fctTestReport);

                       }

                   }
                   catch (JsonProcessingException e)
                   {
                       throw new RuntimeException(e);
                   }
               }
       );
        return results;
    }

}

