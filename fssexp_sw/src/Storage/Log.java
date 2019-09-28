package Storage;

/* External libraries */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

/* Golbriak libraries */
import space.golbriak.io.File;
import space.golbriak.io.FileInputStream;
import space.golbriak.io.FileOutputStream;
import space.golbriak.io.FileWriter;

/* Internal libraries */
import Common.Constants;
import Common.TimeUtils;

public class Log {

    private File m_file;
    private String m_file_path;
    private TimeUtils m_time;    
    private BufferedWriter m_writer;
    private double m_data_to_flush;
    private long m_last_flush;
    private int m_flush_period; 
    
    public Log(TimeUtils timer) {
        m_file_path = Constants.log_file;
        m_file = new File(m_file_path);
        m_time = timer;
        m_data_to_flush = 0;
        m_flush_period = 30000;
        m_last_flush = m_time.getTimeMillis();	/* Initial O-ISL is wrong, but it's OK */
        
        if(m_file.exists() == false) {
        	try {
				m_file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        try {
        	m_writer = new BufferedWriter(new FileWriter(m_file, true));
        } catch(FileNotFoundException e) {
        	e.printStackTrace();
        	m_writer = null;
        }
    }
    
    public synchronized void flush() {
    	if(m_writer != null && m_data_to_flush > 0) {
    		
    		if(m_data_to_flush >= Constants.log_data_to_flush
    			|| m_last_flush + m_flush_period <= m_time.getTimeMillis()) {
	    		try {
					m_writer.flush();
					m_data_to_flush = 0;
					m_last_flush = m_time.getTimeMillis();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}
    }
    
    public synchronized void close() {
    	if(m_writer != null) {
    		
    		/* Store any possible data */
    		if(m_data_to_flush > 0) {
    			try {
    				m_writer.flush();
    				m_data_to_flush = 0;
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
    		
    		/* Close it */
    		try {
				m_writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    }
    
    public synchronized void resetLog() {
    	
    	/* Flush if there is log */
    	if(m_data_to_flush > 0) {
			try {
				m_writer.flush();
				m_data_to_flush = 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
    	/* Initialization - remove data of it */
        if(m_file.exists() == true) {
            m_file.delete();
        }
        
        try {
            m_file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        /* Reset the writer */
        try {
        	try {
        		m_writer.close();
        	} catch (IOException e) {
    			e.printStackTrace();
    		}
        	m_writer = new BufferedWriter(new FileWriter(m_file, true));
        } catch(FileNotFoundException e) {
        	e.printStackTrace();
        	m_writer = null;
        } 
        
        m_data_to_flush = 0;
    }
    
    public synchronized void info(String string) {
        write(m_time.getTimeMillis() + " - [INFO] " + string + "\n");
    }
    
    public synchronized void warning(String string) {
        write(m_time.getTimeMillis() + " - [WARN] " + string + "\n"); 
    }
    
    private void write(String string) {
    	try {
    		m_writer.write(string);
    		m_data_to_flush += string.length();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public synchronized void error(String string) {
            write(m_time.getTimeMillis() + " - [ERROR]" + string + "\n");
    }
    
    public synchronized void error(Throwable exception) {
            String s = m_time.getTimeMillis() + " - [ERROR] " + exception.toString() + "\n";
            for(StackTraceElement element : exception.getStackTrace()) {
            	s += element.toString() + "\n";
            }
            write(s); 
    }
    
    public synchronized void debug(String string) {
            write(m_time.getTimeMillis() + " - [DEBUG]" + string + "\n");
    }
    
    public synchronized void moveToDownload() {
        
    	/* Store anything that is in RAM */
    	if(m_data_to_flush > 0) {
			try {
				m_writer.flush();
				m_data_to_flush = 0;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
    	try {
            String destination = Constants.download_path + Constants.log_name;
            FileOutputStream file_stream = new FileOutputStream(destination);
            BufferedOutputStream writer = new BufferedOutputStream(file_stream);
            FileInputStream file_input_stream = new FileInputStream(m_file);
            BufferedInputStream reader = new BufferedInputStream(file_input_stream);
            byte data[] = new byte[1024];
            int count;
            while((count = reader.read(data)) != -1) {
                writer.write(data, 0, count);
            }
            writer.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
