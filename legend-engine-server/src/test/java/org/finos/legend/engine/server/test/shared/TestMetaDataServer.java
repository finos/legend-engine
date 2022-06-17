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

package org.finos.legend.engine.server.test.shared;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.primitive.LongList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.finos.legend.engine.server.test.shared.execute.PureFunctions;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageNode;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_router_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;

public class TestMetaDataServer
{
    private final Server server;
    private final MutableMap<String, String> fromMappings = Maps.mutable.empty();
    private final MutableMap<String, String> fromServices = Maps.mutable.empty();
    private final MutableMap<String, String> fromStores = Maps.mutable.empty();

    public static void main(String[] args) throws Exception
    {
        TestMetaDataServer testMetaDataServer = new TestMetaDataServer(Integer.parseInt(args[0]), true);
        testMetaDataServer.join();
    }

    public TestMetaDataServer(int port, boolean messageFromPureJAR) throws Exception
    {
        this.server = new Server(port);
        CompiledExecutionSupport executionSupport = new CompiledExecutionSupport(
                new JavaCompilerState(null, TestMetaDataServer.class.getClassLoader()),
                new CompiledProcessorSupport(TestMetaDataServer.class.getClassLoader(), MetadataLazy.fromClassLoader(TestMetaDataServer.class.getClassLoader(), CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName)), Sets.mutable.empty()),
                null,
                new CodeStorage()
                {
                    @Override
                    public String getRepoName(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getAllRepoNames()
                    {
                        return null;
                    }

                    @Override
                    public boolean isRepoName(String s)
                    {
                        return false;
                    }

                    @Override
                    public RichIterable<CodeRepository> getAllRepositories()
                    {
                        return null;
                    }

                    @Override
                    public CodeRepository getRepository(String s)
                    {
                        return null;
                    }

                    @Override
                    public CodeStorageNode getNode(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<CodeStorageNode> getFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getUserFiles()
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<String> getFileOrFiles(String s)
                    {
                        return null;
                    }

                    @Override
                    public InputStream getContent(String s)
                    {
                        return null;
                    }

                    @Override
                    public byte[] getContentAsBytes(String s)
                    {
                        return new byte[0];
                    }

                    @Override
                    public String getContentAsText(String s)
                    {
                        return null;
                    }

                    @Override
                    public boolean exists(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFile(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isFolder(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isEmptyFolder(String s)
                    {
                        return false;
                    }

                    @Override
                    public boolean isVersioned(String s)
                    {
                        return false;
                    }

                    @Override
                    public long getCurrentRevision(String s)
                    {
                        return 1234;
                    }

                    @Override
                    public LongList getAllRevisions(String s)
                    {
                        return null;
                    }

                    @Override
                    public RichIterable<Revision> getAllRevisionLogs(RichIterable<String> richIterable)
                    {
                        return null;
                    }
                },
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );

        AbstractHandler mappingHandle = registerService(
                "/alloy/pureModelFromMapping",
                messageFromPureJAR ?
                        (_package, version) -> PureFunctions.alloy_metadataServer_pureModelFromMapping(_package, version, Root_meta_relational_extension_relationalExtensions__Extension_MANY_(executionSupport), executionSupport) :
                        (_package, version) ->
                        {
                            String key = "" + _package + version;
                            String res = fromMappings.get(key);
                            System.out.println(key);
                            Assert.assertTrue(res != null, () -> key + " can't be found");
                            return res;
                        }
        );

        AbstractHandler storeHandle = registerService(
                "/alloy/pureModelFromStore",
                messageFromPureJAR ?
                        (_package, version) -> PureFunctions.alloy_metadataServer_pureModelFromStore(_package, version, Root_meta_relational_extension_relationalExtensions__Extension_MANY_(executionSupport), executionSupport) :
                        (_package, version) ->
                        {
                            String key = "" + _package + version;
                            String res = fromStores.get(key);
                            System.out.println(key);
                            Assert.assertTrue(res != null, () -> key + " can't be found");
                            return res;
                        }
        );


        AbstractHandler versionHandler = createVersionHandler();
        AbstractHandler pureBaseVersionHandler = createPureBaseVersionHandler();

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.setHandlers(new Handler[]{
                mappingHandle,
                storeHandle,
                versionHandler,
                pureBaseVersionHandler});

        this.server.setHandler(handlerCollection);
        this.server.start();

        System.out.println("Started Test MetaData server on port " + port);
    }

    public void join() throws Exception
    {
        this.server.join();
    }

    public void shutDown() throws Exception
    {
        this.server.stop();
    }

    private static AbstractHandler registerService(String mappingBase, Function2<String, String, String> content)
    {
        ContextHandler contextHandler = new ContextHandler(mappingBase + "/*");
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                OutputStream stream = httpServletResponse.getOutputStream();

                String url = request.getRequestURL().toString();
                String path = url.substring(url.indexOf(mappingBase) + mappingBase.length());

                if (path.startsWith("/") && path.length() > 2)
                {
                    int end = path.indexOf("/", 1);
                    String version = path.substring(1, end);
                    String _package = path.substring(end + 1);
                    String r = content.value(_package, version);
                    stream.write(r.getBytes());
                    stream.flush();
                }
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private AbstractHandler createPureBaseVersionHandler()
    {
        ContextHandler contextHandler = new ContextHandler("/alloy/pureServerBaseVersion");
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                OutputStream stream = httpServletResponse.getOutputStream();
                String r = "1";
                stream.write(r.getBytes());
                stream.flush();
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }

    private AbstractHandler createVersionHandler()
    {
        ContextHandler contextHandler = new ContextHandler("/jsonInfo");
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                OutputStream stream = httpServletResponse.getOutputStream();
                stream.write("{}".getBytes());
                stream.flush();
            }
        };
        contextHandler.setHandler(handler);
        return contextHandler;
    }
}