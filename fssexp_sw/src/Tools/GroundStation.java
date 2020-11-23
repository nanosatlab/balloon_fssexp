package Tools;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.Semaphore;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Downlink.DownloadedStorage;
import Housekeeping.RFISLHousekeepingItem;
import IPCStack.PacketDispatcher;
import InterSatelliteCommunications.Packet;

public class GroundStation extends Thread
{
	public int remote_sat;
	private int m_gs_id;
	private int m_prot_num;
	private int m_tx_packet_counter;
	private int m_rx_packet_timeout;
	private int m_rx_packet_backoff;
	private int m_hello_timeout;
	private int m_rx_packet_max;
	private boolean m_exit;
	private boolean m_established;
	private int m_alive_timeout;
	private int m_alive_counter;
	private long m_alive_next;
	private int m_alive_max;
	private int m_command;
	private boolean m_reply;
	private boolean m_waiting_packet;
	private int m_waiting_counter;
	private int m_waiting_timeout;
	private long m_waiting_next;
	private int m_waiting_max;
	private int m_rx_packet_type_waiting;
	private byte[] m_empty_data;
	private RFISLHousekeepingItem m_item;
	private Packet m_tx_packet;
	private Packet m_rx_packet;
	private TimeUtils m_time;
	private FolderUtils m_folder;
	private PacketDispatcher m_dispatcher;
	private SynchronizedBuffer m_rx_buffer;
	private Log m_log;
	private ExperimentConf m_conf;
	private Semaphore m_mutex;
	private ByteBuffer m_header_stream;
	private ByteBuffer m_data_stream;
	private ByteBuffer m_checksum_stream;
	private ByteBuffer m_telemetry_stream;
	private Random m_rand;
	private DownloadedStorage m_dwn_storage;
	
	private int GS_STATUS_STANDBY = Constants.TTC_STATUS_STANDBY;
	private int GS_STATUS_HANDSHAKING = Constants.TTC_STATUS_HANDSHAKING;
	private int GS_STATUS_CONNECTED = Constants.TTC_STATUS_CONNECTED;
	private int GS_STATUS_TERMINATION = Constants.TTC_STATUS_TERMINATION;
	private int m_status;
	
	private String TAG = "[GroundStation] ";
	
	public GroundStation(TimeUtils time) throws IOException
	{
		/* Configuration */
		m_time = time;
		System.out.println("[" + m_time.getTimeMillis() + "] Constructing folder tree...");
		m_folder = new FolderUtils(m_time);
        System.out.println("[" + m_time.getTimeMillis() + "] Folder tree constructed");
        m_log = new Log(m_time, m_folder);
		m_conf = new ExperimentConf(m_log, m_folder);
		m_gs_id = 1;	/* GS is always 0x01 */
		m_conf.satellite_id = m_gs_id;
		m_tx_packet_counter = 0;
		m_rx_packet_max = m_conf.ttc_retries;
		m_rx_packet_timeout = m_conf.ttc_timeout;		/* ms */
		m_hello_timeout = 8000;		/* ms */
		m_alive_timeout = 90 * 1000;	/* ms */
		m_waiting_timeout = m_rx_packet_timeout;
		m_waiting_max = m_rx_packet_max;
		m_alive_max = m_conf.ttc_retries;
		m_rx_packet_backoff = m_conf.ttc_backoff;	/* ms */
		
		/* Set the Dispatcher faster */
		Constants.dispatcher_sleep = 10;	/* ms */
		
		m_prot_num = Constants.ttc_prot_num;
		m_command = -1;
		remote_sat = -1;
		m_waiting_packet = false;
		m_waiting_counter = 0;
		m_waiting_next = 0;
		m_alive_next = 0;
		m_alive_counter = 0;
		m_established = false;
		m_status = GS_STATUS_STANDBY;
		m_rx_packet_type_waiting = Constants.FSS_PACKET_NOT_VALID;
		m_tx_packet = new Packet();
		m_rx_packet = new Packet();
		m_empty_data = new byte[0];
		
		m_dispatcher = new PacketDispatcher(m_log, m_conf, m_time, m_folder);
		m_rx_buffer = new SynchronizedBuffer(m_log, "rx-buffer-gs");
		m_dispatcher.addProtocolBuffer(m_prot_num, m_rx_buffer);
		m_mutex = new Semaphore(0);
		m_item = new RFISLHousekeepingItem();
		m_rand = new Random(m_time.getTimeMillis());
		m_dwn_storage = new DownloadedStorage(m_log, m_folder, m_time);
		
		m_header_stream = ByteBuffer.allocate(Packet.getHeaderSize());
		m_checksum_stream = ByteBuffer.allocate(Packet.getChecksumSize());
		m_telemetry_stream = ByteBuffer.allocate(RFISLHousekeepingItem.getRawSize());
		
		/* As a sniffer and no broadcast */
		m_dispatcher.disableBroadcast();
		m_dispatcher.activateSniffer();
	}
	
	public boolean isTransceiverConnected()
	{
		boolean connected = false;
		m_dispatcher.requestHK(m_prot_num);
		while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
    		try {
    			/* Wait and release the processor */
            	Thread.sleep(10);
            } catch(InterruptedException e) {
            	e.printStackTrace();
            }
    	}
		
		if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 1) {
			// TODO: verify why this crashes when a packet is received previously
			m_header_stream.clear();
			m_rx_buffer.read(m_header_stream.array());
			m_telemetry_stream.clear();
			m_rx_buffer.read(m_telemetry_stream.array());
			m_checksum_stream.clear();
			m_rx_buffer.read(m_checksum_stream.array());
			connected = true;
		}
		return connected;
	}
	
	public RFISLHousekeepingItem getTransceiverTelemetry()
	{
		RFISLHousekeepingItem item = new RFISLHousekeepingItem();
		m_dispatcher.requestHK(m_prot_num);
		while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
    		try {
    			/* Wait and release the processor */
            	Thread.sleep(10);
            } catch(InterruptedException e) {
            	e.printStackTrace();
            }
    	}
		if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 1) {
			m_header_stream.clear();
			m_rx_buffer.read(m_header_stream.array());
			m_telemetry_stream.clear();
			m_rx_buffer.read(m_telemetry_stream.array());
			m_telemetry_stream.rewind();
			m_checksum_stream.clear();
			m_rx_buffer.read(m_checksum_stream.array());
			item.parseFromBytes(m_telemetry_stream.array());
		}
		return item;
	}
	
	private void setHeaderPacket(Packet packet, int type, int address, int data_length) 
	{
		packet.source = m_gs_id;
		packet.destination = address;
		packet.prot_num = m_prot_num;
		packet.timestamp = m_time.getTimeMillis();
		packet.counter = m_tx_packet_counter;
		packet.type = type;
		packet.length = data_length;
	}
	
	private void generateHelloPacket(int address)
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_HELLO, address, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateHelloAckPacket(int address)
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_HELLO_ACK, address, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateDataAckPacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_ACK, remote_sat, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateClosePacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_CLOSE, remote_sat, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateCloseAckPacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_CLOSE_ACK, remote_sat, 0);
		m_tx_packet.computeChecksum();
	}

	private void generateAlivePacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_ALIVE, remote_sat, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void generateAliveAckPacket()
	{
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_DWN_ALIVE_ACK, remote_sat, 0);
		m_tx_packet.computeChecksum();
	}
	
	private void retransmitPacket() 
	{
		/* Retransmit */
		System.out.println("[" + m_time.getTimeMillis() + "] No replication - Retransmit Packet type " + m_tx_packet.type);
		m_log.info(TAG + "No replication - Retransmit Packet type " + m_tx_packet.type);
		Packet temp_packet = new Packet(m_tx_packet);
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, temp_packet.type, temp_packet.destination, temp_packet.length);
		m_tx_packet.setData(temp_packet.getData());
		m_tx_packet.computeChecksum();
		transmitPacket();
		/* Compute the next waiting time */
		lockForPacket(m_tx_packet.type, m_waiting_timeout);
	}
	
	private boolean transmitPacket()
	{
		boolean transmitted = false;
		m_dispatcher.transmitPacket(m_prot_num, m_tx_packet);
		while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
    		try {
    			/* Wait and release the processor */
            	Thread.sleep(10);
            } catch(InterruptedException e) {
            	e.printStackTrace();
            }
    	}
		
		if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 1) {
			transmitted = true;
			if((m_tx_packet.type & (Constants.PACKET_HELLO | Constants.PACKET_HELLO_ACK | Constants.PACKET_DWN_CLOSE | Constants.PACKET_DWN_CLOSE_ACK)) > 0) {
				System.out.println("[" + m_time.getTimeMillis() + "] Transmitted packet: "
						+ "src = " + m_tx_packet.source + " | dst = " + m_tx_packet.destination
						+ " | prot_num = " + m_tx_packet.prot_num + " | timestamp = " + m_tx_packet.timestamp 
						+ " | counter = " + m_tx_packet.counter + " | type = " + m_tx_packet.type 
						+ " | length = " + m_tx_packet.length);
			}
			m_log.info(TAG + "Transmitted packet: "
					+ "src = " + m_tx_packet.source + " | dst = " + m_tx_packet.destination
					+ " | prot_num = " + m_tx_packet.prot_num + " | timestamp = " + m_tx_packet.timestamp 
					+ " | counter = " + m_tx_packet.counter + " | type = " + m_tx_packet.type 
					+ " | length = " + m_tx_packet.length);
			m_tx_packet_counter += 1;
			m_tx_packet.counter = m_tx_packet_counter;
		} else {
			System.out.println("[" + m_time.getTimeMillis() + "][ERROR] Impossible to transmit a packet through the RF ISL Module");
			m_log.error(TAG + "Impossible to transmit a packet through the RF ISL Module");
		}
		return transmitted;
	}
	
	private boolean receivePacket()
	{
		boolean received = false;
		if(m_rx_buffer.bytesAvailable() > 0) {
			m_rx_packet.resetValues();
			m_header_stream.clear();
			m_rx_buffer.read(m_header_stream.array());
			m_rx_packet.setHeader(m_header_stream.array());
			if(m_rx_packet.length > 0) {
				m_data_stream = ByteBuffer.allocate(m_rx_packet.length);
				m_rx_buffer.read(m_data_stream.array());
				m_rx_packet.setData(m_data_stream.array());
			}
			m_checksum_stream.clear();
			m_rx_buffer.read(m_checksum_stream.array());
			m_rx_packet.setChecksum(m_checksum_stream.array());
			
			/* Received a packet */
			if(m_established == true 
				&& m_rx_packet.source == accessToRemote(0, false)) {
				m_alive_next = m_time.getTimeMillis() + m_alive_timeout;
			}
			
			/* Received a packet for me that I am expecting*/
			if(m_rx_packet.destination == m_gs_id) {
				received = true;
				if((m_rx_packet.type & (Constants.PACKET_HELLO_ACK | Constants.PACKET_DWN_CLOSE_ACK)) > 0) {
					System.out.println("[" + m_time.getTimeMillis() + "] Received packet: "
							+ "src = " + m_rx_packet.source + " | dst = " + m_rx_packet.destination
							+ " | type = " + m_rx_packet.type + " | prot_num = " + m_rx_packet.prot_num
							+ " | counter = " + m_rx_packet.counter);
				}
				m_log.info(TAG + "Received packet: "
						+ "src = " + m_rx_packet.source + " | dst = " + m_rx_packet.destination
						+ " | type = " + m_rx_packet.type + " | prot_num = " + m_rx_packet.prot_num
						+ " | counter = " + m_rx_packet.counter);
			} else {
				System.out.println("[" + m_time.getTimeMillis() + "] Received packet, but discarded; " + m_rx_packet.toString());
				m_rx_packet.resetValues();
			}
		}
		return received;
	}
	
	public boolean establishConnection()
	{
		boolean done = false; 
		/* Send the Hello packet */
		generateHelloPacket(remote_sat);
		if(transmitPacket() == true) {
			/* Change the state */
			m_status = GS_STATUS_HANDSHAKING;
			/* I expect to receive a HELLO ACK */
			lockForPacket(Constants.PACKET_HELLO_ACK, m_hello_timeout);
			done = true;
		} 
		return done;
	}
	
	public boolean closeConnection()
	{
		boolean done = false;
		/* Send the Close packet */
		generateClosePacket();
		if(transmitPacket() == true) {
			/* Change the state */
			m_status = GS_STATUS_TERMINATION;
			/* I expect to receive a CLOSE ACK */
			lockForPacket(Constants.PACKET_DWN_CLOSE_ACK, m_rx_packet_timeout);
			done = true;
		}
		return done;
	}

	public boolean connectWithBalloon(int address)
	{
		m_reply = false;
		accessToCommand(0, true);
		try {
			/* wait to reply the thread */
			m_mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return m_reply;
	}
	
	public boolean reset()
	{
		m_reply = false;
		accessToCommand(4, true);
		try {
			/* wait to reply the thread */
			m_mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return m_reply;
	}
	
	public boolean disconnectWithBalloon()
	{
		m_reply = false;
		accessToCommand(1, true);
		try {
			/* wait to reply the thread */
			m_mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return m_reply;
	}
	
	public boolean checkTransceiver()
	{
		m_reply = false;
		accessToCommand(2, true);
		try {
			/* wait to reply the thread */
			m_mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return m_reply;
	}
	
	public RFISLHousekeepingItem requestTelemetry()
	{
		m_item.resetValues();
		accessToCommand(3, true);
		try {
			/* wait to reply the thread */
			m_mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return m_item;
	}
	
	private synchronized int accessToCommand(int new_command, boolean write)
	{
		if(write == true) {
			m_command = new_command;
		}
		return m_command;
	}
	
	private synchronized int accessToRemote(int remote, boolean write)
	{
		if(write == true) {
			remote_sat = remote;
		}
		return remote_sat;
	}
	
	public void controlledStop()
	{
		m_exit = true;
	}
	
	private void releaseForPacket() 
	{
		m_waiting_packet = false;
		m_waiting_counter = 0;
	}
	
	private void lockForPacket(int packet_type, int timeout) 
	{
		m_waiting_packet = true;
		m_rx_packet_type_waiting = packet_type;
		m_waiting_next = m_time.getTimeMillis() + timeout;
		System.out.println("[" + m_time.getTimeMillis() + "] Expecting to receive packet type " + packet_type + " before " + m_waiting_next);
	}
	
	private void resetAliveSequence()
	{
		m_alive_next = m_time.getTimeMillis() +  m_alive_timeout;
		m_alive_counter = 0;
	}
	
	public void run()
	{
		int command;
		long backoff;
		m_exit = false;
		
		/* Start dispatcher */
		m_dispatcher.start();
		
		while(m_exit == false) {
			
			/* Check if something is received */
			if(receivePacket() == true) {
				if(m_waiting_packet == true && (m_rx_packet_type_waiting & m_rx_packet.type) != 0) {
					/*
					 * I have received the packet that I was waiting - I can release and keep
					 * working
					 */
					releaseForPacket();
					if(m_status == GS_STATUS_CONNECTED) {
						resetAliveSequence();
					}
				}
				/* Something is received, perform according */
				switch(m_rx_packet.type) {
				case Constants.PACKET_HELLO_ACK:
					if(m_status == GS_STATUS_HANDSHAKING) {
						/* reply with a HELLO ack */
						generateHelloAckPacket(remote_sat);
						transmitPacket();
						/* Connection established */
						m_status = GS_STATUS_CONNECTED;
						m_established = true;
						/* Trigger the ALIVE packets */
						resetAliveSequence();
						/* Notify the Ground Segment */
						m_reply = true;
						m_mutex.release();
					} 
					break;
				case Constants.PACKET_DWN_ALIVE:
					if(m_status == GS_STATUS_CONNECTED) {
						/* Reply with an ALIVE ACK if connected */
						generateAliveAckPacket();
						transmitPacket();
						resetAliveSequence();
						/* I do not expect to receive any other message */
						releaseForPacket();
					}
					break;
				case Constants.PACKET_DWN:
					if(m_status == GS_STATUS_CONNECTED) {
						/* Store the data */
						m_dwn_storage.writePacket(m_rx_packet);
						/* Reply with an ACK */
						generateDataAckPacket();
						transmitPacket();
						/* Reset the Alive sequence */
						resetAliveSequence();
					}
					break;
				case Constants.PACKET_DWN_ALIVE_ACK:
					if(m_status == GS_STATUS_CONNECTED) {
						resetAliveSequence();
					}
					break;
				case Constants.PACKET_DWN_CLOSE_ACK:
					if(m_status == GS_STATUS_TERMINATION) {
						/* Reply with CLOSE ACK */
						generateCloseAckPacket();
						transmitPacket();
						/* Connection terminated */
						m_status = GS_STATUS_STANDBY;
						m_established = false;
						remote_sat = -1;
						/* Notify the Ground Segment */
						m_reply = true;
						m_mutex.release();
					}
					break;
				}	
			} else if(m_waiting_packet == true 
				&& m_time.getTimeMillis() >= m_waiting_next
				&& m_waiting_counter < m_waiting_max) {
				/* schedule retransmission */
				try {
					backoff = (long)(m_rand.nextFloat() * m_rx_packet_backoff);
					System.out.println("[" + m_time.getTimeMillis() + "] Retransmission of packet " + m_tx_packet.type + " scheduled after " + backoff + " ms");
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
				m_status = GS_STATUS_STANDBY;
				m_established = false;
				remote_sat = -1;
				releaseForPacket();
				/* Notify the Ground Segment if there it is blocked */
				if(m_mutex.getQueueLength() > 0) {
					m_mutex.release();
				}
			}
			
			/* Execute the mode if connected */
			if(m_status == GS_STATUS_CONNECTED) {
				/* Verify if ALIVE packet has to be sent */
				if(m_time.getTimeMillis() >= m_alive_next
					&& m_alive_counter < m_alive_max) {
					/* Transmit ALIVE packet */
					generateAlivePacket();
					transmitPacket();
					lockForPacket(Constants.PACKET_DWN_ALIVE_ACK, m_rx_packet_timeout);
					/* Reset Alive counter */
					resetAliveSequence();
					m_alive_counter += 1;
				} else if(m_time.getTimeMillis() >= m_alive_next) {
					/* No packet received, thus lost of connection */
					releaseForPacket();
					m_status = GS_STATUS_STANDBY;
					m_established = false;
					remote_sat = -1;
					System.out.println("[" + m_time.getTimeMillis() + "][WARNING] No packet received during " + m_alive_timeout + " ms; Lost of connection with Baloon");
				}
			}
			
			/* Check command from GroundSegment */
			command = accessToCommand(0, false);
			if(command != -1) {
				switch(command) {
				case 0:	/* connect with balloon */
					if(m_status == GS_STATUS_STANDBY) {
						establishConnection();
					} else {
						System.out.println("[" + m_time.getTimeMillis() + "] The GS is not in stanby mode; It is currently in " + m_status);
						/* Notify the Ground Segment */
						m_reply = false;
						m_mutex.release();
					}
					break;
				case 1:	/* disconnect balloon */
					if(m_status == GS_STATUS_CONNECTED) {
						closeConnection();
					} else {
						System.out.println("[" + m_time.getTimeMillis() + "] The GS is not in connected mode; It is currently in " + m_status);
						/* Notify the Ground Segment */
						m_reply = false;
						m_mutex.release();
					}
					break;
				case 2:	/* check transceiver */
					m_reply = isTransceiverConnected();
					/* Notify the Ground Segment */
					m_mutex.release();
					break;
				case 3: /* request telemetry */
					m_item = getTransceiverTelemetry();
					/* Notify the Ground Segment */
					m_mutex.release();
					break;
				case 4: /* reset the status */
					m_status = GS_STATUS_STANDBY;
					m_reply = true;
					/* Notify the Ground Segment */
					m_mutex.release();
					break;
				}
				accessToCommand(-1, true);
			}
			
			/* Sleep a while */
			try {
				sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/* Stop dispatcher */
		m_dispatcher.controlledStop();
	}
}
