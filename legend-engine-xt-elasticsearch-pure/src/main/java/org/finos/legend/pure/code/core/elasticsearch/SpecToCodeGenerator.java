// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.code.core.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProviderHelper;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.classpath.ClassLoaderCodeStorage;
import org.finos.legend.pure.runtime.java.compiled.compiler.JavaCompilerState;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.ConsoleCompiled;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataLazy;

public class SpecToCodeGenerator
{
    public static void main(String... args) throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        String[] esVersions = args.length != 0 ? args : new String[]{"7.17", "8.3"};
        CompiledExecutionSupport classLoaderExecutionSupport = SpecToCodeGenerator.getClassLoaderExecutionSupport();
        // String pureElementsJson = core_elasticsearch_specification_specToPureGenerator.Root_meta_external_store_elasticsearch_metamodel_spec_toPure_generatePureProtocolJson_String_1__String_1_(esVersion, classLoaderExecutionSupport);

        String pureElementsJson = (String) Class.forName("org.finos.legend.pure.generated.core_elasticsearch_specification_specToPureGenerator")
                .getMethod("Root_meta_external_store_elasticsearch_metamodel_spec_toPure_generatePureProtocolJson_String_1__String_1_", String.class, ExecutionSupport.class)
                .invoke(null, esVersions[0], classLoaderExecutionSupport);

        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        List<PackageableElement> elements = objectMapper.readValue(
                pureElementsJson,
                new TypeReference<List<PackageableElement>>()
                {
                }
        );

        PureModelContextData.Builder builder = PureModelContextData
                .newBuilder()
                .withSerializer(new Protocol("pure", "vX_X_X"));

        builder.withElements(elements);

        String pureCode = PureGrammarComposer.newInstance(
                PureGrammarComposerContext.Builder.newInstance()
                        .withRenderStyle(RenderStyle.PRETTY)
                        .build()
        ).renderPureModelContextData(builder.build());

        System.out.println(pureCode);
    }

    public static CompiledExecutionSupport getClassLoaderExecutionSupport()
    {
        RichIterable<CodeRepository> repos = CodeRepositoryProviderHelper.findCodeRepositories();
        MutableList<CodeRepository> codeRepos = Lists.mutable.of(CodeRepository.newPlatformCodeRepository()).withAll(repos);
        ClassLoader classLoader = SpecToCodeGenerator.class.getClassLoader();
        return new CompiledExecutionSupport(new JavaCompilerState(null, classLoader), new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader, repos.collect(CodeRepository::getName)), Sets.mutable.empty()), null, new PureCodeStorage(null, new ClassLoaderCodeStorage(classLoader, codeRepos)), null, null, new ConsoleCompiled(), new FunctionCache(), new ClassCache(classLoader), null, Sets.mutable.empty());
    }
}
