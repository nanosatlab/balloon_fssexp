package Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import space.golbriak.io.File;
import space.golbriak.io.FileInputStream;
import java.io.FileNotFoundException;
import space.golbriak.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import FSS_protocol.FSSPacket;

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
    
    public PacketExchangeBuffer(Log log) {
        
        m_logger = log;
        
        m_tx_num = 0;
        m_rx_num = 0;
        
        m_tx_file_path = Constants.tx_file;
        m_rx_file_path = Constants.rx_file;
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
    
    public void insertTXPacket(FSSPacket packet) {
        try {
            FileOutputStream file_stream = new FileOutputStream(m_tx_file, true);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            if(packet.getType() != Constants.FSS_PACKET_DATA) {
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
    
    public void insertRXPacket(FSSPacket packet) {
        try {
            FileOutputStream file_stream = new FileOutputStream(m_rx_file, true);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            if(packet.getType() != Constants.FSS_PACKET_DATA) {
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

    public void moveToDownload() {
        try {       	
            FileOutputStream file_stream = new FileOutputStream(m_result_file, true);
            FileInputStream file_tx_stream = new FileInputStream(m_tx_file);
            BufferedInputStream reader_tx = new BufferedInputStream(file_tx_stream);
            FileInputStream file_rx_stream = new FileInputStream(m_rx_file);
            BufferedInputStream reader_rx = new BufferedInputStream(file_rx_stream);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            ByteBuffer size = ByteBuffer.allocate(Integer.SIZE / 8);
            
            size.putInt(m_tx_num);
            size.flip();
            writer.write(size.array());
            size.putInt(m_rx_num);
            size.flip();
            writer.write(size.array());
            
            byte data[] = new byte[Constants.packet_buffer_move_block];
            int count;
            while((count = reader_tx.read(data)) != -1) {
                writer.write(data, 0, count);
            }
            reader_tx.close();
            
            /*byte[] tx_packets = new byte[file_tx_stream.available()];
            file_tx_stream.read(tx_packets);
            writer.write(tx_packets);*/
         
            while((count = reader_rx.read(data)) != -1) {
                writer.write(data, 0, count);
            }
            reader_rx.close();
            
            /*byte[] rx_packets = new byte[file_rx_stream.available()];
            file_rx_stream.read(rx_packets);
            writer.write(rx_packets);*/
            
            writer.close();
            /*file_tx_stream.close();
            file_rx_stream.close();*/

        } catch (FileNotFoundException e) {
            m_logger.error(e);
        } catch(IOException e) {
            m_logger.error(e);
        }
    }
    
}
