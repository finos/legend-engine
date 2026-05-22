// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit.junit.extensions;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.extension.GenerationMode;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationConfigurationDescription;
import org.finos.legend.engine.external.shared.format.generations.description.GenerationProperty;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput_Impl;

import java.util.Collections;
import java.util.List;

/**
 * Test-only {@link GenerationExtension} used by the EMIT JUnit integration
 * self-tests to exercise the per-spec "File Generation" task emitted by
 * {@link org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder}.
 * Matches FileGenerationSpecifications written as {@code EmitDemoFile} in the
 * grammar (the parser lowercases the first letter of the type identifier).
 */
public class EmitDemoFileGenerationExtension implements GenerationExtension
{
    public static final String KEY = "emitDemoFile";
    public static final String GREETING_PROPERTY = "greeting";
    public static final String DEFAULT_GREETING = "hello";

    @Override
    public String getLabel()
    {
        return "EMIT Demo File";
    }

    @Override
    public String getKey()
    {
        return KEY;
    }

    @Override
    public GenerationMode getMode()
    {
        return GenerationMode.Schema;
    }

    @Override
    public GenerationConfigurationDescription getGenerationDescription()
    {
        return new GenerationConfigurationDescription()
        {
            @Override
            public String getKey()
            {
                return KEY;
            }

            @Override
            public List<GenerationProperty> getProperties(PureModel pureModel)
            {
                return Collections.emptyList();
            }
        };
    }

    @Override
    public Root_meta_pure_generation_metamodel_GenerationConfiguration defaultConfig(CompileContext context)
    {
        return new Root_meta_pure_generation_metamodel_GenerationConfiguration_Impl("EmitDemoFileGenerationConfig", null, null);
    }

    @Override
    public List<Root_meta_pure_generation_metamodel_GenerationOutput> generateFromElement(PackageableElement element, CompileContext compileContext)
    {
        if (!(element instanceof FileGenerationSpecification))
        {
            return null;
        }
        FileGenerationSpecification spec = (FileGenerationSpecification) element;
        String greeting = (spec.configurationProperties == null)
                          ? DEFAULT_GREETING
                          : ListIterate.detectOptional(spec.configurationProperties, p -> GREETING_PROPERTY.equals(p.name) && (p.value instanceof String))
                            .map(p -> (String) p.value)
                            .orElse(DEFAULT_GREETING);
        Root_meta_pure_generation_metamodel_GenerationOutput output = new Root_meta_pure_generation_metamodel_GenerationOutput_Impl("emit-demo-output", null, null);
        output._fileName(spec.getPath() + ".txt");
        output._content(greeting + " from " + spec.getPath());
        output._format("text/plain");
        return Lists.fixedSize.with(output);
    }
}