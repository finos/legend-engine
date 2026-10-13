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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.Defect;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.DefectSeverityLevel;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestPureModelErrorDefects
{
    private static final SourceInformation FIRST = new SourceInformation("first", 3, 5, 3, 12);
    private static final SourceInformation SECOND = new SourceInformation("second", 7, 1, 8, 4);

    @Test
    public void testNoDefectsDoesNotThrow()
    {
        newPureModel().checkNoErrorDefect();
    }

    @Test
    public void testWarningAndInfoDefectsDoNotThrow()
    {
        PureModel pureModel = newPureModel();
        pureModel.addDefects(Lists.mutable.with(
                new Warning(FIRST, "a warning"),
                new Defect(DefectSeverityLevel.INFO, SECOND, "some info", "TestInfo")));
        pureModel.checkNoErrorDefect();
    }

    @Test
    public void testSingleErrorDefectThrowsCompilationError()
    {
        PureModel pureModel = newPureModel();
        pureModel.addDefects(Lists.mutable.with(
                new Warning(SECOND, "a warning"),
                new Defect(DefectSeverityLevel.ERROR, FIRST, "the error", "TestError")));

        EngineException e = Assert.assertThrows(EngineException.class, pureModel::checkNoErrorDefect);
        Assert.assertEquals("the error", e.getMessage());
        Assert.assertSame(FIRST, e.getSourceInformation());
        Assert.assertEquals(EngineErrorType.COMPILATION, e.getErrorType());
    }

    @Test
    public void testMultipleErrorDefectsAreReportedTogether()
    {
        PureModel pureModel = newPureModel();
        pureModel.addDefects(Lists.mutable.with(
                new Defect(DefectSeverityLevel.ERROR, FIRST, "first error", "TestError"),
                new Warning(FIRST, "a warning"),
                new Defect(DefectSeverityLevel.ERROR, SECOND, "second error", "TestError"),
                new Defect(DefectSeverityLevel.ERROR, null, "error without location", "TestError")));

        EngineException e = Assert.assertThrows(EngineException.class, pureModel::checkNoErrorDefect);
        Assert.assertEquals(
                "Found 3 compilation errors:\n" +
                        "- [3:5-12]: first error\n" +
                        "- [7:1-8:4]: second error\n" +
                        "- error without location",
                e.getMessage());
        Assert.assertSame(FIRST, e.getSourceInformation());
        Assert.assertEquals(EngineErrorType.COMPILATION, e.getErrorType());
    }

    private static PureModel newPureModel()
    {
        return new PureModel(PureModelContextData.newBuilder().build(), null, DeploymentMode.TEST);
    }
}
