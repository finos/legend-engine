// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.functions.unclassified.base.meta;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.tests.RuntimeTestScriptBuilder;
import org.finos.legend.pure.m3.tests.RuntimeVerifier;
import org.junit.After;
import org.junit.Test;

public abstract class AbstractTestCompileValueSpecification extends AbstractPureTestWithCoreCompiled
{
    private static final String EXECUTE_CODE_BLOCK = "function apps::pure::api::execution::compileAndExecuteCodeBlock(block:String[1]):Any[*]\n" +
            "{\n" +
            "   let prefix = '{|';\n" +
            "   let vs = compileValueSpecification($prefix + $block + '}');\n" +
            "   assert($vs.succeeded(), | 'Invalid PURE code block: ' + if($vs.failure.sourceInformation->isEmpty(), |'', | let col = ($vs.failure.sourceInformation->toOne().column - $prefix->length()); '(line:' + $vs.failure.sourceInformation->toOne().line->toString() + ' column:' + $col->toString() + ') ' + $vs.failure->toOne().message;));\n" +
            "\n" +
            "   $vs.result->toOne()->reactivate(^Map<String, List<Any>>())->cast(@Function<{->Any[*]}>)->toOne()->eval();\n" +
            "}\n";

    @After
    public void cleanRuntime()
    {
        runtime.delete("testSource.pure");
        runtime.delete("exec1.pure");
        runtime.delete("source1.pure");
        runtime.delete("source2.pure");
        runtime.delete("source3.pure");
        runtime.compile();
    }

    @Test
    public void testEvalSingle()
    {
        compileTestSource("testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "let result = compileValueSpecification_String_m__CompilationResult_m_->eval('123');" +
                        "assert($result.succeeded->toOne(), |'');" +
                        "assert(123 == $result.result->toOne()->reactivate(^Map<String, List<Any>>()), |'');" +
                        "}\n");
        this.execute("test():Any[*]");
    }

    @Test
    public void testEvalList()
    {
        compileTestSource("testSource.pure",
                "function test():Any[*]\n" +
                        "{\n" +
                        "let result = compileValueSpecification_String_m__CompilationResult_m_->eval(['123', '456']);" +
                        "assert($result->at(0).succeeded->toOne(), |'');" +
                        "assert($result->at(1).succeeded->toOne(), |'');" +
                        "assert(123 == $result->at(0).result->toOne()->reactivate(^Map<String, List<Any>>()), |'');" +
                        "assert(456 == $result->at(1).result->toOne()->reactivate(^Map<String, List<Any>>()), |'');" +
                        "}\n");
        this.execute("test():Any[*]");
    }

    @Test
    public void testCompileFailureRollback()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(
                        "testSource.pure",
                        "function test():Any[1]\n" +
                                "{\n" +
                                "   let res = compileValueSpecification('{|$a + 3}');\n" +
                                "   assert('The variable \\'a\\' is unknown!' == $res.failure.message, |'');\n" +
                                "}\n"
                ).compile(), new RuntimeTestScriptBuilder().executeFunction("test():Any[1]"),
                runtime, functionExecution, this.getExecutionVerifiers());
    }

    @Test
    public void testCompileFailureMultiLine()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(
                        "testSource.pure",
                        "function test():Any[1]\n" +
                                "{\n" +
                                "   let code = '{| let a = \\'hello world\\';\\n unknownfunc::display($a);\\n}';\n" +
                                "   let res = compileValueSpecification($code);\n" +
                                "   assert('The system can\\'t find a match for the function: unknownfunc::display(_:String[1])' == $res.failure.message, |'');\n" +
                                "   assert(2 == $res.failure.sourceInformation.line, |'');\n" +
                                "   assert(15 == $res.failure.sourceInformation.column, |'');\n" +
                                //We do not want to expose the temporary filename we create
                                "   assert($res.failure.sourceInformation.source->isEmpty(),|'');\n" +
                                "}\n"
                ).compile(), new RuntimeTestScriptBuilder().executeFunction("test():Any[1]"),
                runtime, functionExecution, this.getExecutionVerifiers());
    }

    @Test
    public void testCompileSucceedRollback()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(
                        "testSource.pure",
                        "Class my::Person\n" +
                                "{\n" +
                                "   name:String[1];\n" +
                                "}\n" +
                                "\n" +
                                "function test():Any[1]\n" +
                                "{\n" +
                                "   let res = compileValueSpecification('my::Person.all()->filter(p | $p.name == \\'George\\')');\n" +
                                "   assert($res.failure->isEmpty(), |'');\n" +
                                "   assert(!$res.result->isEmpty(), |'');\n" +
                                "}\n"
                ).compile(), new RuntimeTestScriptBuilder().executeFunction("test():Any[1]"),
                runtime, functionExecution, this.getExecutionVerifiers());

    }

    @Test
    public void testCompileSucceedRollbackMultipleCodeBlocks()
    {
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource(
                        "testSource.pure",
                        "Class my::Person\n" +
                                "{\n" +
                                "   name:String[1];\n" +
                                "}\n" +
                                "\n" +
                                "function test():Any[1]\n" +
                                "{\n" +
                                "   let blocks = ['my::Person.all()->filter(p | $p.name == \\'George\\')','my::Person.all()->filter(p | $p.name == \\'Bob\\')'];\n" +
                                "   let res = $blocks->compileValueSpecification();\n" +
                                "   assert($res->at(0).failure->isEmpty(),|'');\n" +
                                "   assert(!$res->at(0).result->isEmpty(), |'');\n" +
                                "   assert($res->at(1).failure->isEmpty(),|'');\n" +
                                "   assert(!$res->at(1).result->isEmpty(),|'');\n" +
                                "}\n"
                ).compile(), new RuntimeTestScriptBuilder().executeFunction("test():Any[1]"),
                runtime, functionExecution, this.getExecutionVerifiers());

    }

    @Test
    public void testExecuteSimpleBlock()
    {
        this.testExecuteCodeBlockIsStable("[1,2,3]->filter(a|$a == 2)->map(s|$s->toString())->joinStrings('')", "2");
    }

    @Test
    public void testExecuteSimpleBlockDeactivated()
    {
        String codeBlock = "let f = [1,2,3]->map(e | $e * 2)->deactivate()->cast(@FunctionExpression);\n" +
                "$f.func->evaluate($f.parametersValues->map(p | ^List<Any>(values=$p->reactivate(^Map<String, List<Any>>()))))->map(s|$s->toString())->joinStrings(',');";
        this.testExecuteCodeBlockIsStable(codeBlock, "2,4,6");
    }

    @Test
    public void testExecuteCodeBlockWithCompileValueSpecification()
    {
        String codeBlock = "let res = compileValueSpecification('[1,2,3]->map(e | $e * 2)');\n" +
                "let f = $res.result->cast(@FunctionExpression);\n" +
                "$f.func->toOne()->evaluate($f.parametersValues->map(p|^List<Any>(values=$p->reactivate())))->map(s|$s->toString())->joinStrings(',');";
        this.testExecuteCodeBlockIsStable(codeBlock, "2,4,6");
    }

    @Test
    public void testExecuteCodeBlockWithFailingCompileValueSpecification()
    {
        String codeBlock = "let res = compileValueSpecification('$z->map(e | $e * 2)');\n" +
                "let f = $res.failure;\n" +
                "$f.message;";
        this.testExecuteCodeBlockIsStable(codeBlock, "The variable \\\'z\\\' is unknown!");
    }

    @Test
    public void testExecuteLambdaWithPrint()
    {
        this.testExecuteCodeBlockIsStable("print(|1, 1); 'Something';", "Something");
    }

    private void testExecuteCodeBlockIsStable(String codeBlock, String expectedResult)
    {
        String escapedCode = codeBlock.replace("'", "\\\'").replace("\n", "");
        RuntimeVerifier.verifyOperationIsStable(new RuntimeTestScriptBuilder().createInMemorySource("exec1.pure", EXECUTE_CODE_BLOCK)
                        .createInMemorySource("source1.pure",
                                "function doSomething():Any[*]\n{ " + codeBlock + "}")
                        .createInMemorySource("source2.pure", "function testOutsideCompileValSpec():Any[*]\n{ assert('" + expectedResult + "' == doSomething(), |'');}")
                        .createInMemorySource("source3.pure", "function test():Any[*]\n{ assert('" + expectedResult + "' == apps::pure::api::execution::compileAndExecuteCodeBlock('" + escapedCode + "'), |'');}").compile()
                        .executeFunction("testOutsideCompileValSpec():Any[*]"),
                new RuntimeTestScriptBuilder().executeFunction("test():Any[*]"), runtime, functionExecution, this.getAdditionalVerifiers());
    }

    public abstract ListIterable<RuntimeVerifier.FunctionExecutionStateVerifier> getExecutionVerifiers();
}
