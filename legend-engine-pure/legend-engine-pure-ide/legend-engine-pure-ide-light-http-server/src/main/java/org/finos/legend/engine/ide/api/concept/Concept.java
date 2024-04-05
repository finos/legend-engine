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

package org.finos.legend.engine.ide.api.concept;

import io.swagger.annotations.Api;
import org.finos.legend.engine.ide.api.concept.GetConcept;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedPropertyInstance;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

@Api(tags = "Concepts")
@Path("/")
public class Concept
{
    private final PureSession session;

    public Concept(PureSession session)
    {
        this.session = session;
    }

    @POST
    @Path("getConcept")
    public Response getConcept(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            this.session.saveOnly(request, response, outputStream, new GetConcept());
        }).build();
    }

    @GET
    @Path("getConceptInfo")
    public Response getConceptInfo(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            String file = request.getParameter("file");
            String line = request.getParameter("line");
            String column = request.getParameter("column");

            PureRuntime pureRuntime = session.getPureRuntime();
            Source src = pureRuntime.getSourceById(file);

            response.setContentType("application/json");
            if (src != null)
            {
                CoreInstance found = src.navigate(Integer.parseInt(line), Integer.parseInt(column), session.getPureRuntime().getProcessorSupport());
                if (found != null)
                {
                    if (Instance.instanceOf(found, M3Paths.AbstractProperty, session.getPureRuntime().getProcessorSupport()))
                    {
                        String path = PackageableElement.getUserPathForPackageableElement(found);
                        CoreInstance owner = Instance.getValueForMetaPropertyToOneResolved(found, M3Properties.owner, session.getPureRuntime().getProcessorSupport());
                        String ownerPath = PackageableElement.getUserPathForPackageableElement(owner);
                        outputStream.write(("{\"path\":\"" + path + "\",\"pureName\":\"" + found.getValueForMetaPropertyToOne(M3Properties.name).getName() + "\",\"owner\":\"" + ownerPath + "\",\"pureType\":\"" + (found instanceof QualifiedPropertyInstance ? "QualifiedProperty" : "Property") + "\"}").getBytes());
                    }
                    else if (Instance.instanceOf(found, M3Paths.Enum, session.getPureRuntime().getProcessorSupport()))
                    {
                        String path = PackageableElement.getUserPathForPackageableElement(found);
                        CoreInstance owner = found.getClassifier();
                        String ownerPath = PackageableElement.getUserPathForPackageableElement(owner);
                        outputStream.write(("{\"path\":\"" + path + "\",\"pureName\":\"" + found.getName() + "\",\"owner\":\"" + ownerPath + "\",\"pureType\":\"Enum\"}").getBytes());
                    }
                    else
                    {
                        String path = PackageableElement.getUserPathForPackageableElement(found);
                        outputStream.write(("{\"path\":\"" + path + "\",\"pureName\":\"" + (found instanceof ConcreteFunctionDefinition ? found.getValueForMetaPropertyToOne(M3Properties.functionName).getName() : found.getName()) + "\",\"pureType\":\"" + found.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.rawType).getName() + "\"}").getBytes());
                    }
                }
                else
                {
                    this.writeErrorResponse(outputStream, file, line, column);
                }
            }
            else
            {
                this.writeErrorResponse(outputStream, file, line, column);
            }

        }).build();
    }

    private void writeErrorResponse(OutputStream outStream, String file, String line, String column) throws IOException
    {
        outStream.write(("{\"error\":true,\"text\":\"Cannot find source for file: " + file + " line: " + line + " col: " + column + "\"}").getBytes());
    }
}
