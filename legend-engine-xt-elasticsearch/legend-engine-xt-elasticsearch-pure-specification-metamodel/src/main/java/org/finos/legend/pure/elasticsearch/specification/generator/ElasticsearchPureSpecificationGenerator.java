//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.pure.elasticsearch.specification.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
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

public class ElasticsearchPureSpecificationGenerator
{
    private final Path specPath;
    private final String pkgPrefix;
    private final RichIterable<String> apisToGenerate;
    private final Path generationOutput;

    public ElasticsearchPureSpecificationGenerator(Path specPath, String pkgPrefix, RichIterable<String> apisToGenerate, Path generationOutput)
    {
        this.specPath = specPath;
        this.pkgPrefix = pkgPrefix;
        this.apisToGenerate = apisToGenerate;
        this.generationOutput = generationOutput;
    }

    public void generate() throws Exception
    {
        String specAJson = String.join("", Files.readAllLines(this.specPath));

        CompiledExecutionSupport compileSupport = getCompileSupport();

        Class<?> generatorClass = Class.forName("org.finos.legend.pure.generated.core_elasticsearch_specification_metamodel_specification_generator");
        Method generatorMethod = generatorClass.getMethod("Root_meta_external_store_elasticsearch_specification_metamodel_generatePureCode_String_1__String_1__String_MANY__Boolean_1__String_1_", String.class, String.class, RichIterable.class, boolean.class, ExecutionSupport.class);
        String pmcdJson = (String) generatorMethod.invoke(null, specAJson, this.pkgPrefix, apisToGenerate, false, compileSupport);

        PureModelContextData pmcd = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(pmcdJson, PureModelContextData.class);
        PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
        String code = grammarTransformer.renderPureModelContextData(pmcd);

        System.out.println(code);

        try (InputStream is = ElasticsearchPureSpecificationGenerator.class.getClassLoader().getResourceAsStream("pure_header.txt");
             PrintWriter writer = new PrintWriter(Files.newBufferedWriter(this.generationOutput, StandardCharsets.UTF_8,  StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))
        )
        {
            for (String headerLine : IOUtils.readLines(is, StandardCharsets.UTF_8))
            {
                writer.println(headerLine);
            }

            writer.println();
            writer.println("// -------------------------------------------------");
            writer.println(String.format("// This was autogenerated using %s(schema=%s, apis=%s) @ %s", ElasticsearchPureSpecificationGenerator.class.getSimpleName(), this.specPath.getFileName(), this.apisToGenerate.makeString(), Instant.now().toString()));
            writer.println("// -------------------------------------------------");
            writer.println();
            writer.printf("Profile %s::ESProfile", this.pkgPrefix);
            writer.println("{");
            writer.println("\tstereotypes: [ContainerProperty, TaggedUnion, ContainerVariant, AdditionalProperty];");
            writer.println("\ttags: [esQuirk, docURL, specLocation, since, stability, enumName];");
            writer.println("}");
            writer.println();
            writer.printf("Class %s::DictionaryEntrySingleValue<K, V>", this.pkgPrefix);
            writer.println("{");
            writer.println("\tkey: K[1];");
            writer.println("\tvalue: V[0..1];");
            writer.println("}");
            writer.println();
            writer.printf("Class %s::DictionaryEntryMultiValue<K, V>", this.pkgPrefix);
            writer.println("{");
            writer.println("\tkey: K[1];");
            writer.println("\tvalue: V[*];");
            writer.println("}");
            writer.println();
            writer.print(code);
        }
    }

    private static CompiledExecutionSupport getCompileSupport()
    {
        MutableList<CodeRepository> codeRepositories = CodeRepositoryProviderHelper.findCodeRepositories().toList();
        ClassLoader classLoader = ElasticsearchPureSpecificationGenerator.class.getClassLoader();
        return new CompiledExecutionSupport(
                new JavaCompilerState(null, classLoader),
                new CompiledProcessorSupport(classLoader, MetadataLazy.fromClassLoader(classLoader, codeRepositories.collect(CodeRepository::getName)), Sets.mutable.empty()),
                null,
                new PureCodeStorage(null, new ClassLoaderCodeStorage(classLoader, codeRepositories.with(CodeRepository.newPlatformCodeRepository()))),
                null,
                null,
                new ConsoleCompiled(),
                new FunctionCache(),
                new ClassCache(),
                null,
                Sets.mutable.empty()
        );
    }

    public static void main(String... args) throws Exception
    {
        Path specPath = Paths.get(args[0]);
        String pkgPrefix = args[1];
        RichIterable<String> apisToGenerate = Lists.fixedSize.of(args[2].split(","));
        Path generationOutput = Paths.get(args[3]);
        new ElasticsearchPureSpecificationGenerator(specPath, pkgPrefix, apisToGenerate, generationOutput).generate();
    }
}
