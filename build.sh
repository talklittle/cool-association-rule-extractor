#!/bin/bash

if [ ! -e bin ]; then
	mkdir bin
fi
javac -d bin src/coms6111/proj3/*.java
