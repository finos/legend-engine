// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.ide.api.find;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.multimap.Multimap;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

@Api(tags = "Find")
@Path("/")
public class FindInSources
{
    private static final String STRING_PARAM = "string";
    private static final String REGEX_PARAM = "regex";
    private static final String SOURCE_REGEX_PARAM = "sourceRegex";
    private static final String CASE_SENSITIVE_PARAM = "caseSensitive";
    private static final String MAX_RESULTS_PARAM = "limit";

    private final PureSession session;

    public FindInSources(PureSession session)
    {
        this.session = session;
    }

    @GET
    @Path("findInSources")
    public Response findInSources(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            String string = request.getParameter(STRING_PARAM);
            boolean regex = Boolean.valueOf(String.valueOf(request.getParameter(REGEX_PARAM)));
            String sourceRegex = request.getParameter(SOURCE_REGEX_PARAM);
            String caseSensitiveString = request.getParameter(CASE_SENSITIVE_PARAM);
            int limit = request.getParameter(MAX_RESULTS_PARAM) != null ? Integer.valueOf(request.getParameter(MAX_RESULTS_PARAM)) : Integer.MAX_VALUE;
            boolean caseSensitive = (caseSensitiveString == null) ? true : Boolean.valueOf(caseSensitiveString);

            if (string == null)
            {
                throw new RuntimeException("Must specify search parameters: " + STRING_PARAM);
            }

            RichIterable<SourceCoordinates> results;
            try
            {
                Pattern sourcePattern = getSourcePattern(sourceRegex);
                if (regex)
                {
                    Pattern pattern = Pattern.compile(string, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                    results = session.getPureRuntime().getSourceRegistry().find(pattern, sourcePattern);
                }
                else
                {
                    results = session.getPureRuntime().getSourceRegistry().find(string, caseSensitive, sourcePattern);
                }

                response.setContentType("application/json");
                writeResultsJSON(outputStream, results, limit);
            }
            catch (IOException | RuntimeException | Error e)
            {
                throw e;
            }
        }).build();
    }

    private Pattern getSourcePattern(String sourceRegex)
    {
        return (sourceRegex == null) ? null : Pattern.compile(sourceRegex);
    }

    private int writeResultsJSON(OutputStream stream, RichIterable<SourceCoordinates> results, int limit) throws IOException
    {
        stream.write("[".getBytes());
        int count = 0;
        if (results.notEmpty())
        {
            Multimap<String, SourceCoordinates> indexBySource = results.groupBy(SourceCoordinates.SOURCE_ID);
            boolean first = true;
            for (String sourceId : indexBySource.keysView().toSortedList())
            {
                if (first)
                {
                    first = false;
                    stream.write("{\"sourceId\":\"".getBytes());
                }
                else
                {
                    stream.write(",{\"sourceId\":\"".getBytes());
                }
                stream.write(JSONValue.escape(sourceId).getBytes());
                stream.write("\",\"coordinates\":[".getBytes());
                boolean firstSC = true;
                for (SourceCoordinates sourceCoordinates : indexBySource.get(sourceId))
                {
                    if (firstSC)
                    {
                        firstSC = false;
                    }
                    else
                    {
                        stream.write(",".getBytes());
                    }
                    writeSourceCoordinatesJSON(stream, sourceCoordinates);
                    count++;
                    if (count > limit)
                    {
                        break;
                    }
                }
                stream.write("]}".getBytes());
                if (count > limit)
                {
                    break;
                }
            }
        }
        stream.write("]".getBytes());
        return count > limit ? limit : count;
    }

    private void writeSourceCoordinatesJSON(OutputStream stream, SourceCoordinates sourceCoordinates) throws IOException
    {
        stream.write("{\"startLine\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getStartLine()).getBytes());
        stream.write(",\"startColumn\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getStartColumn()).getBytes());
        stream.write(",\"endLine\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getEndLine()).getBytes());
        stream.write(",\"endColumn\":".getBytes());
        stream.write(Integer.toString(sourceCoordinates.getEndColumn()).getBytes());
        if (sourceCoordinates.getPreview() != null)
        {
            stream.write(",\"preview\":".getBytes());
            writePreviewJSON(stream, sourceCoordinates.getPreview());
        }
        stream.write("}".getBytes());
    }

    private void writePreviewJSON(OutputStream stream, SourceCoordinates.Preview preview) throws IOException
    {
        stream.write("{\"before\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getBeforeText()).getBytes());
        stream.write("\",\"found\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getFoundText()).getBytes());
        stream.write("\",\"after\":\"".getBytes());
        stream.write(JSONValue.escape(preview.getAfterText()).getBytes());
        stream.write("\"}".getBytes());
    }
}
