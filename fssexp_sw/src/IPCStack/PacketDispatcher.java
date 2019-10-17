package IPCStack;

import java.util.Hashtable;

import Common.Constants;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import InterSatelliteCommunications.Packet;
import Storage.Log;

public class PacketDispatcher extends Thread 
{

	private SimpleLinkProtocol m_ipc_stack;
	private Log m_logger;
	private SynchronizedBuffer m_packet_buffer;
	private SynchronizedBuffer m_hk_buffer;
	private Hashtable<Integer, SynchronizedBuffer> m_prot_buffers;
	private boolean m_exit;
	private Packet m_packet;
	
	public PacketDispatcher(Log log, ExperimentConf conf, TimeUtils timer)
	{
		m_logger = log;
		m_packet = new Packet();
		m_ipc_stack = new SimpleLinkProtocol(log, conf, timer);
		m_prot_buffers = new Hashtable<Integer, SynchronizedBuffer>();
		m_packet_buffer = new SynchronizedBuffer(log, "DispatcherBuffer");
		m_exit = false;
		/* Configure the RF ISL */
		//m_ipc_stack.updateConfiguration(conf);
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
		m_hk_buffer.write(m_packet.toBytes());
	}
	
	public void run()
	{
		byte[] packet_stream;
		byte[] header_stream = new byte[Constants.header_size];
		
		while(m_exit == false) 
		{	
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/* Check if incoming packet */
			while(m_hk_buffer.bytesAvailable() > 0) {
				/* Check the packet length to read all of it */
				m_hk_buffer.read(header_stream);
				m_packet.resetValues();
				m_packet.
				
				/* Request the telemetry */
				m_packet.rese
				
				/* As synchronized communications, add the result */
			}
		}
	}
}
