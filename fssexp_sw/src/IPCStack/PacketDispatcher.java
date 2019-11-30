package IPCStack;

import java.nio.ByteBuffer;
import java.util.Hashtable;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import InterSatelliteCommunications.Packet;
import Storage.PacketExchangeBuffer;
import Storage.PacketSnifferBuffer;

public class PacketDispatcher extends Thread 
{

	private SimpleLinkProtocol m_ipc_stack;
	private Log m_logger;
	private TimeUtils m_time;
	private SynchronizedBuffer m_packet_buffer;
	private SynchronizedBuffer m_hk_buffer;
	private Hashtable<Integer, SynchronizedBuffer> m_prot_buffers;
	private Hashtable<Integer, Integer> m_prot_status;
	private boolean m_exit;
	private boolean m_sniffer;
	private boolean m_broadcast_allowed;
	private Packet m_packet;
	private int m_sat_id;
	private PacketExchangeBuffer m_hk_packets;
	private PacketSnifferBuffer m_sniffed_packets;
	private ByteBuffer m_header_stream;
	private ByteBuffer m_data_stream;	
	private ByteBuffer m_checksum_stream;
	
	
	private String TAG = "[PacketDispatcher] ";
	
	public PacketDispatcher(Log log, ExperimentConf conf, TimeUtils timer, FolderUtils folder)
	{
		m_logger = log;
		m_time = timer;
		m_packet = new Packet();
		m_ipc_stack = new SimpleLinkProtocol(log, conf, timer);
		m_prot_buffers = new Hashtable<Integer, SynchronizedBuffer>();
		m_prot_status = new Hashtable<Integer, Integer>();
		m_packet_buffer = new SynchronizedBuffer(log, "DispatcherBuffer");
		m_hk_buffer = new SynchronizedBuffer(log, "TelemetryBuffer");
		m_hk_packets = new PacketExchangeBuffer(log, folder);
		m_sniffed_packets = new PacketSnifferBuffer(log, timer, folder);
		m_header_stream = ByteBuffer.allocate(Packet.getHeaderSize());
		m_checksum_stream = ByteBuffer.allocate(Packet.getChecksumSize());
		m_exit = false;
		m_sniffer = false;
		m_broadcast_allowed = true;
		m_sat_id = conf.satellite_id;
	}
	
	public void activateSniffer() { m_sniffer = true; }
	
	public void disableBroadcast() { m_broadcast_allowed = false; }
	
	public synchronized int accessRequestStatus(int protocol_number, int new_status, boolean write)
	{
		/* Status values are: 0 = working; 1 = delivered; 2 = IPC error; 3 = prot_num unknown */
		int status = 3;
		if(m_prot_buffers.containsKey(protocol_number) == true) {
			if(write == true) {
				m_prot_status.put(protocol_number, new_status);
				status = new_status;
			} else {
				status = m_prot_status.get(protocol_number);
			}
		}
		return status;
	}
	
	
	public void setSnifferMode() { m_sniffer = true; }
	
	
	public void addProtocolBuffer(int protocol_num, SynchronizedBuffer buff)
	{
		m_prot_buffers.put(protocol_num, buff);
		accessRequestStatus(protocol_num, 0, true); /* The first value is the working one */
	}
	
	public void transmitPacket(int protocol_num, Packet packet)
	{
		m_packet_buffer.write(packet.toBytes());
		accessRequestStatus(protocol_num, 0, true);
	}
	
	public void requestHK(int protocol_num)
	{
		m_packet.resetValues();
		m_packet.prot_num = protocol_num;
		m_packet.type = Constants.PACKET_TYPE_HK;
		m_hk_buffer.write(m_packet.toBytesNoData());
		accessRequestStatus(protocol_num, 0, true);
	}
	
	private boolean receivePacket() throws InterruptedException
	{
		boolean received = false;
		int received_checksum;
		byte[] packet_stream = m_ipc_stack.checkReceptionPacket();
		if(packet_stream.length > 0) {
			/* Something has been received */
			m_packet.fromBytes(packet_stream);
			received_checksum = m_packet.checksum;
			m_packet.computeChecksum();
			/* Store the type of the packet */
			m_hk_packets.insertRXPacket(m_packet);
			/* If sniffer mode - store it */
			if(m_sniffer == true) {
				m_sniffed_packets.insertRXPacket(m_packet);
			}
			/* Check if it has to be forwarded */
			if(m_broadcast_allowed == false 
				&& m_packet.destination == m_sat_id
				&& m_packet.checksum == received_checksum) {
					received  = true;
			} else if((m_packet.destination & m_sat_id) > 0
				&& m_packet.checksum == received_checksum) {
				received = true;
			} else {
				/* Discard the packet */
				m_packet.resetValues();
			}
		}
		return received;
	}
	
	public void run()
	{
		long time_tick;                             /* in ms */
        long spent_time;
        long time_to_sleep;
		byte[] packet_stream;
		byte[] data_stream;
        
		/* Open the IPC Stack */
		if(m_ipc_stack.open() == true) {
		
			/* Configure the RF ISL */
			// TODO:
			/* Exceptionally, initialize the IPC Stack to retrieve status - the only
	    	 * configuration parameter is the redundancy, and it is no explicitly needed
	    	 * to retrieve RF ISL Module status.
	    	 */
			//m_ipc_stack.updateConfiguration(conf);
			
			/* Nominal loop */
			while(m_exit == false) {
				/* Note the time */
				time_tick = m_time.getTimeMillis();
				
				/* Check if IPC stack has a RX packet */
				try {
					if(receivePacket() ==  true) {
						/* Forward the packet */
						m_prot_buffers.get(m_packet.prot_num).write(m_packet.toBytes());
					}
				} catch (InterruptedException e) {
					m_logger.error(e);
				}
				
				/* Check if incoming HK request */
				while(m_hk_buffer.bytesAvailable() >= m_header_stream.capacity()) {
					/* Check the packet length to read all of it */
					m_header_stream.clear();
					m_hk_buffer.read(m_header_stream.array());
					m_header_stream.rewind();
					m_packet.resetValues();
					m_packet.setHeader(m_header_stream.array());
					/* The remaining checksum */
					m_checksum_stream.clear();
					m_hk_buffer.read(m_checksum_stream.array());
					m_checksum_stream.rewind();
					
					if(m_packet.type == Constants.PACKET_TYPE_HK) {
						/* Request the telemetry */
						packet_stream = m_ipc_stack.getTelemetry();
						if(packet_stream.length > 0) {
							m_packet.setData(packet_stream);
							m_packet.length = packet_stream.length;
							/* Forward the packet */
							m_prot_buffers.get(m_packet.prot_num).write(m_packet.toBytes());
							accessRequestStatus(m_packet.prot_num, 1, true);	/* Correctly delivered */
						} else {
							m_logger.error(TAG + "[ERROR] Requested telemetry but not received any reply from IPC Stack");
							accessRequestStatus(m_packet.prot_num, 2, true);	/* Problem with IPC communications */
						}
					}
				}
					
				/* Check if incoming TX request */
				while(m_packet_buffer.bytesAvailable() >= m_header_stream.capacity()) {
					/* Check the packet length to read all of it */
					m_header_stream.clear();
					m_packet_buffer.read(m_header_stream.array());
					m_header_stream.rewind();
					m_packet.resetValues();
					m_packet.setHeader(m_header_stream.array());
					data_stream = new byte[m_packet.length];
					m_packet_buffer.read(data_stream);
					m_packet.setData(data_stream);
					/* The remaining checksum */
					m_checksum_stream.clear();
					m_packet_buffer.read(m_checksum_stream.array());
					m_checksum_stream.rewind();
					m_packet.setChecksum(m_checksum_stream.array());
					/* Send packet */
					try {
						if(m_ipc_stack.transmitPacket(m_packet.toBytes()) == false) {
							m_logger.error(TAG + "[ERROR] Impossible to send a packet through IPC Stack");
							accessRequestStatus(m_packet.prot_num, 2, true);	/* Problem with IPC communications */
						} else {
							m_hk_packets.insertTXPacket(m_packet);
							accessRequestStatus(m_packet.prot_num, 1, true);
							m_packet.resetValues();
						}
					} catch (InterruptedException e) {
						m_logger.error(e);
					}
				}
				
				/* Sleep */
				spent_time = m_time.getTimeMillis() - time_tick;
				if(spent_time < Constants.dispatcher_sleep) {
	            	try {
	                	time_to_sleep = Constants.dispatcher_sleep - spent_time;
						if(time_to_sleep <= Constants.dispatcher_sleep && time_to_sleep > 0) {
							sleep(time_to_sleep);
						} else {
							m_logger.error(TAG + "The time to sleep is different than the expected: " + time_to_sleep);
						}
	                } catch (InterruptedException e) {
	                    m_logger.error(e);
	                }
	            }
			}
			
			/* Close the IPC Stack */
			m_ipc_stack.close();
		} else {
			m_logger.error(TAG + "Impossible to open the IPC Stack - exit the thread");
		}
	}
	
    public void controlledStop() 
    { 
    	m_exit = true;
    }
}
