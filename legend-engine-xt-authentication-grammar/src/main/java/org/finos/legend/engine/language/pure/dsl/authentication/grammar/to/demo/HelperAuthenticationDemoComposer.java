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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.demo;

import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.AuthenticationSpecificationComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.demo.AuthenticationDemo;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;

public class HelperAuthenticationDemoComposer
{
    private HelperAuthenticationDemoComposer()
    {
    }

    public static String renderAuthenticationDemo(AuthenticationDemo authenticationDemo, int indentLevel, PureGrammarComposerContext context)
    {
        return "AuthenticationDemo " + convertPath(authenticationDemo.getPath()) + "\n" +
                "{\n" +
                renderAuthenticationSpecification(authenticationDemo.authenticationSpecification, indentLevel, context) +
                "}";
    }

    private static String renderAuthenticationSpecification(AuthenticationSpecification authenticationSpecification, int indentLevel, PureGrammarComposerContext context)
    {
        return authenticationSpecification.accept(new AuthenticationSpecificationComposer(indentLevel, context));
    }
}