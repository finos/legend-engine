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

import java.util.LinkedHashMap;
import java.util.Map;

public class BenchConfig
{
    public int scale = 100;
    public String query = "simple";
    public int iterations = 10;
    public int warmup = 2;
    public boolean execute = false;
    public String csv = null;
    public boolean pause = false;
    public String dumpPlan = null;

    public String nextMultiplicity = "0..1";
    public int unionSets = 0;
    public int includes = 0;
    public boolean milestoned = false;
    public boolean relationFunction = false;
    public boolean modelToModel = false;
    public int semiDepth = 0;
    public String dbType = "H2";

    public String fileList = null;
    public String queryFile = null;
    public String mappingPath = "test::Map";
    public String storePath = "test::DB";

    public static BenchConfig parse(String[] args)
    {
        Map<String, String> opts = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("--"))
            {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--"))
                {
                    opts.put(key, args[++i]);
                }
                else
                {
                    opts.put(key, "true");
                }
            }
        }

        BenchConfig config = new BenchConfig();
        config.scale = Integer.parseInt(opts.getOrDefault("scale", "100"));
        config.query = opts.getOrDefault("query", "simple");
        config.iterations = Integer.parseInt(opts.getOrDefault("iters", "10"));
        config.warmup = Integer.parseInt(opts.getOrDefault("warmup", "2"));
        config.execute = opts.containsKey("execute");
        config.csv = opts.get("csv");
        config.pause = opts.containsKey("pause");
        config.dumpPlan = opts.get("dumpplan");
        config.nextMultiplicity = opts.getOrDefault("nextmult", "0..1");
        config.unionSets = Integer.parseInt(opts.getOrDefault("union", "0"));
        config.includes = Integer.parseInt(opts.getOrDefault("includes", "0"));
        config.milestoned = opts.containsKey("milestoning");
        config.relationFunction = opts.containsKey("relfunc");
        config.modelToModel = opts.containsKey("m2m");
        config.semiDepth = Integer.parseInt(opts.getOrDefault("semi", "0"));
        config.dbType = opts.getOrDefault("dbtype", "H2");
        config.fileList = opts.get("filelist");
        config.queryFile = opts.get("queryfile");
        config.mappingPath = opts.getOrDefault("mapping", "test::Map");
        config.storePath = opts.getOrDefault("db", "test::DB");
        return config;
    }

    public String workloadId()
    {
        StringBuilder b = new StringBuilder(this.query);
        b.append("@scale").append(this.scale);
        if (this.unionSets > 0)
        {
            b.append("+union").append(this.unionSets);
        }
        if (this.includes > 0)
        {
            b.append("+includes").append(this.includes);
        }
        if (this.semiDepth > 0)
        {
            b.append("+semi").append(this.semiDepth);
        }
        if (this.milestoned)
        {
            b.append("+milestoned");
        }
        if (this.relationFunction)
        {
            b.append("+relfunc");
        }
        if (this.modelToModel)
        {
            b.append("+m2m");
        }
        if (!"0..1".equals(this.nextMultiplicity))
        {
            b.append("+mult").append(this.nextMultiplicity);
        }
        b.append("/").append(this.dbType);
        return b.toString();
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("scale", this.scale);
        map.put("query", this.query);
        map.put("dbType", this.dbType);
        map.put("unionSets", this.unionSets);
        map.put("includes", this.includes);
        map.put("semiDepth", this.semiDepth);
        map.put("milestoned", this.milestoned);
        map.put("relationFunction", this.relationFunction);
        map.put("modelToModel", this.modelToModel);
        map.put("nextMultiplicity", this.nextMultiplicity);
        map.put("execute", this.execute);
        return map;
    }

    public String describe()
    {
        StringBuilder b = new StringBuilder();
        b.append("scale=").append(this.scale).append(" query=").append(this.query).append(" dbType=").append(this.dbType);
        if (this.unionSets > 0)
        {
            b.append(" union=").append(this.unionSets);
        }
        if (this.includes > 0)
        {
            b.append(" includes=").append(this.includes);
        }
        if (this.semiDepth > 0)
        {
            b.append(" semi=").append(this.semiDepth);
        }
        if (this.milestoned)
        {
            b.append(" milestoned");
        }
        if (this.relationFunction)
        {
            b.append(" relfunc");
        }
        if (this.modelToModel)
        {
            b.append(" m2m");
        }
        b.append(" nextMult=[").append(this.nextMultiplicity).append("]");
        b.append(" warmup=").append(this.warmup).append(" iters=").append(this.iterations);
        return b.toString();
    }
}
