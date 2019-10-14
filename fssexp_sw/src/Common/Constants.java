package Common;

public class Constants {

	/* Software version */
	public final static int sw_version = 10;
	
    /* Folder tree */
    public final static String exp_root = "/fss/";
    public final static String persistent_path = exp_root + "persistent/";
    public final static String fss_data_path = exp_root + "exp_data/";
    public final static String conf_path = exp_root + "conf/";
    public final static String download_path = exp_root + "to_download/";
    public final static String log_path = exp_root + "log/";

    public final static String conf_file = conf_path + "fss_exp.conf";
    public final static String fss_data_file = fss_data_path + "fss_buffer.data";
    public final static String fss_result_data_file = download_path + "fss_packets.data";
    public final static String hk_name = "housekeeping.data";
    public final static String hk_file = log_path + hk_name;
    public final static String log_name = "log.data";
    public final static String log_file = log_path + log_name;
    public final static String persistent_file = persistent_path + "manager.prst";
    public final static String tx_file = fss_data_path + "tx_packets.data";
    public final static String rx_file = fss_data_path + "rx_packets.data";
    public final static String dwn_file_pattern = "fssexp.tar";
    
    /* ExperimentManager */
    public final static short server_socket = 4444;
    public final static String rs_port = "/dev/pts/11";
    public final static long manager_sleep = 500;       /* ms */
    public final static long manager_polling_period = 1000;     /* ms */
    public final static int generator_max_polling = 10;  /* i.e. max_polling * polling_period = 3s */
    public final static int fss_protocol_max_polling = 10;
    public final static int uart_interface_max_polling = 10;
    public final static int manager_exit_max = 10;
    public final static long manager_max_exec_time = 15 * 60 * 1000;    /* 15 minutes in ms */
    public final static int rf_isl_max_polling = 5;
    public final static int rf_isl_max_configuration = 5;
    public final static int rf_isl_max_fsspacket = 3;
    public final static int rf_isl_tostorehk_size = 33;
    public final static long dwn_contact_sleep = 500;       /* ms */
    
    public final static int LENGHT_FIELD_SIZE = 2;	/* in Bytes */
    public final static int COMMAND_ITEMS = 2;
    public final static String COMMAND_KEY = "command";
    public final static String TIMESTAMP_KEY = "timestamp";
    public final static int GENERIC_REPLY_ITEMS = 2;
    public final static int STATUS_REPLY_ITEMS = 3;
    public final static String ACK_KEY = "ack";
    public final static String MODE_KEY = "mode";
    public final static String ERROR_KEY = "error";
    
    public final static String COMMAND_STATUS = "STATUS";
    public final static int COMMAND_PARSED_STATUS = 2;
    public final static String COMMAND_EXIT = "EXIT";
    public final static int COMMAND_PARSED_EXIT = 3;
    public final static int COMMAND_PARSED_UNKNOWN = 4;
    public final static String REPLY_ACK_OK = "OK";
    public final static String REPLY_ACK_ERROR = "ERROR";
    public final static String REPLY_READY = "READY";
    public final static String REPLY_RUNNING = "RUNNING";
    public final static String REPLY_FINISHED = "FINISHED";
    public final static int REPLY_STATUS_READY = 0;
    public final static int REPLY_STATUS_NEGOTIATION = 1;
    public final static int REPLY_STATUS_FEDERATION = 2;
    public final static int REPLY_STATUS_CLOSURE = 3;
    public final static int REPLY_STATUS_FINISHED = 4;
    public final static int REPLY_STATUS_ENDING = 5;
    public final static int REPLY_STATUS_ERROR = 6;
    public final static int REPLY_UNKNOWN = 255;
    
    /* FSS Protocol constants */
    public final static long fss_prot_sleep = 500;    /* ms */
    
    public final static int FSS_NOT_STARTED = 3;
    public final static int FSS_NEGOTIATION = 0;
    public final static int FSS_EXCHANGE = 1;
    public final static int FSS_CLOSURE = 2;
    public final static int FSS_ROLE_NOT_DEFINED = 2;
    public final static int FSS_ROLE_CUSTOMER = 0;
    public final static int FSS_ROLE_SUPPLIER = 1;
    
    public final static int FSS_PACKET_SERVICE_PUBLISH 	= 	0x00000000; /* 0 */
    public final static int FSS_PACKET_SERVICE_REQUEST 	= 	0x00000001; /* 1 */
    public final static int FSS_PACKET_SERVICE_ACCEPT 	= 	0x00000002; /* 2 */
    public final static int FSS_PACKET_DATA 			=	0x00000004; /* 4 */
    public final static int FSS_PACKET_DATA_ACK 		= 	0x00000008; /* 8 */
    public final static int FSS_PACKET_CLOSE 			= 	0x00000010; /* 16 */
    public final static int FSS_PACKET_CLOSE_ACK 		= 	0x00000020;	/* 32 */
    public final static int FSS_PACKET_CLOSE_DATA_ACK 	= 	0x00000030;	/* 64 */
    public final static int FSS_PACKET_NOT_VALID 		= 	0x00000040;	/* 128 */
    
    
    public final static int FSS_SERVICE_TYPE_NOT_DEFINED = -1;
    public final static int FSS_SERVICE_TYPE_STORAGE = 0;
    public final static int FSS_SERVICE_TYPE_DOWNLOAD = 1;
    
    public final static int FSS_INTEREST = 1;
    public final static int FSS_NOT_INTEREST = 0;
    
    public final static int FSS_BROADCAST_ADDR = 3;
    
    /* FSS Packet constants */
    public final static int header_size = 13;   /* bytes */
    
    /* Data Generator constants */
    public final static long generator_sleep = 500;    /* ms */
    public final static int generator_STATUS_STOPPED = 0;
    public final static int generator_STATUS_RUNNING = 1;
    public final static int data_header_size = 1;
    public final static int data_timestamp_size = 8;
    public final static int data_reference_size = 155;
    public final static int data_rf_isl_hk_size = 51;
    public final static int data_size = data_header_size + 
                                        data_timestamp_size +
                                        data_rf_isl_hk_size +
                                        data_reference_size;
    
    /* FSSDataBuffer constants */
    public final static int fss_buffer_move_block = 1024;
    
    /* ExperimentConf */
    public final static int conf_parameters_num = 18;
    
    /* HousekeepingBuffer */
    public final static int hk_header_sw_version = 1;
    public final static int hk_header_expnum_satid_size = 1;
    public final static int hk_header_time_size = 8;
    public final static int hk_header_size = hk_header_sw_version +
    											hk_header_expnum_satid_size +
									    		hk_header_time_size; 
    
    public final static int hk_item_time_size = 4;
    public final static int hk_item_manager_size = 6;
    public final static int hk_item_generator_size = 5;    /*  Bytes */ 
    public final static int hk_item_fssbuffer_size = 4;
    public final static int hk_item_fssprotocol_size = 7;
    public final static int hk_header_conf_version_size = 1;
    public final static int hk_item_rf_isl_status = 1;
    public final static int hk_item_size = hk_item_time_size + 
                                            hk_item_manager_size + 
                                            hk_item_generator_size + 
                                            hk_item_fssbuffer_size +
                                            hk_item_fssprotocol_size + 
                                            hk_header_conf_version_size +
                                            hk_item_rf_isl_status + 
                                            rf_isl_tostorehk_size;
    public final static int hk_buffer_move_block = 1024;
    
    /* PacketExchangeBuffer */
    public final static int packet_buffer_move_block = 1024;
    
    /* Link Layer Protocol */
    public final static int ll_header_size = 8;
    public final static int ll_data_mtu = 1744;
    
    /* SimpleLinkProtocol */
    public final static int SLP_header_size = 6;
    public final static byte SLP_COMMAND_SEND = 0x00;
    public final static byte SLP_COMMAND_RECEIVE = 0x02;
    public final static byte SLP_COMMAND_TELEMETRY = 0x01;
    public final static byte SLP_COMMAND_CONFIGURATION = 0x04;
    public final static byte SLP_REPLY_SEND_ACK = 0x05;
    public final static byte SLP_REPLY_SEND_ERROR = 0x03;
    public final static byte SLP_REPLY_TELEMETRY = 0x01;
    public final static byte SLP_REPLY_RX_OK = 0x00;
    public final static byte SLP_REPLY_RX_NOK = 0x03;
    public final static byte SLP_REPLY_CONF_ACK = 0x05;
    public final static int SLP_ACCESS_SEND = 0;
    public final static int SLP_ACCESS_TELEMETRY = 1;
    public final static int SLP_ACCESS_RECEIVE = 2;
    public final static int SLP_ACCESS_CONF = 3;
    public final static int SLP_max_times_reply_rfisl = 10;
    public final static int SLP_reply_timeout_rfisl = 100;
    
    /* KissProtocol */
    public final static int KISS_FEND = 0xC0;
    public final static int KISS_FESC = 0xDB;
    public final static int KISS_TFEND = 0xDC;
    public final static int KISS_TFESC = 0xDD;
    public final static int kiss_max_byte_waiting = 500;
    
    /* Uart Interface */
    public final static int uart_max_buffer_size = 1632; /* Three DATA packets with SimpleLink Protocol Header */
    public final static int uart_nominal_sleep = 50;	/* sleep 10 ms */
    public final static int uart_comms_sleep = 2;	/* sleep 10 ms */
    public final static String uart_port = "/dev/ttyACM0";
    public final static int uart_bps = 115200;
    public final static int uart_max_reply = 100; /* 100 ms */
    
    /* LOG */
    public final static int log_data_to_flush = 500;
}
