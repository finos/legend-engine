package org.finos.legend.engine.external.format.json;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json.JsonDeserializeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json.JsonPathReference;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.format.json.JsonSerializeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.external.shared.PathReference;

import java.util.List;

public class JsonProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.Builder.newInstance(ExecutionNode.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(JsonSerializeExecutionNode.class, "jsonSerialize"),
                                Tuples.pair(JsonDeserializeExecutionNode.class, "jsonDeserialize")
                        )).build(),
                ProtocolSubTypeInfo.Builder.newInstance(PathReference.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(JsonPathReference.class, "json")
                        )).build()
        ));
    }
}
