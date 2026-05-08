// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.changetoken.generation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_changetoken_Versions;
import org.finos.legend.pure.generated.Root_meta_pure_functions_collection_List_Impl;
import org.finos.legend.pure.generated.core_pure_changetoken_cast_generation;
import org.finos.legend.pure.generated.core_pure_changetoken_diff_generation;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.VersionControlledClassLoaderCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.composite.CompositeCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataPelt;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GenerateDiff
{
    private final CompiledExecutionSupport executionSupport;
    private final PureModel pureModel;
    private String typeKeyName = "@type";
    private String versionKeyName = "version";

    public GenerateDiff(List<PackageableElement> newEntities, List<PackageableElement> newDependencies, List<PackageableElement> oldEntities, List<PackageableElement> oldDependencies)
    {
        PureJavaCompiler compiler = new PureJavaCompiler(new Message("ChangeTokens"));
        MemoryClassLoader classLoader = compiler.getClassLoader();
        PureModelContextData.Builder pureModelContextDataBuilder = PureModelContextData.newBuilder();
        Set<String> oldDependenciesPaths = oldDependencies.stream().map(PackageableElement::getPath).collect(Collectors.toSet());
        oldDependencies = ElementPathTransformer.newTransformer("old::dependencies::vX_X_X::"::concat).addElements(oldDependencies).transformElements();
        pureModelContextDataBuilder.addElements(oldDependencies);
        oldEntities = ElementPathTransformer.newTransformer((e) -> oldDependenciesPaths.contains(e) ? "old::dependencies::vX_X_X::".concat(e) : "old::entities::vX_X_X::".concat(e)).addElements(oldEntities).transformElements();
        pureModelContextDataBuilder.addElements(oldEntities);
        Set<String> newDependenciesPaths = newDependencies.stream().map(PackageableElement::getPath).collect(Collectors.toSet());
        newDependencies = ElementPathTransformer.newTransformer("new::dependencies::vX_X_X::"::concat).addElements(newDependencies).transformElements();
        pureModelContextDataBuilder.addElements(newDependencies);
        newEntities = ElementPathTransformer.newTransformer((e) -> newDependenciesPaths.contains(e) ? "new::dependencies::vX_X_X::".concat(e) : "new::entities::vX_X_X::".concat(e)).addElements(newEntities).transformElements();
        pureModelContextDataBuilder.addElements(newEntities);
        PureModelContextData pureModelContextData = pureModelContextDataBuilder.build();
        executionSupport = new CompiledExecutionSupport(new JavaCompilerState(null, classLoader), new CompiledProcessorSupport(classLoader, MetadataPelt.fromClassLoader(classLoader, CodeRepositoryProviderHelper.findCodeRepositories().collect(CodeRepository::getName)), Sets.mutable.empty()), null, new CompositeCodeStorage(new VersionControlledClassLoaderCodeStorage(classLoader, Lists.mutable.of(CodeRepositoryProviderHelper.findPlatformCodeRepository()), null)), null, null, new ConsoleCompiled(), null, Sets.mutable.empty(), CompiledExtensionLoader.extensions());
        pureModel = new PureModel(pureModelContextData, Identity.getAnonymousIdentity().getName(), classLoader, DeploymentMode.PROD);
    }

    private static PureMap toPureMap(Map objectNode)
    {
        if (objectNode == null)
        {
            return null;
        }
        Map<String, Object> res = Maps.mutable.empty();
        Iterator<Map.Entry<String, Object>> it = objectNode.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Object> en = it.next();
            if (en.getValue() instanceof Map)
            {
                res.put(en.getKey(), toPureMap((Map) en.getValue()));
            }
            else if (en.getValue() instanceof List)
            {
                MutableList<Object> list = Lists.mutable.empty();
                for (Object o : (List<?>) en.getValue())
                {
                    list.add(o instanceof Map ? toPureMap((Map) o) : o);
                }
                res.putIfAbsent(en.getKey(), new Root_meta_pure_functions_collection_List_Impl<>("")._values(list));
            }
            else
            {
                res.putIfAbsent(en.getKey(), en.getValue());
            }
        }
        return new PureMap(res);
    }

    public String getTypeKeyName()
    {
        return typeKeyName;
    }

    public void setTypeKeyName(String typeKeyName)
    {
        this.typeKeyName = typeKeyName;
    }

    public String getVersionKeyName()
    {
        return versionKeyName;
    }

    public void setVersionKeyName(String versionKeyName)
    {
        this.versionKeyName = versionKeyName;
    }

    public String execute(String jsonString, Map<String, Map<String, String>> propertyRenames, Map<String, String> classRenames, Map<String, Map<String, Object>> defaultValues)
    {
        Root_meta_pure_changetoken_Versions oldVersions = core_pure_changetoken_cast_generation.Root_meta_pure_changetoken_cast_generation_jsonToVersions_String_1__String_1__Versions_1_(jsonString, typeKeyName, this.executionSupport);
        Root_meta_pure_changetoken_Versions newVersions = core_pure_changetoken_diff_generation.Root_meta_pure_changetoken_diff_generation_generateDiffFromVersions_Versions_1__Map_$0_1$__Map_$0_1$__Map_$0_1$__String_1__String_$0_1$__Versions_1_(oldVersions, toPureMap(propertyRenames), toPureMap(classRenames), toPureMap(defaultValues),
                typeKeyName, versionKeyName,
                this.pureModel.getExecutionSupport());
        return core_pure_changetoken_cast_generation.Root_meta_pure_changetoken_cast_generation_versionsToJson_Versions_1__String_1__String_1_(newVersions, typeKeyName, this.executionSupport);
    }
}
