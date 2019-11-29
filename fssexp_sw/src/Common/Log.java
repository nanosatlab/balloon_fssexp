package Common;

/* External libraries */
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

public class Log {

    private File m_file;
    private TimeUtils m_time;    
    private BufferedWriter m_writer;
    
    public Log(TimeUtils timer, FolderUtils folder) throws IOException 
    {
    	m_file = new File(folder.log_name);
        m_time = timer;
        
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
    
    public synchronized void close() 
    {
    	if(m_writer != null) {
    		/* Close it */
    		try {
				m_writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
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
    		m_writer.flush();
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
