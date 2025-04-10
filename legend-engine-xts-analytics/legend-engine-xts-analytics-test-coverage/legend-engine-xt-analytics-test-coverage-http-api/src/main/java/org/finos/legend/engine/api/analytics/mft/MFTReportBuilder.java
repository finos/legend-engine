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


package org.finos.legend.engine.api.analytics.mft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.test.mft.MFTReport;
import org.finos.legend.engine.test.mft.model.MFTTestReport;
import org.finos.legend.engine.test.mft.model.MFTTestResult;
import org.finos.legend.engine.test.mft.model.FeatureTest;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.navigation._package._Package;
import java.util.List;
import java.util.stream.Collectors;
import static org.finos.legend.pure.generated.core_analytics_test_coverage_modelCoverage_analytics.Root_meta_analytics_testCoverage_featureMatrix_buildStoreReportJSON_ConcreteFunctionDefinition_MANY__String_1__String_1__ConcreteFunctionDefinition_1__String_1_;
import static org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled.getClassLoaderExecutionSupport;

public class MFTReportBuilder
{


    public static List<MFTTestReport> generateReport(ImmutableList<MFTReport> reports)
    {
       ObjectMapper mapper = new ObjectMapper();
       FastList<MFTTestReport> results = FastList.newList();
       reports.forEach(report ->
               {

                  report.getTestCollection().forEach(collection ->

                   {
                       RichIterable<? extends ConcreteFunctionDefinition<? extends Object>> testfunctions = Lists.mutable.withAll(report.getTestCollection().stream()
                               .flatMap(c -> c.getAllTestFunctions().stream()).map(c -> (ConcreteFunctionDefinition<? extends Object>) c).collect(Collectors.toList()));


                       ConcreteFunctionDefinition<? extends Object> evalFN = (ConcreteFunctionDefinition<? extends Object>) _Package.getByUserPath(report.getEvaluatorFunction(), (getClassLoaderExecutionSupport().getProcessorSupport()));

                       String json = Root_meta_analytics_testCoverage_featureMatrix_buildStoreReportJSON_ConcreteFunctionDefinition_MANY__String_1__String_1__ConcreteFunctionDefinition_1__String_1_(testfunctions, report.getReportID(), collection.getStoreType(), evalFN, getClassLoaderExecutionSupport());
                       try
                       {
                           MFTTestResult[] mftTestResults = mapper.readValue(json, MFTTestResult[].class);
                           for (MFTTestResult testResult : mftTestResults)
                           {
                               MFTTestReport mftTestReport = new MFTTestReport(testResult);
                               if (testResult.featureTests != null)
                               {
                                   for (FeatureTest featureTest : testResult.featureTests)
                                   {
                                       mftTestReport.functionName = featureTest.functionName;
                                       //Status is one of Pass,Not Tested,Store Not Supported, Query Not Supported

                                       if (!testResult.supportedFeature)
                                       {
                                           mftTestReport.status = "Mapping Feature Not Supported";
                                       }
                                       else
                                       {
                                           switch (featureTest.assertionType)
                                           {
                                               case "Assertion":
                                                   mftTestReport.status = "Success";
                                                   break;
                                               case "Untested":
                                                   mftTestReport.status = "Untested";
                                                   break;
                                               case "AssertError":
                                                   mftTestReport.status = "Query Feature Not Supported";
                                                   break;
                                               default:
                                                   mftTestReport.status = "Unknown";
                                                   break;
                                           }
                                       }
                                       mftTestReport.errorMessage = featureTest.message;
                                       mftTestReport.assertionType = featureTest.assertionType;

                                   }
                               }
                               else
                               {
                                   mftTestReport.assertionType = "Untested";
                                   mftTestReport.status = "Untested";

                               }

                               if (!testResult.supportedFeature)
                               {
                                   mftTestReport.status = "Mapping Feature Not Supported";
                               }

                               results.add(mftTestReport);

                           }

                       }

                       catch (JsonProcessingException e)
                       {
                           throw new RuntimeException(e);
                       }

                   }
                  );

               }

               );

        return results;
    }

}

