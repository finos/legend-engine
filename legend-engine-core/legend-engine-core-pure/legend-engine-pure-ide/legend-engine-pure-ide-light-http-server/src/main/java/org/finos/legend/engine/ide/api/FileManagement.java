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

package org.finos.legend.engine.ide.api;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.engine.ide.session.PureSessionManager;
import org.finos.legend.pure.m3.serialization.filesystem.repository.ScratchCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNodeStatus;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.ImmutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableRepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.Version;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.VersionControlledCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.welcome.WelcomeCodeStorage;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.regex.Pattern;

@Api(tags = "File Management")
@Path("/")
public class FileManagement
{
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("/?+(\\w+/)*+\\w+(\\.\\w++)*+");

    private final PureSessionManager sessionManager;

    public FileManagement(PureSessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }


    private static final Predicate<CodeStorageNode> IGNORED_NODE = (Predicate<CodeStorageNode>) node ->
    {
        String name = node.getName();
        return ".svn".equals(name) || (name != null && name.endsWith(".iml"));
    };

    private static final Comparator<CodeStorageNode> NODE_COMPARATOR = (node1, node2) ->
    {
        if (node1.isDirectory())
        {
            return node2.isDirectory() ? node1.getName().compareTo(node2.getName()) : -1;
        }
        else
        {
            return node2.isDirectory() ? 1 : node1.getName().compareTo(node2.getName());
        }
    };

    @DELETE
    @Path("deleteFile/{filePath:.+}")
    public Response deleteFile(@PathParam("filePath") String filePath, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            sessionManager.getSession().getPureRuntime().delete("/" + filePath);
            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(("{\"cached\":" + false + "}").getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    @POST
    @Path("newFile/{filePath:.+}")
    public Response newFile(@PathParam("filePath") String filePath, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            sessionManager.getSession().getPureRuntime().create("/" + filePath);

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(("{\"cached\":" + false + "}").getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    @POST
    @Path("newFolder/{filePath:.+}")
    public Response newFolder(@PathParam("filePath") String filePath, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            this.sessionManager.getSession().getCodeStorage().createFolder("/" + filePath);

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(("{\"cached\":" + false + "}").getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    @PUT
    @Path("renameFile")
    public Response renameFile(RenameFileInput input, @Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            String oldPath = input.oldPath;
            String newPath = input.newPath;

            if (oldPath == null || !FILE_NAME_PATTERN.matcher(oldPath).matches())
            {
                throw new IllegalArgumentException("Invalid old path");
            }
            if (newPath == null || !FILE_NAME_PATTERN.matcher(newPath).matches())
            {
                throw new IllegalArgumentException("Invalid new path");
            }

            this.sessionManager.getSession().getPureRuntime().move(oldPath, newPath);

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(("{\"oldPath\":\"" + JSONValue.escape(oldPath) + "\",\"newPath\":\"" + JSONObject.escape(newPath) + "\"}").getBytes());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    @GET
    @Path("dir")
    public Response dir(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        try
        {
            response.setContentType("application/json");
            String path = request.getParameter("parameters");
            StringBuilder json = new StringBuilder("[");
            MutableList<CodeStorageNode> nodes = LazyIterate.reject(sessionManager.getSession().getCodeStorage().getFiles(path), IGNORED_NODE).toSortedList(NODE_COMPARATOR);
            if ("/".equals(path))
            {
                nodes.sortThis((o1, o2) ->
                {
                    String name1 = WelcomeCodeStorage.WELCOME_FILE_NAME.equals(o1.getName()) || "platform".equals(o1.getName()) || ScratchCodeRepository.NAME.equals(o1.getName()) ? "zzz" + o1.getName() : o1.getName();
                    String name2 = WelcomeCodeStorage.WELCOME_FILE_NAME.equals(o2.getName()) || "platform".equals(o2.getName()) || ScratchCodeRepository.NAME.equals(o2.getName()) ? "zzz" + o2.getName() : o2.getName();
                    return name1.compareTo(name2);
                });
            }
            ;
            if (nodes.notEmpty())
            {
                MutableRepositoryCodeStorage codeStorage = sessionManager.getSession().getCodeStorage();
                Iterator<CodeStorageNode> iterator = nodes.iterator();
                writeNode(json, codeStorage, path, iterator.next());
                while (iterator.hasNext())
                {
                    json.append(',');
                    writeNode(json, codeStorage, path, iterator.next());
                }
            }
            json.append(']');

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(json.toString().getBytes(), 0, json.length());
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    @GET
    @Path("fileAsJson/{filePath:.+}")
    public Response fileAsJson(@PathParam("filePath") String filePath)
    {
        try
        {
            MutableRepositoryCodeStorage codeStorage = sessionManager.getSession().getCodeStorage();
            if (codeStorage == null)
            {
                throw new RuntimeException("Cannot find code storage");
            }
            byte[] content;
            try
            {
                content = codeStorage.getContentAsBytes(filePath);
            }
            catch (Exception e)
            {
                StringBuilder message = new StringBuilder("Error accessing resource \"");
                message.append(filePath);
                message.append('"');
                if (e.getMessage() != null)
                {
                    message.append(": ");
                    message.append(e.getMessage());
                }
                throw new IOException(message.toString(), e);
            }
            if (content == null)
            {
                throw new IOException("Could not find resource \"" + filePath + "\"");
            }

            return Response.ok((StreamingOutput) outputStream ->
            {
                outputStream.write(this.transformContent(content, codeStorage.getOriginalCodeStorage(codeStorage.getRepositoryForPath(filePath)) instanceof ImmutableRepositoryCodeStorage));
                outputStream.close();
            }).build();
        }
        catch (Exception e)
        {
            return Response.status(Response.Status.BAD_REQUEST).entity((StreamingOutput) outputStream ->
            {
                outputStream.write(("\"" + JSONValue.escape(ExceptionTranslation.buildExceptionMessage(sessionManager.getSession(), e, new ByteArrayOutputStream()).getText()) + "\"").getBytes());
                outputStream.close();
            }).build();
        }
    }

    private byte[] transformContent(byte[] content, boolean isImmutable)
    {
        JSONObject object = new JSONObject();
        object.put("content", new String(content));
        object.put("RO", isImmutable);
        return object.toJSONString().getBytes();
    }

    private void writeNode(StringBuilder builder, MutableRepositoryCodeStorage codeStorage, String path, CodeStorageNode node)
    {
        String fullPath = "/".equals(path) ? (path + node.getName()) : (path + "/" + node.getName());
        if (node.isDirectory())
        {
            if ("/".equals(path))
            {
                writeRepoNode(builder, codeStorage, fullPath, node);
            }
            else
            {
                writeDirectoryNode(builder, fullPath, codeStorage, node);
            }
        }
        else
        {
            writeFileNode(builder, codeStorage, fullPath, node);
        }
    }

    private void writeRepoNode(StringBuilder builder, MutableRepositoryCodeStorage cs, String path, CodeStorageNode repo)
    {
        VersionControlledCodeStorage codeStorage = (VersionControlledCodeStorage) cs;
        // Assume SVN until Git code storage is added
        Optional<String> currentRev = codeStorage.getCurrentRevision(path);
        long currentRevision = currentRev.isPresent() ? Long.parseLong(currentRev.get()) : 0L;
        String repoName = codeStorage.getRepositoryForPath(path).getName();
        builder.append("{\"li_attr\":{\"id\":\"file_");
        builder.append(path);
        builder.append("\",\"path\":\"").append(path).append("\",\"file\":false,\"repo\":true");
        builder.append(",\"RO\":").append(codeStorage.getOriginalCodeStorage(codeStorage.getRepositoryForPath(path)) instanceof ImmutableRepositoryCodeStorage);
        builder.append("},\"text\":\"");
        builder.append(repo.getName());
        if (currentRevision >= 0)
        {
            builder.append("\",\"icon\":\"/ide/pure/icons/filesystem/cloud.png\",\"state\":\"closed\",\"children\":true}");
        }
        else if ("platform".equals(repoName))
        {
            builder.append(" (");
            builder.append(Version.SERVER);
            builder.append(')');
            builder.append("\",\"icon\":\"/ide/pure/icons/wrench.png\",\"state\":\"closed\",\"children\":true}");
        }
        else
        {
            builder.append("\",\"icon\":\"/ide/pure/icons/scratchpad.png\",\"state\":\"closed\",\"children\":true}");
        }
    }

    private void writeDirectoryNode(StringBuilder builder, String path, MutableRepositoryCodeStorage codeStorage, CodeStorageNode directory)
    {
        builder.append("{\"li_attr\":{\"id\":\"file_");
        builder.append(path);
        builder.append("\",\"path\":\"").append(path).append("\",\"file\":false");
        builder.append(",\"RO\":").append(codeStorage.getOriginalCodeStorage(codeStorage.getRepositoryForPath(path)) instanceof ImmutableRepositoryCodeStorage);
        builder.append("},\"text\":\"");
        builder.append(directory.getName());
        builder.append("\",\"state\":\"closed\",\"children\":").append(!codeStorage.isEmptyFolder(path)).append("}");
    }

    private void writeFileNode(StringBuilder builder, MutableRepositoryCodeStorage codeStorage, String path, CodeStorageNode file)
    {
        builder.append("{\"li_attr\":{\"id\":\"file_");
        builder.append(path);
        builder.append("\",\"path\":\"").append(path).append("\",\"file\":true");
        builder.append(",\"RO\":").append(codeStorage.getOriginalCodeStorage(codeStorage.getRepositoryForPath(path)) instanceof ImmutableRepositoryCodeStorage);

        if (file.getStatus() != CodeStorageNodeStatus.NORMAL)
        {
            builder.append(",\"statusType\":\"");
            builder.append(file.getStatus());
            builder.append('"');
        }

        builder.append("},\"text\":\"");
        builder.append(file.getName());
        builder.append("\",\"icon\":\"/ide/pure/icons/filesystem/txt.png\"}");
    }


    public static class RenameFileInput
    {
        public String oldPath;
        public String newPath;
    }
}