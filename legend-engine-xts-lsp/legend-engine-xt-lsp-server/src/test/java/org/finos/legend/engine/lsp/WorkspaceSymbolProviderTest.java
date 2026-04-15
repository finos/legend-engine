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
import java.util.stream.Collectors;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests workspace symbol search using a real PureRuntime.
 * Compiles user-defined Pure sources and verifies they appear in search results.
 */
public class WorkspaceSymbolProviderTest
{
    private static LegendPureSession session;
    private static UriMapper uriMapper;
    private static WorkspaceSymbolProvider provider;

    @BeforeClass
    public static void init()
    {
        session = new LegendPureSession();
        session.initialize();
        uriMapper = new UriMapper();

        // Register URI mappings for test sources
        uriMapper.register("file:///test/ws_sym_class.pure", "ws_sym_class.pure");
        uriMapper.register("file:///test/ws_sym_enum.pure", "ws_sym_enum.pure");
        uriMapper.register("file:///test/ws_sym_func.pure", "ws_sym_func.pure");

        // Create test sources
        LegendPureSession.CompileResult r1 = session.modifyAndCompile(
                "ws_sym_class.pure",
                "Class test::ws::sym::Customer\n{\n  name: String[1];\n  age: Integer[1];\n}\n"
        );
        Assert.assertTrue("Customer class should compile: " +
                (r1.getError() != null ? r1.getError().getMessage() : ""), r1.isSuccess());

        LegendPureSession.CompileResult r2 = session.modifyAndCompile(
                "ws_sym_enum.pure",
                "Enum test::ws::sym::Color\n{\n  Red,\n  Green,\n  Blue\n}\n"
        );
        Assert.assertTrue("Color enum should compile: " +
                (r2.getError() != null ? r2.getError().getMessage() : ""), r2.isSuccess());

        LegendPureSession.CompileResult r3 = session.modifyAndCompile(
                "ws_sym_func.pure",
                "function test::ws::sym::greet(name: String[1]): String[1]\n{\n  'Hello ' + $name\n}\n"
        );
        Assert.assertTrue("greet function should compile: " +
                (r3.getError() != null ? r3.getError().getMessage() : ""), r3.isSuccess());

        // Build the symbol index after all sources are compiled
        provider = new WorkspaceSymbolProvider();
        provider.buildIndex(session.getPureRuntime());
    }

    @AfterClass
    public static void cleanup()
    {
        session = null;
    }

    @Test
    public void search_byClassName_findsClass()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "Customer", 100);

        List<SymbolInformation> matches = results.stream()
                .filter(s -> s.getName().equals("Customer"))
                .collect(Collectors.toList());

        Assert.assertEquals("Should find exactly one Customer", 1, matches.size());
        Assert.assertEquals(SymbolKind.Class, matches.get(0).getKind());
        Assert.assertEquals("test::ws::sym", matches.get(0).getContainerName());
    }

    @Test
    public void search_byEnumName_findsEnum()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "Color", 100);

        List<SymbolInformation> matches = results.stream()
                .filter(s -> s.getName().equals("Color"))
                .collect(Collectors.toList());

        Assert.assertEquals("Should find exactly one Color", 1, matches.size());
        Assert.assertEquals(SymbolKind.Enum, matches.get(0).getKind());
    }

    @Test
    public void search_byFunctionName_findsFunction()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "greet", 100);

        List<SymbolInformation> matches = results.stream()
                .filter(s -> s.getName().contains("greet"))
                .collect(Collectors.toList());

        Assert.assertFalse("Should find greet function", matches.isEmpty());
        Assert.assertEquals(SymbolKind.Function, matches.get(0).getKind());
    }

    @Test
    public void search_caseInsensitive()
    {
        List<SymbolInformation> lower = provider.search(uriMapper, "customer", 100);
        List<SymbolInformation> upper = provider.search(uriMapper, "CUSTOMER", 100);

        List<SymbolInformation> lowerMatches = lower.stream()
                .filter(s -> s.getName().equals("Customer"))
                .collect(Collectors.toList());
        List<SymbolInformation> upperMatches = upper.stream()
                .filter(s -> s.getName().equals("Customer"))
                .collect(Collectors.toList());

        Assert.assertEquals("Case-insensitive search should find Customer", 1, lowerMatches.size());
        Assert.assertEquals("Case-insensitive search should find Customer", 1, upperMatches.size());
    }

    @Test
    public void search_byPackagePath_findsAllInPackage()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "test::ws::sym", 500);

        // Should find at least our 3 test elements
        List<SymbolInformation> matches = results.stream()
                .filter(s -> s.getContainerName().startsWith("test::ws::sym"))
                .collect(Collectors.toList());

        Assert.assertTrue("Should find at least 3 symbols in test::ws::sym, found " + matches.size(),
                matches.size() >= 3);
    }

    @Test
    public void search_emptyQuery_returnsRegisteredSymbols()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "", 10000);

        // Only symbols with valid URI mappings are returned;
        // platform symbols are excluded since they have no URI mapping
        Assert.assertTrue("Empty query should return at least our test symbols, found " + results.size(),
                results.size() >= 3);
    }

    @Test
    public void search_respectsMaxResults()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "", 5);

        Assert.assertTrue("Should respect maxResults limit",
                results.size() <= 5);
    }

    @Test
    public void search_noMatch_returnsEmpty()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "ZzXxYyNoSuchElement999", 100);

        Assert.assertTrue("Non-matching query should return empty", results.isEmpty());
    }

    @Test
    public void symbols_haveValidSourceLocations()
    {
        List<SymbolInformation> results = provider.search(uriMapper, "Customer", 100);

        List<SymbolInformation> matches = results.stream()
                .filter(s -> s.getName().equals("Customer"))
                .collect(Collectors.toList());

        Assert.assertEquals(1, matches.size());
        SymbolInformation sym = matches.get(0);
        Assert.assertNotNull("Should have location", sym.getLocation());
        Assert.assertNotNull("Should have range", sym.getLocation().getRange());
        // Start line should be 0 (0-based), since the class is on line 1
        Assert.assertEquals("Start line should be 0 (0-based)", 0,
                sym.getLocation().getRange().getStart().getLine());
    }

    @Test
    public void indexSize_containsManyElements()
    {
        // The index should contain all elements from the compiled model
        // including our 3 test elements + platform elements
        Assert.assertTrue("Index should contain many elements, found " + provider.size(),
                provider.size() > 100);
    }

    @Test
    public void toSymbolKind_mapsCorrectly()
    {
        Assert.assertEquals(SymbolKind.Class, WorkspaceSymbolProvider.toSymbolKind("Class"));
        Assert.assertEquals(SymbolKind.Enum, WorkspaceSymbolProvider.toSymbolKind("Enumeration"));
        Assert.assertEquals(SymbolKind.Function, WorkspaceSymbolProvider.toSymbolKind("ConcreteFunctionDefinition"));
        Assert.assertEquals(SymbolKind.Function, WorkspaceSymbolProvider.toSymbolKind("NativeFunction"));
        Assert.assertEquals(SymbolKind.Interface, WorkspaceSymbolProvider.toSymbolKind("Profile"));
        Assert.assertEquals(SymbolKind.Struct, WorkspaceSymbolProvider.toSymbolKind("Association"));
        Assert.assertEquals(SymbolKind.Object, WorkspaceSymbolProvider.toSymbolKind("SomethingElse"));
        Assert.assertEquals(SymbolKind.Object, WorkspaceSymbolProvider.toSymbolKind(null));
    }
}
