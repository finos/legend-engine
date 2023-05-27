// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration.util;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.visitors.MongoDBOperationElementVisitorImpl;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Validator;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.aggregation.ArgumentExpression;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class MongoDBSchemaComposer
{

    public static String renderMongoDBStore(MongoDatabase monogDBStore)
    {
        StringBuilder builder = new StringBuilder();
        int baseIndent = 1;
        builder.append("Database ").append(PureGrammarComposerUtility.convertPath(monogDBStore.getPath())).append("\n(\n");
        monogDBStore.collections.stream().forEach(c -> MongoDBSchemaComposer.renderCollection(c, builder, baseIndent));
        builder.append(")");
        return builder.toString();
    }

    private static void renderCollection(Collection c, StringBuilder builder, int indent)
    {
        builder.append(getTabString(indent)).append("Collection ").append(c.name).append("\n");
        builder.append(getTabString(indent)).append("(\n");
        renderValidatorProperties(c.validator, builder, indent + 1);
        builder.append(getTabString(indent)).append(")\n");
    }

    private static void renderValidatorProperties(Validator v, StringBuilder builder, int indent)
    {
        builder.append(getTabString(indent)).append("validationLevel: ").append(v.validationLevel.name()).append(";\n");
        builder.append(getTabString(indent)).append("validationAction: ").append(v.validationAction.name()).append(";\n");
        renderSchemaExpression(v.validatorExpression, builder, indent);
    }

    private static void renderSchemaExpression(ArgumentExpression argumentExpression, StringBuilder builder, int indent)
    {
        builder.append(getTabString(indent)).append("jsonSchema: ");
        String jsonSchemaString = argumentExpression.accept(new MongoDBOperationElementVisitorImpl(indent)).trim();
        builder.append(jsonSchemaString);
        builder.append(";\n");
    }
}
