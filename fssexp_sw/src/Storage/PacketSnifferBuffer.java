package Storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;
import InterSatelliteCommunications.Packet;
import Payload.PayloadDataBlock;

public class PacketSnifferBuffer {
	
	private File m_file;
	private Log m_logger;
	private TimeUtils m_time;
	private BufferedWriter m_writer;
	private PayloadDataBlock m_data_block;
	private ByteBuffer m_publish_data_stream;
	
    private String TAG = "[PacketSnifferBuffer] ";

    public PacketSnifferBuffer(Log log, TimeUtils time, FolderUtils folder) 
    {
    	m_time = time;
    	m_logger = log;
    	m_data_block = new PayloadDataBlock();
    	m_publish_data_stream = ByteBuffer.allocate(Integer.SIZE * 2 / 8);
    	m_file = new File(folder.sniffer_name);
        if(m_file.exists() == false) {
        	try {
				m_file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        try {
        	m_writer = new BufferedWriter(new FileWriter(m_file, true));
        } catch(IOException e) {
        	m_logger.error(e);
        }
    }
    
    public void insertRXPacket(Packet packet) 
    {
        String line = "[" + m_time.getTimeMillis() + "]";
        line += packet.toString();
        if(packet.length > 0) {
        	line += "::::";
        	if(packet.type == Constants.PACKET_FSS_DATA
        		|| packet.type == Constants.PACKET_DWN) {
        		m_data_block.fromBytes(packet.getData());
        		line += m_data_block.toString();
        	} else if(packet.type == Constants.PACKET_FSS_SERVICE_PUBLISH){
        		m_publish_data_stream.clear();
        		m_publish_data_stream.put(packet.getData());
        		m_publish_data_stream.rewind();
        		line += m_publish_data_stream.getInt() + "," + m_publish_data_stream.getInt();
        	} else {
        		m_publish_data_stream.clear();
        		m_publish_data_stream.put(packet.getData());
        		m_publish_data_stream.rewind();
        		line += m_publish_data_stream.getInt();
        	}
        }
        line += "\n";
        try {
			m_writer.write(line);
			m_writer.flush();
		} catch (IOException e) {
			m_logger.error(e);
		}
    }
}
