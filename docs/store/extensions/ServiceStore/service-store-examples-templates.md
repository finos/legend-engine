# Service store examples and templates

Additional examples can be found [here](https://github.com/finos/legend-engine/tree/master/legend-engine-executionPlan-execution-external-store-service/src/test) as tests.

## Service store example with GET REST API returning JSON response

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

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
  *meta::external::store::service::showcase::domain::S_Product[s_prod_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] ProductServices.GetAllProductsService
  }
)

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

## Service store example with GET REST API returning CSV/flatdata response

```java
###Pure
Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
  *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService
  }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService
      (
         path : '/allTradesService';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
SchemaSet meta::external::store::service::showcase::store::tradeSchemaSet
{
  format  : FlatData;
  schemas : [
    {
        content: 'section A: DelimitedWithHeadings{  scope.untilEof;  delimiter: \',\';  Record  {      s_tradeId       : STRING;      s_traderDetails : STRING;      s_tradeDetails  : STRING;  }}';
    }
  ];
}

Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  schemaSet     : meta::external::store::service::showcase::store::tradeSchemaSet;
  contentType   : 'application/x.flatdata';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
```

## Service store example with GET REST API requiring input parameter

```java
###Service
Service demo::store::service::service::ServiceStoreShowcase
{
  pattern: '/showcase/serviceStoreService/{kerberos}';
  owners:
  [
    'xyz',
    'abc'
  ];
  documentation: 'service demonstarting fetching modelled data via parameterized REST API. API parameters are provided at runtime';
  autoActivateUpdates: true;
  execution: Single
  {
    query: kerberos: String[1]|demo::store::service::domain::Employee.all()->filter(e|$e.Kerberos == $kerberos)->graphFetch(#{demo::store::service::domain::Employee{Kerberos,EmployeeID,Title,FirstName,LastName,CountryCode}}#)->serialize(#{demo::store::service::domain::Employee{Kerberos,EmployeeID,Title,FirstName,LastName,CountryCode}}#);
    mapping: demo::store::service::mapping::ServiceStoreMapping;
    runtime: demo::store::service::runtime::ServiceStoreRuntime;
  }
  testSuites: <<To be Added>>
}


###ServiceStore
ServiceStore demo::store::service::store::LegendServiceStore
(
  ServiceGroup EmployeeServices
  (
    path : '/employees';

    Service EmployeeService
    (
      path : '/employeeByKerberos';
      method : GET;
      parameters :
      (
        kerberos : [String] ( location = query, style = form, explode = true )
      );
      response : [demo::store::service::domain::Employee <- demo::store::service::store::employeeServiceStoreSchemaBinding];
      security : [];
    )
  )
)


###ExternalFormat
Binding demo::store::service::store::employeeServiceStoreSchemaBinding
{
  contentType: 'application/json';
  modelIncludes: [
    demo::store::service::domain::Employee
  ];
}


###Pure
Class demo::store::service::domain::Employee
{
  Kerberos: String[1];
  EmployeeID: String[1];
  Title: String[0..1];
  FirstName: String[0..1];
  LastName: String[0..1];
  CountryCode: String[0..1];
}


###Mapping
Mapping demo::store::service::mapping::ServiceStoreMapping
(
  *demo::store::service::domain::Employee[employee_set]: ServiceStore
  {
    ~service [demo::store::service::store::LegendServiceStore] EmployeeServices.EmployeeService
    (
      ~request
      (
        parameters
        (
          kerberos = $this.Kerberos
        )
      )
    )
  }
)


###Connection
ServiceStoreConnection demo::store::service::connection::serviceStoreConnection
{
  store: demo::store::service::store::LegendServiceStore;
  baseUrl: 'http://127.0.0.1:6060';
}


###Runtime
Runtime demo::store::service::runtime::ServiceStoreRuntime
{
  mappings:
  [
    demo::store::service::mapping::ServiceStoreMapping
  ];
  connections:
  [
    demo::store::service::store::LegendServiceStore:
    [
      connection_1: demo::store::service::connection::serviceStoreConnection
    ]
  ];
}
```

## Service store example with POST REST API

```java
###Pure
Class meta::external::store::service::showcase::domain::Employee
{
  firstName   : String[1];
  lastName    : String[1];
  kerberos    : String[1];
  id          : String[1];
  designation : String[1];
  age         : Integer[1];
  addresses   : meta::external::store::service::showcase::domain::Address[*];
}

Class meta::external::store::service::showcase::domain::Address
{
  street  : String[1];
  city    : String[1];
  country : String[1];
}

Class meta::external::store::service::showcase::domain::EmployeeSearchByFilters
{
  nameFilters        : meta::external::store::service::showcase::domain::NameFilter[*];
  designationFilters : meta::external::store::service::showcase::domain::DesignationFilter[*];
  idFilters          : meta::external::store::service::showcase::domain::IdFilter[*];
}

Class meta::external::store::service::showcase::domain::NameFilter
{
  firstName   : String[1];
  lastName    : String[1];
}

Class meta::external::store::service::showcase::domain::DesignationFilter
{
  designation : String[1];
}

Class meta::external::store::service::showcase::domain::IdFilter
{
  id          : String[1];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
  *meta::external::store::service::showcase::domain::Employee[employee_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::EmployeeServiceStore] EmployeeServices.EmployeeDetailsByFilters
     (
         ~request
         (
             body = ^meta::external::store::service::showcase::domain::EmployeeSearchByFilters
                     (
                        nameFilters = ^meta::external::store::service::showcase::domain::NameFilter(firstName = $this.firstName, lastName = $this.lastName),
                        designationFilters = ^meta::external::store::service::showcase::domain::DesignationFilter(designation = $this.designation),
                        idFilters = ^meta::external::store::service::showcase::domain::IdFilter(id = $this.id)
                     )
         )
     )
  }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::EmployeeServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup EmployeeServices
   (
      path : '/employees';

      Service EmployeeDetailsByFilters
      (
         path : '/employeeDetailsByFilters';
         method : POST;
         requestBody : meta::external::store::service::showcase::domain::EmployeeSearchByFilters <- meta::external::store::service::showcase::store::employeeServiceStoreSchemaBinding;
         security : [];
         response : [meta::external::store::service::showcase::domain::Employee <- meta::external::store::service::showcase::store::employeeServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::employeeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::Employee,
                    meta::external::store::service::showcase::domain::Address,
                    meta::external::store::service::showcase::domain::NameFilter,
                    meta::external::store::service::showcase::domain::DesignationFilter,
                    meta::external::store::service::showcase::domain::IdFilter,
                    meta::external::store::service::showcase::domain::EmployeeSearchByFilters ];
}

###Runtime
Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
    connections :
    [
	 � �meta::external::store::service::showcase::store::EmployeeServiceStore :
        [
            connection_1 : meta::external::store::service::showcase::connection::serviceStoreConnection
        ]
    ];
}

###Connection
ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection
{
    store   : meta::external::store::service::showcase::store::EmployeeServiceStore;
    baseUrl : 'http://127.0.0.1:9090';
}

###Service
Service meta::external::store::service::showcase::service::ServiceStoreShowcase
{
    pattern: '/showcase/serviceStoreService';
    owners:
    [
        'abc',
        'xyz'
    ];
    documentation: 'Service demonstrating service store use case';
    autoActivateUpdates: true;
    execution: Single
    {
        query: {firstName:String[1], lastName:String[1], id:String[1], designation:String[1]|meta::external::store::service::showcase::domain::Employee.all()->filter(e | $e.firstName == $firstName && $e.lastName == $lastName && $e.designation == $designation && $e.id == $id)->graphFetch(#{meta::external::store::service::showcase::domain::Employee {firstName,lastName,kerberos,designation,id,age,addresses{street,city,country}}}#)->serialize(#{meta::external::store::service::showcase::domain::Employee{firstName,lastName,kerberos,designation,id,age,addresses{street,city,country}}}#)};
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with path offset

```java
###Pure
import meta::external::store::service::showcase::domain::*;

Class meta::external::store::service::showcase::domain::ApiResponse
{
    metadata  : Metadata[1];
    employees : Person[*];
    firms     : Firm[*];
}

Class meta::external::store::service::showcase::domain::Metadata
{
    noOfRecords : Integer[1];
}

Class meta::external::store::service::showcase::domain::Person
{
    firstName  : String[1];
    lastName   : String[1];
    middleName : String[0..1];
}

Class meta::external::store::service::showcase::domain::Firm
{
    firmName : String[1];
    firmId   : Integer[1];
    address  : Address[*];
}

Class meta::external::store::service::showcase::domain::Address
{
    street : String[1];
}

Association meta::external::store::service::showcase::domain::Employment
{
    employees : Person[*];
    firm      : Firm[0..1];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
    *meta::external::store::service::showcase::domain::Person[person_set]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::EmployeeServiceStore ] EmployeesService
        (
            ~path $service.response.employees
        )
    }

    *meta::external::store::service::showcase::domain::Firm[firm_set]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::EmployeeServiceStore ] EmployeesService
        (
            ~path $service.response.firms
        )
    }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::EmployeeServiceStore
(
   description : 'Showcase Service Store';

   Service EmployeesService
   (
      path     : '/employees';
      method   : GET;
      security : [];
      response : [meta::external::store::service::showcase::domain::ApiResponse <- meta::external::store::service::showcase::store::ApiResponseSchemaBinding];
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::ApiResponseSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [
                    meta::external::store::service::showcase::domain::Metadata,
                    meta::external::store::service::showcase::domain::Person,
                    meta::external::store::service::showcase::domain::Firm,
                    meta::external::store::service::showcase::domain::Address,
                    meta::external::store::service::showcase::domain::ApiResponse
                  ];
}

###Runtime
Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
    connections :
    [
        meta::external::store::service::showcase::store::EmployeeServiceStore :
        [
            connection_1 : meta::external::store::service::showcase::connection::serviceStoreConnection
        ]
    ];
}

###Connection
ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection
{
    store   : meta::external::store::service::showcase::store::EmployeeServiceStore;
    baseUrl : 'http://127.0.0.1:9090';
}

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
        query: |meta::external::store::service::showcase::domain::Person.all()->graphFetch(#{meta::external::store::service::showcase::domain::Person {firstName, lastName, middleName}}#)->serialize(#{meta::external::store::service::showcase::domain::Person {firstName, lastName, middleName}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with union

```java
###Pure
Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
    *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: Operation
    {
        meta::pure::router::operations::special_union_OperationSetImplementation_1__SetImplementation_MANY_(s_trade_set1, s_trade_set2)
    }

    meta::external::store::service::showcase::domain::S_Trade[s_trade_set1]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService1
    }

    meta::external::store::service::showcase::domain::S_Trade[s_trade_set2]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService2
    }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService1
      (
         path : '/allTradesService1';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )

      Service AllTradeService2
      (
         path : '/allTradesService2';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

###Runtime
Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
    connections :
    [
        meta::external::store::service::showcase::store::TradeProductServiceStore:
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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with batching

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

Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

Association meta::external::store::service::showcase::domain::S_Trade_S_Product
{
  s_product : meta::external::store::service::showcase::domain::S_Product[0..1];
  s_trades  : meta::external::store::service::showcase::domain::S_Trade[*];
}


###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
  *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService
  }

  *meta::external::store::service::showcase::domain::S_Product[s_prod_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] ProductServices.ProductByIdService
     (
         ~request
         (
            parameters
            (
                id = $this.s_productId
            )
         )
     )
  }

  meta::external::store::service::showcase::domain::S_Trade_S_Product[s_cross_set]: XStore
  {
    s_product[s_trade_set, s_prod_set]: $this.s_tradeDetails == $that.s_productId
  }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService
      (
         path : '/allTradesService';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )

   ServiceGroup ProductServices
   (
      path : '/products';

      Service ProductByIdService
      (
         path : '/getProductById';
         method : GET;
         parameters :
         (
            id : [ String ] (location = query, style = form, explode = false)   // If parameter of 2nd api takes in a list, platform is smart enough to trigger batching for simple scenarios
         );
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Product <- meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

Binding meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Product, meta::external::store::service::showcase::domain::S_Synonym ];
}

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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId, s_traderDetails, s_tradeDetails,s_product {s_productId,s_productName,s_description}}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId, s_traderDetails, s_tradeDetails,s_product {s_productId,s_productName,s_description}}}#);
		mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with cross store (enriching data with multiple APIs)

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

Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

Association meta::external::store::service::showcase::domain::S_Trade_S_Product
{
  s_product : meta::external::store::service::showcase::domain::S_Product[0..1];
  s_trades  : meta::external::store::service::showcase::domain::S_Trade[*];
}


###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
  *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: ServiceStore
  {
     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService
  }

  *meta::external::store::service::showcase::domain::S_Product[s_prod_set]: ServiceStore
  {
     +s_tradeId: String[1];

     ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] ProductServices.ProductByIdService
     (
         ~request
         (
            parameters
            (
                id = $this.s_tradeId->split(':')->at(0)
            )
         )
     )
  }

  meta::external::store::service::showcase::domain::S_Trade_S_Product[s_cross_set]: XStore
  {
    s_product[s_trade_set, s_prod_set]: $this.s_tradeDetails == $that.s_tradeId
  }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService
      (
         path : '/allTradesService';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )

   ServiceGroup ProductServices
   (
      path : '/products';

      Service ProductByIdService
      (
         path : '/getProductById/{id}';
         method : GET;
         parameters :
         (
            id : String (location = path)
         );
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Product <- meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

Binding meta::external::store::service::showcase::store::prodServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Product, meta::external::store::service::showcase::domain::S_Synonym ];
}

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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId, s_traderDetails, s_tradeDetails,s_product {s_productId,s_productName,s_description}}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId, s_traderDetails, s_tradeDetails,s_product {s_productId,s_productName,s_description}}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with multiple API mappings for the same class

```java
###Pure
Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
    *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService  // This is used when query has no filters

     	~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.TradeByIdService  // This is used when query has filter on s_tradeId
    	(
            ~request
            (
            	parameters
            	(
                    "trade id" = $this.s_tradeId
            	)
            )
     	)
    }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService
      (
         path : '/allTradesService';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )

      Service TradeByIdService
      (
         path : '/{trade id}';
         method : GET;
         parameters :
         (
            "trade id" : String (location = path)
         );
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

###Runtime
Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
    connections :
    [
        meta::external::store::service::showcase::store::TradeProductServiceStore:
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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}

Service meta::external::store::service::showcase::service::ServiceStoreShowcase2
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
        query: |meta::external::store::service::showcase::domain::S_Trade.all()->filter(t | $t.s_tradeId == '123')->graphFetch(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#)->serialize(#{meta::external::store::service::showcase::domain::S_Trade {s_tradeId,s_traderDetails,s_tradeDetails}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```

## Service store example with chained transforms

```java
###Pure
Class meta::external::store::service::showcase::domain::S_Trade
{
  s_tradeId       : String[1];
  s_traderDetails : String[1];
  s_tradeDetails  : String[1];
}

Class meta::external::store::service::showcase::domain::Trade
{
  tradeId    : String[1];
  traderKerb : String[0..1];
  quantity   : Integer[1];
}

Class meta::external::store::service::showcase::domain::Trader
{
  kerberos  : String[1];
  firstName : String[1];
  lastName  : String[1];
}

Association meta::external::store::service::showcase::domain::Trade_Trader
{
  trader : meta::external::store::service::showcase::domain::Trader[1];
  trades : meta::external::store::service::showcase::domain::Trade[*];
}

###Mapping
Mapping meta::external::store::service::showcase::mapping::ServiceStoreMapping
(
    *meta::external::store::service::showcase::domain::S_Trade[s_trade_set]: ServiceStore
    {
        ~service [meta::external::store::service::showcase::store::TradeProductServiceStore] TradeServices.AllTradeService
    }

    meta::external::store::service::showcase::domain::Trade[trade_set]: Pure
    {
        ~src meta::external::store::service::showcase::domain::S_Trade

    	tradeId: $src.s_tradeId,
    	quantity: $src.s_tradeDetails->split(':')->at(1)->parseInteger(),
    	trader[trader_set]: $src
    }

    meta::external::store::service::showcase::domain::Trader[trader_set]:Pure
    {
    	~src meta::external::store::service::showcase::domain::S_Trade

    	kerberos  : $src.s_traderDetails->split(':')->at(0),
    	firstName : $src.s_traderDetails->split(':')->at(1),
    	lastName  : $src.s_traderDetails->split(':')->at(2)
    }
)

###ServiceStore
ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore
(
   description : 'Showcase Service Store';

   ServiceGroup TradeServices
   (
      path : '/trades';

      Service AllTradeService
      (
         path : '/allTradesService';
         method : GET;
         security : [];
         response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];
      )
   )
)

###ExternalFormat
Binding meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding
{
  contentType   : 'application/json';
  modelIncludes : [ meta::external::store::service::showcase::domain::S_Trade ];
}

###Runtime
Runtime meta::external::store::service::showcase::runtime::ServiceStoreRuntime
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
    connections :
    [
        meta::external::store::service::showcase::store::TradeProductServiceStore:
        [
            connection_1 : meta::external::store::service::showcase::connection::serviceStoreConnection
        ],
        ModelStore :
        [
            connection_2 : meta::external::store::service::showcase::connection::modelChainConnection
        ]
    ];
}

###Connection
ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection
{
    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;
    baseUrl : 'http://127.0.0.1:9090';
}

ModelChainConnection meta::external::store::service::showcase::connection::modelChainConnection
{
    mappings :
    [
        meta::external::store::service::showcase::mapping::ServiceStoreMapping
    ];
}

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
        query: |meta::external::store::service::showcase::domain::Trade.all()->graphFetch(#{meta::external::store::service::showcase::domain::Trade {tradeId,quantity,trader {kerberos,firstName,lastName}}}#)->serialize(#{meta::external::store::service::showcase::domain::Trade {tradeId,quantity,trader {kerberos,firstName,lastName}}}#);
        mapping: meta::external::store::service::showcase::mapping::ServiceStoreMapping;
        runtime: meta::external::store::service::showcase::runtime::ServiceStoreRuntime;
    }
    testSuites: <<To be Added>>
}
```
