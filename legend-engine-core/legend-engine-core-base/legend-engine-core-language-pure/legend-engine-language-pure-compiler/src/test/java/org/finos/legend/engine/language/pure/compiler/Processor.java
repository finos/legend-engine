//  Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.partition.list.PartitionMutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.core_pure_serialization_toPureGrammar;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Scanner;

public class Processor
{
    @Test
    public void test() throws Exception
    {
        // Validate latest
        File grammarTests = new File(Processor.class.getClassLoader().getResource("122390239DF2390").toURI());
        //validate(grammarTests);

        // Generate for old protocol
        //generateOldProtocolFromGrammarTests(grammarTests, "v1_23_0");
        generateOldProtocolFromGrammarTests(grammarTests, "vX_X_X");

        // Validate old protocols
        validate(new File(Processor.class.getClassLoader().getResource("vX_X_X").toURI()));
    }

    private void validate(File latestHashFolder)
    {
        processRecursive(latestHashFolder, a -> validateJsonFromGrammar(a, latestHashFolder));
    }

    private void generateOldProtocolFromGrammarTests(File latestHashFolder, String version)
    {
        processRecursive(
                latestHashFolder,
                files -> generateSerializationTest(
                        version,
                        files,
                        latestHashFolder,
                        new File(latestHashFolder.getParentFile().getParentFile().getParentFile(), "src/test/resources")
                )
        );
    }

    private void processRecursive(File latestHashFolder, Procedure<MutableList<File>> filesProcessor)
    {
        File[] children = latestHashFolder.listFiles();
        MutableList<File> files = children == null ? Lists.mutable.empty() : Lists.mutable.with(children);
        PartitionMutableList<File> partition = files.partition(File::isDirectory);
        filesProcessor.accept(partition.getRejected());
        partition.getSelected().forEach(f -> processRecursive(f, filesProcessor));
    }

    private void validateJsonFromGrammar(MutableList<File> files, File baseFolder)
    {
        try
        {
            String grammar = read(files, "grammar.pure");
            String grammarCompare = read(files, "grammar_compare.pure");
            String protocol = read(files, "protocol.json");
            PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.STANDARD).build());
            if (grammar != null && protocol != null)
            {
                ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
                //System.out.println(files.get(0).getPath());
//                System.out.println(grammar);
//                System.out.println(mapper.writeValueAsString(PureGrammarParser.newInstance().parseModel(grammar)));
                PureModelContextData pureModelContextData = mapper.readValue(protocol, PureModelContextData.class);
                String rendered = grammarTransformer.renderPureModelContextData(pureModelContextData, x -> x._package + "::" + x.name);
                Assert.assertEquals(grammarCompare == null ? grammar : grammarCompare, rendered);
            }
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void generateSerializationTest(String version, MutableList<File> files, File baseFolder, File targetFolder)
    {
        File file = files.detect(x -> x.getName().equals("grammar.pure"));
        if (file != null)
        {
            generateJSONFromProtocol(version, file, targetFolder, (file.getParentFile().getPath().substring(baseFolder.getPath().length())));
        }
    }

    public void generateJSONFromProtocol(String version, File pureFile, File targetFolder, String path)
    {
        try
        {
            File targetSubFolder = new File(targetFolder, version + "/" + path);
            String txt = readFile(pureFile);
            PureModelContextData pureModelContextData = PureGrammarParser.newInstance().parseModel(txt);
            PureModel pureModel = Compiler.compile(pureModelContextData, DeploymentMode.PROD, "me");

            String printed = core_pure_serialization_toPureGrammar.Root_meta_pure_metamodel_serialization_grammar_printAllElementsFromPackageStr_String_1__String_1_("protocol", pureModel.getExecutionSupport());
            writeFile(new File(targetSubFolder, "grammar.pure"), printed);
            //PureGrammarComposer grammarTransformer = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().withRenderStyle(RenderStyle.STANDARD).build());
            //ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

            if (PureClientVersions.versionAGreaterThanOrEqualsVersionB(version, "v1_23_0"))
            {
                Class cl = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + version + "_scan_buildBasePureModel");
                Method method = cl.getMethod("Root_meta_protocols_pure_" + version + "_transformation_fromPureGraph_buildBasePureModelFromAllElementsInPackageStr_String_1__Extension_MANY__String_1_", String.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
                String protocol = (String) method.invoke(null, "protocol", Lists.mutable.empty(), pureModel.getExecutionSupport());

                // Reformat ----
                ObjectMapper mapper = new ObjectMapper();
                Object json = mapper.readValue(protocol, Object.class);
                String formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                // Reformat ----

                writeFile(new File(targetSubFolder, "protocol.json"), formatted);

                // System.out.println(protocol);
                //PureModelContextData pmcd = mapper.readValue(protocol, PureModelContextData.class);
                // System.out.println(grammarTransformer.renderPureModelContextData(pmcd));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println("ERROR! " + pureFile);
        }
    }

    private static String read(MutableList<File> files, String name)
    {
        File file = files.detect(x -> x.getName().equals(name));
        return file == null ? null : readFile(file);
    }

    private static String readFile(File child)
    {
        try (Scanner s = new Scanner(new FileReader(child)).useDelimiter("\\A"))
        {
            return s.next();
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeFile(File file, String text)
    {
        try
        {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        try (FileWriter fw = new FileWriter(file))
        {
            fw.write(text);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
