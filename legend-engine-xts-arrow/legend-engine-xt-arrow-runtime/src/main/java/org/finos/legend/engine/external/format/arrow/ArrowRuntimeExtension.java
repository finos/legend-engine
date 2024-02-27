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


package org.finos.legend.engine.external.format.arrow;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.shared.runtime.ExternalFormatRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeTDSExecutionNode;
import org.pac4j.core.profile.CommonProfile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;


public class ArrowRuntimeExtension implements ExternalFormatRuntimeExtension
{
    private static final String CONTENT_TYPE = "application/x.arrow";

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(CONTENT_TYPE);
    }

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("External_Format", "Arrow");
    }

    @Override
    public Result executeExternalizeTDSExecutionNode(ExternalFormatExternalizeTDSExecutionNode node, Result result, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        try
        {
            if (result instanceof RelationalResult)
            {
                return streamArrowFromRelational((RelationalResult) result);
            }
            else
            {
                throw new RuntimeException("Arrow external format only supported on relational execution");

            }

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Result streamArrowFromRelational(RelationalResult relationalResult) throws SQLException, IOException
    {

        return new ExternalFormatSerializeResult(new ArrowDataWriter(relationalResult), relationalResult, CONTENT_TYPE);

    }


}