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

package org.finos.legend.engine.protocol.graphQL;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.graphQL.grammar.from.GraphQLGrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.graphQL.metamodel.Translator;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;

public class GenerateTestQueryBuilder
{
    public static void main(String[] args)
    {
        PureModel pureModel = new PureModel(PureModelContextData.newBuilder().build(), Lists.mutable.empty(), DeploymentMode.TEST);
        String introspection = "query MyQuery {\n" +
                "  firmByLegalName(legalName: \"ork\") {\n" +
                "    legalName\n" +
                "    employees {\n" +
                "      lastName\n" +
                "    }\n" +
                "  }\n" +
                "  employeeByLastName(lastName: \"yro\") {\n" +
                "    firstName\n" +
                "  }\n" +
                "}";

        GraphQLGrammarParser parser = GraphQLGrammarParser.newInstance();
        Document document = parser.parseDocument(introspection);
        String res = org.finos.legend.pure.generated.core_pure_protocol_generation_builder_generation.Root_meta_protocols_generation_builder_builderGeneration_Any_1__String_1_(new Translator().translate(document, pureModel), pureModel.getExecutionSupport());
        System.out.println(res);
    }
}
