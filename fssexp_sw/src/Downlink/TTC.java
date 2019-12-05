package Downlink;

import java.lang.Thread;
import java.nio.ByteBuffer;
import java.util.Random;

import Configuration.ExperimentConf;
import IPCStack.PacketDispatcher;
import InterSatelliteCommunications.Packet;
import Payload.PayloadDataBlock;
import Storage.FederationPacketsBuffer;
import Storage.PayloadBuffer;
import Common.TimeUtils;
import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;

public class TTC extends Thread 
{

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
	private FederationPacketsBuffer m_federation_buffer;
	private int m_sat_id;
	private int m_gs_id;
	private int m_dwn_packet_counter;
	private int m_rx_packet_type_waiting;
	private boolean m_waiting_packet;
	private int m_waiting_counter;
	private int m_waiting_timeout;
	private long m_waiting_next;
	private int m_waiting_max;
	private int m_status;
	private int m_alive_timeout;
	private int m_alive_counter;
	private long m_alive_next;
	private int m_alive_max;
	private int m_rx_packet_backoff;
	private int m_rx_packet_timeout;
	private int m_dwn_packet_from;
	private boolean m_service_available;
	
	private Packet m_tx_packet;
	private Packet m_rx_packet;
	private ByteBuffer m_header_stream;
	private ByteBuffer m_data_stream;
	private ByteBuffer m_checksum_stream;
	
	
	private final static String TAG = "[TTC] ";
	
	public TTC(TimeUtils time, ExperimentConf conf, Log logger, PacketDispatcher dispatcher, PayloadBuffer payload_buffer, FederationPacketsBuffer fed_buffer) 
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
		m_federation_buffer = fed_buffer;
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
		m_status = Constants.TTC_STATUS_STANDBY;
		m_alive_next = 0;
		m_alive_counter = 0;
		m_dwn_packet_from = -1;
	}
	
	public void setConfiguration(ExperimentConf conf) 
	{
		m_waiting_timeout = conf.ttc_timeout;
		m_waiting_max = conf.ttc_retries;
		cntct_max_period = conf.cntct_max_period;
		cntct_min_period = conf.cntct_min_period;
		cntct_max_duration = conf.cntct_max_duration;
		cntct_min_duration = conf.cntct_min_duration;
		m_alive_max = 2;
		m_alive_timeout = 60 * 1000;	/* ms */
		m_rx_packet_backoff = conf.ttc_backoff;	/* ms */
		m_rx_packet_timeout = 4000;		/* ms */
		
		System.out.println("Configuration: " + m_waiting_timeout + ":" + m_waiting_max + ":" + m_rx_packet_backoff);
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
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN, m_gs_id, content.length);
		/* set the content */
		m_tx_packet.setData(content);
		/* set the checksum */
		m_tx_packet.computeChecksum();
	}
	
	private void generateHelloAckPacket()
	{
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
	
	private void generateAlivePacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_ALIVE, m_gs_id, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateAliveAckPacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_ALIVE_ACK, m_gs_id, 0);
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
			m_logger.info(TAG + "[" + m_time.getTimeMillis() + "] Transmitted packet with header: "
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
			m_logger.info(TAG + "Received packet with header: "
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
				m_logger.info(TAG + "Discarded packet: " + m_rx_packet.toString());
				m_rx_packet.resetValues();
			}		
		}		
		return done;
	}
	
	private void releaseForPacket() 
	{
		m_logger.info(TAG + "Release the packet " + m_rx_packet_type_waiting);
		m_waiting_packet = false;
		m_waiting_counter = 0;
	}
	
	private void lockForPacket(int packet_type, int timeout) 
	{
		m_waiting_packet = true;
		m_rx_packet_type_waiting = packet_type;
		m_waiting_next = m_time.getTimeMillis() + timeout;
		m_logger.info(TAG + "Expecting to receive packet type " + packet_type + " before " + m_waiting_next);
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
		/* Compute the next waiting time */
		lockForPacket(m_rx_packet_type_waiting, m_waiting_timeout);
	}
	
	private void resetAliveSequence()
	{
		m_alive_next = m_time.getTimeMillis() +  m_alive_timeout;
		m_alive_counter = 0;
	}
	
	private void downloadDataBlock(PayloadDataBlock block) 
	{
		generateDataDownloadPacket(block.getBytes());
		downloadPacket();
		lockForPacket(Constants.PACKET_DWN_ACK, m_rx_packet_timeout);
	}
	
	private void releaseSimulatedContact()
	{
		
	}
	
	
	public void run() 
	{
		long backoff;
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
					/* Update the TX counter - we can send a new one */
					m_dwn_packet_counter += 1;
					releaseForPacket();
					if(m_status == Constants.TTC_STATUS_CONNECTED) {
						resetAliveSequence();
					}
				}
				
				/* Something is received - check if it is an DWN_ACK */
				switch(m_rx_packet.type) {
				case Constants.PACKET_HELLO:
					if(m_status == Constants.TTC_STATUS_STANDBY) {
						/* Change the status */
						m_status = Constants.TTC_STATUS_HANDSHAKING;
						/* GS is requesting to have a downlink connection */
						generateHelloAckPacket();
						if(downloadPacket() == true) {
							/* Wait to confirm the downlink connection */
							lockForPacket(Constants.PACKET_HELLO_ACK, m_rx_packet_timeout);
						}
					}
				break;
				case Constants.PACKET_HELLO_ACK:
					if(m_status == Constants.TTC_STATUS_HANDSHAKING) {
						/* downlink established */
						m_status = Constants.TTC_STATUS_CONNECTED;
						m_real_dwn_contact = true;
						/* If no data, force an ALIVE packet in the next loop */
						if(m_payload_buffer.getSize() == 0 && m_federation_buffer.getSize() == 0) {
							m_alive_next = 0;
						} else {
							/* Schedule the next ALIVE packet */
							resetAliveSequence();
						}
					}
				break;
				case Constants.PACKET_DWN_ALIVE:
					if(m_status == Constants.TTC_STATUS_CONNECTED) {
						/* Reply the ALIVE ACK */
						generateAliveAckPacket();
						downloadPacket();
						/* Wait a reply */
						lockForPacket(Constants.PACKET_DWN_ALIVE_ACK, m_rx_packet_timeout);
						/* reset the Alive sequence */
						resetAliveSequence();
					}
				break;
				case Constants.PACKET_DWN_ALIVE_ACK:
					if(m_status == Constants.TTC_STATUS_CONNECTED) {
						/* reset the Alive sequence */
						resetAliveSequence();
					}
				break;
				case Constants.PACKET_DWN_ACK:
					if(m_status == Constants.TTC_STATUS_CONNECTED) {
						/* TODO: Remove the corresponding packet of the buffer (Payload and FSS) */
						if(m_dwn_packet_from == 0) {
							m_payload_buffer.deleteBottomData();
						} else if(m_dwn_packet_from == 1){
							m_federation_buffer.deleteBottomData();
						}
						/* Reset the last downloaded packet */
						m_dwn_packet_from = -1;
						/* reset the ALIVE sequence */
						resetAliveSequence();
					}
				break;
				case Constants.PACKET_DWN_CLOSE:
					if(m_status == Constants.TTC_STATUS_CONNECTED) {
						accessToServiceAvailable(false, true);
						m_status = Constants.TTC_STATUS_TERMINATION;
						generateCloseAckPacket();
						downloadPacket();
						lockForPacket(Constants.PACKET_DWN_CLOSE_ACK, m_rx_packet_timeout);
					}
				break;
				case Constants.PACKET_DWN_CLOSE_ACK:
					if(m_status == Constants.TTC_STATUS_TERMINATION) {
						/* Reset the counter */
						m_status = Constants.TTC_STATUS_STANDBY;
						m_real_dwn_contact = false;
					}
				break;
				}
			} else if(m_waiting_packet == true 
				&& m_time.getTimeMillis() >= m_waiting_next
				&& m_waiting_counter < m_waiting_max) {
				/* schedule retransmission */
				try {
					backoff = (long)(m_rand.nextFloat() * m_rx_packet_backoff * (m_waiting_counter + 1));
					m_logger.info(TAG + "Retransmission of packet " + m_tx_packet.type + " scheduled (backoff:" + backoff + " ms)");
					sleep(backoff);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				retransmitPacket();
				m_waiting_counter++;
			} else if(m_waiting_packet == true 
				&& m_time.getTimeMillis() >= m_waiting_next) {
				/* Connection lost */
				System.out.println("[" + m_time.getTimeMillis() + "] Maximum reply of Packet type " + m_tx_packet.type + " reached. The connection is dead!");
				accessToServiceAvailable(false, true);
				m_status = Constants.TTC_STATUS_STANDBY;
				m_real_dwn_contact = false;
				releaseForPacket();
			}
			
			/* Execute the mode if connected */
			if(m_status == Constants.TTC_STATUS_CONNECTED && m_real_dwn_contact == true) {
				/* Verify if ALIVE packet has to be sent */
				if(m_time.getTimeMillis() >= m_alive_next
					&& m_alive_counter < m_alive_max) {
					/* Transmit ALIVE packet */
					generateAlivePacket();
					downloadPacket();
					lockForPacket(Constants.PACKET_DWN_ALIVE_ACK | Constants.PACKET_DWN_ALIVE, m_rx_packet_timeout);
					/* Reset Alive counter */
					resetAliveSequence();
					m_alive_counter += 1;
				} else if(m_time.getTimeMillis() >= m_alive_next) {
					/* No packet received, thus lost of connection */
					releaseForPacket();
					m_status = Constants.TTC_STATUS_STANDBY;
					m_real_dwn_contact = false;
					m_logger.info(TAG + "No packet received during " + m_alive_timeout + " ms; Lost of connection with Baloon");
				}
			}
			
			/* If there is downlink connection, check to download */
			if(isConnected() == true && m_waiting_packet == false) {
				/* Check if there is payload data to download */
				if(m_payload_buffer.getSize() > 0) {
					downloadDataBlock(m_payload_buffer.getBottomDataBlock());
					resetAliveSequence();
					m_dwn_packet_from = 0;
				} else if(m_federation_buffer.getSize() > 0){
					/* Check if there is data in the FSS Buffer */
					downloadDataBlock(m_federation_buffer.getBottomDataBlock());
					resetAliveSequence();
					m_dwn_packet_from = 1;
				} else {
					/* Downlink is available to be published */
					accessToServiceAvailable(true, true);
				}
			}
			
			// TODO: move to the other site
			/* Check if real contact */
			if(m_real_dwn_contact == false) {
				if(m_time.getTime() >= cntct_end) {
					m_emulated_dwn_contact = false;
					/* Compute when the next downlink contact will start */
					cntct_start = m_rand.nextDouble() * cntct_max_period; 
					if(cntct_start < cntct_min_period) {
						cntct_start = cntct_min_period;
					}
					cntct_start += m_time.getTime();
					/* Compute when the next downlink contact will stop */
					cntct_end = m_rand.nextDouble() * cntct_max_duration;
					if(cntct_end < cntct_min_duration) {
						cntct_end = cntct_min_duration;
					}
					cntct_end += cntct_start;
				}
				
				if(m_emulated_dwn_contact == false && m_time.getTime() >= cntct_start) {
					m_emulated_dwn_contact = true;
				}
			}
			
			/* Sleep until waking up again */
			try {
				Thread.sleep(Constants.dwn_contact_sleep);
			} catch (InterruptedException e) {
				m_logger.error(e);
			}
			
		}
		
	}
	
	public boolean isConnected() 
	{
		return (m_emulated_dwn_contact | m_real_dwn_contact);
	}
	
	public void controlledStop() 
	{
		m_exit = true;
	}

	public synchronized boolean accessToServiceAvailable(boolean new_avail, boolean write)
	{
		boolean available;
		if(write == true) {
			m_service_available = new_avail;
		} 
		available = m_service_available;
		return available;
	}
}
