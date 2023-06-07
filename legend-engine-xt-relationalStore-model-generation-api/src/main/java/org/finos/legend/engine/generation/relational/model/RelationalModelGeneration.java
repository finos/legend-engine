//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.generation.relational.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.core_relational_relational_transform_toPure_relationalToPure;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;

public class RelationalModelGeneration
{
    private final Pair<PureModelContextData, PureModel> graph;
    private final String database;
    private final String schema;
    private final String modelPackage;
    private final String mappingPackage;
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();


    public RelationalModelGeneration(Pair<PureModelContextData, PureModel> graph, String database, String schema, String modelPackage, String mappingPackage)
    {
        this.graph = graph;
        this.database = database;
        this.schema = schema;
        this.modelPackage = modelPackage;
        this.mappingPackage = mappingPackage;
    }


    public PureModelContextData build()
    {
        if (this.database == null)
        {
            throw new EngineException("database required to build relational mapping");
        }

        PureModel pureModel = this.graph.getTwo();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement packageableElement = pureModel.getPackageableElement(this.database);
        if (!(packageableElement instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database))
        {
            throw new EngineException("Database '" + this.database + "' not found");
        }
        org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database db = (org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database) packageableElement;
        Schema _schema = db._schemas().detect(schema -> this.schema.equals(schema.getName()));
        Assert.assertTrue(_schema != null, () -> "Can't find schema '" + this.schema + "' in database '" + db.getName() + "'");

        String graphText = core_relational_relational_transform_toPure_relationalToPure
                .Root_meta_relational_transform_toPure_generateModelAndMappingFromRelationalStore_Schema_1__String_1__String_1__String_1_(_schema, this.modelPackage, this.mappingPackage, pureModel.getExecutionSupport());
        return buildFromString(graphText);
    }

    private PureModelContextData buildFromString(String text)
    {
        return PureGrammarParser.newInstance().parseModel(text, "", 0, 0, false);
    }

}
