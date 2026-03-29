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

package org.finos.legend.engine.lsp;

import java.util.Arrays;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests that use a real PureRuntime.
 * Source IDs for in-memory sources use bare names (no leading slash / repo prefix)
 * because they are not backed by any code repository storage.
 */
public class LegendPureSessionIntegrationTest
{
    private static LegendPureSession session;

    @BeforeClass
    public static void initSession()
    {
        session = new LegendPureSession();
        session.initialize();
        Assert.assertTrue("Session should be initialized", session.isInitialized());
        Assert.assertNotNull("PureRuntime should not be null", session.getPureRuntime());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    // -- Compilation tests --

    @Test
    public void compile_validPureCode_succeeds()
    {
        LegendPureSession.CompileResult result = session.modifyAndCompile(
                "lsp_test_valid.pure",
                "Class test::lsp::Person\n{\n  name: String[1];\n  age: Integer[1];\n}\n"
        );

        Assert.assertTrue("Should be ready", result.isReady());
        if (!result.isSuccess())
        {
            Assert.fail("Valid code should compile but got: " + result.getError());
        }
    }

    @Test
    public void compile_invalidPureCode_returnsCompileError()
    {
        LegendPureSession.CompileResult result = session.modifyAndCompile(
                "lsp_test_invalid.pure",
                "Class test::lsp::Invalid\n{\n  name: UnknownType99[1];\n}\n"
        );

        Assert.assertTrue("Should be ready", result.isReady());
        Assert.assertFalse("Invalid code should fail", result.isSuccess());
        Assert.assertFalse("Compile error is not an internal error", result.isInternalError());
        Assert.assertNotNull("Error expected", result.getError());
    }

    @Test
    public void compile_syntaxError_returnsError()
    {
        LegendPureSession.CompileResult result = session.modifyAndCompile(
                "lsp_test_syntax.pure",
                "Class test::lsp::Bad {{{ not valid"
        );

        Assert.assertTrue("Should be ready", result.isReady());
        Assert.assertFalse("Syntax error should fail", result.isSuccess());
        Assert.assertNotNull("Error expected", result.getError());
    }

    // -- Delta compilation tests --

    @Test
    public void deltaCompile_modifyExistingFile()
    {
        // First compile: define a class
        LegendPureSession.CompileResult r1 = session.modifyAndCompile(
                "lsp_test_delta.pure",
                "Class test::lsp::delta::Animal\n{\n  species: String[1];\n}\n"
        );
        Assert.assertTrue("First compile should succeed", r1.isSuccess());

        // Second compile: modify (add a property) -- this is delta compilation
        LegendPureSession.CompileResult r2 = session.modifyAndCompile(
                "lsp_test_delta.pure",
                "Class test::lsp::delta::Animal\n{\n  species: String[1];\n  age: Integer[1];\n}\n"
        );
        Assert.assertTrue("Delta compile should succeed", r2.isSuccess());
    }

    @Test
    public void deltaCompile_fixError_clearsError()
    {
        // Introduce an error
        LegendPureSession.CompileResult broken = session.modifyAndCompile(
                "lsp_test_fix.pure",
                "Class test::lsp::delta::Broken99\n{\n  x: NoSuchType99[1];\n}\n"
        );
        Assert.assertFalse("Should fail", broken.isSuccess());

        // Fix the error
        LegendPureSession.CompileResult fixed = session.modifyAndCompile(
                "lsp_test_fix.pure",
                "Class test::lsp::delta::Fixed99\n{\n  x: String[1];\n}\n"
        );
        Assert.assertTrue("Should compile after fix", fixed.isSuccess());
    }

    // -- Bulk changes test --

    @Test
    public void applyBulkChanges_multipleFiles_compilesOnce()
    {
        List<LegendPureSession.FileChange> changes = Arrays.asList(
                new LegendPureSession.FileChange(
                        "lsp_test_bulk_a.pure",
                        "Class test::lsp::bulk::ClassA\n{\n  name: String[1];\n}\n",
                        LegendPureSession.FileChangeType.CREATE_OR_MODIFY
                ),
                new LegendPureSession.FileChange(
                        "lsp_test_bulk_b.pure",
                        "Class test::lsp::bulk::ClassB\n{\n  ref: test::lsp::bulk::ClassA[1];\n}\n",
                        LegendPureSession.FileChangeType.CREATE_OR_MODIFY
                )
        );

        LegendPureSession.CompileResult result = session.applyBulkChangesAndCompile(changes);
        if (!result.isSuccess())
        {
            Assert.fail("Bulk compile should succeed but got: " + result.getError());
        }
    }

    // -- Session state tests --

    @Test
    public void reinitialize_restoresCleanState()
    {
        session.modifyAndCompile(
                "lsp_test_reinit.pure",
                "Class test::lsp::reinit::Temp\n{\n}\n"
        );

        session.reinitialize();

        Assert.assertTrue("Should be initialized after reinit", session.isInitialized());
        Assert.assertNotNull("PureRuntime should exist after reinit", session.getPureRuntime());
    }

    // -- Immutable source protection tests --

    @Test
    public void modifyAndCompile_immutableSource_skipsModification()
    {
        // Find a known immutable (platform) source
        org.finos.legend.pure.m3.serialization.runtime.Source platformSource = null;
        for (org.finos.legend.pure.m3.serialization.runtime.Source s : session.getPureRuntime().getSourceRegistry().getSources())
        {
            if (s.isImmutable())
            {
                platformSource = s;
                break;
            }
        }
        Assert.assertNotNull("Should have at least one immutable source", platformSource);

        String originalContent = platformSource.getContent();

        // Attempt to modify the immutable source with garbage — should be silently skipped
        LegendPureSession.CompileResult result = session.modifyAndCompile(
                platformSource.getId(), "THIS IS GARBAGE { NOT VALID PURE");

        // Should succeed (no-op) rather than fail
        Assert.assertTrue("Modifying immutable source should succeed (no-op), got: "
                + (result.getError() != null ? result.getError().getMessage() : ""),
                result.isSuccess());

        // Content should be unchanged
        Assert.assertEquals("Immutable source content should not change",
                originalContent, platformSource.getContent());
    }

    @Test
    public void modifyAndCompile_immutableSource_doesNotPollutOtherFiles()
    {
        // First compile a valid source
        LegendPureSession.CompileResult r1 = session.modifyAndCompile(
                "lsp_test_immune.pure",
                "Class test::lsp::immune::Safe\n{\n  x: String[1];\n}\n"
        );
        Assert.assertTrue("Safe class should compile", r1.isSuccess());

        // Find an immutable source
        org.finos.legend.pure.m3.serialization.runtime.Source immutableSource = null;
        for (org.finos.legend.pure.m3.serialization.runtime.Source s : session.getPureRuntime().getSourceRegistry().getSources())
        {
            if (s.isImmutable())
            {
                immutableSource = s;
                break;
            }
        }
        Assert.assertNotNull("Should have an immutable source", immutableSource);

        // Attempt to modify the immutable source
        session.modifyAndCompile(immutableSource.getId(), "GARBAGE CONTENT");

        // Now compile our valid source again — it should still succeed
        LegendPureSession.CompileResult r2 = session.modifyAndCompile(
                "lsp_test_immune.pure",
                "Class test::lsp::immune::Safe\n{\n  x: String[1];\n  y: Integer[1];\n}\n"
        );
        Assert.assertTrue("Safe class should still compile after immutable source modification attempt, got: "
                + (r2.getError() != null ? r2.getError().getMessage() : ""),
                r2.isSuccess());
    }

    // -- Execute go() tests --

    @Test
    public void executeGo_withGoFunction_returnsOutput()
    {
        // Clean state to avoid pollution from previous tests
        session.reinitialize();

        // Simulate the exact user workflow: create a file with go() and execute it
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "welcome.pure",
                "function go():Any[*]\n{\n  'Hello from go!'\n}\n"
        );
        Assert.assertTrue("go() function should compile, got: " +
                (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());

        LegendPureSession.ExecuteResult result = session.executeGo();
        Assert.assertTrue("executeGo should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertNotNull("Should have output", result.getOutput());
    }

    @Test
    public void executeGo_withoutGoFunction_returnsError()
    {
        session.reinitialize();

        LegendPureSession.ExecuteResult result = session.executeGo();
        Assert.assertFalse("Should fail without go() function", result.isSuccess());
        Assert.assertTrue("Error should mention go(), got: " + result.getError(),
                result.getError().contains("go()"));
    }

    @Test
    public void executeGo_withPrintln_capturesConsoleOutput()
    {
        session.reinitialize();
        LegendPureSession.CompileResult r = session.modifyAndCompile(
                "welcome.pure",
                "function go():Any[*]\n{\n  print('test output', 1)\n}\n"
        );
        Assert.assertTrue("go() with print should compile, got: " +
                (r.getError() != null ? r.getError().getMessage() : ""), r.isSuccess());

        LegendPureSession.ExecuteResult result = session.executeGo();
        Assert.assertTrue("executeGo should succeed, got: " + result.getError(), result.isSuccess());
        Assert.assertNotNull("Should have output", result.getOutput());
        Assert.assertTrue("Should contain printed text, got: " + result.getOutput(),
                result.getOutput().contains("test output"));
    }
}
