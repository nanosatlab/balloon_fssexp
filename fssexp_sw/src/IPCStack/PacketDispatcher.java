package IPCStack;

import java.util.Hashtable;

import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import InterSatelliteCommunications.Packet;

public class PacketDispatcher extends Thread 
{

	private SimpleLinkProtocol m_ipc_stack;
	private Log m_logger;
	private TimeUtils m_time;
	private SynchronizedBuffer m_packet_buffer;
	private SynchronizedBuffer m_hk_buffer;
	private Hashtable<Integer, SynchronizedBuffer> m_prot_buffers;
	private boolean m_exit;
	private Packet m_packet;
	
	private String TAG = "[PacketDispatcher] ";
	
	public PacketDispatcher(Log log, ExperimentConf conf, TimeUtils timer)
	{
		m_logger = log;
		m_time = timer;
		m_packet = new Packet();
		m_ipc_stack = new SimpleLinkProtocol(log, conf, timer);
		m_prot_buffers = new Hashtable<Integer, SynchronizedBuffer>();
		m_packet_buffer = new SynchronizedBuffer(log, "DispatcherBuffer");
		m_hk_buffer = new SynchronizedBuffer(log, "TelemetryBuffer");
		m_exit = false;
	}
	
	public void addProtocolBuffer(int protocol_num, SynchronizedBuffer buff)
	{
		m_prot_buffers.put(protocol_num, buff);
	}
	
	public void transmitPacket(Packet packet)
	{
		m_packet_buffer.write(packet.toBytes());
	}
	
	public void requestHK(int protocol_num)
	{
		m_packet.resetValues();
		m_packet.prot_num = protocol_num;
		m_packet.type = Constants.PACKET_TYPE_HK;
		m_hk_buffer.write(m_packet.toBytesNoData());
	}
	
	public void run()
	{
		byte[] packet_stream;
		byte[] data_stream;
		byte[] header_stream = new byte[Constants.header_size];
		byte[] checksum_stream = new byte[Short.SIZE / 8];
		long time_tick;                             /* in ms */
        long spent_time;
        long time_to_sleep;
		
		/* Open the IPC Stack */
		if(m_ipc_stack.open() == true) {
		
			/* Configure the RF ISL */
			//m_ipc_stack.updateConfiguration(conf);
			
			/* Nominal loop */
			while(m_exit == false) {
				/* Note the time */
				time_tick = m_time.getTimeMillis();
				
				/* Check if IPC stack has a packet */
				try {
					packet_stream = m_ipc_stack.checkReceptionPacket();
					if(packet_stream.length > 0) {
						/* Parse Packet */
						m_packet.fromBytes(packet_stream);
						/* Forward the packet */
						m_prot_buffers.get(m_packet.prot_num).write(m_packet.toBytes());
					}
				} catch (InterruptedException e) {
					m_logger.error(e);
				}
				
				/* Check if incoming HK request */
				while(m_hk_buffer.bytesAvailable() >= header_stream.length) {
					/* Check the packet length to read all of it */
					m_hk_buffer.read(header_stream);
					m_packet.resetValues();
					m_packet.setHeader(header_stream);
					/* The remaining checksum */
					m_hk_buffer.read(checksum_stream);
					
					if(m_packet.type == Constants.PACKET_TYPE_HK) {
						/* Request the telemetry */
						try {
							packet_stream = m_ipc_stack.getTelemetry();
							if(packet_stream.length > 0) {
								m_packet.setData(packet_stream);
								m_packet.length = packet_stream.length;
								/* Forward the packet */
								m_prot_buffers.get(m_packet.prot_num).write(m_packet.toBytes());
							} else {
								m_logger.error(TAG + "[ERROR] Requested telemetry but not received any reply from IPC Stack");
							}
						} catch (InterruptedException e) {
							m_logger.error(e);
						}
					}
				}
					
				/* Check if incoming TX request */
				while(m_packet_buffer.bytesAvailable() >= header_stream.length) {
					/* Check the packet length to read all of it */
					m_packet_buffer.read(header_stream);
					m_packet.resetValues();
					m_packet.setHeader(header_stream);
					data_stream = new byte[m_packet.length];
					m_packet_buffer.read(data_stream);
					m_packet.setData(data_stream);
					/* The remaining checksum */
					m_packet_buffer.read(checksum_stream);
					
					/* Send packet */
					try {
						if(m_ipc_stack.transmitPacket(m_packet.toBytes()) == false) {
							m_logger.error(TAG + "[ERROR] Impossible to send a packet through IPC Stack");
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
	            } else {
	            	m_logger.info(TAG + "No sleep, the process consumed " + spent_time);
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
