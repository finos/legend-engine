// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.mongodb.compiler;

import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.ExecutionPlanJavaCompilerExtension;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.mongodb.result.MongoDBResult;
import org.finos.legend.engine.plan.execution.stores.mongodb.specifics.IMongoDocumentDeserializeExecutionNodeSpecifics;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilter;
import org.finos.legend.engine.shared.javaCompiler.ClassPathFilters;

import java.util.LinkedHashMap;
import java.util.Map;

public class MongoDBDocumentFormatJavaCompilerExtension implements ExecutionPlanJavaCompilerExtension
{
    static final Map<String, Class<?>> DEPENDENCIES = new LinkedHashMap<>();
    private static final String PURE_PACKAGE = "meta::external::store::mongodb::executionPlan::platformBinding::legendJava::";


    static
    {
        DEPENDENCIES.put("org.bson.Document", Document.class);
        DEPENDENCIES.put("com.mongodb.client.MongoCursor", MongoCursor.class);
        DEPENDENCIES.put("org.bson.conversions.Bson", Bson.class);
        DEPENDENCIES.put("org.finos.legend.engine.plan.execution.stores.mongodb.result.MongoDBResult", MongoDBResult.class);
        DEPENDENCIES.put("org.finos.legend.engine.plan.execution.result.Result", Result.class);
        DEPENDENCIES.put(PURE_PACKAGE + "_IMongoDocumentDeserializeExecutionNodeSpecifics", IMongoDocumentDeserializeExecutionNodeSpecifics.class);
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    @Override
    public ClassPathFilter getExtraClassPathFilter()
    {
        return ClassPathFilters.fromClasses(DEPENDENCIES.values());
    }
}
