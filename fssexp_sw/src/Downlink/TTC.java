package Downlink;

import java.lang.Thread;
import java.nio.ByteBuffer;
import java.util.Random;

import Configuration.ExperimentConf;
import IPCStack.PacketDispatcher;
import InterSatelliteCommunications.Packet;
import Storage.PayloadBuffer;
import Common.TimeUtils;
import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;

public class TTC extends Thread {

	private double cntct_min_period;
	private double cntct_max_period;
	private double cntct_max_duration;
	private double cntct_min_duration;
	private boolean m_emulated_dwn_contact;
	private boolean m_real_dwn_contact;
	private boolean m_exit;
	private TimeUtils m_time;
	private PacketDispatcher m_dispatcher;
	private SynchronizedBuffer m_ttc_buffer;
	private int m_prot_num;
	private Random m_rand;
	private Log m_logger;
	private PayloadBuffer m_payload_buffer;
	private int m_sat_id;
	private int m_gs_id;
	private int m_dwn_packet_counter;
	private int m_rx_packet_type_waiting;
	private boolean m_waiting_packet;
	private int m_waiting_counter;
	private int m_waiting_timeout;
	private long m_waiting_next;
	private int m_waiting_max;
	private int m_remote_candidate;
	
	private Packet m_tx_packet;
	private Packet m_rx_packet;
	private ByteBuffer m_header_stream;
	private ByteBuffer m_data_stream;
	private ByteBuffer m_checksum_stream;
	
	
	private final static String TAG = "[TTC] ";
	
	public TTC(TimeUtils time, ExperimentConf conf, Log logger, PacketDispatcher dispatcher, PayloadBuffer payload_buffer) 
	{
		setConfiguration(conf);
		m_time = time;
		m_rand = new Random(m_time.getTimeMillis());
		m_emulated_dwn_contact = false;
		m_real_dwn_contact = false;
		m_exit = false;
		m_logger = logger;
		m_ttc_buffer = new SynchronizedBuffer(m_logger, "ttc-buffer");
		m_dispatcher = dispatcher;
		m_prot_num = Constants.ttc_prot_num;
		m_dispatcher.addProtocolBuffer(m_prot_num, m_ttc_buffer);
		m_payload_buffer = payload_buffer;
		m_gs_id = Constants.gs_id;
		m_sat_id = conf.satellite_id;
		m_dwn_packet_counter = 0;
		m_tx_packet = new Packet();
		m_rx_packet = new Packet();
		m_rx_packet_type_waiting = Constants.FSS_PACKET_NOT_VALID;
		m_waiting_packet = false;
		m_waiting_counter = 0;
		m_waiting_next = 0;
		m_header_stream = ByteBuffer.allocate(Packet.getHeaderSize());
		m_checksum_stream = ByteBuffer.allocate(Packet.getChecksumSize());
		m_remote_candidate = -1;
	}
	
	public void setConfiguration(ExperimentConf conf) 
	{
		m_waiting_timeout = conf.ttc_timeout;
		m_waiting_max = conf.ttc_retries;
		cntct_max_period = conf.cntct_max_period;
		cntct_min_period = conf.cntct_min_period;
		cntct_max_duration = conf.cntct_max_duration;
		cntct_min_duration = conf.cntct_min_duration;
	}
	
	private void setHeaderPacket(Packet packet, int type, int address, int data_length) 
	{
		packet.source = m_sat_id;
		packet.destination = address;
		packet.prot_num = m_prot_num;
		packet.timestamp = m_time.getTimeMillis();
		packet.counter = m_dwn_packet_counter;
		packet.type = type;
		packet.length = data_length;
	}
	
	private void generateDataDownloadPacket(byte[] content)
	{
		m_tx_packet.resetValues();
		/* set the header */
		m_tx_packet.source = m_sat_id;
		m_tx_packet.destination = m_gs_id;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.timestamp = m_time.getTimeMillis();
		m_tx_packet.counter = m_dwn_packet_counter;
		m_tx_packet.type = Constants.PACKET_DWN;
		m_tx_packet.length = content.length;
		/* set the content */
		m_tx_packet.setData(content);
		/* set the checksum */
		m_tx_packet.computeChecksum();
	}
	
	private void generateHelloAckPacket()
	{
		System.out.println("Generating HELLO ACK Packet");
		m_tx_packet.resetValues();
		/* set the header */
		m_tx_packet.source = m_sat_id;
		m_tx_packet.destination = m_gs_id;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.timestamp = m_time.getTimeMillis();
		m_tx_packet.counter = m_dwn_packet_counter;
		m_tx_packet.type = Constants.PACKET_HELLO_ACK;
		m_tx_packet.length = 0;
		/* set the checksum */
		m_tx_packet.computeChecksum();
		System.out.println(m_tx_packet.toString());
	}
	
	private void generateCloseAckPacket()
	{
		m_tx_packet.resetValues();
		/* set the header */
		m_tx_packet.source = m_sat_id;
		m_tx_packet.destination = m_gs_id;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.timestamp = m_time.getTimeMillis();
		m_tx_packet.counter = m_dwn_packet_counter;
		m_tx_packet.type = Constants.PACKET_DWN_CLOSE_ACK;
		m_tx_packet.length = 0;
		/* set the checksum */
		m_tx_packet.computeChecksum();
	}
	
	private boolean transmitPacket()
	{
		boolean done = false;
		m_dispatcher.transmitPacket(m_prot_num, m_tx_packet);
		/* Check the correct transmission */
		while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
    		try {
    			/* Wait and release the processor */
            	Thread.sleep(10);
            } catch(InterruptedException e) {
            	m_logger.error(e);
            }
    	}
    	if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 1) {
			/* Update the counter */
			m_dwn_packet_counter += 1;
			done = true;
    	}
    	return done;
	}
	
	private boolean downloadPacket()
	{
		int counter = 0;
		boolean done = false;
		while(transmitPacket() == false && counter <= Constants.dwn_max_packet) {
			counter ++;
			m_logger.warning(TAG + "Failed to download a data Packet; Try number " + counter);
		}
		
		if(counter > Constants.dwn_max_packet) {
			m_logger.error(TAG + "Data download packet is not sent correctly; Problem with the RF ISL Module.");
			// TODO: something really wrong...
		} else {
			done = true;
			System.out.println(TAG + "[" + m_time.getTimeMillis() + "] Transmitted packet with header: "
					+ "src = " + m_tx_packet.source + " | dst = " + m_tx_packet.destination
					+ " | type = " + m_tx_packet.type + " | prot_num = " + m_tx_packet.prot_num
					+ " | counter = " + m_tx_packet.counter);
		}
		return done;
	}
	
	private boolean receivePacket()
	{
		boolean done = false;
		if(m_ttc_buffer.bytesAvailable() > 0) {
			m_rx_packet.resetValues();
			m_header_stream.clear();
			m_ttc_buffer.read(m_header_stream.array());
			m_rx_packet.setHeader(m_header_stream.array());
			System.out.println(TAG + "[" + m_time.getTimeMillis() + "] Received packet with header: "
					+ "src = " + m_rx_packet.source + " | dst = " + m_rx_packet.destination
					+ " | type = " + m_rx_packet.type + " | prot_num = " + m_rx_packet.prot_num
					+ " | counter = " + m_rx_packet.counter);
			
			if(m_rx_packet.source == m_gs_id) {			
				if(m_rx_packet.length > 0) {
					m_data_stream = ByteBuffer.allocate(m_rx_packet.length);
					m_ttc_buffer.read(m_data_stream.array());
					m_rx_packet.setData(m_data_stream.array());
				}
				m_checksum_stream.clear();
				m_ttc_buffer.read(m_checksum_stream.array());
				m_rx_packet.setChecksum(m_checksum_stream.array());
				done = true;
				
			} else {
				/* Discard the received packet */
				m_rx_packet.resetValues();
			}		
		}		
		return done;
	}
	
	private void releaseForPacket() 
	{
		m_waiting_packet = false;
		m_waiting_counter = 0;
	}
	
	private void lockForPacket(int packet_type) 
	{
		m_waiting_packet = true;
		m_rx_packet_type_waiting = packet_type;
		m_waiting_next = m_time.getTimeMillis() + m_waiting_timeout;
		m_logger.info(TAG + "Retransmission of packet " + packet_type + " set at " + m_waiting_next + "(backoff: "
				+ m_waiting_timeout + ")");
	}
	
	
	private void retransmitPacket() 
	{
		/* Retransmit */
		m_logger.info(TAG + "No replication - Retransmit Packet type " + m_tx_packet.type);
		Packet temp_packet = new Packet(m_tx_packet);
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, temp_packet.type, temp_packet.destination, temp_packet.length);
		m_tx_packet.setData(temp_packet.getData());
		m_tx_packet.computeChecksum();
		downloadPacket();
	}
	
	private void verifyLinkDead() 
	{
		if (m_waiting_counter > m_waiting_max) {
			m_logger.warning(
					TAG + "Maximum reply of Packet type " + m_tx_packet.type + " reached. The downlink is dead!");
			m_real_dwn_contact = false;
			releaseForPacket();
		} else {
			m_waiting_counter++;
			retransmitPacket();
			m_waiting_next = m_time.getTimeMillis() + m_waiting_timeout;
			m_logger.info(TAG + "No replication - Retransmitted packet " + m_tx_packet.type + "; next at "
					+ m_waiting_next);
		}
	}
	
	public void run() 
	{
		byte[] data;
		double cntct_start = 0;
		double cntct_end = 0;
		
		while(m_exit == false) {
			
			/* Check if something is received */
			if(receivePacket() == true) {
				
				if(m_waiting_packet == true && (m_rx_packet_type_waiting & m_rx_packet.type) != 0) {
					/*
					 * I have received the packet that I was waiting - I can release and keep
					 * working
					 */
					System.out.println("Releasing the waiting packet");
					releaseForPacket();
				}
				
				/* Something is received - check if it is an DWN_ACK */
				switch(m_rx_packet.type) {
				case Constants.PACKET_HELLO:
					/* Check source to evaluate if it is a GS */
					System.out.println("Received HELLO packet from " + m_rx_packet.source);
					if(m_real_dwn_contact == false) {
						/* GS is requesting to have a downlink connection */
						generateHelloAckPacket();
						if(downloadPacket() == true) {
							System.out.println("Transmitted HELLO ACK packet to " + m_tx_packet.destination);
						} else {
							System.out.println("Impossible to transmit HELLO ACK packet to " + m_tx_packet.destination);
						}
						/* Wait to confirm the dwnlink connection */
						lockForPacket(Constants.PACKET_HELLO_ACK);
					} else {
						System.out.println("Already connected to ground station " + m_rx_packet.source);
					}
				break;
				case Constants.PACKET_HELLO_ACK:
					/* Check if I was waiting this packet */
					if(m_rx_packet_type_waiting == Constants.PACKET_HELLO_ACK) {
						/* downlink established */
						m_real_dwn_contact = true;
						/* Reset the waiting counter */
						//TODO:
					}
				break;
				case Constants.PACKET_DWN_ACK:
					/* GS confirms the last download */
					if(m_rx_packet_type_waiting == Constants.PACKET_DWN_ACK) {
						// TODO: Send the next one
					}
				break;
				case Constants.PACKET_DWN_CLOSE:
					/* GS wants to close the downlink */
					generateCloseAckPacket();
					downloadPacket();
					// TODO: Reset counter
					m_rx_packet_type_waiting = Constants.PACKET_DWN_CLOSE_ACK;
					m_real_dwn_contact = false;
				break;
				case Constants.PACKET_DWN_CLOSE_ACK:
					/* GS confirms the closure - Nothing to do */
					if(m_rx_packet_type_waiting == Constants.PACKET_DWN_CLOSE_ACK) {
						m_logger.info(TAG + "GS has closed the downlink connection");
						/* Reset the counter */
					} else {
						m_logger.warning(TAG + "Received a DWN_CLOSE_ACK from GS when no CLOSE packet has been received previously; Something wrong with the GS?");
					}
				break;
				}
			} else if(m_waiting_packet == true && m_time.getTimeMillis() >= m_waiting_next) {
				/*
				 * No reply has been received, and the timeout has passed - retransmit or exit
				 */
				verifyLinkDead();
			}
			
			// TODO: move to the other site
			/* Check if real contact */
			if(m_real_dwn_contact == true) {
				/* Check if I have payload data */
				//if(m_payload_buffer.getSize() > 0) {
				//	data = m_payload_buffer.getBottomData();
					/* Format the packet to be downloaded */
				//	generateDataDownloadPacket(data);
					/* Download the packet */
				//	if(downloadPacket() == true) {
						/* Wait the correct reception - If not, retransmit */
						// TODO:
				//	}
				//}
			} else {
				/*if(m_time.getTime() >= cntct_end) {
					m_emulated_dwn_contact = false;
					/* Compute when the next downlink contact will start */
				/*	cntct_start = m_rand.nextDouble() * cntct_max_period; 
					if(cntct_start < cntct_min_period) {
						cntct_start = cntct_min_period;
					}
					cntct_start += m_time.getTime();
					/* Compute when the next downlink contact will stop */
				/*	cntct_end = m_rand.nextDouble() * cntct_max_duration;
					if(cntct_end < cntct_min_duration) {
						cntct_end = cntct_min_duration;
					}
					cntct_end += cntct_start;
				}
				
				if(m_emulated_dwn_contact == false && m_time.getTime() >= cntct_start) {
					m_emulated_dwn_contact = true;
				}*/
			}
			
			/* Sleep until waking up again */
			try {
				Thread.sleep(Constants.dwn_contact_sleep);
			} catch (InterruptedException e) {
				m_logger.error(e);
			}
			
		}
		
	}
	
	public boolean isContact() 
	{
		return (m_emulated_dwn_contact | m_real_dwn_contact);
	}
	
	public void controlledStop() 
	{
		m_exit = true;
	}
}
