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

package org.finos.legend.engine.ide.helpers.response;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.ide.helpers.response.Candidate;
import org.finos.legend.engine.ide.helpers.response.IDEExceptionResponse;
import org.finos.legend.engine.ide.helpers.response.IDEParserOrCompilerException;
import org.finos.legend.engine.ide.helpers.response.IDEPureUnmatchedFunctionExceptionResponse;
import org.finos.legend.engine.ide.helpers.response.IDEPureUnresolvedIdentifierExceptionResponse;
import org.finos.legend.engine.ide.helpers.response.IDEResponse;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.exception.PureUnmatchedFunctionException;
import org.finos.legend.pure.m3.exception.PureUnresolvedIdentifierException;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ExceptionTranslation
{
    public static final SourceInformation DUMMY_SOURCE_INFORMATION = new SourceInformation("TOFILL", -1, -1, -1, -1);

    public static org.finos.legend.engine.ide.helpers.response.IDEResponse buildExceptionMessage(PureSession session, Throwable t, ByteArrayOutputStream pureResponse)
    {
        try
        {
            PureException pureException = PureException.findPureException(t);
            if (pureException != null)
            {
                return pureExceptionToJson(session, pureException, pureResponse);
            }
        }
        catch (Exception ignore)
        {
            // ignore exceptions translating exception to JSON
        }

        IDEExceptionResponse response = new IDEExceptionResponse();
        if (pureResponse != null)
        {
            String pureResponseStr = pureResponse.toString() + '\n';
            response.appendText(pureResponseStr);
        }

        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        response.appendText(sw.toString());

//        if (null != session)
//        {
//             response.compiler = "'" + session.getCompilerLogs() + "'";
//        }
        return response;
    }

    public static IDEResponse pureExceptionToJson(PureSession session, PureException e, ByteArrayOutputStream pureResponse)
    {
        PureException original = e.getOriginatingPureException();
        PureRuntime runtime = (session == null) ? null : session.getPureRuntime();

        IDEExceptionResponse response;
        if (e instanceof PureUnresolvedIdentifierException)
        {
            final PureUnresolvedIdentifierException exception = (PureUnresolvedIdentifierException) e;
            if (exception.getImportCandidates(Objects.requireNonNull(session).codeStorage.getAllRepositories()).isEmpty())
            {
                response = new IDEExceptionResponse();
            }
            else
            {
                RichIterable<CoreInstance> candidates = exception.getImportCandidates(session.codeStorage.getAllRepositories());
                MutableMap<String, Pair<SourceInformation, String>> sourceInfoAndTypeByPath = Maps.mutable.ofInitialCapacity(candidates.size());

                for (CoreInstance candidate : candidates)
                {
                    SourceInformation sourceInfo = candidate.getSourceInformation();
                    // TODO think about what we should do with candidates with no source info
                    if ((sourceInfo != null) && (runtime != null))
                    {
                        sourceInfoAndTypeByPath.put(PackageableElement.getUserPathForPackageableElement(candidate), Tuples.pair(sourceInfo, candidate.getClassifier().getName()));
                    }
                }

                SourceInformation sourceToBeModified = exception.getImportGroup().getSourceInformation();
                if (sourceToBeModified == null)
                {
                    //avoid null pointer exception
                    sourceToBeModified = DUMMY_SOURCE_INFORMATION;
                }

                MutableSet<String> pathsInSameSource = Sets.mutable.empty();
                MutableSet<String> pathsNotInSameSource = Sets.mutable.empty();
                sourceInfoAndTypeByPath.forEachKeyValue((s, sourceInformationStringPair) ->
                {
                    if (sourceInformationStringPair.getOne().getSourceId().equals(exception.getImportGroup().getSourceInformation().getSourceId()))
                    {
                        pathsInSameSource.add(s);
                    }
                    else
                    {
                        pathsNotInSameSource.add(s);
                    }
                });

                org.finos.legend.engine.ide.helpers.response.IDEPureUnresolvedIdentifierExceptionResponse unresolvedResponse = new IDEPureUnresolvedIdentifierExceptionResponse();
                response = unresolvedResponse;
                unresolvedResponse.candidateName = exception.getIdOrPath();
                MutableList<org.finos.legend.engine.ide.helpers.response.Candidate> candidatesOutput = Lists.mutable.of();
                unresolvedResponse.candidates = candidatesOutput;

                for (String path : pathsInSameSource.toSortedList().withAll(pathsNotInSameSource.toSortedList()))
                {
                    Pair<SourceInformation, String> pair = sourceInfoAndTypeByPath.get(path);
                    SourceInformation sourceInfo = pair.getOne();
                    String type = pair.getTwo();
                    String id = exception.getIdOrPath();
                    int index = id.lastIndexOf("::");
                    if (index != -1)
                    {
                        //id contains "::"
                        id = id.substring(index + 2);
                    }
                    index = path.lastIndexOf(id);
                    if (index == -1)
                    {
                        throw new RuntimeException("Unable to find identifier: " + exception.getIdOrPath());
                    }
                    String importToAdd = "import " + path.substring(0, index) + "*;";

                    int lineToBeModified = sourceToBeModified.getStartLine();
                    int columnToBeModified = sourceToBeModified.getStartColumn();
                    if (columnToBeModified == 0)
                    {
                        //special handling for importGroup without any imports, need to add import to the next line
                        lineToBeModified++;
                    }

                    org.finos.legend.engine.ide.helpers.response.Candidate candidate = new org.finos.legend.engine.ide.helpers.response.Candidate();
                    candidate.sourceID = sourceInfo.getSourceId();
                    candidate.line = sourceInfo.getLine();
                    candidate.column = sourceInfo.getColumn();
                    candidate.foundName = path;
                    candidate.fileToBeModified = sourceToBeModified.getSourceId();
                    candidate.lineToBeModified = lineToBeModified;
                    candidate.columnToBeModified = columnToBeModified;
                    candidate.add = true;
                    candidate.messageToBeModified = importToAdd;
                    candidate.type = type;
                    candidatesOutput.add(candidate);
                }
            }
        }
        else if (e instanceof PureUnmatchedFunctionException)
        {
            final PureUnmatchedFunctionException exception = (PureUnmatchedFunctionException) e;
            if (0 == exception.getImportCandidatesWithPackageNotImported().size() + exception.getImportCandidatesWithPackageImported().size())
            {
                response = new IDEExceptionResponse();
            }
            else
            {
                SourceInformation sourceToBeModified = exception.getImportGroup().getSourceInformation();
                if (sourceToBeModified == null)
                {
                    // avoid null pointer exception
                    sourceToBeModified = DUMMY_SOURCE_INFORMATION;
                }

                org.finos.legend.engine.ide.helpers.response.IDEPureUnmatchedFunctionExceptionResponse ideUnmatchedResponse = new IDEPureUnmatchedFunctionExceptionResponse();
                response = ideUnmatchedResponse;
                ideUnmatchedResponse.candidateName = exception.getFunctionName();

                MutableList<org.finos.legend.engine.ide.helpers.response.Candidate> candidatesOutputWithPackageNotImported = Lists.mutable.of();
                ideUnmatchedResponse.candidates = candidatesOutputWithPackageNotImported;
                MutableList<org.finos.legend.engine.ide.helpers.response.Candidate> candidatesOutputWithPackageImported = Lists.mutable.of();
                ideUnmatchedResponse.candidatesWithPackageImported = candidatesOutputWithPackageImported;
                getCandidatesOutput(session, runtime, exception, sourceToBeModified, candidatesOutputWithPackageNotImported, exception.getImportCandidatesWithPackageNotImported());
                getCandidatesOutput(session, runtime, exception, sourceToBeModified, candidatesOutputWithPackageImported, exception.getImportCandidatesWithPackageImported());
            }
        }
        else if ((e instanceof PureParserException || e instanceof PureCompilationException))
        {
            org.finos.legend.engine.ide.helpers.response.IDEParserOrCompilerException parserOrCompilerException = new IDEParserOrCompilerException();
            parserOrCompilerException.exceptionType = e.getExceptionName();
            response = parserOrCompilerException;
        }
        else
        {
            response = new IDEExceptionResponse();
        }

        if (pureResponse != null)
        {
            String pureResponseStr;
            try
            {
                pureResponseStr = pureResponse.toString(StandardCharsets.UTF_8.name()) + "\n";
            }
            catch (UnsupportedEncodingException ex)
            {
                // this should never happen, but just in case ...
                pureResponseStr = pureResponse + "\n";
            }
            response.appendText(pureResponseStr);
        }

        if (e.hasPureStackTrace())
        {
            response.appendText(original.getMessage() + "\n" + e.getPureStackTrace("    "));
        }
        else
        {
            response.appendText(original.getMessage());
        }

        SourceInformation sourceInformation = original.getSourceInformation();
        if ((sourceInformation != null) && (runtime != null) && (runtime.getSourceById(sourceInformation.getSourceId()) != null))
        {
            response.RO = runtime.isSourceImmutable(sourceInformation.getSourceId());
            response.source = sourceInformation.getSourceId();
            response.line = sourceInformation.getLine();
            response.column = sourceInformation.getColumn();
        }

        if (session != null)
        {
            // response.compiler = session.getCompilerLogs();
        }

        return response;
    }

    private static void getCandidatesOutput(PureSession session, PureRuntime runtime, final PureUnmatchedFunctionException exception, SourceInformation sourceToBeModified, MutableList<org.finos.legend.engine.ide.helpers.response.Candidate> candidatesOutput, ListIterable<CoreInstance> candidates)
    {
        MutableMap<String, SourceInformation> sourceInfoAndTypeByPath = Maps.mutable.ofInitialCapacity(candidates.size());
        for (CoreInstance candidate : candidates)
        {
            SourceInformation sourceInfo = candidate.getSourceInformation();
            if ((sourceInfo != null) && (runtime != null))
            {
                StringBuilder functionNameStringBuilder = new StringBuilder();
                try
                {
                    org.finos.legend.pure.m3.navigation.function.Function.print(functionNameStringBuilder, candidate, runtime.getProcessorSupport());
                }
                catch (Exception functionPrintException)
                {
                    // Log error if possible, then ignore the function and continue
                    if (session != null)
                    {
                        //session.log("Error printing: " + candidate, functionPrintException);
                    }
                    continue;
                }
                sourceInfoAndTypeByPath.put(functionNameStringBuilder.toString(), sourceInfo);
            }
        }

        MutableSet<String> pathsInSameSource = Sets.mutable.empty();
        MutableSet<String> pathsNotInSameSource = Sets.mutable.empty();
        sourceInfoAndTypeByPath.forEachKeyValue((s, sourceInfo) ->
        {
            if (sourceInfo.getSourceId().equals(exception.getImportGroup().getSourceInformation().getSourceId()))
            {
                pathsInSameSource.add(s);
            }
            else
            {
                pathsNotInSameSource.add(s);
            }
        });

        pathsInSameSource.toSortedList().withAll(pathsNotInSameSource.toSortedList()).forEach(path ->
        {
            SourceInformation sourceInfo = sourceInfoAndTypeByPath.get(path);
            String functionName = exception.getFunctionName();
            int index = functionName.lastIndexOf("::");
            if (index != -1)
            {
                //id contains "::"
                functionName = functionName.substring(index + 2);
            }
            index = path.lastIndexOf(functionName);
            if (index != -1)
            {
                String importToAdd = "import " + path.substring(0, index) + "*;";

                int lineToBeModified = sourceToBeModified.getStartLine();
                int columnToBeModified = sourceToBeModified.getStartColumn();
                if (0 == columnToBeModified)
                {
                    //special handling for importGroup without any imports, need to add import to the next line
                    lineToBeModified++;
                }

                org.finos.legend.engine.ide.helpers.response.Candidate candidate = new Candidate();
                candidate.sourceID = sourceInfo.getSourceId();
                candidate.line = sourceInfo.getLine();
                candidate.column = sourceInfo.getColumn();
                candidate.foundName = path;
                candidate.fileToBeModified = sourceToBeModified.getSourceId();
                candidate.lineToBeModified = lineToBeModified;
                candidate.columnToBeModified = columnToBeModified;
                candidate.add = true;
                candidate.messageToBeModified = importToAdd;
                candidatesOutput.add(candidate);
            }
        });
    }
}
