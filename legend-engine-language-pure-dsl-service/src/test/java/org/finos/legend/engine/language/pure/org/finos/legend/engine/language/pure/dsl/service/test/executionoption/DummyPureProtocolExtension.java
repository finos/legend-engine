package org.finos.legend.engine.language.pure.org.finos.legend.engine.language.pure.dsl.service.test.executionoption;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.executionOption.ExecutionOption;

import java.util.List;

public class DummyPureProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.Builder
                        .newInstance(ExecutionOption.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(DummyExecOption.class, "dummyExecOption"),
                                Tuples.pair(DummyExecOptionWithParameters.class, "dummyExecOptionWithParam")
                        )).build()
        ));
    }
}
