/*
Reworked to H2 inspired by https://github.com/pthom/northwind_psql, original license information at
https://github.com/pthom/northwind_psql/blob/3d271b23f3357532e63f92ffb10c4f258dfd20af/LICENSE is copied in below:

(The northwind database originates from microsoft : http://northwinddatabase.codeplex.com/license)

# Microsoft Public License (Ms-PL)
Microsoft Public License (Ms-PL)

This license governs use of the accompanying software. If you use the software, you accept this license.
If you do not accept the license, do not use the software.

1. Definitions

The terms "reproduce," "reproduction," "derivative works," and "distribution" have the same meaning
here as under U.S. copyright law.

A "contribution" is the original software, or any additions or changes to the software.

A "contributor" is any person that distributes its contribution under this license.

"Licensed patents" are a contributor's patent claims that read directly on its contribution.

2. Grant of Rights

(A) Copyright Grant- Subject to the terms of this license, including the license conditions and limitations in section 3,
each contributor grants you a non-exclusive, worldwide, royalty-free copyright license to reproduce its contribution,
prepare derivative works of its contribution, and distribute its contribution or any derivative works that you create.


(B) Patent Grant- Subject to the terms of this license, including the license conditions and limitations in section 3, each
contributor grants you a non-exclusive, worldwide, royalty-free license under its licensed patents to make, have made, use,
sell, offer for sale, import, and/or otherwise dispose of its contribution in the software or derivative works of the
contribution in the software.

3. Conditions and Limitations

(A) No Trademark License- This license does not grant you rights to use any contributors' name, logo, or trademarks.

(B) If you bring a patent claim against any contributor over patents that you claim are infringed by the software, your
patent license from such contributor to the software ends automatically.

(C) If you distribute any portion of the software, you must retain all copyright, patent, trademark, and attribution notices
that are present in the software.

(D) If you distribute any portion of the software in source code form, you may do so only under this license by including a
complete copy of this license with your distribution. If you distribute any portion of the software in compiled or object
code form, you may only do so under a license that complies with this license.

(E) The software is licensed "as-is." You bear the risk of using it. The contributors give no express warranties, guarantees
or conditions. You may have additional consumer rights under your local laws which this license cannot change. To the extent
permitted under your local laws, the contributors exclude the implied warranties of merchantability, fitness for a
particular purpose and non-infringement.
*/

drop schema if exists northwind cascade;

create schema northwind;

create table northwind.categories (
  category_id smallint not null,
  category_name varchar(15) not null,
  description varchar(256),
  picture blob
);
create table northwind.customer_customer_demo (
  customer_id varchar(5) not null,
  customer_type_id varchar(5) not null
);
create table northwind.customer_demographics (
  customer_type_id varchar(5) not null,
  customer_desc varchar(256)
);
create table northwind.customers (
  customer_id varchar(5) not null,
  company_name varchar(40) not null,
  contact_name varchar(30),
  contact_title varchar(30),
  address varchar(60),
  city varchar(15),
  region varchar(15),
  postal_code varchar(10),
  country varchar(15),
  phone varchar(24),
  fax varchar(24)
);
create table northwind.employees (
  employee_id smallint not null,
  last_name varchar(20) not null,
  first_name varchar(10) not null,
  title varchar(30),
  title_of_courtesy varchar(25),
  birth_date date,
  hire_date date,
  address varchar(60),
  city varchar(15),
  region varchar(15),
  postal_code varchar(10),
  country varchar(15),
  home_phone varchar(24),
  extension varchar(4),
  photo blob,
  notes clob,
  reports_to smallint,
  photo_path varchar(255)
);
create table northwind.employee_territories (
  employee_id smallint not null,
  territory_id varchar(20) not null
);
create table northwind.order_details (
  order_id smallint not null,
  product_id smallint not null,
  unit_price real not null,
  quantity smallint not null,
  discount real not null
);
create table northwind.orders (
  order_id smallint not null,
  customer_id varchar(5),
  employee_id smallint,
  order_date date,
  required_date date,
  shipped_date date,
  ship_via smallint,
  freight real,
  ship_name varchar(40),
  ship_address varchar(60),
  ship_city varchar(15),
  ship_region varchar(15),
  ship_postal_code varchar(10),
  ship_country varchar(15)
);
create table northwind.products (
  product_id smallint not null,
  product_name varchar(40) not null,
  supplier_id smallint,
  category_id smallint,
  quantity_per_unit varchar(20),
  unit_price real,
  units_in_stock smallint,
  units_on_order smallint,
  reorder_level smallint,
  discontinued int not null
);
create table northwind.region (
  region_id smallint not null,
  region_description varchar(60) not null
);
create table northwind.shippers (
  shipper_id smallint not null,
  company_name varchar(40) not null,
  phone varchar(24)
);
create table northwind.suppliers (
  supplier_id smallint not null,
  company_name varchar(40) not null,
  contact_name varchar(30),
  contact_title varchar(30),
  address varchar(60),
  city varchar(15),
  region varchar(15),
  postal_code varchar(10),
  country varchar(15),
  phone varchar(24),
  fax varchar(24),
  homepage varchar(256)
);
create table northwind.territories (
  territory_id varchar(20) not null,
  territory_description varchar(60) not null,
  region_id smallint not null
);
create table northwind.us_states (
  state_id smallint not null,
  state_name varchar(100),
  state_abbr varchar(2),
  state_region varchar(50)
);
insert into northwind.categories
values (
  1,
  'Beverages',
  'Soft drinks, coffees, teas, beers, and ales',
  null
);
insert into northwind.categories
values (
  2, 
  'Condiments', 
  'Sweet and savory sauces, relishes, spreads, and seasonings', 
  null
);
insert into northwind.categories
values (
  3, 
  'Confections', 
  'Desserts, candies, and sweet breads', 
  null
);
insert into northwind.categories
values (
  4, 
  'Dairy Products', 
  'Cheeses', 
  null
);
insert into northwind.categories
values (
  5, 
  'Grains/Cereals', 
  'Breads, crackers, pasta, and cereal', 
  null
);
insert into northwind.categories
values (
  6, 
  'Meat/Poultry', 
  'Prepared meats', 
  null
);
insert into northwind.categories
values (
  7, 
  'Produce', 
  'Dried fruit and bean curd', 
  null
);
insert into northwind.categories
values (
  8, 
  'Seafood', 
  'Seaweed and fish', 
  null
);
insert into northwind.customers
values (
  'ALFKI', 
  'Alfreds Futterkiste', 
  'Maria Anders', 
  'Sales Representative', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany', 
  '030-0074321', 
  '030-0076545'
);
insert into northwind.customers
values (
  'ANATR', 
  'Ana Trujillo Emparedados y helados', 
  'Ana Trujillo', 
  'Owner', 
  'Avda. de la Constitución 2222', 
  'México D.F.', 
  null, 
  '05021', 
  'Mexico', 
  '(5) 555-4729', 
  '(5) 555-3745'
);
insert into northwind.customers
values (
  'ANTON', 
  'Antonio Moreno Taquería', 
  'Antonio Moreno', 
  'Owner', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico', 
  '(5) 555-3932', 
  null
);
insert into northwind.customers
values (
  'AROUT', 
  'Around the Horn', 
  'Thomas Hardy', 
  'Sales Representative', 
  '120 Hanover Sq.', 
  'London', 
  null, 
  'WA1 1DP', 
  'UK', 
  '(171) 555-7788', 
  '(171) 555-6750'
);
insert into northwind.customers
values (
  'BERGS', 
  'Berglunds snabbköp', 
  'Christina Berglund', 
  'Order Administrator', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden', 
  '0921-12 34 65', 
  '0921-12 34 67'
);
insert into northwind.customers
values (
  'BLAUS', 
  'Blauer See Delikatessen', 
  'Hanna Moos', 
  'Sales Representative', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany', 
  '0621-08460', 
  '0621-08924'
);
insert into northwind.customers
values (
  'BLONP', 
  'Blondesddsl père et fils', 
  'Frédérique Citeaux', 
  'Marketing Manager', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France', 
  '88.60.15.31', 
  '88.60.15.32'
);
insert into northwind.customers
values (
  'BOLID', 
  'Bólido Comidas preparadas', 
  'Martín Sommer', 
  'Owner', 
  'C/ Araquil, 67', 
  'Madrid', 
  null, 
  '28023', 
  'Spain', 
  '(91) 555 22 82', 
  '(91) 555 91 99'
);
insert into northwind.customers
values (
  'BONAP', 
  'Bon app''', 
  'Laurence Lebihan', 
  'Owner', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France', 
  '91.24.45.40', 
  '91.24.45.41'
);
insert into northwind.customers
values (
  'BOTTM', 
  'Bottom-Dollar Markets', 
  'Elizabeth Lincoln', 
  'Accounting Manager', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada', 
  '(604) 555-4729', 
  '(604) 555-3745'
);
insert into northwind.customers
values (
  'BSBEV', 
  'B''s Beverages', 
  'Victoria Ashworth', 
  'Sales Representative', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK', 
  '(171) 555-1212', 
  null
);
insert into northwind.customers
values (
  'CACTU', 
  'Cactus Comidas para llevar', 
  'Patricio Simpson', 
  'Sales Agent', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina', 
  '(1) 135-5555', 
  '(1) 135-4892'
);
insert into northwind.customers
values (
  'CENTC', 
  'Centro comercial Moctezuma', 
  'Francisco Chang', 
  'Marketing Manager', 
  'Sierras de Granada 9993', 
  'México D.F.', 
  null, 
  '05022', 
  'Mexico', 
  '(5) 555-3392', 
  '(5) 555-7293'
);
insert into northwind.customers
values (
  'CHOPS', 
  'Chop-suey Chinese', 
  'Yang Wang', 
  'Owner', 
  'Hauptstr. 29', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland', 
  '0452-076545', 
  null
);
insert into northwind.customers
values (
  'COMMI', 
  'Comércio Mineiro', 
  'Pedro Afonso', 
  'Sales Associate', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil', 
  '(11) 555-7647', 
  null
);
insert into northwind.customers
values (
  'CONSH', 
  'Consolidated Holdings', 
  'Elizabeth Brown', 
  'Sales Representative', 
  'Berkeley Gardens 12  Brewery', 
  'London', 
  null, 
  'WX1 6LT', 
  'UK', 
  '(171) 555-2282', 
  '(171) 555-9199'
);
insert into northwind.customers
values (
  'DRACD', 
  'Drachenblut Delikatessen', 
  'Sven Ottlieb', 
  'Order Administrator', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany', 
  '0241-039123', 
  '0241-059428'
);
insert into northwind.customers
values (
  'DUMON', 
  'Du monde entier', 
  'Janine Labrune', 
  'Owner', 
  '67, rue des Cinquante Otages', 
  'Nantes', 
  null, 
  '44000', 
  'France', 
  '40.67.88.88', 
  '40.67.89.89'
);
insert into northwind.customers
values (
  'EASTC', 
  'Eastern Connection', 
  'Ann Devon', 
  'Sales Agent', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK', 
  '(171) 555-0297', 
  '(171) 555-3373'
);
insert into northwind.customers
values (
  'ERNSH', 
  'Ernst Handel', 
  'Roland Mendel', 
  'Sales Manager', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria', 
  '7675-3425', 
  '7675-3426'
);
insert into northwind.customers
values (
  'FAMIA', 
  'Familia Arquibaldo', 
  'Aria Cruz', 
  'Marketing Assistant', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil', 
  '(11) 555-9857', 
  null
);
insert into northwind.customers
values (
  'FISSA', 
  'FISSA Fabrica Inter. Salchichas S.A.', 
  'Diego Roel', 
  'Accounting Manager', 
  'C/ Moralzarzal, 86', 
  'Madrid', 
  null, 
  '28034', 
  'Spain', 
  '(91) 555 94 44', 
  '(91) 555 55 93'
);
insert into northwind.customers
values (
  'FOLIG', 
  'Folies gourmandes', 
  'Martine Rancé', 
  'Assistant Sales Agent', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France', 
  '20.16.10.16', 
  '20.16.10.17'
);
insert into northwind.customers
values (
  'FOLKO', 
  'Folk och fä HB', 
  'Maria Larsson', 
  'Owner', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden', 
  '0695-34 67 21', 
  null
);
insert into northwind.customers
values (
  'FRANK', 
  'Frankenversand', 
  'Peter Franken', 
  'Marketing Manager', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany', 
  '089-0877310', 
  '089-0877451'
);
insert into northwind.customers
values (
  'FRANR', 
  'France restauration', 
  'Carine Schmitt', 
  'Marketing Manager', 
  '54, rue Royale', 
  'Nantes', 
  null, 
  '44000', 
  'France', 
  '40.32.21.21', 
  '40.32.21.20'
);
insert into northwind.customers
values (
  'FRANS', 
  'Franchi S.p.A.', 
  'Paolo Accorti', 
  'Sales Representative', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy', 
  '011-4988260', 
  '011-4988261'
);
insert into northwind.customers
values (
  'FURIB', 
  'Furia Bacalhau e Frutos do Mar', 
  'Lino Rodriguez', 
  'Sales Manager', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal', 
  '(1) 354-2534', 
  '(1) 354-2535'
);
insert into northwind.customers
values (
  'GALED', 
  'Galería del gastrónomo', 
  'Eduardo Saavedra', 
  'Marketing Manager', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '08022', 
  'Spain', 
  '(93) 203 4560', 
  '(93) 203 4561'
);
insert into northwind.customers
values (
  'GODOS', 
  'Godos Cocina Típica', 
  'José Pedro Freyre', 
  'Sales Manager', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain', 
  '(95) 555 82 82', 
  null
);
insert into northwind.customers
values (
  'GOURL', 
  'Gourmet Lanchonetes', 
  'André Fonseca', 
  'Sales Associate', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil', 
  '(11) 555-9482', 
  null
);
insert into northwind.customers
values (
  'GREAL', 
  'Great Lakes Food Market', 
  'Howard Snyder', 
  'Marketing Manager', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA', 
  '(503) 555-7555', 
  null
);
insert into northwind.customers
values (
  'GROSR', 
  'GROSELLA-Restaurante', 
  'Manuel Pereira', 
  'Owner', 
  '5ª Ave. Los Palos Grandes', 
  'Caracas', 
  'DF', 
  '1081', 
  'Venezuela', 
  '(2) 283-2951', 
  '(2) 283-3397'
);
insert into northwind.customers
values (
  'HANAR', 
  'Hanari Carnes', 
  'Mario Pontes', 
  'Accounting Manager', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil', 
  '(21) 555-0091', 
  '(21) 555-8765'
);
insert into northwind.customers
values (
  'HILAA', 
  'HILARION-Abastos', 
  'Carlos Hernández', 
  'Sales Representative', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela', 
  '(5) 555-1340', 
  '(5) 555-1948'
);
insert into northwind.customers
values (
  'HUNGC', 
  'Hungry Coyote Import Store', 
  'Yoshi Latimer', 
  'Sales Representative', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA', 
  '(503) 555-6874', 
  '(503) 555-2376'
);
insert into northwind.customers
values (
  'HUNGO', 
  'Hungry Owl All-Night Grocers', 
  'Patricia McKenna', 
  'Sales Associate', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland', 
  '2967 542', 
  '2967 3333'
);
insert into northwind.customers
values (
  'ISLAT', 
  'Island Trading', 
  'Helen Bennett', 
  'Marketing Manager', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK', 
  '(198) 555-8888', 
  null
);
insert into northwind.customers
values (
  'KOENE', 
  'Königlich Essen', 
  'Philip Cramer', 
  'Sales Associate', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany', 
  '0555-09876', 
  null
);
insert into northwind.customers
values (
  'LACOR', 
  'La corne d''abondance', 
  'Daniel Tonini', 
  'Sales Representative', 
  '67, avenue de l''Europe', 
  'Versailles', 
  null, 
  '78000', 
  'France', 
  '30.59.84.10', 
  '30.59.85.11'
);
insert into northwind.customers
values (
  'LAMAI', 
  'La maison d''Asie', 
  'Annette Roulet', 
  'Sales Manager', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France', 
  '61.77.61.10', 
  '61.77.61.11'
);
insert into northwind.customers
values (
  'LAUGB', 
  'Laughing Bacchus Wine Cellars', 
  'Yoshi Tannamuri', 
  'Marketing Assistant', 
  '1900 Oak St.', 
  'Vancouver', 
  'BC', 
  'V3F 2K1', 
  'Canada', 
  '(604) 555-3392', 
  '(604) 555-7293'
);
insert into northwind.customers
values (
  'LAZYK', 
  'Lazy K Kountry Store', 
  'John Steel', 
  'Marketing Manager', 
  '12 Orchestra Terrace', 
  'Walla Walla', 
  'WA', 
  '99362', 
  'USA', 
  '(509) 555-7969', 
  '(509) 555-6221'
);
insert into northwind.customers
values (
  'LEHMS', 
  'Lehmanns Marktstand', 
  'Renate Messner', 
  'Sales Representative', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany', 
  '069-0245984', 
  '069-0245874'
);
insert into northwind.customers
values (
  'LETSS', 
  'Let''s Stop N Shop', 
  'Jaime Yorres', 
  'Owner', 
  '87 Polk St. Suite 5', 
  'San Francisco', 
  'CA', 
  '94117', 
  'USA', 
  '(415) 555-5938', 
  null
);
insert into northwind.customers
values (
  'LILAS', 
  'LILA-Supermercado', 
  'Carlos González', 
  'Accounting Manager', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela', 
  '(9) 331-6954', 
  '(9) 331-7256'
);
insert into northwind.customers
values (
  'LINOD', 
  'LINO-Delicateses', 
  'Felipe Izquierdo', 
  'Owner', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela', 
  '(8) 34-56-12', 
  '(8) 34-93-93'
);
insert into northwind.customers
values (
  'LONEP', 
  'Lonesome Pine Restaurant', 
  'Fran Wilson', 
  'Sales Manager', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA', 
  '(503) 555-9573', 
  '(503) 555-9646'
);
insert into northwind.customers
values (
  'MAGAA', 
  'Magazzini Alimentari Riuniti', 
  'Giovanni Rovelli', 
  'Marketing Manager', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy', 
  '035-640230', 
  '035-640231'
);
insert into northwind.customers
values (
  'MAISD', 
  'Maison Dewey', 
  'Catherine Dewey', 
  'Sales Agent', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium', 
  '(02) 201 24 67', 
  '(02) 201 24 68'
);
insert into northwind.customers
values (
  'MEREP', 
  'Mère Paillarde', 
  'Jean Fresnière', 
  'Marketing Assistant', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada', 
  '(514) 555-8054', 
  '(514) 555-8055'
);
insert into northwind.customers
values (
  'MORGK', 
  'Morgenstern Gesundkost', 
  'Alexander Feuer', 
  'Marketing Assistant', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany', 
  '0342-023176', 
  null
);
insert into northwind.customers
values (
  'NORTS', 
  'North/South', 
  'Simon Crowther', 
  'Sales Associate', 
  'South House 300 Queensbridge', 
  'London', 
  null, 
  'SW7 1RZ', 
  'UK', 
  '(171) 555-7733', 
  '(171) 555-2530'
);
insert into northwind.customers
values (
  'OCEAN', 
  'Océano Atlántico Ltda.', 
  'Yvonne Moncada', 
  'Sales Agent', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina', 
  '(1) 135-5333', 
  '(1) 135-5535'
);
insert into northwind.customers
values (
  'OLDWO', 
  'Old World Delicatessen', 
  'Rene Phillips', 
  'Sales Representative', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA', 
  '(907) 555-7584', 
  '(907) 555-2880'
);
insert into northwind.customers
values (
  'OTTIK', 
  'Ottilies Käseladen', 
  'Henriette Pfalzheim', 
  'Owner', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany', 
  '0221-0644327', 
  '0221-0765721'
);
insert into northwind.customers
values (
  'PARIS', 
  'Paris spécialités', 
  'Marie Bertrand', 
  'Owner', 
  '265, boulevard Charonne', 
  'Paris', 
  null, 
  '75012', 
  'France', 
  '(1) 42.34.22.66', 
  '(1) 42.34.22.77'
);
insert into northwind.customers
values (
  'PERIC', 
  'Pericles Comidas clásicas', 
  'Guillermo Fernández', 
  'Sales Representative', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico', 
  '(5) 552-3745', 
  '(5) 545-3745'
);
insert into northwind.customers
values (
  'PICCO', 
  'Piccolo und mehr', 
  'Georg Pipps', 
  'Sales Manager', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria', 
  '6562-9722', 
  '6562-9723'
);
insert into northwind.customers
values (
  'PRINI', 
  'Princesa Isabel Vinhos', 
  'Isabel de Castro', 
  'Sales Representative', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal', 
  '(1) 356-5634', 
  null
);
insert into northwind.customers
values (
  'QUEDE', 
  'Que Delícia', 
  'Bernardo Batista', 
  'Accounting Manager', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil', 
  '(21) 555-4252', 
  '(21) 555-4545'
);
insert into northwind.customers
values (
  'QUEEN', 
  'Queen Cozinha', 
  'Lúcia Carvalho', 
  'Marketing Assistant', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil', 
  '(11) 555-1189', 
  null
);
insert into northwind.customers
values (
  'QUICK', 
  'QUICK-Stop', 
  'Horst Kloss', 
  'Accounting Manager', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany', 
  '0372-035188', 
  null
);
insert into northwind.customers
values (
  'RANCH', 
  'Rancho grande', 
  'Sergio Gutiérrez', 
  'Sales Representative', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina', 
  '(1) 123-5555', 
  '(1) 123-5556'
);
insert into northwind.customers
values (
  'RATTC', 
  'Rattlesnake Canyon Grocery', 
  'Paula Wilson', 
  'Assistant Sales Representative', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA', 
  '(505) 555-5939', 
  '(505) 555-3620'
);
insert into northwind.customers
values (
  'REGGC', 
  'Reggiani Caseifici', 
  'Maurizio Moroni', 
  'Sales Associate', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy', 
  '0522-556721', 
  '0522-556722'
);
insert into northwind.customers
values (
  'RICAR', 
  'Ricardo Adocicados', 
  'Janete Limeira', 
  'Assistant Sales Agent', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil', 
  '(21) 555-3412', 
  null
);
insert into northwind.customers
values (
  'RICSU', 
  'Richter Supermarkt', 
  'Michael Holz', 
  'Sales Manager', 
  'Grenzacherweg 237', 
  'Genève', 
  null, 
  '1203', 
  'Switzerland', 
  '0897-034214', 
  null
);
insert into northwind.customers
values (
  'ROMEY', 
  'Romero y tomillo', 
  'Alejandra Camino', 
  'Accounting Manager', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain', 
  '(91) 745 6200', 
  '(91) 745 6210'
);
insert into northwind.customers
values (
  'SANTG', 
  'Santé Gourmet', 
  'Jonas Bergulfsen', 
  'Owner', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway', 
  '07-98 92 35', 
  '07-98 92 47'
);
insert into northwind.customers
values (
  'SAVEA', 
  'Save-a-lot Markets', 
  'Jose Pavarotti', 
  'Sales Representative', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA', 
  '(208) 555-8097', 
  null
);
insert into northwind.customers
values (
  'SEVES', 
  'Seven Seas Imports', 
  'Hari Kumar', 
  'Sales Manager', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK', 
  '(171) 555-1717', 
  '(171) 555-5646'
);
insert into northwind.customers
values (
  'SIMOB', 
  'Simons bistro', 
  'Jytte Petersen', 
  'Owner', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark', 
  '31 12 34 56', 
  '31 13 35 57'
);
insert into northwind.customers
values (
  'SPECD', 
  'Spécialités du monde', 
  'Dominique Perrier', 
  'Marketing Manager', 
  '25, rue Lauriston', 
  'Paris', 
  null, 
  '75016', 
  'France', 
  '(1) 47.55.60.10', 
  '(1) 47.55.60.20'
);
insert into northwind.customers
values (
  'SPLIR', 
  'Split Rail Beer & Ale', 
  'Art Braunschweiger', 
  'Sales Manager', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA', 
  '(307) 555-4680', 
  '(307) 555-6525'
);
insert into northwind.customers
values (
  'SUPRD', 
  'Suprêmes délices', 
  'Pascale Cartrain', 
  'Accounting Manager', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium', 
  '(071) 23 67 22 20', 
  '(071) 23 67 22 21'
);
insert into northwind.customers
values (
  'THEBI', 
  'The Big Cheese', 
  'Liz Nixon', 
  'Marketing Manager', 
  '89 Jefferson Way Suite 2', 
  'Portland', 
  'OR', 
  '97201', 
  'USA', 
  '(503) 555-3612', 
  null
);
insert into northwind.customers
values (
  'THECR', 
  'The Cracker Box', 
  'Liu Wong', 
  'Marketing Assistant', 
  '55 Grizzly Peak Rd.', 
  'Butte', 
  'MT', 
  '59801', 
  'USA', 
  '(406) 555-5834', 
  '(406) 555-8083'
);
insert into northwind.customers
values (
  'TOMSP', 
  'Toms Spezialitäten', 
  'Karin Josephs', 
  'Marketing Manager', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany', 
  '0251-031259', 
  '0251-035695'
);
insert into northwind.customers
values (
  'TORTU', 
  'Tortuga Restaurante', 
  'Miguel Angel Paolino', 
  'Owner', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico', 
  '(5) 555-2933', 
  null
);
insert into northwind.customers
values (
  'TRADH', 
  'Tradição Hipermercados', 
  'Anabela Domingues', 
  'Sales Representative', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil', 
  '(11) 555-2167', 
  '(11) 555-2168'
);
insert into northwind.customers
values (
  'TRAIH', 
  'Trail''s Head Gourmet Provisioners', 
  'Helvetius Nagy', 
  'Sales Associate', 
  '722 DaVinci Blvd.', 
  'Kirkland', 
  'WA', 
  '98034', 
  'USA', 
  '(206) 555-8257', 
  '(206) 555-2174'
);
insert into northwind.customers
values (
  'VAFFE', 
  'Vaffeljernet', 
  'Palle Ibsen', 
  'Sales Manager', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark', 
  '86 21 32 43', 
  '86 22 33 44'
);
insert into northwind.customers
values (
  'VICTE', 
  'Victuailles en stock', 
  'Mary Saveley', 
  'Sales Agent', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France', 
  '78.32.54.86', 
  '78.32.54.87'
);
insert into northwind.customers
values (
  'VINET', 
  'Vins et alcools Chevalier', 
  'Paul Henriot', 
  'Accounting Manager', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France', 
  '26.47.15.10', 
  '26.47.15.11'
);
insert into northwind.customers
values (
  'WANDK', 
  'Die Wandernde Kuh', 
  'Rita Müller', 
  'Sales Representative', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany', 
  '0711-020361', 
  '0711-035428'
);
insert into northwind.customers
values (
  'WARTH', 
  'Wartian Herkku', 
  'Pirkko Koskitalo', 
  'Accounting Manager', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland', 
  '981-443655', 
  '981-443655'
);
insert into northwind.customers
values (
  'WELLI', 
  'Wellington Importadora', 
  'Paula Parente', 
  'Sales Manager', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil', 
  '(14) 555-8122', 
  null
);
insert into northwind.customers
values (
  'WHITC', 
  'White Clover Markets', 
  'Karl Jablonski', 
  'Owner', 
  '305 - 14th Ave. S. Suite 3B', 
  'Seattle', 
  'WA', 
  '98128', 
  'USA', 
  '(206) 555-4112', 
  '(206) 555-4115'
);
insert into northwind.customers
values (
  'WILMK', 
  'Wilman Kala', 
  'Matti Karttunen', 
  'Owner/Marketing Assistant', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland', 
  '90-224 8858', 
  '90-224 8858'
);
insert into northwind.customers
values (
  'WOLZA', 
  'Wolski  Zajazd', 
  'Zbyszek Piestrzeniewicz', 
  'Owner', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland', 
  '(26) 642-7012', 
  '(26) 642-7012'
);
insert into northwind.employees
values (
  1, 
  'Davolio', 
  'Nancy', 
  'Sales Representative', 
  'Ms.', 
  '1948-12-08', 
  '1992-05-01', 
  '507 - 20th Ave. E.\nApt. 2A', 
  'Seattle', 
  'WA', 
  '98122', 
  'USA', 
  '(206) 555-9857', 
  '5467', 
  null, 
  'Education includes a BA in psychology from Colorado State University in 1970.  She also completed The Art of the Cold Call.  Nancy is a member of Toastmasters International.', 
  2, 
  'http://accweb/emmployees/davolio.bmp'
);
insert into northwind.employees
values (
  2, 
  'Fuller', 
  'Andrew', 
  'Vice President, Sales', 
  'Dr.', 
  '1952-02-19', 
  '1992-08-14', 
  '908 W. Capital Way', 
  'Tacoma', 
  'WA', 
  '98401', 
  'USA', 
  '(206) 555-9482', 
  '3457', 
  null, 
  'Andrew received his BTS commercial in 1974 and a Ph.D. in international marketing from the University of Dallas in 1981.  He is fluent in French and Italian and reads German.  He joined the company as a sales representative, was promoted to sales manager in January 1992 and to vice president of sales in March 1993.  Andrew is a member of the Sales Management Roundtable, the Seattle Chamber of Commerce, and the Pacific Rim Importers Association.', 
  null, 
  'http://accweb/emmployees/fuller.bmp'
);
insert into northwind.employees
values (
  3, 
  'Leverling', 
  'Janet', 
  'Sales Representative', 
  'Ms.', 
  '1963-08-30', 
  '1992-04-01', 
  '722 Moss Bay Blvd.', 
  'Kirkland', 
  'WA', 
  '98033', 
  'USA', 
  '(206) 555-3412', 
  '3355', 
  null, 
  'Janet has a BS degree in chemistry from Boston College (1984).  She has also completed a certificate program in food retailing management.  Janet was hired as a sales associate in 1991 and promoted to sales representative in February 1992.', 
  2, 
  'http://accweb/emmployees/leverling.bmp'
);
insert into northwind.employees
values (
  4, 
  'Peacock', 
  'Margaret', 
  'Sales Representative', 
  'Mrs.', 
  '1937-09-19', 
  '1993-05-03', 
  '4110 Old Redmond Rd.', 
  'Redmond', 
  'WA', 
  '98052', 
  'USA', 
  '(206) 555-8122', 
  '5176', 
  null, 
  'Margaret holds a BA in English literature from Concordia College (1958) and an MA from the American Institute of Culinary Arts (1966).  She was assigned to the London office temporarily from July through November 1992.', 
  2, 
  'http://accweb/emmployees/peacock.bmp'
);
insert into northwind.employees
values (
  5, 
  'Buchanan', 
  'Steven', 
  'Sales Manager', 
  'Mr.', 
  '1955-03-04', 
  '1993-10-17', 
  '14 Garrett Hill', 
  'London', 
  null, 
  'SW1 8JR', 
  'UK', 
  '(71) 555-4848', 
  '3453', 
  null, 
  'Steven Buchanan graduated from St. Andrews University, Scotland, with a BSC degree in 1976.  Upon joining the company as a sales representative in 1992, he spent 6 months in an orientation program at the Seattle office and then returned to his permanent post in London.  He was promoted to sales manager in March 1993.  Mr. Buchanan has completed the courses Successful Telemarketing and International Sales Management.  He is fluent in French.', 
  2, 
  'http://accweb/emmployees/buchanan.bmp'
);
insert into northwind.employees
values (
  6, 
  'Suyama', 
  'Michael', 
  'Sales Representative', 
  'Mr.', 
  '1963-07-02', 
  '1993-10-17', 
  'Coventry House\nMiner Rd.', 
  'London', 
  null, 
  'EC2 7JR', 
  'UK', 
  '(71) 555-7773', 
  '428', 
  null, 
  'Michael is a graduate of Sussex University (MA, economics, 1983) and the University of California at Los Angeles (MBA, marketing, 1986).  He has also taken the courses Multi-Cultural Selling and Time Management for the Sales Professional.  He is fluent in Japanese and can read and write French, Portuguese, and Spanish.', 
  5, 
  'http://accweb/emmployees/davolio.bmp'
);
insert into northwind.employees
values (
  7, 
  'King', 
  'Robert', 
  'Sales Representative', 
  'Mr.', 
  '1960-05-29', 
  '1994-01-02', 
  'Edgeham Hollow\nWinchester Way', 
  'London', 
  null, 
  'RG1 9SP', 
  'UK', 
  '(71) 555-5598', 
  '465', 
  null, 
  'Robert King served in the Peace Corps and traveled extensively before completing his degree in English at the University of Michigan in 1992, the year he joined the company.  After completing a course entitled Selling in Europe, he was transferred to the London office in March 1993.', 
  5, 
  'http://accweb/emmployees/davolio.bmp'
);
insert into northwind.employees
values (
  8, 
  'Callahan', 
  'Laura', 
  'Inside Sales Coordinator', 
  'Ms.', 
  '1958-01-09', 
  '1994-03-05', 
  '4726 - 11th Ave. N.E.', 
  'Seattle', 
  'WA', 
  '98105', 
  'USA', 
  '(206) 555-1189', 
  '2344', 
  null, 
  'Laura received a BA in psychology from the University of Washington.  She has also completed a course in business French.  She reads and writes French.', 
  2, 
  'http://accweb/emmployees/davolio.bmp'
);
insert into northwind.employees
values (
  9, 
  'Dodsworth', 
  'Anne', 
  'Sales Representative', 
  'Ms.', 
  '1966-01-27', 
  '1994-11-15', 
  '7 Houndstooth Rd.', 
  'London', 
  null, 
  'WG2 7LT', 
  'UK', 
  '(71) 555-4444', 
  '452', 
  null, 
  'Anne has a BA degree in English from St. Lawrence College.  She is fluent in French and German.', 
  5, 
  'http://accweb/emmployees/davolio.bmp'
);
insert into northwind.employee_territories
values (
  1, 
  '06897'
);
insert into northwind.employee_territories
values (
  1, 
  '19713'
);
insert into northwind.employee_territories
values (
  2, 
  '01581'
);
insert into northwind.employee_territories
values (
  2, 
  '01730'
);
insert into northwind.employee_territories
values (
  2, 
  '01833'
);
insert into northwind.employee_territories
values (
  2, 
  '02116'
);
insert into northwind.employee_territories
values (
  2, 
  '02139'
);
insert into northwind.employee_territories
values (
  2, 
  '02184'
);
insert into northwind.employee_territories
values (
  2, 
  '40222'
);
insert into northwind.employee_territories
values (
  3, 
  '30346'
);
insert into northwind.employee_territories
values (
  3, 
  '31406'
);
insert into northwind.employee_territories
values (
  3, 
  '32859'
);
insert into northwind.employee_territories
values (
  3, 
  '33607'
);
insert into northwind.employee_territories
values (
  4, 
  '20852'
);
insert into northwind.employee_territories
values (
  4, 
  '27403'
);
insert into northwind.employee_territories
values (
  4, 
  '27511'
);
insert into northwind.employee_territories
values (
  5, 
  '02903'
);
insert into northwind.employee_territories
values (
  5, 
  '07960'
);
insert into northwind.employee_territories
values (
  5, 
  '08837'
);
insert into northwind.employee_territories
values (
  5, 
  '10019'
);
insert into northwind.employee_territories
values (
  5, 
  '10038'
);
insert into northwind.employee_territories
values (
  5, 
  '11747'
);
insert into northwind.employee_territories
values (
  5, 
  '14450'
);
insert into northwind.employee_territories
values (
  6, 
  '85014'
);
insert into northwind.employee_territories
values (
  6, 
  '85251'
);
insert into northwind.employee_territories
values (
  6, 
  '98004'
);
insert into northwind.employee_territories
values (
  6, 
  '98052'
);
insert into northwind.employee_territories
values (
  6, 
  '98104'
);
insert into northwind.employee_territories
values (
  7, 
  '60179'
);
insert into northwind.employee_territories
values (
  7, 
  '60601'
);
insert into northwind.employee_territories
values (
  7, 
  '80202'
);
insert into northwind.employee_territories
values (
  7, 
  '80909'
);
insert into northwind.employee_territories
values (
  7, 
  '90405'
);
insert into northwind.employee_territories
values (
  7, 
  '94025'
);
insert into northwind.employee_territories
values (
  7, 
  '94105'
);
insert into northwind.employee_territories
values (
  7, 
  '95008'
);
insert into northwind.employee_territories
values (
  7, 
  '95054'
);
insert into northwind.employee_territories
values (
  7, 
  '95060'
);
insert into northwind.employee_territories
values (
  8, 
  '19428'
);
insert into northwind.employee_territories
values (
  8, 
  '44122'
);
insert into northwind.employee_territories
values (
  8, 
  '45839'
);
insert into northwind.employee_territories
values (
  8, 
  '53404'
);
insert into northwind.employee_territories
values (
  9, 
  '03049'
);
insert into northwind.employee_territories
values (
  9, 
  '03801'
);
insert into northwind.employee_territories
values (
  9, 
  '48075'
);
insert into northwind.employee_territories
values (
  9, 
  '48084'
);
insert into northwind.employee_territories
values (
  9, 
  '48304'
);
insert into northwind.employee_territories
values (
  9, 
  '55113'
);
insert into northwind.employee_territories
values (
  9, 
  '55439'
);
insert into northwind.order_details
values (
  10248, 
  11, 
  14, 
  12, 
  0
);
insert into northwind.order_details
values (
  10248, 
  42, 
  9.80000019, 
  10, 
  0
);
insert into northwind.order_details
values (
  10248, 
  72, 
  34.7999992, 
  5, 
  0
);
insert into northwind.order_details
values (
  10249, 
  14, 
  18.6000004, 
  9, 
  0
);
insert into northwind.order_details
values (
  10249, 
  51, 
  42.4000015, 
  40, 
  0
);
insert into northwind.order_details
values (
  10250, 
  41, 
  7.69999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10250, 
  51, 
  42.4000015, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10250, 
  65, 
  16.7999992, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10251, 
  22, 
  16.7999992, 
  6, 
  0.0500000007
);
insert into northwind.order_details
values (
  10251, 
  57, 
  15.6000004, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10251, 
  65, 
  16.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10252, 
  20, 
  64.8000031, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10252, 
  33, 
  2, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10252, 
  60, 
  27.2000008, 
  40, 
  0
);
insert into northwind.order_details
values (
  10253, 
  31, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10253, 
  39, 
  14.3999996, 
  42, 
  0
);
insert into northwind.order_details
values (
  10253, 
  49, 
  16, 
  40, 
  0
);
insert into northwind.order_details
values (
  10254, 
  24, 
  3.5999999, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10254, 
  55, 
  19.2000008, 
  21, 
  0.150000006
);
insert into northwind.order_details
values (
  10254, 
  74, 
  8, 
  21, 
  0
);
insert into northwind.order_details
values (
  10255, 
  2, 
  15.1999998, 
  20, 
  0
);
insert into northwind.order_details
values (
  10255, 
  16, 
  13.8999996, 
  35, 
  0
);
insert into northwind.order_details
values (
  10255, 
  36, 
  15.1999998, 
  25, 
  0
);
insert into northwind.order_details
values (
  10255, 
  59, 
  44, 
  30, 
  0
);
insert into northwind.order_details
values (
  10256, 
  53, 
  26.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10256, 
  77, 
  10.3999996, 
  12, 
  0
);
insert into northwind.order_details
values (
  10257, 
  27, 
  35.0999985, 
  25, 
  0
);
insert into northwind.order_details
values (
  10257, 
  39, 
  14.3999996, 
  6, 
  0
);
insert into northwind.order_details
values (
  10257, 
  77, 
  10.3999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10258, 
  2, 
  15.1999998, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10258, 
  5, 
  17, 
  65, 
  0.200000003
);
insert into northwind.order_details
values (
  10258, 
  32, 
  25.6000004, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10259, 
  21, 
  8, 
  10, 
  0
);
insert into northwind.order_details
values (
  10259, 
  37, 
  20.7999992, 
  1, 
  0
);
insert into northwind.order_details
values (
  10260, 
  41, 
  7.69999981, 
  16, 
  0.25
);
insert into northwind.order_details
values (
  10260, 
  57, 
  15.6000004, 
  50, 
  0
);
insert into northwind.order_details
values (
  10260, 
  62, 
  39.4000015, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10260, 
  70, 
  12, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10261, 
  21, 
  8, 
  20, 
  0
);
insert into northwind.order_details
values (
  10261, 
  35, 
  14.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10262, 
  5, 
  17, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10262, 
  7, 
  24, 
  15, 
  0
);
insert into northwind.order_details
values (
  10262, 
  56, 
  30.3999996, 
  2, 
  0
);
insert into northwind.order_details
values (
  10263, 
  16, 
  13.8999996, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10263, 
  24, 
  3.5999999, 
  28, 
  0
);
insert into northwind.order_details
values (
  10263, 
  30, 
  20.7000008, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10263, 
  74, 
  8, 
  36, 
  0.25
);
insert into northwind.order_details
values (
  10264, 
  2, 
  15.1999998, 
  35, 
  0
);
insert into northwind.order_details
values (
  10264, 
  41, 
  7.69999981, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10265, 
  17, 
  31.2000008, 
  30, 
  0
);
insert into northwind.order_details
values (
  10265, 
  70, 
  12, 
  20, 
  0
);
insert into northwind.order_details
values (
  10266, 
  12, 
  30.3999996, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10267, 
  40, 
  14.6999998, 
  50, 
  0
);
insert into northwind.order_details
values (
  10267, 
  59, 
  44, 
  70, 
  0.150000006
);
insert into northwind.order_details
values (
  10267, 
  76, 
  14.3999996, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10268, 
  29, 
  99, 
  10, 
  0
);
insert into northwind.order_details
values (
  10268, 
  72, 
  27.7999992, 
  4, 
  0
);
insert into northwind.order_details
values (
  10269, 
  33, 
  2, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10269, 
  72, 
  27.7999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10270, 
  36, 
  15.1999998, 
  30, 
  0
);
insert into northwind.order_details
values (
  10270, 
  43, 
  36.7999992, 
  25, 
  0
);
insert into northwind.order_details
values (
  10271, 
  33, 
  2, 
  24, 
  0
);
insert into northwind.order_details
values (
  10272, 
  20, 
  64.8000031, 
  6, 
  0
);
insert into northwind.order_details
values (
  10272, 
  31, 
  10, 
  40, 
  0
);
insert into northwind.order_details
values (
  10272, 
  72, 
  27.7999992, 
  24, 
  0
);
insert into northwind.order_details
values (
  10273, 
  10, 
  24.7999992, 
  24, 
  0.0500000007
);
insert into northwind.order_details
values (
  10273, 
  31, 
  10, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10273, 
  33, 
  2, 
  20, 
  0
);
insert into northwind.order_details
values (
  10273, 
  40, 
  14.6999998, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10273, 
  76, 
  14.3999996, 
  33, 
  0.0500000007
);
insert into northwind.order_details
values (
  10274, 
  71, 
  17.2000008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10274, 
  72, 
  27.7999992, 
  7, 
  0
);
insert into northwind.order_details
values (
  10275, 
  24, 
  3.5999999, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10275, 
  59, 
  44, 
  6, 
  0.0500000007
);
insert into northwind.order_details
values (
  10276, 
  10, 
  24.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10276, 
  13, 
  4.80000019, 
  10, 
  0
);
insert into northwind.order_details
values (
  10277, 
  28, 
  36.4000015, 
  20, 
  0
);
insert into northwind.order_details
values (
  10277, 
  62, 
  39.4000015, 
  12, 
  0
);
insert into northwind.order_details
values (
  10278, 
  44, 
  15.5, 
  16, 
  0
);
insert into northwind.order_details
values (
  10278, 
  59, 
  44, 
  15, 
  0
);
insert into northwind.order_details
values (
  10278, 
  63, 
  35.0999985, 
  8, 
  0
);
insert into northwind.order_details
values (
  10278, 
  73, 
  12, 
  25, 
  0
);
insert into northwind.order_details
values (
  10279, 
  17, 
  31.2000008, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10280, 
  24, 
  3.5999999, 
  12, 
  0
);
insert into northwind.order_details
values (
  10280, 
  55, 
  19.2000008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10280, 
  75, 
  6.19999981, 
  30, 
  0
);
insert into northwind.order_details
values (
  10281, 
  19, 
  7.30000019, 
  1, 
  0
);
insert into northwind.order_details
values (
  10281, 
  24, 
  3.5999999, 
  6, 
  0
);
insert into northwind.order_details
values (
  10281, 
  35, 
  14.3999996, 
  4, 
  0
);
insert into northwind.order_details
values (
  10282, 
  30, 
  20.7000008, 
  6, 
  0
);
insert into northwind.order_details
values (
  10282, 
  57, 
  15.6000004, 
  2, 
  0
);
insert into northwind.order_details
values (
  10283, 
  15, 
  12.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10283, 
  19, 
  7.30000019, 
  18, 
  0
);
insert into northwind.order_details
values (
  10283, 
  60, 
  27.2000008, 
  35, 
  0
);
insert into northwind.order_details
values (
  10283, 
  72, 
  27.7999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10284, 
  27, 
  35.0999985, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10284, 
  44, 
  15.5, 
  21, 
  0
);
insert into northwind.order_details
values (
  10284, 
  60, 
  27.2000008, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10284, 
  67, 
  11.1999998, 
  5, 
  0.25
);
insert into northwind.order_details
values (
  10285, 
  1, 
  14.3999996, 
  45, 
  0.200000003
);
insert into northwind.order_details
values (
  10285, 
  40, 
  14.6999998, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10285, 
  53, 
  26.2000008, 
  36, 
  0.200000003
);
insert into northwind.order_details
values (
  10286, 
  35, 
  14.3999996, 
  100, 
  0
);
insert into northwind.order_details
values (
  10286, 
  62, 
  39.4000015, 
  40, 
  0
);
insert into northwind.order_details
values (
  10287, 
  16, 
  13.8999996, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10287, 
  34, 
  11.1999998, 
  20, 
  0
);
insert into northwind.order_details
values (
  10287, 
  46, 
  9.60000038, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10288, 
  54, 
  5.9000001, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10288, 
  68, 
  10, 
  3, 
  0.100000001
);
insert into northwind.order_details
values (
  10289, 
  3, 
  8, 
  30, 
  0
);
insert into northwind.order_details
values (
  10289, 
  64, 
  26.6000004, 
  9, 
  0
);
insert into northwind.order_details
values (
  10290, 
  5, 
  17, 
  20, 
  0
);
insert into northwind.order_details
values (
  10290, 
  29, 
  99, 
  15, 
  0
);
insert into northwind.order_details
values (
  10290, 
  49, 
  16, 
  15, 
  0
);
insert into northwind.order_details
values (
  10290, 
  77, 
  10.3999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10291, 
  13, 
  4.80000019, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10291, 
  44, 
  15.5, 
  24, 
  0.100000001
);
insert into northwind.order_details
values (
  10291, 
  51, 
  42.4000015, 
  2, 
  0.100000001
);
insert into northwind.order_details
values (
  10292, 
  20, 
  64.8000031, 
  20, 
  0
);
insert into northwind.order_details
values (
  10293, 
  18, 
  50, 
  12, 
  0
);
insert into northwind.order_details
values (
  10293, 
  24, 
  3.5999999, 
  10, 
  0
);
insert into northwind.order_details
values (
  10293, 
  63, 
  35.0999985, 
  5, 
  0
);
insert into northwind.order_details
values (
  10293, 
  75, 
  6.19999981, 
  6, 
  0
);
insert into northwind.order_details
values (
  10294, 
  1, 
  14.3999996, 
  18, 
  0
);
insert into northwind.order_details
values (
  10294, 
  17, 
  31.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10294, 
  43, 
  36.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10294, 
  60, 
  27.2000008, 
  21, 
  0
);
insert into northwind.order_details
values (
  10294, 
  75, 
  6.19999981, 
  6, 
  0
);
insert into northwind.order_details
values (
  10295, 
  56, 
  30.3999996, 
  4, 
  0
);
insert into northwind.order_details
values (
  10296, 
  11, 
  16.7999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10296, 
  16, 
  13.8999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10296, 
  69, 
  28.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10297, 
  39, 
  14.3999996, 
  60, 
  0
);
insert into northwind.order_details
values (
  10297, 
  72, 
  27.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10298, 
  2, 
  15.1999998, 
  40, 
  0
);
insert into northwind.order_details
values (
  10298, 
  36, 
  15.1999998, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10298, 
  59, 
  44, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10298, 
  62, 
  39.4000015, 
  15, 
  0
);
insert into northwind.order_details
values (
  10299, 
  19, 
  7.30000019, 
  15, 
  0
);
insert into northwind.order_details
values (
  10299, 
  70, 
  12, 
  20, 
  0
);
insert into northwind.order_details
values (
  10300, 
  66, 
  13.6000004, 
  30, 
  0
);
insert into northwind.order_details
values (
  10300, 
  68, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10301, 
  40, 
  14.6999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10301, 
  56, 
  30.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10302, 
  17, 
  31.2000008, 
  40, 
  0
);
insert into northwind.order_details
values (
  10302, 
  28, 
  36.4000015, 
  28, 
  0
);
insert into northwind.order_details
values (
  10302, 
  43, 
  36.7999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10303, 
  40, 
  14.6999998, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10303, 
  65, 
  16.7999992, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10303, 
  68, 
  10, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10304, 
  49, 
  16, 
  30, 
  0
);
insert into northwind.order_details
values (
  10304, 
  59, 
  44, 
  10, 
  0
);
insert into northwind.order_details
values (
  10304, 
  71, 
  17.2000008, 
  2, 
  0
);
insert into northwind.order_details
values (
  10305, 
  18, 
  50, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10305, 
  29, 
  99, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10305, 
  39, 
  14.3999996, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10306, 
  30, 
  20.7000008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10306, 
  53, 
  26.2000008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10306, 
  54, 
  5.9000001, 
  5, 
  0
);
insert into northwind.order_details
values (
  10307, 
  62, 
  39.4000015, 
  10, 
  0
);
insert into northwind.order_details
values (
  10307, 
  68, 
  10, 
  3, 
  0
);
insert into northwind.order_details
values (
  10308, 
  69, 
  28.7999992, 
  1, 
  0
);
insert into northwind.order_details
values (
  10308, 
  70, 
  12, 
  5, 
  0
);
insert into northwind.order_details
values (
  10309, 
  4, 
  17.6000004, 
  20, 
  0
);
insert into northwind.order_details
values (
  10309, 
  6, 
  20, 
  30, 
  0
);
insert into northwind.order_details
values (
  10309, 
  42, 
  11.1999998, 
  2, 
  0
);
insert into northwind.order_details
values (
  10309, 
  43, 
  36.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10309, 
  71, 
  17.2000008, 
  3, 
  0
);
insert into northwind.order_details
values (
  10310, 
  16, 
  13.8999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10310, 
  62, 
  39.4000015, 
  5, 
  0
);
insert into northwind.order_details
values (
  10311, 
  42, 
  11.1999998, 
  6, 
  0
);
insert into northwind.order_details
values (
  10311, 
  69, 
  28.7999992, 
  7, 
  0
);
insert into northwind.order_details
values (
  10312, 
  28, 
  36.4000015, 
  4, 
  0
);
insert into northwind.order_details
values (
  10312, 
  43, 
  36.7999992, 
  24, 
  0
);
insert into northwind.order_details
values (
  10312, 
  53, 
  26.2000008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10312, 
  75, 
  6.19999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10313, 
  36, 
  15.1999998, 
  12, 
  0
);
insert into northwind.order_details
values (
  10314, 
  32, 
  25.6000004, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10314, 
  58, 
  10.6000004, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10314, 
  62, 
  39.4000015, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10315, 
  34, 
  11.1999998, 
  14, 
  0
);
insert into northwind.order_details
values (
  10315, 
  70, 
  12, 
  30, 
  0
);
insert into northwind.order_details
values (
  10316, 
  41, 
  7.69999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10316, 
  62, 
  39.4000015, 
  70, 
  0
);
insert into northwind.order_details
values (
  10317, 
  1, 
  14.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10318, 
  41, 
  7.69999981, 
  20, 
  0
);
insert into northwind.order_details
values (
  10318, 
  76, 
  14.3999996, 
  6, 
  0
);
insert into northwind.order_details
values (
  10319, 
  17, 
  31.2000008, 
  8, 
  0
);
insert into northwind.order_details
values (
  10319, 
  28, 
  36.4000015, 
  14, 
  0
);
insert into northwind.order_details
values (
  10319, 
  76, 
  14.3999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10320, 
  71, 
  17.2000008, 
  30, 
  0
);
insert into northwind.order_details
values (
  10321, 
  35, 
  14.3999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10322, 
  52, 
  5.5999999, 
  20, 
  0
);
insert into northwind.order_details
values (
  10323, 
  15, 
  12.3999996, 
  5, 
  0
);
insert into northwind.order_details
values (
  10323, 
  25, 
  11.1999998, 
  4, 
  0
);
insert into northwind.order_details
values (
  10323, 
  39, 
  14.3999996, 
  4, 
  0
);
insert into northwind.order_details
values (
  10324, 
  16, 
  13.8999996, 
  21, 
  0.150000006
);
insert into northwind.order_details
values (
  10324, 
  35, 
  14.3999996, 
  70, 
  0.150000006
);
insert into northwind.order_details
values (
  10324, 
  46, 
  9.60000038, 
  30, 
  0
);
insert into northwind.order_details
values (
  10324, 
  59, 
  44, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10324, 
  63, 
  35.0999985, 
  80, 
  0.150000006
);
insert into northwind.order_details
values (
  10325, 
  6, 
  20, 
  6, 
  0
);
insert into northwind.order_details
values (
  10325, 
  13, 
  4.80000019, 
  12, 
  0
);
insert into northwind.order_details
values (
  10325, 
  14, 
  18.6000004, 
  9, 
  0
);
insert into northwind.order_details
values (
  10325, 
  31, 
  10, 
  4, 
  0
);
insert into northwind.order_details
values (
  10325, 
  72, 
  27.7999992, 
  40, 
  0
);
insert into northwind.order_details
values (
  10326, 
  4, 
  17.6000004, 
  24, 
  0
);
insert into northwind.order_details
values (
  10326, 
  57, 
  15.6000004, 
  16, 
  0
);
insert into northwind.order_details
values (
  10326, 
  75, 
  6.19999981, 
  50, 
  0
);
insert into northwind.order_details
values (
  10327, 
  2, 
  15.1999998, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  10327, 
  11, 
  16.7999992, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10327, 
  30, 
  20.7000008, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10327, 
  58, 
  10.6000004, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10328, 
  59, 
  44, 
  9, 
  0
);
insert into northwind.order_details
values (
  10328, 
  65, 
  16.7999992, 
  40, 
  0
);
insert into northwind.order_details
values (
  10328, 
  68, 
  10, 
  10, 
  0
);
insert into northwind.order_details
values (
  10329, 
  19, 
  7.30000019, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10329, 
  30, 
  20.7000008, 
  8, 
  0.0500000007
);
insert into northwind.order_details
values (
  10329, 
  38, 
  210.800003, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10329, 
  56, 
  30.3999996, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10330, 
  26, 
  24.8999996, 
  50, 
  0.150000006
);
insert into northwind.order_details
values (
  10330, 
  72, 
  27.7999992, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10331, 
  54, 
  5.9000001, 
  15, 
  0
);
insert into northwind.order_details
values (
  10332, 
  18, 
  50, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10332, 
  42, 
  11.1999998, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10332, 
  47, 
  7.5999999, 
  16, 
  0.200000003
);
insert into northwind.order_details
values (
  10333, 
  14, 
  18.6000004, 
  10, 
  0
);
insert into northwind.order_details
values (
  10333, 
  21, 
  8, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10333, 
  71, 
  17.2000008, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10334, 
  52, 
  5.5999999, 
  8, 
  0
);
insert into northwind.order_details
values (
  10334, 
  68, 
  10, 
  10, 
  0
);
insert into northwind.order_details
values (
  10335, 
  2, 
  15.1999998, 
  7, 
  0.200000003
);
insert into northwind.order_details
values (
  10335, 
  31, 
  10, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  10335, 
  32, 
  25.6000004, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10335, 
  51, 
  42.4000015, 
  48, 
  0.200000003
);
insert into northwind.order_details
values (
  10336, 
  4, 
  17.6000004, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10337, 
  23, 
  7.19999981, 
  40, 
  0
);
insert into northwind.order_details
values (
  10337, 
  26, 
  24.8999996, 
  24, 
  0
);
insert into northwind.order_details
values (
  10337, 
  36, 
  15.1999998, 
  20, 
  0
);
insert into northwind.order_details
values (
  10337, 
  37, 
  20.7999992, 
  28, 
  0
);
insert into northwind.order_details
values (
  10337, 
  72, 
  27.7999992, 
  25, 
  0
);
insert into northwind.order_details
values (
  10338, 
  17, 
  31.2000008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10338, 
  30, 
  20.7000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10339, 
  4, 
  17.6000004, 
  10, 
  0
);
insert into northwind.order_details
values (
  10339, 
  17, 
  31.2000008, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  10339, 
  62, 
  39.4000015, 
  28, 
  0
);
insert into northwind.order_details
values (
  10340, 
  18, 
  50, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10340, 
  41, 
  7.69999981, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10340, 
  43, 
  36.7999992, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10341, 
  33, 
  2, 
  8, 
  0
);
insert into northwind.order_details
values (
  10341, 
  59, 
  44, 
  9, 
  0.150000006
);
insert into northwind.order_details
values (
  10342, 
  2, 
  15.1999998, 
  24, 
  0.200000003
);
insert into northwind.order_details
values (
  10342, 
  31, 
  10, 
  56, 
  0.200000003
);
insert into northwind.order_details
values (
  10342, 
  36, 
  15.1999998, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10342, 
  55, 
  19.2000008, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10343, 
  64, 
  26.6000004, 
  50, 
  0
);
insert into northwind.order_details
values (
  10343, 
  68, 
  10, 
  4, 
  0.0500000007
);
insert into northwind.order_details
values (
  10343, 
  76, 
  14.3999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10344, 
  4, 
  17.6000004, 
  35, 
  0
);
insert into northwind.order_details
values (
  10344, 
  8, 
  32, 
  70, 
  0.25
);
insert into northwind.order_details
values (
  10345, 
  8, 
  32, 
  70, 
  0
);
insert into northwind.order_details
values (
  10345, 
  19, 
  7.30000019, 
  80, 
  0
);
insert into northwind.order_details
values (
  10345, 
  42, 
  11.1999998, 
  9, 
  0
);
insert into northwind.order_details
values (
  10346, 
  17, 
  31.2000008, 
  36, 
  0.100000001
);
insert into northwind.order_details
values (
  10346, 
  56, 
  30.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10347, 
  25, 
  11.1999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10347, 
  39, 
  14.3999996, 
  50, 
  0.150000006
);
insert into northwind.order_details
values (
  10347, 
  40, 
  14.6999998, 
  4, 
  0
);
insert into northwind.order_details
values (
  10347, 
  75, 
  6.19999981, 
  6, 
  0.150000006
);
insert into northwind.order_details
values (
  10348, 
  1, 
  14.3999996, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10348, 
  23, 
  7.19999981, 
  25, 
  0
);
insert into northwind.order_details
values (
  10349, 
  54, 
  5.9000001, 
  24, 
  0
);
insert into northwind.order_details
values (
  10350, 
  50, 
  13, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10350, 
  69, 
  28.7999992, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10351, 
  38, 
  210.800003, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10351, 
  41, 
  7.69999981, 
  13, 
  0
);
insert into northwind.order_details
values (
  10351, 
  44, 
  15.5, 
  77, 
  0.0500000007
);
insert into northwind.order_details
values (
  10351, 
  65, 
  16.7999992, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10352, 
  24, 
  3.5999999, 
  10, 
  0
);
insert into northwind.order_details
values (
  10352, 
  54, 
  5.9000001, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10353, 
  11, 
  16.7999992, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10353, 
  38, 
  210.800003, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10354, 
  1, 
  14.3999996, 
  12, 
  0
);
insert into northwind.order_details
values (
  10354, 
  29, 
  99, 
  4, 
  0
);
insert into northwind.order_details
values (
  10355, 
  24, 
  3.5999999, 
  25, 
  0
);
insert into northwind.order_details
values (
  10355, 
  57, 
  15.6000004, 
  25, 
  0
);
insert into northwind.order_details
values (
  10356, 
  31, 
  10, 
  30, 
  0
);
insert into northwind.order_details
values (
  10356, 
  55, 
  19.2000008, 
  12, 
  0
);
insert into northwind.order_details
values (
  10356, 
  69, 
  28.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10357, 
  10, 
  24.7999992, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10357, 
  26, 
  24.8999996, 
  16, 
  0
);
insert into northwind.order_details
values (
  10357, 
  60, 
  27.2000008, 
  8, 
  0.200000003
);
insert into northwind.order_details
values (
  10358, 
  24, 
  3.5999999, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10358, 
  34, 
  11.1999998, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10358, 
  36, 
  15.1999998, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10359, 
  16, 
  13.8999996, 
  56, 
  0.0500000007
);
insert into northwind.order_details
values (
  10359, 
  31, 
  10, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  10359, 
  60, 
  27.2000008, 
  80, 
  0.0500000007
);
insert into northwind.order_details
values (
  10360, 
  28, 
  36.4000015, 
  30, 
  0
);
insert into northwind.order_details
values (
  10360, 
  29, 
  99, 
  35, 
  0
);
insert into northwind.order_details
values (
  10360, 
  38, 
  210.800003, 
  10, 
  0
);
insert into northwind.order_details
values (
  10360, 
  49, 
  16, 
  35, 
  0
);
insert into northwind.order_details
values (
  10360, 
  54, 
  5.9000001, 
  28, 
  0
);
insert into northwind.order_details
values (
  10361, 
  39, 
  14.3999996, 
  54, 
  0.100000001
);
insert into northwind.order_details
values (
  10361, 
  60, 
  27.2000008, 
  55, 
  0.100000001
);
insert into northwind.order_details
values (
  10362, 
  25, 
  11.1999998, 
  50, 
  0
);
insert into northwind.order_details
values (
  10362, 
  51, 
  42.4000015, 
  20, 
  0
);
insert into northwind.order_details
values (
  10362, 
  54, 
  5.9000001, 
  24, 
  0
);
insert into northwind.order_details
values (
  10363, 
  31, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10363, 
  75, 
  6.19999981, 
  12, 
  0
);
insert into northwind.order_details
values (
  10363, 
  76, 
  14.3999996, 
  12, 
  0
);
insert into northwind.order_details
values (
  10364, 
  69, 
  28.7999992, 
  30, 
  0
);
insert into northwind.order_details
values (
  10364, 
  71, 
  17.2000008, 
  5, 
  0
);
insert into northwind.order_details
values (
  10365, 
  11, 
  16.7999992, 
  24, 
  0
);
insert into northwind.order_details
values (
  10366, 
  65, 
  16.7999992, 
  5, 
  0
);
insert into northwind.order_details
values (
  10366, 
  77, 
  10.3999996, 
  5, 
  0
);
insert into northwind.order_details
values (
  10367, 
  34, 
  11.1999998, 
  36, 
  0
);
insert into northwind.order_details
values (
  10367, 
  54, 
  5.9000001, 
  18, 
  0
);
insert into northwind.order_details
values (
  10367, 
  65, 
  16.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10367, 
  77, 
  10.3999996, 
  7, 
  0
);
insert into northwind.order_details
values (
  10368, 
  21, 
  8, 
  5, 
  0.100000001
);
insert into northwind.order_details
values (
  10368, 
  28, 
  36.4000015, 
  13, 
  0.100000001
);
insert into northwind.order_details
values (
  10368, 
  57, 
  15.6000004, 
  25, 
  0
);
insert into northwind.order_details
values (
  10368, 
  64, 
  26.6000004, 
  35, 
  0.100000001
);
insert into northwind.order_details
values (
  10369, 
  29, 
  99, 
  20, 
  0
);
insert into northwind.order_details
values (
  10369, 
  56, 
  30.3999996, 
  18, 
  0.25
);
insert into northwind.order_details
values (
  10370, 
  1, 
  14.3999996, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10370, 
  64, 
  26.6000004, 
  30, 
  0
);
insert into northwind.order_details
values (
  10370, 
  74, 
  8, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10371, 
  36, 
  15.1999998, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10372, 
  20, 
  64.8000031, 
  12, 
  0.25
);
insert into northwind.order_details
values (
  10372, 
  38, 
  210.800003, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10372, 
  60, 
  27.2000008, 
  70, 
  0.25
);
insert into northwind.order_details
values (
  10372, 
  72, 
  27.7999992, 
  42, 
  0.25
);
insert into northwind.order_details
values (
  10373, 
  58, 
  10.6000004, 
  80, 
  0.200000003
);
insert into northwind.order_details
values (
  10373, 
  71, 
  17.2000008, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10374, 
  31, 
  10, 
  30, 
  0
);
insert into northwind.order_details
values (
  10374, 
  58, 
  10.6000004, 
  15, 
  0
);
insert into northwind.order_details
values (
  10375, 
  14, 
  18.6000004, 
  15, 
  0
);
insert into northwind.order_details
values (
  10375, 
  54, 
  5.9000001, 
  10, 
  0
);
insert into northwind.order_details
values (
  10376, 
  31, 
  10, 
  42, 
  0.0500000007
);
insert into northwind.order_details
values (
  10377, 
  28, 
  36.4000015, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10377, 
  39, 
  14.3999996, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10378, 
  71, 
  17.2000008, 
  6, 
  0
);
insert into northwind.order_details
values (
  10379, 
  41, 
  7.69999981, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10379, 
  63, 
  35.0999985, 
  16, 
  0.100000001
);
insert into northwind.order_details
values (
  10379, 
  65, 
  16.7999992, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10380, 
  30, 
  20.7000008, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10380, 
  53, 
  26.2000008, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10380, 
  60, 
  27.2000008, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10380, 
  70, 
  12, 
  30, 
  0
);
insert into northwind.order_details
values (
  10381, 
  74, 
  8, 
  14, 
  0
);
insert into northwind.order_details
values (
  10382, 
  5, 
  17, 
  32, 
  0
);
insert into northwind.order_details
values (
  10382, 
  18, 
  50, 
  9, 
  0
);
insert into northwind.order_details
values (
  10382, 
  29, 
  99, 
  14, 
  0
);
insert into northwind.order_details
values (
  10382, 
  33, 
  2, 
  60, 
  0
);
insert into northwind.order_details
values (
  10382, 
  74, 
  8, 
  50, 
  0
);
insert into northwind.order_details
values (
  10383, 
  13, 
  4.80000019, 
  20, 
  0
);
insert into northwind.order_details
values (
  10383, 
  50, 
  13, 
  15, 
  0
);
insert into northwind.order_details
values (
  10383, 
  56, 
  30.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10384, 
  20, 
  64.8000031, 
  28, 
  0
);
insert into northwind.order_details
values (
  10384, 
  60, 
  27.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10385, 
  7, 
  24, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10385, 
  60, 
  27.2000008, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10385, 
  68, 
  10, 
  8, 
  0.200000003
);
insert into northwind.order_details
values (
  10386, 
  24, 
  3.5999999, 
  15, 
  0
);
insert into northwind.order_details
values (
  10386, 
  34, 
  11.1999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10387, 
  24, 
  3.5999999, 
  15, 
  0
);
insert into northwind.order_details
values (
  10387, 
  28, 
  36.4000015, 
  6, 
  0
);
insert into northwind.order_details
values (
  10387, 
  59, 
  44, 
  12, 
  0
);
insert into northwind.order_details
values (
  10387, 
  71, 
  17.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10388, 
  45, 
  7.5999999, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10388, 
  52, 
  5.5999999, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10388, 
  53, 
  26.2000008, 
  40, 
  0
);
insert into northwind.order_details
values (
  10389, 
  10, 
  24.7999992, 
  16, 
  0
);
insert into northwind.order_details
values (
  10389, 
  55, 
  19.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10389, 
  62, 
  39.4000015, 
  20, 
  0
);
insert into northwind.order_details
values (
  10389, 
  70, 
  12, 
  30, 
  0
);
insert into northwind.order_details
values (
  10390, 
  31, 
  10, 
  60, 
  0.100000001
);
insert into northwind.order_details
values (
  10390, 
  35, 
  14.3999996, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10390, 
  46, 
  9.60000038, 
  45, 
  0
);
insert into northwind.order_details
values (
  10390, 
  72, 
  27.7999992, 
  24, 
  0.100000001
);
insert into northwind.order_details
values (
  10391, 
  13, 
  4.80000019, 
  18, 
  0
);
insert into northwind.order_details
values (
  10392, 
  69, 
  28.7999992, 
  50, 
  0
);
insert into northwind.order_details
values (
  10393, 
  2, 
  15.1999998, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  10393, 
  14, 
  18.6000004, 
  42, 
  0.25
);
insert into northwind.order_details
values (
  10393, 
  25, 
  11.1999998, 
  7, 
  0.25
);
insert into northwind.order_details
values (
  10393, 
  26, 
  24.8999996, 
  70, 
  0.25
);
insert into northwind.order_details
values (
  10393, 
  31, 
  10, 
  32, 
  0
);
insert into northwind.order_details
values (
  10394, 
  13, 
  4.80000019, 
  10, 
  0
);
insert into northwind.order_details
values (
  10394, 
  62, 
  39.4000015, 
  10, 
  0
);
insert into northwind.order_details
values (
  10395, 
  46, 
  9.60000038, 
  28, 
  0.100000001
);
insert into northwind.order_details
values (
  10395, 
  53, 
  26.2000008, 
  70, 
  0.100000001
);
insert into northwind.order_details
values (
  10395, 
  69, 
  28.7999992, 
  8, 
  0
);
insert into northwind.order_details
values (
  10396, 
  23, 
  7.19999981, 
  40, 
  0
);
insert into northwind.order_details
values (
  10396, 
  71, 
  17.2000008, 
  60, 
  0
);
insert into northwind.order_details
values (
  10396, 
  72, 
  27.7999992, 
  21, 
  0
);
insert into northwind.order_details
values (
  10397, 
  21, 
  8, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10397, 
  51, 
  42.4000015, 
  18, 
  0.150000006
);
insert into northwind.order_details
values (
  10398, 
  35, 
  14.3999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10398, 
  55, 
  19.2000008, 
  120, 
  0.100000001
);
insert into northwind.order_details
values (
  10399, 
  68, 
  10, 
  60, 
  0
);
insert into northwind.order_details
values (
  10399, 
  71, 
  17.2000008, 
  30, 
  0
);
insert into northwind.order_details
values (
  10399, 
  76, 
  14.3999996, 
  35, 
  0
);
insert into northwind.order_details
values (
  10399, 
  77, 
  10.3999996, 
  14, 
  0
);
insert into northwind.order_details
values (
  10400, 
  29, 
  99, 
  21, 
  0
);
insert into northwind.order_details
values (
  10400, 
  35, 
  14.3999996, 
  35, 
  0
);
insert into northwind.order_details
values (
  10400, 
  49, 
  16, 
  30, 
  0
);
insert into northwind.order_details
values (
  10401, 
  30, 
  20.7000008, 
  18, 
  0
);
insert into northwind.order_details
values (
  10401, 
  56, 
  30.3999996, 
  70, 
  0
);
insert into northwind.order_details
values (
  10401, 
  65, 
  16.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10401, 
  71, 
  17.2000008, 
  60, 
  0
);
insert into northwind.order_details
values (
  10402, 
  23, 
  7.19999981, 
  60, 
  0
);
insert into northwind.order_details
values (
  10402, 
  63, 
  35.0999985, 
  65, 
  0
);
insert into northwind.order_details
values (
  10403, 
  16, 
  13.8999996, 
  21, 
  0.150000006
);
insert into northwind.order_details
values (
  10403, 
  48, 
  10.1999998, 
  70, 
  0.150000006
);
insert into northwind.order_details
values (
  10404, 
  26, 
  24.8999996, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10404, 
  42, 
  11.1999998, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10404, 
  49, 
  16, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10405, 
  3, 
  8, 
  50, 
  0
);
insert into northwind.order_details
values (
  10406, 
  1, 
  14.3999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10406, 
  21, 
  8, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10406, 
  28, 
  36.4000015, 
  42, 
  0.100000001
);
insert into northwind.order_details
values (
  10406, 
  36, 
  15.1999998, 
  5, 
  0.100000001
);
insert into northwind.order_details
values (
  10406, 
  40, 
  14.6999998, 
  2, 
  0.100000001
);
insert into northwind.order_details
values (
  10407, 
  11, 
  16.7999992, 
  30, 
  0
);
insert into northwind.order_details
values (
  10407, 
  69, 
  28.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10407, 
  71, 
  17.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10408, 
  37, 
  20.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10408, 
  54, 
  5.9000001, 
  6, 
  0
);
insert into northwind.order_details
values (
  10408, 
  62, 
  39.4000015, 
  35, 
  0
);
insert into northwind.order_details
values (
  10409, 
  14, 
  18.6000004, 
  12, 
  0
);
insert into northwind.order_details
values (
  10409, 
  21, 
  8, 
  12, 
  0
);
insert into northwind.order_details
values (
  10410, 
  33, 
  2, 
  49, 
  0
);
insert into northwind.order_details
values (
  10410, 
  59, 
  44, 
  16, 
  0
);
insert into northwind.order_details
values (
  10411, 
  41, 
  7.69999981, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  10411, 
  44, 
  15.5, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10411, 
  59, 
  44, 
  9, 
  0.200000003
);
insert into northwind.order_details
values (
  10412, 
  14, 
  18.6000004, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10413, 
  1, 
  14.3999996, 
  24, 
  0
);
insert into northwind.order_details
values (
  10413, 
  62, 
  39.4000015, 
  40, 
  0
);
insert into northwind.order_details
values (
  10413, 
  76, 
  14.3999996, 
  14, 
  0
);
insert into northwind.order_details
values (
  10414, 
  19, 
  7.30000019, 
  18, 
  0.0500000007
);
insert into northwind.order_details
values (
  10414, 
  33, 
  2, 
  50, 
  0
);
insert into northwind.order_details
values (
  10415, 
  17, 
  31.2000008, 
  2, 
  0
);
insert into northwind.order_details
values (
  10415, 
  33, 
  2, 
  20, 
  0
);
insert into northwind.order_details
values (
  10416, 
  19, 
  7.30000019, 
  20, 
  0
);
insert into northwind.order_details
values (
  10416, 
  53, 
  26.2000008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10416, 
  57, 
  15.6000004, 
  20, 
  0
);
insert into northwind.order_details
values (
  10417, 
  38, 
  210.800003, 
  50, 
  0
);
insert into northwind.order_details
values (
  10417, 
  46, 
  9.60000038, 
  2, 
  0.25
);
insert into northwind.order_details
values (
  10417, 
  68, 
  10, 
  36, 
  0.25
);
insert into northwind.order_details
values (
  10417, 
  77, 
  10.3999996, 
  35, 
  0
);
insert into northwind.order_details
values (
  10418, 
  2, 
  15.1999998, 
  60, 
  0
);
insert into northwind.order_details
values (
  10418, 
  47, 
  7.5999999, 
  55, 
  0
);
insert into northwind.order_details
values (
  10418, 
  61, 
  22.7999992, 
  16, 
  0
);
insert into northwind.order_details
values (
  10418, 
  74, 
  8, 
  15, 
  0
);
insert into northwind.order_details
values (
  10419, 
  60, 
  27.2000008, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10419, 
  69, 
  28.7999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10420, 
  9, 
  77.5999985, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10420, 
  13, 
  4.80000019, 
  2, 
  0.100000001
);
insert into northwind.order_details
values (
  10420, 
  70, 
  12, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10420, 
  73, 
  12, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10421, 
  19, 
  7.30000019, 
  4, 
  0.150000006
);
insert into northwind.order_details
values (
  10421, 
  26, 
  24.8999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10421, 
  53, 
  26.2000008, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10421, 
  77, 
  10.3999996, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10422, 
  26, 
  24.8999996, 
  2, 
  0
);
insert into northwind.order_details
values (
  10423, 
  31, 
  10, 
  14, 
  0
);
insert into northwind.order_details
values (
  10423, 
  59, 
  44, 
  20, 
  0
);
insert into northwind.order_details
values (
  10424, 
  35, 
  14.3999996, 
  60, 
  0.200000003
);
insert into northwind.order_details
values (
  10424, 
  38, 
  210.800003, 
  49, 
  0.200000003
);
insert into northwind.order_details
values (
  10424, 
  68, 
  10, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10425, 
  55, 
  19.2000008, 
  10, 
  0.25
);
insert into northwind.order_details
values (
  10425, 
  76, 
  14.3999996, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10426, 
  56, 
  30.3999996, 
  5, 
  0
);
insert into northwind.order_details
values (
  10426, 
  64, 
  26.6000004, 
  7, 
  0
);
insert into northwind.order_details
values (
  10427, 
  14, 
  18.6000004, 
  35, 
  0
);
insert into northwind.order_details
values (
  10428, 
  46, 
  9.60000038, 
  20, 
  0
);
insert into northwind.order_details
values (
  10429, 
  50, 
  13, 
  40, 
  0
);
insert into northwind.order_details
values (
  10429, 
  63, 
  35.0999985, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10430, 
  17, 
  31.2000008, 
  45, 
  0.200000003
);
insert into northwind.order_details
values (
  10430, 
  21, 
  8, 
  50, 
  0
);
insert into northwind.order_details
values (
  10430, 
  56, 
  30.3999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10430, 
  59, 
  44, 
  70, 
  0.200000003
);
insert into northwind.order_details
values (
  10431, 
  17, 
  31.2000008, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10431, 
  40, 
  14.6999998, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10431, 
  47, 
  7.5999999, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10432, 
  26, 
  24.8999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10432, 
  54, 
  5.9000001, 
  40, 
  0
);
insert into northwind.order_details
values (
  10433, 
  56, 
  30.3999996, 
  28, 
  0
);
insert into northwind.order_details
values (
  10434, 
  11, 
  16.7999992, 
  6, 
  0
);
insert into northwind.order_details
values (
  10434, 
  76, 
  14.3999996, 
  18, 
  0.150000006
);
insert into northwind.order_details
values (
  10435, 
  2, 
  15.1999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10435, 
  22, 
  16.7999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10435, 
  72, 
  27.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10436, 
  46, 
  9.60000038, 
  5, 
  0
);
insert into northwind.order_details
values (
  10436, 
  56, 
  30.3999996, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10436, 
  64, 
  26.6000004, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10436, 
  75, 
  6.19999981, 
  24, 
  0.100000001
);
insert into northwind.order_details
values (
  10437, 
  53, 
  26.2000008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10438, 
  19, 
  7.30000019, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10438, 
  34, 
  11.1999998, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10438, 
  57, 
  15.6000004, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10439, 
  12, 
  30.3999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10439, 
  16, 
  13.8999996, 
  16, 
  0
);
insert into northwind.order_details
values (
  10439, 
  64, 
  26.6000004, 
  6, 
  0
);
insert into northwind.order_details
values (
  10439, 
  74, 
  8, 
  30, 
  0
);
insert into northwind.order_details
values (
  10440, 
  2, 
  15.1999998, 
  45, 
  0.150000006
);
insert into northwind.order_details
values (
  10440, 
  16, 
  13.8999996, 
  49, 
  0.150000006
);
insert into northwind.order_details
values (
  10440, 
  29, 
  99, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  10440, 
  61, 
  22.7999992, 
  90, 
  0.150000006
);
insert into northwind.order_details
values (
  10441, 
  27, 
  35.0999985, 
  50, 
  0
);
insert into northwind.order_details
values (
  10442, 
  11, 
  16.7999992, 
  30, 
  0
);
insert into northwind.order_details
values (
  10442, 
  54, 
  5.9000001, 
  80, 
  0
);
insert into northwind.order_details
values (
  10442, 
  66, 
  13.6000004, 
  60, 
  0
);
insert into northwind.order_details
values (
  10443, 
  11, 
  16.7999992, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10443, 
  28, 
  36.4000015, 
  12, 
  0
);
insert into northwind.order_details
values (
  10444, 
  17, 
  31.2000008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10444, 
  26, 
  24.8999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10444, 
  35, 
  14.3999996, 
  8, 
  0
);
insert into northwind.order_details
values (
  10444, 
  41, 
  7.69999981, 
  30, 
  0
);
insert into northwind.order_details
values (
  10445, 
  39, 
  14.3999996, 
  6, 
  0
);
insert into northwind.order_details
values (
  10445, 
  54, 
  5.9000001, 
  15, 
  0
);
insert into northwind.order_details
values (
  10446, 
  19, 
  7.30000019, 
  12, 
  0.100000001
);
insert into northwind.order_details
values (
  10446, 
  24, 
  3.5999999, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10446, 
  31, 
  10, 
  3, 
  0.100000001
);
insert into northwind.order_details
values (
  10446, 
  52, 
  5.5999999, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10447, 
  19, 
  7.30000019, 
  40, 
  0
);
insert into northwind.order_details
values (
  10447, 
  65, 
  16.7999992, 
  35, 
  0
);
insert into northwind.order_details
values (
  10447, 
  71, 
  17.2000008, 
  2, 
  0
);
insert into northwind.order_details
values (
  10448, 
  26, 
  24.8999996, 
  6, 
  0
);
insert into northwind.order_details
values (
  10448, 
  40, 
  14.6999998, 
  20, 
  0
);
insert into northwind.order_details
values (
  10449, 
  10, 
  24.7999992, 
  14, 
  0
);
insert into northwind.order_details
values (
  10449, 
  52, 
  5.5999999, 
  20, 
  0
);
insert into northwind.order_details
values (
  10449, 
  62, 
  39.4000015, 
  35, 
  0
);
insert into northwind.order_details
values (
  10450, 
  10, 
  24.7999992, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10450, 
  54, 
  5.9000001, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10451, 
  55, 
  19.2000008, 
  120, 
  0.100000001
);
insert into northwind.order_details
values (
  10451, 
  64, 
  26.6000004, 
  35, 
  0.100000001
);
insert into northwind.order_details
values (
  10451, 
  65, 
  16.7999992, 
  28, 
  0.100000001
);
insert into northwind.order_details
values (
  10451, 
  77, 
  10.3999996, 
  55, 
  0.100000001
);
insert into northwind.order_details
values (
  10452, 
  28, 
  36.4000015, 
  15, 
  0
);
insert into northwind.order_details
values (
  10452, 
  44, 
  15.5, 
  100, 
  0.0500000007
);
insert into northwind.order_details
values (
  10453, 
  48, 
  10.1999998, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10453, 
  70, 
  12, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10454, 
  16, 
  13.8999996, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10454, 
  33, 
  2, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10454, 
  46, 
  9.60000038, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10455, 
  39, 
  14.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10455, 
  53, 
  26.2000008, 
  50, 
  0
);
insert into northwind.order_details
values (
  10455, 
  61, 
  22.7999992, 
  25, 
  0
);
insert into northwind.order_details
values (
  10455, 
  71, 
  17.2000008, 
  30, 
  0
);
insert into northwind.order_details
values (
  10456, 
  21, 
  8, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10456, 
  49, 
  16, 
  21, 
  0.150000006
);
insert into northwind.order_details
values (
  10457, 
  59, 
  44, 
  36, 
  0
);
insert into northwind.order_details
values (
  10458, 
  26, 
  24.8999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10458, 
  28, 
  36.4000015, 
  30, 
  0
);
insert into northwind.order_details
values (
  10458, 
  43, 
  36.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10458, 
  56, 
  30.3999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10458, 
  71, 
  17.2000008, 
  50, 
  0
);
insert into northwind.order_details
values (
  10459, 
  7, 
  24, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10459, 
  46, 
  9.60000038, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10459, 
  72, 
  27.7999992, 
  40, 
  0
);
insert into northwind.order_details
values (
  10460, 
  68, 
  10, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10460, 
  75, 
  6.19999981, 
  4, 
  0.25
);
insert into northwind.order_details
values (
  10461, 
  21, 
  8, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10461, 
  30, 
  20.7000008, 
  28, 
  0.25
);
insert into northwind.order_details
values (
  10461, 
  55, 
  19.2000008, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10462, 
  13, 
  4.80000019, 
  1, 
  0
);
insert into northwind.order_details
values (
  10462, 
  23, 
  7.19999981, 
  21, 
  0
);
insert into northwind.order_details
values (
  10463, 
  19, 
  7.30000019, 
  21, 
  0
);
insert into northwind.order_details
values (
  10463, 
  42, 
  11.1999998, 
  50, 
  0
);
insert into northwind.order_details
values (
  10464, 
  4, 
  17.6000004, 
  16, 
  0.200000003
);
insert into northwind.order_details
values (
  10464, 
  43, 
  36.7999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10464, 
  56, 
  30.3999996, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10464, 
  60, 
  27.2000008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10465, 
  24, 
  3.5999999, 
  25, 
  0
);
insert into northwind.order_details
values (
  10465, 
  29, 
  99, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10465, 
  40, 
  14.6999998, 
  20, 
  0
);
insert into northwind.order_details
values (
  10465, 
  45, 
  7.5999999, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10465, 
  50, 
  13, 
  25, 
  0
);
insert into northwind.order_details
values (
  10466, 
  11, 
  16.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10466, 
  46, 
  9.60000038, 
  5, 
  0
);
insert into northwind.order_details
values (
  10467, 
  24, 
  3.5999999, 
  28, 
  0
);
insert into northwind.order_details
values (
  10467, 
  25, 
  11.1999998, 
  12, 
  0
);
insert into northwind.order_details
values (
  10468, 
  30, 
  20.7000008, 
  8, 
  0
);
insert into northwind.order_details
values (
  10468, 
  43, 
  36.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10469, 
  2, 
  15.1999998, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10469, 
  16, 
  13.8999996, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10469, 
  44, 
  15.5, 
  2, 
  0.150000006
);
insert into northwind.order_details
values (
  10470, 
  18, 
  50, 
  30, 
  0
);
insert into northwind.order_details
values (
  10470, 
  23, 
  7.19999981, 
  15, 
  0
);
insert into northwind.order_details
values (
  10470, 
  64, 
  26.6000004, 
  8, 
  0
);
insert into northwind.order_details
values (
  10471, 
  7, 
  24, 
  30, 
  0
);
insert into northwind.order_details
values (
  10471, 
  56, 
  30.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10472, 
  24, 
  3.5999999, 
  80, 
  0.0500000007
);
insert into northwind.order_details
values (
  10472, 
  51, 
  42.4000015, 
  18, 
  0
);
insert into northwind.order_details
values (
  10473, 
  33, 
  2, 
  12, 
  0
);
insert into northwind.order_details
values (
  10473, 
  71, 
  17.2000008, 
  12, 
  0
);
insert into northwind.order_details
values (
  10474, 
  14, 
  18.6000004, 
  12, 
  0
);
insert into northwind.order_details
values (
  10474, 
  28, 
  36.4000015, 
  18, 
  0
);
insert into northwind.order_details
values (
  10474, 
  40, 
  14.6999998, 
  21, 
  0
);
insert into northwind.order_details
values (
  10474, 
  75, 
  6.19999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10475, 
  31, 
  10, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10475, 
  66, 
  13.6000004, 
  60, 
  0.150000006
);
insert into northwind.order_details
values (
  10475, 
  76, 
  14.3999996, 
  42, 
  0.150000006
);
insert into northwind.order_details
values (
  10476, 
  55, 
  19.2000008, 
  2, 
  0.0500000007
);
insert into northwind.order_details
values (
  10476, 
  70, 
  12, 
  12, 
  0
);
insert into northwind.order_details
values (
  10477, 
  1, 
  14.3999996, 
  15, 
  0
);
insert into northwind.order_details
values (
  10477, 
  21, 
  8, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10477, 
  39, 
  14.3999996, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10478, 
  10, 
  24.7999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10479, 
  38, 
  210.800003, 
  30, 
  0
);
insert into northwind.order_details
values (
  10479, 
  53, 
  26.2000008, 
  28, 
  0
);
insert into northwind.order_details
values (
  10479, 
  59, 
  44, 
  60, 
  0
);
insert into northwind.order_details
values (
  10479, 
  64, 
  26.6000004, 
  30, 
  0
);
insert into northwind.order_details
values (
  10480, 
  47, 
  7.5999999, 
  30, 
  0
);
insert into northwind.order_details
values (
  10480, 
  59, 
  44, 
  12, 
  0
);
insert into northwind.order_details
values (
  10481, 
  49, 
  16, 
  24, 
  0
);
insert into northwind.order_details
values (
  10481, 
  60, 
  27.2000008, 
  40, 
  0
);
insert into northwind.order_details
values (
  10482, 
  40, 
  14.6999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10483, 
  34, 
  11.1999998, 
  35, 
  0.0500000007
);
insert into northwind.order_details
values (
  10483, 
  77, 
  10.3999996, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10484, 
  21, 
  8, 
  14, 
  0
);
insert into northwind.order_details
values (
  10484, 
  40, 
  14.6999998, 
  10, 
  0
);
insert into northwind.order_details
values (
  10484, 
  51, 
  42.4000015, 
  3, 
  0
);
insert into northwind.order_details
values (
  10485, 
  2, 
  15.1999998, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10485, 
  3, 
  8, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10485, 
  55, 
  19.2000008, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10485, 
  70, 
  12, 
  60, 
  0.100000001
);
insert into northwind.order_details
values (
  10486, 
  11, 
  16.7999992, 
  5, 
  0
);
insert into northwind.order_details
values (
  10486, 
  51, 
  42.4000015, 
  25, 
  0
);
insert into northwind.order_details
values (
  10486, 
  74, 
  8, 
  16, 
  0
);
insert into northwind.order_details
values (
  10487, 
  19, 
  7.30000019, 
  5, 
  0
);
insert into northwind.order_details
values (
  10487, 
  26, 
  24.8999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10487, 
  54, 
  5.9000001, 
  24, 
  0.25
);
insert into northwind.order_details
values (
  10488, 
  59, 
  44, 
  30, 
  0
);
insert into northwind.order_details
values (
  10488, 
  73, 
  12, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10489, 
  11, 
  16.7999992, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10489, 
  16, 
  13.8999996, 
  18, 
  0
);
insert into northwind.order_details
values (
  10490, 
  59, 
  44, 
  60, 
  0
);
insert into northwind.order_details
values (
  10490, 
  68, 
  10, 
  30, 
  0
);
insert into northwind.order_details
values (
  10490, 
  75, 
  6.19999981, 
  36, 
  0
);
insert into northwind.order_details
values (
  10491, 
  44, 
  15.5, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10491, 
  77, 
  10.3999996, 
  7, 
  0.150000006
);
insert into northwind.order_details
values (
  10492, 
  25, 
  11.1999998, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10492, 
  42, 
  11.1999998, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10493, 
  65, 
  16.7999992, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10493, 
  66, 
  13.6000004, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10493, 
  69, 
  28.7999992, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10494, 
  56, 
  30.3999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10495, 
  23, 
  7.19999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10495, 
  41, 
  7.69999981, 
  20, 
  0
);
insert into northwind.order_details
values (
  10495, 
  77, 
  10.3999996, 
  5, 
  0
);
insert into northwind.order_details
values (
  10496, 
  31, 
  10, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10497, 
  56, 
  30.3999996, 
  14, 
  0
);
insert into northwind.order_details
values (
  10497, 
  72, 
  27.7999992, 
  25, 
  0
);
insert into northwind.order_details
values (
  10497, 
  77, 
  10.3999996, 
  25, 
  0
);
insert into northwind.order_details
values (
  10498, 
  24, 
  4.5, 
  14, 
  0
);
insert into northwind.order_details
values (
  10498, 
  40, 
  18.3999996, 
  5, 
  0
);
insert into northwind.order_details
values (
  10498, 
  42, 
  14, 
  30, 
  0
);
insert into northwind.order_details
values (
  10499, 
  28, 
  45.5999985, 
  20, 
  0
);
insert into northwind.order_details
values (
  10499, 
  49, 
  20, 
  25, 
  0
);
insert into northwind.order_details
values (
  10500, 
  15, 
  15.5, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10500, 
  28, 
  45.5999985, 
  8, 
  0.0500000007
);
insert into northwind.order_details
values (
  10501, 
  54, 
  7.44999981, 
  20, 
  0
);
insert into northwind.order_details
values (
  10502, 
  45, 
  9.5, 
  21, 
  0
);
insert into northwind.order_details
values (
  10502, 
  53, 
  32.7999992, 
  6, 
  0
);
insert into northwind.order_details
values (
  10502, 
  67, 
  14, 
  30, 
  0
);
insert into northwind.order_details
values (
  10503, 
  14, 
  23.25, 
  70, 
  0
);
insert into northwind.order_details
values (
  10503, 
  65, 
  21.0499992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10504, 
  2, 
  19, 
  12, 
  0
);
insert into northwind.order_details
values (
  10504, 
  21, 
  10, 
  12, 
  0
);
insert into northwind.order_details
values (
  10504, 
  53, 
  32.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10504, 
  61, 
  28.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  10505, 
  62, 
  49.2999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10506, 
  25, 
  14, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10506, 
  70, 
  15, 
  14, 
  0.100000001
);
insert into northwind.order_details
values (
  10507, 
  43, 
  46, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10507, 
  48, 
  12.75, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10508, 
  13, 
  6, 
  10, 
  0
);
insert into northwind.order_details
values (
  10508, 
  39, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10509, 
  28, 
  45.5999985, 
  3, 
  0
);
insert into northwind.order_details
values (
  10510, 
  29, 
  123.790001, 
  36, 
  0
);
insert into northwind.order_details
values (
  10510, 
  75, 
  7.75, 
  36, 
  0.100000001
);
insert into northwind.order_details
values (
  10511, 
  4, 
  22, 
  50, 
  0.150000006
);
insert into northwind.order_details
values (
  10511, 
  7, 
  30, 
  50, 
  0.150000006
);
insert into northwind.order_details
values (
  10511, 
  8, 
  40, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10512, 
  24, 
  4.5, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10512, 
  46, 
  12, 
  9, 
  0.150000006
);
insert into northwind.order_details
values (
  10512, 
  47, 
  9.5, 
  6, 
  0.150000006
);
insert into northwind.order_details
values (
  10512, 
  60, 
  34, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10513, 
  21, 
  10, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10513, 
  32, 
  32, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10513, 
  61, 
  28.5, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10514, 
  20, 
  81, 
  39, 
  0
);
insert into northwind.order_details
values (
  10514, 
  28, 
  45.5999985, 
  35, 
  0
);
insert into northwind.order_details
values (
  10514, 
  56, 
  38, 
  70, 
  0
);
insert into northwind.order_details
values (
  10514, 
  65, 
  21.0499992, 
  39, 
  0
);
insert into northwind.order_details
values (
  10514, 
  75, 
  7.75, 
  50, 
  0
);
insert into northwind.order_details
values (
  10515, 
  9, 
  97, 
  16, 
  0.150000006
);
insert into northwind.order_details
values (
  10515, 
  16, 
  17.4500008, 
  50, 
  0
);
insert into northwind.order_details
values (
  10515, 
  27, 
  43.9000015, 
  120, 
  0
);
insert into northwind.order_details
values (
  10515, 
  33, 
  2.5, 
  16, 
  0.150000006
);
insert into northwind.order_details
values (
  10515, 
  60, 
  34, 
  84, 
  0.150000006
);
insert into northwind.order_details
values (
  10516, 
  18, 
  62.5, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10516, 
  41, 
  9.64999962, 
  80, 
  0.100000001
);
insert into northwind.order_details
values (
  10516, 
  42, 
  14, 
  20, 
  0
);
insert into northwind.order_details
values (
  10517, 
  52, 
  7, 
  6, 
  0
);
insert into northwind.order_details
values (
  10517, 
  59, 
  55, 
  4, 
  0
);
insert into northwind.order_details
values (
  10517, 
  70, 
  15, 
  6, 
  0
);
insert into northwind.order_details
values (
  10518, 
  24, 
  4.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10518, 
  38, 
  263.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10518, 
  44, 
  19.4500008, 
  9, 
  0
);
insert into northwind.order_details
values (
  10519, 
  10, 
  31, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10519, 
  56, 
  38, 
  40, 
  0
);
insert into northwind.order_details
values (
  10519, 
  60, 
  34, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10520, 
  24, 
  4.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10520, 
  53, 
  32.7999992, 
  5, 
  0
);
insert into northwind.order_details
values (
  10521, 
  35, 
  18, 
  3, 
  0
);
insert into northwind.order_details
values (
  10521, 
  41, 
  9.64999962, 
  10, 
  0
);
insert into northwind.order_details
values (
  10521, 
  68, 
  12.5, 
  6, 
  0
);
insert into northwind.order_details
values (
  10522, 
  1, 
  18, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10522, 
  8, 
  40, 
  24, 
  0
);
insert into northwind.order_details
values (
  10522, 
  30, 
  25.8899994, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10522, 
  40, 
  18.3999996, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  10523, 
  17, 
  39, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10523, 
  20, 
  81, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10523, 
  37, 
  26, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10523, 
  41, 
  9.64999962, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10524, 
  10, 
  31, 
  2, 
  0
);
insert into northwind.order_details
values (
  10524, 
  30, 
  25.8899994, 
  10, 
  0
);
insert into northwind.order_details
values (
  10524, 
  43, 
  46, 
  60, 
  0
);
insert into northwind.order_details
values (
  10524, 
  54, 
  7.44999981, 
  15, 
  0
);
insert into northwind.order_details
values (
  10525, 
  36, 
  19, 
  30, 
  0
);
insert into northwind.order_details
values (
  10525, 
  40, 
  18.3999996, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10526, 
  1, 
  18, 
  8, 
  0.150000006
);
insert into northwind.order_details
values (
  10526, 
  13, 
  6, 
  10, 
  0
);
insert into northwind.order_details
values (
  10526, 
  56, 
  38, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10527, 
  4, 
  22, 
  50, 
  0.100000001
);
insert into northwind.order_details
values (
  10527, 
  36, 
  19, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10528, 
  11, 
  21, 
  3, 
  0
);
insert into northwind.order_details
values (
  10528, 
  33, 
  2.5, 
  8, 
  0.200000003
);
insert into northwind.order_details
values (
  10528, 
  72, 
  34.7999992, 
  9, 
  0
);
insert into northwind.order_details
values (
  10529, 
  55, 
  24, 
  14, 
  0
);
insert into northwind.order_details
values (
  10529, 
  68, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10529, 
  69, 
  36, 
  10, 
  0
);
insert into northwind.order_details
values (
  10530, 
  17, 
  39, 
  40, 
  0
);
insert into northwind.order_details
values (
  10530, 
  43, 
  46, 
  25, 
  0
);
insert into northwind.order_details
values (
  10530, 
  61, 
  28.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10530, 
  76, 
  18, 
  50, 
  0
);
insert into northwind.order_details
values (
  10531, 
  59, 
  55, 
  2, 
  0
);
insert into northwind.order_details
values (
  10532, 
  30, 
  25.8899994, 
  15, 
  0
);
insert into northwind.order_details
values (
  10532, 
  66, 
  17, 
  24, 
  0
);
insert into northwind.order_details
values (
  10533, 
  4, 
  22, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10533, 
  72, 
  34.7999992, 
  24, 
  0
);
insert into northwind.order_details
values (
  10533, 
  73, 
  15, 
  24, 
  0.0500000007
);
insert into northwind.order_details
values (
  10534, 
  30, 
  25.8899994, 
  10, 
  0
);
insert into northwind.order_details
values (
  10534, 
  40, 
  18.3999996, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10534, 
  54, 
  7.44999981, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10535, 
  11, 
  21, 
  50, 
  0.100000001
);
insert into northwind.order_details
values (
  10535, 
  40, 
  18.3999996, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10535, 
  57, 
  19.5, 
  5, 
  0.100000001
);
insert into northwind.order_details
values (
  10535, 
  59, 
  55, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10536, 
  12, 
  38, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10536, 
  31, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10536, 
  33, 
  2.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10536, 
  60, 
  34, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10537, 
  31, 
  12.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10537, 
  51, 
  53, 
  6, 
  0
);
insert into northwind.order_details
values (
  10537, 
  58, 
  13.25, 
  20, 
  0
);
insert into northwind.order_details
values (
  10537, 
  72, 
  34.7999992, 
  21, 
  0
);
insert into northwind.order_details
values (
  10537, 
  73, 
  15, 
  9, 
  0
);
insert into northwind.order_details
values (
  10538, 
  70, 
  15, 
  7, 
  0
);
insert into northwind.order_details
values (
  10538, 
  72, 
  34.7999992, 
  1, 
  0
);
insert into northwind.order_details
values (
  10539, 
  13, 
  6, 
  8, 
  0
);
insert into northwind.order_details
values (
  10539, 
  21, 
  10, 
  15, 
  0
);
insert into northwind.order_details
values (
  10539, 
  33, 
  2.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10539, 
  49, 
  20, 
  6, 
  0
);
insert into northwind.order_details
values (
  10540, 
  3, 
  10, 
  60, 
  0
);
insert into northwind.order_details
values (
  10540, 
  26, 
  31.2299995, 
  40, 
  0
);
insert into northwind.order_details
values (
  10540, 
  38, 
  263.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10540, 
  68, 
  12.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10541, 
  24, 
  4.5, 
  35, 
  0.100000001
);
insert into northwind.order_details
values (
  10541, 
  38, 
  263.5, 
  4, 
  0.100000001
);
insert into northwind.order_details
values (
  10541, 
  65, 
  21.0499992, 
  36, 
  0.100000001
);
insert into northwind.order_details
values (
  10541, 
  71, 
  21.5, 
  9, 
  0.100000001
);
insert into northwind.order_details
values (
  10542, 
  11, 
  21, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10542, 
  54, 
  7.44999981, 
  24, 
  0.0500000007
);
insert into northwind.order_details
values (
  10543, 
  12, 
  38, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10543, 
  23, 
  9, 
  70, 
  0.150000006
);
insert into northwind.order_details
values (
  10544, 
  28, 
  45.5999985, 
  7, 
  0
);
insert into northwind.order_details
values (
  10544, 
  67, 
  14, 
  7, 
  0
);
insert into northwind.order_details
values (
  10545, 
  11, 
  21, 
  10, 
  0
);
insert into northwind.order_details
values (
  10546, 
  7, 
  30, 
  10, 
  0
);
insert into northwind.order_details
values (
  10546, 
  35, 
  18, 
  30, 
  0
);
insert into northwind.order_details
values (
  10546, 
  62, 
  49.2999992, 
  40, 
  0
);
insert into northwind.order_details
values (
  10547, 
  32, 
  32, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  10547, 
  36, 
  19, 
  60, 
  0
);
insert into northwind.order_details
values (
  10548, 
  34, 
  14, 
  10, 
  0.25
);
insert into northwind.order_details
values (
  10548, 
  41, 
  9.64999962, 
  14, 
  0
);
insert into northwind.order_details
values (
  10549, 
  31, 
  12.5, 
  55, 
  0.150000006
);
insert into northwind.order_details
values (
  10549, 
  45, 
  9.5, 
  100, 
  0.150000006
);
insert into northwind.order_details
values (
  10549, 
  51, 
  53, 
  48, 
  0.150000006
);
insert into northwind.order_details
values (
  10550, 
  17, 
  39, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10550, 
  19, 
  9.19999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10550, 
  21, 
  10, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10550, 
  61, 
  28.5, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10551, 
  16, 
  17.4500008, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10551, 
  35, 
  18, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10551, 
  44, 
  19.4500008, 
  40, 
  0
);
insert into northwind.order_details
values (
  10552, 
  69, 
  36, 
  18, 
  0
);
insert into northwind.order_details
values (
  10552, 
  75, 
  7.75, 
  30, 
  0
);
insert into northwind.order_details
values (
  10553, 
  11, 
  21, 
  15, 
  0
);
insert into northwind.order_details
values (
  10553, 
  16, 
  17.4500008, 
  14, 
  0
);
insert into northwind.order_details
values (
  10553, 
  22, 
  21, 
  24, 
  0
);
insert into northwind.order_details
values (
  10553, 
  31, 
  12.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10553, 
  35, 
  18, 
  6, 
  0
);
insert into northwind.order_details
values (
  10554, 
  16, 
  17.4500008, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10554, 
  23, 
  9, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10554, 
  62, 
  49.2999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10554, 
  77, 
  13, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10555, 
  14, 
  23.25, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10555, 
  19, 
  9.19999981, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10555, 
  24, 
  4.5, 
  18, 
  0.200000003
);
insert into northwind.order_details
values (
  10555, 
  51, 
  53, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10555, 
  56, 
  38, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10556, 
  72, 
  34.7999992, 
  24, 
  0
);
insert into northwind.order_details
values (
  10557, 
  64, 
  33.25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10557, 
  75, 
  7.75, 
  20, 
  0
);
insert into northwind.order_details
values (
  10558, 
  47, 
  9.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  10558, 
  51, 
  53, 
  20, 
  0
);
insert into northwind.order_details
values (
  10558, 
  52, 
  7, 
  30, 
  0
);
insert into northwind.order_details
values (
  10558, 
  53, 
  32.7999992, 
  18, 
  0
);
insert into northwind.order_details
values (
  10558, 
  73, 
  15, 
  3, 
  0
);
insert into northwind.order_details
values (
  10559, 
  41, 
  9.64999962, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10559, 
  55, 
  24, 
  18, 
  0.0500000007
);
insert into northwind.order_details
values (
  10560, 
  30, 
  25.8899994, 
  20, 
  0
);
insert into northwind.order_details
values (
  10560, 
  62, 
  49.2999992, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10561, 
  44, 
  19.4500008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10561, 
  51, 
  53, 
  50, 
  0
);
insert into northwind.order_details
values (
  10562, 
  33, 
  2.5, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10562, 
  62, 
  49.2999992, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10563, 
  36, 
  19, 
  25, 
  0
);
insert into northwind.order_details
values (
  10563, 
  52, 
  7, 
  70, 
  0
);
insert into northwind.order_details
values (
  10564, 
  17, 
  39, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10564, 
  31, 
  12.5, 
  6, 
  0.0500000007
);
insert into northwind.order_details
values (
  10564, 
  55, 
  24, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10565, 
  24, 
  4.5, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10565, 
  64, 
  33.25, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10566, 
  11, 
  21, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10566, 
  18, 
  62.5, 
  18, 
  0.150000006
);
insert into northwind.order_details
values (
  10566, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10567, 
  31, 
  12.5, 
  60, 
  0.200000003
);
insert into northwind.order_details
values (
  10567, 
  51, 
  53, 
  3, 
  0
);
insert into northwind.order_details
values (
  10567, 
  59, 
  55, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10568, 
  10, 
  31, 
  5, 
  0
);
insert into northwind.order_details
values (
  10569, 
  31, 
  12.5, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10569, 
  76, 
  18, 
  30, 
  0
);
insert into northwind.order_details
values (
  10570, 
  11, 
  21, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10570, 
  56, 
  38, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10571, 
  14, 
  23.25, 
  11, 
  0.150000006
);
insert into northwind.order_details
values (
  10571, 
  42, 
  14, 
  28, 
  0.150000006
);
insert into northwind.order_details
values (
  10572, 
  16, 
  17.4500008, 
  12, 
  0.100000001
);
insert into northwind.order_details
values (
  10572, 
  32, 
  32, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10572, 
  40, 
  18.3999996, 
  50, 
  0
);
insert into northwind.order_details
values (
  10572, 
  75, 
  7.75, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10573, 
  17, 
  39, 
  18, 
  0
);
insert into northwind.order_details
values (
  10573, 
  34, 
  14, 
  40, 
  0
);
insert into northwind.order_details
values (
  10573, 
  53, 
  32.7999992, 
  25, 
  0
);
insert into northwind.order_details
values (
  10574, 
  33, 
  2.5, 
  14, 
  0
);
insert into northwind.order_details
values (
  10574, 
  40, 
  18.3999996, 
  2, 
  0
);
insert into northwind.order_details
values (
  10574, 
  62, 
  49.2999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10574, 
  64, 
  33.25, 
  6, 
  0
);
insert into northwind.order_details
values (
  10575, 
  59, 
  55, 
  12, 
  0
);
insert into northwind.order_details
values (
  10575, 
  63, 
  43.9000015, 
  6, 
  0
);
insert into northwind.order_details
values (
  10575, 
  72, 
  34.7999992, 
  30, 
  0
);
insert into northwind.order_details
values (
  10575, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10576, 
  1, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10576, 
  31, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10576, 
  44, 
  19.4500008, 
  21, 
  0
);
insert into northwind.order_details
values (
  10577, 
  39, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10577, 
  75, 
  7.75, 
  20, 
  0
);
insert into northwind.order_details
values (
  10577, 
  77, 
  13, 
  18, 
  0
);
insert into northwind.order_details
values (
  10578, 
  35, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10578, 
  57, 
  19.5, 
  6, 
  0
);
insert into northwind.order_details
values (
  10579, 
  15, 
  15.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10579, 
  75, 
  7.75, 
  21, 
  0
);
insert into northwind.order_details
values (
  10580, 
  14, 
  23.25, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10580, 
  41, 
  9.64999962, 
  9, 
  0.0500000007
);
insert into northwind.order_details
values (
  10580, 
  65, 
  21.0499992, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10581, 
  75, 
  7.75, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10582, 
  57, 
  19.5, 
  4, 
  0
);
insert into northwind.order_details
values (
  10582, 
  76, 
  18, 
  14, 
  0
);
insert into northwind.order_details
values (
  10583, 
  29, 
  123.790001, 
  10, 
  0
);
insert into northwind.order_details
values (
  10583, 
  60, 
  34, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  10583, 
  69, 
  36, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10584, 
  31, 
  12.5, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10585, 
  47, 
  9.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10586, 
  52, 
  7, 
  4, 
  0.150000006
);
insert into northwind.order_details
values (
  10587, 
  26, 
  31.2299995, 
  6, 
  0
);
insert into northwind.order_details
values (
  10587, 
  35, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10587, 
  77, 
  13, 
  20, 
  0
);
insert into northwind.order_details
values (
  10588, 
  18, 
  62.5, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10588, 
  42, 
  14, 
  100, 
  0.200000003
);
insert into northwind.order_details
values (
  10589, 
  35, 
  18, 
  4, 
  0
);
insert into northwind.order_details
values (
  10590, 
  1, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10590, 
  77, 
  13, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10591, 
  3, 
  10, 
  14, 
  0
);
insert into northwind.order_details
values (
  10591, 
  7, 
  30, 
  10, 
  0
);
insert into northwind.order_details
values (
  10591, 
  54, 
  7.44999981, 
  50, 
  0
);
insert into northwind.order_details
values (
  10592, 
  15, 
  15.5, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10592, 
  26, 
  31.2299995, 
  5, 
  0.0500000007
);
insert into northwind.order_details
values (
  10593, 
  20, 
  81, 
  21, 
  0.200000003
);
insert into northwind.order_details
values (
  10593, 
  69, 
  36, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10593, 
  76, 
  18, 
  4, 
  0.200000003
);
insert into northwind.order_details
values (
  10594, 
  52, 
  7, 
  24, 
  0
);
insert into northwind.order_details
values (
  10594, 
  58, 
  13.25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10595, 
  35, 
  18, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10595, 
  61, 
  28.5, 
  120, 
  0.25
);
insert into northwind.order_details
values (
  10595, 
  69, 
  36, 
  65, 
  0.25
);
insert into northwind.order_details
values (
  10596, 
  56, 
  38, 
  5, 
  0.200000003
);
insert into northwind.order_details
values (
  10596, 
  63, 
  43.9000015, 
  24, 
  0.200000003
);
insert into northwind.order_details
values (
  10596, 
  75, 
  7.75, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10597, 
  24, 
  4.5, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10597, 
  57, 
  19.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10597, 
  65, 
  21.0499992, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10598, 
  27, 
  43.9000015, 
  50, 
  0
);
insert into northwind.order_details
values (
  10598, 
  71, 
  21.5, 
  9, 
  0
);
insert into northwind.order_details
values (
  10599, 
  62, 
  49.2999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10600, 
  54, 
  7.44999981, 
  4, 
  0
);
insert into northwind.order_details
values (
  10600, 
  73, 
  15, 
  30, 
  0
);
insert into northwind.order_details
values (
  10601, 
  13, 
  6, 
  60, 
  0
);
insert into northwind.order_details
values (
  10601, 
  59, 
  55, 
  35, 
  0
);
insert into northwind.order_details
values (
  10602, 
  77, 
  13, 
  5, 
  0.25
);
insert into northwind.order_details
values (
  10603, 
  22, 
  21, 
  48, 
  0
);
insert into northwind.order_details
values (
  10603, 
  49, 
  20, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10604, 
  48, 
  12.75, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10604, 
  76, 
  18, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10605, 
  16, 
  17.4500008, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10605, 
  59, 
  55, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10605, 
  60, 
  34, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  10605, 
  71, 
  21.5, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10606, 
  4, 
  22, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10606, 
  55, 
  24, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10606, 
  62, 
  49.2999992, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10607, 
  7, 
  30, 
  45, 
  0
);
insert into northwind.order_details
values (
  10607, 
  17, 
  39, 
  100, 
  0
);
insert into northwind.order_details
values (
  10607, 
  33, 
  2.5, 
  14, 
  0
);
insert into northwind.order_details
values (
  10607, 
  40, 
  18.3999996, 
  42, 
  0
);
insert into northwind.order_details
values (
  10607, 
  72, 
  34.7999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10608, 
  56, 
  38, 
  28, 
  0
);
insert into northwind.order_details
values (
  10609, 
  1, 
  18, 
  3, 
  0
);
insert into northwind.order_details
values (
  10609, 
  10, 
  31, 
  10, 
  0
);
insert into northwind.order_details
values (
  10609, 
  21, 
  10, 
  6, 
  0
);
insert into northwind.order_details
values (
  10610, 
  36, 
  19, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10611, 
  1, 
  18, 
  6, 
  0
);
insert into northwind.order_details
values (
  10611, 
  2, 
  19, 
  10, 
  0
);
insert into northwind.order_details
values (
  10611, 
  60, 
  34, 
  15, 
  0
);
insert into northwind.order_details
values (
  10612, 
  10, 
  31, 
  70, 
  0
);
insert into northwind.order_details
values (
  10612, 
  36, 
  19, 
  55, 
  0
);
insert into northwind.order_details
values (
  10612, 
  49, 
  20, 
  18, 
  0
);
insert into northwind.order_details
values (
  10612, 
  60, 
  34, 
  40, 
  0
);
insert into northwind.order_details
values (
  10612, 
  76, 
  18, 
  80, 
  0
);
insert into northwind.order_details
values (
  10613, 
  13, 
  6, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10613, 
  75, 
  7.75, 
  40, 
  0
);
insert into northwind.order_details
values (
  10614, 
  11, 
  21, 
  14, 
  0
);
insert into northwind.order_details
values (
  10614, 
  21, 
  10, 
  8, 
  0
);
insert into northwind.order_details
values (
  10614, 
  39, 
  18, 
  5, 
  0
);
insert into northwind.order_details
values (
  10615, 
  55, 
  24, 
  5, 
  0
);
insert into northwind.order_details
values (
  10616, 
  38, 
  263.5, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10616, 
  56, 
  38, 
  14, 
  0
);
insert into northwind.order_details
values (
  10616, 
  70, 
  15, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10616, 
  71, 
  21.5, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10617, 
  59, 
  55, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10618, 
  6, 
  25, 
  70, 
  0
);
insert into northwind.order_details
values (
  10618, 
  56, 
  38, 
  20, 
  0
);
insert into northwind.order_details
values (
  10618, 
  68, 
  12.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10619, 
  21, 
  10, 
  42, 
  0
);
insert into northwind.order_details
values (
  10619, 
  22, 
  21, 
  40, 
  0
);
insert into northwind.order_details
values (
  10620, 
  24, 
  4.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10620, 
  52, 
  7, 
  5, 
  0
);
insert into northwind.order_details
values (
  10621, 
  19, 
  9.19999981, 
  5, 
  0
);
insert into northwind.order_details
values (
  10621, 
  23, 
  9, 
  10, 
  0
);
insert into northwind.order_details
values (
  10621, 
  70, 
  15, 
  20, 
  0
);
insert into northwind.order_details
values (
  10621, 
  71, 
  21.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10622, 
  2, 
  19, 
  20, 
  0
);
insert into northwind.order_details
values (
  10622, 
  68, 
  12.5, 
  18, 
  0.200000003
);
insert into northwind.order_details
values (
  10623, 
  14, 
  23.25, 
  21, 
  0
);
insert into northwind.order_details
values (
  10623, 
  19, 
  9.19999981, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10623, 
  21, 
  10, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10623, 
  24, 
  4.5, 
  3, 
  0
);
insert into northwind.order_details
values (
  10623, 
  35, 
  18, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10624, 
  28, 
  45.5999985, 
  10, 
  0
);
insert into northwind.order_details
values (
  10624, 
  29, 
  123.790001, 
  6, 
  0
);
insert into northwind.order_details
values (
  10624, 
  44, 
  19.4500008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10625, 
  14, 
  23.25, 
  3, 
  0
);
insert into northwind.order_details
values (
  10625, 
  42, 
  14, 
  5, 
  0
);
insert into northwind.order_details
values (
  10625, 
  60, 
  34, 
  10, 
  0
);
insert into northwind.order_details
values (
  10626, 
  53, 
  32.7999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10626, 
  60, 
  34, 
  20, 
  0
);
insert into northwind.order_details
values (
  10626, 
  71, 
  21.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10627, 
  62, 
  49.2999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10627, 
  73, 
  15, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10628, 
  1, 
  18, 
  25, 
  0
);
insert into northwind.order_details
values (
  10629, 
  29, 
  123.790001, 
  20, 
  0
);
insert into northwind.order_details
values (
  10629, 
  64, 
  33.25, 
  9, 
  0
);
insert into northwind.order_details
values (
  10630, 
  55, 
  24, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10630, 
  76, 
  18, 
  35, 
  0
);
insert into northwind.order_details
values (
  10631, 
  75, 
  7.75, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10632, 
  2, 
  19, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10632, 
  33, 
  2.5, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10633, 
  12, 
  38, 
  36, 
  0.150000006
);
insert into northwind.order_details
values (
  10633, 
  13, 
  6, 
  13, 
  0.150000006
);
insert into northwind.order_details
values (
  10633, 
  26, 
  31.2299995, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10633, 
  62, 
  49.2999992, 
  80, 
  0.150000006
);
insert into northwind.order_details
values (
  10634, 
  7, 
  30, 
  35, 
  0
);
insert into northwind.order_details
values (
  10634, 
  18, 
  62.5, 
  50, 
  0
);
insert into northwind.order_details
values (
  10634, 
  51, 
  53, 
  15, 
  0
);
insert into northwind.order_details
values (
  10634, 
  75, 
  7.75, 
  2, 
  0
);
insert into northwind.order_details
values (
  10635, 
  4, 
  22, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10635, 
  5, 
  21.3500004, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10635, 
  22, 
  21, 
  40, 
  0
);
insert into northwind.order_details
values (
  10636, 
  4, 
  22, 
  25, 
  0
);
insert into northwind.order_details
values (
  10636, 
  58, 
  13.25, 
  6, 
  0
);
insert into northwind.order_details
values (
  10637, 
  11, 
  21, 
  10, 
  0
);
insert into northwind.order_details
values (
  10637, 
  50, 
  16.25, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10637, 
  56, 
  38, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10638, 
  45, 
  9.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10638, 
  65, 
  21.0499992, 
  21, 
  0
);
insert into northwind.order_details
values (
  10638, 
  72, 
  34.7999992, 
  60, 
  0
);
insert into northwind.order_details
values (
  10639, 
  18, 
  62.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10640, 
  69, 
  36, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10640, 
  70, 
  15, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10641, 
  2, 
  19, 
  50, 
  0
);
insert into northwind.order_details
values (
  10641, 
  40, 
  18.3999996, 
  60, 
  0
);
insert into northwind.order_details
values (
  10642, 
  21, 
  10, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10642, 
  61, 
  28.5, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10643, 
  28, 
  45.5999985, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10643, 
  39, 
  18, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10643, 
  46, 
  12, 
  2, 
  0.25
);
insert into northwind.order_details
values (
  10644, 
  18, 
  62.5, 
  4, 
  0.100000001
);
insert into northwind.order_details
values (
  10644, 
  43, 
  46, 
  20, 
  0
);
insert into northwind.order_details
values (
  10644, 
  46, 
  12, 
  21, 
  0.100000001
);
insert into northwind.order_details
values (
  10645, 
  18, 
  62.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10645, 
  36, 
  19, 
  15, 
  0
);
insert into northwind.order_details
values (
  10646, 
  1, 
  18, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10646, 
  10, 
  31, 
  18, 
  0.25
);
insert into northwind.order_details
values (
  10646, 
  71, 
  21.5, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10646, 
  77, 
  13, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10647, 
  19, 
  9.19999981, 
  30, 
  0
);
insert into northwind.order_details
values (
  10647, 
  39, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10648, 
  22, 
  21, 
  15, 
  0
);
insert into northwind.order_details
values (
  10648, 
  24, 
  4.5, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10649, 
  28, 
  45.5999985, 
  20, 
  0
);
insert into northwind.order_details
values (
  10649, 
  72, 
  34.7999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10650, 
  30, 
  25.8899994, 
  30, 
  0
);
insert into northwind.order_details
values (
  10650, 
  53, 
  32.7999992, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10650, 
  54, 
  7.44999981, 
  30, 
  0
);
insert into northwind.order_details
values (
  10651, 
  19, 
  9.19999981, 
  12, 
  0.25
);
insert into northwind.order_details
values (
  10651, 
  22, 
  21, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10652, 
  30, 
  25.8899994, 
  2, 
  0.25
);
insert into northwind.order_details
values (
  10652, 
  42, 
  14, 
  20, 
  0
);
insert into northwind.order_details
values (
  10653, 
  16, 
  17.4500008, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10653, 
  60, 
  34, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10654, 
  4, 
  22, 
  12, 
  0.100000001
);
insert into northwind.order_details
values (
  10654, 
  39, 
  18, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10654, 
  54, 
  7.44999981, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10655, 
  41, 
  9.64999962, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10656, 
  14, 
  23.25, 
  3, 
  0.100000001
);
insert into northwind.order_details
values (
  10656, 
  44, 
  19.4500008, 
  28, 
  0.100000001
);
insert into northwind.order_details
values (
  10656, 
  47, 
  9.5, 
  6, 
  0.100000001
);
insert into northwind.order_details
values (
  10657, 
  15, 
  15.5, 
  50, 
  0
);
insert into northwind.order_details
values (
  10657, 
  41, 
  9.64999962, 
  24, 
  0
);
insert into northwind.order_details
values (
  10657, 
  46, 
  12, 
  45, 
  0
);
insert into northwind.order_details
values (
  10657, 
  47, 
  9.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10657, 
  56, 
  38, 
  45, 
  0
);
insert into northwind.order_details
values (
  10657, 
  60, 
  34, 
  30, 
  0
);
insert into northwind.order_details
values (
  10658, 
  21, 
  10, 
  60, 
  0
);
insert into northwind.order_details
values (
  10658, 
  40, 
  18.3999996, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  10658, 
  60, 
  34, 
  55, 
  0.0500000007
);
insert into northwind.order_details
values (
  10658, 
  77, 
  13, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  10659, 
  31, 
  12.5, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10659, 
  40, 
  18.3999996, 
  24, 
  0.0500000007
);
insert into northwind.order_details
values (
  10659, 
  70, 
  15, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10660, 
  20, 
  81, 
  21, 
  0
);
insert into northwind.order_details
values (
  10661, 
  39, 
  18, 
  3, 
  0.200000003
);
insert into northwind.order_details
values (
  10661, 
  58, 
  13.25, 
  49, 
  0.200000003
);
insert into northwind.order_details
values (
  10662, 
  68, 
  12.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10663, 
  40, 
  18.3999996, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10663, 
  42, 
  14, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10663, 
  51, 
  53, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10664, 
  10, 
  31, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  10664, 
  56, 
  38, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10664, 
  65, 
  21.0499992, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10665, 
  51, 
  53, 
  20, 
  0
);
insert into northwind.order_details
values (
  10665, 
  59, 
  55, 
  1, 
  0
);
insert into northwind.order_details
values (
  10665, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10666, 
  29, 
  123.790001, 
  36, 
  0
);
insert into northwind.order_details
values (
  10666, 
  65, 
  21.0499992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10667, 
  69, 
  36, 
  45, 
  0.200000003
);
insert into northwind.order_details
values (
  10667, 
  71, 
  21.5, 
  14, 
  0.200000003
);
insert into northwind.order_details
values (
  10668, 
  31, 
  12.5, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10668, 
  55, 
  24, 
  4, 
  0.100000001
);
insert into northwind.order_details
values (
  10668, 
  64, 
  33.25, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10669, 
  36, 
  19, 
  30, 
  0
);
insert into northwind.order_details
values (
  10670, 
  23, 
  9, 
  32, 
  0
);
insert into northwind.order_details
values (
  10670, 
  46, 
  12, 
  60, 
  0
);
insert into northwind.order_details
values (
  10670, 
  67, 
  14, 
  25, 
  0
);
insert into northwind.order_details
values (
  10670, 
  73, 
  15, 
  50, 
  0
);
insert into northwind.order_details
values (
  10670, 
  75, 
  7.75, 
  25, 
  0
);
insert into northwind.order_details
values (
  10671, 
  16, 
  17.4500008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10671, 
  62, 
  49.2999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10671, 
  65, 
  21.0499992, 
  12, 
  0
);
insert into northwind.order_details
values (
  10672, 
  38, 
  263.5, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10672, 
  71, 
  21.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  10673, 
  16, 
  17.4500008, 
  3, 
  0
);
insert into northwind.order_details
values (
  10673, 
  42, 
  14, 
  6, 
  0
);
insert into northwind.order_details
values (
  10673, 
  43, 
  46, 
  6, 
  0
);
insert into northwind.order_details
values (
  10674, 
  23, 
  9, 
  5, 
  0
);
insert into northwind.order_details
values (
  10675, 
  14, 
  23.25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10675, 
  53, 
  32.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10675, 
  58, 
  13.25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10676, 
  10, 
  31, 
  2, 
  0
);
insert into northwind.order_details
values (
  10676, 
  19, 
  9.19999981, 
  7, 
  0
);
insert into northwind.order_details
values (
  10676, 
  44, 
  19.4500008, 
  21, 
  0
);
insert into northwind.order_details
values (
  10677, 
  26, 
  31.2299995, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10677, 
  33, 
  2.5, 
  8, 
  0.150000006
);
insert into northwind.order_details
values (
  10678, 
  12, 
  38, 
  100, 
  0
);
insert into northwind.order_details
values (
  10678, 
  33, 
  2.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10678, 
  41, 
  9.64999962, 
  120, 
  0
);
insert into northwind.order_details
values (
  10678, 
  54, 
  7.44999981, 
  30, 
  0
);
insert into northwind.order_details
values (
  10679, 
  59, 
  55, 
  12, 
  0
);
insert into northwind.order_details
values (
  10680, 
  16, 
  17.4500008, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10680, 
  31, 
  12.5, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10680, 
  42, 
  14, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10681, 
  19, 
  9.19999981, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10681, 
  21, 
  10, 
  12, 
  0.100000001
);
insert into northwind.order_details
values (
  10681, 
  64, 
  33.25, 
  28, 
  0
);
insert into northwind.order_details
values (
  10682, 
  33, 
  2.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10682, 
  66, 
  17, 
  4, 
  0
);
insert into northwind.order_details
values (
  10682, 
  75, 
  7.75, 
  30, 
  0
);
insert into northwind.order_details
values (
  10683, 
  52, 
  7, 
  9, 
  0
);
insert into northwind.order_details
values (
  10684, 
  40, 
  18.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10684, 
  47, 
  9.5, 
  40, 
  0
);
insert into northwind.order_details
values (
  10684, 
  60, 
  34, 
  30, 
  0
);
insert into northwind.order_details
values (
  10685, 
  10, 
  31, 
  20, 
  0
);
insert into northwind.order_details
values (
  10685, 
  41, 
  9.64999962, 
  4, 
  0
);
insert into northwind.order_details
values (
  10685, 
  47, 
  9.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10686, 
  17, 
  39, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10686, 
  26, 
  31.2299995, 
  15, 
  0
);
insert into northwind.order_details
values (
  10687, 
  9, 
  97, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10687, 
  29, 
  123.790001, 
  10, 
  0
);
insert into northwind.order_details
values (
  10687, 
  36, 
  19, 
  6, 
  0.25
);
insert into northwind.order_details
values (
  10688, 
  10, 
  31, 
  18, 
  0.100000001
);
insert into northwind.order_details
values (
  10688, 
  28, 
  45.5999985, 
  60, 
  0.100000001
);
insert into northwind.order_details
values (
  10688, 
  34, 
  14, 
  14, 
  0
);
insert into northwind.order_details
values (
  10689, 
  1, 
  18, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10690, 
  56, 
  38, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10690, 
  77, 
  13, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10691, 
  1, 
  18, 
  30, 
  0
);
insert into northwind.order_details
values (
  10691, 
  29, 
  123.790001, 
  40, 
  0
);
insert into northwind.order_details
values (
  10691, 
  43, 
  46, 
  40, 
  0
);
insert into northwind.order_details
values (
  10691, 
  44, 
  19.4500008, 
  24, 
  0
);
insert into northwind.order_details
values (
  10691, 
  62, 
  49.2999992, 
  48, 
  0
);
insert into northwind.order_details
values (
  10692, 
  63, 
  43.9000015, 
  20, 
  0
);
insert into northwind.order_details
values (
  10693, 
  9, 
  97, 
  6, 
  0
);
insert into northwind.order_details
values (
  10693, 
  54, 
  7.44999981, 
  60, 
  0.150000006
);
insert into northwind.order_details
values (
  10693, 
  69, 
  36, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10693, 
  73, 
  15, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10694, 
  7, 
  30, 
  90, 
  0
);
insert into northwind.order_details
values (
  10694, 
  59, 
  55, 
  25, 
  0
);
insert into northwind.order_details
values (
  10694, 
  70, 
  15, 
  50, 
  0
);
insert into northwind.order_details
values (
  10695, 
  8, 
  40, 
  10, 
  0
);
insert into northwind.order_details
values (
  10695, 
  12, 
  38, 
  4, 
  0
);
insert into northwind.order_details
values (
  10695, 
  24, 
  4.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10696, 
  17, 
  39, 
  20, 
  0
);
insert into northwind.order_details
values (
  10696, 
  46, 
  12, 
  18, 
  0
);
insert into northwind.order_details
values (
  10697, 
  19, 
  9.19999981, 
  7, 
  0.25
);
insert into northwind.order_details
values (
  10697, 
  35, 
  18, 
  9, 
  0.25
);
insert into northwind.order_details
values (
  10697, 
  58, 
  13.25, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10697, 
  70, 
  15, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10698, 
  11, 
  21, 
  15, 
  0
);
insert into northwind.order_details
values (
  10698, 
  17, 
  39, 
  8, 
  0.0500000007
);
insert into northwind.order_details
values (
  10698, 
  29, 
  123.790001, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10698, 
  65, 
  21.0499992, 
  65, 
  0.0500000007
);
insert into northwind.order_details
values (
  10698, 
  70, 
  15, 
  8, 
  0.0500000007
);
insert into northwind.order_details
values (
  10699, 
  47, 
  9.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  10700, 
  1, 
  18, 
  5, 
  0.200000003
);
insert into northwind.order_details
values (
  10700, 
  34, 
  14, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10700, 
  68, 
  12.5, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10700, 
  71, 
  21.5, 
  60, 
  0.200000003
);
insert into northwind.order_details
values (
  10701, 
  59, 
  55, 
  42, 
  0.150000006
);
insert into northwind.order_details
values (
  10701, 
  71, 
  21.5, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10701, 
  76, 
  18, 
  35, 
  0.150000006
);
insert into northwind.order_details
values (
  10702, 
  3, 
  10, 
  6, 
  0
);
insert into northwind.order_details
values (
  10702, 
  76, 
  18, 
  15, 
  0
);
insert into northwind.order_details
values (
  10703, 
  2, 
  19, 
  5, 
  0
);
insert into northwind.order_details
values (
  10703, 
  59, 
  55, 
  35, 
  0
);
insert into northwind.order_details
values (
  10703, 
  73, 
  15, 
  35, 
  0
);
insert into northwind.order_details
values (
  10704, 
  4, 
  22, 
  6, 
  0
);
insert into northwind.order_details
values (
  10704, 
  24, 
  4.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10704, 
  48, 
  12.75, 
  24, 
  0
);
insert into northwind.order_details
values (
  10705, 
  31, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10705, 
  32, 
  32, 
  4, 
  0
);
insert into northwind.order_details
values (
  10706, 
  16, 
  17.4500008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10706, 
  43, 
  46, 
  24, 
  0
);
insert into northwind.order_details
values (
  10706, 
  59, 
  55, 
  8, 
  0
);
insert into northwind.order_details
values (
  10707, 
  55, 
  24, 
  21, 
  0
);
insert into northwind.order_details
values (
  10707, 
  57, 
  19.5, 
  40, 
  0
);
insert into northwind.order_details
values (
  10707, 
  70, 
  15, 
  28, 
  0.150000006
);
insert into northwind.order_details
values (
  10708, 
  5, 
  21.3500004, 
  4, 
  0
);
insert into northwind.order_details
values (
  10708, 
  36, 
  19, 
  5, 
  0
);
insert into northwind.order_details
values (
  10709, 
  8, 
  40, 
  40, 
  0
);
insert into northwind.order_details
values (
  10709, 
  51, 
  53, 
  28, 
  0
);
insert into northwind.order_details
values (
  10709, 
  60, 
  34, 
  10, 
  0
);
insert into northwind.order_details
values (
  10710, 
  19, 
  9.19999981, 
  5, 
  0
);
insert into northwind.order_details
values (
  10710, 
  47, 
  9.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10711, 
  19, 
  9.19999981, 
  12, 
  0
);
insert into northwind.order_details
values (
  10711, 
  41, 
  9.64999962, 
  42, 
  0
);
insert into northwind.order_details
values (
  10711, 
  53, 
  32.7999992, 
  120, 
  0
);
insert into northwind.order_details
values (
  10712, 
  53, 
  32.7999992, 
  3, 
  0.0500000007
);
insert into northwind.order_details
values (
  10712, 
  56, 
  38, 
  30, 
  0
);
insert into northwind.order_details
values (
  10713, 
  10, 
  31, 
  18, 
  0
);
insert into northwind.order_details
values (
  10713, 
  26, 
  31.2299995, 
  30, 
  0
);
insert into northwind.order_details
values (
  10713, 
  45, 
  9.5, 
  110, 
  0
);
insert into northwind.order_details
values (
  10713, 
  46, 
  12, 
  24, 
  0
);
insert into northwind.order_details
values (
  10714, 
  2, 
  19, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10714, 
  17, 
  39, 
  27, 
  0.25
);
insert into northwind.order_details
values (
  10714, 
  47, 
  9.5, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10714, 
  56, 
  38, 
  18, 
  0.25
);
insert into northwind.order_details
values (
  10714, 
  58, 
  13.25, 
  12, 
  0.25
);
insert into northwind.order_details
values (
  10715, 
  10, 
  31, 
  21, 
  0
);
insert into northwind.order_details
values (
  10715, 
  71, 
  21.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10716, 
  21, 
  10, 
  5, 
  0
);
insert into northwind.order_details
values (
  10716, 
  51, 
  53, 
  7, 
  0
);
insert into northwind.order_details
values (
  10716, 
  61, 
  28.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10717, 
  21, 
  10, 
  32, 
  0.0500000007
);
insert into northwind.order_details
values (
  10717, 
  54, 
  7.44999981, 
  15, 
  0
);
insert into northwind.order_details
values (
  10717, 
  69, 
  36, 
  25, 
  0.0500000007
);
insert into northwind.order_details
values (
  10718, 
  12, 
  38, 
  36, 
  0
);
insert into northwind.order_details
values (
  10718, 
  16, 
  17.4500008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10718, 
  36, 
  19, 
  40, 
  0
);
insert into northwind.order_details
values (
  10718, 
  62, 
  49.2999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10719, 
  18, 
  62.5, 
  12, 
  0.25
);
insert into northwind.order_details
values (
  10719, 
  30, 
  25.8899994, 
  3, 
  0.25
);
insert into northwind.order_details
values (
  10719, 
  54, 
  7.44999981, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10720, 
  35, 
  18, 
  21, 
  0
);
insert into northwind.order_details
values (
  10720, 
  71, 
  21.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10721, 
  44, 
  19.4500008, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10722, 
  2, 
  19, 
  3, 
  0
);
insert into northwind.order_details
values (
  10722, 
  31, 
  12.5, 
  50, 
  0
);
insert into northwind.order_details
values (
  10722, 
  68, 
  12.5, 
  45, 
  0
);
insert into northwind.order_details
values (
  10722, 
  75, 
  7.75, 
  42, 
  0
);
insert into northwind.order_details
values (
  10723, 
  26, 
  31.2299995, 
  15, 
  0
);
insert into northwind.order_details
values (
  10724, 
  10, 
  31, 
  16, 
  0
);
insert into northwind.order_details
values (
  10724, 
  61, 
  28.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10725, 
  41, 
  9.64999962, 
  12, 
  0
);
insert into northwind.order_details
values (
  10725, 
  52, 
  7, 
  4, 
  0
);
insert into northwind.order_details
values (
  10725, 
  55, 
  24, 
  6, 
  0
);
insert into northwind.order_details
values (
  10726, 
  4, 
  22, 
  25, 
  0
);
insert into northwind.order_details
values (
  10726, 
  11, 
  21, 
  5, 
  0
);
insert into northwind.order_details
values (
  10727, 
  17, 
  39, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10727, 
  56, 
  38, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10727, 
  59, 
  55, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10728, 
  30, 
  25.8899994, 
  15, 
  0
);
insert into northwind.order_details
values (
  10728, 
  40, 
  18.3999996, 
  6, 
  0
);
insert into northwind.order_details
values (
  10728, 
  55, 
  24, 
  12, 
  0
);
insert into northwind.order_details
values (
  10728, 
  60, 
  34, 
  15, 
  0
);
insert into northwind.order_details
values (
  10729, 
  1, 
  18, 
  50, 
  0
);
insert into northwind.order_details
values (
  10729, 
  21, 
  10, 
  30, 
  0
);
insert into northwind.order_details
values (
  10729, 
  50, 
  16.25, 
  40, 
  0
);
insert into northwind.order_details
values (
  10730, 
  16, 
  17.4500008, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10730, 
  31, 
  12.5, 
  3, 
  0.0500000007
);
insert into northwind.order_details
values (
  10730, 
  65, 
  21.0499992, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10731, 
  21, 
  10, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10731, 
  51, 
  53, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10732, 
  76, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10733, 
  14, 
  23.25, 
  16, 
  0
);
insert into northwind.order_details
values (
  10733, 
  28, 
  45.5999985, 
  20, 
  0
);
insert into northwind.order_details
values (
  10733, 
  52, 
  7, 
  25, 
  0
);
insert into northwind.order_details
values (
  10734, 
  6, 
  25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10734, 
  30, 
  25.8899994, 
  15, 
  0
);
insert into northwind.order_details
values (
  10734, 
  76, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10735, 
  61, 
  28.5, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10735, 
  77, 
  13, 
  2, 
  0.100000001
);
insert into northwind.order_details
values (
  10736, 
  65, 
  21.0499992, 
  40, 
  0
);
insert into northwind.order_details
values (
  10736, 
  75, 
  7.75, 
  20, 
  0
);
insert into northwind.order_details
values (
  10737, 
  13, 
  6, 
  4, 
  0
);
insert into northwind.order_details
values (
  10737, 
  41, 
  9.64999962, 
  12, 
  0
);
insert into northwind.order_details
values (
  10738, 
  16, 
  17.4500008, 
  3, 
  0
);
insert into northwind.order_details
values (
  10739, 
  36, 
  19, 
  6, 
  0
);
insert into northwind.order_details
values (
  10739, 
  52, 
  7, 
  18, 
  0
);
insert into northwind.order_details
values (
  10740, 
  28, 
  45.5999985, 
  5, 
  0.200000003
);
insert into northwind.order_details
values (
  10740, 
  35, 
  18, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10740, 
  45, 
  9.5, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10740, 
  56, 
  38, 
  14, 
  0.200000003
);
insert into northwind.order_details
values (
  10741, 
  2, 
  19, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10742, 
  3, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10742, 
  60, 
  34, 
  50, 
  0
);
insert into northwind.order_details
values (
  10742, 
  72, 
  34.7999992, 
  35, 
  0
);
insert into northwind.order_details
values (
  10743, 
  46, 
  12, 
  28, 
  0.0500000007
);
insert into northwind.order_details
values (
  10744, 
  40, 
  18.3999996, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10745, 
  18, 
  62.5, 
  24, 
  0
);
insert into northwind.order_details
values (
  10745, 
  44, 
  19.4500008, 
  16, 
  0
);
insert into northwind.order_details
values (
  10745, 
  59, 
  55, 
  45, 
  0
);
insert into northwind.order_details
values (
  10745, 
  72, 
  34.7999992, 
  7, 
  0
);
insert into northwind.order_details
values (
  10746, 
  13, 
  6, 
  6, 
  0
);
insert into northwind.order_details
values (
  10746, 
  42, 
  14, 
  28, 
  0
);
insert into northwind.order_details
values (
  10746, 
  62, 
  49.2999992, 
  9, 
  0
);
insert into northwind.order_details
values (
  10746, 
  69, 
  36, 
  40, 
  0
);
insert into northwind.order_details
values (
  10747, 
  31, 
  12.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10747, 
  41, 
  9.64999962, 
  35, 
  0
);
insert into northwind.order_details
values (
  10747, 
  63, 
  43.9000015, 
  9, 
  0
);
insert into northwind.order_details
values (
  10747, 
  69, 
  36, 
  30, 
  0
);
insert into northwind.order_details
values (
  10748, 
  23, 
  9, 
  44, 
  0
);
insert into northwind.order_details
values (
  10748, 
  40, 
  18.3999996, 
  40, 
  0
);
insert into northwind.order_details
values (
  10748, 
  56, 
  38, 
  28, 
  0
);
insert into northwind.order_details
values (
  10749, 
  56, 
  38, 
  15, 
  0
);
insert into northwind.order_details
values (
  10749, 
  59, 
  55, 
  6, 
  0
);
insert into northwind.order_details
values (
  10749, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10750, 
  14, 
  23.25, 
  5, 
  0.150000006
);
insert into northwind.order_details
values (
  10750, 
  45, 
  9.5, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10750, 
  59, 
  55, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10751, 
  26, 
  31.2299995, 
  12, 
  0.100000001
);
insert into northwind.order_details
values (
  10751, 
  30, 
  25.8899994, 
  30, 
  0
);
insert into northwind.order_details
values (
  10751, 
  50, 
  16.25, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10751, 
  73, 
  15, 
  15, 
  0
);
insert into northwind.order_details
values (
  10752, 
  1, 
  18, 
  8, 
  0
);
insert into northwind.order_details
values (
  10752, 
  69, 
  36, 
  3, 
  0
);
insert into northwind.order_details
values (
  10753, 
  45, 
  9.5, 
  4, 
  0
);
insert into northwind.order_details
values (
  10753, 
  74, 
  10, 
  5, 
  0
);
insert into northwind.order_details
values (
  10754, 
  40, 
  18.3999996, 
  3, 
  0
);
insert into northwind.order_details
values (
  10755, 
  47, 
  9.5, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10755, 
  56, 
  38, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10755, 
  57, 
  19.5, 
  14, 
  0.25
);
insert into northwind.order_details
values (
  10755, 
  69, 
  36, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  10756, 
  18, 
  62.5, 
  21, 
  0.200000003
);
insert into northwind.order_details
values (
  10756, 
  36, 
  19, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10756, 
  68, 
  12.5, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10756, 
  69, 
  36, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10757, 
  34, 
  14, 
  30, 
  0
);
insert into northwind.order_details
values (
  10757, 
  59, 
  55, 
  7, 
  0
);
insert into northwind.order_details
values (
  10757, 
  62, 
  49.2999992, 
  30, 
  0
);
insert into northwind.order_details
values (
  10757, 
  64, 
  33.25, 
  24, 
  0
);
insert into northwind.order_details
values (
  10758, 
  26, 
  31.2299995, 
  20, 
  0
);
insert into northwind.order_details
values (
  10758, 
  52, 
  7, 
  60, 
  0
);
insert into northwind.order_details
values (
  10758, 
  70, 
  15, 
  40, 
  0
);
insert into northwind.order_details
values (
  10759, 
  32, 
  32, 
  10, 
  0
);
insert into northwind.order_details
values (
  10760, 
  25, 
  14, 
  12, 
  0.25
);
insert into northwind.order_details
values (
  10760, 
  27, 
  43.9000015, 
  40, 
  0
);
insert into northwind.order_details
values (
  10760, 
  43, 
  46, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10761, 
  25, 
  14, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10761, 
  75, 
  7.75, 
  18, 
  0
);
insert into northwind.order_details
values (
  10762, 
  39, 
  18, 
  16, 
  0
);
insert into northwind.order_details
values (
  10762, 
  47, 
  9.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10762, 
  51, 
  53, 
  28, 
  0
);
insert into northwind.order_details
values (
  10762, 
  56, 
  38, 
  60, 
  0
);
insert into northwind.order_details
values (
  10763, 
  21, 
  10, 
  40, 
  0
);
insert into northwind.order_details
values (
  10763, 
  22, 
  21, 
  6, 
  0
);
insert into northwind.order_details
values (
  10763, 
  24, 
  4.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10764, 
  3, 
  10, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10764, 
  39, 
  18, 
  130, 
  0.100000001
);
insert into northwind.order_details
values (
  10765, 
  65, 
  21.0499992, 
  80, 
  0.100000001
);
insert into northwind.order_details
values (
  10766, 
  2, 
  19, 
  40, 
  0
);
insert into northwind.order_details
values (
  10766, 
  7, 
  30, 
  35, 
  0
);
insert into northwind.order_details
values (
  10766, 
  68, 
  12.5, 
  40, 
  0
);
insert into northwind.order_details
values (
  10767, 
  42, 
  14, 
  2, 
  0
);
insert into northwind.order_details
values (
  10768, 
  22, 
  21, 
  4, 
  0
);
insert into northwind.order_details
values (
  10768, 
  31, 
  12.5, 
  50, 
  0
);
insert into northwind.order_details
values (
  10768, 
  60, 
  34, 
  15, 
  0
);
insert into northwind.order_details
values (
  10768, 
  71, 
  21.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  10769, 
  41, 
  9.64999962, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10769, 
  52, 
  7, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10769, 
  61, 
  28.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10769, 
  62, 
  49.2999992, 
  15, 
  0
);
insert into northwind.order_details
values (
  10770, 
  11, 
  21, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10771, 
  71, 
  21.5, 
  16, 
  0
);
insert into northwind.order_details
values (
  10772, 
  29, 
  123.790001, 
  18, 
  0
);
insert into northwind.order_details
values (
  10772, 
  59, 
  55, 
  25, 
  0
);
insert into northwind.order_details
values (
  10773, 
  17, 
  39, 
  33, 
  0
);
insert into northwind.order_details
values (
  10773, 
  31, 
  12.5, 
  70, 
  0.200000003
);
insert into northwind.order_details
values (
  10773, 
  75, 
  7.75, 
  7, 
  0.200000003
);
insert into northwind.order_details
values (
  10774, 
  31, 
  12.5, 
  2, 
  0.25
);
insert into northwind.order_details
values (
  10774, 
  66, 
  17, 
  50, 
  0
);
insert into northwind.order_details
values (
  10775, 
  10, 
  31, 
  6, 
  0
);
insert into northwind.order_details
values (
  10775, 
  67, 
  14, 
  3, 
  0
);
insert into northwind.order_details
values (
  10776, 
  31, 
  12.5, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10776, 
  42, 
  14, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10776, 
  45, 
  9.5, 
  27, 
  0.0500000007
);
insert into northwind.order_details
values (
  10776, 
  51, 
  53, 
  120, 
  0.0500000007
);
insert into northwind.order_details
values (
  10777, 
  42, 
  14, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10778, 
  41, 
  9.64999962, 
  10, 
  0
);
insert into northwind.order_details
values (
  10779, 
  16, 
  17.4500008, 
  20, 
  0
);
insert into northwind.order_details
values (
  10779, 
  62, 
  49.2999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10780, 
  70, 
  15, 
  35, 
  0
);
insert into northwind.order_details
values (
  10780, 
  77, 
  13, 
  15, 
  0
);
insert into northwind.order_details
values (
  10781, 
  54, 
  7.44999981, 
  3, 
  0.200000003
);
insert into northwind.order_details
values (
  10781, 
  56, 
  38, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10781, 
  74, 
  10, 
  35, 
  0
);
insert into northwind.order_details
values (
  10782, 
  31, 
  12.5, 
  1, 
  0
);
insert into northwind.order_details
values (
  10783, 
  31, 
  12.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10783, 
  38, 
  263.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10784, 
  36, 
  19, 
  30, 
  0
);
insert into northwind.order_details
values (
  10784, 
  39, 
  18, 
  2, 
  0.150000006
);
insert into northwind.order_details
values (
  10784, 
  72, 
  34.7999992, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10785, 
  10, 
  31, 
  10, 
  0
);
insert into northwind.order_details
values (
  10785, 
  75, 
  7.75, 
  10, 
  0
);
insert into northwind.order_details
values (
  10786, 
  8, 
  40, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10786, 
  30, 
  25.8899994, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10786, 
  75, 
  7.75, 
  42, 
  0.200000003
);
insert into northwind.order_details
values (
  10787, 
  2, 
  19, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10787, 
  29, 
  123.790001, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10788, 
  19, 
  9.19999981, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10788, 
  75, 
  7.75, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10789, 
  18, 
  62.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10789, 
  35, 
  18, 
  15, 
  0
);
insert into northwind.order_details
values (
  10789, 
  63, 
  43.9000015, 
  30, 
  0
);
insert into northwind.order_details
values (
  10789, 
  68, 
  12.5, 
  18, 
  0
);
insert into northwind.order_details
values (
  10790, 
  7, 
  30, 
  3, 
  0.150000006
);
insert into northwind.order_details
values (
  10790, 
  56, 
  38, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10791, 
  29, 
  123.790001, 
  14, 
  0.0500000007
);
insert into northwind.order_details
values (
  10791, 
  41, 
  9.64999962, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10792, 
  2, 
  19, 
  10, 
  0
);
insert into northwind.order_details
values (
  10792, 
  54, 
  7.44999981, 
  3, 
  0
);
insert into northwind.order_details
values (
  10792, 
  68, 
  12.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10793, 
  41, 
  9.64999962, 
  14, 
  0
);
insert into northwind.order_details
values (
  10793, 
  52, 
  7, 
  8, 
  0
);
insert into northwind.order_details
values (
  10794, 
  14, 
  23.25, 
  15, 
  0.200000003
);
insert into northwind.order_details
values (
  10794, 
  54, 
  7.44999981, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10795, 
  16, 
  17.4500008, 
  65, 
  0
);
insert into northwind.order_details
values (
  10795, 
  17, 
  39, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10796, 
  26, 
  31.2299995, 
  21, 
  0.200000003
);
insert into northwind.order_details
values (
  10796, 
  44, 
  19.4500008, 
  10, 
  0
);
insert into northwind.order_details
values (
  10796, 
  64, 
  33.25, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  10796, 
  69, 
  36, 
  24, 
  0.200000003
);
insert into northwind.order_details
values (
  10797, 
  11, 
  21, 
  20, 
  0
);
insert into northwind.order_details
values (
  10798, 
  62, 
  49.2999992, 
  2, 
  0
);
insert into northwind.order_details
values (
  10798, 
  72, 
  34.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10799, 
  13, 
  6, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10799, 
  24, 
  4.5, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10799, 
  59, 
  55, 
  25, 
  0
);
insert into northwind.order_details
values (
  10800, 
  11, 
  21, 
  50, 
  0.100000001
);
insert into northwind.order_details
values (
  10800, 
  51, 
  53, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  10800, 
  54, 
  7.44999981, 
  7, 
  0.100000001
);
insert into northwind.order_details
values (
  10801, 
  17, 
  39, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10801, 
  29, 
  123.790001, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10802, 
  30, 
  25.8899994, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  10802, 
  51, 
  53, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10802, 
  55, 
  24, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10802, 
  62, 
  49.2999992, 
  5, 
  0.25
);
insert into northwind.order_details
values (
  10803, 
  19, 
  9.19999981, 
  24, 
  0.0500000007
);
insert into northwind.order_details
values (
  10803, 
  25, 
  14, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10803, 
  59, 
  55, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10804, 
  10, 
  31, 
  36, 
  0
);
insert into northwind.order_details
values (
  10804, 
  28, 
  45.5999985, 
  24, 
  0
);
insert into northwind.order_details
values (
  10804, 
  49, 
  20, 
  4, 
  0.150000006
);
insert into northwind.order_details
values (
  10805, 
  34, 
  14, 
  10, 
  0
);
insert into northwind.order_details
values (
  10805, 
  38, 
  263.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10806, 
  2, 
  19, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10806, 
  65, 
  21.0499992, 
  2, 
  0
);
insert into northwind.order_details
values (
  10806, 
  74, 
  10, 
  15, 
  0.25
);
insert into northwind.order_details
values (
  10807, 
  40, 
  18.3999996, 
  1, 
  0
);
insert into northwind.order_details
values (
  10808, 
  56, 
  38, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10808, 
  76, 
  18, 
  50, 
  0.150000006
);
insert into northwind.order_details
values (
  10809, 
  52, 
  7, 
  20, 
  0
);
insert into northwind.order_details
values (
  10810, 
  13, 
  6, 
  7, 
  0
);
insert into northwind.order_details
values (
  10810, 
  25, 
  14, 
  5, 
  0
);
insert into northwind.order_details
values (
  10810, 
  70, 
  15, 
  5, 
  0
);
insert into northwind.order_details
values (
  10811, 
  19, 
  9.19999981, 
  15, 
  0
);
insert into northwind.order_details
values (
  10811, 
  23, 
  9, 
  18, 
  0
);
insert into northwind.order_details
values (
  10811, 
  40, 
  18.3999996, 
  30, 
  0
);
insert into northwind.order_details
values (
  10812, 
  31, 
  12.5, 
  16, 
  0.100000001
);
insert into northwind.order_details
values (
  10812, 
  72, 
  34.7999992, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10812, 
  77, 
  13, 
  20, 
  0
);
insert into northwind.order_details
values (
  10813, 
  2, 
  19, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10813, 
  46, 
  12, 
  35, 
  0
);
insert into northwind.order_details
values (
  10814, 
  41, 
  9.64999962, 
  20, 
  0
);
insert into northwind.order_details
values (
  10814, 
  43, 
  46, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10814, 
  48, 
  12.75, 
  8, 
  0.150000006
);
insert into northwind.order_details
values (
  10814, 
  61, 
  28.5, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10815, 
  33, 
  2.5, 
  16, 
  0
);
insert into northwind.order_details
values (
  10816, 
  38, 
  263.5, 
  30, 
  0.0500000007
);
insert into northwind.order_details
values (
  10816, 
  62, 
  49.2999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10817, 
  26, 
  31.2299995, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10817, 
  38, 
  263.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10817, 
  40, 
  18.3999996, 
  60, 
  0.150000006
);
insert into northwind.order_details
values (
  10817, 
  62, 
  49.2999992, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10818, 
  32, 
  32, 
  20, 
  0
);
insert into northwind.order_details
values (
  10818, 
  41, 
  9.64999962, 
  20, 
  0
);
insert into northwind.order_details
values (
  10819, 
  43, 
  46, 
  7, 
  0
);
insert into northwind.order_details
values (
  10819, 
  75, 
  7.75, 
  20, 
  0
);
insert into northwind.order_details
values (
  10820, 
  56, 
  38, 
  30, 
  0
);
insert into northwind.order_details
values (
  10821, 
  35, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10821, 
  51, 
  53, 
  6, 
  0
);
insert into northwind.order_details
values (
  10822, 
  62, 
  49.2999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10822, 
  70, 
  15, 
  6, 
  0
);
insert into northwind.order_details
values (
  10823, 
  11, 
  21, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10823, 
  57, 
  19.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10823, 
  59, 
  55, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10823, 
  77, 
  13, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10824, 
  41, 
  9.64999962, 
  12, 
  0
);
insert into northwind.order_details
values (
  10824, 
  70, 
  15, 
  9, 
  0
);
insert into northwind.order_details
values (
  10825, 
  26, 
  31.2299995, 
  12, 
  0
);
insert into northwind.order_details
values (
  10825, 
  53, 
  32.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10826, 
  31, 
  12.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10826, 
  57, 
  19.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10827, 
  10, 
  31, 
  15, 
  0
);
insert into northwind.order_details
values (
  10827, 
  39, 
  18, 
  21, 
  0
);
insert into northwind.order_details
values (
  10828, 
  20, 
  81, 
  5, 
  0
);
insert into northwind.order_details
values (
  10828, 
  38, 
  263.5, 
  2, 
  0
);
insert into northwind.order_details
values (
  10829, 
  2, 
  19, 
  10, 
  0
);
insert into northwind.order_details
values (
  10829, 
  8, 
  40, 
  20, 
  0
);
insert into northwind.order_details
values (
  10829, 
  13, 
  6, 
  10, 
  0
);
insert into northwind.order_details
values (
  10829, 
  60, 
  34, 
  21, 
  0
);
insert into northwind.order_details
values (
  10830, 
  6, 
  25, 
  6, 
  0
);
insert into northwind.order_details
values (
  10830, 
  39, 
  18, 
  28, 
  0
);
insert into northwind.order_details
values (
  10830, 
  60, 
  34, 
  30, 
  0
);
insert into northwind.order_details
values (
  10830, 
  68, 
  12.5, 
  24, 
  0
);
insert into northwind.order_details
values (
  10831, 
  19, 
  9.19999981, 
  2, 
  0
);
insert into northwind.order_details
values (
  10831, 
  35, 
  18, 
  8, 
  0
);
insert into northwind.order_details
values (
  10831, 
  38, 
  263.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10831, 
  43, 
  46, 
  9, 
  0
);
insert into northwind.order_details
values (
  10832, 
  13, 
  6, 
  3, 
  0.200000003
);
insert into northwind.order_details
values (
  10832, 
  25, 
  14, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10832, 
  44, 
  19.4500008, 
  16, 
  0.200000003
);
insert into northwind.order_details
values (
  10832, 
  64, 
  33.25, 
  3, 
  0
);
insert into northwind.order_details
values (
  10833, 
  7, 
  30, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10833, 
  31, 
  12.5, 
  9, 
  0.100000001
);
insert into northwind.order_details
values (
  10833, 
  53, 
  32.7999992, 
  9, 
  0.100000001
);
insert into northwind.order_details
values (
  10834, 
  29, 
  123.790001, 
  8, 
  0.0500000007
);
insert into northwind.order_details
values (
  10834, 
  30, 
  25.8899994, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10835, 
  59, 
  55, 
  15, 
  0
);
insert into northwind.order_details
values (
  10835, 
  77, 
  13, 
  2, 
  0.200000003
);
insert into northwind.order_details
values (
  10836, 
  22, 
  21, 
  52, 
  0
);
insert into northwind.order_details
values (
  10836, 
  35, 
  18, 
  6, 
  0
);
insert into northwind.order_details
values (
  10836, 
  57, 
  19.5, 
  24, 
  0
);
insert into northwind.order_details
values (
  10836, 
  60, 
  34, 
  60, 
  0
);
insert into northwind.order_details
values (
  10836, 
  64, 
  33.25, 
  30, 
  0
);
insert into northwind.order_details
values (
  10837, 
  13, 
  6, 
  6, 
  0
);
insert into northwind.order_details
values (
  10837, 
  40, 
  18.3999996, 
  25, 
  0
);
insert into northwind.order_details
values (
  10837, 
  47, 
  9.5, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10837, 
  76, 
  18, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10838, 
  1, 
  18, 
  4, 
  0.25
);
insert into northwind.order_details
values (
  10838, 
  18, 
  62.5, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  10838, 
  36, 
  19, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10839, 
  58, 
  13.25, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10839, 
  72, 
  34.7999992, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  10840, 
  25, 
  14, 
  6, 
  0.200000003
);
insert into northwind.order_details
values (
  10840, 
  39, 
  18, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10841, 
  10, 
  31, 
  16, 
  0
);
insert into northwind.order_details
values (
  10841, 
  56, 
  38, 
  30, 
  0
);
insert into northwind.order_details
values (
  10841, 
  59, 
  55, 
  50, 
  0
);
insert into northwind.order_details
values (
  10841, 
  77, 
  13, 
  15, 
  0
);
insert into northwind.order_details
values (
  10842, 
  11, 
  21, 
  15, 
  0
);
insert into northwind.order_details
values (
  10842, 
  43, 
  46, 
  5, 
  0
);
insert into northwind.order_details
values (
  10842, 
  68, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10842, 
  70, 
  15, 
  12, 
  0
);
insert into northwind.order_details
values (
  10843, 
  51, 
  53, 
  4, 
  0.25
);
insert into northwind.order_details
values (
  10844, 
  22, 
  21, 
  35, 
  0
);
insert into northwind.order_details
values (
  10845, 
  23, 
  9, 
  70, 
  0.100000001
);
insert into northwind.order_details
values (
  10845, 
  35, 
  18, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  10845, 
  42, 
  14, 
  42, 
  0.100000001
);
insert into northwind.order_details
values (
  10845, 
  58, 
  13.25, 
  60, 
  0.100000001
);
insert into northwind.order_details
values (
  10845, 
  64, 
  33.25, 
  48, 
  0
);
insert into northwind.order_details
values (
  10846, 
  4, 
  22, 
  21, 
  0
);
insert into northwind.order_details
values (
  10846, 
  70, 
  15, 
  30, 
  0
);
insert into northwind.order_details
values (
  10846, 
  74, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10847, 
  1, 
  18, 
  80, 
  0.200000003
);
insert into northwind.order_details
values (
  10847, 
  19, 
  9.19999981, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10847, 
  37, 
  26, 
  60, 
  0.200000003
);
insert into northwind.order_details
values (
  10847, 
  45, 
  9.5, 
  36, 
  0.200000003
);
insert into northwind.order_details
values (
  10847, 
  60, 
  34, 
  45, 
  0.200000003
);
insert into northwind.order_details
values (
  10847, 
  71, 
  21.5, 
  55, 
  0.200000003
);
insert into northwind.order_details
values (
  10848, 
  5, 
  21.3500004, 
  30, 
  0
);
insert into northwind.order_details
values (
  10848, 
  9, 
  97, 
  3, 
  0
);
insert into northwind.order_details
values (
  10849, 
  3, 
  10, 
  49, 
  0
);
insert into northwind.order_details
values (
  10849, 
  26, 
  31.2299995, 
  18, 
  0.150000006
);
insert into northwind.order_details
values (
  10850, 
  25, 
  14, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10850, 
  33, 
  2.5, 
  4, 
  0.150000006
);
insert into northwind.order_details
values (
  10850, 
  70, 
  15, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10851, 
  2, 
  19, 
  5, 
  0.0500000007
);
insert into northwind.order_details
values (
  10851, 
  25, 
  14, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10851, 
  57, 
  19.5, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10851, 
  59, 
  55, 
  42, 
  0.0500000007
);
insert into northwind.order_details
values (
  10852, 
  2, 
  19, 
  15, 
  0
);
insert into northwind.order_details
values (
  10852, 
  17, 
  39, 
  6, 
  0
);
insert into northwind.order_details
values (
  10852, 
  62, 
  49.2999992, 
  50, 
  0
);
insert into northwind.order_details
values (
  10853, 
  18, 
  62.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10854, 
  10, 
  31, 
  100, 
  0.150000006
);
insert into northwind.order_details
values (
  10854, 
  13, 
  6, 
  65, 
  0.150000006
);
insert into northwind.order_details
values (
  10855, 
  16, 
  17.4500008, 
  50, 
  0
);
insert into northwind.order_details
values (
  10855, 
  31, 
  12.5, 
  14, 
  0
);
insert into northwind.order_details
values (
  10855, 
  56, 
  38, 
  24, 
  0
);
insert into northwind.order_details
values (
  10855, 
  65, 
  21.0499992, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  10856, 
  2, 
  19, 
  20, 
  0
);
insert into northwind.order_details
values (
  10856, 
  42, 
  14, 
  20, 
  0
);
insert into northwind.order_details
values (
  10857, 
  3, 
  10, 
  30, 
  0
);
insert into northwind.order_details
values (
  10857, 
  26, 
  31.2299995, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10857, 
  29, 
  123.790001, 
  10, 
  0.25
);
insert into northwind.order_details
values (
  10858, 
  7, 
  30, 
  5, 
  0
);
insert into northwind.order_details
values (
  10858, 
  27, 
  43.9000015, 
  10, 
  0
);
insert into northwind.order_details
values (
  10858, 
  70, 
  15, 
  4, 
  0
);
insert into northwind.order_details
values (
  10859, 
  24, 
  4.5, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10859, 
  54, 
  7.44999981, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10859, 
  64, 
  33.25, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10860, 
  51, 
  53, 
  3, 
  0
);
insert into northwind.order_details
values (
  10860, 
  76, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10861, 
  17, 
  39, 
  42, 
  0
);
insert into northwind.order_details
values (
  10861, 
  18, 
  62.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10861, 
  21, 
  10, 
  40, 
  0
);
insert into northwind.order_details
values (
  10861, 
  33, 
  2.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10861, 
  62, 
  49.2999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10862, 
  11, 
  21, 
  25, 
  0
);
insert into northwind.order_details
values (
  10862, 
  52, 
  7, 
  8, 
  0
);
insert into northwind.order_details
values (
  10863, 
  1, 
  18, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10863, 
  58, 
  13.25, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10864, 
  35, 
  18, 
  4, 
  0
);
insert into northwind.order_details
values (
  10864, 
  67, 
  14, 
  15, 
  0
);
insert into northwind.order_details
values (
  10865, 
  38, 
  263.5, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  10865, 
  39, 
  18, 
  80, 
  0.0500000007
);
insert into northwind.order_details
values (
  10866, 
  2, 
  19, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  10866, 
  24, 
  4.5, 
  6, 
  0.25
);
insert into northwind.order_details
values (
  10866, 
  30, 
  25.8899994, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10867, 
  53, 
  32.7999992, 
  3, 
  0
);
insert into northwind.order_details
values (
  10868, 
  26, 
  31.2299995, 
  20, 
  0
);
insert into northwind.order_details
values (
  10868, 
  35, 
  18, 
  30, 
  0
);
insert into northwind.order_details
values (
  10868, 
  49, 
  20, 
  42, 
  0.100000001
);
insert into northwind.order_details
values (
  10869, 
  1, 
  18, 
  40, 
  0
);
insert into northwind.order_details
values (
  10869, 
  11, 
  21, 
  10, 
  0
);
insert into northwind.order_details
values (
  10869, 
  23, 
  9, 
  50, 
  0
);
insert into northwind.order_details
values (
  10869, 
  68, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10870, 
  35, 
  18, 
  3, 
  0
);
insert into northwind.order_details
values (
  10870, 
  51, 
  53, 
  2, 
  0
);
insert into northwind.order_details
values (
  10871, 
  6, 
  25, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10871, 
  16, 
  17.4500008, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10871, 
  17, 
  39, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10872, 
  55, 
  24, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  10872, 
  62, 
  49.2999992, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10872, 
  64, 
  33.25, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10872, 
  65, 
  21.0499992, 
  21, 
  0.0500000007
);
insert into northwind.order_details
values (
  10873, 
  21, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10873, 
  28, 
  45.5999985, 
  3, 
  0
);
insert into northwind.order_details
values (
  10874, 
  10, 
  31, 
  10, 
  0
);
insert into northwind.order_details
values (
  10875, 
  19, 
  9.19999981, 
  25, 
  0
);
insert into northwind.order_details
values (
  10875, 
  47, 
  9.5, 
  21, 
  0.100000001
);
insert into northwind.order_details
values (
  10875, 
  49, 
  20, 
  15, 
  0
);
insert into northwind.order_details
values (
  10876, 
  46, 
  12, 
  21, 
  0
);
insert into northwind.order_details
values (
  10876, 
  64, 
  33.25, 
  20, 
  0
);
insert into northwind.order_details
values (
  10877, 
  16, 
  17.4500008, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10877, 
  18, 
  62.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  10878, 
  20, 
  81, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10879, 
  40, 
  18.3999996, 
  12, 
  0
);
insert into northwind.order_details
values (
  10879, 
  65, 
  21.0499992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10879, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10880, 
  23, 
  9, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10880, 
  61, 
  28.5, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10880, 
  70, 
  15, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10881, 
  73, 
  15, 
  10, 
  0
);
insert into northwind.order_details
values (
  10882, 
  42, 
  14, 
  25, 
  0
);
insert into northwind.order_details
values (
  10882, 
  49, 
  20, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10882, 
  54, 
  7.44999981, 
  32, 
  0.150000006
);
insert into northwind.order_details
values (
  10883, 
  24, 
  4.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  10884, 
  21, 
  10, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10884, 
  56, 
  38, 
  21, 
  0.0500000007
);
insert into northwind.order_details
values (
  10884, 
  65, 
  21.0499992, 
  12, 
  0.0500000007
);
insert into northwind.order_details
values (
  10885, 
  2, 
  19, 
  20, 
  0
);
insert into northwind.order_details
values (
  10885, 
  24, 
  4.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  10885, 
  70, 
  15, 
  30, 
  0
);
insert into northwind.order_details
values (
  10885, 
  77, 
  13, 
  25, 
  0
);
insert into northwind.order_details
values (
  10886, 
  10, 
  31, 
  70, 
  0
);
insert into northwind.order_details
values (
  10886, 
  31, 
  12.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10886, 
  77, 
  13, 
  40, 
  0
);
insert into northwind.order_details
values (
  10887, 
  25, 
  14, 
  5, 
  0
);
insert into northwind.order_details
values (
  10888, 
  2, 
  19, 
  20, 
  0
);
insert into northwind.order_details
values (
  10888, 
  68, 
  12.5, 
  18, 
  0
);
insert into northwind.order_details
values (
  10889, 
  11, 
  21, 
  40, 
  0
);
insert into northwind.order_details
values (
  10889, 
  38, 
  263.5, 
  40, 
  0
);
insert into northwind.order_details
values (
  10890, 
  17, 
  39, 
  15, 
  0
);
insert into northwind.order_details
values (
  10890, 
  34, 
  14, 
  10, 
  0
);
insert into northwind.order_details
values (
  10890, 
  41, 
  9.64999962, 
  14, 
  0
);
insert into northwind.order_details
values (
  10891, 
  30, 
  25.8899994, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10892, 
  59, 
  55, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  10893, 
  8, 
  40, 
  30, 
  0
);
insert into northwind.order_details
values (
  10893, 
  24, 
  4.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10893, 
  29, 
  123.790001, 
  24, 
  0
);
insert into northwind.order_details
values (
  10893, 
  30, 
  25.8899994, 
  35, 
  0
);
insert into northwind.order_details
values (
  10893, 
  36, 
  19, 
  20, 
  0
);
insert into northwind.order_details
values (
  10894, 
  13, 
  6, 
  28, 
  0.0500000007
);
insert into northwind.order_details
values (
  10894, 
  69, 
  36, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10894, 
  75, 
  7.75, 
  120, 
  0.0500000007
);
insert into northwind.order_details
values (
  10895, 
  24, 
  4.5, 
  110, 
  0
);
insert into northwind.order_details
values (
  10895, 
  39, 
  18, 
  45, 
  0
);
insert into northwind.order_details
values (
  10895, 
  40, 
  18.3999996, 
  91, 
  0
);
insert into northwind.order_details
values (
  10895, 
  60, 
  34, 
  100, 
  0
);
insert into northwind.order_details
values (
  10896, 
  45, 
  9.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10896, 
  56, 
  38, 
  16, 
  0
);
insert into northwind.order_details
values (
  10897, 
  29, 
  123.790001, 
  80, 
  0
);
insert into northwind.order_details
values (
  10897, 
  30, 
  25.8899994, 
  36, 
  0
);
insert into northwind.order_details
values (
  10898, 
  13, 
  6, 
  5, 
  0
);
insert into northwind.order_details
values (
  10899, 
  39, 
  18, 
  8, 
  0.150000006
);
insert into northwind.order_details
values (
  10900, 
  70, 
  15, 
  3, 
  0.25
);
insert into northwind.order_details
values (
  10901, 
  41, 
  9.64999962, 
  30, 
  0
);
insert into northwind.order_details
values (
  10901, 
  71, 
  21.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10902, 
  55, 
  24, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  10902, 
  62, 
  49.2999992, 
  6, 
  0.150000006
);
insert into northwind.order_details
values (
  10903, 
  13, 
  6, 
  40, 
  0
);
insert into northwind.order_details
values (
  10903, 
  65, 
  21.0499992, 
  21, 
  0
);
insert into northwind.order_details
values (
  10903, 
  68, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10904, 
  58, 
  13.25, 
  15, 
  0
);
insert into northwind.order_details
values (
  10904, 
  62, 
  49.2999992, 
  35, 
  0
);
insert into northwind.order_details
values (
  10905, 
  1, 
  18, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10906, 
  61, 
  28.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10907, 
  75, 
  7.75, 
  14, 
  0
);
insert into northwind.order_details
values (
  10908, 
  7, 
  30, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10908, 
  52, 
  7, 
  14, 
  0.0500000007
);
insert into northwind.order_details
values (
  10909, 
  7, 
  30, 
  12, 
  0
);
insert into northwind.order_details
values (
  10909, 
  16, 
  17.4500008, 
  15, 
  0
);
insert into northwind.order_details
values (
  10909, 
  41, 
  9.64999962, 
  5, 
  0
);
insert into northwind.order_details
values (
  10910, 
  19, 
  9.19999981, 
  12, 
  0
);
insert into northwind.order_details
values (
  10910, 
  49, 
  20, 
  10, 
  0
);
insert into northwind.order_details
values (
  10910, 
  61, 
  28.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10911, 
  1, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10911, 
  17, 
  39, 
  12, 
  0
);
insert into northwind.order_details
values (
  10911, 
  67, 
  14, 
  15, 
  0
);
insert into northwind.order_details
values (
  10912, 
  11, 
  21, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10912, 
  29, 
  123.790001, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10913, 
  4, 
  22, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10913, 
  33, 
  2.5, 
  40, 
  0.25
);
insert into northwind.order_details
values (
  10913, 
  58, 
  13.25, 
  15, 
  0
);
insert into northwind.order_details
values (
  10914, 
  71, 
  21.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  10915, 
  17, 
  39, 
  10, 
  0
);
insert into northwind.order_details
values (
  10915, 
  33, 
  2.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10915, 
  54, 
  7.44999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  10916, 
  16, 
  17.4500008, 
  6, 
  0
);
insert into northwind.order_details
values (
  10916, 
  32, 
  32, 
  6, 
  0
);
insert into northwind.order_details
values (
  10916, 
  57, 
  19.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10917, 
  30, 
  25.8899994, 
  1, 
  0
);
insert into northwind.order_details
values (
  10917, 
  60, 
  34, 
  10, 
  0
);
insert into northwind.order_details
values (
  10918, 
  1, 
  18, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  10918, 
  60, 
  34, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  10919, 
  16, 
  17.4500008, 
  24, 
  0
);
insert into northwind.order_details
values (
  10919, 
  25, 
  14, 
  24, 
  0
);
insert into northwind.order_details
values (
  10919, 
  40, 
  18.3999996, 
  20, 
  0
);
insert into northwind.order_details
values (
  10920, 
  50, 
  16.25, 
  24, 
  0
);
insert into northwind.order_details
values (
  10921, 
  35, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10921, 
  63, 
  43.9000015, 
  40, 
  0
);
insert into northwind.order_details
values (
  10922, 
  17, 
  39, 
  15, 
  0
);
insert into northwind.order_details
values (
  10922, 
  24, 
  4.5, 
  35, 
  0
);
insert into northwind.order_details
values (
  10923, 
  42, 
  14, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10923, 
  43, 
  46, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  10923, 
  67, 
  14, 
  24, 
  0.200000003
);
insert into northwind.order_details
values (
  10924, 
  10, 
  31, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10924, 
  28, 
  45.5999985, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10924, 
  75, 
  7.75, 
  6, 
  0
);
insert into northwind.order_details
values (
  10925, 
  36, 
  19, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10925, 
  52, 
  7, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10926, 
  11, 
  21, 
  2, 
  0
);
insert into northwind.order_details
values (
  10926, 
  13, 
  6, 
  10, 
  0
);
insert into northwind.order_details
values (
  10926, 
  19, 
  9.19999981, 
  7, 
  0
);
insert into northwind.order_details
values (
  10926, 
  72, 
  34.7999992, 
  10, 
  0
);
insert into northwind.order_details
values (
  10927, 
  20, 
  81, 
  5, 
  0
);
insert into northwind.order_details
values (
  10927, 
  52, 
  7, 
  5, 
  0
);
insert into northwind.order_details
values (
  10927, 
  76, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  10928, 
  47, 
  9.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10928, 
  76, 
  18, 
  5, 
  0
);
insert into northwind.order_details
values (
  10929, 
  21, 
  10, 
  60, 
  0
);
insert into northwind.order_details
values (
  10929, 
  75, 
  7.75, 
  49, 
  0
);
insert into northwind.order_details
values (
  10929, 
  77, 
  13, 
  15, 
  0
);
insert into northwind.order_details
values (
  10930, 
  21, 
  10, 
  36, 
  0
);
insert into northwind.order_details
values (
  10930, 
  27, 
  43.9000015, 
  25, 
  0
);
insert into northwind.order_details
values (
  10930, 
  55, 
  24, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  10930, 
  58, 
  13.25, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10931, 
  13, 
  6, 
  42, 
  0.150000006
);
insert into northwind.order_details
values (
  10931, 
  57, 
  19.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10932, 
  16, 
  17.4500008, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  10932, 
  62, 
  49.2999992, 
  14, 
  0.100000001
);
insert into northwind.order_details
values (
  10932, 
  72, 
  34.7999992, 
  16, 
  0
);
insert into northwind.order_details
values (
  10932, 
  75, 
  7.75, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  10933, 
  53, 
  32.7999992, 
  2, 
  0
);
insert into northwind.order_details
values (
  10933, 
  61, 
  28.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10934, 
  6, 
  25, 
  20, 
  0
);
insert into northwind.order_details
values (
  10935, 
  1, 
  18, 
  21, 
  0
);
insert into northwind.order_details
values (
  10935, 
  18, 
  62.5, 
  4, 
  0.25
);
insert into northwind.order_details
values (
  10935, 
  23, 
  9, 
  8, 
  0.25
);
insert into northwind.order_details
values (
  10936, 
  36, 
  19, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  10937, 
  28, 
  45.5999985, 
  8, 
  0
);
insert into northwind.order_details
values (
  10937, 
  34, 
  14, 
  20, 
  0
);
insert into northwind.order_details
values (
  10938, 
  13, 
  6, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10938, 
  43, 
  46, 
  24, 
  0.25
);
insert into northwind.order_details
values (
  10938, 
  60, 
  34, 
  49, 
  0.25
);
insert into northwind.order_details
values (
  10938, 
  71, 
  21.5, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10939, 
  2, 
  19, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  10939, 
  67, 
  14, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10940, 
  7, 
  30, 
  8, 
  0
);
insert into northwind.order_details
values (
  10940, 
  13, 
  6, 
  20, 
  0
);
insert into northwind.order_details
values (
  10941, 
  31, 
  12.5, 
  44, 
  0.25
);
insert into northwind.order_details
values (
  10941, 
  62, 
  49.2999992, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  10941, 
  68, 
  12.5, 
  80, 
  0.25
);
insert into northwind.order_details
values (
  10941, 
  72, 
  34.7999992, 
  50, 
  0
);
insert into northwind.order_details
values (
  10942, 
  49, 
  20, 
  28, 
  0
);
insert into northwind.order_details
values (
  10943, 
  13, 
  6, 
  15, 
  0
);
insert into northwind.order_details
values (
  10943, 
  22, 
  21, 
  21, 
  0
);
insert into northwind.order_details
values (
  10943, 
  46, 
  12, 
  15, 
  0
);
insert into northwind.order_details
values (
  10944, 
  11, 
  21, 
  5, 
  0.25
);
insert into northwind.order_details
values (
  10944, 
  44, 
  19.4500008, 
  18, 
  0.25
);
insert into northwind.order_details
values (
  10944, 
  56, 
  38, 
  18, 
  0
);
insert into northwind.order_details
values (
  10945, 
  13, 
  6, 
  20, 
  0
);
insert into northwind.order_details
values (
  10945, 
  31, 
  12.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  10946, 
  10, 
  31, 
  25, 
  0
);
insert into northwind.order_details
values (
  10946, 
  24, 
  4.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  10946, 
  77, 
  13, 
  40, 
  0
);
insert into northwind.order_details
values (
  10947, 
  59, 
  55, 
  4, 
  0
);
insert into northwind.order_details
values (
  10948, 
  50, 
  16.25, 
  9, 
  0
);
insert into northwind.order_details
values (
  10948, 
  51, 
  53, 
  40, 
  0
);
insert into northwind.order_details
values (
  10948, 
  55, 
  24, 
  4, 
  0
);
insert into northwind.order_details
values (
  10949, 
  6, 
  25, 
  12, 
  0
);
insert into northwind.order_details
values (
  10949, 
  10, 
  31, 
  30, 
  0
);
insert into northwind.order_details
values (
  10949, 
  17, 
  39, 
  6, 
  0
);
insert into northwind.order_details
values (
  10949, 
  62, 
  49.2999992, 
  60, 
  0
);
insert into northwind.order_details
values (
  10950, 
  4, 
  22, 
  5, 
  0
);
insert into northwind.order_details
values (
  10951, 
  33, 
  2.5, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10951, 
  41, 
  9.64999962, 
  6, 
  0.0500000007
);
insert into northwind.order_details
values (
  10951, 
  75, 
  7.75, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10952, 
  6, 
  25, 
  16, 
  0.0500000007
);
insert into northwind.order_details
values (
  10952, 
  28, 
  45.5999985, 
  2, 
  0
);
insert into northwind.order_details
values (
  10953, 
  20, 
  81, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10953, 
  31, 
  12.5, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  10954, 
  16, 
  17.4500008, 
  28, 
  0.150000006
);
insert into northwind.order_details
values (
  10954, 
  31, 
  12.5, 
  25, 
  0.150000006
);
insert into northwind.order_details
values (
  10954, 
  45, 
  9.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10954, 
  60, 
  34, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  10955, 
  75, 
  7.75, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  10956, 
  21, 
  10, 
  12, 
  0
);
insert into northwind.order_details
values (
  10956, 
  47, 
  9.5, 
  14, 
  0
);
insert into northwind.order_details
values (
  10956, 
  51, 
  53, 
  8, 
  0
);
insert into northwind.order_details
values (
  10957, 
  30, 
  25.8899994, 
  30, 
  0
);
insert into northwind.order_details
values (
  10957, 
  35, 
  18, 
  40, 
  0
);
insert into northwind.order_details
values (
  10957, 
  64, 
  33.25, 
  8, 
  0
);
insert into northwind.order_details
values (
  10958, 
  5, 
  21.3500004, 
  20, 
  0
);
insert into northwind.order_details
values (
  10958, 
  7, 
  30, 
  6, 
  0
);
insert into northwind.order_details
values (
  10958, 
  72, 
  34.7999992, 
  5, 
  0
);
insert into northwind.order_details
values (
  10959, 
  75, 
  7.75, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10960, 
  24, 
  4.5, 
  10, 
  0.25
);
insert into northwind.order_details
values (
  10960, 
  41, 
  9.64999962, 
  24, 
  0
);
insert into northwind.order_details
values (
  10961, 
  52, 
  7, 
  6, 
  0.0500000007
);
insert into northwind.order_details
values (
  10961, 
  76, 
  18, 
  60, 
  0
);
insert into northwind.order_details
values (
  10962, 
  7, 
  30, 
  45, 
  0
);
insert into northwind.order_details
values (
  10962, 
  13, 
  6, 
  77, 
  0
);
insert into northwind.order_details
values (
  10962, 
  53, 
  32.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10962, 
  69, 
  36, 
  9, 
  0
);
insert into northwind.order_details
values (
  10962, 
  76, 
  18, 
  44, 
  0
);
insert into northwind.order_details
values (
  10963, 
  60, 
  34, 
  2, 
  0.150000006
);
insert into northwind.order_details
values (
  10964, 
  18, 
  62.5, 
  6, 
  0
);
insert into northwind.order_details
values (
  10964, 
  38, 
  263.5, 
  5, 
  0
);
insert into northwind.order_details
values (
  10964, 
  69, 
  36, 
  10, 
  0
);
insert into northwind.order_details
values (
  10965, 
  51, 
  53, 
  16, 
  0
);
insert into northwind.order_details
values (
  10966, 
  37, 
  26, 
  8, 
  0
);
insert into northwind.order_details
values (
  10966, 
  56, 
  38, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10966, 
  62, 
  49.2999992, 
  12, 
  0.150000006
);
insert into northwind.order_details
values (
  10967, 
  19, 
  9.19999981, 
  12, 
  0
);
insert into northwind.order_details
values (
  10967, 
  49, 
  20, 
  40, 
  0
);
insert into northwind.order_details
values (
  10968, 
  12, 
  38, 
  30, 
  0
);
insert into northwind.order_details
values (
  10968, 
  24, 
  4.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10968, 
  64, 
  33.25, 
  4, 
  0
);
insert into northwind.order_details
values (
  10969, 
  46, 
  12, 
  9, 
  0
);
insert into northwind.order_details
values (
  10970, 
  52, 
  7, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10971, 
  29, 
  123.790001, 
  14, 
  0
);
insert into northwind.order_details
values (
  10972, 
  17, 
  39, 
  6, 
  0
);
insert into northwind.order_details
values (
  10972, 
  33, 
  2.5, 
  7, 
  0
);
insert into northwind.order_details
values (
  10973, 
  26, 
  31.2299995, 
  5, 
  0
);
insert into northwind.order_details
values (
  10973, 
  41, 
  9.64999962, 
  6, 
  0
);
insert into northwind.order_details
values (
  10973, 
  75, 
  7.75, 
  10, 
  0
);
insert into northwind.order_details
values (
  10974, 
  63, 
  43.9000015, 
  10, 
  0
);
insert into northwind.order_details
values (
  10975, 
  8, 
  40, 
  16, 
  0
);
insert into northwind.order_details
values (
  10975, 
  75, 
  7.75, 
  10, 
  0
);
insert into northwind.order_details
values (
  10976, 
  28, 
  45.5999985, 
  20, 
  0
);
insert into northwind.order_details
values (
  10977, 
  39, 
  18, 
  30, 
  0
);
insert into northwind.order_details
values (
  10977, 
  47, 
  9.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  10977, 
  51, 
  53, 
  10, 
  0
);
insert into northwind.order_details
values (
  10977, 
  63, 
  43.9000015, 
  20, 
  0
);
insert into northwind.order_details
values (
  10978, 
  8, 
  40, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  10978, 
  21, 
  10, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  10978, 
  40, 
  18.3999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  10978, 
  44, 
  19.4500008, 
  6, 
  0.150000006
);
insert into northwind.order_details
values (
  10979, 
  7, 
  30, 
  18, 
  0
);
insert into northwind.order_details
values (
  10979, 
  12, 
  38, 
  20, 
  0
);
insert into northwind.order_details
values (
  10979, 
  24, 
  4.5, 
  80, 
  0
);
insert into northwind.order_details
values (
  10979, 
  27, 
  43.9000015, 
  30, 
  0
);
insert into northwind.order_details
values (
  10979, 
  31, 
  12.5, 
  24, 
  0
);
insert into northwind.order_details
values (
  10979, 
  63, 
  43.9000015, 
  35, 
  0
);
insert into northwind.order_details
values (
  10980, 
  75, 
  7.75, 
  40, 
  0.200000003
);
insert into northwind.order_details
values (
  10981, 
  38, 
  263.5, 
  60, 
  0
);
insert into northwind.order_details
values (
  10982, 
  7, 
  30, 
  20, 
  0
);
insert into northwind.order_details
values (
  10982, 
  43, 
  46, 
  9, 
  0
);
insert into northwind.order_details
values (
  10983, 
  13, 
  6, 
  84, 
  0.150000006
);
insert into northwind.order_details
values (
  10983, 
  57, 
  19.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  10984, 
  16, 
  17.4500008, 
  55, 
  0
);
insert into northwind.order_details
values (
  10984, 
  24, 
  4.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  10984, 
  36, 
  19, 
  40, 
  0
);
insert into northwind.order_details
values (
  10985, 
  16, 
  17.4500008, 
  36, 
  0.100000001
);
insert into northwind.order_details
values (
  10985, 
  18, 
  62.5, 
  8, 
  0.100000001
);
insert into northwind.order_details
values (
  10985, 
  32, 
  32, 
  35, 
  0.100000001
);
insert into northwind.order_details
values (
  10986, 
  11, 
  21, 
  30, 
  0
);
insert into northwind.order_details
values (
  10986, 
  20, 
  81, 
  15, 
  0
);
insert into northwind.order_details
values (
  10986, 
  76, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  10986, 
  77, 
  13, 
  15, 
  0
);
insert into northwind.order_details
values (
  10987, 
  7, 
  30, 
  60, 
  0
);
insert into northwind.order_details
values (
  10987, 
  43, 
  46, 
  6, 
  0
);
insert into northwind.order_details
values (
  10987, 
  72, 
  34.7999992, 
  20, 
  0
);
insert into northwind.order_details
values (
  10988, 
  7, 
  30, 
  60, 
  0
);
insert into northwind.order_details
values (
  10988, 
  62, 
  49.2999992, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  10989, 
  6, 
  25, 
  40, 
  0
);
insert into northwind.order_details
values (
  10989, 
  11, 
  21, 
  15, 
  0
);
insert into northwind.order_details
values (
  10989, 
  41, 
  9.64999962, 
  4, 
  0
);
insert into northwind.order_details
values (
  10990, 
  21, 
  10, 
  65, 
  0
);
insert into northwind.order_details
values (
  10990, 
  34, 
  14, 
  60, 
  0.150000006
);
insert into northwind.order_details
values (
  10990, 
  55, 
  24, 
  65, 
  0.150000006
);
insert into northwind.order_details
values (
  10990, 
  61, 
  28.5, 
  66, 
  0.150000006
);
insert into northwind.order_details
values (
  10991, 
  2, 
  19, 
  50, 
  0.200000003
);
insert into northwind.order_details
values (
  10991, 
  70, 
  15, 
  20, 
  0.200000003
);
insert into northwind.order_details
values (
  10991, 
  76, 
  18, 
  90, 
  0.200000003
);
insert into northwind.order_details
values (
  10992, 
  72, 
  34.7999992, 
  2, 
  0
);
insert into northwind.order_details
values (
  10993, 
  29, 
  123.790001, 
  50, 
  0.25
);
insert into northwind.order_details
values (
  10993, 
  41, 
  9.64999962, 
  35, 
  0.25
);
insert into northwind.order_details
values (
  10994, 
  59, 
  55, 
  18, 
  0.0500000007
);
insert into northwind.order_details
values (
  10995, 
  51, 
  53, 
  20, 
  0
);
insert into northwind.order_details
values (
  10995, 
  60, 
  34, 
  4, 
  0
);
insert into northwind.order_details
values (
  10996, 
  42, 
  14, 
  40, 
  0
);
insert into northwind.order_details
values (
  10997, 
  32, 
  32, 
  50, 
  0
);
insert into northwind.order_details
values (
  10997, 
  46, 
  12, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10997, 
  52, 
  7, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  10998, 
  24, 
  4.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  10998, 
  61, 
  28.5, 
  7, 
  0
);
insert into northwind.order_details
values (
  10998, 
  74, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  10998, 
  75, 
  7.75, 
  30, 
  0
);
insert into northwind.order_details
values (
  10999, 
  41, 
  9.64999962, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  10999, 
  51, 
  53, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  10999, 
  77, 
  13, 
  21, 
  0.0500000007
);
insert into northwind.order_details
values (
  11000, 
  4, 
  22, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  11000, 
  24, 
  4.5, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  11000, 
  77, 
  13, 
  30, 
  0
);
insert into northwind.order_details
values (
  11001, 
  7, 
  30, 
  60, 
  0
);
insert into northwind.order_details
values (
  11001, 
  22, 
  21, 
  25, 
  0
);
insert into northwind.order_details
values (
  11001, 
  46, 
  12, 
  25, 
  0
);
insert into northwind.order_details
values (
  11001, 
  55, 
  24, 
  6, 
  0
);
insert into northwind.order_details
values (
  11002, 
  13, 
  6, 
  56, 
  0
);
insert into northwind.order_details
values (
  11002, 
  35, 
  18, 
  15, 
  0.150000006
);
insert into northwind.order_details
values (
  11002, 
  42, 
  14, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  11002, 
  55, 
  24, 
  40, 
  0
);
insert into northwind.order_details
values (
  11003, 
  1, 
  18, 
  4, 
  0
);
insert into northwind.order_details
values (
  11003, 
  40, 
  18.3999996, 
  10, 
  0
);
insert into northwind.order_details
values (
  11003, 
  52, 
  7, 
  10, 
  0
);
insert into northwind.order_details
values (
  11004, 
  26, 
  31.2299995, 
  6, 
  0
);
insert into northwind.order_details
values (
  11004, 
  76, 
  18, 
  6, 
  0
);
insert into northwind.order_details
values (
  11005, 
  1, 
  18, 
  2, 
  0
);
insert into northwind.order_details
values (
  11005, 
  59, 
  55, 
  10, 
  0
);
insert into northwind.order_details
values (
  11006, 
  1, 
  18, 
  8, 
  0
);
insert into northwind.order_details
values (
  11006, 
  29, 
  123.790001, 
  2, 
  0.25
);
insert into northwind.order_details
values (
  11007, 
  8, 
  40, 
  30, 
  0
);
insert into northwind.order_details
values (
  11007, 
  29, 
  123.790001, 
  10, 
  0
);
insert into northwind.order_details
values (
  11007, 
  42, 
  14, 
  14, 
  0
);
insert into northwind.order_details
values (
  11008, 
  28, 
  45.5999985, 
  70, 
  0.0500000007
);
insert into northwind.order_details
values (
  11008, 
  34, 
  14, 
  90, 
  0.0500000007
);
insert into northwind.order_details
values (
  11008, 
  71, 
  21.5, 
  21, 
  0
);
insert into northwind.order_details
values (
  11009, 
  24, 
  4.5, 
  12, 
  0
);
insert into northwind.order_details
values (
  11009, 
  36, 
  19, 
  18, 
  0.25
);
insert into northwind.order_details
values (
  11009, 
  60, 
  34, 
  9, 
  0
);
insert into northwind.order_details
values (
  11010, 
  7, 
  30, 
  20, 
  0
);
insert into northwind.order_details
values (
  11010, 
  24, 
  4.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  11011, 
  58, 
  13.25, 
  40, 
  0.0500000007
);
insert into northwind.order_details
values (
  11011, 
  71, 
  21.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  11012, 
  19, 
  9.19999981, 
  50, 
  0.0500000007
);
insert into northwind.order_details
values (
  11012, 
  60, 
  34, 
  36, 
  0.0500000007
);
insert into northwind.order_details
values (
  11012, 
  71, 
  21.5, 
  60, 
  0.0500000007
);
insert into northwind.order_details
values (
  11013, 
  23, 
  9, 
  10, 
  0
);
insert into northwind.order_details
values (
  11013, 
  42, 
  14, 
  4, 
  0
);
insert into northwind.order_details
values (
  11013, 
  45, 
  9.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  11013, 
  68, 
  12.5, 
  2, 
  0
);
insert into northwind.order_details
values (
  11014, 
  41, 
  9.64999962, 
  28, 
  0.100000001
);
insert into northwind.order_details
values (
  11015, 
  30, 
  25.8899994, 
  15, 
  0
);
insert into northwind.order_details
values (
  11015, 
  77, 
  13, 
  18, 
  0
);
insert into northwind.order_details
values (
  11016, 
  31, 
  12.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  11016, 
  36, 
  19, 
  16, 
  0
);
insert into northwind.order_details
values (
  11017, 
  3, 
  10, 
  25, 
  0
);
insert into northwind.order_details
values (
  11017, 
  59, 
  55, 
  110, 
  0
);
insert into northwind.order_details
values (
  11017, 
  70, 
  15, 
  30, 
  0
);
insert into northwind.order_details
values (
  11018, 
  12, 
  38, 
  20, 
  0
);
insert into northwind.order_details
values (
  11018, 
  18, 
  62.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  11018, 
  56, 
  38, 
  5, 
  0
);
insert into northwind.order_details
values (
  11019, 
  46, 
  12, 
  3, 
  0
);
insert into northwind.order_details
values (
  11019, 
  49, 
  20, 
  2, 
  0
);
insert into northwind.order_details
values (
  11020, 
  10, 
  31, 
  24, 
  0.150000006
);
insert into northwind.order_details
values (
  11021, 
  2, 
  19, 
  11, 
  0.25
);
insert into northwind.order_details
values (
  11021, 
  20, 
  81, 
  15, 
  0
);
insert into northwind.order_details
values (
  11021, 
  26, 
  31.2299995, 
  63, 
  0
);
insert into northwind.order_details
values (
  11021, 
  51, 
  53, 
  44, 
  0.25
);
insert into northwind.order_details
values (
  11021, 
  72, 
  34.7999992, 
  35, 
  0
);
insert into northwind.order_details
values (
  11022, 
  19, 
  9.19999981, 
  35, 
  0
);
insert into northwind.order_details
values (
  11022, 
  69, 
  36, 
  30, 
  0
);
insert into northwind.order_details
values (
  11023, 
  7, 
  30, 
  4, 
  0
);
insert into northwind.order_details
values (
  11023, 
  43, 
  46, 
  30, 
  0
);
insert into northwind.order_details
values (
  11024, 
  26, 
  31.2299995, 
  12, 
  0
);
insert into northwind.order_details
values (
  11024, 
  33, 
  2.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  11024, 
  65, 
  21.0499992, 
  21, 
  0
);
insert into northwind.order_details
values (
  11024, 
  71, 
  21.5, 
  50, 
  0
);
insert into northwind.order_details
values (
  11025, 
  1, 
  18, 
  10, 
  0.100000001
);
insert into northwind.order_details
values (
  11025, 
  13, 
  6, 
  20, 
  0.100000001
);
insert into northwind.order_details
values (
  11026, 
  18, 
  62.5, 
  8, 
  0
);
insert into northwind.order_details
values (
  11026, 
  51, 
  53, 
  10, 
  0
);
insert into northwind.order_details
values (
  11027, 
  24, 
  4.5, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  11027, 
  62, 
  49.2999992, 
  21, 
  0.25
);
insert into northwind.order_details
values (
  11028, 
  55, 
  24, 
  35, 
  0
);
insert into northwind.order_details
values (
  11028, 
  59, 
  55, 
  24, 
  0
);
insert into northwind.order_details
values (
  11029, 
  56, 
  38, 
  20, 
  0
);
insert into northwind.order_details
values (
  11029, 
  63, 
  43.9000015, 
  12, 
  0
);
insert into northwind.order_details
values (
  11030, 
  2, 
  19, 
  100, 
  0.25
);
insert into northwind.order_details
values (
  11030, 
  5, 
  21.3500004, 
  70, 
  0
);
insert into northwind.order_details
values (
  11030, 
  29, 
  123.790001, 
  60, 
  0.25
);
insert into northwind.order_details
values (
  11030, 
  59, 
  55, 
  100, 
  0.25
);
insert into northwind.order_details
values (
  11031, 
  1, 
  18, 
  45, 
  0
);
insert into northwind.order_details
values (
  11031, 
  13, 
  6, 
  80, 
  0
);
insert into northwind.order_details
values (
  11031, 
  24, 
  4.5, 
  21, 
  0
);
insert into northwind.order_details
values (
  11031, 
  64, 
  33.25, 
  20, 
  0
);
insert into northwind.order_details
values (
  11031, 
  71, 
  21.5, 
  16, 
  0
);
insert into northwind.order_details
values (
  11032, 
  36, 
  19, 
  35, 
  0
);
insert into northwind.order_details
values (
  11032, 
  38, 
  263.5, 
  25, 
  0
);
insert into northwind.order_details
values (
  11032, 
  59, 
  55, 
  30, 
  0
);
insert into northwind.order_details
values (
  11033, 
  53, 
  32.7999992, 
  70, 
  0.100000001
);
insert into northwind.order_details
values (
  11033, 
  69, 
  36, 
  36, 
  0.100000001
);
insert into northwind.order_details
values (
  11034, 
  21, 
  10, 
  15, 
  0.100000001
);
insert into northwind.order_details
values (
  11034, 
  44, 
  19.4500008, 
  12, 
  0
);
insert into northwind.order_details
values (
  11034, 
  61, 
  28.5, 
  6, 
  0
);
insert into northwind.order_details
values (
  11035, 
  1, 
  18, 
  10, 
  0
);
insert into northwind.order_details
values (
  11035, 
  35, 
  18, 
  60, 
  0
);
insert into northwind.order_details
values (
  11035, 
  42, 
  14, 
  30, 
  0
);
insert into northwind.order_details
values (
  11035, 
  54, 
  7.44999981, 
  10, 
  0
);
insert into northwind.order_details
values (
  11036, 
  13, 
  6, 
  7, 
  0
);
insert into northwind.order_details
values (
  11036, 
  59, 
  55, 
  30, 
  0
);
insert into northwind.order_details
values (
  11037, 
  70, 
  15, 
  4, 
  0
);
insert into northwind.order_details
values (
  11038, 
  40, 
  18.3999996, 
  5, 
  0.200000003
);
insert into northwind.order_details
values (
  11038, 
  52, 
  7, 
  2, 
  0
);
insert into northwind.order_details
values (
  11038, 
  71, 
  21.5, 
  30, 
  0
);
insert into northwind.order_details
values (
  11039, 
  28, 
  45.5999985, 
  20, 
  0
);
insert into northwind.order_details
values (
  11039, 
  35, 
  18, 
  24, 
  0
);
insert into northwind.order_details
values (
  11039, 
  49, 
  20, 
  60, 
  0
);
insert into northwind.order_details
values (
  11039, 
  57, 
  19.5, 
  28, 
  0
);
insert into northwind.order_details
values (
  11040, 
  21, 
  10, 
  20, 
  0
);
insert into northwind.order_details
values (
  11041, 
  2, 
  19, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  11041, 
  63, 
  43.9000015, 
  30, 
  0
);
insert into northwind.order_details
values (
  11042, 
  44, 
  19.4500008, 
  15, 
  0
);
insert into northwind.order_details
values (
  11042, 
  61, 
  28.5, 
  4, 
  0
);
insert into northwind.order_details
values (
  11043, 
  11, 
  21, 
  10, 
  0
);
insert into northwind.order_details
values (
  11044, 
  62, 
  49.2999992, 
  12, 
  0
);
insert into northwind.order_details
values (
  11045, 
  33, 
  2.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  11045, 
  51, 
  53, 
  24, 
  0
);
insert into northwind.order_details
values (
  11046, 
  12, 
  38, 
  20, 
  0.0500000007
);
insert into northwind.order_details
values (
  11046, 
  32, 
  32, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  11046, 
  35, 
  18, 
  18, 
  0.0500000007
);
insert into northwind.order_details
values (
  11047, 
  1, 
  18, 
  25, 
  0.25
);
insert into northwind.order_details
values (
  11047, 
  5, 
  21.3500004, 
  30, 
  0.25
);
insert into northwind.order_details
values (
  11048, 
  68, 
  12.5, 
  42, 
  0
);
insert into northwind.order_details
values (
  11049, 
  2, 
  19, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  11049, 
  12, 
  38, 
  4, 
  0.200000003
);
insert into northwind.order_details
values (
  11050, 
  76, 
  18, 
  50, 
  0.100000001
);
insert into northwind.order_details
values (
  11051, 
  24, 
  4.5, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  11052, 
  43, 
  46, 
  30, 
  0.200000003
);
insert into northwind.order_details
values (
  11052, 
  61, 
  28.5, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  11053, 
  18, 
  62.5, 
  35, 
  0.200000003
);
insert into northwind.order_details
values (
  11053, 
  32, 
  32, 
  20, 
  0
);
insert into northwind.order_details
values (
  11053, 
  64, 
  33.25, 
  25, 
  0.200000003
);
insert into northwind.order_details
values (
  11054, 
  33, 
  2.5, 
  10, 
  0
);
insert into northwind.order_details
values (
  11054, 
  67, 
  14, 
  20, 
  0
);
insert into northwind.order_details
values (
  11055, 
  24, 
  4.5, 
  15, 
  0
);
insert into northwind.order_details
values (
  11055, 
  25, 
  14, 
  15, 
  0
);
insert into northwind.order_details
values (
  11055, 
  51, 
  53, 
  20, 
  0
);
insert into northwind.order_details
values (
  11055, 
  57, 
  19.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  11056, 
  7, 
  30, 
  40, 
  0
);
insert into northwind.order_details
values (
  11056, 
  55, 
  24, 
  35, 
  0
);
insert into northwind.order_details
values (
  11056, 
  60, 
  34, 
  50, 
  0
);
insert into northwind.order_details
values (
  11057, 
  70, 
  15, 
  3, 
  0
);
insert into northwind.order_details
values (
  11058, 
  21, 
  10, 
  3, 
  0
);
insert into northwind.order_details
values (
  11058, 
  60, 
  34, 
  21, 
  0
);
insert into northwind.order_details
values (
  11058, 
  61, 
  28.5, 
  4, 
  0
);
insert into northwind.order_details
values (
  11059, 
  13, 
  6, 
  30, 
  0
);
insert into northwind.order_details
values (
  11059, 
  17, 
  39, 
  12, 
  0
);
insert into northwind.order_details
values (
  11059, 
  60, 
  34, 
  35, 
  0
);
insert into northwind.order_details
values (
  11060, 
  60, 
  34, 
  4, 
  0
);
insert into northwind.order_details
values (
  11060, 
  77, 
  13, 
  10, 
  0
);
insert into northwind.order_details
values (
  11061, 
  60, 
  34, 
  15, 
  0
);
insert into northwind.order_details
values (
  11062, 
  53, 
  32.7999992, 
  10, 
  0.200000003
);
insert into northwind.order_details
values (
  11062, 
  70, 
  15, 
  12, 
  0.200000003
);
insert into northwind.order_details
values (
  11063, 
  34, 
  14, 
  30, 
  0
);
insert into northwind.order_details
values (
  11063, 
  40, 
  18.3999996, 
  40, 
  0.100000001
);
insert into northwind.order_details
values (
  11063, 
  41, 
  9.64999962, 
  30, 
  0.100000001
);
insert into northwind.order_details
values (
  11064, 
  17, 
  39, 
  77, 
  0.100000001
);
insert into northwind.order_details
values (
  11064, 
  41, 
  9.64999962, 
  12, 
  0
);
insert into northwind.order_details
values (
  11064, 
  53, 
  32.7999992, 
  25, 
  0.100000001
);
insert into northwind.order_details
values (
  11064, 
  55, 
  24, 
  4, 
  0.100000001
);
insert into northwind.order_details
values (
  11064, 
  68, 
  12.5, 
  55, 
  0
);
insert into northwind.order_details
values (
  11065, 
  30, 
  25.8899994, 
  4, 
  0.25
);
insert into northwind.order_details
values (
  11065, 
  54, 
  7.44999981, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  11066, 
  16, 
  17.4500008, 
  3, 
  0
);
insert into northwind.order_details
values (
  11066, 
  19, 
  9.19999981, 
  42, 
  0
);
insert into northwind.order_details
values (
  11066, 
  34, 
  14, 
  35, 
  0
);
insert into northwind.order_details
values (
  11067, 
  41, 
  9.64999962, 
  9, 
  0
);
insert into northwind.order_details
values (
  11068, 
  28, 
  45.5999985, 
  8, 
  0.150000006
);
insert into northwind.order_details
values (
  11068, 
  43, 
  46, 
  36, 
  0.150000006
);
insert into northwind.order_details
values (
  11068, 
  77, 
  13, 
  28, 
  0.150000006
);
insert into northwind.order_details
values (
  11069, 
  39, 
  18, 
  20, 
  0
);
insert into northwind.order_details
values (
  11070, 
  1, 
  18, 
  40, 
  0.150000006
);
insert into northwind.order_details
values (
  11070, 
  2, 
  19, 
  20, 
  0.150000006
);
insert into northwind.order_details
values (
  11070, 
  16, 
  17.4500008, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  11070, 
  31, 
  12.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  11071, 
  7, 
  30, 
  15, 
  0.0500000007
);
insert into northwind.order_details
values (
  11071, 
  13, 
  6, 
  10, 
  0.0500000007
);
insert into northwind.order_details
values (
  11072, 
  2, 
  19, 
  8, 
  0
);
insert into northwind.order_details
values (
  11072, 
  41, 
  9.64999962, 
  40, 
  0
);
insert into northwind.order_details
values (
  11072, 
  50, 
  16.25, 
  22, 
  0
);
insert into northwind.order_details
values (
  11072, 
  64, 
  33.25, 
  130, 
  0
);
insert into northwind.order_details
values (
  11073, 
  11, 
  21, 
  10, 
  0
);
insert into northwind.order_details
values (
  11073, 
  24, 
  4.5, 
  20, 
  0
);
insert into northwind.order_details
values (
  11074, 
  16, 
  17.4500008, 
  14, 
  0.0500000007
);
insert into northwind.order_details
values (
  11075, 
  2, 
  19, 
  10, 
  0.150000006
);
insert into northwind.order_details
values (
  11075, 
  46, 
  12, 
  30, 
  0.150000006
);
insert into northwind.order_details
values (
  11075, 
  76, 
  18, 
  2, 
  0.150000006
);
insert into northwind.order_details
values (
  11076, 
  6, 
  25, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  11076, 
  14, 
  23.25, 
  20, 
  0.25
);
insert into northwind.order_details
values (
  11076, 
  19, 
  9.19999981, 
  10, 
  0.25
);
insert into northwind.order_details
values (
  11077, 
  2, 
  19, 
  24, 
  0.200000003
);
insert into northwind.order_details
values (
  11077, 
  3, 
  10, 
  4, 
  0
);
insert into northwind.order_details
values (
  11077, 
  4, 
  22, 
  1, 
  0
);
insert into northwind.order_details
values (
  11077, 
  6, 
  25, 
  1, 
  0.0199999996
);
insert into northwind.order_details
values (
  11077, 
  7, 
  30, 
  1, 
  0.0500000007
);
insert into northwind.order_details
values (
  11077, 
  8, 
  40, 
  2, 
  0.100000001
);
insert into northwind.order_details
values (
  11077, 
  10, 
  31, 
  1, 
  0
);
insert into northwind.order_details
values (
  11077, 
  12, 
  38, 
  2, 
  0.0500000007
);
insert into northwind.order_details
values (
  11077, 
  13, 
  6, 
  4, 
  0
);
insert into northwind.order_details
values (
  11077, 
  14, 
  23.25, 
  1, 
  0.0299999993
);
insert into northwind.order_details
values (
  11077, 
  16, 
  17.4500008, 
  2, 
  0.0299999993
);
insert into northwind.order_details
values (
  11077, 
  20, 
  81, 
  1, 
  0.0399999991
);
insert into northwind.order_details
values (
  11077, 
  23, 
  9, 
  2, 
  0
);
insert into northwind.order_details
values (
  11077, 
  32, 
  32, 
  1, 
  0
);
insert into northwind.order_details
values (
  11077, 
  39, 
  18, 
  2, 
  0.0500000007
);
insert into northwind.order_details
values (
  11077, 
  41, 
  9.64999962, 
  3, 
  0
);
insert into northwind.order_details
values (
  11077, 
  46, 
  12, 
  3, 
  0.0199999996
);
insert into northwind.order_details
values (
  11077, 
  52, 
  7, 
  2, 
  0
);
insert into northwind.order_details
values (
  11077, 
  55, 
  24, 
  2, 
  0
);
insert into northwind.order_details
values (
  11077, 
  60, 
  34, 
  2, 
  0.0599999987
);
insert into northwind.order_details
values (
  11077, 
  64, 
  33.25, 
  2, 
  0.0299999993
);
insert into northwind.order_details
values (
  11077, 
  66, 
  17, 
  1, 
  0
);
insert into northwind.order_details
values (
  11077, 
  73, 
  15, 
  2, 
  0.00999999978
);
insert into northwind.order_details
values (
  11077, 
  75, 
  7.75, 
  4, 
  0
);
insert into northwind.order_details
values (
  11077, 
  77, 
  13, 
  2, 
  0
);
insert into northwind.orders
values (
  10248, 
  'VINET', 
  5, 
  '1996-07-04', 
  '1996-08-01', 
  '1996-07-16', 
  3, 
  32.3800011, 
  'Vins et alcools Chevalier', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France'
);
insert into northwind.orders
values (
  10249, 
  'TOMSP', 
  6, 
  '1996-07-05', 
  '1996-08-16', 
  '1996-07-10', 
  1, 
  11.6099997, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10250, 
  'HANAR', 
  4, 
  '1996-07-08', 
  '1996-08-05', 
  '1996-07-12', 
  2, 
  65.8300018, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10251, 
  'VICTE', 
  3, 
  '1996-07-08', 
  '1996-08-05', 
  '1996-07-15', 
  1, 
  41.3400002, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10252, 
  'SUPRD', 
  4, 
  '1996-07-09', 
  '1996-08-06', 
  '1996-07-11', 
  2, 
  51.2999992, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10253, 
  'HANAR', 
  3, 
  '1996-07-10', 
  '1996-07-24', 
  '1996-07-16', 
  2, 
  58.1699982, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10254, 
  'CHOPS', 
  5, 
  '1996-07-11', 
  '1996-08-08', 
  '1996-07-23', 
  2, 
  22.9799995, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10255, 
  'RICSU', 
  9, 
  '1996-07-12', 
  '1996-08-09', 
  '1996-07-15', 
  3, 
  148.330002, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10256, 
  'WELLI', 
  3, 
  '1996-07-15', 
  '1996-08-12', 
  '1996-07-17', 
  2, 
  13.9700003, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10257, 
  'HILAA', 
  4, 
  '1996-07-16', 
  '1996-08-13', 
  '1996-07-22', 
  3, 
  81.9100037, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10258, 
  'ERNSH', 
  1, 
  '1996-07-17', 
  '1996-08-14', 
  '1996-07-23', 
  1, 
  140.509995, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10259, 
  'CENTC', 
  4, 
  '1996-07-18', 
  '1996-08-15', 
  '1996-07-25', 
  3, 
  3.25, 
  'Centro comercial Moctezuma', 
  'Sierras de Granada 9993', 
  'México D.F.', 
  null, 
  '05022', 
  'Mexico'
);
insert into northwind.orders
values (
  10260, 
  'OTTIK', 
  4, 
  '1996-07-19', 
  '1996-08-16', 
  '1996-07-29', 
  1, 
  55.0900002, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10261, 
  'QUEDE', 
  4, 
  '1996-07-19', 
  '1996-08-16', 
  '1996-07-30', 
  2, 
  3.04999995, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10262, 
  'RATTC', 
  8, 
  '1996-07-22', 
  '1996-08-19', 
  '1996-07-25', 
  3, 
  48.2900009, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10263, 
  'ERNSH', 
  9, 
  '1996-07-23', 
  '1996-08-20', 
  '1996-07-31', 
  3, 
  146.059998, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10264, 
  'FOLKO', 
  6, 
  '1996-07-24', 
  '1996-08-21', 
  '1996-08-23', 
  3, 
  3.67000008, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10265, 
  'BLONP', 
  2, 
  '1996-07-25', 
  '1996-08-22', 
  '1996-08-12', 
  1, 
  55.2799988, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10266, 
  'WARTH', 
  3, 
  '1996-07-26', 
  '1996-09-06', 
  '1996-07-31', 
  3, 
  25.7299995, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10267, 
  'FRANK', 
  4, 
  '1996-07-29', 
  '1996-08-26', 
  '1996-08-06', 
  1, 
  208.580002, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10268, 
  'GROSR', 
  8, 
  '1996-07-30', 
  '1996-08-27', 
  '1996-08-02', 
  3, 
  66.2900009, 
  'GROSELLA-Restaurante', 
  '5ª Ave. Los Palos Grandes', 
  'Caracas', 
  'DF', 
  '1081', 
  'Venezuela'
);
insert into northwind.orders
values (
  10269, 
  'WHITC', 
  5, 
  '1996-07-31', 
  '1996-08-14', 
  '1996-08-09', 
  1, 
  4.55999994, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10270, 
  'WARTH', 
  1, 
  '1996-08-01', 
  '1996-08-29', 
  '1996-08-02', 
  1, 
  136.539993, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10271, 
  'SPLIR', 
  6, 
  '1996-08-01', 
  '1996-08-29', 
  '1996-08-30', 
  2, 
  4.53999996, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10272, 
  'RATTC', 
  6, 
  '1996-08-02', 
  '1996-08-30', 
  '1996-08-06', 
  2, 
  98.0299988, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10273, 
  'QUICK', 
  3, 
  '1996-08-05', 
  '1996-09-02', 
  '1996-08-12', 
  3, 
  76.0699997, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10274, 
  'VINET', 
  6, 
  '1996-08-06', 
  '1996-09-03', 
  '1996-08-16', 
  1, 
  6.01000023, 
  'Vins et alcools Chevalier', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France'
);
insert into northwind.orders
values (
  10275, 
  'MAGAA', 
  1, 
  '1996-08-07', 
  '1996-09-04', 
  '1996-08-09', 
  1, 
  26.9300003, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10276, 
  'TORTU', 
  8, 
  '1996-08-08', 
  '1996-08-22', 
  '1996-08-14', 
  3, 
  13.8400002, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10277, 
  'MORGK', 
  2, 
  '1996-08-09', 
  '1996-09-06', 
  '1996-08-13', 
  3, 
  125.769997, 
  'Morgenstern Gesundkost', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany'
);
insert into northwind.orders
values (
  10278, 
  'BERGS', 
  8, 
  '1996-08-12', 
  '1996-09-09', 
  '1996-08-16', 
  2, 
  92.6900024, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10279, 
  'LEHMS', 
  8, 
  '1996-08-13', 
  '1996-09-10', 
  '1996-08-16', 
  2, 
  25.8299999, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10280, 
  'BERGS', 
  2, 
  '1996-08-14', 
  '1996-09-11', 
  '1996-09-12', 
  1, 
  8.97999954, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10281, 
  'ROMEY', 
  4, 
  '1996-08-14', 
  '1996-08-28', 
  '1996-08-21', 
  1, 
  2.94000006, 
  'Romero y tomillo', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain'
);
insert into northwind.orders
values (
  10282, 
  'ROMEY', 
  4, 
  '1996-08-15', 
  '1996-09-12', 
  '1996-08-21', 
  1, 
  12.6899996, 
  'Romero y tomillo', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain'
);
insert into northwind.orders
values (
  10283, 
  'LILAS', 
  3, 
  '1996-08-16', 
  '1996-09-13', 
  '1996-08-23', 
  3, 
  84.8099976, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10284, 
  'LEHMS', 
  4, 
  '1996-08-19', 
  '1996-09-16', 
  '1996-08-27', 
  1, 
  76.5599976, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10285, 
  'QUICK', 
  1, 
  '1996-08-20', 
  '1996-09-17', 
  '1996-08-26', 
  2, 
  76.8300018, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10286, 
  'QUICK', 
  8, 
  '1996-08-21', 
  '1996-09-18', 
  '1996-08-30', 
  3, 
  229.240005, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10287, 
  'RICAR', 
  8, 
  '1996-08-22', 
  '1996-09-19', 
  '1996-08-28', 
  3, 
  12.7600002, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10288, 
  'REGGC', 
  4, 
  '1996-08-23', 
  '1996-09-20', 
  '1996-09-03', 
  1, 
  7.44999981, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10289, 
  'BSBEV', 
  7, 
  '1996-08-26', 
  '1996-09-23', 
  '1996-08-28', 
  3, 
  22.7700005, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10290, 
  'COMMI', 
  8, 
  '1996-08-27', 
  '1996-09-24', 
  '1996-09-03', 
  1, 
  79.6999969, 
  'Comércio Mineiro', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil'
);
insert into northwind.orders
values (
  10291, 
  'QUEDE', 
  6, 
  '1996-08-27', 
  '1996-09-24', 
  '1996-09-04', 
  2, 
  6.4000001, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10292, 
  'TRADH', 
  1, 
  '1996-08-28', 
  '1996-09-25', 
  '1996-09-02', 
  2, 
  1.35000002, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10293, 
  'TORTU', 
  1, 
  '1996-08-29', 
  '1996-09-26', 
  '1996-09-11', 
  3, 
  21.1800003, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10294, 
  'RATTC', 
  4, 
  '1996-08-30', 
  '1996-09-27', 
  '1996-09-05', 
  2, 
  147.259995, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10295, 
  'VINET', 
  2, 
  '1996-09-02', 
  '1996-09-30', 
  '1996-09-10', 
  2, 
  1.14999998, 
  'Vins et alcools Chevalier', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France'
);
insert into northwind.orders
values (
  10296, 
  'LILAS', 
  6, 
  '1996-09-03', 
  '1996-10-01', 
  '1996-09-11', 
  1, 
  0.119999997, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10297, 
  'BLONP', 
  5, 
  '1996-09-04', 
  '1996-10-16', 
  '1996-09-10', 
  2, 
  5.73999977, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10298, 
  'HUNGO', 
  6, 
  '1996-09-05', 
  '1996-10-03', 
  '1996-09-11', 
  2, 
  168.220001, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10299, 
  'RICAR', 
  4, 
  '1996-09-06', 
  '1996-10-04', 
  '1996-09-13', 
  2, 
  29.7600002, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10300, 
  'MAGAA', 
  2, 
  '1996-09-09', 
  '1996-10-07', 
  '1996-09-18', 
  2, 
  17.6800003, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10301, 
  'WANDK', 
  8, 
  '1996-09-09', 
  '1996-10-07', 
  '1996-09-17', 
  2, 
  45.0800018, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10302, 
  'SUPRD', 
  4, 
  '1996-09-10', 
  '1996-10-08', 
  '1996-10-09', 
  2, 
  6.26999998, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10303, 
  'GODOS', 
  7, 
  '1996-09-11', 
  '1996-10-09', 
  '1996-09-18', 
  2, 
  107.830002, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10304, 
  'TORTU', 
  1, 
  '1996-09-12', 
  '1996-10-10', 
  '1996-09-17', 
  2, 
  63.7900009, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10305, 
  'OLDWO', 
  8, 
  '1996-09-13', 
  '1996-10-11', 
  '1996-10-09', 
  3, 
  257.619995, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10306, 
  'ROMEY', 
  1, 
  '1996-09-16', 
  '1996-10-14', 
  '1996-09-23', 
  3, 
  7.55999994, 
  'Romero y tomillo', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain'
);
insert into northwind.orders
values (
  10307, 
  'LONEP', 
  2, 
  '1996-09-17', 
  '1996-10-15', 
  '1996-09-25', 
  2, 
  0.560000002, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10308, 
  'ANATR', 
  7, 
  '1996-09-18', 
  '1996-10-16', 
  '1996-09-24', 
  3, 
  1.61000001, 
  'Ana Trujillo Emparedados y helados', 
  'Avda. de la Constitución 2222', 
  'México D.F.', 
  null, 
  '05021', 
  'Mexico'
);
insert into northwind.orders
values (
  10309, 
  'HUNGO', 
  3, 
  '1996-09-19', 
  '1996-10-17', 
  '1996-10-23', 
  1, 
  47.2999992, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10310, 
  'THEBI', 
  8, 
  '1996-09-20', 
  '1996-10-18', 
  '1996-09-27', 
  2, 
  17.5200005, 
  'The Big Cheese', 
  '89 Jefferson Way Suite 2', 
  'Portland', 
  'OR', 
  '97201', 
  'USA'
);
insert into northwind.orders
values (
  10311, 
  'DUMON', 
  1, 
  '1996-09-20', 
  '1996-10-04', 
  '1996-09-26', 
  3, 
  24.6900005, 
  'Du monde entier', 
  '67, rue des Cinquante Otages', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10312, 
  'WANDK', 
  2, 
  '1996-09-23', 
  '1996-10-21', 
  '1996-10-03', 
  2, 
  40.2599983, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10313, 
  'QUICK', 
  2, 
  '1996-09-24', 
  '1996-10-22', 
  '1996-10-04', 
  2, 
  1.96000004, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10314, 
  'RATTC', 
  1, 
  '1996-09-25', 
  '1996-10-23', 
  '1996-10-04', 
  2, 
  74.1600037, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10315, 
  'ISLAT', 
  4, 
  '1996-09-26', 
  '1996-10-24', 
  '1996-10-03', 
  2, 
  41.7599983, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10316, 
  'RATTC', 
  1, 
  '1996-09-27', 
  '1996-10-25', 
  '1996-10-08', 
  3, 
  150.149994, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10317, 
  'LONEP', 
  6, 
  '1996-09-30', 
  '1996-10-28', 
  '1996-10-10', 
  1, 
  12.6899996, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10318, 
  'ISLAT', 
  8, 
  '1996-10-01', 
  '1996-10-29', 
  '1996-10-04', 
  2, 
  4.73000002, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10319, 
  'TORTU', 
  7, 
  '1996-10-02', 
  '1996-10-30', 
  '1996-10-11', 
  3, 
  64.5, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10320, 
  'WARTH', 
  5, 
  '1996-10-03', 
  '1996-10-17', 
  '1996-10-18', 
  3, 
  34.5699997, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10321, 
  'ISLAT', 
  3, 
  '1996-10-03', 
  '1996-10-31', 
  '1996-10-11', 
  2, 
  3.43000007, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10322, 
  'PERIC', 
  7, 
  '1996-10-04', 
  '1996-11-01', 
  '1996-10-23', 
  3, 
  0.400000006, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10323, 
  'KOENE', 
  4, 
  '1996-10-07', 
  '1996-11-04', 
  '1996-10-14', 
  1, 
  4.88000011, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10324, 
  'SAVEA', 
  9, 
  '1996-10-08', 
  '1996-11-05', 
  '1996-10-10', 
  1, 
  214.270004, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10325, 
  'KOENE', 
  1, 
  '1996-10-09', 
  '1996-10-23', 
  '1996-10-14', 
  3, 
  64.8600006, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10326, 
  'BOLID', 
  4, 
  '1996-10-10', 
  '1996-11-07', 
  '1996-10-14', 
  2, 
  77.9199982, 
  'Bólido Comidas preparadas', 
  'C/ Araquil, 67', 
  'Madrid', 
  null, 
  '28023', 
  'Spain'
);
insert into northwind.orders
values (
  10327, 
  'FOLKO', 
  2, 
  '1996-10-11', 
  '1996-11-08', 
  '1996-10-14', 
  1, 
  63.3600006, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10328, 
  'FURIB', 
  4, 
  '1996-10-14', 
  '1996-11-11', 
  '1996-10-17', 
  3, 
  87.0299988, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10329, 
  'SPLIR', 
  4, 
  '1996-10-15', 
  '1996-11-26', 
  '1996-10-23', 
  2, 
  191.669998, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10330, 
  'LILAS', 
  3, 
  '1996-10-16', 
  '1996-11-13', 
  '1996-10-28', 
  1, 
  12.75, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10331, 
  'BONAP', 
  9, 
  '1996-10-16', 
  '1996-11-27', 
  '1996-10-21', 
  1, 
  10.1899996, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10332, 
  'MEREP', 
  3, 
  '1996-10-17', 
  '1996-11-28', 
  '1996-10-21', 
  2, 
  52.8400002, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10333, 
  'WARTH', 
  5, 
  '1996-10-18', 
  '1996-11-15', 
  '1996-10-25', 
  3, 
  0.589999974, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10334, 
  'VICTE', 
  8, 
  '1996-10-21', 
  '1996-11-18', 
  '1996-10-28', 
  2, 
  8.56000042, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10335, 
  'HUNGO', 
  7, 
  '1996-10-22', 
  '1996-11-19', 
  '1996-10-24', 
  2, 
  42.1100006, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10336, 
  'PRINI', 
  7, 
  '1996-10-23', 
  '1996-11-20', 
  '1996-10-25', 
  2, 
  15.5100002, 
  'Princesa Isabel Vinhos', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal'
);
insert into northwind.orders
values (
  10337, 
  'FRANK', 
  4, 
  '1996-10-24', 
  '1996-11-21', 
  '1996-10-29', 
  3, 
  108.260002, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10338, 
  'OLDWO', 
  4, 
  '1996-10-25', 
  '1996-11-22', 
  '1996-10-29', 
  3, 
  84.2099991, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10339, 
  'MEREP', 
  2, 
  '1996-10-28', 
  '1996-11-25', 
  '1996-11-04', 
  2, 
  15.6599998, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10340, 
  'BONAP', 
  1, 
  '1996-10-29', 
  '1996-11-26', 
  '1996-11-08', 
  3, 
  166.309998, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10341, 
  'SIMOB', 
  7, 
  '1996-10-29', 
  '1996-11-26', 
  '1996-11-05', 
  3, 
  26.7800007, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10342, 
  'FRANK', 
  4, 
  '1996-10-30', 
  '1996-11-13', 
  '1996-11-04', 
  2, 
  54.8300018, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10343, 
  'LEHMS', 
  4, 
  '1996-10-31', 
  '1996-11-28', 
  '1996-11-06', 
  1, 
  110.370003, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10344, 
  'WHITC', 
  4, 
  '1996-11-01', 
  '1996-11-29', 
  '1996-11-05', 
  2, 
  23.2900009, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10345, 
  'QUICK', 
  2, 
  '1996-11-04', 
  '1996-12-02', 
  '1996-11-11', 
  2, 
  249.059998, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10346, 
  'RATTC', 
  3, 
  '1996-11-05', 
  '1996-12-17', 
  '1996-11-08', 
  3, 
  142.080002, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10347, 
  'FAMIA', 
  4, 
  '1996-11-06', 
  '1996-12-04', 
  '1996-11-08', 
  3, 
  3.0999999, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10348, 
  'WANDK', 
  4, 
  '1996-11-07', 
  '1996-12-05', 
  '1996-11-15', 
  2, 
  0.779999971, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10349, 
  'SPLIR', 
  7, 
  '1996-11-08', 
  '1996-12-06', 
  '1996-11-15', 
  1, 
  8.63000011, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10350, 
  'LAMAI', 
  6, 
  '1996-11-11', 
  '1996-12-09', 
  '1996-12-03', 
  2, 
  64.1900024, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10351, 
  'ERNSH', 
  1, 
  '1996-11-11', 
  '1996-12-09', 
  '1996-11-20', 
  1, 
  162.330002, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10352, 
  'FURIB', 
  3, 
  '1996-11-12', 
  '1996-11-26', 
  '1996-11-18', 
  3, 
  1.29999995, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10353, 
  'PICCO', 
  7, 
  '1996-11-13', 
  '1996-12-11', 
  '1996-11-25', 
  3, 
  360.630005, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10354, 
  'PERIC', 
  8, 
  '1996-11-14', 
  '1996-12-12', 
  '1996-11-20', 
  3, 
  53.7999992, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10355, 
  'AROUT', 
  6, 
  '1996-11-15', 
  '1996-12-13', 
  '1996-11-20', 
  1, 
  41.9500008, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10356, 
  'WANDK', 
  6, 
  '1996-11-18', 
  '1996-12-16', 
  '1996-11-27', 
  2, 
  36.7099991, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10357, 
  'LILAS', 
  1, 
  '1996-11-19', 
  '1996-12-17', 
  '1996-12-02', 
  3, 
  34.8800011, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10358, 
  'LAMAI', 
  5, 
  '1996-11-20', 
  '1996-12-18', 
  '1996-11-27', 
  1, 
  19.6399994, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10359, 
  'SEVES', 
  5, 
  '1996-11-21', 
  '1996-12-19', 
  '1996-11-26', 
  3, 
  288.429993, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10360, 
  'BLONP', 
  4, 
  '1996-11-22', 
  '1996-12-20', 
  '1996-12-02', 
  3, 
  131.699997, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10361, 
  'QUICK', 
  1, 
  '1996-11-22', 
  '1996-12-20', 
  '1996-12-03', 
  2, 
  183.169998, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10362, 
  'BONAP', 
  3, 
  '1996-11-25', 
  '1996-12-23', 
  '1996-11-28', 
  1, 
  96.0400009, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10363, 
  'DRACD', 
  4, 
  '1996-11-26', 
  '1996-12-24', 
  '1996-12-04', 
  3, 
  30.5400009, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  10364, 
  'EASTC', 
  1, 
  '1996-11-26', 
  '1997-01-07', 
  '1996-12-04', 
  1, 
  71.9700012, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  10365, 
  'ANTON', 
  3, 
  '1996-11-27', 
  '1996-12-25', 
  '1996-12-02', 
  2, 
  22, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10366, 
  'GALED', 
  8, 
  '1996-11-28', 
  '1997-01-09', 
  '1996-12-30', 
  2, 
  10.1400003, 
  'Galería del gastronómo', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '8022', 
  'Spain'
);
insert into northwind.orders
values (
  10367, 
  'VAFFE', 
  7, 
  '1996-11-28', 
  '1996-12-26', 
  '1996-12-02', 
  3, 
  13.5500002, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10368, 
  'ERNSH', 
  2, 
  '1996-11-29', 
  '1996-12-27', 
  '1996-12-02', 
  2, 
  101.949997, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10369, 
  'SPLIR', 
  8, 
  '1996-12-02', 
  '1996-12-30', 
  '1996-12-09', 
  2, 
  195.679993, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10370, 
  'CHOPS', 
  6, 
  '1996-12-03', 
  '1996-12-31', 
  '1996-12-27', 
  2, 
  1.16999996, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10371, 
  'LAMAI', 
  1, 
  '1996-12-03', 
  '1996-12-31', 
  '1996-12-24', 
  1, 
  0.449999988, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10372, 
  'QUEEN', 
  5, 
  '1996-12-04', 
  '1997-01-01', 
  '1996-12-09', 
  2, 
  890.780029, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10373, 
  'HUNGO', 
  4, 
  '1996-12-05', 
  '1997-01-02', 
  '1996-12-11', 
  3, 
  124.120003, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10374, 
  'WOLZA', 
  1, 
  '1996-12-05', 
  '1997-01-02', 
  '1996-12-09', 
  3, 
  3.94000006, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10375, 
  'HUNGC', 
  3, 
  '1996-12-06', 
  '1997-01-03', 
  '1996-12-09', 
  2, 
  20.1200008, 
  'Hungry Coyote Import Store', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA'
);
insert into northwind.orders
values (
  10376, 
  'MEREP', 
  1, 
  '1996-12-09', 
  '1997-01-06', 
  '1996-12-13', 
  2, 
  20.3899994, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10377, 
  'SEVES', 
  1, 
  '1996-12-09', 
  '1997-01-06', 
  '1996-12-13', 
  3, 
  22.2099991, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10378, 
  'FOLKO', 
  5, 
  '1996-12-10', 
  '1997-01-07', 
  '1996-12-19', 
  3, 
  5.44000006, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10379, 
  'QUEDE', 
  2, 
  '1996-12-11', 
  '1997-01-08', 
  '1996-12-13', 
  1, 
  45.0299988, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10380, 
  'HUNGO', 
  8, 
  '1996-12-12', 
  '1997-01-09', 
  '1997-01-16', 
  3, 
  35.0299988, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10381, 
  'LILAS', 
  3, 
  '1996-12-12', 
  '1997-01-09', 
  '1996-12-13', 
  3, 
  7.98999977, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10382, 
  'ERNSH', 
  4, 
  '1996-12-13', 
  '1997-01-10', 
  '1996-12-16', 
  1, 
  94.7699966, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10383, 
  'AROUT', 
  8, 
  '1996-12-16', 
  '1997-01-13', 
  '1996-12-18', 
  3, 
  34.2400017, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10384, 
  'BERGS', 
  3, 
  '1996-12-16', 
  '1997-01-13', 
  '1996-12-20', 
  3, 
  168.639999, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10385, 
  'SPLIR', 
  1, 
  '1996-12-17', 
  '1997-01-14', 
  '1996-12-23', 
  2, 
  30.9599991, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10386, 
  'FAMIA', 
  9, 
  '1996-12-18', 
  '1997-01-01', 
  '1996-12-25', 
  3, 
  13.9899998, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10387, 
  'SANTG', 
  1, 
  '1996-12-18', 
  '1997-01-15', 
  '1996-12-20', 
  2, 
  93.6299973, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  10388, 
  'SEVES', 
  2, 
  '1996-12-19', 
  '1997-01-16', 
  '1996-12-20', 
  1, 
  34.8600006, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10389, 
  'BOTTM', 
  4, 
  '1996-12-20', 
  '1997-01-17', 
  '1996-12-24', 
  2, 
  47.4199982, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10390, 
  'ERNSH', 
  6, 
  '1996-12-23', 
  '1997-01-20', 
  '1996-12-26', 
  1, 
  126.379997, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10391, 
  'DRACD', 
  3, 
  '1996-12-23', 
  '1997-01-20', 
  '1996-12-31', 
  3, 
  5.44999981, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  10392, 
  'PICCO', 
  2, 
  '1996-12-24', 
  '1997-01-21', 
  '1997-01-01', 
  3, 
  122.459999, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10393, 
  'SAVEA', 
  1, 
  '1996-12-25', 
  '1997-01-22', 
  '1997-01-03', 
  3, 
  126.559998, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10394, 
  'HUNGC', 
  1, 
  '1996-12-25', 
  '1997-01-22', 
  '1997-01-03', 
  3, 
  30.3400002, 
  'Hungry Coyote Import Store', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA'
);
insert into northwind.orders
values (
  10395, 
  'HILAA', 
  6, 
  '1996-12-26', 
  '1997-01-23', 
  '1997-01-03', 
  1, 
  184.410004, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10396, 
  'FRANK', 
  1, 
  '1996-12-27', 
  '1997-01-10', 
  '1997-01-06', 
  3, 
  135.350006, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10397, 
  'PRINI', 
  5, 
  '1996-12-27', 
  '1997-01-24', 
  '1997-01-02', 
  1, 
  60.2599983, 
  'Princesa Isabel Vinhos', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal'
);
insert into northwind.orders
values (
  10398, 
  'SAVEA', 
  2, 
  '1996-12-30', 
  '1997-01-27', 
  '1997-01-09', 
  3, 
  89.1600037, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10399, 
  'VAFFE', 
  8, 
  '1996-12-31', 
  '1997-01-14', 
  '1997-01-08', 
  3, 
  27.3600006, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10400, 
  'EASTC', 
  1, 
  '1997-01-01', 
  '1997-01-29', 
  '1997-01-16', 
  3, 
  83.9300003, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  10401, 
  'RATTC', 
  1, 
  '1997-01-01', 
  '1997-01-29', 
  '1997-01-10', 
  1, 
  12.5100002, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10402, 
  'ERNSH', 
  8, 
  '1997-01-02', 
  '1997-02-13', 
  '1997-01-10', 
  2, 
  67.8799973, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10403, 
  'ERNSH', 
  4, 
  '1997-01-03', 
  '1997-01-31', 
  '1997-01-09', 
  3, 
  73.7900009, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10404, 
  'MAGAA', 
  2, 
  '1997-01-03', 
  '1997-01-31', 
  '1997-01-08', 
  1, 
  155.970001, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10405, 
  'LINOD', 
  1, 
  '1997-01-06', 
  '1997-02-03', 
  '1997-01-22', 
  1, 
  34.8199997, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10406, 
  'QUEEN', 
  7, 
  '1997-01-07', 
  '1997-02-18', 
  '1997-01-13', 
  1, 
  108.040001, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10407, 
  'OTTIK', 
  2, 
  '1997-01-07', 
  '1997-02-04', 
  '1997-01-30', 
  2, 
  91.4800034, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10408, 
  'FOLIG', 
  8, 
  '1997-01-08', 
  '1997-02-05', 
  '1997-01-14', 
  1, 
  11.2600002, 
  'Folies gourmandes', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France'
);
insert into northwind.orders
values (
  10409, 
  'OCEAN', 
  3, 
  '1997-01-09', 
  '1997-02-06', 
  '1997-01-14', 
  1, 
  29.8299999, 
  'Océano Atlántico Ltda.', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10410, 
  'BOTTM', 
  3, 
  '1997-01-10', 
  '1997-02-07', 
  '1997-01-15', 
  3, 
  2.4000001, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10411, 
  'BOTTM', 
  9, 
  '1997-01-10', 
  '1997-02-07', 
  '1997-01-21', 
  3, 
  23.6499996, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10412, 
  'WARTH', 
  8, 
  '1997-01-13', 
  '1997-02-10', 
  '1997-01-15', 
  2, 
  3.76999998, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10413, 
  'LAMAI', 
  3, 
  '1997-01-14', 
  '1997-02-11', 
  '1997-01-16', 
  2, 
  95.6600037, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10414, 
  'FAMIA', 
  2, 
  '1997-01-14', 
  '1997-02-11', 
  '1997-01-17', 
  3, 
  21.4799995, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10415, 
  'HUNGC', 
  3, 
  '1997-01-15', 
  '1997-02-12', 
  '1997-01-24', 
  1, 
  0.200000003, 
  'Hungry Coyote Import Store', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA'
);
insert into northwind.orders
values (
  10416, 
  'WARTH', 
  8, 
  '1997-01-16', 
  '1997-02-13', 
  '1997-01-27', 
  3, 
  22.7199993, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10417, 
  'SIMOB', 
  4, 
  '1997-01-16', 
  '1997-02-13', 
  '1997-01-28', 
  3, 
  70.2900009, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10418, 
  'QUICK', 
  4, 
  '1997-01-17', 
  '1997-02-14', 
  '1997-01-24', 
  1, 
  17.5499992, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10419, 
  'RICSU', 
  4, 
  '1997-01-20', 
  '1997-02-17', 
  '1997-01-30', 
  2, 
  137.350006, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10420, 
  'WELLI', 
  3, 
  '1997-01-21', 
  '1997-02-18', 
  '1997-01-27', 
  1, 
  44.1199989, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10421, 
  'QUEDE', 
  8, 
  '1997-01-21', 
  '1997-03-04', 
  '1997-01-27', 
  1, 
  99.2300034, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10422, 
  'FRANS', 
  2, 
  '1997-01-22', 
  '1997-02-19', 
  '1997-01-31', 
  1, 
  3.01999998, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  10423, 
  'GOURL', 
  6, 
  '1997-01-23', 
  '1997-02-06', 
  '1997-02-24', 
  3, 
  24.5, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10424, 
  'MEREP', 
  7, 
  '1997-01-23', 
  '1997-02-20', 
  '1997-01-27', 
  2, 
  370.609985, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10425, 
  'LAMAI', 
  6, 
  '1997-01-24', 
  '1997-02-21', 
  '1997-02-14', 
  2, 
  7.92999983, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10426, 
  'GALED', 
  4, 
  '1997-01-27', 
  '1997-02-24', 
  '1997-02-06', 
  1, 
  18.6900005, 
  'Galería del gastronómo', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '8022', 
  'Spain'
);
insert into northwind.orders
values (
  10427, 
  'PICCO', 
  4, 
  '1997-01-27', 
  '1997-02-24', 
  '1997-03-03', 
  2, 
  31.2900009, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10428, 
  'REGGC', 
  7, 
  '1997-01-28', 
  '1997-02-25', 
  '1997-02-04', 
  1, 
  11.0900002, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10429, 
  'HUNGO', 
  3, 
  '1997-01-29', 
  '1997-03-12', 
  '1997-02-07', 
  2, 
  56.6300011, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10430, 
  'ERNSH', 
  4, 
  '1997-01-30', 
  '1997-02-13', 
  '1997-02-03', 
  1, 
  458.779999, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10431, 
  'BOTTM', 
  4, 
  '1997-01-30', 
  '1997-02-13', 
  '1997-02-07', 
  2, 
  44.1699982, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10432, 
  'SPLIR', 
  3, 
  '1997-01-31', 
  '1997-02-14', 
  '1997-02-07', 
  2, 
  4.34000015, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10433, 
  'PRINI', 
  3, 
  '1997-02-03', 
  '1997-03-03', 
  '1997-03-04', 
  3, 
  73.8300018, 
  'Princesa Isabel Vinhos', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal'
);
insert into northwind.orders
values (
  10434, 
  'FOLKO', 
  3, 
  '1997-02-03', 
  '1997-03-03', 
  '1997-02-13', 
  2, 
  17.9200001, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10435, 
  'CONSH', 
  8, 
  '1997-02-04', 
  '1997-03-18', 
  '1997-02-07', 
  2, 
  9.21000004, 
  'Consolidated Holdings', 
  'Berkeley Gardens 12  Brewery', 
  'London', 
  null, 
  'WX1 6LT', 
  'UK'
);
insert into northwind.orders
values (
  10436, 
  'BLONP', 
  3, 
  '1997-02-05', 
  '1997-03-05', 
  '1997-02-11', 
  2, 
  156.660004, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10437, 
  'WARTH', 
  8, 
  '1997-02-05', 
  '1997-03-05', 
  '1997-02-12', 
  1, 
  19.9699993, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10438, 
  'TOMSP', 
  3, 
  '1997-02-06', 
  '1997-03-06', 
  '1997-02-14', 
  2, 
  8.23999977, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10439, 
  'MEREP', 
  6, 
  '1997-02-07', 
  '1997-03-07', 
  '1997-02-10', 
  3, 
  4.07000017, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10440, 
  'SAVEA', 
  4, 
  '1997-02-10', 
  '1997-03-10', 
  '1997-02-28', 
  2, 
  86.5299988, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10441, 
  'OLDWO', 
  3, 
  '1997-02-10', 
  '1997-03-24', 
  '1997-03-14', 
  2, 
  73.0199966, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10442, 
  'ERNSH', 
  3, 
  '1997-02-11', 
  '1997-03-11', 
  '1997-02-18', 
  2, 
  47.9399986, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10443, 
  'REGGC', 
  8, 
  '1997-02-12', 
  '1997-03-12', 
  '1997-02-14', 
  1, 
  13.9499998, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10444, 
  'BERGS', 
  3, 
  '1997-02-12', 
  '1997-03-12', 
  '1997-02-21', 
  3, 
  3.5, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10445, 
  'BERGS', 
  3, 
  '1997-02-13', 
  '1997-03-13', 
  '1997-02-20', 
  1, 
  9.30000019, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10446, 
  'TOMSP', 
  6, 
  '1997-02-14', 
  '1997-03-14', 
  '1997-02-19', 
  1, 
  14.6800003, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10447, 
  'RICAR', 
  4, 
  '1997-02-14', 
  '1997-03-14', 
  '1997-03-07', 
  2, 
  68.6600037, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10448, 
  'RANCH', 
  4, 
  '1997-02-17', 
  '1997-03-17', 
  '1997-02-24', 
  2, 
  38.8199997, 
  'Rancho grande', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10449, 
  'BLONP', 
  3, 
  '1997-02-18', 
  '1997-03-18', 
  '1997-02-27', 
  2, 
  53.2999992, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10450, 
  'VICTE', 
  8, 
  '1997-02-19', 
  '1997-03-19', 
  '1997-03-11', 
  2, 
  7.23000002, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10451, 
  'QUICK', 
  4, 
  '1997-02-19', 
  '1997-03-05', 
  '1997-03-12', 
  3, 
  189.089996, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10452, 
  'SAVEA', 
  8, 
  '1997-02-20', 
  '1997-03-20', 
  '1997-02-26', 
  1, 
  140.259995, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10453, 
  'AROUT', 
  1, 
  '1997-02-21', 
  '1997-03-21', 
  '1997-02-26', 
  2, 
  25.3600006, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10454, 
  'LAMAI', 
  4, 
  '1997-02-21', 
  '1997-03-21', 
  '1997-02-25', 
  3, 
  2.74000001, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10455, 
  'WARTH', 
  8, 
  '1997-02-24', 
  '1997-04-07', 
  '1997-03-03', 
  2, 
  180.449997, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10456, 
  'KOENE', 
  8, 
  '1997-02-25', 
  '1997-04-08', 
  '1997-02-28', 
  2, 
  8.11999989, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10457, 
  'KOENE', 
  2, 
  '1997-02-25', 
  '1997-03-25', 
  '1997-03-03', 
  1, 
  11.5699997, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10458, 
  'SUPRD', 
  7, 
  '1997-02-26', 
  '1997-03-26', 
  '1997-03-04', 
  3, 
  147.059998, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10459, 
  'VICTE', 
  4, 
  '1997-02-27', 
  '1997-03-27', 
  '1997-02-28', 
  2, 
  25.0900002, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10460, 
  'FOLKO', 
  8, 
  '1997-02-28', 
  '1997-03-28', 
  '1997-03-03', 
  1, 
  16.2700005, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10461, 
  'LILAS', 
  1, 
  '1997-02-28', 
  '1997-03-28', 
  '1997-03-05', 
  3, 
  148.610001, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10462, 
  'CONSH', 
  2, 
  '1997-03-03', 
  '1997-03-31', 
  '1997-03-18', 
  1, 
  6.17000008, 
  'Consolidated Holdings', 
  'Berkeley Gardens 12  Brewery', 
  'London', 
  null, 
  'WX1 6LT', 
  'UK'
);
insert into northwind.orders
values (
  10463, 
  'SUPRD', 
  5, 
  '1997-03-04', 
  '1997-04-01', 
  '1997-03-06', 
  3, 
  14.7799997, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10464, 
  'FURIB', 
  4, 
  '1997-03-04', 
  '1997-04-01', 
  '1997-03-14', 
  2, 
  89, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10465, 
  'VAFFE', 
  1, 
  '1997-03-05', 
  '1997-04-02', 
  '1997-03-14', 
  3, 
  145.039993, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10466, 
  'COMMI', 
  4, 
  '1997-03-06', 
  '1997-04-03', 
  '1997-03-13', 
  1, 
  11.9300003, 
  'Comércio Mineiro', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil'
);
insert into northwind.orders
values (
  10467, 
  'MAGAA', 
  8, 
  '1997-03-06', 
  '1997-04-03', 
  '1997-03-11', 
  2, 
  4.92999983, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10468, 
  'KOENE', 
  3, 
  '1997-03-07', 
  '1997-04-04', 
  '1997-03-12', 
  3, 
  44.1199989, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10469, 
  'WHITC', 
  1, 
  '1997-03-10', 
  '1997-04-07', 
  '1997-03-14', 
  1, 
  60.1800003, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10470, 
  'BONAP', 
  4, 
  '1997-03-11', 
  '1997-04-08', 
  '1997-03-14', 
  2, 
  64.5599976, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10471, 
  'BSBEV', 
  2, 
  '1997-03-11', 
  '1997-04-08', 
  '1997-03-18', 
  3, 
  45.5900002, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10472, 
  'SEVES', 
  8, 
  '1997-03-12', 
  '1997-04-09', 
  '1997-03-19', 
  1, 
  4.19999981, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10473, 
  'ISLAT', 
  1, 
  '1997-03-13', 
  '1997-03-27', 
  '1997-03-21', 
  3, 
  16.3700008, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10474, 
  'PERIC', 
  5, 
  '1997-03-13', 
  '1997-04-10', 
  '1997-03-21', 
  2, 
  83.4899979, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10475, 
  'SUPRD', 
  9, 
  '1997-03-14', 
  '1997-04-11', 
  '1997-04-04', 
  1, 
  68.5199966, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10476, 
  'HILAA', 
  8, 
  '1997-03-17', 
  '1997-04-14', 
  '1997-03-24', 
  3, 
  4.40999985, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10477, 
  'PRINI', 
  5, 
  '1997-03-17', 
  '1997-04-14', 
  '1997-03-25', 
  2, 
  13.0200005, 
  'Princesa Isabel Vinhos', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal'
);
insert into northwind.orders
values (
  10478, 
  'VICTE', 
  2, 
  '1997-03-18', 
  '1997-04-01', 
  '1997-03-26', 
  3, 
  4.80999994, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10479, 
  'RATTC', 
  3, 
  '1997-03-19', 
  '1997-04-16', 
  '1997-03-21', 
  3, 
  708.950012, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10480, 
  'FOLIG', 
  6, 
  '1997-03-20', 
  '1997-04-17', 
  '1997-03-24', 
  2, 
  1.35000002, 
  'Folies gourmandes', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France'
);
insert into northwind.orders
values (
  10481, 
  'RICAR', 
  8, 
  '1997-03-20', 
  '1997-04-17', 
  '1997-03-25', 
  2, 
  64.3300018, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10482, 
  'LAZYK', 
  1, 
  '1997-03-21', 
  '1997-04-18', 
  '1997-04-10', 
  3, 
  7.48000002, 
  'Lazy K Kountry Store', 
  '12 Orchestra Terrace', 
  'Walla Walla', 
  'WA', 
  '99362', 
  'USA'
);
insert into northwind.orders
values (
  10483, 
  'WHITC', 
  7, 
  '1997-03-24', 
  '1997-04-21', 
  '1997-04-25', 
  2, 
  15.2799997, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10484, 
  'BSBEV', 
  3, 
  '1997-03-24', 
  '1997-04-21', 
  '1997-04-01', 
  3, 
  6.88000011, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10485, 
  'LINOD', 
  4, 
  '1997-03-25', 
  '1997-04-08', 
  '1997-03-31', 
  2, 
  64.4499969, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10486, 
  'HILAA', 
  1, 
  '1997-03-26', 
  '1997-04-23', 
  '1997-04-02', 
  2, 
  30.5300007, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10487, 
  'QUEEN', 
  2, 
  '1997-03-26', 
  '1997-04-23', 
  '1997-03-28', 
  2, 
  71.0699997, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10488, 
  'FRANK', 
  8, 
  '1997-03-27', 
  '1997-04-24', 
  '1997-04-02', 
  2, 
  4.92999983, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10489, 
  'PICCO', 
  6, 
  '1997-03-28', 
  '1997-04-25', 
  '1997-04-09', 
  2, 
  5.28999996, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10490, 
  'HILAA', 
  7, 
  '1997-03-31', 
  '1997-04-28', 
  '1997-04-03', 
  2, 
  210.190002, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10491, 
  'FURIB', 
  8, 
  '1997-03-31', 
  '1997-04-28', 
  '1997-04-08', 
  3, 
  16.9599991, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10492, 
  'BOTTM', 
  3, 
  '1997-04-01', 
  '1997-04-29', 
  '1997-04-11', 
  1, 
  62.8899994, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10493, 
  'LAMAI', 
  4, 
  '1997-04-02', 
  '1997-04-30', 
  '1997-04-10', 
  3, 
  10.6400003, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10494, 
  'COMMI', 
  4, 
  '1997-04-02', 
  '1997-04-30', 
  '1997-04-09', 
  2, 
  65.9899979, 
  'Comércio Mineiro', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil'
);
insert into northwind.orders
values (
  10495, 
  'LAUGB', 
  3, 
  '1997-04-03', 
  '1997-05-01', 
  '1997-04-11', 
  3, 
  4.6500001, 
  'Laughing Bacchus Wine Cellars', 
  '2319 Elm St.', 
  'Vancouver', 
  'BC', 
  'V3F 2K1', 
  'Canada'
);
insert into northwind.orders
values (
  10496, 
  'TRADH', 
  7, 
  '1997-04-04', 
  '1997-05-02', 
  '1997-04-07', 
  2, 
  46.7700005, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10497, 
  'LEHMS', 
  7, 
  '1997-04-04', 
  '1997-05-02', 
  '1997-04-07', 
  1, 
  36.2099991, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10498, 
  'HILAA', 
  8, 
  '1997-04-07', 
  '1997-05-05', 
  '1997-04-11', 
  2, 
  29.75, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10499, 
  'LILAS', 
  4, 
  '1997-04-08', 
  '1997-05-06', 
  '1997-04-16', 
  2, 
  102.019997, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10500, 
  'LAMAI', 
  6, 
  '1997-04-09', 
  '1997-05-07', 
  '1997-04-17', 
  1, 
  42.6800003, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10501, 
  'BLAUS', 
  9, 
  '1997-04-09', 
  '1997-05-07', 
  '1997-04-16', 
  3, 
  8.85000038, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10502, 
  'PERIC', 
  2, 
  '1997-04-10', 
  '1997-05-08', 
  '1997-04-29', 
  1, 
  69.3199997, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10503, 
  'HUNGO', 
  6, 
  '1997-04-11', 
  '1997-05-09', 
  '1997-04-16', 
  2, 
  16.7399998, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10504, 
  'WHITC', 
  4, 
  '1997-04-11', 
  '1997-05-09', 
  '1997-04-18', 
  3, 
  59.1300011, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10505, 
  'MEREP', 
  3, 
  '1997-04-14', 
  '1997-05-12', 
  '1997-04-21', 
  3, 
  7.13000011, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10506, 
  'KOENE', 
  9, 
  '1997-04-15', 
  '1997-05-13', 
  '1997-05-02', 
  2, 
  21.1900005, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10507, 
  'ANTON', 
  7, 
  '1997-04-15', 
  '1997-05-13', 
  '1997-04-22', 
  1, 
  47.4500008, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10508, 
  'OTTIK', 
  1, 
  '1997-04-16', 
  '1997-05-14', 
  '1997-05-13', 
  2, 
  4.98999977, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10509, 
  'BLAUS', 
  4, 
  '1997-04-17', 
  '1997-05-15', 
  '1997-04-29', 
  1, 
  0.150000006, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10510, 
  'SAVEA', 
  6, 
  '1997-04-18', 
  '1997-05-16', 
  '1997-04-28', 
  3, 
  367.630005, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10511, 
  'BONAP', 
  4, 
  '1997-04-18', 
  '1997-05-16', 
  '1997-04-21', 
  3, 
  350.640015, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10512, 
  'FAMIA', 
  7, 
  '1997-04-21', 
  '1997-05-19', 
  '1997-04-24', 
  2, 
  3.52999997, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10513, 
  'WANDK', 
  7, 
  '1997-04-22', 
  '1997-06-03', 
  '1997-04-28', 
  1, 
  105.650002, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10514, 
  'ERNSH', 
  3, 
  '1997-04-22', 
  '1997-05-20', 
  '1997-05-16', 
  2, 
  789.950012, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10515, 
  'QUICK', 
  2, 
  '1997-04-23', 
  '1997-05-07', 
  '1997-05-23', 
  1, 
  204.470001, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10516, 
  'HUNGO', 
  2, 
  '1997-04-24', 
  '1997-05-22', 
  '1997-05-01', 
  3, 
  62.7799988, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10517, 
  'NORTS', 
  3, 
  '1997-04-24', 
  '1997-05-22', 
  '1997-04-29', 
  3, 
  32.0699997, 
  'North/South', 
  'South House 300 Queensbridge', 
  'London', 
  null, 
  'SW7 1RZ', 
  'UK'
);
insert into northwind.orders
values (
  10518, 
  'TORTU', 
  4, 
  '1997-04-25', 
  '1997-05-09', 
  '1997-05-05', 
  2, 
  218.149994, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10519, 
  'CHOPS', 
  6, 
  '1997-04-28', 
  '1997-05-26', 
  '1997-05-01', 
  3, 
  91.7600021, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10520, 
  'SANTG', 
  7, 
  '1997-04-29', 
  '1997-05-27', 
  '1997-05-01', 
  1, 
  13.3699999, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  10521, 
  'CACTU', 
  8, 
  '1997-04-29', 
  '1997-05-27', 
  '1997-05-02', 
  2, 
  17.2199993, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10522, 
  'LEHMS', 
  4, 
  '1997-04-30', 
  '1997-05-28', 
  '1997-05-06', 
  1, 
  45.3300018, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10523, 
  'SEVES', 
  7, 
  '1997-05-01', 
  '1997-05-29', 
  '1997-05-30', 
  2, 
  77.6299973, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10524, 
  'BERGS', 
  1, 
  '1997-05-01', 
  '1997-05-29', 
  '1997-05-07', 
  2, 
  244.789993, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10525, 
  'BONAP', 
  1, 
  '1997-05-02', 
  '1997-05-30', 
  '1997-05-23', 
  2, 
  11.0600004, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10526, 
  'WARTH', 
  4, 
  '1997-05-05', 
  '1997-06-02', 
  '1997-05-15', 
  2, 
  58.5900002, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10527, 
  'QUICK', 
  7, 
  '1997-05-05', 
  '1997-06-02', 
  '1997-05-07', 
  1, 
  41.9000015, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10528, 
  'GREAL', 
  6, 
  '1997-05-06', 
  '1997-05-20', 
  '1997-05-09', 
  2, 
  3.3499999, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10529, 
  'MAISD', 
  5, 
  '1997-05-07', 
  '1997-06-04', 
  '1997-05-09', 
  2, 
  66.6900024, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10530, 
  'PICCO', 
  3, 
  '1997-05-08', 
  '1997-06-05', 
  '1997-05-12', 
  2, 
  339.220001, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10531, 
  'OCEAN', 
  7, 
  '1997-05-08', 
  '1997-06-05', 
  '1997-05-19', 
  1, 
  8.11999989, 
  'Océano Atlántico Ltda.', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10532, 
  'EASTC', 
  7, 
  '1997-05-09', 
  '1997-06-06', 
  '1997-05-12', 
  3, 
  74.4599991, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  10533, 
  'FOLKO', 
  8, 
  '1997-05-12', 
  '1997-06-09', 
  '1997-05-22', 
  1, 
  188.039993, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10534, 
  'LEHMS', 
  8, 
  '1997-05-12', 
  '1997-06-09', 
  '1997-05-14', 
  2, 
  27.9400005, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10535, 
  'ANTON', 
  4, 
  '1997-05-13', 
  '1997-06-10', 
  '1997-05-21', 
  1, 
  15.6400003, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10536, 
  'LEHMS', 
  3, 
  '1997-05-14', 
  '1997-06-11', 
  '1997-06-06', 
  2, 
  58.8800011, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10537, 
  'RICSU', 
  1, 
  '1997-05-14', 
  '1997-05-28', 
  '1997-05-19', 
  1, 
  78.8499985, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10538, 
  'BSBEV', 
  9, 
  '1997-05-15', 
  '1997-06-12', 
  '1997-05-16', 
  3, 
  4.86999989, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10539, 
  'BSBEV', 
  6, 
  '1997-05-16', 
  '1997-06-13', 
  '1997-05-23', 
  3, 
  12.3599997, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10540, 
  'QUICK', 
  3, 
  '1997-05-19', 
  '1997-06-16', 
  '1997-06-13', 
  3, 
  1007.64001, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10541, 
  'HANAR', 
  2, 
  '1997-05-19', 
  '1997-06-16', 
  '1997-05-29', 
  1, 
  68.6500015, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10542, 
  'KOENE', 
  1, 
  '1997-05-20', 
  '1997-06-17', 
  '1997-05-26', 
  3, 
  10.9499998, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10543, 
  'LILAS', 
  8, 
  '1997-05-21', 
  '1997-06-18', 
  '1997-05-23', 
  2, 
  48.1699982, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10544, 
  'LONEP', 
  4, 
  '1997-05-21', 
  '1997-06-18', 
  '1997-05-30', 
  1, 
  24.9099998, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10545, 
  'LAZYK', 
  8, 
  '1997-05-22', 
  '1997-06-19', 
  '1997-06-26', 
  2, 
  11.9200001, 
  'Lazy K Kountry Store', 
  '12 Orchestra Terrace', 
  'Walla Walla', 
  'WA', 
  '99362', 
  'USA'
);
insert into northwind.orders
values (
  10546, 
  'VICTE', 
  1, 
  '1997-05-23', 
  '1997-06-20', 
  '1997-05-27', 
  3, 
  194.720001, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10547, 
  'SEVES', 
  3, 
  '1997-05-23', 
  '1997-06-20', 
  '1997-06-02', 
  2, 
  178.429993, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10548, 
  'TOMSP', 
  3, 
  '1997-05-26', 
  '1997-06-23', 
  '1997-06-02', 
  2, 
  1.42999995, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10549, 
  'QUICK', 
  5, 
  '1997-05-27', 
  '1997-06-10', 
  '1997-05-30', 
  1, 
  171.240005, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10550, 
  'GODOS', 
  7, 
  '1997-05-28', 
  '1997-06-25', 
  '1997-06-06', 
  3, 
  4.32000017, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10551, 
  'FURIB', 
  4, 
  '1997-05-28', 
  '1997-07-09', 
  '1997-06-06', 
  3, 
  72.9499969, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10552, 
  'HILAA', 
  2, 
  '1997-05-29', 
  '1997-06-26', 
  '1997-06-05', 
  1, 
  83.2200012, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10553, 
  'WARTH', 
  2, 
  '1997-05-30', 
  '1997-06-27', 
  '1997-06-03', 
  2, 
  149.490005, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10554, 
  'OTTIK', 
  4, 
  '1997-05-30', 
  '1997-06-27', 
  '1997-06-05', 
  3, 
  120.970001, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10555, 
  'SAVEA', 
  6, 
  '1997-06-02', 
  '1997-06-30', 
  '1997-06-04', 
  3, 
  252.490005, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10556, 
  'SIMOB', 
  2, 
  '1997-06-03', 
  '1997-07-15', 
  '1997-06-13', 
  1, 
  9.80000019, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10557, 
  'LEHMS', 
  9, 
  '1997-06-03', 
  '1997-06-17', 
  '1997-06-06', 
  2, 
  96.7200012, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10558, 
  'AROUT', 
  1, 
  '1997-06-04', 
  '1997-07-02', 
  '1997-06-10', 
  2, 
  72.9700012, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10559, 
  'BLONP', 
  6, 
  '1997-06-05', 
  '1997-07-03', 
  '1997-06-13', 
  1, 
  8.05000019, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10560, 
  'FRANK', 
  8, 
  '1997-06-06', 
  '1997-07-04', 
  '1997-06-09', 
  1, 
  36.6500015, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10561, 
  'FOLKO', 
  2, 
  '1997-06-06', 
  '1997-07-04', 
  '1997-06-09', 
  2, 
  242.210007, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10562, 
  'REGGC', 
  1, 
  '1997-06-09', 
  '1997-07-07', 
  '1997-06-12', 
  1, 
  22.9500008, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10563, 
  'RICAR', 
  2, 
  '1997-06-10', 
  '1997-07-22', 
  '1997-06-24', 
  2, 
  60.4300003, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10564, 
  'RATTC', 
  4, 
  '1997-06-10', 
  '1997-07-08', 
  '1997-06-16', 
  3, 
  13.75, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10565, 
  'MEREP', 
  8, 
  '1997-06-11', 
  '1997-07-09', 
  '1997-06-18', 
  2, 
  7.1500001, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10566, 
  'BLONP', 
  9, 
  '1997-06-12', 
  '1997-07-10', 
  '1997-06-18', 
  1, 
  88.4000015, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10567, 
  'HUNGO', 
  1, 
  '1997-06-12', 
  '1997-07-10', 
  '1997-06-17', 
  1, 
  33.9700012, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10568, 
  'GALED', 
  3, 
  '1997-06-13', 
  '1997-07-11', 
  '1997-07-09', 
  3, 
  6.53999996, 
  'Galería del gastronómo', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '8022', 
  'Spain'
);
insert into northwind.orders
values (
  10569, 
  'RATTC', 
  5, 
  '1997-06-16', 
  '1997-07-14', 
  '1997-07-11', 
  1, 
  58.9799995, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10570, 
  'MEREP', 
  3, 
  '1997-06-17', 
  '1997-07-15', 
  '1997-06-19', 
  3, 
  188.990005, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10571, 
  'ERNSH', 
  8, 
  '1997-06-17', 
  '1997-07-29', 
  '1997-07-04', 
  3, 
  26.0599995, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10572, 
  'BERGS', 
  3, 
  '1997-06-18', 
  '1997-07-16', 
  '1997-06-25', 
  2, 
  116.43, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10573, 
  'ANTON', 
  7, 
  '1997-06-19', 
  '1997-07-17', 
  '1997-06-20', 
  3, 
  84.8399963, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10574, 
  'TRAIH', 
  4, 
  '1997-06-19', 
  '1997-07-17', 
  '1997-06-30', 
  2, 
  37.5999985, 
  'Trail''s Head Gourmet Provisioners', 
  '722 DaVinci Blvd.', 
  'Kirkland', 
  'WA', 
  '98034', 
  'USA'
);
insert into northwind.orders
values (
  10575, 
  'MORGK', 
  5, 
  '1997-06-20', 
  '1997-07-04', 
  '1997-06-30', 
  1, 
  127.339996, 
  'Morgenstern Gesundkost', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany'
);
insert into northwind.orders
values (
  10576, 
  'TORTU', 
  3, 
  '1997-06-23', 
  '1997-07-07', 
  '1997-06-30', 
  3, 
  18.5599995, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10577, 
  'TRAIH', 
  9, 
  '1997-06-23', 
  '1997-08-04', 
  '1997-06-30', 
  2, 
  25.4099998, 
  'Trail''s Head Gourmet Provisioners', 
  '722 DaVinci Blvd.', 
  'Kirkland', 
  'WA', 
  '98034', 
  'USA'
);
insert into northwind.orders
values (
  10578, 
  'BSBEV', 
  4, 
  '1997-06-24', 
  '1997-07-22', 
  '1997-07-25', 
  3, 
  29.6000004, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10579, 
  'LETSS', 
  1, 
  '1997-06-25', 
  '1997-07-23', 
  '1997-07-04', 
  2, 
  13.7299995, 
  'Let''s Stop N Shop', 
  '87 Polk St. Suite 5', 
  'San Francisco', 
  'CA', 
  '94117', 
  'USA'
);
insert into northwind.orders
values (
  10580, 
  'OTTIK', 
  4, 
  '1997-06-26', 
  '1997-07-24', 
  '1997-07-01', 
  3, 
  75.8899994, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10581, 
  'FAMIA', 
  3, 
  '1997-06-26', 
  '1997-07-24', 
  '1997-07-02', 
  1, 
  3.00999999, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10582, 
  'BLAUS', 
  3, 
  '1997-06-27', 
  '1997-07-25', 
  '1997-07-14', 
  2, 
  27.7099991, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10583, 
  'WARTH', 
  2, 
  '1997-06-30', 
  '1997-07-28', 
  '1997-07-04', 
  2, 
  7.28000021, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10584, 
  'BLONP', 
  4, 
  '1997-06-30', 
  '1997-07-28', 
  '1997-07-04', 
  1, 
  59.1399994, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10585, 
  'WELLI', 
  7, 
  '1997-07-01', 
  '1997-07-29', 
  '1997-07-10', 
  1, 
  13.4099998, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10586, 
  'REGGC', 
  9, 
  '1997-07-02', 
  '1997-07-30', 
  '1997-07-09', 
  1, 
  0.479999989, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10587, 
  'QUEDE', 
  1, 
  '1997-07-02', 
  '1997-07-30', 
  '1997-07-09', 
  1, 
  62.5200005, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10588, 
  'QUICK', 
  2, 
  '1997-07-03', 
  '1997-07-31', 
  '1997-07-10', 
  3, 
  194.669998, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10589, 
  'GREAL', 
  8, 
  '1997-07-04', 
  '1997-08-01', 
  '1997-07-14', 
  2, 
  4.42000008, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10590, 
  'MEREP', 
  4, 
  '1997-07-07', 
  '1997-08-04', 
  '1997-07-14', 
  3, 
  44.7700005, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10591, 
  'VAFFE', 
  1, 
  '1997-07-07', 
  '1997-07-21', 
  '1997-07-16', 
  1, 
  55.9199982, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10592, 
  'LEHMS', 
  3, 
  '1997-07-08', 
  '1997-08-05', 
  '1997-07-16', 
  1, 
  32.0999985, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10593, 
  'LEHMS', 
  7, 
  '1997-07-09', 
  '1997-08-06', 
  '1997-08-13', 
  2, 
  174.199997, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10594, 
  'OLDWO', 
  3, 
  '1997-07-09', 
  '1997-08-06', 
  '1997-07-16', 
  2, 
  5.23999977, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10595, 
  'ERNSH', 
  2, 
  '1997-07-10', 
  '1997-08-07', 
  '1997-07-14', 
  1, 
  96.7799988, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10596, 
  'WHITC', 
  8, 
  '1997-07-11', 
  '1997-08-08', 
  '1997-08-12', 
  1, 
  16.3400002, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10597, 
  'PICCO', 
  7, 
  '1997-07-11', 
  '1997-08-08', 
  '1997-07-18', 
  3, 
  35.1199989, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10598, 
  'RATTC', 
  1, 
  '1997-07-14', 
  '1997-08-11', 
  '1997-07-18', 
  3, 
  44.4199982, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10599, 
  'BSBEV', 
  6, 
  '1997-07-15', 
  '1997-08-26', 
  '1997-07-21', 
  3, 
  29.9799995, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10600, 
  'HUNGC', 
  4, 
  '1997-07-16', 
  '1997-08-13', 
  '1997-07-21', 
  1, 
  45.1300011, 
  'Hungry Coyote Import Store', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA'
);
insert into northwind.orders
values (
  10601, 
  'HILAA', 
  7, 
  '1997-07-16', 
  '1997-08-27', 
  '1997-07-22', 
  1, 
  58.2999992, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10602, 
  'VAFFE', 
  8, 
  '1997-07-17', 
  '1997-08-14', 
  '1997-07-22', 
  2, 
  2.92000008, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10603, 
  'SAVEA', 
  8, 
  '1997-07-18', 
  '1997-08-15', 
  '1997-08-08', 
  2, 
  48.7700005, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10604, 
  'FURIB', 
  1, 
  '1997-07-18', 
  '1997-08-15', 
  '1997-07-29', 
  1, 
  7.46000004, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10605, 
  'MEREP', 
  1, 
  '1997-07-21', 
  '1997-08-18', 
  '1997-07-29', 
  2, 
  379.130005, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10606, 
  'TRADH', 
  4, 
  '1997-07-22', 
  '1997-08-19', 
  '1997-07-31', 
  3, 
  79.4000015, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10607, 
  'SAVEA', 
  5, 
  '1997-07-22', 
  '1997-08-19', 
  '1997-07-25', 
  1, 
  200.240005, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10608, 
  'TOMSP', 
  4, 
  '1997-07-23', 
  '1997-08-20', 
  '1997-08-01', 
  2, 
  27.7900009, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10609, 
  'DUMON', 
  7, 
  '1997-07-24', 
  '1997-08-21', 
  '1997-07-30', 
  2, 
  1.85000002, 
  'Du monde entier', 
  '67, rue des Cinquante Otages', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10610, 
  'LAMAI', 
  8, 
  '1997-07-25', 
  '1997-08-22', 
  '1997-08-06', 
  1, 
  26.7800007, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10611, 
  'WOLZA', 
  6, 
  '1997-07-25', 
  '1997-08-22', 
  '1997-08-01', 
  2, 
  80.6500015, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10612, 
  'SAVEA', 
  1, 
  '1997-07-28', 
  '1997-08-25', 
  '1997-08-01', 
  2, 
  544.080017, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10613, 
  'HILAA', 
  4, 
  '1997-07-29', 
  '1997-08-26', 
  '1997-08-01', 
  2, 
  8.10999966, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10614, 
  'BLAUS', 
  8, 
  '1997-07-29', 
  '1997-08-26', 
  '1997-08-01', 
  3, 
  1.92999995, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10615, 
  'WILMK', 
  2, 
  '1997-07-30', 
  '1997-08-27', 
  '1997-08-06', 
  3, 
  0.75, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10616, 
  'GREAL', 
  1, 
  '1997-07-31', 
  '1997-08-28', 
  '1997-08-05', 
  2, 
  116.529999, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10617, 
  'GREAL', 
  4, 
  '1997-07-31', 
  '1997-08-28', 
  '1997-08-04', 
  2, 
  18.5300007, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10618, 
  'MEREP', 
  1, 
  '1997-08-01', 
  '1997-09-12', 
  '1997-08-08', 
  1, 
  154.679993, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10619, 
  'MEREP', 
  3, 
  '1997-08-04', 
  '1997-09-01', 
  '1997-08-07', 
  3, 
  91.0500031, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10620, 
  'LAUGB', 
  2, 
  '1997-08-05', 
  '1997-09-02', 
  '1997-08-14', 
  3, 
  0.939999998, 
  'Laughing Bacchus Wine Cellars', 
  '2319 Elm St.', 
  'Vancouver', 
  'BC', 
  'V3F 2K1', 
  'Canada'
);
insert into northwind.orders
values (
  10621, 
  'ISLAT', 
  4, 
  '1997-08-05', 
  '1997-09-02', 
  '1997-08-11', 
  2, 
  23.7299995, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10622, 
  'RICAR', 
  4, 
  '1997-08-06', 
  '1997-09-03', 
  '1997-08-11', 
  3, 
  50.9700012, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10623, 
  'FRANK', 
  8, 
  '1997-08-07', 
  '1997-09-04', 
  '1997-08-12', 
  2, 
  97.1800003, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10624, 
  'THECR', 
  4, 
  '1997-08-07', 
  '1997-09-04', 
  '1997-08-19', 
  2, 
  94.8000031, 
  'The Cracker Box', 
  '55 Grizzly Peak Rd.', 
  'Butte', 
  'MT', 
  '59801', 
  'USA'
);
insert into northwind.orders
values (
  10625, 
  'ANATR', 
  3, 
  '1997-08-08', 
  '1997-09-05', 
  '1997-08-14', 
  1, 
  43.9000015, 
  'Ana Trujillo Emparedados y helados', 
  'Avda. de la Constitución 2222', 
  'México D.F.', 
  null, 
  '05021', 
  'Mexico'
);
insert into northwind.orders
values (
  10626, 
  'BERGS', 
  1, 
  '1997-08-11', 
  '1997-09-08', 
  '1997-08-20', 
  2, 
  138.690002, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10627, 
  'SAVEA', 
  8, 
  '1997-08-11', 
  '1997-09-22', 
  '1997-08-21', 
  3, 
  107.459999, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10628, 
  'BLONP', 
  4, 
  '1997-08-12', 
  '1997-09-09', 
  '1997-08-20', 
  3, 
  30.3600006, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10629, 
  'GODOS', 
  4, 
  '1997-08-12', 
  '1997-09-09', 
  '1997-08-20', 
  3, 
  85.4599991, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10630, 
  'KOENE', 
  1, 
  '1997-08-13', 
  '1997-09-10', 
  '1997-08-19', 
  2, 
  32.3499985, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10631, 
  'LAMAI', 
  8, 
  '1997-08-14', 
  '1997-09-11', 
  '1997-08-15', 
  1, 
  0.870000005, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10632, 
  'WANDK', 
  8, 
  '1997-08-14', 
  '1997-09-11', 
  '1997-08-19', 
  1, 
  41.3800011, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10633, 
  'ERNSH', 
  7, 
  '1997-08-15', 
  '1997-09-12', 
  '1997-08-18', 
  3, 
  477.899994, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10634, 
  'FOLIG', 
  4, 
  '1997-08-15', 
  '1997-09-12', 
  '1997-08-21', 
  3, 
  487.380005, 
  'Folies gourmandes', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France'
);
insert into northwind.orders
values (
  10635, 
  'MAGAA', 
  8, 
  '1997-08-18', 
  '1997-09-15', 
  '1997-08-21', 
  3, 
  47.4599991, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10636, 
  'WARTH', 
  4, 
  '1997-08-19', 
  '1997-09-16', 
  '1997-08-26', 
  1, 
  1.14999998, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10637, 
  'QUEEN', 
  6, 
  '1997-08-19', 
  '1997-09-16', 
  '1997-08-26', 
  1, 
  201.289993, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10638, 
  'LINOD', 
  3, 
  '1997-08-20', 
  '1997-09-17', 
  '1997-09-01', 
  1, 
  158.440002, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10639, 
  'SANTG', 
  7, 
  '1997-08-20', 
  '1997-09-17', 
  '1997-08-27', 
  3, 
  38.6399994, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  10640, 
  'WANDK', 
  4, 
  '1997-08-21', 
  '1997-09-18', 
  '1997-08-28', 
  1, 
  23.5499992, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10641, 
  'HILAA', 
  4, 
  '1997-08-22', 
  '1997-09-19', 
  '1997-08-26', 
  2, 
  179.610001, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10642, 
  'SIMOB', 
  7, 
  '1997-08-22', 
  '1997-09-19', 
  '1997-09-05', 
  3, 
  41.8899994, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10643, 
  'ALFKI', 
  6, 
  '1997-08-25', 
  '1997-09-22', 
  '1997-09-02', 
  1, 
  29.4599991, 
  'Alfreds Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  10644, 
  'WELLI', 
  3, 
  '1997-08-25', 
  '1997-09-22', 
  '1997-09-01', 
  2, 
  0.140000001, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10645, 
  'HANAR', 
  4, 
  '1997-08-26', 
  '1997-09-23', 
  '1997-09-02', 
  1, 
  12.4099998, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10646, 
  'HUNGO', 
  9, 
  '1997-08-27', 
  '1997-10-08', 
  '1997-09-03', 
  3, 
  142.330002, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10647, 
  'QUEDE', 
  4, 
  '1997-08-27', 
  '1997-09-10', 
  '1997-09-03', 
  2, 
  45.5400009, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10648, 
  'RICAR', 
  5, 
  '1997-08-28', 
  '1997-10-09', 
  '1997-09-09', 
  2, 
  14.25, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10649, 
  'MAISD', 
  5, 
  '1997-08-28', 
  '1997-09-25', 
  '1997-08-29', 
  3, 
  6.19999981, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10650, 
  'FAMIA', 
  5, 
  '1997-08-29', 
  '1997-09-26', 
  '1997-09-03', 
  3, 
  176.809998, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10651, 
  'WANDK', 
  8, 
  '1997-09-01', 
  '1997-09-29', 
  '1997-09-11', 
  2, 
  20.6000004, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10652, 
  'GOURL', 
  4, 
  '1997-09-01', 
  '1997-09-29', 
  '1997-09-08', 
  2, 
  7.13999987, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10653, 
  'FRANK', 
  1, 
  '1997-09-02', 
  '1997-09-30', 
  '1997-09-19', 
  1, 
  93.25, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10654, 
  'BERGS', 
  5, 
  '1997-09-02', 
  '1997-09-30', 
  '1997-09-11', 
  1, 
  55.2599983, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10655, 
  'REGGC', 
  1, 
  '1997-09-03', 
  '1997-10-01', 
  '1997-09-11', 
  2, 
  4.40999985, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10656, 
  'GREAL', 
  6, 
  '1997-09-04', 
  '1997-10-02', 
  '1997-09-10', 
  1, 
  57.1500015, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10657, 
  'SAVEA', 
  2, 
  '1997-09-04', 
  '1997-10-02', 
  '1997-09-15', 
  2, 
  352.690002, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10658, 
  'QUICK', 
  4, 
  '1997-09-05', 
  '1997-10-03', 
  '1997-09-08', 
  1, 
  364.149994, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10659, 
  'QUEEN', 
  7, 
  '1997-09-05', 
  '1997-10-03', 
  '1997-09-10', 
  2, 
  105.809998, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10660, 
  'HUNGC', 
  8, 
  '1997-09-08', 
  '1997-10-06', 
  '1997-10-15', 
  1, 
  111.290001, 
  'Hungry Coyote Import Store', 
  'City Center Plaza 516 Main St.', 
  'Elgin', 
  'OR', 
  '97827', 
  'USA'
);
insert into northwind.orders
values (
  10661, 
  'HUNGO', 
  7, 
  '1997-09-09', 
  '1997-10-07', 
  '1997-09-15', 
  3, 
  17.5499992, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10662, 
  'LONEP', 
  3, 
  '1997-09-09', 
  '1997-10-07', 
  '1997-09-18', 
  2, 
  1.27999997, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10663, 
  'BONAP', 
  2, 
  '1997-09-10', 
  '1997-09-24', 
  '1997-10-03', 
  2, 
  113.150002, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10664, 
  'FURIB', 
  1, 
  '1997-09-10', 
  '1997-10-08', 
  '1997-09-19', 
  3, 
  1.26999998, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10665, 
  'LONEP', 
  1, 
  '1997-09-11', 
  '1997-10-09', 
  '1997-09-17', 
  2, 
  26.3099995, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10666, 
  'RICSU', 
  7, 
  '1997-09-12', 
  '1997-10-10', 
  '1997-09-22', 
  2, 
  232.419998, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10667, 
  'ERNSH', 
  7, 
  '1997-09-12', 
  '1997-10-10', 
  '1997-09-19', 
  1, 
  78.0899963, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10668, 
  'WANDK', 
  1, 
  '1997-09-15', 
  '1997-10-13', 
  '1997-09-23', 
  2, 
  47.2200012, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  10669, 
  'SIMOB', 
  2, 
  '1997-09-15', 
  '1997-10-13', 
  '1997-09-22', 
  1, 
  24.3899994, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10670, 
  'FRANK', 
  4, 
  '1997-09-16', 
  '1997-10-14', 
  '1997-09-18', 
  1, 
  203.479996, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10671, 
  'FRANR', 
  1, 
  '1997-09-17', 
  '1997-10-15', 
  '1997-09-24', 
  1, 
  30.3400002, 
  'France restauration', 
  '54, rue Royale', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10672, 
  'BERGS', 
  9, 
  '1997-09-17', 
  '1997-10-01', 
  '1997-09-26', 
  2, 
  95.75, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10673, 
  'WILMK', 
  2, 
  '1997-09-18', 
  '1997-10-16', 
  '1997-09-19', 
  1, 
  22.7600002, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10674, 
  'ISLAT', 
  4, 
  '1997-09-18', 
  '1997-10-16', 
  '1997-09-30', 
  2, 
  0.899999976, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10675, 
  'FRANK', 
  5, 
  '1997-09-19', 
  '1997-10-17', 
  '1997-09-23', 
  2, 
  31.8500004, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10676, 
  'TORTU', 
  2, 
  '1997-09-22', 
  '1997-10-20', 
  '1997-09-29', 
  2, 
  2.00999999, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10677, 
  'ANTON', 
  1, 
  '1997-09-22', 
  '1997-10-20', 
  '1997-09-26', 
  3, 
  4.03000021, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10678, 
  'SAVEA', 
  7, 
  '1997-09-23', 
  '1997-10-21', 
  '1997-10-16', 
  3, 
  388.980011, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10679, 
  'BLONP', 
  8, 
  '1997-09-23', 
  '1997-10-21', 
  '1997-09-30', 
  3, 
  27.9400005, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10680, 
  'OLDWO', 
  1, 
  '1997-09-24', 
  '1997-10-22', 
  '1997-09-26', 
  1, 
  26.6100006, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10681, 
  'GREAL', 
  3, 
  '1997-09-25', 
  '1997-10-23', 
  '1997-09-30', 
  3, 
  76.1299973, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10682, 
  'ANTON', 
  3, 
  '1997-09-25', 
  '1997-10-23', 
  '1997-10-01', 
  2, 
  36.1300011, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10683, 
  'DUMON', 
  2, 
  '1997-09-26', 
  '1997-10-24', 
  '1997-10-01', 
  1, 
  4.4000001, 
  'Du monde entier', 
  '67, rue des Cinquante Otages', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10684, 
  'OTTIK', 
  3, 
  '1997-09-26', 
  '1997-10-24', 
  '1997-09-30', 
  1, 
  145.630005, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10685, 
  'GOURL', 
  4, 
  '1997-09-29', 
  '1997-10-13', 
  '1997-10-03', 
  2, 
  33.75, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10686, 
  'PICCO', 
  2, 
  '1997-09-30', 
  '1997-10-28', 
  '1997-10-08', 
  1, 
  96.5, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10687, 
  'HUNGO', 
  9, 
  '1997-09-30', 
  '1997-10-28', 
  '1997-10-30', 
  2, 
  296.429993, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10688, 
  'VAFFE', 
  4, 
  '1997-10-01', 
  '1997-10-15', 
  '1997-10-07', 
  2, 
  299.089996, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10689, 
  'BERGS', 
  1, 
  '1997-10-01', 
  '1997-10-29', 
  '1997-10-07', 
  2, 
  13.4200001, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10690, 
  'HANAR', 
  1, 
  '1997-10-02', 
  '1997-10-30', 
  '1997-10-03', 
  1, 
  15.8000002, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10691, 
  'QUICK', 
  2, 
  '1997-10-03', 
  '1997-11-14', 
  '1997-10-22', 
  2, 
  810.049988, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10692, 
  'ALFKI', 
  4, 
  '1997-10-03', 
  '1997-10-31', 
  '1997-10-13', 
  2, 
  61.0200005, 
  'Alfred''s Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  10693, 
  'WHITC', 
  3, 
  '1997-10-06', 
  '1997-10-20', 
  '1997-10-10', 
  3, 
  139.339996, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10694, 
  'QUICK', 
  8, 
  '1997-10-06', 
  '1997-11-03', 
  '1997-10-09', 
  3, 
  398.359985, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10695, 
  'WILMK', 
  7, 
  '1997-10-07', 
  '1997-11-18', 
  '1997-10-14', 
  1, 
  16.7199993, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10696, 
  'WHITC', 
  8, 
  '1997-10-08', 
  '1997-11-19', 
  '1997-10-14', 
  3, 
  102.550003, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10697, 
  'LINOD', 
  3, 
  '1997-10-08', 
  '1997-11-05', 
  '1997-10-14', 
  1, 
  45.5200005, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10698, 
  'ERNSH', 
  4, 
  '1997-10-09', 
  '1997-11-06', 
  '1997-10-17', 
  1, 
  272.470001, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10699, 
  'MORGK', 
  3, 
  '1997-10-09', 
  '1997-11-06', 
  '1997-10-13', 
  3, 
  0.579999983, 
  'Morgenstern Gesundkost', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany'
);
insert into northwind.orders
values (
  10700, 
  'SAVEA', 
  3, 
  '1997-10-10', 
  '1997-11-07', 
  '1997-10-16', 
  1, 
  65.0999985, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10701, 
  'HUNGO', 
  6, 
  '1997-10-13', 
  '1997-10-27', 
  '1997-10-15', 
  3, 
  220.309998, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10702, 
  'ALFKI', 
  4, 
  '1997-10-13', 
  '1997-11-24', 
  '1997-10-21', 
  1, 
  23.9400005, 
  'Alfred''s Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  10703, 
  'FOLKO', 
  6, 
  '1997-10-14', 
  '1997-11-11', 
  '1997-10-20', 
  2, 
  152.300003, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10704, 
  'QUEEN', 
  6, 
  '1997-10-14', 
  '1997-11-11', 
  '1997-11-07', 
  1, 
  4.78000021, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10705, 
  'HILAA', 
  9, 
  '1997-10-15', 
  '1997-11-12', 
  '1997-11-18', 
  2, 
  3.51999998, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10706, 
  'OLDWO', 
  8, 
  '1997-10-16', 
  '1997-11-13', 
  '1997-10-21', 
  3, 
  135.630005, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10707, 
  'AROUT', 
  4, 
  '1997-10-16', 
  '1997-10-30', 
  '1997-10-23', 
  3, 
  21.7399998, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10708, 
  'THEBI', 
  6, 
  '1997-10-17', 
  '1997-11-28', 
  '1997-11-05', 
  2, 
  2.96000004, 
  'The Big Cheese', 
  '89 Jefferson Way Suite 2', 
  'Portland', 
  'OR', 
  '97201', 
  'USA'
);
insert into northwind.orders
values (
  10709, 
  'GOURL', 
  1, 
  '1997-10-17', 
  '1997-11-14', 
  '1997-11-20', 
  3, 
  210.800003, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10710, 
  'FRANS', 
  1, 
  '1997-10-20', 
  '1997-11-17', 
  '1997-10-23', 
  1, 
  4.98000002, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  10711, 
  'SAVEA', 
  5, 
  '1997-10-21', 
  '1997-12-02', 
  '1997-10-29', 
  2, 
  52.4099998, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10712, 
  'HUNGO', 
  3, 
  '1997-10-21', 
  '1997-11-18', 
  '1997-10-31', 
  1, 
  89.9300003, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10713, 
  'SAVEA', 
  1, 
  '1997-10-22', 
  '1997-11-19', 
  '1997-10-24', 
  1, 
  167.050003, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10714, 
  'SAVEA', 
  5, 
  '1997-10-22', 
  '1997-11-19', 
  '1997-10-27', 
  3, 
  24.4899998, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10715, 
  'BONAP', 
  3, 
  '1997-10-23', 
  '1997-11-06', 
  '1997-10-29', 
  1, 
  63.2000008, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10716, 
  'RANCH', 
  4, 
  '1997-10-24', 
  '1997-11-21', 
  '1997-10-27', 
  2, 
  22.5699997, 
  'Rancho grande', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10717, 
  'FRANK', 
  1, 
  '1997-10-24', 
  '1997-11-21', 
  '1997-10-29', 
  2, 
  59.25, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10718, 
  'KOENE', 
  1, 
  '1997-10-27', 
  '1997-11-24', 
  '1997-10-29', 
  3, 
  170.880005, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10719, 
  'LETSS', 
  8, 
  '1997-10-27', 
  '1997-11-24', 
  '1997-11-05', 
  2, 
  51.4399986, 
  'Let''s Stop N Shop', 
  '87 Polk St. Suite 5', 
  'San Francisco', 
  'CA', 
  '94117', 
  'USA'
);
insert into northwind.orders
values (
  10720, 
  'QUEDE', 
  8, 
  '1997-10-28', 
  '1997-11-11', 
  '1997-11-05', 
  2, 
  9.52999973, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10721, 
  'QUICK', 
  5, 
  '1997-10-29', 
  '1997-11-26', 
  '1997-10-31', 
  3, 
  48.9199982, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10722, 
  'SAVEA', 
  8, 
  '1997-10-29', 
  '1997-12-10', 
  '1997-11-04', 
  1, 
  74.5800018, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10723, 
  'WHITC', 
  3, 
  '1997-10-30', 
  '1997-11-27', 
  '1997-11-25', 
  1, 
  21.7199993, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10724, 
  'MEREP', 
  8, 
  '1997-10-30', 
  '1997-12-11', 
  '1997-11-05', 
  2, 
  57.75, 
  'Mère Paillarde', 
  '43 rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada'
);
insert into northwind.orders
values (
  10725, 
  'FAMIA', 
  4, 
  '1997-10-31', 
  '1997-11-28', 
  '1997-11-05', 
  3, 
  10.8299999, 
  'Familia Arquibaldo', 
  'Rua Orós, 92', 
  'Sao Paulo', 
  'SP', 
  '05442-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10726, 
  'EASTC', 
  4, 
  '1997-11-03', 
  '1997-11-17', 
  '1997-12-05', 
  1, 
  16.5599995, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  10727, 
  'REGGC', 
  2, 
  '1997-11-03', 
  '1997-12-01', 
  '1997-12-05', 
  1, 
  89.9000015, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10728, 
  'QUEEN', 
  4, 
  '1997-11-04', 
  '1997-12-02', 
  '1997-11-11', 
  2, 
  58.3300018, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10729, 
  'LINOD', 
  8, 
  '1997-11-04', 
  '1997-12-16', 
  '1997-11-14', 
  3, 
  141.059998, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10730, 
  'BONAP', 
  5, 
  '1997-11-05', 
  '1997-12-03', 
  '1997-11-14', 
  1, 
  20.1200008, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10731, 
  'CHOPS', 
  7, 
  '1997-11-06', 
  '1997-12-04', 
  '1997-11-14', 
  1, 
  96.6500015, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10732, 
  'BONAP', 
  3, 
  '1997-11-06', 
  '1997-12-04', 
  '1997-11-07', 
  1, 
  16.9699993, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10733, 
  'BERGS', 
  1, 
  '1997-11-07', 
  '1997-12-05', 
  '1997-11-10', 
  3, 
  110.110001, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10734, 
  'GOURL', 
  2, 
  '1997-11-07', 
  '1997-12-05', 
  '1997-11-12', 
  3, 
  1.63, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10735, 
  'LETSS', 
  6, 
  '1997-11-10', 
  '1997-12-08', 
  '1997-11-21', 
  2, 
  45.9700012, 
  'Let''s Stop N Shop', 
  '87 Polk St. Suite 5', 
  'San Francisco', 
  'CA', 
  '94117', 
  'USA'
);
insert into northwind.orders
values (
  10736, 
  'HUNGO', 
  9, 
  '1997-11-11', 
  '1997-12-09', 
  '1997-11-21', 
  2, 
  44.0999985, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10737, 
  'VINET', 
  2, 
  '1997-11-11', 
  '1997-12-09', 
  '1997-11-18', 
  2, 
  7.78999996, 
  'Vins et alcools Chevalier', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France'
);
insert into northwind.orders
values (
  10738, 
  'SPECD', 
  2, 
  '1997-11-12', 
  '1997-12-10', 
  '1997-11-18', 
  1, 
  2.91000009, 
  'Spécialités du monde', 
  '25, rue Lauriston', 
  'Paris', 
  null, 
  '75016', 
  'France'
);
insert into northwind.orders
values (
  10739, 
  'VINET', 
  3, 
  '1997-11-12', 
  '1997-12-10', 
  '1997-11-17', 
  3, 
  11.0799999, 
  'Vins et alcools Chevalier', 
  '59 rue de l''Abbaye', 
  'Reims', 
  null, 
  '51100', 
  'France'
);
insert into northwind.orders
values (
  10740, 
  'WHITC', 
  4, 
  '1997-11-13', 
  '1997-12-11', 
  '1997-11-25', 
  2, 
  81.8799973, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10741, 
  'AROUT', 
  4, 
  '1997-11-14', 
  '1997-11-28', 
  '1997-11-18', 
  3, 
  10.96, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10742, 
  'BOTTM', 
  3, 
  '1997-11-14', 
  '1997-12-12', 
  '1997-11-18', 
  3, 
  243.729996, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10743, 
  'AROUT', 
  1, 
  '1997-11-17', 
  '1997-12-15', 
  '1997-11-21', 
  2, 
  23.7199993, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10744, 
  'VAFFE', 
  6, 
  '1997-11-17', 
  '1997-12-15', 
  '1997-11-24', 
  1, 
  69.1900024, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10745, 
  'QUICK', 
  9, 
  '1997-11-18', 
  '1997-12-16', 
  '1997-11-27', 
  1, 
  3.51999998, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10746, 
  'CHOPS', 
  1, 
  '1997-11-19', 
  '1997-12-17', 
  '1997-11-21', 
  3, 
  31.4300003, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10747, 
  'PICCO', 
  6, 
  '1997-11-19', 
  '1997-12-17', 
  '1997-11-26', 
  1, 
  117.330002, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10748, 
  'SAVEA', 
  3, 
  '1997-11-20', 
  '1997-12-18', 
  '1997-11-28', 
  1, 
  232.550003, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10749, 
  'ISLAT', 
  4, 
  '1997-11-20', 
  '1997-12-18', 
  '1997-12-19', 
  2, 
  61.5299988, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10750, 
  'WARTH', 
  9, 
  '1997-11-21', 
  '1997-12-19', 
  '1997-11-24', 
  1, 
  79.3000031, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10751, 
  'RICSU', 
  3, 
  '1997-11-24', 
  '1997-12-22', 
  '1997-12-03', 
  3, 
  130.789993, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10752, 
  'NORTS', 
  2, 
  '1997-11-24', 
  '1997-12-22', 
  '1997-11-28', 
  3, 
  1.38999999, 
  'North/South', 
  'South House 300 Queensbridge', 
  'London', 
  null, 
  'SW7 1RZ', 
  'UK'
);
insert into northwind.orders
values (
  10753, 
  'FRANS', 
  3, 
  '1997-11-25', 
  '1997-12-23', 
  '1997-11-27', 
  1, 
  7.69999981, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  10754, 
  'MAGAA', 
  6, 
  '1997-11-25', 
  '1997-12-23', 
  '1997-11-27', 
  3, 
  2.38000011, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10755, 
  'BONAP', 
  4, 
  '1997-11-26', 
  '1997-12-24', 
  '1997-11-28', 
  2, 
  16.7099991, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10756, 
  'SPLIR', 
  8, 
  '1997-11-27', 
  '1997-12-25', 
  '1997-12-02', 
  2, 
  73.2099991, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10757, 
  'SAVEA', 
  6, 
  '1997-11-27', 
  '1997-12-25', 
  '1997-12-15', 
  1, 
  8.18999958, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10758, 
  'RICSU', 
  3, 
  '1997-11-28', 
  '1997-12-26', 
  '1997-12-04', 
  3, 
  138.169998, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10759, 
  'ANATR', 
  3, 
  '1997-11-28', 
  '1997-12-26', 
  '1997-12-12', 
  3, 
  11.9899998, 
  'Ana Trujillo Emparedados y helados', 
  'Avda. de la Constitución 2222', 
  'México D.F.', 
  null, 
  '05021', 
  'Mexico'
);
insert into northwind.orders
values (
  10760, 
  'MAISD', 
  4, 
  '1997-12-01', 
  '1997-12-29', 
  '1997-12-10', 
  1, 
  155.639999, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10761, 
  'RATTC', 
  5, 
  '1997-12-02', 
  '1997-12-30', 
  '1997-12-08', 
  2, 
  18.6599998, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10762, 
  'FOLKO', 
  3, 
  '1997-12-02', 
  '1997-12-30', 
  '1997-12-09', 
  1, 
  328.73999, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10763, 
  'FOLIG', 
  3, 
  '1997-12-03', 
  '1997-12-31', 
  '1997-12-08', 
  3, 
  37.3499985, 
  'Folies gourmandes', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France'
);
insert into northwind.orders
values (
  10764, 
  'ERNSH', 
  6, 
  '1997-12-03', 
  '1997-12-31', 
  '1997-12-08', 
  3, 
  145.449997, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10765, 
  'QUICK', 
  3, 
  '1997-12-04', 
  '1998-01-01', 
  '1997-12-09', 
  3, 
  42.7400017, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10766, 
  'OTTIK', 
  4, 
  '1997-12-05', 
  '1998-01-02', 
  '1997-12-09', 
  1, 
  157.550003, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10767, 
  'SUPRD', 
  4, 
  '1997-12-05', 
  '1998-01-02', 
  '1997-12-15', 
  3, 
  1.59000003, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10768, 
  'AROUT', 
  3, 
  '1997-12-08', 
  '1998-01-05', 
  '1997-12-15', 
  2, 
  146.320007, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10769, 
  'VAFFE', 
  3, 
  '1997-12-08', 
  '1998-01-05', 
  '1997-12-12', 
  1, 
  65.0599976, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10770, 
  'HANAR', 
  8, 
  '1997-12-09', 
  '1998-01-06', 
  '1997-12-17', 
  3, 
  5.32000017, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10771, 
  'ERNSH', 
  9, 
  '1997-12-10', 
  '1998-01-07', 
  '1998-01-02', 
  2, 
  11.1899996, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10772, 
  'LEHMS', 
  3, 
  '1997-12-10', 
  '1998-01-07', 
  '1997-12-19', 
  2, 
  91.2799988, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10773, 
  'ERNSH', 
  1, 
  '1997-12-11', 
  '1998-01-08', 
  '1997-12-16', 
  3, 
  96.4300003, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10774, 
  'FOLKO', 
  4, 
  '1997-12-11', 
  '1997-12-25', 
  '1997-12-12', 
  1, 
  48.2000008, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10775, 
  'THECR', 
  7, 
  '1997-12-12', 
  '1998-01-09', 
  '1997-12-26', 
  1, 
  20.25, 
  'The Cracker Box', 
  '55 Grizzly Peak Rd.', 
  'Butte', 
  'MT', 
  '59801', 
  'USA'
);
insert into northwind.orders
values (
  10776, 
  'ERNSH', 
  1, 
  '1997-12-15', 
  '1998-01-12', 
  '1997-12-18', 
  3, 
  351.529999, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10777, 
  'GOURL', 
  7, 
  '1997-12-15', 
  '1997-12-29', 
  '1998-01-21', 
  2, 
  3.00999999, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10778, 
  'BERGS', 
  3, 
  '1997-12-16', 
  '1998-01-13', 
  '1997-12-24', 
  1, 
  6.78999996, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10779, 
  'MORGK', 
  3, 
  '1997-12-16', 
  '1998-01-13', 
  '1998-01-14', 
  2, 
  58.1300011, 
  'Morgenstern Gesundkost', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany'
);
insert into northwind.orders
values (
  10780, 
  'LILAS', 
  2, 
  '1997-12-16', 
  '1997-12-30', 
  '1997-12-25', 
  1, 
  42.1300011, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10781, 
  'WARTH', 
  2, 
  '1997-12-17', 
  '1998-01-14', 
  '1997-12-19', 
  3, 
  73.1600037, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  10782, 
  'CACTU', 
  9, 
  '1997-12-17', 
  '1998-01-14', 
  '1997-12-22', 
  3, 
  1.10000002, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10783, 
  'HANAR', 
  4, 
  '1997-12-18', 
  '1998-01-15', 
  '1997-12-19', 
  2, 
  124.980003, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10784, 
  'MAGAA', 
  4, 
  '1997-12-18', 
  '1998-01-15', 
  '1997-12-22', 
  3, 
  70.0899963, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10785, 
  'GROSR', 
  1, 
  '1997-12-18', 
  '1998-01-15', 
  '1997-12-24', 
  3, 
  1.50999999, 
  'GROSELLA-Restaurante', 
  '5ª Ave. Los Palos Grandes', 
  'Caracas', 
  'DF', 
  '1081', 
  'Venezuela'
);
insert into northwind.orders
values (
  10786, 
  'QUEEN', 
  8, 
  '1997-12-19', 
  '1998-01-16', 
  '1997-12-23', 
  1, 
  110.870003, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10787, 
  'LAMAI', 
  2, 
  '1997-12-19', 
  '1998-01-02', 
  '1997-12-26', 
  1, 
  249.929993, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10788, 
  'QUICK', 
  1, 
  '1997-12-22', 
  '1998-01-19', 
  '1998-01-19', 
  2, 
  42.7000008, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10789, 
  'FOLIG', 
  1, 
  '1997-12-22', 
  '1998-01-19', 
  '1997-12-31', 
  2, 
  100.599998, 
  'Folies gourmandes', 
  '184, chaussée de Tournai', 
  'Lille', 
  null, 
  '59000', 
  'France'
);
insert into northwind.orders
values (
  10790, 
  'GOURL', 
  6, 
  '1997-12-22', 
  '1998-01-19', 
  '1997-12-26', 
  1, 
  28.2299995, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10791, 
  'FRANK', 
  6, 
  '1997-12-23', 
  '1998-01-20', 
  '1998-01-01', 
  2, 
  16.8500004, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10792, 
  'WOLZA', 
  1, 
  '1997-12-23', 
  '1998-01-20', 
  '1997-12-31', 
  3, 
  23.7900009, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10793, 
  'AROUT', 
  3, 
  '1997-12-24', 
  '1998-01-21', 
  '1998-01-08', 
  3, 
  4.51999998, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10794, 
  'QUEDE', 
  6, 
  '1997-12-24', 
  '1998-01-21', 
  '1998-01-02', 
  1, 
  21.4899998, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10795, 
  'ERNSH', 
  8, 
  '1997-12-24', 
  '1998-01-21', 
  '1998-01-20', 
  2, 
  126.660004, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10796, 
  'HILAA', 
  3, 
  '1997-12-25', 
  '1998-01-22', 
  '1998-01-14', 
  1, 
  26.5200005, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10797, 
  'DRACD', 
  7, 
  '1997-12-25', 
  '1998-01-22', 
  '1998-01-05', 
  2, 
  33.3499985, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  10798, 
  'ISLAT', 
  2, 
  '1997-12-26', 
  '1998-01-23', 
  '1998-01-05', 
  1, 
  2.32999992, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10799, 
  'KOENE', 
  9, 
  '1997-12-26', 
  '1998-02-06', 
  '1998-01-05', 
  3, 
  30.7600002, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10800, 
  'SEVES', 
  1, 
  '1997-12-26', 
  '1998-01-23', 
  '1998-01-05', 
  3, 
  137.440002, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10801, 
  'BOLID', 
  4, 
  '1997-12-29', 
  '1998-01-26', 
  '1997-12-31', 
  2, 
  97.0899963, 
  'Bólido Comidas preparadas', 
  'C/ Araquil, 67', 
  'Madrid', 
  null, 
  '28023', 
  'Spain'
);
insert into northwind.orders
values (
  10802, 
  'SIMOB', 
  4, 
  '1997-12-29', 
  '1998-01-26', 
  '1998-01-02', 
  2, 
  257.26001, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  10803, 
  'WELLI', 
  4, 
  '1997-12-30', 
  '1998-01-27', 
  '1998-01-06', 
  1, 
  55.2299995, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10804, 
  'SEVES', 
  6, 
  '1997-12-30', 
  '1998-01-27', 
  '1998-01-07', 
  2, 
  27.3299999, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10805, 
  'THEBI', 
  2, 
  '1997-12-30', 
  '1998-01-27', 
  '1998-01-09', 
  3, 
  237.339996, 
  'The Big Cheese', 
  '89 Jefferson Way Suite 2', 
  'Portland', 
  'OR', 
  '97201', 
  'USA'
);
insert into northwind.orders
values (
  10806, 
  'VICTE', 
  3, 
  '1997-12-31', 
  '1998-01-28', 
  '1998-01-05', 
  2, 
  22.1100006, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10807, 
  'FRANS', 
  4, 
  '1997-12-31', 
  '1998-01-28', 
  '1998-01-30', 
  1, 
  1.36000001, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  10808, 
  'OLDWO', 
  2, 
  '1998-01-01', 
  '1998-01-29', 
  '1998-01-09', 
  3, 
  45.5299988, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10809, 
  'WELLI', 
  7, 
  '1998-01-01', 
  '1998-01-29', 
  '1998-01-07', 
  1, 
  4.86999989, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10810, 
  'LAUGB', 
  2, 
  '1998-01-01', 
  '1998-01-29', 
  '1998-01-07', 
  3, 
  4.32999992, 
  'Laughing Bacchus Wine Cellars', 
  '2319 Elm St.', 
  'Vancouver', 
  'BC', 
  'V3F 2K1', 
  'Canada'
);
insert into northwind.orders
values (
  10811, 
  'LINOD', 
  8, 
  '1998-01-02', 
  '1998-01-30', 
  '1998-01-08', 
  1, 
  31.2199993, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10812, 
  'REGGC', 
  5, 
  '1998-01-02', 
  '1998-01-30', 
  '1998-01-12', 
  1, 
  59.7799988, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10813, 
  'RICAR', 
  1, 
  '1998-01-05', 
  '1998-02-02', 
  '1998-01-09', 
  1, 
  47.3800011, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10814, 
  'VICTE', 
  3, 
  '1998-01-05', 
  '1998-02-02', 
  '1998-01-14', 
  3, 
  130.940002, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10815, 
  'SAVEA', 
  2, 
  '1998-01-05', 
  '1998-02-02', 
  '1998-01-14', 
  3, 
  14.6199999, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10816, 
  'GREAL', 
  4, 
  '1998-01-06', 
  '1998-02-03', 
  '1998-02-04', 
  2, 
  719.780029, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10817, 
  'KOENE', 
  3, 
  '1998-01-06', 
  '1998-01-20', 
  '1998-01-13', 
  2, 
  306.070007, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10818, 
  'MAGAA', 
  7, 
  '1998-01-07', 
  '1998-02-04', 
  '1998-01-12', 
  3, 
  65.4800034, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10819, 
  'CACTU', 
  2, 
  '1998-01-07', 
  '1998-02-04', 
  '1998-01-16', 
  3, 
  19.7600002, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10820, 
  'RATTC', 
  3, 
  '1998-01-07', 
  '1998-02-04', 
  '1998-01-13', 
  2, 
  37.5200005, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10821, 
  'SPLIR', 
  1, 
  '1998-01-08', 
  '1998-02-05', 
  '1998-01-15', 
  1, 
  36.6800003, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10822, 
  'TRAIH', 
  6, 
  '1998-01-08', 
  '1998-02-05', 
  '1998-01-16', 
  3, 
  7, 
  'Trail''s Head Gourmet Provisioners', 
  '722 DaVinci Blvd.', 
  'Kirkland', 
  'WA', 
  '98034', 
  'USA'
);
insert into northwind.orders
values (
  10823, 
  'LILAS', 
  5, 
  '1998-01-09', 
  '1998-02-06', 
  '1998-01-13', 
  2, 
  163.970001, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10824, 
  'FOLKO', 
  8, 
  '1998-01-09', 
  '1998-02-06', 
  '1998-01-30', 
  1, 
  1.23000002, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10825, 
  'DRACD', 
  1, 
  '1998-01-09', 
  '1998-02-06', 
  '1998-01-14', 
  1, 
  79.25, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  10826, 
  'BLONP', 
  6, 
  '1998-01-12', 
  '1998-02-09', 
  '1998-02-06', 
  1, 
  7.09000015, 
  'Blondel père et fils', 
  '24, place Kléber', 
  'Strasbourg', 
  null, 
  '67000', 
  'France'
);
insert into northwind.orders
values (
  10827, 
  'BONAP', 
  1, 
  '1998-01-12', 
  '1998-01-26', 
  '1998-02-06', 
  2, 
  63.5400009, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10828, 
  'RANCH', 
  9, 
  '1998-01-13', 
  '1998-01-27', 
  '1998-02-04', 
  1, 
  90.8499985, 
  'Rancho grande', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10829, 
  'ISLAT', 
  9, 
  '1998-01-13', 
  '1998-02-10', 
  '1998-01-23', 
  1, 
  154.720001, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10830, 
  'TRADH', 
  4, 
  '1998-01-13', 
  '1998-02-24', 
  '1998-01-21', 
  2, 
  81.8300018, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10831, 
  'SANTG', 
  3, 
  '1998-01-14', 
  '1998-02-11', 
  '1998-01-23', 
  2, 
  72.1900024, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  10832, 
  'LAMAI', 
  2, 
  '1998-01-14', 
  '1998-02-11', 
  '1998-01-19', 
  2, 
  43.2599983, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10833, 
  'OTTIK', 
  6, 
  '1998-01-15', 
  '1998-02-12', 
  '1998-01-23', 
  2, 
  71.4899979, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  10834, 
  'TRADH', 
  1, 
  '1998-01-15', 
  '1998-02-12', 
  '1998-01-19', 
  3, 
  29.7800007, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10835, 
  'ALFKI', 
  1, 
  '1998-01-15', 
  '1998-02-12', 
  '1998-01-21', 
  3, 
  69.5299988, 
  'Alfred''s Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  10836, 
  'ERNSH', 
  7, 
  '1998-01-16', 
  '1998-02-13', 
  '1998-01-21', 
  1, 
  411.880005, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10837, 
  'BERGS', 
  9, 
  '1998-01-16', 
  '1998-02-13', 
  '1998-01-23', 
  3, 
  13.3199997, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10838, 
  'LINOD', 
  3, 
  '1998-01-19', 
  '1998-02-16', 
  '1998-01-23', 
  3, 
  59.2799988, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10839, 
  'TRADH', 
  3, 
  '1998-01-19', 
  '1998-02-16', 
  '1998-01-22', 
  3, 
  35.4300003, 
  'Tradiçao Hipermercados', 
  'Av. Inês de Castro, 414', 
  'Sao Paulo', 
  'SP', 
  '05634-030', 
  'Brazil'
);
insert into northwind.orders
values (
  10840, 
  'LINOD', 
  4, 
  '1998-01-19', 
  '1998-03-02', 
  '1998-02-16', 
  2, 
  2.71000004, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10841, 
  'SUPRD', 
  5, 
  '1998-01-20', 
  '1998-02-17', 
  '1998-01-29', 
  2, 
  424.299988, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10842, 
  'TORTU', 
  1, 
  '1998-01-20', 
  '1998-02-17', 
  '1998-01-29', 
  3, 
  54.4199982, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10843, 
  'VICTE', 
  4, 
  '1998-01-21', 
  '1998-02-18', 
  '1998-01-26', 
  2, 
  9.26000023, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10844, 
  'PICCO', 
  8, 
  '1998-01-21', 
  '1998-02-18', 
  '1998-01-26', 
  2, 
  25.2199993, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  10845, 
  'QUICK', 
  8, 
  '1998-01-21', 
  '1998-02-04', 
  '1998-01-30', 
  1, 
  212.979996, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10846, 
  'SUPRD', 
  2, 
  '1998-01-22', 
  '1998-03-05', 
  '1998-01-23', 
  3, 
  56.4599991, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10847, 
  'SAVEA', 
  4, 
  '1998-01-22', 
  '1998-02-05', 
  '1998-02-10', 
  3, 
  487.570007, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10848, 
  'CONSH', 
  7, 
  '1998-01-23', 
  '1998-02-20', 
  '1998-01-29', 
  2, 
  38.2400017, 
  'Consolidated Holdings', 
  'Berkeley Gardens 12  Brewery', 
  'London', 
  null, 
  'WX1 6LT', 
  'UK'
);
insert into northwind.orders
values (
  10849, 
  'KOENE', 
  9, 
  '1998-01-23', 
  '1998-02-20', 
  '1998-01-30', 
  2, 
  0.560000002, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10850, 
  'VICTE', 
  1, 
  '1998-01-23', 
  '1998-03-06', 
  '1998-01-30', 
  1, 
  49.1899986, 
  'Victuailles en stock', 
  '2, rue du Commerce', 
  'Lyon', 
  null, 
  '69004', 
  'France'
);
insert into northwind.orders
values (
  10851, 
  'RICAR', 
  5, 
  '1998-01-26', 
  '1998-02-23', 
  '1998-02-02', 
  1, 
  160.550003, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10852, 
  'RATTC', 
  8, 
  '1998-01-26', 
  '1998-02-09', 
  '1998-01-30', 
  1, 
  174.050003, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10853, 
  'BLAUS', 
  9, 
  '1998-01-27', 
  '1998-02-24', 
  '1998-02-03', 
  2, 
  53.8300018, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10854, 
  'ERNSH', 
  3, 
  '1998-01-27', 
  '1998-02-24', 
  '1998-02-05', 
  2, 
  100.220001, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10855, 
  'OLDWO', 
  3, 
  '1998-01-27', 
  '1998-02-24', 
  '1998-02-04', 
  1, 
  170.970001, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10856, 
  'ANTON', 
  3, 
  '1998-01-28', 
  '1998-02-25', 
  '1998-02-10', 
  2, 
  58.4300003, 
  'Antonio Moreno Taquería', 
  'Mataderos  2312', 
  'México D.F.', 
  null, 
  '05023', 
  'Mexico'
);
insert into northwind.orders
values (
  10857, 
  'BERGS', 
  8, 
  '1998-01-28', 
  '1998-02-25', 
  '1998-02-06', 
  2, 
  188.850006, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10858, 
  'LACOR', 
  2, 
  '1998-01-29', 
  '1998-02-26', 
  '1998-02-03', 
  1, 
  52.5099983, 
  'La corne d''abondance', 
  '67, avenue de l''Europe', 
  'Versailles', 
  null, 
  '78000', 
  'France'
);
insert into northwind.orders
values (
  10859, 
  'FRANK', 
  1, 
  '1998-01-29', 
  '1998-02-26', 
  '1998-02-02', 
  2, 
  76.0999985, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10860, 
  'FRANR', 
  3, 
  '1998-01-29', 
  '1998-02-26', 
  '1998-02-04', 
  3, 
  19.2600002, 
  'France restauration', 
  '54, rue Royale', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10861, 
  'WHITC', 
  4, 
  '1998-01-30', 
  '1998-02-27', 
  '1998-02-17', 
  2, 
  14.9300003, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10862, 
  'LEHMS', 
  8, 
  '1998-01-30', 
  '1998-03-13', 
  '1998-02-02', 
  2, 
  53.2299995, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10863, 
  'HILAA', 
  4, 
  '1998-02-02', 
  '1998-03-02', 
  '1998-02-17', 
  2, 
  30.2600002, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10864, 
  'AROUT', 
  4, 
  '1998-02-02', 
  '1998-03-02', 
  '1998-02-09', 
  2, 
  3.03999996, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10865, 
  'QUICK', 
  2, 
  '1998-02-02', 
  '1998-02-16', 
  '1998-02-12', 
  1, 
  348.140015, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10866, 
  'BERGS', 
  5, 
  '1998-02-03', 
  '1998-03-03', 
  '1998-02-12', 
  1, 
  109.110001, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10867, 
  'LONEP', 
  6, 
  '1998-02-03', 
  '1998-03-17', 
  '1998-02-11', 
  1, 
  1.92999995, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10868, 
  'QUEEN', 
  7, 
  '1998-02-04', 
  '1998-03-04', 
  '1998-02-23', 
  2, 
  191.270004, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10869, 
  'SEVES', 
  5, 
  '1998-02-04', 
  '1998-03-04', 
  '1998-02-09', 
  1, 
  143.279999, 
  'Seven Seas Imports', 
  '90 Wadhurst Rd.', 
  'London', 
  null, 
  'OX15 4NB', 
  'UK'
);
insert into northwind.orders
values (
  10870, 
  'WOLZA', 
  5, 
  '1998-02-04', 
  '1998-03-04', 
  '1998-02-13', 
  3, 
  12.04, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10871, 
  'BONAP', 
  9, 
  '1998-02-05', 
  '1998-03-05', 
  '1998-02-10', 
  2, 
  112.269997, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10872, 
  'GODOS', 
  5, 
  '1998-02-05', 
  '1998-03-05', 
  '1998-02-09', 
  2, 
  175.320007, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10873, 
  'WILMK', 
  4, 
  '1998-02-06', 
  '1998-03-06', 
  '1998-02-09', 
  1, 
  0.819999993, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10874, 
  'GODOS', 
  5, 
  '1998-02-06', 
  '1998-03-06', 
  '1998-02-11', 
  2, 
  19.5799999, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10875, 
  'BERGS', 
  4, 
  '1998-02-06', 
  '1998-03-06', 
  '1998-03-03', 
  2, 
  32.3699989, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10876, 
  'BONAP', 
  7, 
  '1998-02-09', 
  '1998-03-09', 
  '1998-02-12', 
  3, 
  60.4199982, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10877, 
  'RICAR', 
  1, 
  '1998-02-09', 
  '1998-03-09', 
  '1998-02-19', 
  1, 
  38.0600014, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  10878, 
  'QUICK', 
  4, 
  '1998-02-10', 
  '1998-03-10', 
  '1998-02-12', 
  1, 
  46.6899986, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10879, 
  'WILMK', 
  3, 
  '1998-02-10', 
  '1998-03-10', 
  '1998-02-12', 
  3, 
  8.5, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10880, 
  'FOLKO', 
  7, 
  '1998-02-10', 
  '1998-03-24', 
  '1998-02-18', 
  1, 
  88.0100021, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10881, 
  'CACTU', 
  4, 
  '1998-02-11', 
  '1998-03-11', 
  '1998-02-18', 
  1, 
  2.83999991, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10882, 
  'SAVEA', 
  4, 
  '1998-02-11', 
  '1998-03-11', 
  '1998-02-20', 
  3, 
  23.1000004, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10883, 
  'LONEP', 
  8, 
  '1998-02-12', 
  '1998-03-12', 
  '1998-02-20', 
  3, 
  0.529999971, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  10884, 
  'LETSS', 
  4, 
  '1998-02-12', 
  '1998-03-12', 
  '1998-02-13', 
  2, 
  90.9700012, 
  'Let''s Stop N Shop', 
  '87 Polk St. Suite 5', 
  'San Francisco', 
  'CA', 
  '94117', 
  'USA'
);
insert into northwind.orders
values (
  10885, 
  'SUPRD', 
  6, 
  '1998-02-12', 
  '1998-03-12', 
  '1998-02-18', 
  3, 
  5.63999987, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10886, 
  'HANAR', 
  1, 
  '1998-02-13', 
  '1998-03-13', 
  '1998-03-02', 
  1, 
  4.98999977, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10887, 
  'GALED', 
  8, 
  '1998-02-13', 
  '1998-03-13', 
  '1998-02-16', 
  3, 
  1.25, 
  'Galería del gastronómo', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '8022', 
  'Spain'
);
insert into northwind.orders
values (
  10888, 
  'GODOS', 
  1, 
  '1998-02-16', 
  '1998-03-16', 
  '1998-02-23', 
  2, 
  51.8699989, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10889, 
  'RATTC', 
  9, 
  '1998-02-16', 
  '1998-03-16', 
  '1998-02-23', 
  3, 
  280.609985, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10890, 
  'DUMON', 
  7, 
  '1998-02-16', 
  '1998-03-16', 
  '1998-02-18', 
  1, 
  32.7599983, 
  'Du monde entier', 
  '67, rue des Cinquante Otages', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10891, 
  'LEHMS', 
  7, 
  '1998-02-17', 
  '1998-03-17', 
  '1998-02-19', 
  2, 
  20.3700008, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10892, 
  'MAISD', 
  4, 
  '1998-02-17', 
  '1998-03-17', 
  '1998-02-19', 
  2, 
  120.269997, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10893, 
  'KOENE', 
  9, 
  '1998-02-18', 
  '1998-03-18', 
  '1998-02-20', 
  2, 
  77.7799988, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  10894, 
  'SAVEA', 
  1, 
  '1998-02-18', 
  '1998-03-18', 
  '1998-02-20', 
  1, 
  116.129997, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10895, 
  'ERNSH', 
  3, 
  '1998-02-18', 
  '1998-03-18', 
  '1998-02-23', 
  1, 
  162.75, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10896, 
  'MAISD', 
  7, 
  '1998-02-19', 
  '1998-03-19', 
  '1998-02-27', 
  3, 
  32.4500008, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10897, 
  'HUNGO', 
  3, 
  '1998-02-19', 
  '1998-03-19', 
  '1998-02-25', 
  2, 
  603.539978, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10898, 
  'OCEAN', 
  4, 
  '1998-02-20', 
  '1998-03-20', 
  '1998-03-06', 
  2, 
  1.26999998, 
  'Océano Atlántico Ltda.', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10899, 
  'LILAS', 
  5, 
  '1998-02-20', 
  '1998-03-20', 
  '1998-02-26', 
  3, 
  1.21000004, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10900, 
  'WELLI', 
  1, 
  '1998-02-20', 
  '1998-03-20', 
  '1998-03-04', 
  2, 
  1.65999997, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10901, 
  'HILAA', 
  4, 
  '1998-02-23', 
  '1998-03-23', 
  '1998-02-26', 
  1, 
  62.0900002, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10902, 
  'FOLKO', 
  1, 
  '1998-02-23', 
  '1998-03-23', 
  '1998-03-03', 
  1, 
  44.1500015, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10903, 
  'HANAR', 
  3, 
  '1998-02-24', 
  '1998-03-24', 
  '1998-03-04', 
  3, 
  36.7099991, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10904, 
  'WHITC', 
  3, 
  '1998-02-24', 
  '1998-03-24', 
  '1998-02-27', 
  3, 
  162.949997, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  10905, 
  'WELLI', 
  9, 
  '1998-02-24', 
  '1998-03-24', 
  '1998-03-06', 
  2, 
  13.7200003, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10906, 
  'WOLZA', 
  4, 
  '1998-02-25', 
  '1998-03-11', 
  '1998-03-03', 
  3, 
  26.2900009, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10907, 
  'SPECD', 
  6, 
  '1998-02-25', 
  '1998-03-25', 
  '1998-02-27', 
  3, 
  9.18999958, 
  'Spécialités du monde', 
  '25, rue Lauriston', 
  'Paris', 
  null, 
  '75016', 
  'France'
);
insert into northwind.orders
values (
  10908, 
  'REGGC', 
  4, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-06', 
  2, 
  32.9599991, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10909, 
  'SANTG', 
  1, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-10', 
  2, 
  53.0499992, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  10910, 
  'WILMK', 
  1, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-04', 
  3, 
  38.1100006, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  10911, 
  'GODOS', 
  3, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-05', 
  1, 
  38.1899986, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10912, 
  'HUNGO', 
  2, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-18', 
  2, 
  580.909973, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10913, 
  'QUEEN', 
  4, 
  '1998-02-26', 
  '1998-03-26', 
  '1998-03-04', 
  1, 
  33.0499992, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10914, 
  'QUEEN', 
  6, 
  '1998-02-27', 
  '1998-03-27', 
  '1998-03-02', 
  1, 
  21.1900005, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10915, 
  'TORTU', 
  2, 
  '1998-02-27', 
  '1998-03-27', 
  '1998-03-02', 
  2, 
  3.50999999, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10916, 
  'RANCH', 
  1, 
  '1998-02-27', 
  '1998-03-27', 
  '1998-03-09', 
  2, 
  63.7700005, 
  'Rancho grande', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10917, 
  'ROMEY', 
  4, 
  '1998-03-02', 
  '1998-03-30', 
  '1998-03-11', 
  2, 
  8.28999996, 
  'Romero y tomillo', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain'
);
insert into northwind.orders
values (
  10918, 
  'BOTTM', 
  3, 
  '1998-03-02', 
  '1998-03-30', 
  '1998-03-11', 
  3, 
  48.8300018, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10919, 
  'LINOD', 
  2, 
  '1998-03-02', 
  '1998-03-30', 
  '1998-03-04', 
  2, 
  19.7999992, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10920, 
  'AROUT', 
  4, 
  '1998-03-03', 
  '1998-03-31', 
  '1998-03-09', 
  2, 
  29.6100006, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10921, 
  'VAFFE', 
  1, 
  '1998-03-03', 
  '1998-04-14', 
  '1998-03-09', 
  1, 
  176.479996, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10922, 
  'HANAR', 
  5, 
  '1998-03-03', 
  '1998-03-31', 
  '1998-03-05', 
  3, 
  62.7400017, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10923, 
  'LAMAI', 
  7, 
  '1998-03-03', 
  '1998-04-14', 
  '1998-03-13', 
  3, 
  68.2600021, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  10924, 
  'BERGS', 
  3, 
  '1998-03-04', 
  '1998-04-01', 
  '1998-04-08', 
  2, 
  151.520004, 
  'Berglunds snabbköp', 
  'Berguvsvägen  8', 
  'Luleå', 
  null, 
  'S-958 22', 
  'Sweden'
);
insert into northwind.orders
values (
  10925, 
  'HANAR', 
  3, 
  '1998-03-04', 
  '1998-04-01', 
  '1998-03-13', 
  1, 
  2.26999998, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10926, 
  'ANATR', 
  4, 
  '1998-03-04', 
  '1998-04-01', 
  '1998-03-11', 
  3, 
  39.9199982, 
  'Ana Trujillo Emparedados y helados', 
  'Avda. de la Constitución 2222', 
  'México D.F.', 
  null, 
  '05021', 
  'Mexico'
);
insert into northwind.orders
values (
  10927, 
  'LACOR', 
  4, 
  '1998-03-05', 
  '1998-04-02', 
  '1998-04-08', 
  1, 
  19.7900009, 
  'La corne d''abondance', 
  '67, avenue de l''Europe', 
  'Versailles', 
  null, 
  '78000', 
  'France'
);
insert into northwind.orders
values (
  10928, 
  'GALED', 
  1, 
  '1998-03-05', 
  '1998-04-02', 
  '1998-03-18', 
  1, 
  1.36000001, 
  'Galería del gastronómo', 
  'Rambla de Cataluña, 23', 
  'Barcelona', 
  null, 
  '8022', 
  'Spain'
);
insert into northwind.orders
values (
  10929, 
  'FRANK', 
  6, 
  '1998-03-05', 
  '1998-04-02', 
  '1998-03-12', 
  1, 
  33.9300003, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  10930, 
  'SUPRD', 
  4, 
  '1998-03-06', 
  '1998-04-17', 
  '1998-03-18', 
  3, 
  15.5500002, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  10931, 
  'RICSU', 
  4, 
  '1998-03-06', 
  '1998-03-20', 
  '1998-03-19', 
  2, 
  13.6000004, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10932, 
  'BONAP', 
  8, 
  '1998-03-06', 
  '1998-04-03', 
  '1998-03-24', 
  1, 
  134.639999, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10933, 
  'ISLAT', 
  6, 
  '1998-03-06', 
  '1998-04-03', 
  '1998-03-16', 
  3, 
  54.1500015, 
  'Island Trading', 
  'Garden House Crowther Way', 
  'Cowes', 
  'Isle of Wight', 
  'PO31 7PJ', 
  'UK'
);
insert into northwind.orders
values (
  10934, 
  'LEHMS', 
  3, 
  '1998-03-09', 
  '1998-04-06', 
  '1998-03-12', 
  3, 
  32.0099983, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  10935, 
  'WELLI', 
  4, 
  '1998-03-09', 
  '1998-04-06', 
  '1998-03-18', 
  3, 
  47.5900002, 
  'Wellington Importadora', 
  'Rua do Mercado, 12', 
  'Resende', 
  'SP', 
  '08737-363', 
  'Brazil'
);
insert into northwind.orders
values (
  10936, 
  'GREAL', 
  3, 
  '1998-03-09', 
  '1998-04-06', 
  '1998-03-18', 
  2, 
  33.6800003, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  10937, 
  'CACTU', 
  7, 
  '1998-03-10', 
  '1998-03-24', 
  '1998-03-13', 
  3, 
  31.5100002, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10938, 
  'QUICK', 
  3, 
  '1998-03-10', 
  '1998-04-07', 
  '1998-03-16', 
  2, 
  31.8899994, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10939, 
  'MAGAA', 
  2, 
  '1998-03-10', 
  '1998-04-07', 
  '1998-03-13', 
  2, 
  76.3300018, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10940, 
  'BONAP', 
  8, 
  '1998-03-11', 
  '1998-04-08', 
  '1998-03-23', 
  3, 
  19.7700005, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  10941, 
  'SAVEA', 
  7, 
  '1998-03-11', 
  '1998-04-08', 
  '1998-03-20', 
  2, 
  400.809998, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10942, 
  'REGGC', 
  9, 
  '1998-03-11', 
  '1998-04-08', 
  '1998-03-18', 
  3, 
  17.9500008, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  10943, 
  'BSBEV', 
  4, 
  '1998-03-11', 
  '1998-04-08', 
  '1998-03-19', 
  2, 
  2.17000008, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10944, 
  'BOTTM', 
  6, 
  '1998-03-12', 
  '1998-03-26', 
  '1998-03-13', 
  3, 
  52.9199982, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10945, 
  'MORGK', 
  4, 
  '1998-03-12', 
  '1998-04-09', 
  '1998-03-18', 
  1, 
  10.2200003, 
  'Morgenstern Gesundkost', 
  'Heerstr. 22', 
  'Leipzig', 
  null, 
  '04179', 
  'Germany'
);
insert into northwind.orders
values (
  10946, 
  'VAFFE', 
  1, 
  '1998-03-12', 
  '1998-04-09', 
  '1998-03-19', 
  2, 
  27.2000008, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10947, 
  'BSBEV', 
  3, 
  '1998-03-13', 
  '1998-04-10', 
  '1998-03-16', 
  2, 
  3.25999999, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  10948, 
  'GODOS', 
  3, 
  '1998-03-13', 
  '1998-04-10', 
  '1998-03-19', 
  3, 
  23.3899994, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  10949, 
  'BOTTM', 
  2, 
  '1998-03-13', 
  '1998-04-10', 
  '1998-03-17', 
  3, 
  74.4400024, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10950, 
  'MAGAA', 
  1, 
  '1998-03-16', 
  '1998-04-13', 
  '1998-03-23', 
  2, 
  2.5, 
  'Magazzini Alimentari Riuniti', 
  'Via Ludovico il Moro 22', 
  'Bergamo', 
  null, 
  '24100', 
  'Italy'
);
insert into northwind.orders
values (
  10951, 
  'RICSU', 
  9, 
  '1998-03-16', 
  '1998-04-27', 
  '1998-04-07', 
  2, 
  30.8500004, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  10952, 
  'ALFKI', 
  1, 
  '1998-03-16', 
  '1998-04-27', 
  '1998-03-24', 
  1, 
  40.4199982, 
  'Alfred''s Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  10953, 
  'AROUT', 
  9, 
  '1998-03-16', 
  '1998-03-30', 
  '1998-03-25', 
  2, 
  23.7199993, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  10954, 
  'LINOD', 
  5, 
  '1998-03-17', 
  '1998-04-28', 
  '1998-03-20', 
  1, 
  27.9099998, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  10955, 
  'FOLKO', 
  8, 
  '1998-03-17', 
  '1998-04-14', 
  '1998-03-20', 
  2, 
  3.25999999, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10956, 
  'BLAUS', 
  6, 
  '1998-03-17', 
  '1998-04-28', 
  '1998-03-20', 
  2, 
  44.6500015, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  10957, 
  'HILAA', 
  8, 
  '1998-03-18', 
  '1998-04-15', 
  '1998-03-27', 
  3, 
  105.360001, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10958, 
  'OCEAN', 
  7, 
  '1998-03-18', 
  '1998-04-15', 
  '1998-03-27', 
  2, 
  49.5600014, 
  'Océano Atlántico Ltda.', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10959, 
  'GOURL', 
  6, 
  '1998-03-18', 
  '1998-04-29', 
  '1998-03-23', 
  2, 
  4.98000002, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  10960, 
  'HILAA', 
  3, 
  '1998-03-19', 
  '1998-04-02', 
  '1998-04-08', 
  1, 
  2.07999992, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10961, 
  'QUEEN', 
  8, 
  '1998-03-19', 
  '1998-04-16', 
  '1998-03-30', 
  1, 
  104.470001, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  10962, 
  'QUICK', 
  8, 
  '1998-03-19', 
  '1998-04-16', 
  '1998-03-23', 
  2, 
  275.790009, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10963, 
  'FURIB', 
  9, 
  '1998-03-19', 
  '1998-04-16', 
  '1998-03-26', 
  3, 
  2.70000005, 
  'Furia Bacalhau e Frutos do Mar', 
  'Jardim das rosas n. 32', 
  'Lisboa', 
  null, 
  '1675', 
  'Portugal'
);
insert into northwind.orders
values (
  10964, 
  'SPECD', 
  3, 
  '1998-03-20', 
  '1998-04-17', 
  '1998-03-24', 
  2, 
  87.3799973, 
  'Spécialités du monde', 
  '25, rue Lauriston', 
  'Paris', 
  null, 
  '75016', 
  'France'
);
insert into northwind.orders
values (
  10965, 
  'OLDWO', 
  6, 
  '1998-03-20', 
  '1998-04-17', 
  '1998-03-30', 
  3, 
  144.380005, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  10966, 
  'CHOPS', 
  4, 
  '1998-03-20', 
  '1998-04-17', 
  '1998-04-08', 
  1, 
  27.1900005, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  10967, 
  'TOMSP', 
  2, 
  '1998-03-23', 
  '1998-04-20', 
  '1998-04-02', 
  2, 
  62.2200012, 
  'Toms Spezialitäten', 
  'Luisenstr. 48', 
  'Münster', 
  null, 
  '44087', 
  'Germany'
);
insert into northwind.orders
values (
  10968, 
  'ERNSH', 
  1, 
  '1998-03-23', 
  '1998-04-20', 
  '1998-04-01', 
  3, 
  74.5999985, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10969, 
  'COMMI', 
  1, 
  '1998-03-23', 
  '1998-04-20', 
  '1998-03-30', 
  2, 
  0.209999993, 
  'Comércio Mineiro', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil'
);
insert into northwind.orders
values (
  10970, 
  'BOLID', 
  9, 
  '1998-03-24', 
  '1998-04-07', 
  '1998-04-24', 
  1, 
  16.1599998, 
  'Bólido Comidas preparadas', 
  'C/ Araquil, 67', 
  'Madrid', 
  null, 
  '28023', 
  'Spain'
);
insert into northwind.orders
values (
  10971, 
  'FRANR', 
  2, 
  '1998-03-24', 
  '1998-04-21', 
  '1998-04-02', 
  2, 
  121.82, 
  'France restauration', 
  '54, rue Royale', 
  'Nantes', 
  null, 
  '44000', 
  'France'
);
insert into northwind.orders
values (
  10972, 
  'LACOR', 
  4, 
  '1998-03-24', 
  '1998-04-21', 
  '1998-03-26', 
  2, 
  0.0199999996, 
  'La corne d''abondance', 
  '67, avenue de l''Europe', 
  'Versailles', 
  null, 
  '78000', 
  'France'
);
insert into northwind.orders
values (
  10973, 
  'LACOR', 
  6, 
  '1998-03-24', 
  '1998-04-21', 
  '1998-03-27', 
  2, 
  15.1700001, 
  'La corne d''abondance', 
  '67, avenue de l''Europe', 
  'Versailles', 
  null, 
  '78000', 
  'France'
);
insert into northwind.orders
values (
  10974, 
  'SPLIR', 
  3, 
  '1998-03-25', 
  '1998-04-08', 
  '1998-04-03', 
  3, 
  12.96, 
  'Split Rail Beer & Ale', 
  'P.O. Box 555', 
  'Lander', 
  'WY', 
  '82520', 
  'USA'
);
insert into northwind.orders
values (
  10975, 
  'BOTTM', 
  1, 
  '1998-03-25', 
  '1998-04-22', 
  '1998-03-27', 
  3, 
  32.2700005, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10976, 
  'HILAA', 
  1, 
  '1998-03-25', 
  '1998-05-06', 
  '1998-04-03', 
  1, 
  37.9700012, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  10977, 
  'FOLKO', 
  8, 
  '1998-03-26', 
  '1998-04-23', 
  '1998-04-10', 
  3, 
  208.5, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10978, 
  'MAISD', 
  9, 
  '1998-03-26', 
  '1998-04-23', 
  '1998-04-23', 
  2, 
  32.8199997, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  10979, 
  'ERNSH', 
  8, 
  '1998-03-26', 
  '1998-04-23', 
  '1998-03-31', 
  2, 
  353.070007, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10980, 
  'FOLKO', 
  4, 
  '1998-03-27', 
  '1998-05-08', 
  '1998-04-17', 
  1, 
  1.25999999, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10981, 
  'HANAR', 
  1, 
  '1998-03-27', 
  '1998-04-24', 
  '1998-04-02', 
  2, 
  193.369995, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  10982, 
  'BOTTM', 
  2, 
  '1998-03-27', 
  '1998-04-24', 
  '1998-04-08', 
  1, 
  14.0100002, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  10983, 
  'SAVEA', 
  2, 
  '1998-03-27', 
  '1998-04-24', 
  '1998-04-06', 
  2, 
  657.539978, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10984, 
  'SAVEA', 
  1, 
  '1998-03-30', 
  '1998-04-27', 
  '1998-04-03', 
  3, 
  211.220001, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  10985, 
  'HUNGO', 
  2, 
  '1998-03-30', 
  '1998-04-27', 
  '1998-04-02', 
  1, 
  91.5100021, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  10986, 
  'OCEAN', 
  8, 
  '1998-03-30', 
  '1998-04-27', 
  '1998-04-21', 
  2, 
  217.860001, 
  'Océano Atlántico Ltda.', 
  'Ing. Gustavo Moncada 8585 Piso 20-A', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  10987, 
  'EASTC', 
  8, 
  '1998-03-31', 
  '1998-04-28', 
  '1998-04-06', 
  1, 
  185.479996, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  10988, 
  'RATTC', 
  3, 
  '1998-03-31', 
  '1998-04-28', 
  '1998-04-10', 
  2, 
  61.1399994, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  10989, 
  'QUEDE', 
  2, 
  '1998-03-31', 
  '1998-04-28', 
  '1998-04-02', 
  1, 
  34.7599983, 
  'Que Delícia', 
  'Rua da Panificadora, 12', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-673', 
  'Brazil'
);
insert into northwind.orders
values (
  10990, 
  'ERNSH', 
  2, 
  '1998-04-01', 
  '1998-05-13', 
  '1998-04-07', 
  3, 
  117.610001, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  10991, 
  'QUICK', 
  1, 
  '1998-04-01', 
  '1998-04-29', 
  '1998-04-07', 
  1, 
  38.5099983, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10992, 
  'THEBI', 
  1, 
  '1998-04-01', 
  '1998-04-29', 
  '1998-04-03', 
  3, 
  4.26999998, 
  'The Big Cheese', 
  '89 Jefferson Way Suite 2', 
  'Portland', 
  'OR', 
  '97201', 
  'USA'
);
insert into northwind.orders
values (
  10993, 
  'FOLKO', 
  7, 
  '1998-04-01', 
  '1998-04-29', 
  '1998-04-10', 
  3, 
  8.81000042, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  10994, 
  'VAFFE', 
  2, 
  '1998-04-02', 
  '1998-04-16', 
  '1998-04-09', 
  3, 
  65.5299988, 
  'Vaffeljernet', 
  'Smagsloget 45', 
  'Århus', 
  null, 
  '8200', 
  'Denmark'
);
insert into northwind.orders
values (
  10995, 
  'PERIC', 
  1, 
  '1998-04-02', 
  '1998-04-30', 
  '1998-04-06', 
  3, 
  46, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  10996, 
  'QUICK', 
  4, 
  '1998-04-02', 
  '1998-04-30', 
  '1998-04-10', 
  2, 
  1.12, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  10997, 
  'LILAS', 
  8, 
  '1998-04-03', 
  '1998-05-15', 
  '1998-04-13', 
  2, 
  73.9100037, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  10998, 
  'WOLZA', 
  8, 
  '1998-04-03', 
  '1998-04-17', 
  '1998-04-17', 
  2, 
  20.3099995, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  10999, 
  'OTTIK', 
  6, 
  '1998-04-03', 
  '1998-05-01', 
  '1998-04-10', 
  2, 
  96.3499985, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  11000, 
  'RATTC', 
  2, 
  '1998-04-06', 
  '1998-05-04', 
  '1998-04-14', 
  3, 
  55.1199989, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.orders
values (
  11001, 
  'FOLKO', 
  2, 
  '1998-04-06', 
  '1998-05-04', 
  '1998-04-14', 
  2, 
  197.300003, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  11002, 
  'SAVEA', 
  4, 
  '1998-04-06', 
  '1998-05-04', 
  '1998-04-16', 
  1, 
  141.160004, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  11003, 
  'THECR', 
  3, 
  '1998-04-06', 
  '1998-05-04', 
  '1998-04-08', 
  3, 
  14.9099998, 
  'The Cracker Box', 
  '55 Grizzly Peak Rd.', 
  'Butte', 
  'MT', 
  '59801', 
  'USA'
);
insert into northwind.orders
values (
  11004, 
  'MAISD', 
  3, 
  '1998-04-07', 
  '1998-05-05', 
  '1998-04-20', 
  1, 
  44.8400002, 
  'Maison Dewey', 
  'Rue Joseph-Bens 532', 
  'Bruxelles', 
  null, 
  'B-1180', 
  'Belgium'
);
insert into northwind.orders
values (
  11005, 
  'WILMK', 
  2, 
  '1998-04-07', 
  '1998-05-05', 
  '1998-04-10', 
  1, 
  0.75, 
  'Wilman Kala', 
  'Keskuskatu 45', 
  'Helsinki', 
  null, 
  '21240', 
  'Finland'
);
insert into northwind.orders
values (
  11006, 
  'GREAL', 
  3, 
  '1998-04-07', 
  '1998-05-05', 
  '1998-04-15', 
  2, 
  25.1900005, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  11007, 
  'PRINI', 
  8, 
  '1998-04-08', 
  '1998-05-06', 
  '1998-04-13', 
  2, 
  202.240005, 
  'Princesa Isabel Vinhos', 
  'Estrada da saúde n. 58', 
  'Lisboa', 
  null, 
  '1756', 
  'Portugal'
);
insert into northwind.orders
values (
  11008, 
  'ERNSH', 
  7, 
  '1998-04-08', 
  '1998-05-06', 
  null, 
  3, 
  79.4599991, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  11009, 
  'GODOS', 
  2, 
  '1998-04-08', 
  '1998-05-06', 
  '1998-04-10', 
  1, 
  59.1100006, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  11010, 
  'REGGC', 
  2, 
  '1998-04-09', 
  '1998-05-07', 
  '1998-04-21', 
  2, 
  28.7099991, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  11011, 
  'ALFKI', 
  3, 
  '1998-04-09', 
  '1998-05-07', 
  '1998-04-13', 
  1, 
  1.21000004, 
  'Alfred''s Futterkiste', 
  'Obere Str. 57', 
  'Berlin', 
  null, 
  '12209', 
  'Germany'
);
insert into northwind.orders
values (
  11012, 
  'FRANK', 
  1, 
  '1998-04-09', 
  '1998-04-23', 
  '1998-04-17', 
  3, 
  242.949997, 
  'Frankenversand', 
  'Berliner Platz 43', 
  'München', 
  null, 
  '80805', 
  'Germany'
);
insert into northwind.orders
values (
  11013, 
  'ROMEY', 
  2, 
  '1998-04-09', 
  '1998-05-07', 
  '1998-04-10', 
  1, 
  32.9900017, 
  'Romero y tomillo', 
  'Gran Vía, 1', 
  'Madrid', 
  null, 
  '28001', 
  'Spain'
);
insert into northwind.orders
values (
  11014, 
  'LINOD', 
  2, 
  '1998-04-10', 
  '1998-05-08', 
  '1998-04-15', 
  3, 
  23.6000004, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  11015, 
  'SANTG', 
  2, 
  '1998-04-10', 
  '1998-04-24', 
  '1998-04-20', 
  2, 
  4.61999989, 
  'Santé Gourmet', 
  'Erling Skakkes gate 78', 
  'Stavern', 
  null, 
  '4110', 
  'Norway'
);
insert into northwind.orders
values (
  11016, 
  'AROUT', 
  9, 
  '1998-04-10', 
  '1998-05-08', 
  '1998-04-13', 
  2, 
  33.7999992, 
  'Around the Horn', 
  'Brook Farm Stratford St. Mary', 
  'Colchester', 
  'Essex', 
  'CO7 6JX', 
  'UK'
);
insert into northwind.orders
values (
  11017, 
  'ERNSH', 
  9, 
  '1998-04-13', 
  '1998-05-11', 
  '1998-04-20', 
  2, 
  754.26001, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  11018, 
  'LONEP', 
  4, 
  '1998-04-13', 
  '1998-05-11', 
  '1998-04-16', 
  2, 
  11.6499996, 
  'Lonesome Pine Restaurant', 
  '89 Chiaroscuro Rd.', 
  'Portland', 
  'OR', 
  '97219', 
  'USA'
);
insert into northwind.orders
values (
  11019, 
  'RANCH', 
  6, 
  '1998-04-13', 
  '1998-05-11', 
  null, 
  3, 
  3.17000008, 
  'Rancho grande', 
  'Av. del Libertador 900', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  11020, 
  'OTTIK', 
  2, 
  '1998-04-14', 
  '1998-05-12', 
  '1998-04-16', 
  2, 
  43.2999992, 
  'Ottilies Käseladen', 
  'Mehrheimerstr. 369', 
  'Köln', 
  null, 
  '50739', 
  'Germany'
);
insert into northwind.orders
values (
  11021, 
  'QUICK', 
  3, 
  '1998-04-14', 
  '1998-05-12', 
  '1998-04-21', 
  1, 
  297.179993, 
  'QUICK-Stop', 
  'Taucherstraße 10', 
  'Cunewalde', 
  null, 
  '01307', 
  'Germany'
);
insert into northwind.orders
values (
  11022, 
  'HANAR', 
  9, 
  '1998-04-14', 
  '1998-05-12', 
  '1998-05-04', 
  2, 
  6.26999998, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  11023, 
  'BSBEV', 
  1, 
  '1998-04-14', 
  '1998-04-28', 
  '1998-04-24', 
  2, 
  123.830002, 
  'B''s Beverages', 
  'Fauntleroy Circus', 
  'London', 
  null, 
  'EC2 5NT', 
  'UK'
);
insert into northwind.orders
values (
  11024, 
  'EASTC', 
  4, 
  '1998-04-15', 
  '1998-05-13', 
  '1998-04-20', 
  1, 
  74.3600006, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  11025, 
  'WARTH', 
  6, 
  '1998-04-15', 
  '1998-05-13', 
  '1998-04-24', 
  3, 
  29.1700001, 
  'Wartian Herkku', 
  'Torikatu 38', 
  'Oulu', 
  null, 
  '90110', 
  'Finland'
);
insert into northwind.orders
values (
  11026, 
  'FRANS', 
  4, 
  '1998-04-15', 
  '1998-05-13', 
  '1998-04-28', 
  1, 
  47.0900002, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  11027, 
  'BOTTM', 
  1, 
  '1998-04-16', 
  '1998-05-14', 
  '1998-04-20', 
  1, 
  52.5200005, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  11028, 
  'KOENE', 
  2, 
  '1998-04-16', 
  '1998-05-14', 
  '1998-04-22', 
  1, 
  29.5900002, 
  'Königlich Essen', 
  'Maubelstr. 90', 
  'Brandenburg', 
  null, 
  '14776', 
  'Germany'
);
insert into northwind.orders
values (
  11029, 
  'CHOPS', 
  4, 
  '1998-04-16', 
  '1998-05-14', 
  '1998-04-27', 
  1, 
  47.8400002, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  11030, 
  'SAVEA', 
  7, 
  '1998-04-17', 
  '1998-05-15', 
  '1998-04-27', 
  2, 
  830.75, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  11031, 
  'SAVEA', 
  6, 
  '1998-04-17', 
  '1998-05-15', 
  '1998-04-24', 
  2, 
  227.220001, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  11032, 
  'WHITC', 
  2, 
  '1998-04-17', 
  '1998-05-15', 
  '1998-04-23', 
  3, 
  606.190002, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  11033, 
  'RICSU', 
  7, 
  '1998-04-17', 
  '1998-05-15', 
  '1998-04-23', 
  3, 
  84.7399979, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  11034, 
  'OLDWO', 
  8, 
  '1998-04-20', 
  '1998-06-01', 
  '1998-04-27', 
  1, 
  40.3199997, 
  'Old World Delicatessen', 
  '2743 Bering St.', 
  'Anchorage', 
  'AK', 
  '99508', 
  'USA'
);
insert into northwind.orders
values (
  11035, 
  'SUPRD', 
  2, 
  '1998-04-20', 
  '1998-05-18', 
  '1998-04-24', 
  2, 
  0.170000002, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  11036, 
  'DRACD', 
  8, 
  '1998-04-20', 
  '1998-05-18', 
  '1998-04-22', 
  3, 
  149.470001, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  11037, 
  'GODOS', 
  7, 
  '1998-04-21', 
  '1998-05-19', 
  '1998-04-27', 
  1, 
  3.20000005, 
  'Godos Cocina Típica', 
  'C/ Romero, 33', 
  'Sevilla', 
  null, 
  '41101', 
  'Spain'
);
insert into northwind.orders
values (
  11038, 
  'SUPRD', 
  1, 
  '1998-04-21', 
  '1998-05-19', 
  '1998-04-30', 
  2, 
  29.5900002, 
  'Suprêmes délices', 
  'Boulevard Tirou, 255', 
  'Charleroi', 
  null, 
  'B-6000', 
  'Belgium'
);
insert into northwind.orders
values (
  11039, 
  'LINOD', 
  1, 
  '1998-04-21', 
  '1998-05-19', 
  null, 
  2, 
  65, 
  'LINO-Delicateses', 
  'Ave. 5 de Mayo Porlamar', 
  'I. de Margarita', 
  'Nueva Esparta', 
  '4980', 
  'Venezuela'
);
insert into northwind.orders
values (
  11040, 
  'GREAL', 
  4, 
  '1998-04-22', 
  '1998-05-20', 
  null, 
  3, 
  18.8400002, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  11041, 
  'CHOPS', 
  3, 
  '1998-04-22', 
  '1998-05-20', 
  '1998-04-28', 
  2, 
  48.2200012, 
  'Chop-suey Chinese', 
  'Hauptstr. 31', 
  'Bern', 
  null, 
  '3012', 
  'Switzerland'
);
insert into northwind.orders
values (
  11042, 
  'COMMI', 
  2, 
  '1998-04-22', 
  '1998-05-06', 
  '1998-05-01', 
  1, 
  29.9899998, 
  'Comércio Mineiro', 
  'Av. dos Lusíadas, 23', 
  'Sao Paulo', 
  'SP', 
  '05432-043', 
  'Brazil'
);
insert into northwind.orders
values (
  11043, 
  'SPECD', 
  5, 
  '1998-04-22', 
  '1998-05-20', 
  '1998-04-29', 
  2, 
  8.80000019, 
  'Spécialités du monde', 
  '25, rue Lauriston', 
  'Paris', 
  null, 
  '75016', 
  'France'
);
insert into northwind.orders
values (
  11044, 
  'WOLZA', 
  4, 
  '1998-04-23', 
  '1998-05-21', 
  '1998-05-01', 
  1, 
  8.72000027, 
  'Wolski Zajazd', 
  'ul. Filtrowa 68', 
  'Warszawa', 
  null, 
  '01-012', 
  'Poland'
);
insert into northwind.orders
values (
  11045, 
  'BOTTM', 
  6, 
  '1998-04-23', 
  '1998-05-21', 
  null, 
  2, 
  70.5800018, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  11046, 
  'WANDK', 
  8, 
  '1998-04-23', 
  '1998-05-21', 
  '1998-04-24', 
  2, 
  71.6399994, 
  'Die Wandernde Kuh', 
  'Adenauerallee 900', 
  'Stuttgart', 
  null, 
  '70563', 
  'Germany'
);
insert into northwind.orders
values (
  11047, 
  'EASTC', 
  7, 
  '1998-04-24', 
  '1998-05-22', 
  '1998-05-01', 
  3, 
  46.6199989, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  11048, 
  'BOTTM', 
  7, 
  '1998-04-24', 
  '1998-05-22', 
  '1998-04-30', 
  3, 
  24.1200008, 
  'Bottom-Dollar Markets', 
  '23 Tsawassen Blvd.', 
  'Tsawassen', 
  'BC', 
  'T2F 8M4', 
  'Canada'
);
insert into northwind.orders
values (
  11049, 
  'GOURL', 
  3, 
  '1998-04-24', 
  '1998-05-22', 
  '1998-05-04', 
  1, 
  8.34000015, 
  'Gourmet Lanchonetes', 
  'Av. Brasil, 442', 
  'Campinas', 
  'SP', 
  '04876-786', 
  'Brazil'
);
insert into northwind.orders
values (
  11050, 
  'FOLKO', 
  8, 
  '1998-04-27', 
  '1998-05-25', 
  '1998-05-05', 
  2, 
  59.4099998, 
  'Folk och fä HB', 
  'Åkergatan 24', 
  'Bräcke', 
  null, 
  'S-844 67', 
  'Sweden'
);
insert into northwind.orders
values (
  11051, 
  'LAMAI', 
  7, 
  '1998-04-27', 
  '1998-05-25', 
  null, 
  3, 
  2.78999996, 
  'La maison d''Asie', 
  '1 rue Alsace-Lorraine', 
  'Toulouse', 
  null, 
  '31000', 
  'France'
);
insert into northwind.orders
values (
  11052, 
  'HANAR', 
  3, 
  '1998-04-27', 
  '1998-05-25', 
  '1998-05-01', 
  1, 
  67.2600021, 
  'Hanari Carnes', 
  'Rua do Paço, 67', 
  'Rio de Janeiro', 
  'RJ', 
  '05454-876', 
  'Brazil'
);
insert into northwind.orders
values (
  11053, 
  'PICCO', 
  2, 
  '1998-04-27', 
  '1998-05-25', 
  '1998-04-29', 
  2, 
  53.0499992, 
  'Piccolo und mehr', 
  'Geislweg 14', 
  'Salzburg', 
  null, 
  '5020', 
  'Austria'
);
insert into northwind.orders
values (
  11054, 
  'CACTU', 
  8, 
  '1998-04-28', 
  '1998-05-26', 
  null, 
  1, 
  0.330000013, 
  'Cactus Comidas para llevar', 
  'Cerrito 333', 
  'Buenos Aires', 
  null, 
  '1010', 
  'Argentina'
);
insert into northwind.orders
values (
  11055, 
  'HILAA', 
  7, 
  '1998-04-28', 
  '1998-05-26', 
  '1998-05-05', 
  2, 
  120.919998, 
  'HILARION-Abastos', 
  'Carrera 22 con Ave. Carlos Soublette #8-35', 
  'San Cristóbal', 
  'Táchira', 
  '5022', 
  'Venezuela'
);
insert into northwind.orders
values (
  11056, 
  'EASTC', 
  8, 
  '1998-04-28', 
  '1998-05-12', 
  '1998-05-01', 
  2, 
  278.959991, 
  'Eastern Connection', 
  '35 King George', 
  'London', 
  null, 
  'WX3 6FW', 
  'UK'
);
insert into northwind.orders
values (
  11057, 
  'NORTS', 
  3, 
  '1998-04-29', 
  '1998-05-27', 
  '1998-05-01', 
  3, 
  4.13000011, 
  'North/South', 
  'South House 300 Queensbridge', 
  'London', 
  null, 
  'SW7 1RZ', 
  'UK'
);
insert into northwind.orders
values (
  11058, 
  'BLAUS', 
  9, 
  '1998-04-29', 
  '1998-05-27', 
  null, 
  3, 
  31.1399994, 
  'Blauer See Delikatessen', 
  'Forsterstr. 57', 
  'Mannheim', 
  null, 
  '68306', 
  'Germany'
);
insert into northwind.orders
values (
  11059, 
  'RICAR', 
  2, 
  '1998-04-29', 
  '1998-06-10', 
  null, 
  2, 
  85.8000031, 
  'Ricardo Adocicados', 
  'Av. Copacabana, 267', 
  'Rio de Janeiro', 
  'RJ', 
  '02389-890', 
  'Brazil'
);
insert into northwind.orders
values (
  11060, 
  'FRANS', 
  2, 
  '1998-04-30', 
  '1998-05-28', 
  '1998-05-04', 
  2, 
  10.9799995, 
  'Franchi S.p.A.', 
  'Via Monte Bianco 34', 
  'Torino', 
  null, 
  '10100', 
  'Italy'
);
insert into northwind.orders
values (
  11061, 
  'GREAL', 
  4, 
  '1998-04-30', 
  '1998-06-11', 
  null, 
  3, 
  14.0100002, 
  'Great Lakes Food Market', 
  '2732 Baker Blvd.', 
  'Eugene', 
  'OR', 
  '97403', 
  'USA'
);
insert into northwind.orders
values (
  11062, 
  'REGGC', 
  4, 
  '1998-04-30', 
  '1998-05-28', 
  null, 
  2, 
  29.9300003, 
  'Reggiani Caseifici', 
  'Strada Provinciale 124', 
  'Reggio Emilia', 
  null, 
  '42100', 
  'Italy'
);
insert into northwind.orders
values (
  11063, 
  'HUNGO', 
  3, 
  '1998-04-30', 
  '1998-05-28', 
  '1998-05-06', 
  2, 
  81.7300034, 
  'Hungry Owl All-Night Grocers', 
  '8 Johnstown Road', 
  'Cork', 
  'Co. Cork', 
  null, 
  'Ireland'
);
insert into northwind.orders
values (
  11064, 
  'SAVEA', 
  1, 
  '1998-05-01', 
  '1998-05-29', 
  '1998-05-04', 
  1, 
  30.0900002, 
  'Save-a-lot Markets', 
  '187 Suffolk Ln.', 
  'Boise', 
  'ID', 
  '83720', 
  'USA'
);
insert into northwind.orders
values (
  11065, 
  'LILAS', 
  8, 
  '1998-05-01', 
  '1998-05-29', 
  null, 
  1, 
  12.9099998, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  11066, 
  'WHITC', 
  7, 
  '1998-05-01', 
  '1998-05-29', 
  '1998-05-04', 
  2, 
  44.7200012, 
  'White Clover Markets', 
  '1029 - 12th Ave. S.', 
  'Seattle', 
  'WA', 
  '98124', 
  'USA'
);
insert into northwind.orders
values (
  11067, 
  'DRACD', 
  1, 
  '1998-05-04', 
  '1998-05-18', 
  '1998-05-06', 
  2, 
  7.98000002, 
  'Drachenblut Delikatessen', 
  'Walserweg 21', 
  'Aachen', 
  null, 
  '52066', 
  'Germany'
);
insert into northwind.orders
values (
  11068, 
  'QUEEN', 
  8, 
  '1998-05-04', 
  '1998-06-01', 
  null, 
  2, 
  81.75, 
  'Queen Cozinha', 
  'Alameda dos Canàrios, 891', 
  'Sao Paulo', 
  'SP', 
  '05487-020', 
  'Brazil'
);
insert into northwind.orders
values (
  11069, 
  'TORTU', 
  1, 
  '1998-05-04', 
  '1998-06-01', 
  '1998-05-06', 
  2, 
  15.6700001, 
  'Tortuga Restaurante', 
  'Avda. Azteca 123', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  11070, 
  'LEHMS', 
  2, 
  '1998-05-05', 
  '1998-06-02', 
  null, 
  1, 
  136, 
  'Lehmanns Marktstand', 
  'Magazinweg 7', 
  'Frankfurt a.M.', 
  null, 
  '60528', 
  'Germany'
);
insert into northwind.orders
values (
  11071, 
  'LILAS', 
  1, 
  '1998-05-05', 
  '1998-06-02', 
  null, 
  1, 
  0.930000007, 
  'LILA-Supermercado', 
  'Carrera 52 con Ave. Bolívar #65-98 Llano Largo', 
  'Barquisimeto', 
  'Lara', 
  '3508', 
  'Venezuela'
);
insert into northwind.orders
values (
  11072, 
  'ERNSH', 
  4, 
  '1998-05-05', 
  '1998-06-02', 
  null, 
  2, 
  258.640015, 
  'Ernst Handel', 
  'Kirchgasse 6', 
  'Graz', 
  null, 
  '8010', 
  'Austria'
);
insert into northwind.orders
values (
  11073, 
  'PERIC', 
  2, 
  '1998-05-05', 
  '1998-06-02', 
  null, 
  2, 
  24.9500008, 
  'Pericles Comidas clásicas', 
  'Calle Dr. Jorge Cash 321', 
  'México D.F.', 
  null, 
  '05033', 
  'Mexico'
);
insert into northwind.orders
values (
  11074, 
  'SIMOB', 
  7, 
  '1998-05-06', 
  '1998-06-03', 
  null, 
  2, 
  18.4400005, 
  'Simons bistro', 
  'Vinbæltet 34', 
  'Kobenhavn', 
  null, 
  '1734', 
  'Denmark'
);
insert into northwind.orders
values (
  11075, 
  'RICSU', 
  8, 
  '1998-05-06', 
  '1998-06-03', 
  null, 
  2, 
  6.19000006, 
  'Richter Supermarkt', 
  'Starenweg 5', 
  'Genève', 
  null, 
  '1204', 
  'Switzerland'
);
insert into northwind.orders
values (
  11076, 
  'BONAP', 
  4, 
  '1998-05-06', 
  '1998-06-03', 
  null, 
  2, 
  38.2799988, 
  'Bon app''', 
  '12, rue des Bouchers', 
  'Marseille', 
  null, 
  '13008', 
  'France'
);
insert into northwind.orders
values (
  11077, 
  'RATTC', 
  1, 
  '1998-05-06', 
  '1998-06-03', 
  null, 
  2, 
  8.52999973, 
  'Rattlesnake Canyon Grocery', 
  '2817 Milton Dr.', 
  'Albuquerque', 
  'NM', 
  '87110', 
  'USA'
);
insert into northwind.products
values (
  1, 
  'Chai', 
  8, 
  1, 
  '10 boxes x 30 bags', 
  18, 
  39, 
  0, 
  10, 
  1
);
insert into northwind.products
values (
  2, 
  'Chang', 
  1, 
  1, 
  '24 - 12 oz bottles', 
  19, 
  17, 
  40, 
  25, 
  1
);
insert into northwind.products
values (
  3, 
  'Aniseed Syrup', 
  1, 
  2, 
  '12 - 550 ml bottles', 
  10, 
  13, 
  70, 
  25, 
  0
);
insert into northwind.products
values (
  4, 
  'Chef Anton''s Cajun Seasoning', 
  2, 
  2, 
  '48 - 6 oz jars', 
  22, 
  53, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  5, 
  'Chef Anton''s Gumbo Mix', 
  2, 
  2, 
  '36 boxes', 
  21.3500004, 
  0, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  6, 
  'Grandma''s Boysenberry Spread', 
  3, 
  2, 
  '12 - 8 oz jars', 
  25, 
  120, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  7, 
  'Uncle Bob''s Organic Dried Pears', 
  3, 
  7, 
  '12 - 1 lb pkgs.', 
  30, 
  15, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  8, 
  'Northwoods Cranberry Sauce', 
  3, 
  2, 
  '12 - 12 oz jars', 
  40, 
  6, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  9, 
  'Mishi Kobe Niku', 
  4, 
  6, 
  '18 - 500 g pkgs.', 
  97, 
  29, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  10, 
  'Ikura', 
  4, 
  8, 
  '12 - 200 ml jars', 
  31, 
  31, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  11, 
  'Queso Cabrales', 
  5, 
  4, 
  '1 kg pkg.', 
  21, 
  22, 
  30, 
  30, 
  0
);
insert into northwind.products
values (
  12, 
  'Queso Manchego La Pastora', 
  5, 
  4, 
  '10 - 500 g pkgs.', 
  38, 
  86, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  13, 
  'Konbu', 
  6, 
  8, 
  '2 kg box', 
  6, 
  24, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  14, 
  'Tofu', 
  6, 
  7, 
  '40 - 100 g pkgs.', 
  23.25, 
  35, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  15, 
  'Genen Shouyu', 
  6, 
  2, 
  '24 - 250 ml bottles', 
  13, 
  39, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  16, 
  'Pavlova', 
  7, 
  3, 
  '32 - 500 g boxes', 
  17.4500008, 
  29, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  17, 
  'Alice Mutton', 
  7, 
  6, 
  '20 - 1 kg tins', 
  39, 
  0, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  18, 
  'Carnarvon Tigers', 
  7, 
  8, 
  '16 kg pkg.', 
  62.5, 
  42, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  19, 
  'Teatime Chocolate Biscuits', 
  8, 
  3, 
  '10 boxes x 12 pieces', 
  9.19999981, 
  25, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  20, 
  'Sir Rodney''s Marmalade', 
  8, 
  3, 
  '30 gift boxes', 
  81, 
  40, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  21, 
  'Sir Rodney''s Scones', 
  8, 
  3, 
  '24 pkgs. x 4 pieces', 
  10, 
  3, 
  40, 
  5, 
  0
);
insert into northwind.products
values (
  22, 
  'Gustaf''s Knäckebröd', 
  9, 
  5, 
  '24 - 500 g pkgs.', 
  21, 
  104, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  23, 
  'Tunnbröd', 
  9, 
  5, 
  '12 - 250 g pkgs.', 
  9, 
  61, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  24, 
  'Guaraná Fantástica', 
  10, 
  1, 
  '12 - 355 ml cans', 
  4.5, 
  20, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  25, 
  'NuNuCa Nuß-Nougat-Creme', 
  11, 
  3, 
  '20 - 450 g glasses', 
  14, 
  76, 
  0, 
  30, 
  0
);
insert into northwind.products
values (
  26, 
  'Gumbär Gummibärchen', 
  11, 
  3, 
  '100 - 250 g bags', 
  31.2299995, 
  15, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  27, 
  'Schoggi Schokolade', 
  11, 
  3, 
  '100 - 100 g pieces', 
  43.9000015, 
  49, 
  0, 
  30, 
  0
);
insert into northwind.products
values (
  28, 
  'Rössle Sauerkraut', 
  12, 
  7, 
  '25 - 825 g cans', 
  45.5999985, 
  26, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  29, 
  'Thüringer Rostbratwurst', 
  12, 
  6, 
  '50 bags x 30 sausgs.', 
  123.790001, 
  0, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  30, 
  'Nord-Ost Matjeshering', 
  13, 
  8, 
  '10 - 200 g glasses', 
  25.8899994, 
  10, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  31, 
  'Gorgonzola Telino', 
  14, 
  4, 
  '12 - 100 g pkgs', 
  12.5, 
  0, 
  70, 
  20, 
  0
);
insert into northwind.products
values (
  32, 
  'Mascarpone Fabioli', 
  14, 
  4, 
  '24 - 200 g pkgs.', 
  32, 
  9, 
  40, 
  25, 
  0
);
insert into northwind.products
values (
  33, 
  'Geitost', 
  15, 
  4, 
  '500 g', 
  2.5, 
  112, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  34, 
  'Sasquatch Ale', 
  16, 
  1, 
  '24 - 12 oz bottles', 
  14, 
  111, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  35, 
  'Steeleye Stout', 
  16, 
  1, 
  '24 - 12 oz bottles', 
  18, 
  20, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  36, 
  'Inlagd Sill', 
  17, 
  8, 
  '24 - 250 g  jars', 
  19, 
  112, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  37, 
  'Gravad lax', 
  17, 
  8, 
  '12 - 500 g pkgs.', 
  26, 
  11, 
  50, 
  25, 
  0
);
insert into northwind.products
values (
  38, 
  'Côte de Blaye', 
  18, 
  1, 
  '12 - 75 cl bottles', 
  263.5, 
  17, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  39, 
  'Chartreuse verte', 
  18, 
  1, 
  '750 cc per bottle', 
  18, 
  69, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  40, 
  'Boston Crab Meat', 
  19, 
  8, 
  '24 - 4 oz tins', 
  18.3999996, 
  123, 
  0, 
  30, 
  0
);
insert into northwind.products
values (
  41, 
  'Jack''s New England Clam Chowder', 
  19, 
  8, 
  '12 - 12 oz cans', 
  9.64999962, 
  85, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  42, 
  'Singaporean Hokkien Fried Mee', 
  20, 
  5, 
  '32 - 1 kg pkgs.', 
  14, 
  26, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  43, 
  'Ipoh Coffee', 
  20, 
  1, 
  '16 - 500 g tins', 
  46, 
  17, 
  10, 
  25, 
  0
);
insert into northwind.products
values (
  44, 
  'Gula Malacca', 
  20, 
  2, 
  '20 - 2 kg bags', 
  19.4500008, 
  27, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  45, 
  'Rogede sild', 
  21, 
  8, 
  '1k pkg.', 
  9.5, 
  5, 
  70, 
  15, 
  0
);
insert into northwind.products
values (
  46, 
  'Spegesild', 
  21, 
  8, 
  '4 - 450 g glasses', 
  12, 
  95, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  47, 
  'Zaanse koeken', 
  22, 
  3, 
  '10 - 4 oz boxes', 
  9.5, 
  36, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  48, 
  'Chocolade', 
  22, 
  3, 
  '10 pkgs.', 
  12.75, 
  15, 
  70, 
  25, 
  0
);
insert into northwind.products
values (
  49, 
  'Maxilaku', 
  23, 
  3, 
  '24 - 50 g pkgs.', 
  20, 
  10, 
  60, 
  15, 
  0
);
insert into northwind.products
values (
  50, 
  'Valkoinen suklaa', 
  23, 
  3, 
  '12 - 100 g bars', 
  16.25, 
  65, 
  0, 
  30, 
  0
);
insert into northwind.products
values (
  51, 
  'Manjimup Dried Apples', 
  24, 
  7, 
  '50 - 300 g pkgs.', 
  53, 
  20, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  52, 
  'Filo Mix', 
  24, 
  5, 
  '16 - 2 kg boxes', 
  7, 
  38, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  53, 
  'Perth Pasties', 
  24, 
  6, 
  '48 pieces', 
  32.7999992, 
  0, 
  0, 
  0, 
  1
);
insert into northwind.products
values (
  54, 
  'Tourtière', 
  25, 
  6, 
  '16 pies', 
  7.44999981, 
  21, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  55, 
  'Pâté chinois', 
  25, 
  6, 
  '24 boxes x 2 pies', 
  24, 
  115, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  56, 
  'Gnocchi di nonna Alice', 
  26, 
  5, 
  '24 - 250 g pkgs.', 
  38, 
  21, 
  10, 
  30, 
  0
);
insert into northwind.products
values (
  57, 
  'Ravioli Angelo', 
  26, 
  5, 
  '24 - 250 g pkgs.', 
  19.5, 
  36, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  58, 
  'Escargots de Bourgogne', 
  27, 
  8, 
  '24 pieces', 
  13.25, 
  62, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  59, 
  'Raclette Courdavault', 
  28, 
  4, 
  '5 kg pkg.', 
  55, 
  79, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  60, 
  'Camembert Pierrot', 
  28, 
  4, 
  '15 - 300 g rounds', 
  34, 
  19, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  61, 
  'Sirop d''érable', 
  29, 
  2, 
  '24 - 500 ml bottles', 
  28.5, 
  113, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  62, 
  'Tarte au sucre', 
  29, 
  3, 
  '48 pies', 
  49.2999992, 
  17, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  63, 
  'Vegie-spread', 
  7, 
  2, 
  '15 - 625 g jars', 
  43.9000015, 
  24, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  64, 
  'Wimmers gute Semmelknödel', 
  12, 
  5, 
  '20 bags x 4 pieces', 
  33.25, 
  22, 
  80, 
  30, 
  0
);
insert into northwind.products
values (
  65, 
  'Louisiana Fiery Hot Pepper Sauce', 
  2, 
  2, 
  '32 - 8 oz bottles', 
  21.0499992, 
  76, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  66, 
  'Louisiana Hot Spiced Okra', 
  2, 
  2, 
  '24 - 8 oz jars', 
  17, 
  4, 
  100, 
  20, 
  0
);
insert into northwind.products
values (
  67, 
  'Laughing Lumberjack Lager', 
  16, 
  1, 
  '24 - 12 oz bottles', 
  14, 
  52, 
  0, 
  10, 
  0
);
insert into northwind.products
values (
  68, 
  'Scottish Longbreads', 
  8, 
  3, 
  '10 boxes x 8 pieces', 
  12.5, 
  6, 
  10, 
  15, 
  0
);
insert into northwind.products
values (
  69, 
  'Gudbrandsdalsost', 
  15, 
  4, 
  '10 kg pkg.', 
  36, 
  26, 
  0, 
  15, 
  0
);
insert into northwind.products
values (
  70, 
  'Outback Lager', 
  7, 
  1, 
  '24 - 355 ml bottles', 
  15, 
  15, 
  10, 
  30, 
  0
);
insert into northwind.products
values (
  71, 
  'Flotemysost', 
  15, 
  4, 
  '10 - 500 g pkgs.', 
  21.5, 
  26, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  72, 
  'Mozzarella di Giovanni', 
  14, 
  4, 
  '24 - 200 g pkgs.', 
  34.7999992, 
  14, 
  0, 
  0, 
  0
);
insert into northwind.products
values (
  73, 
  'Röd Kaviar', 
  17, 
  8, 
  '24 - 150 g jars', 
  15, 
  101, 
  0, 
  5, 
  0
);
insert into northwind.products
values (
  74, 
  'Longlife Tofu', 
  4, 
  7, 
  '5 kg pkg.', 
  10, 
  4, 
  20, 
  5, 
  0
);
insert into northwind.products
values (
  75, 
  'Rhönbräu Klosterbier', 
  12, 
  1, 
  '24 - 0.5 l bottles', 
  7.75, 
  125, 
  0, 
  25, 
  0
);
insert into northwind.products
values (
  76, 
  'Lakkalikööri', 
  23, 
  1, 
  '500 ml', 
  18, 
  57, 
  0, 
  20, 
  0
);
insert into northwind.products
values (
  77, 
  'Original Frankfurter grüne Soße', 
  12, 
  2, 
  '12 boxes', 
  13, 
  32, 
  0, 
  15, 
  0
);
insert into northwind.region
values (
  1, 
  'Eastern'
);
insert into northwind.region
values (
  2, 
  'Western'
);
insert into northwind.region
values (
  3, 
  'Northern'
);
insert into northwind.region
values (
  4, 
  'Southern'
);
insert into northwind.shippers
values (
  1, 
  'Speedy Express', 
  '(503) 555-9831'
);
insert into northwind.shippers
values (
  2, 
  'United Package', 
  '(503) 555-3199'
);
insert into northwind.shippers
values (
  3, 
  'Federal Shipping', 
  '(503) 555-9931'
);
insert into northwind.shippers
values (
  4, 
  'Alliance Shippers', 
  '1-800-222-0451'
);
insert into northwind.shippers
values (
  5, 
  'UPS', 
  '1-800-782-7892'
);
insert into northwind.shippers
values (
  6, 
  'DHL', 
  '1-800-225-5345'
);
insert into northwind.suppliers
values (
  1, 
  'Exotic Liquids', 
  'Charlotte Cooper', 
  'Purchasing Manager', 
  '49 Gilbert St.', 
  'London', 
  null, 
  'EC1 4SD', 
  'UK', 
  '(171) 555-2222', 
  null, 
  null
);
insert into northwind.suppliers
values (
  2, 
  'New Orleans Cajun Delights', 
  'Shelley Burke', 
  'Order Administrator', 
  'P.O. Box 78934', 
  'New Orleans', 
  'LA', 
  '70117', 
  'USA', 
  '(100) 555-4822', 
  null, 
  '#CAJUN.HTM#'
);
insert into northwind.suppliers
values (
  3, 
  'Grandma Kelly''s Homestead', 
  'Regina Murphy', 
  'Sales Representative', 
  '707 Oxford Rd.', 
  'Ann Arbor', 
  'MI', 
  '48104', 
  'USA', 
  '(313) 555-5735', 
  '(313) 555-3349', 
  null
);
insert into northwind.suppliers
values (
  4, 
  'Tokyo Traders', 
  'Yoshi Nagase', 
  'Marketing Manager', 
  '9-8 Sekimai Musashino-shi', 
  'Tokyo', 
  null, 
  '100', 
  'Japan', 
  '(03) 3555-5011', 
  null, 
  null
);
insert into northwind.suppliers
values (
  5, 
  'Cooperativa de Quesos ''Las Cabras''', 
  'Antonio del Valle Saavedra', 
  'Export Administrator', 
  'Calle del Rosal 4', 
  'Oviedo', 
  'Asturias', 
  '33007', 
  'Spain', 
  '(98) 598 76 54', 
  null, 
  null
);
insert into northwind.suppliers
values (
  6, 
  'Mayumi''s', 
  'Mayumi Ohno', 
  'Marketing Representative', 
  '92 Setsuko Chuo-ku', 
  'Osaka', 
  null, 
  '545', 
  'Japan', 
  '(06) 431-7877', 
  null, 
  'Mayumi''s (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/mayumi.htm#'
);
insert into northwind.suppliers
values (
  7, 
  'Pavlova, Ltd.', 
  'Ian Devling', 
  'Marketing Manager', 
  '74 Rose St. Moonie Ponds', 
  'Melbourne', 
  'Victoria', 
  '3058', 
  'Australia', 
  '(03) 444-2343', 
  '(03) 444-6588', 
  null
);
insert into northwind.suppliers
values (
  8, 
  'Specialty Biscuits, Ltd.', 
  'Peter Wilson', 
  'Sales Representative', 
  '29 King''s Way', 
  'Manchester', 
  null, 
  'M14 GSD', 
  'UK', 
  '(161) 555-4448', 
  null, 
  null
);
insert into northwind.suppliers
values (
  9, 
  'PB Knäckebröd AB', 
  'Lars Peterson', 
  'Sales Agent', 
  'Kaloadagatan 13', 
  'Göteborg', 
  null, 
  'S-345 67', 
  'Sweden', 
  '031-987 65 43', 
  '031-987 65 91', 
  null
);
insert into northwind.suppliers
values (
  10, 
  'Refrescos Americanas LTDA', 
  'Carlos Diaz', 
  'Marketing Manager', 
  'Av. das Americanas 12.890', 
  'Sao Paulo', 
  null, 
  '5442', 
  'Brazil', 
  '(11) 555 4640', 
  null, 
  null
);
insert into northwind.suppliers
values (
  11, 
  'Heli Süßwaren GmbH & Co. KG', 
  'Petra Winkler', 
  'Sales Manager', 
  'Tiergartenstraße 5', 
  'Berlin', 
  null, 
  '10785', 
  'Germany', 
  '(010) 9984510', 
  null, 
  null
);
insert into northwind.suppliers
values (
  12, 
  'Plutzer Lebensmittelgroßmärkte AG', 
  'Martin Bein', 
  'International Marketing Mgr.', 
  'Bogenallee 51', 
  'Frankfurt', 
  null, 
  '60439', 
  'Germany', 
  '(069) 992755', 
  null, 
  'Plutzer (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/plutzer.htm#'
);
insert into northwind.suppliers
values (
  13, 
  'Nord-Ost-Fisch Handelsgesellschaft mbH', 
  'Sven Petersen', 
  'Coordinator Foreign Markets', 
  'Frahmredder 112a', 
  'Cuxhaven', 
  null, 
  '27478', 
  'Germany', 
  '(04721) 8713', 
  '(04721) 8714', 
  null
);
insert into northwind.suppliers
values (
  14, 
  'Formaggi Fortini s.r.l.', 
  'Elio Rossi', 
  'Sales Representative', 
  'Viale Dante, 75', 
  'Ravenna', 
  null, 
  '48100', 
  'Italy', 
  '(0544) 60323', 
  '(0544) 60603', 
  '#FORMAGGI.HTM#'
);
insert into northwind.suppliers
values (
  15, 
  'Norske Meierier', 
  'Beate Vileid', 
  'Marketing Manager', 
  'Hatlevegen 5', 
  'Sandvika', 
  null, 
  '1320', 
  'Norway', 
  '(0)2-953010', 
  null, 
  null
);
insert into northwind.suppliers
values (
  16, 
  'Bigfoot Breweries', 
  'Cheryl Saylor', 
  'Regional Account Rep.', 
  '3400 - 8th Avenue Suite 210', 
  'Bend', 
  'OR', 
  '97101', 
  'USA', 
  '(503) 555-9931', 
  null, 
  null
);
insert into northwind.suppliers
values (
  17, 
  'Svensk Sjöföda AB', 
  'Michael Björn', 
  'Sales Representative', 
  'Brovallavägen 231', 
  'Stockholm', 
  null, 
  'S-123 45', 
  'Sweden', 
  '08-123 45 67', 
  null, 
  null
);
insert into northwind.suppliers
values (
  18, 
  'Aux joyeux ecclésiastiques', 
  'Guylène Nodier', 
  'Sales Manager', 
  '203, Rue des Francs-Bourgeois', 
  'Paris', 
  null, 
  '75004', 
  'France', 
  '(1) 03.83.00.68', 
  '(1) 03.83.00.62', 
  null
);
insert into northwind.suppliers
values (
  19, 
  'New England Seafood Cannery', 
  'Robb Merchant', 
  'Wholesale Account Agent', 
  'Order Processing Dept. 2100 Paul Revere Blvd.', 
  'Boston', 
  'MA', 
  '02134', 
  'USA', 
  '(617) 555-3267', 
  '(617) 555-3389', 
  null
);
insert into northwind.suppliers
values (
  20, 
  'Leka Trading', 
  'Chandra Leka', 
  'Owner', 
  '471 Serangoon Loop, Suite #402', 
  'Singapore', 
  null, 
  '0512', 
  'Singapore', 
  '555-8787', 
  null, 
  null
);
insert into northwind.suppliers
values (
  21, 
  'Lyngbysild', 
  'Niels Petersen', 
  'Sales Manager', 
  'Lyngbysild Fiskebakken 10', 
  'Lyngby', 
  null, 
  '2800', 
  'Denmark', 
  '43844108', 
  '43844115', 
  null
);
insert into northwind.suppliers
values (
  22, 
  'Zaanse Snoepfabriek', 
  'Dirk Luchte', 
  'Accounting Manager', 
  'Verkoop Rijnweg 22', 
  'Zaandam', 
  null, 
  '9999 ZZ', 
  'Netherlands', 
  '(12345) 1212', 
  '(12345) 1210', 
  null
);
insert into northwind.suppliers
values (
  23, 
  'Karkki Oy', 
  'Anne Heikkonen', 
  'Product Manager', 
  'Valtakatu 12', 
  'Lappeenranta', 
  null, 
  '53120', 
  'Finland', 
  '(953) 10956', 
  null, 
  null
);
insert into northwind.suppliers
values (
  24, 
  'G''day, Mate', 
  'Wendy Mackenzie', 
  'Sales Representative', 
  '170 Prince Edward Parade Hunter''s Hill', 
  'Sydney', 
  'NSW', 
  '2042', 
  'Australia', 
  '(02) 555-5914', 
  '(02) 555-4873', 
  'G''day Mate (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/gdaymate.htm#'
);
insert into northwind.suppliers
values (
  25, 
  'Ma Maison', 
  'Jean-Guy Lauzon', 
  'Marketing Manager', 
  '2960 Rue St. Laurent', 
  'Montréal', 
  'Québec', 
  'H1J 1C3', 
  'Canada', 
  '(514) 555-9022', 
  null, 
  null
);
insert into northwind.suppliers
values (
  26, 
  'Pasta Buttini s.r.l.', 
  'Giovanni Giudici', 
  'Order Administrator', 
  'Via dei Gelsomini, 153', 
  'Salerno', 
  null, 
  '84100', 
  'Italy', 
  '(089) 6547665', 
  '(089) 6547667', 
  null
);
insert into northwind.suppliers
values (
  27, 
  'Escargots Nouveaux', 
  'Marie Delamare', 
  'Sales Manager', 
  '22, rue H. Voiron', 
  'Montceau', 
  null, 
  '71300', 
  'France', 
  '85.57.00.07', 
  null, 
  null
);
insert into northwind.suppliers
values (
  28, 
  'Gai pâturage', 
  'Eliane Noz', 
  'Sales Representative', 
  'Bat. B 3, rue des Alpes', 
  'Annecy', 
  null, 
  '74000', 
  'France', 
  '38.76.98.06', 
  '38.76.98.58', 
  null
);
insert into northwind.suppliers
values (
  29, 
  'Forêts d''érables', 
  'Chantal Goulet', 
  'Accounting Manager', 
  '148 rue Chasseur', 
  'Ste-Hyacinthe', 
  'Québec', 
  'J2S 7S8', 
  'Canada', 
  '(514) 555-2955', 
  '(514) 555-2921', 
  null
);
insert into northwind.territories
values (
  '01581', 
  'Westboro', 
  1
);
insert into northwind.territories
values (
  '01730', 
  'Bedford', 
  1
);
insert into northwind.territories
values (
  '01833', 
  'Georgetow', 
  1
);
insert into northwind.territories
values (
  '02116', 
  'Boston', 
  1
);
insert into northwind.territories
values (
  '02139', 
  'Cambridge', 
  1
);
insert into northwind.territories
values (
  '02184', 
  'Braintree', 
  1
);
insert into northwind.territories
values (
  '02903', 
  'Providence', 
  1
);
insert into northwind.territories
values (
  '03049', 
  'Hollis', 
  3
);
insert into northwind.territories
values (
  '03801', 
  'Portsmouth', 
  3
);
insert into northwind.territories
values (
  '06897', 
  'Wilton', 
  1
);
insert into northwind.territories
values (
  '07960', 
  'Morristown', 
  1
);
insert into northwind.territories
values (
  '08837', 
  'Edison', 
  1
);
insert into northwind.territories
values (
  '10019', 
  'New York', 
  1
);
insert into northwind.territories
values (
  '10038', 
  'New York', 
  1
);
insert into northwind.territories
values (
  '11747', 
  'Mellvile', 
  1
);
insert into northwind.territories
values (
  '14450', 
  'Fairport', 
  1
);
insert into northwind.territories
values (
  '19428', 
  'Philadelphia', 
  3
);
insert into northwind.territories
values (
  '19713', 
  'Neward', 
  1
);
insert into northwind.territories
values (
  '20852', 
  'Rockville', 
  1
);
insert into northwind.territories
values (
  '27403', 
  'Greensboro', 
  1
);
insert into northwind.territories
values (
  '27511', 
  'Cary', 
  1
);
insert into northwind.territories
values (
  '29202', 
  'Columbia', 
  4
);
insert into northwind.territories
values (
  '30346', 
  'Atlanta', 
  4
);
insert into northwind.territories
values (
  '31406', 
  'Savannah', 
  4
);
insert into northwind.territories
values (
  '32859', 
  'Orlando', 
  4
);
insert into northwind.territories
values (
  '33607', 
  'Tampa', 
  4
);
insert into northwind.territories
values (
  '40222', 
  'Louisville', 
  1
);
insert into northwind.territories
values (
  '44122', 
  'Beachwood', 
  3
);
insert into northwind.territories
values (
  '45839', 
  'Findlay', 
  3
);
insert into northwind.territories
values (
  '48075', 
  'Southfield', 
  3
);
insert into northwind.territories
values (
  '48084', 
  'Troy', 
  3
);
insert into northwind.territories
values (
  '48304', 
  'Bloomfield Hills', 
  3
);
insert into northwind.territories
values (
  '53404', 
  'Racine', 
  3
);
insert into northwind.territories
values (
  '55113', 
  'Roseville', 
  3
);
insert into northwind.territories
values (
  '55439', 
  'Minneapolis', 
  3
);
insert into northwind.territories
values (
  '60179', 
  'Hoffman Estates', 
  2
);
insert into northwind.territories
values (
  '60601', 
  'Chicago', 
  2
);
insert into northwind.territories
values (
  '72716', 
  'Bentonville', 
  4
);
insert into northwind.territories
values (
  '75234', 
  'Dallas', 
  4
);
insert into northwind.territories
values (
  '78759', 
  'Austin', 
  4
);
insert into northwind.territories
values (
  '80202', 
  'Denver', 
  2
);
insert into northwind.territories
values (
  '80909', 
  'Colorado Springs', 
  2
);
insert into northwind.territories
values (
  '85014', 
  'Phoenix', 
  2
);
insert into northwind.territories
values (
  '85251', 
  'Scottsdale', 
  2
);
insert into northwind.territories
values (
  '90405', 
  'Santa Monica', 
  2
);
insert into northwind.territories
values (
  '94025', 
  'Menlo Park', 
  2
);
insert into northwind.territories
values (
  '94105', 
  'San Francisco', 
  2
);
insert into northwind.territories
values (
  '95008', 
  'Campbell', 
  2
);
insert into northwind.territories
values (
  '95054', 
  'Santa Clara', 
  2
);
insert into northwind.territories
values (
  '95060', 
  'Santa Cruz', 
  2
);
insert into northwind.territories
values (
  '98004', 
  'Bellevue', 
  2
);
insert into northwind.territories
values (
  '98052', 
  'Redmond', 
  2
);
insert into northwind.territories
values (
  '98104', 
  'Seattle', 
  2
);
insert into northwind.us_states
values (
  1, 
  'Alabama', 
  'AL', 
  'south'
);
insert into northwind.us_states
values (
  2, 
  'Alaska', 
  'AK', 
  'north'
);
insert into northwind.us_states
values (
  3, 
  'Arizona', 
  'AZ', 
  'west'
);
insert into northwind.us_states
values (
  4, 
  'Arkansas', 
  'AR', 
  'south'
);
insert into northwind.us_states
values (
  5, 
  'California', 
  'CA', 
  'west'
);
insert into northwind.us_states
values (
  6, 
  'Colorado', 
  'CO', 
  'west'
);
insert into northwind.us_states
values (
  7, 
  'Connecticut', 
  'CT', 
  'east'
);
insert into northwind.us_states
values (
  8, 
  'Delaware', 
  'DE', 
  'east'
);
insert into northwind.us_states
values (
  9, 
  'District of Columbia', 
  'DC', 
  'east'
);
insert into northwind.us_states
values (
  10, 
  'Florida', 
  'FL', 
  'south'
);
insert into northwind.us_states
values (
  11, 
  'Georgia', 
  'GA', 
  'south'
);
insert into northwind.us_states
values (
  12, 
  'Hawaii', 
  'HI', 
  'west'
);
insert into northwind.us_states
values (
  13, 
  'Idaho', 
  'ID', 
  'midwest'
);
insert into northwind.us_states
values (
  14, 
  'Illinois', 
  'IL', 
  'midwest'
);
insert into northwind.us_states
values (
  15, 
  'Indiana', 
  'IN', 
  'midwest'
);
insert into northwind.us_states
values (
  16, 
  'Iowa', 
  'IO', 
  'midwest'
);
insert into northwind.us_states
values (
  17, 
  'Kansas', 
  'KS', 
  'midwest'
);
insert into northwind.us_states
values (
  18, 
  'Kentucky', 
  'KY', 
  'south'
);
insert into northwind.us_states
values (
  19, 
  'Louisiana', 
  'LA', 
  'south'
);
insert into northwind.us_states
values (
  20, 
  'Maine', 
  'ME', 
  'north'
);
insert into northwind.us_states
values (
  21, 
  'Maryland', 
  'MD', 
  'east'
);
insert into northwind.us_states
values (
  22, 
  'Massachusetts', 
  'MA', 
  'north'
);
insert into northwind.us_states
values (
  23, 
  'Michigan', 
  'MI', 
  'north'
);
insert into northwind.us_states
values (
  24, 
  'Minnesota', 
  'MN', 
  'north'
);
insert into northwind.us_states
values (
  25, 
  'Mississippi', 
  'MS', 
  'south'
);
insert into northwind.us_states
values (
  26, 
  'Missouri', 
  'MO', 
  'south'
);
insert into northwind.us_states
values (
  27, 
  'Montana', 
  'MT', 
  'west'
);
insert into northwind.us_states
values (
  28, 
  'Nebraska', 
  'NE', 
  'midwest'
);
insert into northwind.us_states
values (
  29, 
  'Nevada', 
  'NV', 
  'west'
);
insert into northwind.us_states
values (
  30, 
  'New Hampshire', 
  'NH', 
  'east'
);
insert into northwind.us_states
values (
  31, 
  'New Jersey', 
  'NJ', 
  'east'
);
insert into northwind.us_states
values (
  32, 
  'New Mexico', 
  'NM', 
  'west'
);
insert into northwind.us_states
values (
  33, 
  'New York', 
  'NY', 
  'east'
);
insert into northwind.us_states
values (
  34, 
  'North Carolina', 
  'NC', 
  'east'
);
insert into northwind.us_states
values (
  35, 
  'North Dakota', 
  'ND', 
  'midwest'
);
insert into northwind.us_states
values (
  36, 
  'Ohio', 
  'OH', 
  'midwest'
);
insert into northwind.us_states
values (
  37, 
  'Oklahoma', 
  'OK', 
  'midwest'
);
insert into northwind.us_states
values (
  38, 
  'Oregon', 
  'OR', 
  'west'
);
insert into northwind.us_states
values (
  39, 
  'Pennsylvania', 
  'PA', 
  'east'
);
insert into northwind.us_states
values (
  40, 
  'Rhode Island', 
  'RI', 
  'east'
);
insert into northwind.us_states
values (
  41, 
  'South Carolina', 
  'SC', 
  'east'
);
insert into northwind.us_states
values (
  42, 
  'South Dakota', 
  'SD', 
  'midwest'
);
insert into northwind.us_states
values (
  43, 
  'Tennessee', 
  'TN', 
  'midwest'
);
insert into northwind.us_states
values (
  44, 
  'Texas', 
  'TX', 
  'west'
);
insert into northwind.us_states
values (
  45, 
  'Utah', 
  'UT', 
  'west'
);
insert into northwind.us_states
values (
  46, 
  'Vermont', 
  'VT', 
  'east'
);
insert into northwind.us_states
values (
  47, 
  'Virginia', 
  'VA', 
  'east'
);
insert into northwind.us_states
values (
  48, 
  'Washington', 
  'WA', 
  'west'
);
insert into northwind.us_states
values (
  49, 
  'West Virginia', 
  'WV', 
  'south'
);
insert into northwind.us_states
values (
  50, 
  'Wisconsin', 
  'WI', 
  'midwest'
);
insert into northwind.us_states
values (
  51, 
  'Wyoming', 
  'WY', 
  'west'
);
alter table northwind.categories
  add constraint pk_categories
    primary key (category_id);
alter table northwind.customer_customer_demo
  add constraint pk_customer_customer_demo
    primary key (customer_id, customer_type_id);
alter table northwind.customer_demographics
  add constraint pk_customer_demographics
    primary key (customer_type_id);
alter table northwind.customers
  add constraint pk_customers
    primary key (customer_id);
alter table northwind.employees
  add constraint pk_employees
    primary key (employee_id);
alter table northwind.employee_territories
  add constraint pk_employee_territories
    primary key (employee_id, territory_id);
alter table northwind.order_details
  add constraint pk_order_details
    primary key (order_id, product_id);
alter table northwind.orders
  add constraint pk_orders
    primary key (order_id);
alter table northwind.products
  add constraint pk_products
    primary key (product_id);
alter table northwind.region
  add constraint pk_region
    primary key (region_id);
alter table northwind.shippers
  add constraint pk_shippers
    primary key (shipper_id);
alter table northwind.suppliers
  add constraint pk_suppliers
    primary key (supplier_id);
alter table northwind.territories
  add constraint pk_territories
    primary key (territory_id);
alter table northwind.us_states
  add constraint pk_usstates
    primary key (state_id);
alter table northwind.orders
  add constraint fk_orders_customers
    foreign key (customer_id)
    references northwind.customers;
alter table northwind.orders
  add constraint fk_orders_employees
    foreign key (employee_id)
    references northwind.employees;
alter table northwind.orders
  add constraint fk_orders_shippers
    foreign key (ship_via)
    references northwind.shippers;
alter table northwind.order_details
  add constraint fk_order_details_products
    foreign key (product_id)
    references northwind.products;
alter table northwind.order_details
  add constraint fk_order_details_orders
    foreign key (order_id)
    references northwind.orders;
alter table northwind.products
  add constraint fk_products_categories
    foreign key (category_id)
    references northwind.categories;
alter table northwind.products
  add constraint fk_products_suppliers
    foreign key (supplier_id)
    references northwind.suppliers;
alter table northwind.territories
  add constraint fk_territories_region
    foreign key (region_id)
    references northwind.region;
alter table northwind.employee_territories
  add constraint fk_employee_territories_territories
    foreign key (territory_id)
    references northwind.territories;
alter table northwind.employee_territories
  add constraint fk_employee_territories_employees
    foreign key (employee_id)
    references northwind.employees;
alter table northwind.customer_customer_demo
  add constraint fk_customer_customer_demo_customer_demographics
    foreign key (customer_type_id)
    references northwind.customer_demographics;
alter table northwind.customer_customer_demo
  add constraint fk_customer_customer_demo_customers
    foreign key (customer_id)
    references northwind.customers;
alter table northwind.employees
  add constraint fk_employees_employees
    foreign key (reports_to)
    references northwind.employees;