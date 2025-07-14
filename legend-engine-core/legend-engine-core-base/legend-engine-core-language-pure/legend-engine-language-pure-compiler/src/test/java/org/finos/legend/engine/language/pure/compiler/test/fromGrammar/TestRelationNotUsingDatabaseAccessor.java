// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.junit.Assert;
import org.junit.Test;

public class TestRelationNotUsingDatabaseAccessor extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    // More advanced tests can be found in the Relational Section
    // org.finos.legend.engine.language.pure.compiler.test.TestRelationFunctions

    @Test
    public void testProject()
    {
        Pair<PureModelContextData, PureModel> data = test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name])\n" +
                        "}"
        );
        
        ConcreteFunctionDefinition<?> func = data.getTwo().getConcreteFunctionDefinition_safe("test::f__Any_MANY_");
        String functionName = ((SimpleFunctionExpression) func._expressionSequence().getOnly())._func()._name();
        Assert.assertEquals("project_C_MANY__FuncColSpecArray_1__Relation_1_", functionName);
    }
    
    @Test
    public void testProjectOnRelation()
    {
        Pair<PureModelContextData, PureModel> data = test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   1->cast(@Relation<(name:String)>)->project(~[mycol:x|$x.name])\n" +
                        "}"
        );

        ConcreteFunctionDefinition<?> func = data.getTwo().getConcreteFunctionDefinition_safe("test::f__Any_MANY_");
        String functionName = ((SimpleFunctionExpression) func._expressionSequence().getOnly())._func()._name();
        Assert.assertEquals("project_Relation_1__FuncColSpecArray_1__Relation_1_", functionName);
    }

    @Test
    public void testProjectError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.nme])\n" +
                        "}", "COMPILATION error at [4:45-47]: Can't find property 'nme' in class 'test::Person'"
        );
    }

    @Test
    public void testProjectMulti()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])\n" +
                        "}"
        );
    }

    @Test
    public void testProjectMultiError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.vals])\n" +
                        "}", "COMPILATION error at [4:59-62]: Can't find property 'vals' in class 'test::Person'"
        );
    }

    @Test
    public void testProjectReturn()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])->filter(x|$x.co > 2)\n" +
                        "}"
        );
    }

    @Test
    public void testProjectReturnError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])->filter(x|$x.ceo > 2)\n" +
                        "}", "COMPILATION error at [4:78-80]: The column 'ceo' can't be found in the relation (mycol:String[1], co:Integer[1])"
        );
    }

    @Test
    public void testTypeVariableInRelationError()
    {
        test(
                "###Pure\n" +
                        "function test::f(x:meta::pure::metamodel::relation::Relation<(x:Integer(2))>[1]):Any[*]\n" +
                        "{\n" +
                        "   []" +
                        "}", "COMPILATION error at [2:65-74]: Wrong type variables count (1) for type: Integer"
        );
    }

    @Test
    public void testTypeVariableWithPrecisePrimitiveInRelation()
    {
        test(
                "###Pure\n" +
                        "function test::f(x:meta::pure::metamodel::relation::Relation<(x:meta::pure::precisePrimitives::Varchar(2))>[1]):Any[*]\n" +
                        "{\n" +
                        "   []" +
                        "}"
        );
    }

    @Test
    public void testMerge()
    {
        PureModel pureModel = test(
                "###Pure\n" +
                        "function test::func1():meta::pure::metamodel::relation::Relation<(num:Number[1], other:Varchar(222))>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}\n" +
                        "function test::func2():meta::pure::metamodel::relation::Relation<(num:Number[1], other:Varchar(222))>[0..1]\n" +
                        "{\n" +
                        "   [];\n" +
                        "}"
        ).getTwo();
        FunctionType fType1 = (FunctionType) Function.computeFunctionType(pureModel.getPackageableElement("test::func1__Relation_$0_1$_"), pureModel.getExecutionSupport().getProcessorSupport());
        FunctionType fType2 = (FunctionType) Function.computeFunctionType(pureModel.getPackageableElement("test::func2__Relation_$0_1$_"), pureModel.getExecutionSupport().getProcessorSupport());
        Assert.assertEquals("(num:Number[1], other:Varchar(222))", GenericType.print(_RelationType.merge(fType1._returnType()._typeArguments().getFirst(), fType2._returnType()._typeArguments().getFirst(), true, pureModel.getExecutionSupport().getProcessorSupport()), pureModel.getExecutionSupport().getProcessorSupport()));
    }

    @Test
    public void testExtendOverWithWrongSortCompilerFeedback()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name])->extend(over(~mycol,[~mycol]->descending()), ~newCol:x|$x.mycol:y|$y->count())\n" +
                        "}", "COMPILATION error at [4:82-91]: Can't infer the type of the function parameter within over"
        );
    }

    @Test
    public void testExtendOverWithWrongSortFuncCompilerFeedback()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name])->extend(over(~mycol, ~mycol->desceeending()), ~newCol:x|$x.mycol:y|$y->count())\n" +
                        "}", "COMPILATION error at [4:81-92]: Can't resolve the builder for function 'desceeending' - stack:[Function 'test::f__Any_MANY_' Third Pass, Applying extend, Applying over, Applying desceeending]"
        );
    }

    @Test
    public void testExtendOverCompilerFeedback()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name])->extend(over('www'), ~newCol:x|$x.mycol:y|$y->count())\n" +
                        "}",
                "COMPILATION error at [4:60-63]: Can't find a match for function 'over(String[1])'"
        );
    }
}
