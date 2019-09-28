/***************************************************************************************************
*  File:        FSSPacketBuffer.java                                                               *
*  Authors:     Joan Adrià Ruiz de Azúa (JARA), <joan.adria@tsc.upc.edu>                           *
*  Creation:    2018-jun-25                                                                        *
*  Description: Class that represents the buffer in which the FSS data is stored.                  *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  2018-jun-25     JARA    Skeleton creation                                                  *
***************************************************************************************************/

/* Own package */
package Storage;

/* Internal imports */

/* External imports */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import space.golbriak.io.File;
import space.golbriak.io.FileInputStream;
import java.io.FileNotFoundException;
import space.golbriak.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import Configuration.ExperimentConf;

public class FSSDataBuffer {

    private File m_file;
    private int m_max_size;
    private int m_size;
    private String m_file_path;
    private String m_result_file_path;
    private File m_result_file;
    private int m_drop_packets;
    private int m_read_pointer;
    private Log m_logger;
    private ExperimentConf m_conf;
    
    private String TAG = "[FSSDataBuffer] ";
    
    public FSSDataBuffer(Log log, ExperimentConf conf) {
        m_logger = log;
        m_conf = conf;
        m_file_path = Constants.fss_data_file;
        m_result_file_path = Constants.fss_result_data_file;
        m_drop_packets = 0;
        m_read_pointer = 0;
        m_file = new File(m_file_path);
        resetBuffer();
    }
    
    public void setConfiguration() {
    	m_max_size = m_conf.fss_buffer_size;
    }
    
    public void resetBuffer() {
    	/* Initialization - remove data of it */
        if(m_file.exists() == true) {
            m_file.delete();
        }
        try {
            m_file.createNewFile();
        } catch (IOException e) {
            m_logger.error(e);
        }
    }
    
    public boolean insertData(byte[] data) {
        
        if(getSize() < m_max_size) {
            try {
                FileOutputStream file_stream = new FileOutputStream(m_file, true);
                BufferedOutputStream writer = new BufferedOutputStream(file_stream);
                writer.write(data);
                accessToSize(true, 1, 1);   /* m_size ++ */
                writer.close();
            } catch (FileNotFoundException e) {
                m_logger.error(e);
                return false;
            } catch(IOException e) {
                m_logger.error(e);
                return false;
            }
        } else {
            accessToDrop(true, 1, 1); /* m_drop_packets ++ */
            return false;
        }
        
        return true;
    }
    
    public byte[] getBottomData() {
        
        if(getSize() > 0) {

            try {
                byte[] data = new byte[Constants.data_size];
                FileInputStream file_stream = new FileInputStream(m_file);
                BufferedInputStream reader = new BufferedInputStream(file_stream);
                
                try {
                    reader.skip(m_read_pointer * Constants.data_size);
                    reader.read(data);
                } catch (IOException e) {
                    m_logger.error(e);
                    
                } 

                try {
                    reader.close();
                } catch (IOException e) {
                    m_logger.error(e);
                }
                
                return data;
            
            } catch (FileNotFoundException e) {
                m_logger.error(e);
            }
            
            
        }
        
        return null;
    }
    
    public void deleteBottomData() {
    	if(accessToSize(false, 0, 0) > 0) {
    		accessToSize(true, -1, 1);  /* m_size -- */ 
    		m_read_pointer ++;
    	}
    }
    
    public int getSize() { return accessToSize(false, 0, 0); }
    
    private synchronized int accessToSize(boolean write, int sign, int value) { 
        
        if(write == true) {
            m_size = m_size + sign * value;
        }
        
        return m_size;
    }
    
    public int getDrops() { return accessToDrop(false, 0, 0); }
    
    public synchronized int accessToDrop(boolean write, int sign, int value) { 
        if(write == true) {
            m_drop_packets = m_drop_packets + sign * value;
        }
        
        return m_drop_packets; 
    }
    
    public int getCapacity() { return m_max_size; }
    
    public void moveToDownload() {
        
        try {
        	
        	/* Initialization - remove data of it */
            m_result_file = new File(m_result_file_path);
            if(m_result_file.exists() == true) {
                m_result_file.delete();
            }
            try {
                m_result_file.createNewFile();
            } catch (IOException e) {
                m_logger.error(e);
            }
        	
            FileOutputStream file_stream = new FileOutputStream(m_result_file);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            
            /*ByteBuffer size = ByteBuffer.allocate(Integer.SIZE / 8).putInt(m_size);
            size.flip();
            writer.write(size.array());*/
            
            /*byte[] data;
            while((data = extractData()) != null) {
                writer.write(data);
            }*/
            
            /*FileInputStream origin_file_stream = new FileInputStream(m_file);
            ByteBuffer size = ByteBuffer.allocate(Integer.SIZE / 8).putInt(origin_file_stream.available() / Constants.data_size);
            size.flip();
            writer.write(size.array());
            byte[] data = new byte[origin_file_stream.available()];
            origin_file_stream.read(data);
            origin_file_stream.close();
            writer.write(data);
            */
            
            /* TODO: Proposed by SIMONE - VERIFY!! */
            FileInputStream origin_file_stream = new FileInputStream(m_file);
            BufferedInputStream reader = new BufferedInputStream(origin_file_stream);
            int data_block_number = (int)(m_file.length() & 0xFFFFFFFF) / Constants.data_size;
            ByteBuffer size = ByteBuffer.allocate(Integer.SIZE / 8).putInt(data_block_number);
            size.flip();
            writer.write(size.array());
            
            byte [] data = new byte[Constants.fss_buffer_move_block];
            int count;
        	while((count = reader.read(data)) != -1) {
        		writer.write(data, 0, count);
        	}
    		reader.close();
            writer.close();
            
        } catch (FileNotFoundException e) {
            m_logger.error(e);
        } catch(IOException e) {
            m_logger.error(e);
        }
    }
}
