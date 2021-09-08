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
    runtime: foo::runtime::spanner;
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
    runtime: foo::runtime::spanner;
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
    runtime: foo::runtime::spanner;
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
    runtime: foo::runtime::spanner;
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
Database foo::database::spanner
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
    Table THING
    (
      ID INTEGER PRIMARY KEY,
      VALUE VARCHAR(200)
    )

  Join Firm_Person(PERSON.FIRMID = FIRM.ID)
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
      [foo::database::spanner]CUSTOMER.ID
    )
    ~mainTable [foo::database::spanner]CUSTOMER
    id: [foo::database::spanner]CUSTOMER.ID,
    name: [foo::database::spanner]CUSTOMER.NAME
  }
)

Mapping foo::mapping::firmperson
(
  foo::domain::Person: Relational
  {
    ~primaryKey
    (
      [foo::database::spanner]PERSON.NAME
    )
    ~mainTable [foo::database::spanner]PERSON
    name: [foo::database::spanner]PERSON.NAME,
    firmId: [foo::database::spanner]PERSON.FIRMID
  }
  foo::domain::Firm: Relational
  {
    ~primaryKey
    (
      [foo::database::spanner]FIRM.ID
    )
    ~mainTable [foo::database::spanner]FIRM
    id: [foo::database::spanner]FIRM.ID,
    legalName: [foo::database::spanner]FIRM.LEGALNAME,
    employees[foo_domain_Person]: [foo::database::spanner]@Firm_Person
  }
)

Mapping foo::mapping::thing
(
  foo::domain::Thing: Relational
  {
    ~primaryKey
    (
      [foo::database::spanner]THING.ID
    )
    ~mainTable [foo::database::spanner]THING
    id: [foo::database::spanner]THING.ID,
    value: [foo::database::spanner]THING.VALUE
  }
)


###Connection
RelationalDatabaseConnection foo::connection::spanner
{
  store: foo::database::spanner;
  type: Spanner;
  specification: Spanner
  {
    projectId: 'central-eon-311221';
    instanceId: 'instance1';
    databaseId: 'database1';
  };
  auth: GCPApplicationDefaultCredentials;
}


###Runtime
Runtime foo::runtime::spanner
{
  mappings:
  [
    foo::mapping::customer
  ];
  connections:
  [
    foo::database::spanner:
    [
      connection: foo::connection::spanner
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
    foo::database::spanner:
    [
      connection: foo::connection::spanner
    ]
  ];
}
