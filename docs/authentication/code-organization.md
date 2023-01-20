# Code Organization

## Pure metamodel

All the authentication metamodels are housed in the ```legend-engine-xt-authentication-pure``` module.

Authentication protocols tend to be reused in different contexts. 

Keeping all the metamodels in one place encourages re-use.

## Java protocol

All the Java protocols are housed in the ```legend-engine-xt-authentication-protocol``` module.

Just like the Pure metamodels all the protocols are in a single module.

## Grammar 

Parsers, compilers are housed in the ```legend-engine-xt-authentication-grammar``` module.

## Implementation Core 

Code Java classes and implementations are housed in the ```legend-engine-xt-authentication-implementation-code``` module.

These include implementations of credential providers and rules etc. that can be re-used across the platform.

```
Note : This module should be lightweight in terms Java library dependencies to avoid polluting other modules.
```

````
Note : This module also has a few lightweight vault implementations which can be freely used across the platform without bringing in other library dependencies.
````

## Implementation Vault XXX

Code for a specific vault implementation is housed in a ```legend-engine-xt-authentication-vault-xxxx``` module.

Each vault implementation should be in its own module.

## Implementation Package 

Depending on the use case, we might have to split implementation code into a separate module. 

For example the ```legend-engine-xt-authentication-implementation-gcp-federation``` module contains implementation classes for GCP federation.

This module can be used to house classes for both GCP Workload and Workforce federation.



