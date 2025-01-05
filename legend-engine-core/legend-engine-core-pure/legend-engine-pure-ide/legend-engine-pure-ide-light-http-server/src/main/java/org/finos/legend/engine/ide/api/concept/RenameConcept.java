// Copyright 2023 Goldman Sachs
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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.engine.ide.session.PureSession;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Api(tags = "Concepts")
@Path("/")
public class RenameConcept
{
    private final PureSession session;

    public RenameConcept(PureSession session)
    {
        this.session = session;
    }

    @PUT
    @Path("renameConcept")
    public Response renameConcept(RenameConceptInput input, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            MutableListMultimap<String, org.finos.legend.engine.ide.api.concept.RenameConceptEntry> entryIndex = Multimaps.mutable.list.empty();
            for (org.finos.legend.engine.ide.api.concept.RenameConceptUtility.RenameConceptInputSourceInformation sourceInformation : input.sourceInformations)
            {
                entryIndex.add(
                        Tuples.pair(
                                sourceInformation.sourceId,
                                new RenameConceptEntry(sourceInformation.line - 1, sourceInformation.column - 1, input.oldName, input.pureType, input.newName)
                        )
                );
            }

            if (!entryIndex.keysView().allSatisfy(sourceId -> this.session.getPureRuntime().getSourceById(sourceId).isCompiled()))
            {
                throw new IllegalStateException("Source code must be compiled before refactoring");
            }
            if (!entryIndex.keysView().allSatisfy(sourceId -> !sourceId.startsWith("/platform/")))
            {
                throw new IllegalArgumentException("Some files belong in /platform directory. Cannot refactor files in /platform directory");
            }

            entryIndex.forEachKeyMultiValues((sourceId, entry) ->
            {
                String[] originalSourceCodeLines = session.getPureRuntime().getSourceById(sourceId).getContent().split("\n", -1);
                session.getPureRuntime().modify(sourceId, org.finos.legend.engine.ide.api.concept.RenameConceptUtility.replace(originalSourceCodeLines, org.finos.legend.engine.ide.api.concept.RenameConceptUtility.removeInvalidReplaceConceptEntries(originalSourceCodeLines, (MutableList<? extends AbstractRenameConceptEntry>) entry)));
            });

            return Response.noContent().build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(this.session, e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    public static class RenameConceptInput
    {
        public String oldName;
        public String newName;
        public String pureType;
        public List<RenameConceptUtility.RenameConceptInputSourceInformation> sourceInformations;
    }
}
