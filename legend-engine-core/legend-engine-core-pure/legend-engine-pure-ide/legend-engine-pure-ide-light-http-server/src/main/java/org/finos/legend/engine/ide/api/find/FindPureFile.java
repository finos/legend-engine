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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.finos.legend.engine.ide.session.PureSessionManager;

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
public class FindPureFile
{
    private final PureSessionManager sessionManager;

    public FindPureFile(PureSessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
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
                RichIterable<String> fileMatches = isRegex ? sessionManager.getSession().getPureRuntime().getSourceRegistry().findSourceIds(filePattern)
                        : sessionManager.getSession().getPureRuntime().getSourceRegistry().findSourceIds(fileName);
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
