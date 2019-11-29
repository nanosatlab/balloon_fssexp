package Downlink;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;
import Common.Log;
import InterSatelliteCommunications.Packet;

public class DownloadedStorage {
	private PrintWriter m_writer;

    public DownloadedStorage(Log log, FolderUtils folder)
    {
        try {
			m_writer = new PrintWriter(folder.dwn_name);
		} catch (FileNotFoundException e) {
			log.error(e);
		}
    }
    
    public void writePacket(Packet packet) 
    {
            m_writer.write(packet.toString() + "\n");
            m_writer.flush();
    }
    
    public void writeString(String str) 
    {
        m_writer.write(str);
        m_writer.flush();
    }
}
