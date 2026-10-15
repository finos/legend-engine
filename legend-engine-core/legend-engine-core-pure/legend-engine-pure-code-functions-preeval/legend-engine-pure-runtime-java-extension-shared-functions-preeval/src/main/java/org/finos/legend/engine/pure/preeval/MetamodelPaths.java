// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

public final class MetamodelPaths
{
    public static final String LET_FUNCTION = "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_";
    public static final String CAST_FUNCTION = "meta::pure::functions::lang::cast_Any_m__T_1__T_m_";
    public static final String IF_FUNCTION = "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_";
    public static final String AND_FUNCTION = "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_";
    public static final String OR_FUNCTION = "meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_";
    public static final String EVAL_FUNCTION = "meta::pure::functions::lang::eval_Function_1__V_m_";
    public static final ImmutableList<String> EVAL_FUNCTIONS = Lists.immutable.with(
            "meta::pure::functions::lang::eval_Function_1__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__U_p__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__U_p__W_q__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__U_p__W_q__X_r__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__U_p__W_q__X_r__Y_s__V_m_",
            "meta::pure::functions::lang::eval_Function_1__T_n__U_p__W_q__X_r__Y_s__Z_t__V_m_",
            "meta::pure::functions::lang::eval_Function_1__S_n__T_o__U_p__W_q__X_r__Y_s__Z_t__V_m_");
    public static final ImmutableList<String> MAP_FUNCTIONS = Lists.immutable.with(
            "meta::pure::functions::collection::map_T_m__Function_1__V_m_",
            "meta::pure::functions::collection::map_T_$0_1$__Function_1__V_$0_1$_",
            "meta::pure::functions::collection::map_T_MANY__Function_1__V_MANY_");
    public static final String FOLD_FUNCTION = "meta::pure::functions::collection::fold_T_MANY__Function_1__V_m__V_m_";
    public static final String CONCATENATE_FUNCTION = "meta::pure::functions::collection::concatenate_T_MANY__T_MANY__T_MANY_";
    public static final String FILTER_FUNCTION = "meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_";
    public static final String TO_ONE_FUNCTION = "meta::pure::functions::multiplicity::toOne_T_MANY__T_1_";
    public static final String TO_ONE_MANY_FUNCTION = "meta::pure::functions::multiplicity::toOneMany_T_MANY__T_$1_MANY$_";
    public static final String GENERIC_TYPE_FUNCTION = "meta::pure::functions::meta::genericType_Any_MANY__GenericType_1_";
    public static final String NIL = "meta::pure::metamodel::type::Nil";
    public static final String TDS_ROW = "meta::pure::tds::TDSRow";
    public static final String TABULAR_DATA_SET = "meta::pure::tds::TabularDataSet";
    public static final String RELATION_TYPE = "meta::pure::metamodel::relation::RelationType";
    public static final String COLUMN = "meta::pure::metamodel::relation::Column";
    public static final String PATH = "meta::pure::metamodel::path::Path";
    public static final String FUNCTION_TYPE_PROFILE = "meta::pure::profiles::functionType";
    public static final String TDS = "meta::pure::metamodel::relation::TDS";
    public static final String COLUMN_SPECIFICATION = "meta::pure::tds::ColumnSpecification";
    public static final String BASIC_COLUMN_SPECIFICATION = "meta::pure::tds::BasicColumnSpecification";
    public static final String TDS_AGGREGATE_VALUE = "meta::pure::tds::AggregateValue";
    public static final String COLLECTION_AGGREGATE_VALUE = "meta::pure::functions::collection::AggregateValue";
    public static final String AGG_COL_SPEC = "meta::pure::metamodel::relation::AggColSpec";
    public static final String AGG_COL_SPEC_ARRAY = "meta::pure::metamodel::relation::AggColSpecArray";
    public static final String FUNC_COL_SPEC = "meta::pure::metamodel::relation::FuncColSpec";
    public static final String SCHEMA_STATE = "meta::pure::tds::schema::SchemaState";
    public static final String ROOT_GRAPH_FETCH_TREE = "meta::pure::graphFetch::RootGraphFetchTree";
    public static final String BINDING = "meta::external::format::shared::binding::Binding";
    public static final String STORE = "meta::pure::store::Store";
    public static final String RELATION_STORE_ACCESSOR = "meta::pure::store::RelationStoreAccessor";
    public static final String TEST_PARAMETERS = "meta::pure::test::mft::TestParameters";
    public static final String RELATION_ELEMENT_ACCESSOR = "meta::pure::metamodel::relation::RelationElementAccessor";

    private MetamodelPaths()
    {
    }
}
