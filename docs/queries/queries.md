<h1 align="center">QUERIES</h1>

# Overview

A query provides a means of interrogating data, processing it and preparing it into a resultant
form for consumption.  Data can originate from:

* A `Store` which holds data and are modelled in Legend.  A `Runtime` provides the `Connection` details
  for accessing the `Store`.
* A `Binding` which describes how to deserialize external data that is provided either directly or in the form
  of a URL which states how to access the data.
* Some combinations of the above.

A query is expressed as a lambda function.
The lambda may accept parameters.
If it does these will need to be supplied at runtime.
This can be done for example:

* When executing as a Service on an engine server by describing how the parts of the
  HTTP call relate to the query parameters.
* When executing using `ServiceRunner` and a Service generated JAR by passing parameters 
  into the `ServiceRunner`. 

Function calls are chained to make up the expression in the lambda that specifies what that result should be (i.e. the query is stated as an expression consisting of a chain of function calls).
The result of the lambda is the result of the query.  This can be one of the following:

* A serialization of the queried objects.
* A TabularDataSet (TDS).
* A serialization of a TDS.
* A collection of PURE objects reified to the bound platform (not currently supported).

:question: How should services handle Activities in light of ByteStream responses?

When support is added for the case where objects are returned directly in the bound platform format they must be 
fully verified to comply with datatypes, multiplicities and constraints.

In order to execute a query the lambda function is analysed by a routing algorithm to determine which
functional units (systems), the `Stores` and Platform features, will be responsible for which parts of the query.  From this
analysis an execution plan is created.  Lastly the execution plan is bound to a particular implementation
of the Platform (currenly only a Java platform exists) before being executed with that implementation.

The Java platform allows execution either as an JAR file that is build during an SDLC project's pipeline
(along with dependencies on the engine libraries) or via HTTP to an instance of the engine running as
a server.

# Specification

## Routing

Routing is an algorithm the purpose of which is to examine a query and supporting information (Mapping, Runtime and
ExecutionContext) in order to determine how it can be performed using the capabilities of the platform and the
Stores which hold data.

Function routing takes the user query and analyses it into a sequence of cluster functions.
Each cluster function represents a set of work to be performed by a functional unit.
The functional units are Stores or the platform itself.

Function routing is possible with or without an externally supplied mapping and runtime (external to the function being routed):

``` pure
function meta::pure::router::routeFunction(f:FunctionDefinition<Any>[1], 
                                           exeCtx: ExecutionContext[1], 
                                           extensions:meta::pure::router::extension::RouterExtension[*], 
                                           debug:DebugContext[1]
                                          ):RoutingResult[1]

// DEPRECATED
function meta::pure::router::routeFunction(f:FunctionDefinition<Any>[1], 
                                           mapping:Mapping[1], 
                                           runtime:Runtime[1], 
                                           exeCtx: ExecutionContext[1], 
                                           inScopeVars:Map<String, List<Any>>[0..1], 
                                           extensions:meta::pure::router::extension::RouterExtension[*], 
                                           debug:DebugContext[1]
                                          ):RoutingResult[1]
```

### Phase 1 - Mapping allocation

The AST of the query function is walked to determine how to fulfil the query.  The most significant components
are the functions used.  Any native function used must have its purpose fulfilled by the platform or a Store.
Non-native functions can also have their purposes fulfilled by the platform or a Store.
Alternatively their implementation in Pure is examined as part of the AST walk to determine how to achieve
their purpose from their definition.  This allows for the use of user defined functions that are not known
to the algorithm.

Each ValueSpecification in the AST that relates to a `SetImplementation` (such as a class mapping) is decorated
by being wrapped into an `ExtendedRoutingValueSpecification` so that the mapping and set(s) are attached.
For example a property access will be assoicated if there's a mapping in scope.  Mappings are in scope
if they are passed in (see deprecated entry above) or are introduced by a `from()` expression.

### Phase 2 - Set permutation

Sets discovered in phase 1 can form permutation sets.  That is there may be one or more cases where multiple
sets could be used.  This phase works out the possible set of permutations that involve only one set from each
permutation set.

### Phase 3 - Build

The expressions are rebuild using only the sets available for each permutation.  If the full query cannot be
satisfied (for example a set is missing a mapping for some required property) then that built expression is
discarded.

### Phase 4 - Clustering

The AST ofthe built functions is now walked to further decorate the ValueSpeciciations.  This time they may be wrapped 
with a `ClusteredValueSpecification` which identifies whether it is to be satisfied by a `Store` (and if so which one)
or by the platform.  The capabilities of each functional unit (Store or Platform) is represented by a
`SystemMapping`.

## Query elements

### all()
``` pure
Person.all()
```
Used to indicate the universe of model objects that the query is interrogating.
It is used as a derived property of the relevant model class.
In the example shown `Person` is the class.
The result is all available instances of the `Person` class.
In most cases this will require further qualification to indicate how those instances are realized.

### from()
The use of `from` is best understood as limiting the object collection.  For example:
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
}
```
can be read as query all `Person` objects *that are realizable from the specified Store(s)*.

The positioning of the `from` function in the query is used to indicate the bounds of
functions to be negotiated with a Store.  For example:
``` pure
{|
   Person.all()
      ->filter(p| $p.firstName == 'Peter')
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
}
```
implies that the `filter` is available for implementation by a Store.  Whereas:
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
      ->filter(p| $p.firstName == 'Peter')
}
```
implies that the `filter` should be performed by the platform.

#### Signatures
```pure
from<T|m>(T[m], Mapping[1], Runtime[1]): T[m]
```
To indicate how instances can be realized from Store(s).

Parameters:
* The set of objects not yet known to be realizable
* A Mapping - which specifies how model objects can be created from Stores
* A Runtime - which provides connectivity details for accessing Stores

```pure
from(TabularDataSet[1], Mapping[1], Runtime[1]): TabularDataSet[1]
```
Used to indicate how instances that are used to populate a TDS can be realized from Store(s).

Parameters:
* The TDS not yet known to be realizable
* A Mapping - which specifies how model objects can be created from Stores
* A Runtime - which provides connectivity details for accessing Stores

### filter()
Used to specify criteria for inclusion of objects within the query results.

#### Signatures
``` pure
filter<T>(T[*], Function<{T[1]->Boolean[1]}>[1]): T[*]
```
Used to limit an object query.

Parameters:
* The object collection formed by the query so far
* A lambda that acts as a predicate to determine which objects are included.

``` pure
filter(TabularDataSet[1], Function<{TDSRow[1]->Boolean[1]}>[1]): TabularDataSet[1]
```
Used to limit a query in TDS form.

Parameters:
* The TDS
* A lambda that acts as a predicate to determine which rows of the TDS are included.

### internalize()
Used to deserialize data that is described by a Binding from its external representation to an object collection.

#### Signatures
``` pure
internalize<T>(Binding[1], Class<T>[1], Url[1]): T[*]
```
To deserialize source data that is available at some URL.

Parameters:
* The Binding which defines how the data can be deserialized
* The Class which will be realized by the Binding from the source data
* The URL at which the data is located.

``` pure
internalize<T>(Binding[1], Class<T>[1], ByteStream[1]): T[*]
```
same as `internalize<T>($binding, $class, $byteStream, 'UTF-8')` (see below).

``` pure
internalize<T>(Binding[1], Class<T>[1], ByteStream[1], String[1]): T[*]
```
_(Not yet implemented.)_
To deserialize source data that is made available in the form of a stream.

Parameters:
* The Binding which defines how the data can be deserialized
* The Class which will be realized by the Binding from the source data
* The ByteStream which contains the data.
* The character encoding to interpret bytes to characters.

``` pure
internalize<T>(Binding[1], Class<T>[1], String[1]): T[*]
```
To deserialize source data that is held in a String.

Parameters:
* The Binding which defines how the data can be deserialized
* The Class which will be realized by the Binding from the source data
* The String which contains the data.

### externalize()
Used to serialize data that is described by a Binding from an object collection to its external representation.
The supplied graph fetch tree determines the scope of the data to be serialized.
To better handle checked results a number of features will be added to [Graph Fetch Trees](#graph-fetch-trees)  

#### Signatures
``` pure
externalize<T>(T[*], Binding[1], RootGraphFetchTree<T>[1]): ByteStream[1]
```
Parameters:
* The objects to be serialized
* The Binding which defines how the data can be serialized
* A graph fetch tree which defines the data to be serialized per object in the input collection

### zip() unzip()  encode() decode()
Functions that operate on ByteStreams (in either direction):
``` pure
my::CsvPersonBinding
   ->internalize(my::Person, $data->unzip()->decode(EncryptionScheme.DES, $desKey), 'UTF-8')
   ->externalize(my::JsonPersonBinding, 'windows-1251')->encode(EncryptionScheme.AES, $aesKey)->zip();
```

### checked() graphFetch() graphFetchChecked() graphFetchUnexpanded() graphFetchCheckedUnexpanded()

These functions are used to indicate the scope of data fetching, the validations that should be performed
and whether the query should return Checked results or should fail.  

Checked results wrap the values returned by the query in a structure that allows defects to be reported
in a streaming manner.  Without Checked results the first error encounters causes the query to fail.
The structure of `Checked` and the `Defect`s they can contain are:

``` pure
Class meta::pure::dataQuality::Checked<T>
{
  defects : Defect[*];
  source  : Any[0..1];
  value   : T[0..1];
}

Class meta::pure::dataQuality::Defect
{
   id               : String[0..1];
   externalId       : String[0..1];
   message          : String[0..1];
   enforcementLevel : EnforcementLevel[0..1];
   ruleDefinerPath  : String[1];
   ruleType         : RuleType[1];
   path             : RelativePathNode[*];
}
```

`Checked` wraps each object in the collection.  It gives the set of defects related to each object and the
object itself (`value`) if it is still able to construct it (that is if the defects are not serious enough
to inhibit construction).  Defects that inhibit construction are marked with an enforcement level of
`Critical`. Additionally, the `Checked` carries the source from which the object is derived, for example
a line of a CSV file.  If the source is via a model-to-model mapping then the source itself will be another
`Checked` detailing the construction of that source object.

When evaluating defects there maybe a graph of objects reachable from the root objects of the collection
on which defects may be detected (see below).  Each `Defect` in the `Checked` includes a `path` which
describes the navigation from the root object to the  object to which the `Defect` applies.

The choice of function determines the scope of defect detection and whether the query returns `Checked`
results or fails on error (a `Defect` of severity `Error` or `Critical`):

| Scope                                                                               | Fail on error          | Checked results               |
|-------------------------------------------------------------------------------------|------------------------|-------------------------------|
| root object only                                                                    | _none_                 | `checked`                     |
| properties in supplied tree                                                         | `graphFetchUnexpanded` | `graphFetchCheckedUnexpanded` |
| properties in supplied tree and any used in constraints on the classes in the tree  | `graphFetch`           | `graphFetchChecked`           |      

Constraints are evaluated according to the properties available. That is if a constraint uses properties stated 
within the query. 
* In the case of `graphFetchUnexpanded`/`graphFetchCheckedUnexpanded` those are the properties
  as stated in the tree (including defaulted simple properties when appropriate - see [Graph Fetch Trees](#graph-fetch-trees)).
* For `graphFetch`/`graphFetchChecked` the given trees are expanded to include any properties which are used in constraints
  but which have not been explicitly stated in the given trees
* For _none_/`checked` constraints are evaluated if the query implies fetching those properties.  That is, when
  when partial classes are adequate to answer the query constraints are only evaluated if their properties are fetched. 

#### Signatures

``` pure
checked<T>(T[*]): Checked<T>[*]
```
To produce Checked instances of `T` (a full or partial class) where valid means meets the multiplicities
and constraints defined by `T` (or by a partial of `T`).  Any child objects will not have constraints 
or multiplicities validated.

Parameters:
* The collection of objects to be checked 

``` pure
graphFetchCheckedUnexpanded<T>(T[*], RootGraphFetchTree<T>[1]): Checked<T>[*]
```
To produce Checked instances of `T` (a full or partial class) and all child objects of the instances as
described in the GFT supplied.

Parameters:
* The collection of objects to be checked
* The Graph Fetch Tree that describes the extent of the checks to be performed.

``` pure
graphFetchUnexpanded<T>(T[*], RootGraphFetchTree<T>[1]): T[*]
```
To produce instances of `T` (a full or partial class) and all child objects of the instances as
described in the GFT supplied.  The query will fail if an error is encountered

Parameters:
* The collection of objects to be checked
* The Graph Fetch Tree that describes the extent of the checks to be performed.

``` pure
graphFetchChecked<T>(T[*], RootGraphFetchTree<T>[1]): Checked<T>[*]
```
As per `graphFetchCheckedUnexpanded` however any partial classes are expanded to include all properties necessary
to evaluate all constraints of the classes being verified.

Parameters:
* The collection of objects to be checked
* The Graph Fetch Tree that describes the extent of the checks to be performed.

``` pure
graphFetch<T>(T[*], RootGraphFetchTree<T>[1]): T[*]
```
As per `graphFetchUnexpanded` however any partial classes are expanded to include all properties necessary
to evaluate all constraints of the classes being verified.

Parameters:
* The collection of objects to be checked
* The Graph Fetch Tree that describes the extent of the checks to be performed.

### Graph Fetch Trees
Graph fetch trees (which appear in function signatures as `RootGraphFetchTree<T>[1]`) are used to
describe an object graph structure.

The simplest tree defines a root class and an empty list of properties:
``` pure
#{Person {}}#
```
This is interpreted as meaning object of that class and all of its simple (primitive and enum) properties.
_(The parser currently doesn't allow this form but it should.)_
If one or more simple properties are given then only those properties will be included:
``` pure
#{Person {firstName}}#
```
If complex (class) properties are given then they are further qualified by the properties of that class
following the same rules recursively:
``` pure
#{Person {
  firstName, 
  firm {
    address {
      firstLine, 
      secondLine
    }
  }
}#
```
It is also possible to specify behaviour for different subtypes of a property:
``` pure
#{Person
  {
    firstName, 
    firm 
    {
      address->subType(@TraditionalAddress) 
      {
        firstLine, 
        secondLine
      },
      address->subType(@WhatThreeWordsAddress) 
      {
        words
      }
    }
  }
}#
```

If for a property both subtype-specific and non-subtype-specific sub trees are defined the
appropriate tree is selected and used exclusively.  For example
``` pure
#{Person {
    firstName, 
    firm { name },
    firm->subType(@Incorporation) { legalName, registration },
    firm->subType(@SoleTrader) { owner },
  }
}#
```
Will use `legalName` and `registration` if the `firm` is an `Incorporation`; `owner` if `firm` is a `SoleTrader`;
and `name` for any other instances.

#### Graph Fetch Trees as Packageable Elements (TBD)
To allow for reuse trees should be definable as packageable elements.
This in conjunction with [Graph Fetch Trees inclusion](graph-fetch-trees-inclusion) allows for reuse and the 
definition of standard trees (pacakable element trees defined in core).
Standard trees should be defined for describing Defects and each of the possible input records of binding formats.

``` pure
GraphFetchTree my::GFT
#{ ... }#
```

#### Graph Fetch Trees inclusion
Tree reuse is provided by allowing the inclusion predefined trees.  Predefined trees are either
packageable element trees or variables: 

``` pure
let aTree = #{...}#
#{ ClassA
   {
      property1 {...},
      property2 my::GFT,
      property3 $aTree
   } 
}#
```

It's also possible to use parameterization to allow predefined trees to have broader reuse:
Graph Fetch Trees inclusion

``` pure
// Given:

GraphFetchTree standard::trees::Defect
#{ Defect {...} }#

GraphFetchTree standard::trees::Checked
#{(value, source) 
  Checked 
  {
     defects standard::trees::Defect,
     value   $value,
     source  $source
  } 
}#

GraphFetchTree standard::trees::FlatDataRecord
#{ FlatDataRecord {...} }#

// And then used as:
{|
  let tree = #{ Person { firstName, lastName }}#;
  
  internalize(FlatDataBinding, Person, ^Url(url='...'))
    ->graphFetchChecked($tree)
    ->externalize(Binding2, standard::trees::Checked($tree, standard::trees::FlatDataRecord); 
}
```

#### Graph Fetch Trees root subTypes
Currently subtypes are not distinguishable at the root level of the tree.  This should change: 

``` pure
#{
  Person { ... }
  subType(@Employee) {...}
  subType(@Customer) {...}
}#
```

## Execution plans

Execution plans are a means of preserving routing decisions as a graph of ExecutionNodes.
The nodes of the plan describe how the features of the platform and the Stores which hold data are to be used to perform the query.
The plan can then be used to repeatedly perform the query.

Preparing an execution plan for execution on the engine first calls function routing.
Each cluster function returned is converted into a set of execution nodes (see execution planning) and then unified into a
`SequenceExecutionNode` to form the `ExecutionPlan`.
(For a single cluster this is optimized so that the `ExecutionPlan` only contains the execution nodes for that one cluster.)

Each cluster is planned using the `planExecution` function from its `SystemMapping`.  These functions will walk the
AST for their expressions and generate Execution nodes that fulfil those features on the functional unit (Store or
Platform) that they represent. 

Entry points are:
``` pure
function meta::pure::executionPlan::executionPlan(f:FunctionDefinition<Any>[1], 
                                                  context:ExecutionContext[1], 
                                                  extensions:meta::pure::router::extension::RouterExtension[*], 
                                                  debugContext:DebugContext[1]
                                                 ):ExecutionPlan[1]
```

``` pure
// DEPRECATED
function meta::pure::executionPlan::executionPlan(f:FunctionDefinition<Any>[1], 
                                                  m:Mapping[1], 
                                                  runtime:Runtime[1], 
                                                  context:ExecutionContext[1], 
                                                  extensions:meta::pure::router::extension::RouterExtension[*], 
                                                  debugContext:DebugContext[1]
                                                 ):ExecutionPlan[1]
```

### Execution nodes

:crayon: TODO describe execution nodes

# Examples

## Fetch from relational
_(Object stream return not currently suppored)_
### Query
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
}
```
### Execution Plan
``` yaml
# Relational
  type: Class[my::Person]
  resultSizeRange: 1
  resultColumns: [...]
  sql: select ...
  connection: DatabaseConnection
```
### Explanation
* `from` provides the mapping which in turn identifies the store (`my::Db`)
* `from` also provides the runtime which further associates `my::Db` with a connection
* the connection determines the database type
* This allows construction of the appropriate SQL and thus creation of the execution plan shown

## Fetch from relational with filter and projection
### Query
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
      ->filter(p| $p.firstName == 'Peter')
      ->project(p| $p.firm.legalName, 'Name')
}
```
### Execution Plan
``` yaml
# Relational
  type: TDS[...]
  resultSizeRange: 1
  resultColumns: [...]
  sql: select ...
  connection: DatabaseConnection
```
### Explanation
* `from` provides the mapping which in turn identifies the store (`my::Db`)
* `from` also provides the runtime which further associates `my::Db` with a connection
* the connection determines the database type
* `filter` is pushed down to the store (made part of SQL) since the database supports it
* `project` changes the type to TDS.  The expression in it widens the classes required (and hence mappings involved)
  to include Firm.
* This allows construction of the appropriate SQL and thus creation of the execution plan shown


## Class collection externalized
### Query
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
      ->externalize(my::Binding, #{Person {firstName, lastName)}#)
}
```
### Execution Plan
``` yaml
# Externalize
  type: String
  resultSizeRange: 1Ï
  bindng: my::TdsBinding
  tree: []
    # Relational
      type: Class[my::Person]
      resultSizeRange: *
      resultColumns: [...]
      sql: select ...
      connection: DatabaseConnection
```
### Explanation
* see [Fetch from relational](#fetch-from-relational)
* `externalize` provides a binding which determines the output format associated with the model.  In this case assumed to be XML.
* `externalize` also provides a tree which determines the scope of the serialization
* This allows construction of the execution plan shown


## TDS externalized
### Query
Given the binding:
``` pure
###ExternalFormat
Binding my::TdsBinding
{
   contentType: 'text/csv';
   modelIncludes: meta::pure::tds::TabularDataSet;
}
```
then
``` pure
{|
   Person.all()
      ->from(my::Mapping, ^Runtime(connections=^DatabaseConnection(element=my::Db, ...)))
      ->filter(p| $p.firstName == 'Peter')
      ->project(p| $p.firm.legalName, 'Name')
      ->externalize(my::TdsBinding)
}
```
### Execution Plan
``` yaml
# Externalize
  type: String
  resultSizeRange: 1
  bindng: my::TdsBinding
  tree: []
    # Relational
      type: TDS[...]
      resultSizeRange: 1
      resultColumns: [...]
      sql: select ...
      connection: DatabaseConnection
```
### Explanation
* See [Fetch from relational with filter and projection](#Fetch-from-relational-with-filter-and-projection)
* `externalize` provides the binding between a TDS and an external format.
  In this case `text/csv` associates to a Delimited driver within the FlatData external format.
* This allows construction of the execution plan shown


## Read in JSON and validate
### Query
``` pure
{url:Url[1]|
   my::Binding
      ->internalize(my::Person, $url)
      ->check()          
      ->serialize(#{...}#)
}        
```
### Execution Plan
``` yaml
# Sequence
    # FunctionParametersValidationNode
      funcionParameters: [url:Url]
    # PureExp
      type: String
      resultSizeRange: 1
      expression: serialize(#{...}#)
        # DataQuality:
          type: Checked[Class[my::Person]]
          resultSizeRange: *
          enableConstraints: true
          checked: true
            # Internalize:
              type: Checked[Class[my::Person]]
              resultSizeRange: *
              binding: my::Binding
              class: my::Person
              offset: []
              url: $url
```
### Explanation
* `internalize` specifies a binding and the desired class obtainable from it
* `internalize` also provides the URL from which to read data
* `check` applies multiplicity and constraint checks
* This allows construction of the execution plan shown
  * Internalize includes parsing defects in the Checked result
  * DataQuality further validates successfully parsed objects
  * PureExp (serialize) turns the results into JSON

## Read in JSON and M2M Map
### Query
``` pure
{data:ByteStream[1], key:String[1]|
   let fromJson = my::Binding->internalize(my::Person, $data->decrypt(DES, $key));
   
   LegalEntity.all
      ->graphFetch(#{...}#)
      ->from(my::PersonToLegalEntityMapping, ^Runtime(connections=^ModelConnection(element=^ModelStore(), instances=newMop(pair(Person, list($fromJson)))))) 
      ->externalize(my::SecondBinding, #{...}#)
}
```
### Execution Plan
:crayon: TODO
### Explanation
:crayon: TODO describe
