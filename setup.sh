#!/bin/bash
git submodule update --init
cd src/main/c/Beethoven-Software/runtime
git pull origin master
bash setup_dramsim.sh
