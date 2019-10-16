package Storage;

/* External libraries */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

/* Internal libraries */
import Common.Constants;
import Common.FolderUtils;
import Common.TimeUtils;

public class Log {

    private File m_file;
    private TimeUtils m_time;    
    private BufferedWriter m_writer;
    private double m_data_to_flush;
    private long m_last_flush;
    private int m_flush_period;
    
    public Log(TimeUtils timer, FolderUtils folder) throws IOException {
    	m_file = new File(folder.log_name);
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
        	final String dir = System.getProperty("user.dir");
            System.out.println("current dir = " + dir);
            System.out.println(folder.log_name);
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
    
    public synchronized void resetLog() throws IOException {
    	
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
}
