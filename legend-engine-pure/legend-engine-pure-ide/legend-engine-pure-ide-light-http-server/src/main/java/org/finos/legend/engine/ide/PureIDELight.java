//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.ide;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.pure.runtime.compiler.interpreted.natives.LegendCompileMixedProcessorSupport;
import org.finos.legend.engine.ide.PureIDEServer;
import org.finos.legend.engine.ide.SourceLocationConfiguration;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.repository.GenericCodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.fs.MutableFSCodeStorage;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

import javax.xml.transform.Source;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PureIDELight extends PureIDEServer
{
    public static void main(String[] args) throws Exception
    {
        System.setProperty("legend.test.h2.port", "1975");
        new PureIDELight().run(args.length == 0 ? new String[]{"server", "legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json"} : args);
    }

    @Override
    protected void postInit()
    {
        FunctionExecutionInterpreted fe = (FunctionExecutionInterpreted) this.getPureSession().getFunctionExecution();
        fe.setProcessorSupport(new LegendCompileMixedProcessorSupport(fe.getRuntime().getContext(), fe.getRuntime().getModelRepository(), fe.getProcessorSupport()));
    }
}
