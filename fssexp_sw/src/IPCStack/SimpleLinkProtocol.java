package IPCStack;

import CRC.CRC;
import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;

public class SimpleLinkProtocol {

	private KissProtocol m_kiss; 
	private Log m_logger;
	private int m_packet_redundancy;
	private ExperimentConf m_conf;
	
	// TODO: remove with LL
	private int m_id_packet;

	private final static String TAG = "[SimpleLinkProtocol] ";
	
	public SimpleLinkProtocol(Log log, ExperimentConf conf, TimeUtils timer) {
		m_logger = log;
		m_kiss = new KissProtocol(m_logger, timer, conf);
		m_id_packet = 0;
		m_conf = conf;
	}
	
	public void setConfiguration() 
	{
		m_packet_redundancy = Constants.rf_isl_redundancy;
	}
	
	public boolean open() 
	{
		return m_kiss.open();
	}
	
	private byte[] createHeader(byte command, int redundancy, byte[] data) 
	{
		short crc;
		short length = (short)data.length;
		byte[] header = new byte[Constants.SLP_header_size];
	
		header[0] = command;
		//System.out.println(TAG + "Redundancy: " + redundancy);
		header[1] = (byte)(redundancy & 0xFF);
		header[2] = (byte)((length & 0xFF00) >> 8);
		header[3] = (byte)(length & 0xFF);
		if(data.length > 0) {
			crc = (short)CRC.calculateCRC(CRC.Parameters.CCITT, data);
			header[4] = (byte)((crc & 0xFF00) >> 8);
			header[5] = (byte)(crc & 0xFF);
		} else {
			header[4] = (byte)0xFF;
			header[5] = (byte)0xFF;
		}
		return header;
	}
	
	private boolean verifyHeader(short[] header, byte command, int redundancy, byte[] data) 
	{	
		short crc;
		boolean correct = true;
		
		if(command == Constants.SLP_COMMAND_RECEIVE) {
			if(header[0] != (Constants.SLP_REPLY_RX_OK & 0xFF) 
				&& header[0] != (Constants.SLP_REPLY_RX_NOK & 0xFF)) {
				m_logger.info(TAG + "Received a SLP header with bad command:Exp-" 
						+ (Constants.SLP_REPLY_RX_OK & 0xFF) + "/" + (Constants.SLP_REPLY_RX_NOK & 0xFF) 
						+ ";RX-" + header[0]);
				correct = false;
			}
		} else if(command == Constants.SLP_COMMAND_TELEMETRY) {
			if(header[0] != (Constants.SLP_REPLY_TELEMETRY & 0xFF)) {
				m_logger.info(TAG + "Received a SLP header with bad command:Exp-" 
						+ (Constants.SLP_REPLY_TELEMETRY & 0xFF) + ";RX-" + header[0]);
				correct = false;
			}
		} else if(command == Constants.SLP_COMMAND_SEND) {
			if(header[0] != (Constants.SLP_REPLY_SEND_ACK & 0xFF)
				&& header[0] != (Constants.SLP_REPLY_SEND_ERROR & 0xFF)) {
				m_logger.info(TAG + "Received a SLP header with bad command:Exp-" 
						+ (Constants.SLP_REPLY_SEND_ACK & 0xFF) + "/" + (Constants.SLP_REPLY_SEND_ERROR & 0xFF)
						+ ";RX-" + header[0]);
				correct = false;
			}
		} else if(command == Constants.SLP_COMMAND_CONFIGURATION) {
			if(header[0] != (Constants.SLP_REPLY_CONF_ACK & 0xFF)) {
				m_logger.info(TAG + "Received a SLP header with bad command:Exp-" 
						+ (Constants.SLP_REPLY_CONF_ACK & 0xFF) + "/" + (Constants.SLP_REPLY_SEND_ERROR & 0xFF)
						+ ";RX-" + header[0]);
				correct = false;
			}
		}
		
		if(header[1] != redundancy) {
			m_logger.info(TAG + "Received a SLP header with bad redundancy:Exp-" 
					+ header[1] + ";RX-" + redundancy);
			correct = false;
		}
		
		if(header[2] != data.length) {
			m_logger.info(TAG + "Received a SLP header with bad data length:Exp-" 
					+ data.length + ";RX-" + header[2]);
			correct = false;
		}
	
		if(data.length > 0) {
			crc = (short)CRC.calculateCRC(CRC.Parameters.CCITT, data);
			if(header[3] != crc){
				m_logger.info(TAG + "Received a SLP header with bad CRC:Exp-"
						+ crc + ";RX-" + header[3]);
				correct = false;
			}
			
		} else {
			if(header[3] != -1) {
				m_logger.info(TAG + "Received a SLP header with bad CRC:Exp-\\xFF\\xFF" 
						+ ";RX-" + header[3]);
				correct = false;
			}
		}
		return correct;
	}
	
	private short[] parseHeader(byte[] header_bytes) {
		
		short[] header = new short[4];	/* Command | Redundancy | Length | CRC */
		
		header[0] = (short)(header_bytes[0] & 0xFF);
		header[1] = (short)(header_bytes[1] & 0xFF);
		header[2] = (short)(((header_bytes[2] & 0xFF) << 8) | (header_bytes[3] & 0xFF));
		header[3] = (short)(((header_bytes[4] & 0xFF) << 8) | (header_bytes[5] & 0xFF));
		
		return header;
	}
	
	public byte[] getTelemetry() {
		
		short[] header;
		byte[] header_bytes;
		byte[] data;
		boolean correct;
		int tries = 0;
		boolean received = false;
		
		try {
			//m_logger.info(TAG + "Sending a TELEMETRY packet");
			/* Request the Telemetry */
			correct = m_kiss.send(createHeader(Constants.SLP_COMMAND_TELEMETRY, 0, new byte[0]));
			
			if(correct == true) {
				/* Receive the Telemetry */
				while(received == false) {
					byte[] content = m_kiss.receive();
					//m_logger.info(TAG + "Retrieved Telemtry with " + content.length + " Bytes");
					if(content.length >= Constants.SLP_header_size) {
						received = true;
						header_bytes = new byte[Constants.SLP_header_size];
						System.arraycopy(content, 0, header_bytes, 0, header_bytes.length);
						header = parseHeader(header_bytes);
						data = new byte[header[2]];
						
						if(content.length == Constants.SLP_header_size + data.length) {
							System.arraycopy(content, header_bytes.length, data, 0, data.length);
							if(verifyHeader(header, Constants.SLP_COMMAND_TELEMETRY, 0, data)) {
								return data;
							} else {
								return new byte[0];
							}
						
						} else {
							m_logger.error(TAG + "SimpleLinkProtocol packet received with the correct header, but wrong data size (packet size: " + content.length + " data size: " + data.length);
							return new byte[0];
						}		
						
					} else {
						/* Wait a while and check again */
						if(tries < Constants.SLP_max_times_reply_rfisl) {
							Thread.sleep(Constants.SLP_reply_timeout_rfisl);
							tries ++;
						} else {
							m_logger.warning(TAG + "I have been trying, but impossible to receive a HK from the RF ISL Module (after sending one command)");
							return new byte[0];
						}
					}
				}
				
			} else {
				m_logger.warning(TAG + "Impossible to send a HK request to the RF ISL Module");
				return new byte[0];
			}
			
		} catch(Exception e) {
			m_logger.error(e);
		}
		
		return new byte[0];
	}
	
	private byte[] receive() throws InterruptedException 
	{
		short[] header;
		byte[] header_bytes;
		byte[] data;
		int tries = 0;
		boolean received = false;
		boolean correct = false;
		
		try {
			/* Request the Content */
			correct = m_kiss.send(createHeader(Constants.SLP_COMMAND_RECEIVE, 0, new byte[0]));
			
			if(correct == true) {
				/* Receive the Content */
				while(received == false) {
					byte[] content = m_kiss.receive();
					//m_logger.info(TAG + "At SimpleLinkProtocol retrieved RX with " + content.length + " Bytes");
					if(content.length >= Constants.SLP_header_size) {
						received = true;
						header_bytes = new byte[Constants.SLP_header_size];
						System.arraycopy(content, 0, header_bytes, 0, header_bytes.length);
						header = parseHeader(header_bytes);
						//System.out.println(TAG + "Header params: - Command: " + header[0] + " - Redundancy: " + header[1] + 
						//		" - Length: " + header[2] + " - CRC: " + header[3]);
						
						
						if(header[2] <= (content.length - header_bytes.length)) {
							data = new byte[header[2]];
							System.arraycopy(content, header_bytes.length, data, 0, data.length);
							//System.out.println(TAG + "Data received: " + content.length + " Bytes");
							
							if(verifyHeader(header, Constants.SLP_COMMAND_RECEIVE, 0, data)) {
								//System.out.println(TAG + "Good Header");
								
								return data;
							} else {
								System.out.println(TAG + "Wrong Header while requesting a received packet");
								return new byte[0];
							}
						} else {
							m_logger.error(TAG + "Data Length greater (" + header[2] + ") than the content (" + content.length + ")"); 
							return new byte[0]; 
						}
						
					
					} else {
						/* Wait a while and check again */
						if(tries < Constants.SLP_max_times_reply_rfisl) {
							Thread.sleep(Constants.SLP_reply_timeout_rfisl);
							tries ++;
						} else {
							m_logger.warning(TAG + "I have been trying, but impossible to receive an ACK of sent data from the RF ISL Module");
							return new byte[0];
						}
					}
				}
				
			} else {
				m_logger.warning(TAG + "Impossible to send a receive command to the RF ISL Module");
				return new byte[0];
			}
		} catch(Exception e) {
			m_logger.error(e);
		}
		
		return new byte[0];
	}
	
	public boolean updateConfiguration(byte[] conf)
	{
		int tries = 0;
		boolean received = false;
		/* Send configuration */
		byte[] content;
		byte[] header;
		byte[] header_bytes;
		short[] header_short;
		byte[] data;
		header = createHeader(Constants.SLP_COMMAND_CONFIGURATION, m_packet_redundancy, conf);
		content = new byte[header.length + conf.length];
		System.arraycopy(header, 0, content, 0, header.length);
		System.arraycopy(conf, 0, content, header.length, conf.length);
		
		try {
			boolean correct = m_kiss.send(content);
			if(correct == true) {
				/* Receive ACK */
				while(received == false) {
					content = m_kiss.receive();
					if(content.length >= Constants.SLP_header_size) {
						
						received = true;
						header_bytes = new byte[Constants.SLP_header_size];
						System.arraycopy(content, 0, header_bytes, 0, header_bytes.length);
						header_short = parseHeader(header_bytes);
						data = new byte[header[2]];
						
						if(verifyHeader(header_short, Constants.SLP_COMMAND_CONFIGURATION, 0, data)) {
							received = true;
							return true;
						} else {
							return false;
						}
					} else {
						/* Wait a while and check again */
						if(tries < Constants.SLP_max_times_reply_rfisl) {
							Thread.sleep(Constants.SLP_reply_timeout_rfisl);
							tries ++;
						} else {
							m_logger.warning(TAG + "I have been trying, but impossible to receive an ACK of sent CONF from the RF ISL Module");
							return false;
						}
					}
				}
			}
		} catch(Exception e) {
			m_logger.error(e);
		}
		
		return false;
	}
	
	private boolean send(byte[] data) throws InterruptedException {
		
		//m_logger.info(TAG + "At SimpleLinkProtocol received " + data.length + " bytes");
		
		
		int tries = 0;
		boolean received = false;
		byte[] content;
		byte[] header = createHeader(Constants.SLP_COMMAND_SEND, m_packet_redundancy, data);
		content = new byte[header.length + data.length];
		System.arraycopy(header, 0, content, 0, header.length);
		System.arraycopy(data, 0, content, header.length, data.length);
		try {
			if(m_kiss.send(content) == true) {
			
				/* Receive ACK */
				while(received == false) {
					byte[] header_bytes = m_kiss.receive();
					
					if(header_bytes.length >= Constants.SLP_header_size) {
						short[] header_short = parseHeader(header_bytes);
				
						if(verifyHeader(header_short, Constants.SLP_COMMAND_SEND, 0, new byte[0])) {
							received = true;
							return true;
						} else {
							return false;
						}
					} else {
						/* Wait a while and check again */
						if(tries < Constants.SLP_max_times_reply_rfisl) {
							Thread.sleep(Constants.SLP_reply_timeout_rfisl);
							tries ++;
						} else {
							m_logger.warning(TAG + "I have been trying, but impossible to receive an ACK of sent DATA from the RF ISL Module");
							return false;
						}
					}
				}
				
			} else {
				m_logger.warning(TAG + "Impossible to send a send request to the RF ISL Module");
				return false;
			}
		} catch (Exception e) {
			m_logger.error(e);
		}
		
		return false;
	}
	
	public void close() {
		m_kiss.close();
	}
	
	public synchronized int bytesAvailable() {
		return m_kiss.bytesAvailable();
	}
	
	// TODO: solve this situation
	/***********************************************************************************************//**
	 * Method to transmit through RF ISL a packet following the Link Layer Protocol.
	 **************************************************************************************************/
	public boolean transmitPacket(byte[] data) throws InterruptedException {
		
		//m_logger.info(TAG + "At LinkLayerProtocol received " + data.length + " bytes of data");
		
		//System.out.println("Input data length " + data.length);
		boolean correct = false;
		
		byte[] packet = new byte[data.length + Constants.ll_header_size];
		byte[] header = new byte[Constants.ll_header_size];
		
		/* Fragment data to the corresponding MTU */
		int total_blocks = data.length / Constants.ll_data_mtu;
		int remaining_block = data.length % Constants.ll_data_mtu;
		int length = Constants.ll_data_mtu;
		byte[] fragment = new byte[length];
		int crc = 0;
		if(remaining_block > 0) {
			total_blocks ++;
		}
		
		for(int i = 0; i < total_blocks; i++) {
			/* Last packet needs to be specially addressed */
			if(i == total_blocks - 1) {
				length = remaining_block;
				packet = new byte[header.length + length];
				fragment = new byte[length];
			}
			
			//System.out.println("Fragment Length " + length);
			/* Insert ID */
			header[0] = (byte)(m_id_packet & 0xFF);
			
			/* Insert block count */
			header[1] = (byte)(i+1 & 0xFF);
			
			/* Insert total blocks */
			header[2] = (byte)(total_blocks & 0xFF);
			
			/* header[3] is reserved, not used */
			
			/* Insert Length */
			header[4] = (byte)((length & 0xFF00) >> 8);
			header[5] = (byte)(length & 0xFF);
			
			/* Insert CRC-16 bits */
			System.arraycopy(data, i * length, fragment, 0, length);
			crc = fletcher16(fragment, length);
			header[6] = (byte)((crc & 0xFF00) >> 8);
			header[7] = (byte)(crc & 0xFF);
			
			/* Send Packet */
			System.arraycopy(header, 0, packet, 0, header.length);
			System.arraycopy(fragment, 0, packet, header.length, fragment.length);
			//System.out.println("LL packet length " + packet.length);
			//m_logger.info(TAG + "At LinkLayerProtocol send " + packet.length + " bytes of data");
			correct = send(packet);
			if(correct == false) {
				m_logger.warning(TAG + "Something got wrong during sending a LL fragment");
				break;
			}
		}
		
		m_id_packet ++;		
		
		return correct;
	}
	
	/***********************************************************************************************//**
	 * Sends a command to evaluate if a packet has been received. When a packet is received, the method
	 * replies with the corresponding packet (in byte[] format). Otherwise, the array is empty.
	 **************************************************************************************************/
	public byte[] checkReceptionPacket() throws InterruptedException 
	{	
		int total_blocks; 
		int count_blocks;
		int length;
		int crc;
		int actual_id;
		byte[] packet = receive();
		
		//m_logger.info(TAG + "At LinkLayerProtocol RX received " + packet.length + " bytes");
		
		
		if(packet.length > 0) {
			byte[] header = new byte[Constants.ll_header_size];
			System.arraycopy(packet, 0, header, 0, header.length);
			//System.out.println("Packet ID " + String.format("0x%02x", header[0]));
			count_blocks = header[1];
			//System.out.println("Count Blocks " + count_blocks);
			total_blocks = header[2];
			//System.out.println("Total Blocks " + total_blocks);
			//System.out.println("Length Byte 1: " + String.format("0x%02x", header[4]));
			//System.out.println("Length Byte 2: " + String.format("0x%02x", header[5]));
			length = (short)(((header[4] & 0xFF) << 8) | (header[5] & 0xFF));
			//System.out.println("Length Computed: " + length);
			//System.out.println("Length Computed: " + String.format("0x%02x", length));
			crc = (short)(((header[6] & 0xFF) << 8) | (header[7] & 0xFF));
			byte[] data = new byte[length];
			System.arraycopy(packet, header.length, data, 0, length);
			
			if(total_blocks == 1) {
				//System.out.println("Comparing the CRC; Received: " + crc + " Computed " + fletcher16(data, length));
				// TODO: Improve CRC-16
				//if(fletcher16(data, length) == crc) {
					//System.out.println("Returning the data");
					return data;
				//}
			} else {
				//TODO: Am I interested on fragmenting?
				//System.out.println(TAG + "No return, total_blocks: " + total_blocks);
				
				return new byte[0];
			}
			
		} else {
			
			//m_logger.info(TAG + "At LinkLayerProtocol RX outputs " + packet.length + " bytes");
			
			return packet;
		}
		//return new byte[0];
	}
	
	private int fletcher16(byte[] data, int size)
	{
	    int c0 = 0;
	    int c1 = 0;
	    for(int i = 0; i < size; i++) {
	        c0 += data[i];
	        c1 = c1 + c0;
	    }
	    c0 = c0 % 255;
	    c1 = c1 % 255;
	    return ((c0 << 8) | c1);
	}

	
}
