package Storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import Common.Constants;

public class HousekeepingBuffer {

    private File m_file;
    private String m_file_path;
    private Log m_logger;
    
    public HousekeepingBuffer(Log log) {
        m_logger = log;
        m_file_path = Constants.hk_file;
        m_file = new File(m_file_path);
        resetBuffer();
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
    
    public void writeHK(byte[] hk) {
        try {
            FileOutputStream file_stream = new FileOutputStream(m_file, true);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            writer.write(hk);
            writer.close();
        } catch (FileNotFoundException e) {
            m_logger.error(e);
        } catch(IOException e) {
            m_logger.error(e);
        }
    }
    
    public void moveToDownload() {
        try {
            String destination = Constants.download_path + Constants.hk_name;
            FileOutputStream file_stream = new FileOutputStream(destination);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            FileInputStream file_input_stream = new FileInputStream(m_file);
            BufferedInputStream reader = new BufferedInputStream(file_input_stream);
            byte data[] = new byte[Constants.hk_buffer_move_block];
            int count;
            while((count = reader.read(data)) != -1) {
                writer.write(data, 0, count);
            }
            writer.close();
            reader.close();
        } catch (FileNotFoundException e) {
            
        } catch(IOException e) {
            
        }
    }
}
