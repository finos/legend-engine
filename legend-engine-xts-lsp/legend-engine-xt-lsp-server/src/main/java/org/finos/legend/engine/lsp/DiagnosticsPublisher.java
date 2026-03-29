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

import java.util.Collections;
import java.util.List;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;

/**
 * Converts PureRuntime compilation exceptions into LSP diagnostics.
 * PureRuntime uses 1-based line/column; LSP uses 0-based.
 */
public class DiagnosticsPublisher
{
    public static List<Diagnostic> fromException(Exception e)
    {
        SourceInformation sourceInfo = extractSourceInfo(e);
        Range range = (sourceInfo != null)
                ? toRange(sourceInfo)
                : new Range(new Position(0, 0), new Position(0, 0));

        return Collections.singletonList(new Diagnostic(
                range,
                e.getMessage(),
                DiagnosticSeverity.Error,
                "legend-pure"
        ));
    }

    public static void publish(LanguageClient client, String uri, List<Diagnostic> diagnostics)
    {
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    public static void clear(LanguageClient client, String uri)
    {
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, Collections.emptyList()));
    }

    static Range toRange(SourceInformation si)
    {
        return SourceInfoUtil.toRange(si);
    }

    private static SourceInformation extractSourceInfo(Throwable t)
    {
        PureException pe = PureException.findPureException(t);
        return (pe != null) ? pe.getSourceInformation() : null;
    }
}
