package org.finos.legend.engine.persistence.iceberg;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.catalog.Catalog;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;

import java.util.Map;

public class JdbcCatalogBuilder
{
    private Map<String, String> jdbcProperties;
    private Map<String, String> s3Properties;
    private Configuration hadoopConfiguration;

    public JdbcCatalogBuilder jdbcProperties(Map<String, String> jdbcProperties)
    {
        this.jdbcProperties = jdbcProperties;
        return this;
    }

    public JdbcCatalogBuilder s3Properties(Map<String, String> s3Properties)
    {
        this.s3Properties = s3Properties;
        return this;
    }

    public JdbcCatalogBuilder hadoopConfiguration(Configuration hadoopConfiguration)
    {
        this.hadoopConfiguration = hadoopConfiguration;
        return this;
    }

    /*
       // Init Jdbc catalog
        properties.put(CatalogProperties.CATALOG_IMPL, JdbcCatalog .class.getCanonicalName());
        properties.put(CatalogProperties.URI, "jdbc:postgresql://localhost:5432/postgres");
        properties.put(JdbcCatalog.PROPERTY_PREFIX + "user", "postgres");
        properties.put(JdbcCatalog.PROPERTY_PREFIX + "password", "postgres");

        // Init S3
        properties.put(CatalogProperties.WAREHOUSE_LOCATION, "s3://iceberg");
        properties.put(CatalogProperties.FILE_IO_IMPL, S3FileIO.class.getCanonicalName());
        properties.put(AwsProperties.S3FILEIO_ENDPOINT, "http://localhost:9000");
        properties.put(AwsProperties.S3FILEIO_SECRET_ACCESS_KEY, "minioadmin");
        properties.put(AwsProperties.S3FILEIO_ACCESS_KEY_ID, "minioadmin");
        properties.put(AwsProperties.S3FILEIO_PATH_STYLE_ACCESS, "true");

    */

    public Catalog build(String name)
    {
        Configuration hadoopConf = new Configuration();
        MutableMap<String, String> properties = Maps.mutable.empty();
        properties.putAll(this.jdbcProperties);
        properties.putAll(this.s3Properties);
        return CatalogUtil.buildIcebergCatalog(name, properties, hadoopConf);
    }
}