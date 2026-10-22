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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.finos.legend.engine.ide.session.PureSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

@Tag(name = "Find")
@Path("/")
public class FindPureFile
{
    private final PureSession session;

    public FindPureFile(PureSession session)
    {
        this.session = session;
    }

    @GET
    @Path("findPureFiles")
    public Response findPureFiles(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            String fileName = request.getParameter("file");
            boolean isRegex = Boolean.parseBoolean(String.valueOf(request.getParameter("regex")));

            try
            {
                Pattern filePattern = Pattern.compile(fileName);
                RichIterable<String> fileMatches = isRegex ? session.getPureRuntime().getSourceRegistry().findSourceIds(filePattern)
                        : session.getPureRuntime().getSourceRegistry().findSourceIds(fileName);
                response.setContentType("application/json");
                final StringBuilder sb = new StringBuilder("[");
                fileMatches.toSortedList().forEach(new Procedure<String>()
                {
                    @Override
                    public void value(String name)
                    {
                        sb.append("\"").append(name).append("\"").append(",");
                    }
                });
                if (!fileMatches.isEmpty())
                {
                    sb.deleteCharAt(sb.length() - 1);
                }
                sb.append("]");
                outputStream.write(sb.toString().getBytes());
            }
            catch (Exception e)
            {
                this.writeErrorResponse(outputStream, fileName);
            }

        }).build();
    }

    private void writeErrorResponse(OutputStream outStream, String file) throws IOException
    {
        outStream.write(("{\"error\":true,\"text\":\"Cannot find source file: " + file + "\"}").getBytes());
    }
}
