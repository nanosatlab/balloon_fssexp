/***************************************************************************************************
*  File:        DataGenerator.java                                                                 *
*  Authors:     Joan Adrià Ruiz de Azúa (JARA), <joan.adria@tsc.upc.edu>                           *
*  Creation:    2018-jun-18                                                                        *
*  Description: Class that emulates a payload that generates packets accordingly a packet rate     *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  2018-jun-18     JARA    Skeleton creation                                                  *
***************************************************************************************************/

/* Own packages */
package FSS_experiment;

/* External imports */
import java.nio.ByteBuffer;

/* Internal imports */
import Storage.FSSDataBuffer;
import Storage.Log;
import Common.Constants;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import IPCStack.SimpleLinkProtocol;


/***********************************************************************************************//**
 * It represents a spacecraft payload that generates data. This generation is performed accordingly
 * to a packet rate. This generation forces the situation in which a spacecraft request a federation
 * in order to download the data. It becomes thus an important element in the experiment.
 **************************************************************************************************/
public class DataGenerator extends Thread{

    private float m_packet_rate;                /**< It is the packet rate with which the data is  
                                                 *   generated [packet/second] */
    private int m_iterations;                   /**< Number of executions of the payload */
    private FSSDataBuffer m_packet_buffer;    /**< Buffer in which the data is stored */
    private boolean m_running;                  /**< Condition that indicates if the thread has to 
                                                 *   be stopped */
    private boolean m_poll_token;
    private int m_number_generated;
    private int m_sat_id;
    private Log m_logger;
    private SimpleLinkProtocol m_ipc_stack;
    byte[] m_reference_data;
    private TimeUtils m_time;
    
    
    private ExperimentConf m_conf;
    private int m_initial_packets;
    
    private final static String TAG = "[DataGenerator] ";
    
    /*******************************************************************************************//**
     * Constructor that initializes the different attributes of the class. This class is 
     * characterized with the packet rate and the number of iterations, without these parameters, 
     * class cannot be instantiated.
     *
     * @param     file name in which the configuration is done.
     **********************************************************************************************/
    public DataGenerator(Log log, ExperimentConf conf, FSSDataBuffer fss_buffer, SimpleLinkProtocol ipc_stack, TimeUtils timer) {
        super();
        
        m_conf = conf;
        m_logger = log;
        m_poll_token = false;
        m_number_generated = 0;
        m_packet_buffer = fss_buffer;
		m_ipc_stack = ipc_stack;
		m_time = timer;
		
		m_reference_data = new byte[Constants.data_reference_size];
		
		/* TODO: to test only */
		for(int i = 0; i < m_reference_data.length; i ++) {
			m_reference_data[i] = (byte)(i & 0xFF);
		}
    }
    
    public void setConfiguration() {
    	m_packet_rate = m_conf.payload_packet_rate;
        m_iterations = m_conf.payload_iterations;
        m_sat_id = m_conf.satellite_id;
        m_initial_packets = m_conf.payload_initial_packets;
    }
    
    /*******************************************************************************************//**
     * Main process that generates the data accordingly to a packet rate. This method represents the
     * core of this class, in which the generation process is executed during a certain iterations
     * and generating the data at each iteration.
     *
     * @see  Thread
     **********************************************************************************************/
    @Override
    public void run() {
        
    	/* Upload the configuration */
    	setConfiguration();
    	
    	/* At the beginning the execution is performed */
        accessToState(true, true); /* m_running = true */
        long time_tick;                             /* in ms */
        long spent_time;
        long data_period = (long)(1000 / m_packet_rate);  /* in ms */  
        int executed = 0;
        long next_iteration = 0;
        long time_to_sleep;
        
        // TODO: Correct reference
        m_reference_data[0] = (byte)(0xFF);
        
        
        /* Conditioned loop */
        m_logger.info(TAG + "Started Thread");
        
        /* Verify if some packets need to be generated at the beginning */
        for(int i = 0; i < m_initial_packets; i++) {
        	/* Poll myself */
            polling(true);
            /* Insert packet */
        	if(getStatus() == true) {
        		m_packet_buffer.insertData(generateData());
        	} else {
        		break;
        	}
        }
        
        if(getStatus() == true) {
        	m_logger.info(TAG + "Inserted all the initial data, entering in the loop");
        }
        	
        while(getStatus() == true && (executed < m_iterations || m_iterations == -1)) {
            
            /* Poll myself */
            polling(true);
            
            /* Get current time to compute the rate */
            //time_tick = space.golbriak.lang.System.currentTimeMillis(); /* Time in milliseconds */
            time_tick = m_time.getTimeMillis();
            
            if(time_tick >= next_iteration) {
                
            	/* Generate and Store in the buffer */
                m_packet_buffer.insertData(generateData());
                
                /* Update for next state */
                executed ++;
                accessToGeneratedNumber(true, 1, 1);    /* m_number_generated ++ */
                next_iteration += data_period;
            }
            
            if(getStatus() == false) {
            	break;
            } else {
            	polling(true);
            }
            
            /* Sleep to work as the requested rate */
            //spent_time = space.golbriak.lang.System.currentTimeMillis() - time_tick;
            spent_time = m_time.getTimeMillis() - time_tick;
            if(spent_time < Constants.generator_sleep) {
                try {
                	time_to_sleep = Constants.generator_sleep - spent_time;
					if(time_to_sleep <= Constants.generator_sleep && time_to_sleep > 0) {
						sleep(time_to_sleep);
					} else {
						m_logger.error(TAG + "The time to sleep is different than the expected: " + time_to_sleep);
					}
                } catch (InterruptedException e) {
                    m_logger.error(e);
                }
            } else {
            	m_logger.info(TAG + "No sleep, the process consumed " + spent_time);
            }
        }
        
        accessToState(true, false);
        m_logger.info(TAG + "Stopped Thread");
    }
    
    private byte[] generateData() {
    	
    	byte[] temp;
    	byte[] rf_isl_hk = new byte[Constants.data_rf_isl_hk_size];
    	byte[] data = new byte[Constants.data_size];
    	ByteBuffer data_header = ByteBuffer.allocate(Constants.data_header_size);
        ByteBuffer timestamp = ByteBuffer.allocate(Constants.data_timestamp_size);
        ByteBuffer short_converter = ByteBuffer.allocate(Short.SIZE / 8);
        
    	
    	/* Generate data */
        data_header.put((byte)(m_sat_id & 0xFF));
        temp = data_header.array();
        System.arraycopy(temp, 0, data, 0, temp.length);
        
        //timestamp.putLong(space.golbriak.lang.System.currentTimeMillis());
        timestamp.putLong(m_time.getTimeMillis());
        temp = timestamp.array();
        System.arraycopy(temp, 0, data, Constants.data_header_size, temp.length);
        
        /* Retrieve RF ISL data */
    	rf_isl_hk = (byte[])m_ipc_stack.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
    	if(rf_isl_hk.length > 0) {
        	System.arraycopy(rf_isl_hk, 0, data, Constants.data_header_size + 
                    			Constants.data_timestamp_size, Constants.data_rf_isl_hk_size);
    	} else {
    		m_logger.warning(TAG + "No HK retrieved from the RF ISL Module - Connection lost?");
    		System.arraycopy(generateDefaultRFISLHK(), 0, data, Constants.data_header_size + 
        						Constants.data_timestamp_size, Constants.data_rf_isl_hk_size);
    	}
        
        // TODO: Include reference data
        System.arraycopy(m_reference_data, 0, data, Constants.data_header_size + 
                                Constants.data_timestamp_size + 
                                Constants.data_rf_isl_hk_size, m_reference_data.length); 
        
        data_header.clear();
        timestamp.clear();
        short_converter.clear();
        
        return data;
    }
    
    private byte[] generateDefaultRFISLHK() {
    	return new byte[Constants.data_rf_isl_hk_size];
    }
    
    
    /*******************************************************************************************//**
     * It provides the capability to stop and clean the thread in a controlled manner. This thread
     * is based on a conditioned loop. When the conditions is wrong, the thread is stopped. This 
     * method is capable of changing the loop state.
     **********************************************************************************************/
    public void controlledStop() { 
    	accessToState(true, false);
    }
    
    public synchronized boolean polling(boolean poll) {
        boolean previous_poll = m_poll_token;
        m_poll_token = poll;
        return previous_poll;
    }
    
    /*******************************************************************************************//**
     * It provides access to the attribute m_number_generated in a synchronized manner. This is 
     * needed, because O-ISL does not have a Semaphore class, and only the tag synchronized is 
     * available.
     **********************************************************************************************/
    public int getGenerated() { return accessToGeneratedNumber(false, 0, 0); }
    
    public synchronized int accessToGeneratedNumber(boolean write, int sign, int value) {
        
        if(write == true) {
            m_number_generated = m_number_generated + sign * value;
        }
        return m_number_generated;
    }
    
    public boolean getStatus() {
    	return accessToState(false, false);
    }
    
    private synchronized boolean accessToState(boolean write, boolean state) { 
        
        if(write == true) {
        	m_running = state;
            m_logger.info(TAG + "Changed to GeneratorData state " + m_running);
        }
        
        return m_running;
    }
    
}
