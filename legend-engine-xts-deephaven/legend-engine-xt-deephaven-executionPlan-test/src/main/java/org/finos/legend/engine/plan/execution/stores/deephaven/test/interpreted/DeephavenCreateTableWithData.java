//// Copyright 2026 Goldman Sachs
////
//// Licensed under the Apache License, Version 2.0 (the "License");
//// you may not use this file except in compliance with the License.
//// You may obtain a copy of the License at
////
////      http://www.apache.org/licenses/LICENSE-2.0
////
//// Unless required by applicable law or agreed to in writing, software
//// distributed under the License is distributed on an "AS IS" BASIS,
//// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//// See the License for the specific language governing permissions and
//// limitations under the License.
////
//
//package org.finos.legend.engine.plan.execution.stores.deephaven.test.interpreted;
//
//import io.deephaven.client.impl.Session;
//import io.deephaven.client.impl.TableHandle;
//import io.deephaven.qst.TableCreator;
//import io.deephaven.qst.column.Column;
//import io.deephaven.qst.table.NewTable;
//import org.eclipse.collections.api.list.ListIterable;
//import org.eclipse.collections.api.map.MutableMap;
//import org.eclipse.collections.api.stack.MutableStack;
//import org.finos.legend.pure.m3.compiler.Context;
//import org.finos.legend.pure.m3.exception.PureExecutionException;
//import org.finos.legend.pure.m3.navigation.ProcessorSupport;
//import org.finos.legend.pure.m4.ModelRepository;
//import org.finos.legend.pure.m4.coreinstance.CoreInstance;
//import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
//import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
//import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
//import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
//import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
//import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Stack;
//import java.util.concurrent.ExecutionException;
//
//public class DeephavenCreateTableWithData extends NativeFunction
//{
//    private final FunctionExecutionInterpreted functionExecution;
//    private final ModelRepository repository;
//
//    public DeephavenCreateTableWithData(FunctionExecutionInterpreted functionExecution, ModelRepository modelRepository)
//    {
//        this.functionExecution = functionExecution;
//        this.repository = modelRepository;
//    }
//
//    @Override
//    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters,
//                                Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext,
//                                MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler,
//                                InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context,
//                                ProcessorSupport processorSupport) throws PureExecutionException
//    {
//        CsvToNewTable converter = new CsvToNewTable();
//        Session session = null; // How to get Deephaven session?
//        List<Column<?>> columns = new ArrayList<>();
//        Column<Integer> column = Column.ofInt("myColumnName", new int[] {1, 2, 3});
//        TableCreator<TableHandle> tableCreator = session.batch();
//        NewTable table = NewTable.of(columns);
//        TableHandle tableHandle = tableCreator.of(table);
//        try
//        {
//            session.publish("myColumnName", tableHandle).get();
//        }
//        catch (InterruptedException e)
//        {
//            throw new RuntimeException(e);
//        }
//        catch (ExecutionException e)
//        {
//            throw new RuntimeException(e);
//        }
//    }
//}
