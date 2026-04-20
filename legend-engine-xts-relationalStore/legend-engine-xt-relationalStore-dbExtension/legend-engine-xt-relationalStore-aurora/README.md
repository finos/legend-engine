# Legend Engine - Amazon Aurora Database Support

This module provides Amazon Aurora (PostgreSQL-compatible) database support for Legend Engine, including built-in failover capabilities via the [AWS Advanced JDBC Wrapper](https://github.com/aws/aws-advanced-jdbc-wrapper).

## Overview

Aurora support is implemented as two datasource specifications:

- **Aurora** — For regional Aurora clusters with local failover
- **GlobalAurora** — For Aurora Global Database clusters with cross-region failover

Both specifications reuse the **PostgreSQL SQL dialect** for query generation, meaning all SQL features supported by the Postgres dialect are automatically available for Aurora connections.

## Connection Specifications

### Aurora (Regional)

Connects to a single-region Aurora cluster with automatic local failover between reader and writer instances.

```legend
#>{
    store: myStore;
    type: Aurora;
    specification: Aurora
    {
        host: 'my-cluster.cluster-abc123.us-east-1.rds.amazonaws.com';
        port: 5432;
        name: 'myDatabase';
        clusterInstanceHostPattern: '?.us-east-1.rds.amazonaws.com';
    };
    auth: UserNamePassword
    {
        baseVaultReference: 'myVault';
        userNameVaultReference: 'db/username';
        passwordVaultReference: 'db/password';
    };
}#
```

| Field | Required | Description |
|-------|----------|-------------|
| `host` | Yes | Aurora cluster endpoint |
| `port` | Yes | Database port (typically `5432`) |
| `name` | Yes | Database name |
| `clusterInstanceHostPattern` | No | Host pattern for instance discovery during failover (e.g., `?.us-east-1.rds.amazonaws.com`) |

**JDBC Wrapper Plugins:** `initialConnection`, `failover2`, `efm2`

### GlobalAurora (Cross-Region)

Connects to an Aurora Global Database with automatic cross-region failover.

```legend
#>{
    store: myStore;
    type: Aurora;
    specification: GlobalAurora
    {
        host: 'my-global-cluster.cluster-abc123.us-east-1.rds.amazonaws.com';
        port: 5432;
        name: 'myDatabase';
        region: 'us-east-1';
        globalClusterInstanceHostPatterns: [
            '?.us-east-1.rds.amazonaws.com',
            '?.eu-west-1.rds.amazonaws.com'
        ];
    };
    auth: UserNamePassword
    {
        baseVaultReference: 'myVault';
        userNameVaultReference: 'db/username';
        passwordVaultReference: 'db/password';
    };
}#
```

| Field | Required | Description |
|-------|----------|-------------|
| `host` | Yes | Primary cluster endpoint |
| `port` | Yes | Database port (typically `5432`) |
| `name` | Yes | Database name |
| `region` | Yes | Home AWS region for failover routing |
| `globalClusterInstanceHostPatterns` | Yes | Host patterns for each region in the global cluster |

**JDBC Wrapper Plugins:** `initialConnection`, `gdbFailover`, `efm2`

## Architecture

```
legend-engine-xt-relationalStore-aurora/
├── aurora-protocol/     # Datasource specification POJOs + protocol extension
├── aurora-grammar/      # ANTLR grammar, parser, composer, compiler extensions
├── aurora-execution/    # JDBC driver, connection manager, auth flows
└── aurora-pure/         # Pure metamodel, dialect redirection, protocol serializers
```

### SQL Dialect

Aurora redirects to the PostgreSQL dialect via `auroraExtension.pure`. The `DbExtensionLoader` for Aurora directly references `createDbExtensionForPostgres`, so all PostgreSQL SQL generation functions are used as-is.

### JDBC Driver

Connections use the AWS Advanced JDBC Wrapper (`software.amazon.jdbc.Driver`) with the `jdbc:aws-wrapper:postgresql://` URL scheme. The wrapper provides:

- **`failover2`** — Automatic failover within a regional Aurora cluster
- **`gdbFailover`** — Automatic failover across regions in a Global Database
- **`efm2`** — Enhanced Failure Monitoring for faster failure detection
- **`initialConnection`** — Connection initialization plugin
