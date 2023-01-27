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

package org.finos.legend.engine.language.mongodb.schema.grammar.to;

import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDatabase;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendJsonKey;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendStringWithQuotes;
import static org.finos.legend.engine.language.mongodb.schema.grammar.to.ComposerUtility.appendTabString;

public class MongodbSchemaGrammarComposer
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private MongodbSchemaGrammarComposer()
    {
    }

    public static MongodbSchemaGrammarComposer newInstance()
    {
        return new MongodbSchemaGrammarComposer();
    }

    public String renderDocument(MongoDatabase database)
    {
        return this.visit(database);
    }

    /*
    {
    "database": {
    "databaseName": "my_database_1",
    "collections": [
    ],
    "schemas": [
    ]
  }
}
     */
    private String visit(MongoDatabase database)
    {
        int dbNodeTabIndex = 1;
        StringBuilder builder = new StringBuilder();
        builder.append("{").append("\n");
        appendTabString(builder, dbNodeTabIndex);
        appendStringWithQuotes("database", builder);
        builder.append(": {\n");
        int dbKeysTabIndex = dbNodeTabIndex + 1;
        appendTabString(builder, dbKeysTabIndex);
        appendStringWithQuotes("databaseName", builder);
        builder.append(": ");
        appendStringWithQuotes(database.name, builder);
        builder.append(",\n");
        appendTabString(builder, dbKeysTabIndex);
        appendStringWithQuotes("collections", builder);
        builder.append(": [\n");
        builder.append(renderCollection(database.collections, dbKeysTabIndex));
        appendTabString(builder, dbKeysTabIndex);
        builder.append("],\n");
        appendTabString(builder, dbKeysTabIndex);
        appendStringWithQuotes("schemas", builder);
        builder.append(": [\n");
        visitSchemas(database.collections, builder, dbKeysTabIndex);
        //removeLastChar(builder);
        builder.append("\n");
        appendTabString(builder, dbKeysTabIndex);
        builder.append("]\n");
        appendTabString(builder, dbNodeTabIndex);
        builder.append("}\n");
        builder.append("}");
        return builder.toString();
    }

    private void visitSchemas(List<Collection> collections, StringBuilder builder, int tabIndex)
    {
        collections.stream().forEach(col ->
        {
            int schemaNodeTabIndex = tabIndex + 1;
            builder.append(col.schema.accept(new BaseTypeVisitorImpl(schemaNodeTabIndex)));
        });
        //removeLastChar(builder);
    }


    private String renderCollection(List<Collection> collections, int tabIndex)
    {
        StringBuilder sb = new StringBuilder();
        String collectionsString = collections.stream().map(col ->
        {
            int currentTab = tabIndex + 1;
            appendTabString(sb, currentTab);
            sb.append("{\n");
            /*"options": {
          "validator": {
            "$ref": "https://github.com/finos/legend/employee.schema"
          },
          "validationLevel": "strict",
          "validationAction": "error"
        },*/
            int collectionValuesIndex = currentTab + 1;
            appendTabString(sb, collectionValuesIndex);
            appendJsonKey("options", sb);
            sb.append("{\n");
            int optionValuesIndex = collectionValuesIndex + 1;
            appendTabString(sb, optionValuesIndex);
            appendJsonKey("validator", sb);
            sb.append("{\n");
            int validatorValuesIndex = optionValuesIndex + 1;
            appendTabString(sb, validatorValuesIndex);
            appendJsonKey("$ref", sb);
            appendStringWithQuotes(col.schema.id, sb);
            sb.append("\n");
            appendTabString(sb, optionValuesIndex);
            sb.append("}");
            if (col.schema.validationLevel != null)
            {
                sb.append(",\n");
                appendTabString(sb, optionValuesIndex);
                appendJsonKey("validationLevel", sb);
                appendStringWithQuotes(col.schema.validationLevel.name(), sb);
            }

            if (col.schema.validationAction != null)
            {
                sb.append(",\n");
                appendTabString(sb, optionValuesIndex);
                appendJsonKey("validationAction", sb);
                appendStringWithQuotes(col.schema.validationAction.name(), sb);
            }
            sb.append("\n");
            appendTabString(sb, collectionValuesIndex);
            sb.append("},\n");

            appendTabString(sb, collectionValuesIndex);
            appendJsonKey("collectionName", sb);
            appendStringWithQuotes(col.name, sb);
            sb.append(",\n");
            appendTabString(sb, collectionValuesIndex);
            appendJsonKey("type", sb);
            appendStringWithQuotes("collection", sb);
            sb.append("\n");
            appendTabString(sb, currentTab);
            sb.append("}");
            return sb.toString();
        }).collect(Collectors.joining(",\n", "", "\n"));
        return collectionsString;
    }


}
