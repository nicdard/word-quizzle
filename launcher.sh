#!/usr/bin/env bash

mvn clean package;
java -cp target/classes/ client.Main;