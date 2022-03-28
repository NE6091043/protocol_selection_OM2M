#!/bin/bash

date -d "$(curl -s --head http://google.com | grep ^Date: | sed 's/Date: //g')" +%s