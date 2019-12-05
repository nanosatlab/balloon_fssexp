package FSS_experiment;

/* External libraries */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.TimeUtils;

public class TCPInterface extends Thread
{
	private Log m_logger = null;
    private ExperimentManager m_experiment = null;
    private TimeUtils m_time_utils;
    private FolderUtils m_folder;
    
    /* Interface with Platform */
    byte[] m_cbor_content;
    byte[] m_length_field_bytes;
    ByteArrayInputStream m_intermediate_input_stream;
    ByteArrayOutputStream m_intermediate_output_stream;
    ByteBuffer m_length_field_converter;
	
    private static String TAG = "[TCPInterface] ";
    
	public TCPInterface() throws IOException {
		m_time_utils = new TimeUtils();
		m_folder = new FolderUtils(m_time_utils);
		m_logger = new Log(m_time_utils, m_folder);
		m_length_field_bytes = new byte[Constants.LENGHT_FIELD_SIZE];
		m_intermediate_output_stream = new ByteArrayOutputStream();
    	m_length_field_converter = ByteBuffer.allocate(Constants.LENGHT_FIELD_SIZE);
    	
		try {
			m_experiment = new ExperimentManager(m_logger, m_time_utils, m_folder);
		} catch (FileNotFoundException e) {
			m_logger.error(TAG + "Impossible to create ExperimentManager: " + e.getMessage());
		}
	}
	
	public void run() {
		
		m_logger.info(TAG + "********* FSSExp SOFTWARE STARTED *********");
		
		/* Launch ExperimentManager Thread */
		m_experiment.start();
    	try {
			m_experiment.join();
		} catch (InterruptedException e) {
			m_logger.error(e);
		}
		
		System.out.println(TAG + "Bye Bye!");
        m_logger.info(TAG + "Bye Bye!");
        
        /* Close LOG */
        m_logger.info(TAG + "********* FSSExp SOFTWARE EXIT *********");
        m_logger.close();
	}

}
