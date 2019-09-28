#! /bin/bash 

# Launch PTY connection
echo "Launching PTY connection..."
socat -d -d -b 1 pty,rawer pty,rawer
echo "PTY - GOOD"
