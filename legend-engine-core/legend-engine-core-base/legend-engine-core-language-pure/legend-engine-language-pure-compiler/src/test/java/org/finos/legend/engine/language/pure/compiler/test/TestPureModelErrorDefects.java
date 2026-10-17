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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.Warning;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.Defect;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.defect.DefectSeverityLevel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Assert;
import org.junit.Test;

public class TestPureModelErrorDefects
{
    private static final String IMPOSSIBLE_CASTS = "function test::f(i: Integer[1]): Boolean[1]\n" +
            "{\n" +
            "  $i->cast(@String);\n" +
            "  $i->cast(@Float);\n" +
            "  $i->cast(@Boolean);\n" +
            "  true;\n" +
            "}\n";

    @Test
    public void testNoDefectsDoesNotThrow()
    {
        newPureModel().checkNoErrorDefect();
    }

    @Test
    public void testWarningDefectsDoNotThrow()
    {
        PureModel pureModel = compile("Enum test::E\n{\n  A, A\n}\n");

        Assert.assertEquals(DefectSeverityLevel.WARN, pureModel.getDefects().getOnly().defectSeverityLevel);
        pureModel.checkNoErrorDefect();
    }

    @Test
    public void testSingleErrorDefectThrowsCompilationError()
    {
        PureModel pureModel = compile("function test::f(i: Integer[1]): String[1]\n{\n  $i->cast(@String)\n}\n");

        EngineException e = Assert.assertThrows(EngineException.class, pureModel::checkNoErrorDefect);
        Assert.assertEquals("Cast will always fail: Integer cannot be cast to String", e.getMessage());
        Assert.assertEquals("[3:7-10]", e.getSourceInformation().getMessage());
        Assert.assertEquals(EngineErrorType.COMPILATION, e.getErrorType());
    }

    @Test
    public void testMultipleErrorDefectsAreReportedTogether()
    {
        PureModel pureModel = compile(IMPOSSIBLE_CASTS);

        EngineException e = Assert.assertThrows(EngineException.class, pureModel::checkNoErrorDefect);
        Assert.assertEquals(
                "Found 3 compilation errors:\n" +
                        "- [3:7-10]: Cast will always fail: Integer cannot be cast to String\n" +
                        "- [4:7-10]: Cast will always fail: Integer cannot be cast to Float\n" +
                        "- [5:7-10]: Cast will always fail: Integer cannot be cast to Boolean",
                e.getMessage());
        Assert.assertEquals("[3:7-10]", e.getSourceInformation().getMessage());
        Assert.assertEquals(EngineErrorType.COMPILATION, e.getErrorType());
    }

    @Test
    public void testErrorDefectsWithoutLocationAreReportedTogether()
    {
        PureModel pureModel = new PureModel(PureGrammarParser.newInstance().parseModel(IMPOSSIBLE_CASTS, false), null, DeploymentMode.TEST);

        EngineException e = Assert.assertThrows(EngineException.class, pureModel::checkNoErrorDefect);
        Assert.assertEquals(
                "Found 3 compilation errors:\n" +
                        "- Cast will always fail: Integer cannot be cast to String\n" +
                        "- Cast will always fail: Integer cannot be cast to Float\n" +
                        "- Cast will always fail: Integer cannot be cast to Boolean",
                e.getMessage());
    }

    @Test
    public void testDefectsAddedToBuiltModelAreIgnored()
    {
        PureModel pureModel = newPureModel();
        pureModel.addDefects(Lists.mutable.with(new Defect(DefectSeverityLevel.ERROR, new SourceInformation("src", 3, 5, 3, 12), "the error", "TestError")));
        pureModel.addWarnings(Lists.mutable.with(new Warning(new SourceInformation("src", 3, 5, 3, 12), "a warning")));
        HelperValueSpecificationBuilder.buildLambda(PureGrammarParser.newInstance().parseLambda("|1->cast(@String)"), pureModel.getContext());

        Assert.assertTrue(pureModel.getDefects().isEmpty());
        pureModel.checkNoErrorDefect();
    }

    @Test
    public void testDefectsWithSameContentAreEqual()
    {
        Defect defect = new Defect(DefectSeverityLevel.ERROR, new SourceInformation("src", 3, 5, 3, 12), "the error", "TestError");
        Defect same = new Defect(DefectSeverityLevel.ERROR, new SourceInformation("src", 3, 5, 3, 12), "the error", "TestError");

        Assert.assertEquals(defect, same);
        Assert.assertEquals(defect.hashCode(), same.hashCode());
        Assert.assertEquals(new Defect(DefectSeverityLevel.ERROR, null, "the error", "TestError"), new Defect(DefectSeverityLevel.ERROR, null, "the error", "TestError"));
        Assert.assertNotEquals(defect, new Defect(DefectSeverityLevel.ERROR, new SourceInformation("src", 3, 5, 3, 13), "the error", "TestError"));
        Assert.assertNotEquals(defect, new Defect(DefectSeverityLevel.WARN, new SourceInformation("src", 3, 5, 3, 12), "the error", "TestError"));
        Assert.assertNotEquals(defect, new Defect(DefectSeverityLevel.ERROR, null, "the error", "TestError"));
    }

    private static PureModel compile(String code)
    {
        return new PureModel(PureGrammarParser.newInstance().parseModel(code), null, DeploymentMode.TEST);
    }

    private static PureModel newPureModel()
    {
        return new PureModel(PureModelContextData.newBuilder().build(), null, DeploymentMode.TEST);
    }
}
