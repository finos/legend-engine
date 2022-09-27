#!/bin/bash

CURR_DIR=$1
WORK_DIR=$CURR_DIR/generated
rm -rf $WORK_DIR
mkdir -p $WORK_DIR

########## Root CA

# create a root CA key and cet
openssl req -x509  -sha256 -days 356 -nodes -newkey rsa:2048 -subj "/CN=localhost/C=US/L=NY"  -keyout $WORK_DIR/ca.key -out $WORK_DIR/ca.crt

# export the root ca cert into a keystore
openssl pkcs12 -export -name cacert -in $WORK_DIR/ca.crt -inkey $WORK_DIR/ca.key -out $WORK_DIR/cakeystore.p12  -passout pass:changeit -passin pass:changeit
keytool -importkeystore -destkeystore $WORK_DIR/cakeystore.jks -srckeystore $WORK_DIR/cakeystore.p12 -srcstoretype pkcs12 -alias cacert -keypass changeit -storepass changeit -srcstorepass changeit

########### Server

# create a key for the server
openssl genrsa -out $WORK_DIR/server.key 2048

# create a CSR and create a server key signed by the root CA
openssl req -new -key $WORK_DIR/server.key -out $WORK_DIR/server.csr -config $CURR_DIR/server.csr.conf
openssl x509 -req  -in $WORK_DIR/server.csr  -CA $WORK_DIR/ca.crt -CAkey $WORK_DIR/ca.key  -CAcreateserial -out $WORK_DIR/server.crt  -days 365  -sha256 -extfile $CURR_DIR/cert.conf

# export the server cert into a keystore
openssl pkcs12 -export -name servercert -in $WORK_DIR/server.crt -inkey $WORK_DIR/server.key -out $WORK_DIR/serverkeystore.p12 -passout pass:changeit -passin pass:changeit
keytool -importkeystore -destkeystore $WORK_DIR/serverkeystore.jks -srckeystore $WORK_DIR/serverkeystore.p12 -srcstoretype pkcs12 -alias servercert -keypass changeit -storepass changeit -srcstorepass changeit

############# Client

# create a key for the client
openssl genrsa -out $WORK_DIR/client.key 2048

# create a CSR and create a client key signed by the root CA
openssl req -new -key $WORK_DIR/client.key -out $WORK_DIR/client.csr -config $CURR_DIR/client.csr.conf
openssl x509 -req  -in $WORK_DIR/client.csr  -CA $WORK_DIR/ca.crt -CAkey $WORK_DIR/ca.key  -CAcreateserial -out $WORK_DIR/client.crt  -days 365  -sha256 -extfile $CURR_DIR/cert.conf

# export the client cert into a keystore
openssl pkcs12 -export -name clientcert -in $WORK_DIR/client.crt -inkey $WORK_DIR/client.key -out $WORK_DIR/clientkeystore.p12 -passout pass:changeit -passin pass:changeit
keytool -importkeystore -destkeystore $WORK_DIR/clientkeystore.jks -srckeystore $WORK_DIR/clientkeystore.p12 -srcstoretype pkcs12 -alias clientcert -keypass changeit -storepass changeit -srcstorepass changeit

