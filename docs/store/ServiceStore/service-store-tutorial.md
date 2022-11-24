# Get started with service store

This page lists the steps to quickly get started with integrating your REST API in the Legend ecosystem.

1. Model your business concepts using classes in Studio.

    ```java
    ###Pure
    Class meta::external::store::service::showcase::domain::S_Product
    {
        s_productId   : String[1];
        s_productName : String[1];
        s_description : String[1];
        s_synonyms    : meta::external::store::service::showcase::domain::S_Synonym[*];
    }
    
    Class meta::external::store::service::showcase::domain::S_Synonym
    {
        s_name : String[1];
        s_type : String[1];
    }
    ```

2. Model your REST API using the service store concept.

    ```java
    ###ServiceStore
    ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
    (
        description : 'Showcase Service Store';
        
        ServiceGroup ProductServices
        (
            path : '/products';
        
            Service GetAllProductsService
            (
               path : '/getAllProducts';
               method : GET;
               security : [];
               response : [meta::external::store::service::showcase::domain::S_Product <- meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding];
            )
        )
    )

    ###ExternalFormat
    Binding meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding
    {
        contentType   : 'application/json';
        modelIncludes : [ meta::external::store::service::showcase::domain::S_Product, meta::external::store::service::showcase::domain::S_Synonym ];
    }
    ```

3. Connect your model with your API using mapping.

    ```java
    ###Mapping
    Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
    (
        *meta::external::store::service::showcase::domain::S_Product[s_prod_set]: ServiceStore
        {
            ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] ProductServices.GetAllProductsService
        }
    )
    ```

4. Define `ServiceStoreConnection` and `runtime` providing the `baseUrl` for your HTTP server.

    ```java
    ###Runtime
    Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
    {
        mappings :
        [
            meta::external::store::service::showcase::mapping::ServiceStoreMapping
        ];
        connections :
        [
            meta::external::store::service::showcase::store::TradeProductServiceStore :
            [
                connection_1 : meta::external::store::service::showcase::connection::serviceStoreConnection
            ]
        ];
    }
    
    ###Connection
    ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection
    {
        store   : meta::external::store::service::showcase::store::TradeProductServiceStore;
        baseUrl : 'http://127.0.0.1:9090';
    }
    ```

5. Define Legend service with queries on your models.

    ```java
    ###Service
    Service meta::external::store::service::showcase::service::ServiceStoreShowcase
    {
        pattern: '/showcase/serviceStoreService';
        owners:
        [
            'xyz',
            'abc'
        ];
        documentation: 'Service demonstrating service store use case';
        autoActivateUpdates: true;
        execution: Single
        {
            query: |meta::external::store::service::showcase::domain::S_Product.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Product {s_productId,s_productName,s_description,s_synonyms {s_name, s_type}}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Product {s_productId,s_productName,s_description,s_synonyms {s_name, s_type}}}#);
            mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
            runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
        }
        testSuites: <<To be Added>>
    }
    ```

6. Consume your service.

## More info

- [Service store reference documents](./service-store.md)
- [Service store examples and templates](./service-store-examples-templates.md)
