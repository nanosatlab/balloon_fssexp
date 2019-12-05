package Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;
import InterSatelliteCommunications.Packet;
import Payload.PayloadDataBlock;

public class PacketExchangeBuffer 
{
    private File m_tx_file;
    private File m_rx_file;
    private Log m_logger;
    private BufferedWriter m_tx_writer;
    private BufferedWriter m_rx_writer;
    private TimeUtils m_time;
    private PayloadDataBlock m_data_block;
	private ByteBuffer m_publish_data_stream;
    
    private String TAG = "[PacketExchangeBuffer] ";
    
    public PacketExchangeBuffer(Log log, FolderUtils folder, TimeUtils time) 
    {
        m_logger = log;
        m_time = time;
        m_tx_file = new File(folder.tx_name);
        m_rx_file = new File(folder.rx_name);
        resetBuffer();
        m_data_block = new PayloadDataBlock();
    	m_publish_data_stream = ByteBuffer.allocate(Integer.SIZE * 2 / 8);
        /* Get the Writers */
        try {
	        m_tx_writer = new BufferedWriter(new FileWriter(m_tx_file, true));
	        m_rx_writer = new BufferedWriter(new FileWriter(m_rx_file, true));
        } catch (IOException e) {
			m_logger.error(e);
		}
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
    
    public void insertTXPacket(Packet packet) 
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
			m_tx_writer.write(line);
			m_tx_writer.flush();
		} catch (IOException e) {
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
			m_rx_writer.write(line);
			m_rx_writer.flush();
		} catch (IOException e) {
			m_logger.error(e);
		}
    }
}
