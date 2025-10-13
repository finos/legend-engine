// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.postgres;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.postgres.protocol.sql.serialization.SQLSerializer;
import org.junit.Assert;
import org.junit.Test;

public class TestRewriteQuery
{
    @Test
    public void testMeta1()
    {
        test("select round(extract(epoch from pg_postmaster_start_time() at time zone 'UTC')) as startup_time");
    }

    @Test
    public void testMeta2()
    {
        test("select N.oid::bigint as id,\n" +
                " datname as name,\n" +
                " D.description,\n" +
                " datistemplate as is_template,\n" +
                " datallowconn as allow_connections,\n" +
                " pg_catalog.pg_get_userbyid(N.datdba) as \"owner\"\n" +
                " from pg_catalog.pg_database N\n" +
                " left outer join pg_catalog.pg_shdescription D on N.oid = D.objoid\n" +
                " order by case when datname = pg_catalog.current_database() then -1::bigint else N.oid::bigint end");
    }

    @Test
    public void testMeta3()
    {
        test("select N.oid::bigint as id,\n" +
                " N.xmin as state_number,\n" +
                " nspname as name,\n" +
                " D.description,\n" +
                " pg_catalog.pg_get_userbyid(N.nspowner) as \"owner\"\n" +
                " from pg_catalog.pg_namespace N\n" +
                " left outer join pg_catalog.pg_description D on N.oid = D.objoid\n" +
                " order by case when nspname = pg_catalog.current_schema() then -1::bigint else N.oid::bigint end");
    }

    @Test
    public void testMeta4()
    {
        test("select typinput = 'pg_catalog.array_in'::regproc as is_array, typtype, typname, pg_type.oid from pg_catalog.pg_type left outer join (select ns.oid as nspoid, ns.nspname, r.r from pg_namespace as ns join (select s.r, (current_schemas(false))[s.r] as nspname from generate_series(1, array_upper(current_schemas(false), 1)) as s(r)) as r using (nspname)) as sp on sp.nspoid = typnamespace where pg_type.oid = $1 order by sp.r, pg_type.oid DESC");
    }

    @Test
    public void testMeta5()
    {
        test("select usesuper\n" +
                " from pg_user\n" +
                " where usename = current_user");
    }

    @Test
    public void testMeta6()
    {
        test("select T.oid as oid,\n" +
                " relnamespace as schemaId,\n" +
                " pg_catalog.translate(relkind, 'rmvpfS', 'rmvrfS') as kind\n" +
                " from pg_catalog.pg_class T\n" +
                " where relnamespace in ($1, $2)\n" +
                " and relkind in ('r', 'm', 'v', 'p', 'f', 'S')\n" +
                " union all\n" +
                " select T.oid,\n" +
                " T.typnamespace,\n" +
                " 'T' as kind\n" +
                " from pg_catalog.pg_type T\n" +
                " left outer join pg_catalog.pg_class C on T.typrelid = C.oid\n" +
                " where T.typnamespace in ($3, $4)\n" +
                " and (T.typtype in ('d', 'e') or\n" +
                " C.relkind = 'c'::\"char\" or\n" +
                " (T.typtype = 'b' and (T.typelem = 0 OR T.typcategory <> 'A')) or\n" +
                " T.typtype = 'p' and not T.typisdefined)\n" +
                " union all\n" +
                " select oid,\n" +
                " collnamespace,\n" +
                " 'C' as kind\n" +
                " from pg_catalog.pg_collation\n" +
                " where collnamespace in ($5, $6)\n" +
                " union all\n" +
                " select oid,\n" +
                " oprnamespace,\n" +
                " 'O' as kind\n" +
                " from pg_catalog.pg_operator\n" +
                " where oprnamespace in ($7, $8)\n" +
                " union all\n" +
                " select oid,\n" +
                " opcnamespace,\n" +
                " 'c' as kind\n" +
                " from pg_catalog.pg_opclass\n" +
                " where opcnamespace in ($9, $10)\n" +
                " union all\n" +
                " select oid,\n" +
                " opfnamespace,\n" +
                " 'F' as kind\n" +
                " from pg_catalog.pg_opfamily\n" +
                " where opfnamespace in ($11, $12)\n" +
                " union all\n" +
                " select oid,\n" +
                " pronamespace,\n" +
                " case when not proisagg then 'R'\n" +
                " else 'a'\n" +
                " end as kind\n" +
                " from pg_catalog.pg_proc\n" +
                " where pronamespace in ($13, $14)");
    }

    @Test
    public void testMeta7()
    {
        test("select L.transactionid::varchar::bigint as transaction_id\n" +
                " from pg_catalog.pg_locks L\n" +
                " where L.transactionid is not null\n" +
                " order by pg_catalog.age(L.transactionid) desc\n" +
                " limit 1");
    }

    @Test
    public void testMeta8()
    {
        test("select case\n" +
                " when pg_catalog.pg_is_in_recovery()\n" +
                " then null\n" +
                " else\n" +
                " (pg_catalog.txid_current() % 4294967296)::varchar::bigint\n" +
                " end as current_txid");
    }

    @Test
    public void testMeta9()
    {
        test("show DateStyle");
    }

    @Test
    public void testMeta10()
    {
        test("select member id, roleid role_id, admin_option\n" +
                " from pg_catalog.pg_auth_members order by id, roleid::text");
    }

    @Test
    public void testMeta11()
    {
        test("select e.oid, n.nspname = any(current_schemas(true)), n.nspname, e.typname from pg_catalog.pg_type t join pg_catalog.pg_type e on t.typelem = e.oid join pg_catalog.pg_namespace n on t.typnamespace = n.oid where t.oid = $1");
    }

    @Test
    public void testMeta12()
    {
        test("select A.oid as access_method_id,\n" +
                " A.xmin as state_number,\n" +
                " A.amname as access_method_name\n" +
                ",\n" +
                " A.amhandler::oid as handler_id,\n" +
                " pg_catalog.quote_ident(N.nspname) || '.' || pg_catalog.quote_ident(P.proname) as handler_name,\n" +
                " A.amtype as access_method_type\n" +
                " from pg_am A\n" +
                " join pg_proc P on A.amhandler::oid = P.oid\n" +
                " join pg_namespace N on P.pronamespace = N.oid\n"
        );
    }

    @Test
    public void testMeta13()
    {
        test("select E.oid as id,\n" +
                " E.xmin as state_number,\n" +
                " extname as name,\n" +
                " extversion as version,\n" +
                " extnamespace as schema_id,\n" +
                " nspname as schema_name\n" +
                ",\n" +
                " array (select unnest\n" +
                " from unnest(available_versions)\n" +
                " where unnest > extversion) as available_updates\n" +
                " from pg_catalog.pg_extension E\n" +
                " join pg_namespace N on E.extnamespace = N.oid\n" +
                " left outer join (select name, array_agg(version) as available_versions\n" +
                " from pg_available_extension_versions()\n" +
                " group by name) V on E.extname = V.name");
    }

    @Test
    public void testMeta14()
    {
        test("select T.relkind as table_kind,\n" +
                " T.relname as table_name,\n" +
                " T.oid as table_id,\n" +
                " T.xmin as table_state_number,\n" +
                " T.relhasoids as table_with_oids,\n" +
                " T.reltablespace as tablespace_id,\n" +
                " T.reloptions as options,\n" +
                " T.relpersistence as persistence,\n" +
                " (select pg_catalog.array_agg(inhparent::bigint order by inhseqno)::varchar from pg_catalog.pg_inherits where T.oid = inhrelid) as ancestors,\n" +
                " (select pg_catalog.array_agg(inhrelid::bigint order by inhrelid)::varchar from pg_catalog.pg_inherits where T.oid = inhparent) as successors,\n" +
                " T.relispartition as is_partition,\n" +
                " pg_catalog.pg_get_partkeydef(T.oid) as partition_key,\n" +
                " pg_catalog.pg_get_expr(T.relpartbound, T.oid) as partition_expression,\n" +
                " T.relam am_id,\n" +
                " pg_catalog.pg_get_userbyid(T.relowner) as \"owner\"\n" +
                " from pg_catalog.pg_class T\n" +
                " where relnamespace = $1::oid\n" +
                " and relkind in ('r', 'm', 'v', 'f', 'p')\n" +
                " order by table_kind, table_id");
    }

    @Test
    public void testMeta15()
    {
        test("with languages as (select oid as lang_oid, lanname as lang\n" +
                " from pg_catalog.pg_language),\n" +
                " routines as (select proname as r_name,\n" +
                " prolang as lang_oid,\n" +
                " oid as r_id,\n" +
                " xmin as r_state_number,\n" +
                " proargnames as arg_names,\n" +
                " proargmodes as arg_modes,\n" +
                " proargtypes::int[] as in_arg_types,\n" +
                " proallargtypes::int[] as all_arg_types,\n" +
                " pg_catalog.pg_get_expr(proargdefaults, 0) as arg_defaults,\n" +
                " provariadic as arg_variadic_id,\n" +
                " prorettype as ret_type_id,\n" +
                " proretset as ret_set,\n" +
                " case when proiswindow then 'w'\n" +
                " when proisagg then 'a'\n" +
                " else 'f'\n" +
                " end as kind,\n" +
                " provolatile as volatile_kind,\n" +
                " proisstrict as is_strict,\n" +
                " prosecdef as is_security_definer,\n" +
                " proconfig as configuration_parameters,\n" +
                " procost as cost,\n" +
                " pg_catalog.pg_get_userbyid(proowner) as \"owner\",\n" +
                " prorows as rows,\n" +
                " proleakproof as is_leakproof,\n" +
                " proparallel as concurrency_kind\n" +
                " from pg_catalog.pg_proc\n" +
                " where pronamespace = $1::oid\n" +
                " and not proisagg\n" +
                ")\n" +
                " select *\n" +
                " from routines natural join languages");
    }

    @Test
    public void testMeta16()
    {
        test("with T as (select distinct\n" +
                " T.oid as table_id, T.relname as table_name\n" +
                " from pg_catalog.pg_class T\n" +
                ", pg_catalog.pg_attribute A\n" +
                " where T.relnamespace = $1::oid\n" +
                " and T.relkind in ('r', 'm', 'v', 'f', 'p')\n" +
                " and (pg_catalog.age(A.xmin) <= coalesce(nullif(greatest(pg_catalog.age($2::varchar::xid), -1), -1), 2147483647) or pg_catalog.age(T.xmin) <= coalesce(nullif(greatest(pg_catalog.age($3::varchar::xid), -1), -1), 2147483647))\n" +
                " and A.attrelid = T.oid)\n" +
                " select T.table_id,\n" +
                " C.attnum as column_position,\n" +
                " C.attname as column_name,\n" +
                " C.xmin as column_state_number,\n" +
                " C.atttypmod as type_mod,\n" +
                " C.attndims as dimensions_number,\n" +
                " pg_catalog.format_type(C.atttypid, C.atttypmod) as type_spec,\n" +
                " C.atttypid as type_id,\n" +
                " C.attnotnull as mandatory,\n" +
                " D.adsrc as column_default_expression,\n" +
                " not C.attislocal as column_is_inherited,\n" +
                " C.attfdwoptions as options,\n" +
                " C.attisdropped as column_is_dropped,\n" +
                " C.attidentity as identity_kind,\n" +
                " null as generated\n" +
                " from T\n" +
                " join pg_catalog.pg_attribute C on T.table_id = C.attrelid\n" +
                " left outer join pg_catalog.pg_attrdef D on (C.attrelid, C.attnum) = (D.adrelid, D.adnum)\n" +
                " where attnum > 0\n" +
                " order by table_id, attnum");
    }

    @Test
    public void testMeta17()
    {
        test("select R.ev_class as table_id,\n" +
                " R.oid as rule_id,\n" +
                " R.xmin as rule_state_number,\n" +
                " R.rulename as rule_name,\n" +
                " pg_catalog.translate(ev_type, '1234', 'SUID') as rule_event_code,\n" +
                " R.ev_enabled as rule_fire_mode,\n" +
                " R.is_instead as rule_is_instead\n" +
                " from pg_catalog.pg_rewrite R\n" +
                " where R.ev_class in (select oid\n" +
                " from pg_catalog.pg_class\n" +
                " where relnamespace = $1::oid)\n" +
                " and pg_catalog.age(R.xmin) <= coalesce(nullif(greatest(pg_catalog.age($2::varchar::xid), -1), -1), 2147483647)\n" +
                " and R.rulename != '_RETURN'::name\n" +
                " order by R.ev_class::bigint, ev_type");
    }

    @Test
    public void testMeta18()
    {
        test("select D.objoid id, pg_catalog.array_agg(D.objsubid) sub_ids\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_class C on D.objoid = C.oid\n" +
                " where C.relnamespace = $1::oid and C.relkind != 'c' and D.classoid = 'pg_catalog.pg_class'::regclass\n" +
                " group by D.objoid\n" +
                " union all\n" +
                " select T.oid id, pg_catalog.array_agg(D.objsubid)\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_type T on T.oid = D.objoid or T.typrelid = D.objoid\n" +
                " left outer join pg_catalog.pg_class C on T.typrelid = C.oid\n" +
                " where T.typnamespace = $2::oid and (C.relkind = 'c' or C.relkind is null)\n" +
                " group by T.oid\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_constraint C on D.objoid = C.oid\n" +
                " where C.connamespace = $3::oid and D.classoid = 'pg_catalog.pg_constraint'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_trigger T on T.oid = D.objoid\n" +
                " join pg_catalog.pg_class C on C.oid = T.tgrelid\n" +
                " where C.relnamespace = $4::oid and D.classoid = 'pg_catalog.pg_trigger'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_rewrite R on R.oid = D.objoid\n" +
                " join pg_catalog.pg_class C on C.oid = R.ev_class\n" +
                " where C.relnamespace = $5::oid and D.classoid = 'pg_catalog.pg_rewrite'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_proc P on P.oid = D.objoid\n" +
                " where P.pronamespace = $6::oid and D.classoid = 'pg_catalog.pg_proc'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_operator O on O.oid = D.objoid\n" +
                " where O.oprnamespace = $7::oid and D.classoid = 'pg_catalog.pg_operator'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_opclass O on O.oid = D.objoid\n" +
                " where O.opcnamespace = $8::oid and D.classoid = 'pg_catalog.pg_opclass'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_opfamily O on O.oid = D.objoid\n" +
                " where O.opfnamespace = $9::oid and D.classoid = 'pg_catalog.pg_opfamily'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_collation C on C.oid = D.objoid\n" +
                " where C.collnamespace = $10::oid and D.classoid = 'pg_catalog.pg_collation'::regclass\n" +
                " union all\n" +
                " select D.objoid id, array[D.objsubid]\n" +
                " from pg_catalog.pg_description D\n" +
                " join pg_catalog.pg_policy P on P.oid = D.objoid\n" +
                " join pg_catalog.pg_class C on P.polrelid = C.oid\n" +
                " where C.relnamespace = $11::oid and D.classoid = 'pg_catalog.pg_policy'::regclass");
    }

    @Test
    public void testMeta19()
    {
        test("select ind_head.indexrelid index_id,\n" +
                " k col_idx,\n" +
                " true in_key,\n" +
                " ind_head.indkey[k - 1] column_position,\n" +
                " ind_head.indoption[k - 1] column_options,\n" +
                " ind_head.indcollation[k - 1] as collation,\n" +
                " colln.nspname as collation_schema,\n" +
                " collname as collation_str,\n" +
                " ind_head.indclass[k - 1] as opclass,\n" +
                " case when opcdefault then null else opcn.nspname end as opclass_schema,\n" +
                " case when opcdefault then null else opcname end as opclass_str,\n" +
                " case\n" +
                " when indexprs is null then null\n" +
                " when ind_head.indkey[k - 1] = 0 then chr(27) || pg_catalog.pg_get_indexdef(ind_head.indexrelid, k::int, true)\n" +
                " else pg_catalog.pg_get_indexdef(ind_head.indexrelid, k::int, true)\n" +
                " end as expression,\n" +
                " amcanorder can_order\n" +
                " from pg_catalog.pg_index ind_head\n" +
                " join pg_catalog.pg_class ind_stor\n" +
                " on ind_stor.oid = ind_head.indexrelid\n" +
                " cross join unnest(ind_head.indkey) with ordinality u(u, k)\n" +
                " left outer join pg_catalog.pg_collation\n" +
                " on pg_collation.oid = ind_head.indcollation[k - 1]\n" +
                " left outer join pg_catalog.pg_namespace colln on collnamespace = colln.oid\n" +
                " cross join pg_catalog.pg_indexam_has_property(ind_stor.relam, 'can_order') amcanorder\n" +
                " left outer join pg_catalog.pg_opclass\n" +
                " on pg_opclass.oid = ind_head.indclass[k - 1]\n" +
                " left outer join pg_catalog.pg_namespace opcn on opcnamespace = opcn.oid\n" +
                " where ind_stor.relnamespace = $1::oid\n" +
                " and ind_stor.relkind in ('i', 'I')\n" +
                " and pg_catalog.age(ind_stor.xmin) <= coalesce(nullif(greatest(pg_catalog.age($2::varchar::xid), -1), -1), 2147483647)\n" +
                " order by index_id, k");
    }

    @Test
    public void testMeta20()
    {
        test("select T.relkind as view_kind,\n" +
                " T.oid as view_id,\n" +
                " pg_catalog.pg_get_viewdef(T.oid, true) as source_text\n" +
                " from pg_catalog.pg_class T\n" +
                " join pg_catalog.pg_namespace N on T.relnamespace = N.oid\n" +
                " where N.oid = $1::oid\n" +
                " and T.relkind in ('m', 'v')\n" +
                " and (pg_catalog.age(T.xmin) <= coalesce(nullif(greatest(pg_catalog.age($2::varchar::xid), -1), -1), 2147483647) or exists (\n" +
                "select A.attrelid from pg_catalog.pg_attribute A where A.attrelid = T.oid and pg_catalog.age(A.xmin) <= coalesce(nullif(greatest(pg_catalog.age($3::varchar::xid), -1), -1), 2147483647)))");
    }

    @Test
    public void testMeta21()
    {
        test("select t.*, CTID\n" +
                " from pg_catalog.pg_aggregate t\n" +
                " limit 501");
    }

    @Test
    public void testMeta22()
    {
        test("select a, b from ((select * from myTable) union select * from xTable union all (select * from yTable))\n");
    }

    @Test
    public void testLike()
    {
        test("select a, b from myTable where x like '%s'\n");
        test("select a, b from myTable where x not like '%s'\n");
        test("select a, b from myTable where x ilike '%s'\n");
        test("select a, b from myTable where x ilike '%s' escape 'a'\n");
    }

    @Test
    public void testBitwiseBinary()
    {
        test("select a, b from myTable where x & 3 = 3\n");
        test("select a, b from myTable where x | 3 = 3\n");
        test("select a, b from myTable where x ^ 3 = 3\n");
    }

    @Test
    public void testRecordSubscript()
    {
        test("select a, (val.func('x')).b from myTable\n");
    }

    @Test
    public void testTrim()
    {
        test("select a, trim(x.val) from myTable as x\n");
        test("select a, trim(leading from x.val) from myTable as x\n");
        test("select a, trim(leading '.' from x.val) from myTable as x\n");
    }

    @Test
    public void testSimpleCase()
    {
        test("select case x.a = 1 when true then 'ok' when false then 'other' end from myTable as x\n");
        test("select case x.a = 1 when true then 'ok' when false then 'other' else 'bla' end from myTable as x\n");
    }

    @Test
    public void testSelectEscape()
    {
        test("select * from myTable as x where x.val = E'%'\n");
    }

    public void test(String sql)
    {
        Assert.assertEquals(sql.replace("\n", ""), SQLGrammarParser.getSqlBaseParser(sql, "query").statement().accept(new SQLSerializer()));
    }
}