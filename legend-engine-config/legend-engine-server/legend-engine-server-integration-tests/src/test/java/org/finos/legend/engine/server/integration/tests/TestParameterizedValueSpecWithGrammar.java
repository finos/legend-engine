/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.engine.server.integration.tests;

import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.plan.execution.parameterization.ParameterizedValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.junit.Assert;
import org.junit.Test;

public class TestParameterizedValueSpecWithGrammar
{
    @Test
    public void testParameterizedSpecWithGrammar()
    {
        DomainParser parser = new DomainParser();
        Lambda lambda = parser.parseLambda("{test:String[1]|example::Person.all()->filter(f|$f.name=='ABC' || $f.id==1 ||$f.age->in([1,2,3])  && $f.foo==$test )}", "id", 0, 0, false);

        ValueSpecification spec = new ParameterizedValueSpecification(lambda, "GENERATED").getValueSpecification();
        String actualLambda = spec.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.STANDARD).build());
        Assert.assertEquals("test: String[1]|example::Person.all()->filter(f|(($f.name == $GENERATEDL0L1L0L0L0L1: String[1]) || ($f.id == $GENERATEDL0L1L0L0L1L1: Integer[1])) || ($f.age->in([1, 2, 3]) && ($f.foo == $test)))", actualLambda);

    }
}
