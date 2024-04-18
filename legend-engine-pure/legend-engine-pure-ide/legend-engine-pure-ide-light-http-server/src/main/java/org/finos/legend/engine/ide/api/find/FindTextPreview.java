// Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.serialization.runtime.SourceCoordinates;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Api(tags = "Find")
@Path("/")
public class FindTextPreview
{
    private final PureSession session;

    public FindTextPreview(PureSession session)
    {
        this.session = session;
    }

    @POST
    @Path("getTextSearchPreview")
    public Response getTextPreview(@Context HttpServletRequest request, List<SourceCoordinates> coordinates, @Context HttpServletResponse response) throws IOException
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            try
            {
                response.setContentType("application/json");
                writeResultsJSON(outputStream, session.getPureRuntime().getSourceRegistry().getPreviewTextWithCoordinates(coordinates));
            }
            catch (IOException | RuntimeException | Error e)
            {
                throw e;
            }
        }).build();
    }

    private void writeResultsJSON(OutputStream stream, RichIterable<SourceCoordinates> results) throws IOException
    {
        stream.write("[".getBytes());
        if (results.notEmpty())
        {
            boolean firstSC = true;
            for (SourceCoordinates sourceCoordinates : results)
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
            }
        }
        stream.write("]".getBytes());
    }

    private void writeSourceCoordinatesJSON(OutputStream stream, SourceCoordinates sourceCoordinates) throws IOException
    {
        stream.write("{\"sourceId\":\"".getBytes());
        stream.write(sourceCoordinates.getSourceId().getBytes());
        stream.write("\",\"startLine\":".getBytes());
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
