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

package org.finos.legend.engine.language.mongodb.grammar.integration;

import org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaParser;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.function.Consumer;

public class MongoDBSchemaParseTreeWalker
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;


    public MongoDBSchemaParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation)
    {
        this.walkerSourceInformation = sourceInformation;
        this.elementConsumer = null;
        this.section = null;
    }

    public MongoDBSchemaParseTreeWalker(ParseTreeWalkerSourceInformation sourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.walkerSourceInformation = sourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(MongoDBSchemaParser.DefinitionContext rootContext)
    {
    }
}
