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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import org.finos.legend.engine.language.pure.grammar.test.from.TestMappingGrammarFrom;
import org.finos.legend.engine.language.pure.grammar.test.from.TestMixedBooleanArithmeticConstraint;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestConnectionGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestDocumentationParsing;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestDomainGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestMappingGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.parser.TestRuntimeGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestConnectionGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestDataGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestDocumentationRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestDomainGrammarArgumentsRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestDomainGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestDomainTypeVariablesRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestLambdaPrettyRendering;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestLambdaRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestMappingGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestMultiLineStringRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestRuntimeGrammarRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.TestSectionRoundtrip;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.embedded.TestExtendableEmbedded;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.valueSpecification.TestPrimitives;
import org.finos.legend.engine.language.pure.grammar.test.to.TestDomainGrammarTo;
import org.finos.legend.engine.language.pure.grammar.test.to.TestMappingGrammarTo;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The core grammar module's own parser and roundtrip suites, run with the Rust parser in shadow mode (the surefire
 * default here): every section, standalone expression and whole model they parse is also parsed by the Rust parser and
 * compared. A comparison neither matching nor named by the accepted list fails the suite when it ends (see
 * {@link RustGrammarParity#unexpected()}); with {@code -Dlegend.grammar.rust.failOnMismatch=true} a mismatch fails the
 * test that parsed it. Skipped when the native library is not available.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestMappingGrammarFrom.class,
        TestMixedBooleanArithmeticConstraint.class,
        TestConnectionGrammarParser.class,
        TestDataGrammarParser.class,
        TestDocumentationParsing.class,
        TestDomainGrammarParser.class,
        TestMappingGrammarParser.class,
        TestRuntimeGrammarParser.class,
        TestConnectionGrammarRoundtrip.class,
        TestDataGrammarRoundtrip.class,
        TestDocumentationRoundtrip.class,
        TestDomainGrammarArgumentsRoundtrip.class,
        TestDomainGrammarRoundtrip.class,
        TestDomainTypeVariablesRoundtrip.class,
        TestLambdaRoundtrip.class,
        TestLambdaPrettyRendering.class,
        TestMappingGrammarRoundtrip.class,
        TestMultiLineStringRoundtrip.class,
        TestRuntimeGrammarRoundtrip.class,
        TestSectionRoundtrip.class,
        TestExtendableEmbedded.class,
        TestPrimitives.class,
        TestDomainGrammarTo.class,
        TestMappingGrammarTo.class
})
public class TestRustParityCoreGrammar
{
    private static int unexpectedBefore;

    @BeforeClass
    public static void requireNativeLibrary()
    {
        Assume.assumeTrue("Rust parser not available: " + NativeGrammarParser.getLoadFailure(), NativeGrammarParser.isAvailable());
        unexpectedBefore = RustGrammarParity.unexpected().size();
    }

    @AfterClass
    public static void nothingUnexpected()
    {
        List<RustGrammarParity.Unexpected> unexpected = RustGrammarParity.unexpected();
        List<RustGrammarParity.Unexpected> found = unexpected.subList(unexpectedBefore, unexpected.size());
        Assert.assertTrue(found.size() + " unexpected comparison(s), to fix or name in " + RustGrammarParity.ACCEPTED_RESOURCE + ":\n" + found.stream().map(Object::toString).collect(Collectors.joining("\n")), found.isEmpty());
    }
}
