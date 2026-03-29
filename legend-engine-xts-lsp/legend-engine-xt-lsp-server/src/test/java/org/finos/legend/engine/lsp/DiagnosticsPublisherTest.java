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
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;
import org.junit.Assert;
import org.junit.Test;

public class DiagnosticsPublisherTest
{
    @Test
    public void toRange_convertsOneBasedToZeroBased()
    {
        SourceInformation si = new SourceInformation("test.pure", 3, 5, 3, 10);
        Range range = DiagnosticsPublisher.toRange(si);

        Assert.assertEquals(2, range.getStart().getLine());
        Assert.assertEquals(4, range.getStart().getCharacter());
        Assert.assertEquals(2, range.getEnd().getLine());
        Assert.assertEquals(10, range.getEnd().getCharacter());
    }

    @Test
    public void toRange_handlesLineOne()
    {
        SourceInformation si = new SourceInformation("test.pure", 1, 1, 1, 5);
        Range range = DiagnosticsPublisher.toRange(si);

        Assert.assertEquals(0, range.getStart().getLine());
        Assert.assertEquals(0, range.getStart().getCharacter());
    }

    @Test
    public void fromException_withPlainException_producesErrorAtFileStart()
    {
        Exception e = new RuntimeException("something broke");
        List<Diagnostic> diagnostics = DiagnosticsPublisher.fromException(e);

        Assert.assertEquals(1, diagnostics.size());
        Diagnostic d = diagnostics.get(0);
        Assert.assertEquals(DiagnosticSeverity.Error, d.getSeverity());
        Assert.assertEquals(0, d.getRange().getStart().getLine());
        Assert.assertEquals(0, d.getRange().getStart().getCharacter());
        Assert.assertEquals("legend-pure", d.getSource());
        Assert.assertTrue(d.getMessage().contains("something broke"));
    }

    @Test
    public void fromException_withPureParserException_extractsSourceInfo()
    {
        SourceInformation si = new SourceInformation("test.pure", 5, 3, 5, 8);
        PureParserException ppe = new PureParserException(si, "unexpected token");
        List<Diagnostic> diagnostics = DiagnosticsPublisher.fromException(ppe);

        Assert.assertEquals(1, diagnostics.size());
        Diagnostic d = diagnostics.get(0);
        Assert.assertEquals(4, d.getRange().getStart().getLine());
        Assert.assertEquals(2, d.getRange().getStart().getCharacter());
    }

    @Test
    public void fromException_withWrappedPureException_extractsSourceInfo()
    {
        SourceInformation si = new SourceInformation("test.pure", 10, 1, 10, 20);
        PureParserException inner = new PureParserException(si, "parse error");
        RuntimeException wrapper = new RuntimeException("wrapped", inner);

        List<Diagnostic> diagnostics = DiagnosticsPublisher.fromException(wrapper);
        Assert.assertEquals(1, diagnostics.size());
        Assert.assertEquals(9, diagnostics.get(0).getRange().getStart().getLine());
    }
}
