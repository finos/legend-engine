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


package org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.JDBCSessionHandler;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model.Column;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model.Database;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model.Function;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model.Schema;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.model.Table;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendExecution;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.TypeConversion;
import org.finos.legend.engine.postgres.protocol.wire.serialization.types.PGType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableType;
import org.finos.legend.engine.query.sql.api.schema.AddressableRelation;
import org.finos.legend.engine.query.sql.api.schema.SchemaResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.finos.legend.engine.postgres.protocol.wire.serialization.types.PGTypes.SQL_TO_PG_TYPES;

public class CatalogManager
{
    private static AtomicInteger counter = new AtomicInteger(0);
    private final int id;

    public CatalogManager(Identity identity, String database, LegendExecution legendExecution, JDBCSessionHandler legendSessionHandler)
    {
        id = counter.incrementAndGet();
        SchemaResult result;
        if (identity.getFirstCredential() instanceof LegendKerberosCredential)
        {
            LegendKerberosCredential credential = (LegendKerberosCredential) identity.getFirstCredential();
            result = Subject.doAs(credential.getSubject(), (PrivilegedAction<SchemaResult>) () -> legendExecution.getProjectSchema(database));
        }
        else
        {
            result = legendExecution.getProjectSchema(database);
        }

        if (result != null)
        {
            MutableMap<String, List<String>> types = Maps.mutable.empty();
            result.typeInheritances.forEach(x ->
            {
                types.put(x.type, x.linearizedInheritance);
            });

            try
            {
                // Prepare
                legendSessionHandler.prepareStatement("CREATE SCHEMA metadata_" + id + ";").execute();
                legendSessionHandler.prepareStatement("CREATE TABLE metadata_" + id + ".database as select oid, * from pg_catalog.pg_database where 1 = 0;").execute();
                legendSessionHandler.prepareStatement("CREATE TABLE metadata_" + id + ".namespace as select oid, * from pg_catalog.pg_namespace where nspname='pg_catalog';").execute();
                legendSessionHandler.prepareStatement("CREATE TABLE metadata_" + id + ".class as select oid, * from pg_catalog.pg_class;").execute();
                legendSessionHandler.prepareStatement("CREATE TABLE metadata_" + id + ".attribute as select * from pg_catalog.pg_attribute;").execute();
                legendSessionHandler.prepareStatement("CREATE TABLE metadata_" + id + ".proc as select oid,* from pg_catalog.pg_proc where 0 = 1;").execute();

                Database db = new Database(database);

                MutableListMultimap<String, AddressableRelation> grouped = ListIterate.groupBy(result.addressableRelations, x -> x.packageableElement);

                for (String key : grouped.keySet())
                {
                    Schema schema = db.schema(new Schema(key.replace("::", "_")));

                    schema.tables(
                            ListIterate.collect(grouped.get(key), z ->
                                    {
                                        Table tb = new Table(z.tableFunctionName + "_" + Lists.mutable.withAll(z.pathWithinElement).makeString("."));
                                        tb.columns(ListIterate.collect(z.relationType.columns, p ->
                                        {
                                            String typeName = ((PackageableType) p.genericType.rawType).fullPath;
                                            Integer sqlType = TypeConversion._typeConversions.get(ListIterate.detect(types.get(typeName), v -> TypeConversion._typeConversions.get(v) != null));
                                            if (sqlType == null)
                                            {
                                                throw new RuntimeException("The type '" + typeName + "' can't be converted to a SQL type.");
                                            }
                                            PGType<?> pgType = SQL_TO_PG_TYPES.get(sqlType);
                                            if (pgType == null)
                                            {
                                                throw new RuntimeException("The SQL Type '" + sqlType + "' from the type '" + typeName + "' can't be converted to a Postgres type.");
                                            }
                                            return new Column(p.name, pgType);
                                        }));
                                        return tb;
                                    }
                            )
                    );
                }

                create(db, legendSessionHandler);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public int getId()
    {
        return id;
    }

    public void close()
    {

    }

//            Database db = new Database("pierredb");
//            Schema schema = db.schema(new Schema("pierreSchema"));
//            schema.table(new Table("pierreTable"))
//                    .columns(Lists.mutable.with(new Column("pierreColumn", BooleanType.INSTANCE)));
//            schema.function(new Function("myFunc", Lists.mutable.with(BooleanType.INSTANCE, IntegerType.INSTANCE), VarCharType.INSTANCE, false));
//
//            create(db, legendSessionHandler);

    public void create(Database database, JDBCSessionHandler legendSessionHandler) throws Exception
    {
        // Create DB
        legendSessionHandler.prepareStatement(
                "insert into metadata_" + id + ".database (oid, datname, datdba, encoding, datcollate, datctype, datistemplate, datallowconn, datconnlimit, datlastsysoid, datfrozenxid, datminmxid, dattablespace, datacl)" +
                        " values " +
                        "(" + database.getDbId() + ", '" + database.getName() + "', 10, 6, 'en_US.utf8', 'en_US.utf8', false, true, -1, 12993, 549::varchar::xid, 1::varchar::xid, 1663, null);"
        ).execute();

        // Create Schema
        for (Schema schema : database.getSchemas())
        {
            legendSessionHandler.prepareStatement(
                    "insert into metadata_" + id + ".namespace (oid, nspname, nspowner, nspacl)" +
                            " values " +
                            "(" + schema.getSchemaId() + ", '" + schema.getName() + "', 10, null);"
            ).execute();

            for (Table table : schema.getTables())
            {
                // Create Table
                legendSessionHandler.prepareStatement(
                        "insert into metadata_" + id + ".class (oid, relname, relnamespace, reltype, reloftype, relowner, relam, relfilenode, reltablespace, relpages, reltuples, relallvisible, reltoastrelid, relhasindex, relisshared, relpersistence, relkind, relnatts, relchecks, relhasoids, relhaspkey, relhasrules, relhastriggers, relhassubclass, relrowsecurity, relforcerowsecurity, relispopulated, relreplident, relispartition, relfrozenxid, relminmxid, relacl, reloptions, relpartbound)" +
                                " values " +
                                "(" + table.getTableId() + ", '" + table.getName() + "', " + schema.getSchemaId() + ", null, 0, 10, 0, null, 0, 0, 0, 0, null, false, false, 'p', 'r', null, 0, true, false, false, false, false, false, false, true, 'd', false, null, 1::varchar::xid, null, null, null);"
                ).execute();

                int attNum = 1;
                for (Column column : table.getColumns())
                {
                    // Create Column
                    legendSessionHandler.prepareStatement(
                            "insert into metadata_" + id + ".attribute (attrelid, attname, atttypid, attstattarget, attlen, attnum, attndims, attcacheoff, atttypmod, attbyval, attstorage, attalign, attnotnull, atthasdef, attidentity, attisdropped, attislocal, attinhcount, attcollation, attacl, attoptions, attfdwoptions)" +
                                    " values " +
                                    "(" + table.getTableId() + ", '" + column.getName() + "', " + column.getType().oid() + ", -1, " + column.getType().typeLen() + ", " + (attNum++) + ", 0, -1, " + column.getType().typeMod() + ", true, 'p', 'i', true, false, '', false, true, 0, 0, null, null, null);"
                    ).execute();

                }
            }

            for (Function function : schema.getFunctions())
            {
                // Create Function
                legendSessionHandler.prepareStatement(
                        "insert into metadata_" + id + ".proc (proname, pronamespace, proowner, prolang, procost, prorows, provariadic, protransform, proisagg, proiswindow, prosecdef, proleakproof, proisstrict, proretset, provolatile, proparallel, pronargs, pronargdefaults, prorettype, proargtypes, proallargtypes, proargmodes, proargnames, proargdefaults, protrftypes, prosrc, probin, proconfig, proacl)" +
                                " values " +
                                "('" + function.getName() + "', " + schema.getSchemaId() + ", 10, 14, 1, 1000, 0, '-', false, false, false, false, true, " + function.getReturnSet() + ", 's', 's', " + function.getParameters().size() + ", 0, " + function.getReturnType().oid() + ", '" + function.getParameters().collect(PGType::oid).makeString(" ") + "',  null, null, null, null, null, 'xx', null, null, null);"
                ).execute();
            }
        }
    }
}