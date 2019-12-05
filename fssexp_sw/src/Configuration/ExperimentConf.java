/***************************************************************************************************
*  File:        ExperimentConf.java                                                                *
*  Authors:     Joan Adrià Ruiz de Azúa (JARA), <joan.adria@tsc.upc.edu>                           *
*  Creation:    2018-jun-18                                                                        *
*  Description: Class that represents the FSS experiment configuration				               *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  2018-jun-18     JARA    Skeleton creation				                                   *
***************************************************************************************************/

/* Own packages */
package Configuration;

/* External imports */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/* Internal imports */
import Common.Constants;
import Common.FolderUtils;
import Common.Log;


/***********************************************************************************************//**
 * Class that parses the configuration of the FSS Experiment. In particular, it reads a CSV-based 
 * file and parses it to a more comfortable class.
 **************************************************************************************************/
public class ExperimentConf {

      /**< Stream to read the configuration file */
    
    /* Configuration parameters */
    public int version;               /**< Version of the configuration file */
    public int satellite_id;
    public int fss_timeout;
    public int fss_retries;
    public int fss_backoff;
    public int manager_hk_period;         /* ms */
    public float rf_isl_freq;
    public double cntct_max_duration;
    public double cntct_min_duration;
    public double cntct_max_period; 
    public double cntct_min_period; 
    public int ttc_timeout;
    public int ttc_retries;
    public int ttc_backoff;
    public String port_desc;
    private File m_conf_file;
    private Log m_logger;
    
    private final static String TAG = "[ExperimentConf] ";
    
    /*******************************************************************************************//**
     * Constructor that initializes the reader with the input file name. It generates thus an 
     * interface to read and then parse the configuration file. The parsing is performed in a 
     * different method of this class.
     *
     * @param     file name in which the configuration is done.
     * @throws FileNotFoundException - It is impossible to be thrown, this verification is done  
     **********************************************************************************************/
    public ExperimentConf(Log log, FolderUtils folder) 
    {
        m_logger = log;
        m_conf_file = new File(folder.conf_name);
        defaultValues();
        parseConf();
    }
    
    private void defaultValues() {
        
        /* Common */
        version = 0;
        satellite_id = 2;
        
        /* ExperimentManager */
        manager_hk_period = 10 * 1000;			/* ms */
        
        /* FSSProtocol */
        fss_timeout = 5000;
        fss_backoff = 500;
        fss_retries = 2;
        
        /* RF ISL */
        rf_isl_freq = 435.35e6f;
        
        /* UART port */
        port_desc = "/tty/ttyACM1";
        
        /* Dwn contacts */
        cntct_min_period = 5 * 60;
        cntct_max_period = 10 * 60;
        cntct_max_duration = 2 * 60;
        cntct_min_duration = 30;
        ttc_timeout = 5000;	// TODO: confirm this value
        ttc_backoff = 500;
        ttc_retries = 2;
    }
    
    public void parseConf() 
    {
    	BufferedReader m_conf_stream = null;
    	if(m_conf_file.exists()) {
    		try {
    			m_conf_stream = new BufferedReader(new FileReader(m_conf_file));
            
            	boolean correct = true;
            	int counter = 0;
                String line = m_conf_stream.readLine();
                while(line != "[END]" && line != null) {
                    
                    if(line.contains("version") == true) {
                        version = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(version < 0) {
                        	m_logger.warning(TAG + "Negative VERSION value.");
                        	correct = false;
                        } else if(version == 0) {
                        	m_logger.warning(TAG + "Zero VERSION value. Only this value is accepted for Default configuration.");
                        	correct = false;
                        }
                    } else if(line.contains("port_desc") == true) {
                        port_desc = line.split(":")[1];
                        counter ++;
                    } else if(line.contains("satellite_id") == true) {
                        satellite_id = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(satellite_id < 0) {
                        	m_logger.warning(TAG + "Negative SATELLITE ID value.");
                        	correct = false;
                        }
                    } else if(line.contains("manager_hk_period") == true) {
                        manager_hk_period = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(manager_hk_period < 0) {
                        	m_logger.warning(TAG + "Negative MANAGER HK PERIOD value.");
                        	correct = false;
                        }
                    } else if(line.contains("fss_timeout") == true) {
                        fss_timeout = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_timeout < 0) {
                        	m_logger.warning(TAG + "Negative FSS TIMEOUT value.");
                        	correct = false;
                        }
                    } else if(line.contains("central_freq") == true) {
                    	rf_isl_freq = Float.parseFloat(line.split(":")[1]);
                    	counter ++;
                    	if(rf_isl_freq > 450e6f || rf_isl_freq < 400e6f) {
                        	m_logger.warning(TAG + "RF ISL FREQUENCY out of bounds: " + rf_isl_freq);
                        	correct = false;
                        }
                    } else if(line.contains("ttc_retries") == true) {
                    	ttc_retries = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(ttc_retries < 0) {
                        	m_logger.warning(TAG + "Retransmission value out of bounds: " + ttc_retries);
                        	correct = false;
                        }
                    } else if(line.contains("fss_retries") == true) {
                    	fss_retries = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(fss_retries < 0) {
                        	m_logger.warning(TAG + "FSS Retransmission value out of bounds: " + fss_retries);
                        	correct = false;
                        }
                    } else if(line.contains("ttc_timeout") == true) {
                    	ttc_timeout = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(ttc_timeout < 0) {
                        	m_logger.warning(TAG + "Retransmission timeout out of bounds: " + ttc_timeout);
                        	correct = false;
                        }
                    } else if(line.contains("ttc_backoff") == true) {
                    	ttc_backoff = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(ttc_backoff < 0) {
                        	m_logger.warning(TAG + "Retransmission backoff out of bounds: " + ttc_backoff);
                        	correct = false;
                        }
                    } else if(line.contains("fss_backoff") == true) {
                    	fss_backoff = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(fss_backoff < 0) {
                        	m_logger.warning(TAG + "FSS Retransmission backoff out of bounds: " + fss_backoff);
                        	correct = false;
                        }
                    }
                    
                    
                    line = m_conf_stream.readLine();
                }
                
                if(counter != Constants.conf_parameters_num) {
                	m_logger.warning(TAG + "Not enough configuration parameters: " 
                						+ counter + "/" + Constants.conf_parameters_num);
                	correct = false;
                }
                
                /* If any error, just use the default values */
                if(correct == false) {
                	m_logger.warning(TAG + "Wrong input configuration. Default configuration used.");
                	defaultValues();
                } else {
                	m_logger.info(TAG + "Configuration " + version + " uploaded correctly");
                }
            
            } catch (IOException e) {
                m_logger.error(e);
                defaultValues();
            } finally {
                try {
                    if(m_conf_stream != null) {
                    	m_conf_stream.close();
                    }
                } catch (IOException e) {
                    m_logger.error(e);
                }
            }

        } else {
        	m_logger.warning(TAG + "No configuration file found. Default configuration used.");
            defaultValues();
        }
    }
}
