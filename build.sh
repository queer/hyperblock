#!/usr/bin/env bash

env MAVEN_OPTS="--illegal-access=permit" mvn clean package
