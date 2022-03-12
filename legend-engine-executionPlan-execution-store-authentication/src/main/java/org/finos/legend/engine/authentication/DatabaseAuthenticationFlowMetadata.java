package org.finos.legend.engine.authentication;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;

public class DatabaseAuthenticationFlowMetadata implements Comparable<DatabaseAuthenticationFlowMetadata>
{
    private DatabaseType databaseType;

    //TODO : Should these be strings ??
    private String datasourceType;
    private String authenticationType;

    public DatabaseAuthenticationFlowMetadata()
    {
        // Jackson
    }

    public DatabaseAuthenticationFlowMetadata(DatabaseType databaseType, String datasourceType, String authenticationType)
    {
        this.databaseType = databaseType;
        this.datasourceType = datasourceType;
        this.authenticationType = authenticationType;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public String getDatasourceType() {
        return datasourceType;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    @Override
    public int compareTo(DatabaseAuthenticationFlowMetadata other)
    {
        int comparison = this.databaseType.compareTo(other.databaseType);
        if (comparison != 0 )
        {
            return comparison;
        }
        comparison = this.datasourceType.compareTo(other.datasourceType);
        if (comparison != 0 )
        {
            return comparison;
        }
        return this.authenticationType.compareTo(other.authenticationType);
    }
}
