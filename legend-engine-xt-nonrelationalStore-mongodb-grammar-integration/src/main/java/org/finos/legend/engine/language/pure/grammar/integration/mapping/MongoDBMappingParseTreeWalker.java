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

package org.finos.legend.engine.language.pure.grammar.integration.mapping;

import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MongoDBMappingParserGrammar;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.Collection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;

public class MongoDBMappingParseTreeWalker
{
    public MongoDBMappingParseTreeWalker()
    {
    }


    public void visitMongoDBClassMapping(MongoDBMappingParserGrammar.ClassMappingContext ctx, RootMongoDBClassMapping classMapping)
    {
        if (ctx.mappingFilter() != null)
        {
            //mappingFilter not currently needed
        }
        if (ctx.mappingBinding().size() == 1)
        {
            classMapping.bindingPath = ctx.mappingBinding().get(0).qualifiedName().getText();
        }
        if (ctx.mappingMainCollection().size() == 1)
        {
            classMapping.mainCollectionName = ctx.mappingMainCollection().get(0).mappingScopeInfo().identifier().getText();
            classMapping.storePath = ctx.mappingMainCollection().get(0).databasePointer().qualifiedName().getText();
        }
    }

}
