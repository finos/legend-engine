// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.ide.lsp.converters;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.finos.legend.engine.ide.lsp.utils.PositionUtils;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.exception.PureUnresolvedIdentifierException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

/**
 * Converter for Pure exceptions to LSP Diagnostic objects.
 */
public class DiagnosticConverter
{
    private static final String SOURCE = "Pure";

    private DiagnosticConverter()
    {
        // Utility class - no instantiation
    }

    /**
     * Convert a PureException to an LSP Diagnostic.
     */
    public static Diagnostic toDiagnostic(PureException exception)
    {
        PureException original = exception.getOriginatingPureException();
        SourceInformation sourceInfo = original.getSourceInformation();

        Range range = getRange(sourceInfo);
        String message = buildMessage(exception);
        DiagnosticSeverity severity = getSeverity(exception);
        String code = getCode(exception);

        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(range);
        diagnostic.setMessage(message);
        diagnostic.setSeverity(severity);
        diagnostic.setSource(SOURCE);
        if (code != null)
        {
            diagnostic.setCode(code);
        }

        return diagnostic;
    }

    /**
     * Convert a generic Throwable to an LSP Diagnostic.
     */
    public static Diagnostic toDiagnostic(Throwable throwable)
    {
        // Try to find a PureException in the cause chain
        PureException pureException = PureException.findPureException(throwable);
        if (pureException != null)
        {
            return toDiagnostic(pureException);
        }

        // Handle generic exception
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(new Range(new Position(0, 0), new Position(0, 1)));
        diagnostic.setMessage(throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getName());
        diagnostic.setSeverity(DiagnosticSeverity.Error);
        diagnostic.setSource(SOURCE);
        return diagnostic;
    }

    /**
     * Convert a PureException to a list of diagnostics (one per source file).
     * This handles cases where an exception may reference multiple files.
     */
    public static MutableList<Diagnostic> toDiagnostics(PureException exception)
    {
        MutableList<Diagnostic> diagnostics = Lists.mutable.empty();
        diagnostics.add(toDiagnostic(exception));
        return diagnostics;
    }

    private static Range getRange(SourceInformation sourceInfo)
    {
        if (sourceInfo == null)
        {
            return new Range(new Position(0, 0), new Position(0, 1));
        }
        return PositionUtils.sourceInfoToRange(sourceInfo);
    }

    private static String buildMessage(PureException exception)
    {
        StringBuilder message = new StringBuilder();
        PureException original = exception.getOriginatingPureException();

        message.append(original.getInfo());

        // Add additional context for specific exception types
        if (exception instanceof PureUnresolvedIdentifierException)
        {
            PureUnresolvedIdentifierException unresolvedEx = (PureUnresolvedIdentifierException) exception;
            message.append("\nUnresolved identifier: ").append(unresolvedEx.getIdOrPath());
        }
        else if (exception instanceof PureUnmatchedFunctionException)
        {
            PureUnmatchedFunctionException unmatchedEx = (PureUnmatchedFunctionException) exception;
            message.append("\nUnmatched function: ").append(unmatchedEx.getFunctionName());
        }

        return message.toString();
    }

    private static DiagnosticSeverity getSeverity(PureException exception)
    {
        // All Pure exceptions are errors for now
        // Could be extended to support warnings for specific cases
        return DiagnosticSeverity.Error;
    }

    private static String getCode(PureException exception)
    {
        if (exception instanceof PureParserException)
        {
            return "PARSER_ERROR";
        }
        else if (exception instanceof PureCompilationException)
        {
            return "COMPILATION_ERROR";
        }
        else if (exception instanceof PureUnresolvedIdentifierException)
        {
            return "UNRESOLVED_IDENTIFIER";
        }
        else if (exception instanceof PureUnmatchedFunctionException)
        {
            return "UNMATCHED_FUNCTION";
        }
        return null;
    }

    /**
     * Get the source ID from a PureException.
     * Returns null if no source information is available.
     */
    public static String getSourceId(PureException exception)
    {
        PureException original = exception.getOriginatingPureException();
        SourceInformation sourceInfo = original.getSourceInformation();
        return sourceInfo != null ? sourceInfo.getSourceId() : null;
    }

    /**
     * Create an info-level diagnostic for a message.
     */
    public static Diagnostic createInfoDiagnostic(String message, Range range)
    {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(range);
        diagnostic.setMessage(message);
        diagnostic.setSeverity(DiagnosticSeverity.Information);
        diagnostic.setSource(SOURCE);
        return diagnostic;
    }

    /**
     * Create a warning-level diagnostic for a message.
     */
    public static Diagnostic createWarningDiagnostic(String message, Range range)
    {
        Diagnostic diagnostic = new Diagnostic();
        diagnostic.setRange(range);
        diagnostic.setMessage(message);
        diagnostic.setSeverity(DiagnosticSeverity.Warning);
        diagnostic.setSource(SOURCE);
        return diagnostic;
    }
}
