package Housekeeping;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import Common.FolderUtils;

public class HousekeepingStorage {

    private PrintWriter m_writer;

    public HousekeepingStorage(FolderUtils folder) throws FileNotFoundException {
        m_writer = new PrintWriter(folder.hk_name);
    }
    
    public void writeHK(HousekeepingItem hk) {
            m_writer.write(hk.toString());
            m_writer.flush();
    }
    
    public void writeString(String str) {
        m_writer.write(str);
        m_writer.flush();
    }
}
