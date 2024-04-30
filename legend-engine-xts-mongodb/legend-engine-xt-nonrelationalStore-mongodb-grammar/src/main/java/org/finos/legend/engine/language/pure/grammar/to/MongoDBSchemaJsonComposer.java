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

package org.finos.legend.engine.language.pure.grammar.to;

import org.finos.legend.engine.language.pure.grammar.to.visitors.MongoDBOperationElementVisitorImpl;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.MongoDBOperationElement;

import java.util.List;
import java.util.stream.Collectors;


public class MongoDBSchemaJsonComposer
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MongoDBSchemaJsonComposer.class);

    private MongoDBSchemaJsonComposer()
    {
    }

    public static MongoDBSchemaJsonComposer newInstance()
    {
        return new MongoDBSchemaJsonComposer();
    }

    public String renderMongoDatabase(MongoDatabase database)
    {
        int baseIndentation = 1;

        return "";
    }

    public String renderDocument(MongoDatabase database)
    {
        return this.visit(database);
    }

    private String visit(MongoDatabase database)
    {
        int dbNodeTabIndex = 1;
        StringBuilder builder = new StringBuilder();

        builder.append("{").append("\n");
        ComposerUtility.appendJsonKey(builder, "_type");
        ComposerUtility.appendStringWithQuotes(builder, "MongoDatabase");
        builder.append(",\n");


        ComposerUtility.appendTabString(builder, dbNodeTabIndex);
        ComposerUtility.appendStringWithQuotes(builder, "database");
        builder.append(": {\n");

        int dbKeysTabIndex = dbNodeTabIndex + 1;
        ComposerUtility.appendTabString(builder, dbKeysTabIndex);
        ComposerUtility.appendStringWithQuotes(builder, "databaseName");
        builder.append(": ");
        ComposerUtility.appendStringWithQuotes(builder, database.name);
        builder.append(",\n");

        ComposerUtility.appendTabString(builder, dbKeysTabIndex);
        ComposerUtility.appendStringWithQuotes(builder, "collections");
        builder.append(": [\n");
        builder.append(renderCollection(database.collections, dbKeysTabIndex));
        ComposerUtility.appendTabString(builder, dbKeysTabIndex);
        builder.append("]\n");

        ComposerUtility.appendTabString(builder, dbNodeTabIndex);
        builder.append("}\n");

        builder.append("}");
        return builder.toString();
    }

    private void visitSchemas(MongoDBOperationElement jsonSchemaExpr, StringBuilder builder, int tabIndex)
    {
        builder.append(jsonSchemaExpr.accept(new MongoDBOperationElementVisitorImpl(tabIndex)));
    }


    private String renderCollection(List<Collection> collections, int tabIndex)
    {
        StringBuilder builder = new StringBuilder();
        String collectionsString = collections.stream().map(col ->
        {
            int currentTab = tabIndex + 1;
            ComposerUtility.appendTabString(builder, currentTab);
            builder.append("{\n");
            int collectionValuesIndex = currentTab + 1;
            ComposerUtility.appendTabString(builder, collectionValuesIndex);
            ComposerUtility.appendJsonKey(builder, "options");
            builder.append("{\n");
            int optionValuesIndex = collectionValuesIndex + 1;
            ComposerUtility.appendTabString(builder, optionValuesIndex);
            ComposerUtility.appendJsonKey(builder, "validator");
            // Start the validator node
            builder.append("{\n");
            int validatorValuesIndex = optionValuesIndex + 1;
            ComposerUtility.appendTabString(builder, validatorValuesIndex);
            ComposerUtility.appendJsonKey(builder, "$jsonSchema");
            visitSchemas(col.validator.validatorExpression, builder, validatorValuesIndex);

            if (col.validator.validationLevel != null)
            {
                builder.append(",\n");
                ComposerUtility.appendTabString(builder, optionValuesIndex);
                ComposerUtility.appendJsonKey(builder, "validationLevel");
                ComposerUtility.appendStringWithQuotes(builder, col.validator.validationLevel.name());
            }

            if (col.validator.validationAction != null)
            {
                builder.append(",\n");
                ComposerUtility.appendTabString(builder, optionValuesIndex);
                ComposerUtility.appendJsonKey(builder, "validationAction");
                ComposerUtility.appendStringWithQuotes(builder, col.validator.validationAction.name());
            }
            builder.append("\n");
            ComposerUtility.appendTabString(builder, optionValuesIndex);
            // Close the validator node
            builder.append("}");
            builder.append("\n");
            ComposerUtility.appendTabString(builder, collectionValuesIndex);
            builder.append("},\n");

            ComposerUtility.appendTabString(builder, collectionValuesIndex);
            ComposerUtility.appendJsonKey(builder, "uuid");
            ComposerUtility.appendStringWithQuotes(builder, col.uuid);
            builder.append(",\n");

            ComposerUtility.appendTabString(builder, collectionValuesIndex);
            ComposerUtility.appendJsonKey(builder, "collectionName");
            ComposerUtility.appendStringWithQuotes(builder, col.name);
            builder.append(",\n");

            ComposerUtility.appendTabString(builder, collectionValuesIndex);
            ComposerUtility.appendJsonKey(builder, "type");
            ComposerUtility.appendStringWithQuotes(builder, "collection");
            builder.append("\n");
            ComposerUtility.appendTabString(builder, currentTab);
            builder.append("}");
            return builder.toString();
        }).collect(Collectors.joining(",\n", "", "\n"));
        return collectionsString;
    }


}
