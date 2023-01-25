###Service
Service foo::service::customerService
{
  pattern: '/api/customers';
  documentation: '';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |foo::domain::Customer.all()->project([x|$x.id, x|$x.name], ['Id', 'Name']);
    mapping: foo::mapping::customer;
    runtime: foo::runtime::bq;
  }
  test: Single
  {
    data: '';
    asserts:
    [
    ];
  }
}

Service foo::service::thingService
{
  pattern: '/api/things';
  documentation: '';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |foo::domain::Thing.all()->project([t|$t.id, t|$t.value], ['Id', 'Value']);
    mapping: foo::mapping::thing;
    runtime: foo::runtime::bq;
  }
  test: Single
  {
    data: '';
    asserts:
    [
    ];
  }
}

Service foo::service::firmService
{
  pattern: '/api/firm1Persons';
  documentation: '';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |foo::domain::Firm.all()->filter(f|$f.id == 1).employees->project([p|$p.name, p|$p.firmId], ['Name', 'FirmId']);
    mapping: foo::mapping::firmperson;
    runtime: foo::runtime::bq;
  }
  test: Single
  {
    data: '';
    asserts:
    [
    ];
  }
}

Service foo::service::personService
{
  pattern: '/api/persons';
  documentation: '';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |foo::domain::Person.all()->project([x|$x.name, x|$x.firmId], ['Name', 'FirmId']);
    mapping: foo::mapping::firmperson;
    runtime: foo::runtime::bq;
  }
  test: Single
  {
    data: '';
    asserts:
    [
    ];
  }
}


###Relational
Database foo::database::bq
(
  Schema dataset1
  (
    Table CUSTOMER
    (
      ID VARCHAR(25) PRIMARY KEY,
      NAME VARCHAR(200)
    )
    Table PERSON
    (
      NAME VARCHAR(200) PRIMARY KEY,
      FIRMID INTEGER
    )
    Table FIRM
    (
      ID INTEGER PRIMARY KEY,
      LEGALNAME VARCHAR(200) PRIMARY KEY
    )
  )

  Table THING
  (
    ID INTEGER PRIMARY KEY,
    VALUE VARCHAR(200)
  )

  Join Firm_Person(dataset1.PERSON.FIRMID = dataset1.FIRM.ID)
)


###Pure
Class foo::domain::Customer
{
  id: String[1];
  name: String[1];
}

Class foo::domain::Person
{
  name: String[1];
  firmId: Integer[1];
}

Class foo::domain::Firm
{
  id: Integer[1];
  legalName: String[1];
  employees: foo::domain::Person[*];
}

Class foo::domain::Thing
{
  id: Integer[1];
  value: String[1];
}


###Mapping
Mapping foo::mapping::customer
(
  foo::domain::Customer: Relational
  {
    ~primaryKey
    (
      [foo::database::bq]dataset1.CUSTOMER.ID
    )
    ~mainTable [foo::database::bq]dataset1.CUSTOMER
    id: [foo::database::bq]dataset1.CUSTOMER.ID,
    name: [foo::database::bq]dataset1.CUSTOMER.NAME
  }
)

Mapping foo::mapping::firmperson
(
  foo::domain::Person: Relational
  {
    ~primaryKey
    (
      [foo::database::bq]dataset1.PERSON.NAME
    )
    ~mainTable [foo::database::bq]dataset1.PERSON
    name: [foo::database::bq]dataset1.PERSON.NAME,
    firmId: [foo::database::bq]dataset1.PERSON.FIRMID
  }
  foo::domain::Firm: Relational
  {
    ~primaryKey
    (
      [foo::database::bq]dataset1.FIRM.ID
    )
    ~mainTable [foo::database::bq]dataset1.FIRM
    id: [foo::database::bq]dataset1.FIRM.ID,
    legalName: [foo::database::bq]dataset1.FIRM.LEGALNAME,
    employees[foo_domain_Person]: [foo::database::bq]@Firm_Person
  }
)

Mapping foo::mapping::thing
(
  foo::domain::Thing: Relational
  {
    ~primaryKey
    (
      [foo::database::bq]THING.ID
    )
    ~mainTable [foo::database::bq]THING
    id: [foo::database::bq]THING.ID,
    value: [foo::database::bq]THING.VALUE
  }
)


###Connection
RelationalDatabaseConnection foo::connection::bq
{
  store: foo::database::bq;
  type: BigQuery;
  specification: BigQuery
  {
    projectId: 'central-eon-311221';
    defaultDataset: 'dataset1';
  };
  auth: GCPApplicationDefaultCredentials;
}


###Runtime
Runtime foo::runtime::bq
{
  mappings:
  [
    foo::mapping::customer
  ];
  connections:
  [
    foo::database::bq:
    [
      connection: foo::connection::bq
    ]
  ];
}

Runtime foo::runtime::firmperson
{
  mappings:
  [
    foo::mapping::firmperson
  ];
  connections:
  [
    foo::database::bq:
    [
      connection: foo::connection::bq
    ]
  ];
}
