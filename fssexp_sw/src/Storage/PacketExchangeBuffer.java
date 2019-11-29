package Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import InterSatelliteCommunications.Packet;

public class PacketExchangeBuffer {

    private File m_tx_file;
    private File m_rx_file;
    private String m_tx_file_path;
    private String m_rx_file_path;
    private String m_result_file_path;
    private File m_result_file;
    private int m_tx_num;
    private int m_rx_num;
    private Log m_logger;
    
    private String TAG = "[PacketExchangeBuffer] ";
    
    public PacketExchangeBuffer(Log log, FolderUtils folder) {
        
        m_logger = log;
        
        m_tx_num = 0;
        m_rx_num = 0;
        
        m_tx_file_path = folder.tx_name;
        m_rx_file_path = folder.rx_name;
        m_result_file_path = Constants.fss_result_data_file;
        
        m_tx_file = new File(m_tx_file_path);
        m_rx_file = new File(m_rx_file_path);
        resetBuffer();
        
        /* Result file - no remove */
        m_result_file = new File(m_result_file_path);
    }
    
    public void resetBuffer() {
    	/* Initialization - TX packets */
        if(m_tx_file.exists() == true) {
            m_tx_file.delete();
        }
        try {
        	m_tx_file.createNewFile();
        } catch (IOException e) {
            m_logger.error(e);
        }
        
        /* Initialization - RX packets */
        if(m_rx_file.exists() == true) {
            m_rx_file.delete();
        }
        try {
            m_rx_file.createNewFile();
        } catch (IOException e) {
            m_logger.error(e);
        }
    }
    
    public void insertTXPacket(Packet packet) {
        try {
            FileOutputStream file_stream = new FileOutputStream(m_tx_file, true);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            if(packet.type != Constants.PACKET_FSS_DATA) {
                writer.write(packet.toBytes());
            } else {
                writer.write(packet.toBytesNoData());
            }
            m_tx_num ++;
            writer.close();
        } catch (FileNotFoundException e) {
            m_logger.error(e);
        } catch(IOException e) {
            m_logger.error(e);
        }
    }
    
    public void insertRXPacket(Packet packet) {
        try {
            FileOutputStream file_stream = new FileOutputStream(m_rx_file, true);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            if(packet.type != Constants.PACKET_FSS_DATA) {
                writer.write(packet.toBytes());
            } else {
                writer.write(packet.toBytesNoData());
            }
            m_rx_num ++;
            writer.close();
        } catch (FileNotFoundException e) {
            m_logger.error(e);
        } catch(IOException e) {
            m_logger.error(e);
        }
    }
}
