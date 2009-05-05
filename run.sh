#!/bin/bash

cd bin
java -Xmx1536m coms6111/proj3/FileReader $* 2>&1 | tee proj3.log
