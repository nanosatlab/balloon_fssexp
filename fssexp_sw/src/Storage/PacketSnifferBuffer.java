package Storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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
	
    private String TAG = "[PacketSnifferBuffer] ";

    public PacketSnifferBuffer(Log log, TimeUtils time, FolderUtils folder) 
    {
    	m_time = time;
    	m_logger = log;
    	m_data_block = new PayloadDataBlock();
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
        	m_data_block.fromBytes(packet.getData());
        	line += m_data_block.toString();
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
