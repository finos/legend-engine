# Key Concepts / Glossary
## Pure Runtime
The runtime of the platform. This runtime performs the evaluation of Pure code to either execute it or cross-compile it to
be executed in target runtimes.

## Target Runtime
Runtimes external to the platform to which we cross-compile pure code for execution. The cross-compiled code is in the language
of the target platform and executes in the target runtime.

## Native Function
A Native function has no implementation in Pure. The Pure runtime depends on the "native" implementation of the function 
in order to cross-compile or execute it.

### Native function in Pure Runtime
These are defined using Java, the "native" language of the platform. They have no Pure-language definition.

### Native Functions in Target Runtimes
These are the functions of the target runtime. For example, the "timeBucket" platform function has equivalent functions in target databases.
However, the function names/syntax/implementation in the target are specific to the target. E.g. the function name in DuckDb is *time_bucket* and
the function name in Snowflake is *TIME_SLICE*. The platform will use the database-specific, or "native," function for the cross-compiled code.
See [Routing](#routing) below for details.

##### Functional Correctness
PCT / Pure cares about the functional correctness against targets, rather than about the implementation. What does this mean?
It means that the function should behave the same way in target runtimes as it does on the platform runtime. A good example of this is
the function ```meta::pure::functions::math::pi():Float[1]```. This function returns pi to some precision. If calling pi on any target
platform, the **same** precision result should be returned by the target platform. Even though equivalent function could be 
implemented to return a different precision on some target database, the cross-compiled code is expected to return the same result as
that which the platform function returns. Aka, the cross-compiled code is expected to be functionally correct. 
It is for this reason that we do not stop routing this function.
See [Routing](#routing) for details on routing.

## Routing
A *Pure Function* can be thought of as an expression tree in which the leaves of the tree are either Constants or native functions. The Pure Router will keep flattening the tree until it reaches one of three types of leaves:
1. Constant Value
2. Stop Routing exclusion (configured in ```legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/routing/router_routing.pure```)
3. Native Function

For cases 2. and 3. above, the routing will then be passed to the pureToTarget binding (e.g. pureToSQL, pureToJava, pureToElastic, etc.). The pureToTarget code will be expected to provide instructions on how to handle those functions which are Native or for which there was a stop routing exclusion.

### Router-Deepdive
The router traverses/flattens the Pure Function expression tree. As it does this, it analyzes whether a function can be simplified. It will decide whether to pass the *function composition* (flattened/optimized expression) to the database/target OR to push the calculation down to the database/target itself.

##### Example
For example, when adding the "between" pure function (see [Non-native How-To](purefunction-howto) for full walkthrough), we
can see that, for the same Pure code:

```Java
meta::pure::functions::boolean::between(value:Number[0..1], lower:Number[0..1], upper:Number[0..1]):Boolean[1]
{
  ($value >= $lower) && ($value <= $upper);
}
```

###### stop routing cross-compiled code
if we add the function signatures to ```function meta::pure::router::routing::shouldStopFunctions``` in router_routing.pure, this
cross-compiled SQL is generated for DuckDb:
```Sql
select -10000003 between -10000002 and -999999
```
As you can see, stop routing configs cause the implementation of the function to be delegate to the target runtime (DuckDb)

###### fully routed cross-compiled code
if we *do not* put any stop routing config in router_routing.pure, this SQL
cross-compiled SQL is generated for DuckDb:
```Sql
select (((-10000003 is not null and -10000002 is not null) and -10000003 >= -10000002) and ((-10000003 is not null and -999999 is not null) and -10000003 <= -999999))
```

##### What is the difference between pushing the function expression composition (fully routed) vs the function (without fully routing) to the target runtime?
When you push down the function - you call the function natively in the target runtime and this may pick up any optimizations that the native runtime has defined for the function. In doing so, you are also subject to the target-specific implementation of that runtime. This is where PCT provides value in ensuring functional correctness.

------
# FAQ
### How do I know if a new platform function should be implemented in Java vs Pure?
The default should be to implement the new function in Pure.
In certain cases, e.g. acosine, it may make more sense to implement the function in Java to leverage underlying library functions not currently exposed via Pure.