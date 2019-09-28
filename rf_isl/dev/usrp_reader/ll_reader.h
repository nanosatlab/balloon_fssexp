#ifndef __LL_READER_H__
#define __LL_READER_H__

#include <string.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>
#include <arpa/inet.h>

#define LINK_LAYER_HEADER_SIZE       (8)
#define LINK_LAYER_PAYLOAD_SIZE      (1744)

typedef union __attribute__ ((__packed__)) link_layer_packet_s {
    uint8_t     raw[LINK_LAYER_HEADER_SIZE + LINK_LAYER_PAYLOAD_SIZE];
    struct __attribute__ ((packed)) {
        /* attribs, len, and crc must be set here! */
        uint8_t     id; /* shall be consistent with ApplicationLayerFormat id */
        uint8_t     block_count;
        uint8_t     block_total;
        uint8_t     reserved;
        uint16_t    len;
        uint16_t    crc;    /* for the moment 16 bit crc... */
        uint8_t     payload[LINK_LAYER_PAYLOAD_SIZE];
    }fields;
}link_layer_packet_t;

typedef struct usrp_metadata{
    size_t   channel;
    bool     has_time_spec;
    uint8_t  time_spec_array[23];
    uint32_t event_code;
    uint32_t user_payload[4];
}usrp_metadata;

int  ll_init(char *host_ip);
int  receive_ll_packet(link_layer_packet_t *ll_packet, int timeout_ms);
int  send_ll_packet(link_layer_packet_t *ll_packet, int packet_size);

#endif
