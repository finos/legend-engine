// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.test.deephaven.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestDeephavenConnectionIntegrationLoader;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreDeephavenPCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Deephaven_EssentialFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreDeephavenPCTCodeRepositoryProvider.deephavenAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            pack("meta::pure::functions::collection::tests::add", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::at", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::concatenate", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::contains", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::drop", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::exists", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::find", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::fold", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::forall", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::get", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::head", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::indexof", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::init", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::keys", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::last", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::put", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::putAll", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::removeDuplicates", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::removeDuplicatesBy", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::reverse", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::slice", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::sort", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::tail", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::take", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::collection::tests::values", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),

            pack("meta::pure::functions::date::tests", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),

            pack("meta::pure::functions::lang::tests::if", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::lang::tests::match", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),

            pack("meta::pure::functions::math::tests::abs", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::ceiling", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::exp", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::floor", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::log", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::log10", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::mod", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::pow", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::rem", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::round", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::sign", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::toDecimal", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::toFloat", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::trigonometry", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::math::tests::trigonometry", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::testCubeRoot_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::testSquareRootEval_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::testSquareRoot_Function_1__Boolean_1_", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),

            pack("meta::pure::functions::string::tests::contains", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::endswith", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::format", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::indexOf", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::joinStrings", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::length", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::parseBoolean", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::parseDate", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::parseDecimal", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::parseFloat", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::parseInteger", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::replace", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::reverse", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::split", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::startswith", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::substring", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::toString", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::tolower", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::toupper", "Field ''tables'' is required", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::string::tests::trim", "Field ''tables'' is required", AdapterQualifier.needsInvestigation)
            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestDeephavenConnectionIntegrationLoader.extensions().getFirst())
        );
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public Adapter getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
