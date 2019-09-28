#! /bin/bash


function do_help() {
    echo "Script to establish a UART communication through a TCP/IP stack"
    echo "It is useful to test the FSS Experiment software"
    do_usage
}

function do_usage() {
    echo "Usage:"
    echo "    - Reception mode: ./set_RF_ISL_channel.sh RX_mode"
    echo "    - Transmission mode: ./set_RF_ISL_channel.sh TX_mode IP"
}

if [[ $# -eq 0 ]]; then
    do_help
    exit -1
fi

case "$1" in
    "RX_mode")
        #socat -d -d -d tcp-listen:4040 pty,rawer,link=/home/noitty/dev/pty_fss
        socat -d -d -d tcp-listen:4040 pty,rawer,link=/dev/ttyACM0
        ;;
    "TX_mode")
        #socat -d -d -d tcp:$2:4040 pty,rawer,link=/home/noitty/dev/pty_fss
        socat -d -d -d tcp:$2:4040 pty,rawer,link=/dev/ttyACM0
        ;;
    "--help")
	do_help
	;;
    *)
        do_usage
        ;;
esac

exit 0
        
