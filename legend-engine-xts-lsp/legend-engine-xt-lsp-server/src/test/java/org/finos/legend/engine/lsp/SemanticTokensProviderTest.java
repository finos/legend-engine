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

    private static final String BODY_SOURCE_ID = "sem_test_body.pure";
    private static final String BODY_CLS_CODE =
            "Class test::sem::Animal\n" +
            "{\n" +
            "  species: String[1];\n" +
            "}\n";
    private static final String BODY_CODE =
            "function test::sem::process(a: test::sem::Animal[1]): String[1]\n" +  // line 1
            "{\n" +                                                                  // line 2
            "  let nm = $a.species;\n" +                                             // line 3
            "  $nm->toUpper();\n" +                                                  // line 4
            "}\n";                                                                   // line 5

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

        LegendPureSession.CompileResult r4 = session.modifyAndCompile("sem_test_body_cls.pure", BODY_CLS_CODE);
        Assert.assertTrue("Animal should compile: " +
                (r4.getError() != null ? r4.getError().getMessage() : ""), r4.isSuccess());
        LegendPureSession.CompileResult r5 = session.modifyAndCompile(BODY_SOURCE_ID, BODY_CODE);
        Assert.assertTrue("process should compile: " +
                (r5.getError() != null ? r5.getError().getMessage() : ""), r5.isSuccess());
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

    @Test
    public void classDefinitionToken_positionedAtName_notKeyword()
    {
        // "Class test::sem::Person\n" — the "Class" keyword is at col 1,
        // the qualified name "test::sem::Person" starts at col 7.
        // Token must be at the name position, NOT the keyword position.
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), CLASS_SOURCE_ID);

        // Decode delta to absolute positions to find the class definition token
        int absLine = 0;
        int absCol = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            int deltaLine = data.get(i);
            int deltaCol = data.get(i + 1);
            int length = data.get(i + 2);
            int tokenType = data.get(i + 3);
            int modifiers = data.get(i + 4);

            absLine += deltaLine;
            absCol = (deltaLine == 0) ? absCol + deltaCol : deltaCol;

            if (tokenType == 1 && (modifiers & 1) != 0) // class + definition
            {
                // Line 0 (0-based) = line 1 in source
                Assert.assertEquals("Class definition should be on line 1 (0-based: 0)", 0, absLine);
                // Column must be > 0 (0-based) — proving it's NOT at column 1 where "Class" keyword is.
                // "test::sem::Person" starts at column 7 (1-based) = column 6 (0-based).
                Assert.assertTrue("Class definition token should not be at column 0 (keyword position), " +
                        "but was at column " + absCol, absCol > 0);
                // Length covers the simple name "Person" (si.getLine()/getColumn() points to simple name)
                Assert.assertEquals("Class definition token length should cover simple name",
                        "Person".length(), length);
                return;
            }
        }
        Assert.fail("Did not find a class definition token");
    }

    @Test
    public void enumDefinitionToken_positionedAtName_notKeyword()
    {
        // "Enum test::sem::Color\n" — "Enum" keyword at col 1, name at col 6.
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), ENUM_SOURCE_ID);

        int absLine = 0;
        int absCol = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            int deltaLine = data.get(i);
            int deltaCol = data.get(i + 1);
            int length = data.get(i + 2);
            int tokenType = data.get(i + 3);
            int modifiers = data.get(i + 4);

            absLine += deltaLine;
            absCol = (deltaLine == 0) ? absCol + deltaCol : deltaCol;

            if (tokenType == 2 && (modifiers & 1) != 0) // enum + definition
            {
                Assert.assertEquals("Enum definition should be on line 1 (0-based: 0)", 0, absLine);
                Assert.assertTrue("Enum definition token should not be at column 0 (keyword position), " +
                        "but was at column " + absCol, absCol > 0);
                Assert.assertEquals("Enum definition token length should cover simple name",
                        "Color".length(), length);
                return;
            }
        }
        Assert.fail("Did not find an enum definition token");
    }

    @Test
    public void funcDefinitionToken_positionedAtName_notKeyword()
    {
        // "function test::sem::greet(..." — "function" keyword at col 1, name at col 10.
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), FUNC_SOURCE_ID);

        int absLine = 0;
        int absCol = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            int deltaLine = data.get(i);
            int deltaCol = data.get(i + 1);
            int length = data.get(i + 2);
            int tokenType = data.get(i + 3);
            int modifiers = data.get(i + 4);

            absLine += deltaLine;
            absCol = (deltaLine == 0) ? absCol + deltaCol : deltaCol;

            if (tokenType == 3 && (modifiers & 1) != 0) // function + definition
            {
                Assert.assertEquals("Function definition should be on line 1 (0-based: 0)", 0, absLine);
                Assert.assertTrue("Function definition token should not be at column 0 (keyword position), " +
                        "but was at column " + absCol, absCol > 0);
                Assert.assertEquals("Function definition token length should cover simple name",
                        "greet".length(), length);
                return;
            }
        }
        Assert.fail("Did not find a function definition token");
    }

    @Test
    public void funcSource_containsParameterToken()
    {
        // "function test::sem::greet(x: String[1]): String[1]"
        // Should produce a parameter token for "x"
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), FUNC_SOURCE_ID);
        int paramCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 7) // tokenType == parameter
            {
                paramCount++;
            }
        }
        Assert.assertTrue("Should contain parameter token for 'x', found: " + paramCount,
                paramCount >= 1);
    }

    @Test
    public void funcSource_containsReturnTypeToken()
    {
        // "function test::sem::greet(x: String[1]): String[1]"
        // Should produce type tokens for parameter type and/or return type
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), FUNC_SOURCE_ID);
        int typeCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 6) // tokenType == type
            {
                typeCount++;
            }
        }
        // At least 1 type token: the return type String (parameter type may also produce one)
        Assert.assertTrue("Should contain type reference token(s) for String, found: " + typeCount,
                typeCount >= 1);
    }

    // ── Function body expression tree tests ──────────────────────────────

    @Test
    public void functionBody_containsVariableTokens()
    {
        // "let nm = $a.species; $nm->toUpper();"
        // Should produce variable tokens for $a, $nm references
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), BODY_SOURCE_ID);
        int varCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 10) // tokenType == variable
            {
                varCount++;
            }
        }
        // At least 2: $a (line 3) and $nm (line 4), plus let variable "nm"
        Assert.assertTrue("Should contain variable tokens, found: " + varCount,
                varCount >= 2);
    }

    @Test
    public void functionBody_containsPropertyAccessToken()
    {
        // "$a.species" → "species" should be a property token
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), BODY_SOURCE_ID);
        int propCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 4 && (data.get(i + 4) & 1) == 0) // property, NOT definition
            {
                propCount++;
            }
        }
        Assert.assertTrue("Should contain property access token for .species, found: " + propCount,
                propCount >= 1);
    }

    @Test
    public void functionBody_containsFunctionCallToken()
    {
        // "$nm->toUpper()" → "toUpper" should be a function token
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), BODY_SOURCE_ID);
        int fnCount = 0;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 3 && (data.get(i + 4) & 1) == 0) // function, NOT definition
            {
                fnCount++;
            }
        }
        Assert.assertTrue("Should contain function call token for toUpper, found: " + fnCount,
                fnCount >= 1);
    }

    @Test
    public void functionBody_letBindingCreatesVariableDefinition()
    {
        // "let nm = ..." → "nm" should be a variable with definition modifier
        List<Integer> data = SemanticTokensProvider.getTokens(session.getPureRuntime(), BODY_SOURCE_ID);
        boolean foundLetVar = false;
        for (int i = 0; i < data.size(); i += 5)
        {
            if (data.get(i + 3) == 10 && (data.get(i + 4) & 1) != 0) // variable + definition
            {
                foundLetVar = true;
                break;
            }
        }
        Assert.assertTrue("Should contain a variable definition token for let binding", foundLetVar);
    }
}
