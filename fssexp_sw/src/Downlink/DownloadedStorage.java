package Downlink;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;
import InterSatelliteCommunications.Packet;
import Payload.PayloadDataBlock;

public class DownloadedStorage 
{
	private PrintWriter m_writer;
	private TimeUtils m_time;
	private PayloadDataBlock m_data_container;
	
    public DownloadedStorage(Log log, FolderUtils folder, TimeUtils time)
    {
    	m_time = time;
    	m_data_container = new PayloadDataBlock();
    	try {
			m_writer = new PrintWriter(folder.dwn_name);
		} catch (FileNotFoundException e) {
			log.error(e);
		}
    }
    
    public void writePacket(Packet packet) 
    {
    	m_data_container.resetValues();
    	m_data_container.fromBytes(packet.getData());
    	String s = "[" + m_time.getTimeMillis() + "]" + packet.toString();
    	s += "::::" + m_data_container.toString() + "\n";
        writeString(s);
    }
    
    public void writeString(String str) 
    {
        m_writer.write(str);
        m_writer.flush();
    }
}
