package InterSatelliteCommunications;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;

public class TxISLStorage {
	private PrintWriter m_writer;

    public TxISLStorage(FolderUtils folder) throws FileNotFoundException {
        m_writer = new PrintWriter(folder.tx_name);
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
