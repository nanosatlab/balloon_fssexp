package Payload;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;
import InterSatelliteCommunications.Packet;

public class PayloadStorage {
	private PrintWriter m_writer;

    public PayloadStorage(FolderUtils folder) throws FileNotFoundException {
        m_writer = new PrintWriter(folder.dwn_name);
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
