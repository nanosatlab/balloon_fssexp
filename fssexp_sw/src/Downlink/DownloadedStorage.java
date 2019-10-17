package Downlink;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;
import InterSatelliteCommunications.Packet;

public class DownloadedStorage {
	private PrintWriter m_writer;

    public DownloadedStorage(FolderUtils folder) throws FileNotFoundException {
        m_writer = new PrintWriter(folder.payload_name);
    }
    
    public void writePacket(Packet packet) {
            m_writer.write(packet.toString());
            m_writer.flush();
    }
    
    public void writeString(String str) {
        m_writer.write(str);
        m_writer.flush();
    }
}
