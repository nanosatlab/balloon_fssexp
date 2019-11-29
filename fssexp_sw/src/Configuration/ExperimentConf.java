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

import Common.Constants;
import Common.Log;


/***********************************************************************************************//**
 * Class that parses the configuration of the FSS Experiment. In particular, it reads a CSV-based 
 * file and parses it to a more comfortable class.
 **************************************************************************************************/
public class ExperimentConf {

      /**< Stream to read the configuration file */
    
    /* Configuration parameters */
    public int version;               /**< Version of the configuration file */
    public int fss_buffer_size;          /**< Maximum of packets that can be stored in the internal
                                             *   buffer [packets] */
    public int fss_buffer_thr;           /**< Threshold that indicates when the service is 
                                             *   requested [packets] */
    public int fss_retries;              /**< Number of retries when a packet is not ACKed */
    public float payload_packet_rate;    /**< Packet rate that indicates the generation of internal
                                             *   payload data [packets/second] */
    public int payload_iterations;       /**< Number of executions to generate packets */
    
    public int manager_hk_period;         /* ms */
    public int satellite_id;
    public int fss_interest;
    public int sat_destination;
    
    public int fss_timeout;
    
    public int rf_isl_redundancy;
    public float rf_isl_freq;
    
    public int payload_initial_packets;
    
    public boolean download_experiment_activated;
    public int download_start;
    public int download_end;
    public double cntct_max_duration;
    public double cntct_min_duration;
    public double cntct_max_period; 
    public double cntct_min_period; 
    public int ttc_timeout;
    public int ttc_retries;
    
    public String port_desc;
    
    public int download_rate;
    
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
    public ExperimentConf(Log log) {
        m_logger = log;
        m_conf_file = new File(Constants.conf_file);
        defaultValues();
    }
    
    private void defaultValues() {
        
        /* Common */
        version = 0;
        satellite_id = 1;
        
        /* ExperimentManager */
        manager_hk_period = 1000;
        download_experiment_activated = false;
        download_start = 0;
        download_end = 0;
        
        /* FSSDataBuffer */
        //fss_buffer_size = 30;
        fss_buffer_size = -1;
        fss_buffer_thr = 10;
        
        /* DataGenerator */
        payload_packet_rate = 1;
        payload_iterations = -1;
        payload_initial_packets = 0;
        
        /* FSSProtocol */
        sat_destination = 0;
        fss_retries = 3;
        fss_interest = 1;
        fss_timeout = 5000;
        download_rate = 1;
        
        /* RF ISL */
        rf_isl_redundancy = 0;
        rf_isl_freq = 435.35e6f;
        
        /* UART port */
        port_desc = "/tty/ttyACM0";
        
        /* Dwn contacts */
        cntct_min_period = 5 * 60;
        cntct_max_period = 10 * 60;
        cntct_max_duration = 2 * 60;
        cntct_min_duration = 30;
        ttc_timeout = 5000;	// TODO: confirm this value
        ttc_retries = 3;
    }
    
    public void parseConf() {
            
    	BufferedReader m_conf_stream = null;
    	if(m_conf_file.exists()) {
    		
    		try {
    			m_conf_stream = new BufferedReader(new FileReader(m_conf_file.getName()));
            
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
                    } else if(line.contains("fss_buffer_size") == true) {
                    	fss_buffer_size = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_buffer_size < 0) {
                        	m_logger.warning(TAG + "Negative FSS BUFFER SIZE value.");
                        	correct = false;
                        }
                    } else if(line.contains("fss_buffer_thr") == true) {
                        fss_buffer_thr = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_buffer_thr < 0) {
                        	m_logger.warning(TAG + "Negative FSS BUFFER THRESHOLD value.");
                        	correct = false;
                        }
                    } else if(line.contains("payload_packet_rate") == true) {
                        payload_packet_rate = Float.parseFloat(line.split(":")[1]);
                        counter ++;
                        if(payload_packet_rate < 0) {
                        	m_logger.warning(TAG + "Negative PAYLOAD PACKET RATE value.");
                        	correct = false;
                        }
                    } else if(line.contains("payload_iterations") == true) {
                        payload_iterations = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(payload_iterations < -1) {
                        	m_logger.warning(TAG + "PAYLOAD ITERATIONS value less than -1");
                        	correct = false;
                        }
                    } else if(line.contains("download_rate") == true) {
                    	download_rate = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(download_rate < 0) {
                        	m_logger.warning(TAG + "Negative DOWNLOAD RATE value.");
                        	correct = false;
                        }
                    } else if(line.contains("fss_retries") == true) {
                        fss_retries = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_retries < 0) {
                        	m_logger.warning(TAG + "Negative FSS TRIES value.");
                        	correct = false;
                        }
                    } else if(line.contains("fss_interest") == true) {
                        fss_interest = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_interest != 0 && fss_interest != 1) {
                        	m_logger.warning(TAG + "Not binary FSS INTEREST value (0 = not intersted; 1 = interested).");
                        	correct = false;
                        }
                    } else if(line.contains("sat_destination") == true) {
                        sat_destination = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(sat_destination < 0) {
                        	m_logger.warning(TAG + "Negative SATELLITE DESTINATION value.");
                        	correct = false;
                        }
                    } else if(line.contains("fss_timeout") == true) {
                        fss_timeout = Integer.parseInt(line.split(":")[1]);
                        counter ++;
                        if(fss_timeout < 0) {
                        	m_logger.warning(TAG + "Negative FSS TIMEOUT value.");
                        	correct = false;
                        }
                    } else if(line.contains("rf_isl_redundancy") == true) {
                    	rf_isl_redundancy = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(rf_isl_redundancy < 0) {
                        	m_logger.warning(TAG + "Negative RF ISL REDUNDANCY value.");
                        	correct = false;
                        }
                    } else if(line.contains("central_freq") == true) {
                    	rf_isl_freq = Float.parseFloat(line.split(":")[1]);
                    	counter ++;
                    	if(rf_isl_freq > 450e6f || rf_isl_freq < 400e6f) {
                        	m_logger.warning(TAG + "RF ISL FREQUENCY out of bounds: " + rf_isl_freq);
                        	correct = false;
                        }
                    } else if(line.contains("initial_packets") == true) {
                    	payload_initial_packets = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(payload_initial_packets < 0) {
                        	m_logger.warning(TAG + "Negative INITIAL PAYLOAD PACKET NUMBER value.");
                        	correct = false;
                        }
                    } else if(line.contains("download_experiment") == true) {
                    	int value = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(value == 0) {
                    		download_experiment_activated = false;
                    	} else if(value == 1) {
                    		download_experiment_activated = true;
                    	} else {
                    		m_logger.warning(TAG + "Incorrect DOWNLOAD EXP ACTIVATED value: " + download_experiment_activated);
                        	correct = false;
                    	}
                    } else if(line.contains("download_start") == true) {
                    	download_start = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(download_start < 0) {
                        	m_logger.warning(TAG + "Negative DOWNLOAD START value.");
                        	correct = false;
                        }
                    } else if(line.contains("download_end") == true) {
                    	download_end = Integer.parseInt(line.split(":")[1]);
                    	counter ++;
                    	if(download_end < 0) {
                        	m_logger.warning(TAG + "Negative DOWNLOAD END value.");
                        	correct = false;
                        }
                    }
                    
                    line = m_conf_stream.readLine();
                }
                
                if(counter != Constants.conf_parameters_num) {
                	m_logger.warning(TAG + "Not enough configuration parameters: " 
                						+ counter + "/" + Constants.conf_parameters_num);
                	correct = false;
                } else {
	                if(fss_buffer_thr > fss_buffer_size) {
	                	m_logger.warning(TAG + "FSS BUFFER THRESHOLD value is greater than FSS BUFFER SIZE value.");
	                	correct = false;
	                }
	                if(sat_destination == satellite_id) {
	                	m_logger.warning(TAG + "SATELLITE DESTINATION value is equal to SATELLITE ID value.");
	                	correct = false;
	                }
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
