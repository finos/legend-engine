# Example Setup


## Domain Model
###Pure


>`Class demo::domain::Person` \
`{` \
    `firstName: String[1];`\
    `lastName: String[1];`\
    `otherNames: String[*];`\
`}`

`Class demo::domain::Firm
`{
`id: Integer[1];
`legalName: String[1];
`employees: foo::domain::Person[*];
`}


## Store Model

>`###DocumentStore` \
`Database demo::database::Mongo` \
`(` \
    `&nbsp;&nbsp;&nbsp;&nbsp;Document Firm` \
    `&nbsp;&nbsp;&nbsp;&nbsp;(`\
    `&nbsp;&nbsp;&nbsp;&nbsp;_id: objectId;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;firmId: long;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;name: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;location: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;address : object (`\
    `&nbsp;&nbsp;&nbsp;&nbsp;    city: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;    country: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;)`\
    `&nbsp;&nbsp;&nbsp;&nbsp;employee: employeeNode;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;govIdentifier: array( object  ( type: string, value: string: country: string ) )`\
    `&nbsp;&nbsp;&nbsp;&nbsp;)`\
    `&nbsp;&nbsp;&nbsp;&nbsp;DocumentFragment employeeNode`\
    `&nbsp;&nbsp;&nbsp;&nbsp;(`\
    `&nbsp;&nbsp;&nbsp;&nbsp;_id: objectId;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;dept: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;name: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;email: string;`\
    `&nbsp;&nbsp;&nbsp;&nbsp;)`\
`)`\
