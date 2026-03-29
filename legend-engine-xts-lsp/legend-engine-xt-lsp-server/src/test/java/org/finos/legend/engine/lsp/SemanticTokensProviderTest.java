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

import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SemanticTokensProviderTest
{
    private static LegendPureSession session;

    private static final String CLASS_SOURCE_ID = "sem_test_class.pure";
    private static final String CLASS_CODE =
            "Class test::sem::Person\n" +          // line 1: class definition
            "{\n" +                                 // line 2
            "  name: String[1];\n" +                // line 3: property + type ref
            "  age: Integer[1];\n" +                // line 4: property + type ref
            "}\n";                                  // line 5

    private static final String ENUM_SOURCE_ID = "sem_test_enum.pure";
    private static final String ENUM_CODE =
            "Enum test::sem::Color\n" +             // line 1: enum definition
            "{\n" +                                 // line 2
            "  Red,\n" +                            // line 3: enum member
            "  Green,\n" +                          // line 4: enum member
            "  Blue\n" +                            // line 5: enum member
            "}\n";                                  // line 6

    private static final String FUNC_SOURCE_ID = "sem_test_func.pure";
    private static final String FUNC_CODE =
            "function test::sem::greet(x: String[1]): String[1]\n" +  // line 1
            "{\n" +                                                    // line 2
            "  'Hello ' + $x\n" +                                     // line 3
            "}\n";                                                     // line 4

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();

        LegendPureSession.CompileResult r1 = session.modifyAndCompile(CLASS_SOURCE_ID, CLASS_CODE);
        Assert.assertTrue("Person should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(ENUM_SOURCE_ID, ENUM_CODE);
        Assert.assertTrue("Color should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        LegendPureSession.CompileResult r3 = session.modifyAndCompile(FUNC_SOURCE_ID, FUNC_CODE);
        Assert.assertTrue("greet should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void classSource_producesTokens()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);
        Assert.assertFalse("Class source should produce semantic tokens", data.isEmpty());
        Assert.assertEquals("Token data should be multiples of 5", 0, data.size() % 5);
    }

    @Test
    public void classSource_containsClassDefinitionToken()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);
        // First token should be the class definition
        // Token types: 1 = class
        boolean foundClass = false;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 1) // tokenType == class
            {
                foundClass = true;
                break;
            }
        }
        Assert.assertTrue("Should contain a class token", foundClass);
    }

    @Test
    public void classSource_containsPropertyTokens()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);
        int propertyCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 4) // tokenType == property
            {
                propertyCount++;
            }
        }
        Assert.assertTrue("Should contain property tokens (name, age), found: " + propertyCount,
                propertyCount >= 2);
    }

    @Test
    public void classSource_containsTypeReferenceTokens()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);
        int typeCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 6) // tokenType == type
            {
                typeCount++;
            }
        }
        Assert.assertTrue("Should contain type reference tokens (String, Integer), found: " + typeCount,
                typeCount >= 2);
    }

    @Test
    public void enumSource_containsEnumDefinitionAndMembers()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), ENUM_SOURCE_ID);
        Assert.assertFalse("Enum source should produce tokens", data.isEmpty());

        boolean foundEnum = false;
        int memberCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 2) // tokenType == enum
            {
                foundEnum = true;
            }
            if (data.get(i + 3) == 5) // tokenType == enumMember
            {
                memberCount++;
            }
        }
        Assert.assertTrue("Should contain enum definition token", foundEnum);
        Assert.assertTrue("Should contain enum member tokens (Red, Green, Blue), found: " + memberCount,
                memberCount >= 3);
    }

    @Test
    public void funcSource_containsFunctionDefinition()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), FUNC_SOURCE_ID);
        Assert.assertFalse("Function source should produce tokens", data.isEmpty());

        boolean foundFunction = false;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 3) // tokenType == function
            {
                foundFunction = true;
                break;
            }
        }
        Assert.assertTrue("Should contain function definition token", foundFunction);
    }

    @Test
    public void unknownSource_returnsEmpty()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), "nonexistent.pure");
        Assert.assertTrue("Unknown source should return empty", data.isEmpty());
    }

    @Test
    public void tokenData_hasValidDeltaEncoding()
    {
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);
        // Verify delta encoding is valid: no negative deltas, lengths > 0
        for (int i = 0; i < data.size(); i += 5)
        {
            int deltaLine = data.get(i);
            int deltaCol = data.get(i + 1);
            int length = data.get(i + 2);
            int tokenType = data.get(i + 3);

            Assert.assertTrue("deltaLine should be >= 0, got " + deltaLine + " at token " + (i / 5),
                    deltaLine >= 0);
            Assert.assertTrue("length should be > 0, got " + length + " at token " + (i / 5),
                    length > 0);
            Assert.assertTrue("tokenType should be valid, got " + tokenType + " at token " + (i / 5),
                    tokenType >= 0 && tokenType < SemanticTokensProvider.TOKEN_TYPES.size());
        }
    }
}
