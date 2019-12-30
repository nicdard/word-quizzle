#!/usr/bin/env bash

mvn clean package;
java -cp server/target/classes/ MainClassWQServer;
java -cp client/target/classes/ MainClassWQClient;
