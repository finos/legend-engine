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

package org.finos.legend.engine.perf;

/**
 * Generates a synthetic Legend model of a requested size: N classes, N tables, N-1 joins and a
 * mapping covering every class. Optional flags switch the mapping into the shapes whose cost this
 * harness measures - unions, include chains, milestoning, Relation functions with ModelJoins,
 * model-to-model views and semi-structured bindings.
 */
public class ModelGenerator
{
    private final BenchConfig config;

    public ModelGenerator(BenchConfig config)
    {
        this.config = config;
    }

    public String generate()
    {
        int n = this.config.scale;
        StringBuilder b = new StringBuilder();
        this.appendDomain(b, n);
        this.appendStore(b, n);
        this.appendBinding(b);
        this.appendMapping(b, n);
        this.appendRuntime(b);
        return b.toString();
    }

    private void appendDomain(StringBuilder b, int n)
    {
        b.append("###Pure\n");
        String stereotype = this.config.milestoned ? "<<meta::pure::profiles::temporal.businesstemporal>> " : "";
        for (int i = 0; i < n; i++)
        {
            b.append("Class ").append(stereotype).append("test::domain::C").append(i).append("\n{\n");
            b.append("  id: Integer[1];\n  p0: String[0..1];\n  p1: String[0..1];\n  p2: String[0..1];\n  p3: Integer[0..1];\n");
            if (this.config.semiDepth > 0 && i == 0)
            {
                b.append("  det: test::sdet::S0[1];\n");
            }
            if (!this.config.relationFunction && i < n - 1)
            {
                b.append("  next: test::domain::C").append(i + 1).append("[").append(this.config.nextMultiplicity).append("];\n");
            }
            b.append("}\n");
        }

        for (int j = 0; j < this.config.semiDepth; j++)
        {
            b.append("Class test::sdet::S").append(j).append("\n{\n  v: String[0..1];\n  w: Integer[0..1];\n");
            if (j < this.config.semiDepth - 1)
            {
                b.append("  child: test::sdet::S").append(j + 1).append("[1];\n");
            }
            b.append("}\n");
        }

        if (this.config.modelToModel)
        {
            b.append("Class test::view::V0\n{\n  q0: String[0..1];\n  q1: String[0..1];\n  q2: String[0..1];\n}\n");
        }

        if (this.config.relationFunction)
        {
            for (int i = 0; i < n - 1; i++)
            {
                b.append("Association test::domain::A").append(i).append("\n{\n");
                b.append("  prev").append(i).append(": test::domain::C").append(i).append("[1];\n");
                b.append("  next: test::domain::C").append(i + 1).append("[1];\n}\n");
            }
        }
    }

    private void appendStore(StringBuilder b, int n)
    {
        b.append("###Relational\nDatabase test::DB\n(\n");
        for (int i = 0; i < n; i++)
        {
            b.append("  Table T").append(i).append(" (\n");
            if (this.config.milestoned)
            {
                b.append("    milestoning\n    (\n      business(BUS_FROM = from_z, BUS_THRU = thru_z)\n    )\n");
            }
            b.append("    id INT PRIMARY KEY, p0 VARCHAR(100), p1 VARCHAR(100), p2 VARCHAR(100), p3 INT, fk INT");
            if (this.config.semiDepth > 0 && i == 0)
            {
                b.append(", sdet SEMISTRUCTURED");
            }
            if (this.config.milestoned)
            {
                b.append(", from_z TIMESTAMP PRIMARY KEY, thru_z TIMESTAMP");
            }
            b.append("\n  )\n");
        }
        for (int i = 0; i < n - 1; i++)
        {
            b.append("  Join J").append(i).append("(T").append(i).append(".fk = T").append(i + 1).append(".id)\n");
        }
        b.append(")\n");
    }

    private void appendBinding(StringBuilder b)
    {
        if (this.config.semiDepth <= 0)
        {
            return;
        }
        b.append("###ExternalFormat\nBinding test::SB\n{\n  contentType: 'application/json';\n  modelIncludes: [\n");
        for (int j = 0; j < this.config.semiDepth; j++)
        {
            b.append("    test::sdet::S").append(j).append(j < this.config.semiDepth - 1 ? ",\n" : "\n");
        }
        b.append("  ];\n}\n");
    }

    private void appendMapping(StringBuilder b, int n)
    {
        if (this.config.relationFunction)
        {
            this.appendRelationFunctionMapping(b, n);
            return;
        }
        b.append("###Mapping\n");
        if (this.config.includes > 0)
        {
            this.appendIncludeChainMapping(b, n);
            return;
        }
        b.append("Mapping test::Map\n(\n");
        if (this.config.unionSets > 0)
        {
            this.appendUnionMapping(b, n);
        }
        for (int i = this.config.unionSets > 0 ? 1 : 0; i < n; i++)
        {
            this.appendClassMapping(b, i, n);
        }
        if (this.config.modelToModel)
        {
            b.append("  test::view::V0: Pure\n  {\n    ~src test::domain::C0\n    q0: $src.p0,\n    q1: $src.p1,\n    q2: $src.p2\n  }\n");
        }
        b.append(")\n");
    }

    private void appendIncludeChainMapping(StringBuilder b, int n)
    {
        for (int j = 0; j < this.config.includes; j++)
        {
            b.append("Mapping test::M").append(j).append("\n(\n");
            if (j > 0)
            {
                b.append("  include mapping test::M").append(j - 1).append("\n");
            }
            for (int i = 0; i < n; i++)
            {
                if ((int) ((long) i * this.config.includes / n) == j)
                {
                    this.appendClassMapping(b, i, n);
                }
            }
            b.append(")\n");
        }
        b.append("Mapping test::Map\n(\n  include mapping test::M").append(this.config.includes - 1).append("\n)\n");
    }

    private void appendUnionMapping(StringBuilder b, int n)
    {
        StringBuilder ids = new StringBuilder();
        for (int u = 0; u < this.config.unionSets; u++)
        {
            ids.append(u > 0 ? "," : "").append("s").append(u);
        }
        b.append("  *test::domain::C0: Operation\n  {\n    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(").append(ids).append(")\n  }\n");
        for (int u = 0; u < this.config.unionSets; u++)
        {
            b.append("  test::domain::C0[s").append(u).append("]: Relational\n  {\n");
            b.append("    ~primaryKey([test::DB]T0.id)\n    ~mainTable [test::DB]T0\n");
            b.append("    id: [test::DB]T0.id,\n    p0: [test::DB]T0.p0,\n    p1: [test::DB]T0.p1,\n    p2: [test::DB]T0.p2,\n    p3: [test::DB]T0.p3");
            if (this.config.semiDepth > 0)
            {
                b.append(",\n    det: Binding test::SB : [test::DB]T0.sdet");
            }
            if (n > 1)
            {
                b.append(",\n    next: [test::DB]@J0\n");
            }
            else
            {
                b.append("\n");
            }
            b.append("  }\n");
        }
    }

    private void appendClassMapping(StringBuilder b, int i, int n)
    {
        b.append("  test::domain::C").append(i).append(": Relational\n  {\n");
        b.append("    ~primaryKey([test::DB]T").append(i).append(".id)\n");
        b.append("    ~mainTable [test::DB]T").append(i).append("\n");
        b.append("    id: [test::DB]T").append(i).append(".id,\n");
        b.append("    p0: [test::DB]T").append(i).append(".p0,\n");
        b.append("    p1: [test::DB]T").append(i).append(".p1,\n");
        b.append("    p2: [test::DB]T").append(i).append(".p2,\n");
        b.append("    p3: [test::DB]T").append(i).append(".p3");
        if (this.config.semiDepth > 0 && i == 0)
        {
            b.append(",\n    det: Binding test::SB : [test::DB]T0.sdet");
        }
        if (i < n - 1)
        {
            b.append(",\n    next: [test::DB]@J").append(i).append("\n");
        }
        else
        {
            b.append("\n");
        }
        b.append("  }\n");
    }

    private void appendRelationFunctionMapping(StringBuilder b, int n)
    {
        b.append("###Pure\n");
        for (int i = 0; i < n; i++)
        {
            b.append("function test::f::rel").append(i).append("(): meta::pure::metamodel::relation::Relation<Any>[1]\n{\n  #>{test::DB.T").append(i).append("}#\n}\n");
        }
        b.append("###Mapping\nMapping test::Map\n(\n");
        for (int i = 0; i < n; i++)
        {
            b.append("  *test::domain::C").append(i).append("[s").append(i).append("]: Relation\n  {\n");
            b.append("    ~func test::f::rel").append(i).append("():Relation<Any>[1]\n");
            b.append("    id: id,\n    p0: p0,\n    p1: p1,\n    p2: p2,\n    p3: p3,\n");
            b.append("    +fkv: Integer[0..1]: fk\n  }\n");
        }
        for (int i = 0; i < n - 1; i++)
        {
            b.append("  test::domain::A").append(i).append(": ModelJoin\n  {\n");
            b.append("    {prev").append(i).append(": test::domain::C").append(i).append("[1], next: test::domain::C").append(i + 1)
                    .append("[1] | $prev").append(i).append(".fkv == $next.id}\n  }\n");
        }
        b.append(")\n");
    }

    private void appendRuntime(StringBuilder b)
    {
        b.append("###Runtime\nRuntime test::Runtime\n{\n  mappings:\n  [\n    test::Map\n  ];\n  connections:\n  [\n");
        if (this.config.modelToModel)
        {
            b.append("    ModelStore:\n    [\n      cm: #{\n        ModelChainConnection\n        {\n          mappings: [test::Map];\n        }\n      }#\n    ],\n");
        }
        b.append("    test::DB:\n    [\n      c1: #{\n        RelationalDatabaseConnection\n        {\n");
        b.append(connectionBody(this.config.dbType));
        b.append("        }\n      }#\n    ]\n  ];\n}\n");
    }

    public static String connectionBody(String dbType)
    {
        if ("DuckDB".equals(dbType))
        {
            return "          type: DuckDB;\n          specification: DuckDB\n          {\n            path: '/tmp/legend-perf-bench.duckdb';\n          };\n          auth: Test;\n";
        }
        if ("Snowflake".equals(dbType))
        {
            return "          type: Snowflake;\n          specification: Snowflake\n          {\n            name: 'test';\n            account: 'account';\n            warehouse: 'wh';\n            region: 'us-east2';\n            cloudType: 'aws';\n          };\n"
                    + "          auth: SnowflakePublic\n          {\n            publicUserName: 'u';\n            privateKeyVaultReference: 'k';\n            passPhraseVaultReference: 'p';\n          };\n";
        }
        return "          type: H2;\n          specification: LocalH2 {};\n          auth: DefaultH2;\n";
    }
}
