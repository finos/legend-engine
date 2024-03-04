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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.ide.api.concept.AbstractRenameConceptEntry;
import org.finos.legend.engine.ide.api.concept.RenameConceptUtility;
import org.finos.legend.engine.ide.api.concept.RenamePackageEntry;
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
import java.util.regex.Pattern;

@Api(tags = "Concepts")
@Path("/")
public class MovePackageableElements
{
    private static final Pattern PACKAGE_PATH_PATTERN = Pattern.compile("\\w[\\w$]*+(::\\w[\\w$]*+)*+");

    private final PureSession session;

    public MovePackageableElements(PureSession session)
    {
        this.session = session;
    }

    @PUT
    @Path("movePackageableElements")
    public Response movePackageableElements(List<MovePackageableElementInput> inputs, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            MutableListMultimap<String, org.finos.legend.engine.ide.api.concept.RenamePackageEntry> entryIndex = Multimaps.mutable.list.empty();
            for (MovePackageableElementInput input : inputs)
            {
                String sourcePackage = input.sourcePackage;
                String destinationPackage = input.destinationPackage;

                if (sourcePackage == null || !PACKAGE_PATH_PATTERN.matcher(sourcePackage).matches())
                {
                    throw new IllegalArgumentException("Invalid source package");
                }
                if (destinationPackage == null || !PACKAGE_PATH_PATTERN.matcher(destinationPackage).matches())
                {
                    throw new IllegalArgumentException("Invalid destination package");
                }
                for (org.finos.legend.engine.ide.api.concept.RenameConceptUtility.RenameConceptInputSourceInformation sourceInformation : input.sourceInformations)
                {
                    entryIndex.add(
                            Tuples.pair(
                                    sourceInformation.sourceId,
                                    new org.finos.legend.engine.ide.api.concept.RenamePackageEntry(sourceInformation.line - 1, sourceInformation.column - 1, input.pureName, input.pureType, sourcePackage, destinationPackage)
                            )
                    );
                }
            }

            if (!entryIndex.keysView().allSatisfy(sourceId -> this.session.getPureRuntime().getSourceById(sourceId).isCompiled()))
            {
                throw new IllegalStateException("Source code must be compiled before refactoring");
            }
            if (!entryIndex.keysView().allSatisfy(sourceId -> !sourceId.startsWith("/platform/")))
            {
                throw new IllegalArgumentException("Some files belong in /platform directory. Cannot refactor files in /platform directory");
            }

            entryIndex.forEachKeyMultiValues((sourceId, entries) ->
            {
                String originalSourceCode = session.getPureRuntime().getSourceById(sourceId).getContent();
                String[] originalSourceCodeLines = originalSourceCode.split("\n", -1);
                PartitionMutableList<org.finos.legend.engine.ide.api.concept.RenamePackageEntry> partition = (PartitionMutableList<org.finos.legend.engine.ide.api.concept.RenamePackageEntry>) (org.finos.legend.engine.ide.api.concept.RenameConceptUtility.removeInvalidReplaceConceptEntries(originalSourceCodeLines, Lists.mutable.ofAll(entries)))
                        .partition(entry -> entry.getReplaceColumnIndex() >= 0 && originalSourceCodeLines[entry.getReplaceLineIndex()].startsWith(entry.getOriginalReplaceString(), entry.getReplaceColumnIndex()));
                String updatedSourceCode = org.finos.legend.engine.ide.api.concept.RenameConceptUtility.replace(originalSourceCodeLines, partition.getSelected());
                updatedSourceCode = MovePackageableElements.updateImportStatements(
                        updatedSourceCode.split("\n", -1),
                        partition.getRejected().sortThisBy(AbstractRenameConceptEntry::getReplaceLineIndex)
                );
                session.getPureRuntime().modify(sourceId, updatedSourceCode);
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

    private static String updateImportStatements(String[] sourceCodeLines, MutableList<RenamePackageEntry> importUpdateEntries)
    {
        // identify sections which will reset import scopes
        MutableList<Integer> lineIndices = Lists.mutable.empty();
        lineIndices.add(0);
        for (int i = 0; i < sourceCodeLines.length; ++i)
        {
            if (sourceCodeLines[i].trim().startsWith("###"))
            {
                lineIndices.add(i);
            }
        }
        lineIndices.add(sourceCodeLines.length);

        // find all the import lines to be updated indexed by the section
        MutableSetMultimap<Integer, String> lineImportStatementsMap = Multimaps.mutable.set.empty();
        int i = 0;
        int j = 0;
        while (i < importUpdateEntries.size())
        {
            while (j < lineIndices.size() - 1)
            {
                if (lineIndices.get(j) <= importUpdateEntries.get(i).getReplaceLineIndex() && importUpdateEntries.get(i).getReplaceLineIndex() < lineIndices.get(j + 1))
                {
                    lineImportStatementsMap.add(Tuples.pair(lineIndices.get(j), importUpdateEntries.get(i).getNewReplaceString()));
                    break;
                }
                j++;
            }
            i++;
        }

        // update import statements
        final StringBuilder stringBuilder = new StringBuilder();
        for (int lineIndex = 0; lineIndex < sourceCodeLines.length; ++lineIndex)
        {
            if (lineImportStatementsMap.containsKey(lineIndex))
            {
                if (sourceCodeLines[lineIndex].trim().startsWith("###"))
                {
                    stringBuilder.append(sourceCodeLines[lineIndex]).append("\n");
                    lineImportStatementsMap.get(lineIndex).forEach(destinationPackage -> stringBuilder.append("import ").append(destinationPackage).append("::*;\n"));
                }
                else
                {
                    lineImportStatementsMap.get(lineIndex).forEach(destinationPackage -> stringBuilder.append("import ").append(destinationPackage).append("::*;\n"));
                    stringBuilder.append(sourceCodeLines[lineIndex]).append("\n");
                }
            }
            else
            {
                stringBuilder.append(sourceCodeLines[lineIndex]).append("\n");
            }
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    public static class MovePackageableElementInput
    {
        public String pureName;
        public String pureType;
        public String sourcePackage;
        public String destinationPackage;
        public List<RenameConceptUtility.RenameConceptInputSourceInformation> sourceInformations;
    }
}
