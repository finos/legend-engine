package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey {
    private String host;
    private int port;
    private String databaseName;
    public String clusterID;

    private String region;
    private String endPointURL;

    public RedshiftDataSourceSpecificationKey(String host, int port, String databaseName, String clusterID, String region, String endPointURL) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.clusterID = clusterID;
        this.region = region;
        this.endPointURL = endPointURL;
    }



    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "host='" + host + '\'' +
                ", region='" + region + '\'' +
                ", port='" + port + '\'' +
                ", databaseName='" + databaseName + '\'' +
                ", clusterID='" + clusterID + '\'' +
                ", endPointURL='" + endPointURL + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "host:" + host + "_" +
                "region:" + region + "_" +
                "port:" + port + "_" +
                "databaseName:" + databaseName + "_" +
                "clusterID:" + clusterID +"_"+
                "endPointURL:" + endPointURL ;
    }
}