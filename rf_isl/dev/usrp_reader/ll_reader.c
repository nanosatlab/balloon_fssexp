#include "ll_reader.h"

static int  m_receive_socket = 0;
static int  m_send_feedback_socket = 0;

static char m_host_ip[64];

static const int  m_tcp_feedback_port = 52004;
static const int  m_udp_send_port = 52001;

static const int  m_tcp_receive_port = 52000;

static void send_udp(void *p, size_t len)
{
    int fd;
    struct hostent *he;
    /* estructura que recibirá información sobre el nodo remoto */
    struct sockaddr_in server;
    /* información sobre la dirección del servidor */
    if((he=gethostbyname(m_host_ip))==NULL){
        /* llamada a gethostbyname() */
        perror("gethostbyname() error\n");
        exit(1);
    }

    if((fd=socket(AF_INET, SOCK_DGRAM, 0))==-1){
        /* llamada a socket() */
        printf("socket() error\n");
        exit(1);
    }
    memset(&server, 0, sizeof(server));
    server.sin_family = AF_INET;
    server.sin_port = htons(m_udp_send_port);

    server.sin_addr = *((struct in_addr *)he->h_addr);
    sendto(fd, p, len, 0, (struct sockaddr *)&server, sizeof(server));
    close(fd);
}

static int socket_connect(char *host_ip, int port)
{
    int fd;
    struct hostent *he;
    /* estructura que recibirá información sobre el nodo remoto */
    struct sockaddr_in server;
    /* información sobre la dirección del servidor */
    if((he=gethostbyname(host_ip))==NULL){
        /* llamada a gethostbyname() */
        perror("gethostbyname() error\n");
        exit(1);
    }

    if((fd=socket(AF_INET, SOCK_STREAM, 0))==-1){
        /* llamada a socket() */
        printf("socket() error\n");
        exit(1);
    }

    server.sin_family = AF_INET;
    server.sin_port = htons(port);
    /* htons() es necesaria nuevamente ;-o */
    server.sin_addr = *((struct in_addr *)he->h_addr);
    /*he->h_addr pasa la información de ``*he'' a "h_addr" */
    bzero(&(server.sin_zero),8);

    if(connect(fd, (struct sockaddr *)&server,
               sizeof(struct sockaddr))==-1){
        /* llamada a connect() */
        printf("connect() error\n");
        exit(-1);
    }
    return fd;
}

static int timeout_on_fd(int fd, int timeout_ms)
{
    struct timeval tv;
    // fd_set passed into select
    fd_set fds;
    int control_ret;
    if(timeout_ms >= 1000) {
        tv.tv_sec = timeout_ms / 1000;
        tv.tv_usec = (timeout_ms % 1000) * 1000;
    } else {
        tv.tv_sec = 0;
        tv.tv_usec = timeout_ms * 1000;
    }
    // Zero out the fd_set - make sure it's pristine
    FD_ZERO(&fds);
    // Set the FD that we want to read
    FD_SET(fd, &fds);
    // select takes the last file descriptor value + 1 in the fdset to check,
    // the fdset for reads, writes, and errors.  We are only passing in reads.
    // the last parameter is the timeout.  select will return if an FD is ready or
    // the timeout has occurred
    if( (control_ret = select(fd+1, &fds, NULL, NULL, &tv) ) == -1) {
        return -1;
    }
    // return 0 if fd is not ready to be read.
    if( ( control_ret = FD_ISSET(fd, &fds) ) > 0 ) {
        /* Something to read! */
        return 1;
    } else {
        if(control_ret == 0) {
            return 0;
        } else {
            return -1;
        }
    }
}

int ll_init(char *host_ip)
{
    strncpy(m_host_ip, host_ip, 64);
    m_receive_socket        = socket_connect(m_host_ip, m_tcp_receive_port);
    m_send_feedback_socket  = socket_connect(m_host_ip, m_tcp_feedback_port);
    if(m_receive_socket != -1 && m_send_feedback_socket != -1) {
        return 0;
    } else {
        return -1;
    }
}

int receive_ll_packet(link_layer_packet_t *ll_packet, int timeout_ms)
{
    bool packet_not_full = true;
    int read_ret = 0, readed_size = 0;
    int expected_size = 0;
    int to_read_size = (LINK_LAYER_HEADER_SIZE + LINK_LAYER_PAYLOAD_SIZE);
    /* perform a select */
    int count = 0;
    while(packet_not_full) {
        read_ret = read(m_receive_socket, &ll_packet->raw[readed_size], to_read_size);
        if(read_ret > 0) {
            count++;
            expected_size = ntohs(ll_packet->fields.len) + LINK_LAYER_HEADER_SIZE;
            readed_size += read_ret;
            if(expected_size == readed_size) {
                packet_not_full = false;
            } else {
                to_read_size = expected_size - readed_size;
            }
        } else {
            packet_not_full = false;
            readed_size = read_ret;
        }
    }
    if(count > 1) {
        printf("Packet recovered with fragmentation in TCP\n");
    }
    return readed_size;
}

int send_ll_packet(link_layer_packet_t *ll_packet, int packet_size)
{
    int ret;
    usrp_metadata feedback;
    /* send a ll packet to comms */
    while(timeout_on_fd(m_send_feedback_socket, 0) > 0) {
        read(m_send_feedback_socket, &feedback, sizeof(usrp_metadata));
        printf("There was bullshit on the channel!!\n");
    }

    printf("Sending packet of size: %d\n", packet_size);
    send_udp(ll_packet->raw, packet_size);
    /* wait the ACK from the USRP */

    do {
        /* clear that before going in */
        if(timeout_on_fd(m_send_feedback_socket, 2500) <= 0) {
            printf("Timeout receiving USRP feedback\n");
            break;
        } else {
            if(read(m_send_feedback_socket, &feedback, sizeof(usrp_metadata)) > 0) {
                if(feedback.event_code == 2) {
                    printf("USRP finished sending\n");
                } else {
                    printf("USRP still sending\n");
                }
            } else {
                perror("Read feedback returned bad...\n");
            }
        }
    }while(feedback.event_code != 2);
    return 0;
}

int main(void)
{
    int ret;
    link_layer_packet_t packet;
    char *host = "localhost";

    printf("LL init returned: %d\n", ll_init(host));

    packet.fields.len = htons(1740);
    while(1) {
        send_ll_packet(&packet, ntohs(packet.fields.len)+LINK_LAYER_HEADER_SIZE);
	sleep(10);
    }

    while(1) {
        if((ret = receive_ll_packet(&packet, 0)) > 0) {
            printf("Received packet from USRP of length: %d\n", ntohs(packet.fields.len));
            sleep(1);
            send_ll_packet(&packet, ntohs(packet.fields.len)+LINK_LAYER_HEADER_SIZE);
        } else {
            printf("Error while receiving\n");
            sleep(1);
            ll_init(host);
        }
    }
}
