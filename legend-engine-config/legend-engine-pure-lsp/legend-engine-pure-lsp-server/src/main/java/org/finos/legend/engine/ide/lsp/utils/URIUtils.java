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

package org.finos.legend.engine.ide.lsp.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for handling file:// URIs used in the LSP protocol.
 *
 * LSP uses file:// URIs for document identification, while Pure uses
 * source IDs that may be file paths or virtual paths.
 */
public class URIUtils
{
    private static final String FILE_SCHEME = "file";

    private URIUtils()
    {
        // Utility class - no instantiation
    }

    /**
     * Convert a file:// URI string to a Pure source ID.
     * For file URIs, this returns the file path.
     * For other formats, returns the original string.
     */
    public static String uriToSourceId(String uriString)
    {
        if (uriString == null || uriString.isEmpty())
        {
            return uriString;
        }

        try
        {
            URI uri = new URI(uriString);
            if (FILE_SCHEME.equals(uri.getScheme()))
            {
                return Paths.get(uri).toString();
            }
            return uriString;
        }
        catch (URISyntaxException | IllegalArgumentException e)
        {
            // If it's not a valid URI, treat it as a path
            return uriString;
        }
    }

    /**
     * Convert a Pure source ID to a file:// URI string.
     * If the source ID is already a URI, returns it as-is.
     * If the source ID is a file path, converts it to a file:// URI.
     */
    public static String sourceIdToUri(String sourceId)
    {
        if (sourceId == null || sourceId.isEmpty())
        {
            return sourceId;
        }

        // Check if already a URI
        if (sourceId.startsWith("file://") || sourceId.startsWith("http://") || sourceId.startsWith("https://"))
        {
            return sourceId;
        }

        // Convert file path to URI
        try
        {
            Path path = Paths.get(sourceId);
            return path.toUri().toString();
        }
        catch (Exception e)
        {
            // If conversion fails, return as-is with file:// prefix
            return "file://" + sourceId;
        }
    }

    /**
     * Get the file name from a URI string.
     */
    public static String getFileName(String uriString)
    {
        String sourceId = uriToSourceId(uriString);
        int lastSeparator = Math.max(sourceId.lastIndexOf('/'), sourceId.lastIndexOf('\\'));
        return lastSeparator >= 0 ? sourceId.substring(lastSeparator + 1) : sourceId;
    }

    /**
     * Check if a URI represents a Pure file.
     */
    public static boolean isPureFile(String uriString)
    {
        return uriString != null && uriString.toLowerCase().endsWith(".pure");
    }

    /**
     * Normalize a path string for consistent handling.
     */
    public static String normalizePath(String path)
    {
        if (path == null)
        {
            return null;
        }
        // Replace backslashes with forward slashes for consistency
        return path.replace('\\', '/');
    }
}
