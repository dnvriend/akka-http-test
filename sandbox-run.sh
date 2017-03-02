#!/bin/bash
sudo sh -c "ifconfig lo0 alias 192.168.10.1 255.255.255.0 && \
ifconfig lo0 alias 192.168.10.2 255.255.255.0 && \
ifconfig lo0 alias 192.168.10.3 255.255.255.0"
sandbox run 2.0.2 --feature visualization --feature monitoring --nr-of-containers 2