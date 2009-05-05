#!/bin/bash

cd bin
java -Xmx2048m coms6111/proj3/FileReader $* 2>&1 | tee proj3.log
