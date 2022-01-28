// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.extensions.collection.generation;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.engine.external.shared.format.extension.GenerationExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatPlanGeneratorExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.plan.generation.extension.LegendPlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.protocol.pure.v1.PersistenceProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtensionLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

public class TestExtensions
{

    // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
    private static final ImmutableList<Class<? extends PureProtocolExtension>> EXPECTED_PROTOCOL_EXTENSIONS = Lists.mutable.<Class<? extends PureProtocolExtension>>empty()
            .with(org.finos.legend.engine.protocol.pure.v1.CorePureProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.DiagramProtocolExtension.class)
            .with(org.finos.legend.engine.external.shared.ExternalFormatProtocolExtension.class)
            .with(org.finos.legend.engine.external.format.flatdata.FlatDataProtocolExtension.class)
            .with(org.finos.legend.engine.external.format.json.JsonProtocolExtension.class)
            .with(org.finos.legend.engine.external.format.xml.XmlProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.GenerationProtocolExtension.class)
            .with(PersistenceProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.RelationalProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.ServiceProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.ServiceStoreProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.TextProtocolExtension.class)
            .with(org.finos.legend.engine.protocol.pure.v1.DataSpaceProtocolExtension.class)
            .toImmutable();

    // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
    private static final ImmutableList<Class<? extends GenerationExtension>> EXPECTED_GENERATION_EXTENSIONS = Lists.mutable.<Class<? extends GenerationExtension>>empty()
            .with(org.finos.legend.engine.external.format.protobuf.extension.ProtobufGenerationExtension.class)
            .with(org.finos.legend.engine.external.format.avro.extension.AvroGenerationExtension.class)
            .with(org.finos.legend.engine.external.format.jsonSchema.extension.JSONSchemaGenerationExtension.class)
            .with(org.finos.legend.engine.external.format.rosetta.extension.RosettaGenerationExtension.class)
            .with(org.finos.legend.engine.external.language.morphir.extension.MorphirGenerationExtension.class)
            .toImmutable();


    // DO NOT DELETE ITEMS FROM THIS LIST (except when replacing them with something equivalent)
    private static final ImmutableList<Class<? extends PureGrammarParserExtension>> EXPECTED_GRAMMAR_EXTENSIONS = Lists.mutable.<Class<? extends PureGrammarParserExtension>>empty()
            .with(org.finos.legend.engine.language.pure.grammar.from.CorePureGrammarParser.class)
            .with(org.finos.legend.engine.language.pure.grammar.from.RelationalGrammarParserExtension.class)
            .with(org.finos.legend.engine.language.pure.grammar.from.ServiceStoreGrammarParserExtension.class)
            .with(org.finos.legend.engine.language.pure.grammar.from.ExternalFormatGrammarParserExtension.class)
            .toImmutable();

    private static final ImmutableList<Class<? extends PlanGeneratorExtension>> EXPECTED_PLAN_GENERATOR_EXTENSIONS = Lists.mutable.<Class<? extends PlanGeneratorExtension>>empty()
            .with(LegendPlanGeneratorExtension.class)
            .with(ExternalFormatPlanGeneratorExtension.class)
            .toImmutable();

    private static final ImmutableList<Class<? extends ExternalFormatExtension>> EXPECTED_EXTERNAL_FORMAT_EXTENSIONS = Lists.mutable.<Class<? extends ExternalFormatExtension>>empty()
            .with(org.finos.legend.engine.external.format.flatdata.FlatDataExternalFormatExtension.class)
            .with(org.finos.legend.engine.external.format.json.JsonExternalFormatExtension.class)
            .with(org.finos.legend.engine.external.format.xsd.XsdExternalFormatExtension.class)
            .toImmutable();

    @Test
    public void testExpectedProtocolExtensionsArePresent()
    {
        assertHasExtensions(PureProtocolExtensionLoader.extensions(), org.eclipse.collections.api.factory.Sets.mutable.withAll(EXPECTED_PROTOCOL_EXTENSIONS), PureProtocolExtension.class, true);
    }

    @Test
    public void testExpectedGenerationExtension()
    {
        assertHasExtensions(org.eclipse.collections.api.factory.Sets.mutable.withAll(EXPECTED_GENERATION_EXTENSIONS), GenerationExtension.class, true);
    }

    @Test
    public void testExpectedGrammarExtensionsArePresent()
    {
        assertHasExtensions(org.eclipse.collections.api.factory.Sets.mutable.withAll(EXPECTED_GRAMMAR_EXTENSIONS), PureGrammarParserExtension.class, false);
    }

    @Test
    public void testPlanGeneratorExtensionArePresent()
    {
        assertHasExtensions(Sets.mutable.withAll(EXPECTED_PLAN_GENERATOR_EXTENSIONS), PlanGeneratorExtension.class, true);
    }

    @Test
    public void testExpectedExternalFormatExtensionsArePresent()
    {
        assertHasExtensions(org.eclipse.collections.api.factory.Sets.mutable.withAll(EXPECTED_EXTERNAL_FORMAT_EXTENSIONS), ExternalFormatExtension.class, true);
    }

    private <T> void assertHasExtensions(Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass, boolean failOnAdditional)
    {

        assertHasExtensions(Lists.mutable.withAll(ServiceLoader.load(extensionClass)), expectedExtensionClasses, extensionClass, failOnAdditional);
    }

    private <T> void assertHasExtensions(List<T> availableClasses, Iterable<? extends Class<? extends T>> expectedExtensionClasses, Class<T> extensionClass, boolean failOnAdditional)
    {
        MutableSet<Class<? extends T>> missingClasses = Sets.mutable.withAll(expectedExtensionClasses);
        MutableList<Class<?>> unexpectedClasses = Lists.mutable.empty();
        availableClasses.forEach(e ->
        {
            if (!missingClasses.remove(e.getClass()))
            {
                unexpectedClasses.add(e.getClass());
            }
        });
        Assert.assertEquals("Missing extensions for " + extensionClass.getName(), Collections.emptySet(), missingClasses);
        if (failOnAdditional)
        {
            Assert.assertEquals("Unexpected extensions for " + extensionClass.getName(), Collections.emptyList(), unexpectedClasses);
        }
    }

}
