#!/bin/bash

CURR_DIR=$1
WORK_DIR=$CURR_DIR/generated
rm -rf $WORK_DIR
mkdir -p $WORK_DIR

# create a root CA key and cet
openssl req -x509  -sha256 -days 356 -nodes -newkey rsa:2048 -subj "/CN=localhost/C=US/L=NY"  -keyout $WORK_DIR/rootCA.key -out $WORK_DIR/rootCA.crt

# create a key for the server
openssl genrsa -out $WORK_DIR/server.key 2048

# create a CSR and create a server key signed by the root CA
openssl req -new -key $WORK_DIR/server.key -out $WORK_DIR/server.csr -config $CURR_DIR/csr.conf
openssl x509 -req  -in $WORK_DIR/server.csr  -CA $WORK_DIR/rootCA.crt -CAkey $WORK_DIR/rootCA.key  -CAcreateserial -out $WORK_DIR/server.crt  -days 365  -sha256 -extfile $CURR_DIR/cert.conf

# export the server cert into a keystore
openssl pkcs12 -export -name servercert -in $WORK_DIR/server.crt -inkey $WORK_DIR/server.key -out $WORK_DIR/serverkeystore.p12 -passout pass:changeit -passin pass:changeit
keytool -importkeystore -destkeystore $WORK_DIR/serverkeystore.jks -srckeystore $WORK_DIR/serverkeystore.p12 -srcstoretype pkcs12 -alias servercert -keypass changeit -storepass changeit -srcstorepass changeit

# export the root ca cert into a keystore
openssl pkcs12 -export -name rootcacert -in $WORK_DIR/rootCA.crt -inkey $WORK_DIR/rootCA.key -out $WORK_DIR/cakeystore.p12  -passout pass:changeit -passin pass:changeit
keytool -importkeystore -destkeystore $WORK_DIR/cakeystore.jks -srckeystore $WORK_DIR/cakeystore.p12 -srcstoretype pkcs12 -alias rootcacert -keypass changeit -storepass changeit -srcstorepass changeit