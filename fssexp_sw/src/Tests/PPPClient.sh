#! /bin/bash

# Establish PPP connection
echo "Launching PPP connection ..."
sudo pppd -detach crtscts noauth  nodeflate nomagic  lock debug nopcomp noaccomp noccp novj novjccomp lcp-max-configure 100 lcp-restart 1 ipcp-restart 1 ipcp-max-configure 100 record /tmp/out 192.168.2.1:192.168.2.2 /dev/pts/4  115200

