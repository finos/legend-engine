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

package org.finos.legend.engine.language.pure.grammar.integration;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.authentication.grammar.to.IAuthenticationGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBMappingComposer;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBSchemaComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDatabase;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.runtime.MongoDBURL;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class MongoDBGrammarComposerExtension implements IMongoDBGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof MongoDatabase)
        {
            return MongoDBSchemaComposer.renderMongoDBStore((MongoDatabase) element);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(MongoDBGrammarParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            MutableList<MongoDatabase> composableElements = ListIterate.selectInstancesOf(elements, MongoDatabase.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(composableElements.collect(MongoDBSchemaComposer::renderMongoDBStore).makeString("###" + MongoDBGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }


    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof MongoDBConnection)
            {
                MongoDBConnection mongoDBConnection = (MongoDBConnection) connectionValue;

                return Tuples.pair(MongoDBGrammarParserExtension.MONGO_DB_CONNECTION_TYPE,
                        context.getIndentationString() + "{\n" +
                                context.getIndentationString() + getTabString() + "database: " + mongoDBConnection.dataSourceSpecification.databaseName + ";\n" +
                                context.getIndentationString() + getTabString() + "store: " + mongoDBConnection.element + ";\n" +
                                context.getIndentationString() + getTabString() + "serverURLs: [" + this.getMongoDBURLs(mongoDBConnection.dataSourceSpecification.serverURLs) + "];\n" +
                                context.getIndentationString() + getTabString() + "authentication: " + IAuthenticationGrammarComposerExtension.renderAuthentication(mongoDBConnection.authenticationSpecification, 1, context) + ";\n" +
                                context.getIndentationString() + "}");
            }
            return null;
        });
    }

    private String getMongoDBURLs(List<MongoDBURL> serverURLs)
    {
        return serverURLs.stream().map(i -> i.baseUrl + ":" + i.port).collect(Collectors.joining(", "));
    }

    @Override
    public List<Function2<ClassMapping, PureGrammarComposerContext, String>> getExtraClassMappingComposers()
    {
        return Lists.mutable.with((classMapping, context) ->
        {
            if (classMapping instanceof RootMongoDBClassMapping)
            {
                RootMongoDBClassMapping rootMongoDBClassMapping = (RootMongoDBClassMapping) classMapping;
                StringBuilder builder = new StringBuilder();
                builder.append(": ").append(MongoDBGrammarParserExtension.MONGO_DB_MAPPING_ELEMENT_TYPE).append("\n");
                builder.append(getTabString()).append("{\n");
                String mapping = MongoDBMappingComposer.renderRootMongoDBClassMapping(rootMongoDBClassMapping);
                builder.append(mapping);
                builder.append(getTabString()).append("}");
                return builder.toString();
            }
            return null;
        });
    }
}
