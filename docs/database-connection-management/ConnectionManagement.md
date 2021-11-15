# Connection State Management

Database connections are acquired in four layers

1/ The first layer is the RelationalConnectionManager/Selector

- This layers receives a request for a database connection.
- Using the specification of the datasource and the authentication strategy, this layer computes a "credential supplier".
- Note : We compute only a supplier object and not the actual credential. The credential is not computed till later down the line.
- This credential supplier and the caller's identity are passed to the next layer.

2/ The second layer is the DatasourceSpecification 

- This layer receives the identity and the credential supplier from the previous layer.
- The very first time this layer is invoked for a specific user + datasource + auth strategy, it computes a Hikari connection pool. Subsequent requests for the same user + datasource + auth strategy, reuse the pool 
- When the Hikari connection pool is constructed, this layer stores the identity and credential supplier in a "state manager". This state is identified by a key which is also added to the Hikari datasource properties.

3/ The third layer is the Driver wrapper 

- The Hikari connection pool asynchronously creates connections in its own threads
- Connection creation requires the caller identity and database credentials 
- To make these available to the connection pool, we intercept the connection creation process by wrapping the Jdbc driver in a DriverWrapper 
- The DriverWrapper's getConnection invokes the next layer 

4/ The fourth layer is the Authentication Strategy 

- This layer actually computes the the database credential that is passed to the database
- This layer fetches the identity and credential supplier cached in layer two and creates a credential 
- The credential is then added back to the Jdbc properties such that when control return to Hikari, it can pass the credentials to the database  