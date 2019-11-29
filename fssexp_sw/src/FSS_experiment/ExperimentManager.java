/***************************************************************************************************
*  File:        DataGenerator.java                                                                 *
*  Authors:     Joan Adrià Ruiz de Azúa (JARA), <joan.adria@tsc.upc.edu>                           *
*  Creation:    2018-jun-18                                                                        *
*  Description: Class that is the manager of the FSS experiment. It controles the state of the     *
*               experiment and interacts with the platform.                                        *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  2018-jun-18     JARA    Skeleton creation                                                  *
***************************************************************************************************/

/* Own package */
package FSS_experiment;

/* Internal imports */
import Storage.PayloadBuffer;
import Storage.PacketExchangeBuffer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import Common.Constants;
import Common.FolderUtils;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Housekeeping.HousekeepingItem;
import Housekeeping.HousekeepingStorage;

/* When working in Linux Computer */
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;

import IPCStack.PacketDispatcher;
import IPCStack.SimpleLinkProtocol;
import InterSatelliteCommunications.FSSProtocol;
import InterSatelliteCommunications.Packet;
import Payload.Payload;
import Payload.PayloadDataBlock;

/* External imports */


/***********************************************************************************************//**
 * It represents a spacecraft payload that generates data. This generation is performed accordingly
 * to a packet rate. This generation forces the situation in which a spacecraft request a federation
 * in order to download the data. It becomes thus an important element in the experiment.
 **************************************************************************************************/
public class ExperimentManager extends Thread{

    private static String TAG = "[ExperimentManager] ";
    
    /* Internal objects */
    private Log m_logger;
    private ExperimentConf m_conf;
    private PacketExchangeBuffer m_hk_packets;
    private PayloadBuffer m_fss_buffer;
    //private Payload m_generator;
    private FSSProtocol m_fss_protocol;
    private HousekeepingStorage m_hk_buffer;
    //private SimpleLinkProtocol m_ipc_stack;
    private TimeUtils m_time;
    //private SynchronizedBuffer m_uart_rx_buffer;
    //private SynchronizedBuffer m_uart_tx_buffer;
    private SynchronizedBuffer m_rfisltelemetry_buffer;
    
    /* Manager attributes */
    private int m_prot_num; 
    private int m_sat_id;
    private int m_status;                       /**< State of the experiment */
    private int m_generator_counter;
    private int m_fss_protocol_counter;
    private boolean m_exit;
    private long m_initial_time;
    private int m_exp_number;
    private long m_start_time;
    private boolean m_err_finished;
    private String m_err_message;
    private int m_rf_isl_counter;
    private boolean m_rf_isl_alive;
    private String m_persistent_file;
    private FileInputStream m_persistent_reader;
    private FileOutputStream m_persistent_writer;
    private int m_number_generated;
    
    /* Housekeeping */
    ByteBuffer m_hk_header;
    private long m_time_tick;
    private long m_next_hk;
    private int m_data_generator_polling;
    private int m_fss_protocol_polling;
    private int m_command_time = 0;
    private int m_number_sc_commands = 0;
    private byte[] m_rf_isl_hk;
    
    /* DataBlock */
    private PayloadDataBlock m_payload_data;
    
    /* IPC classes */
    private PacketDispatcher m_dispatcher;
    private byte[] m_ipc_header_stream;
    private byte[] m_ipc_checksum_stream;
    
    private boolean m_data_generator_error_notification;
    private boolean m_fss_protocol_error_notification;
    
    private String m_command;
    private int m_ack_reply;
    
    public ExperimentManager(Log logger, TimeUtils timer, FolderUtils folder) throws FileNotFoundException 
    {
    	/* Internal objects */
    	m_logger = logger;
    	m_time = timer;
    	m_conf = new ExperimentConf(m_logger);
    	m_dispatcher = new PacketDispatcher(m_logger, m_conf, m_time, folder);
    	m_hk_packets = new PacketExchangeBuffer(m_logger, folder);
    	m_fss_buffer = new PayloadBuffer(m_logger, m_conf, folder);
    	//m_generator = new Payload(m_logger, m_conf, m_fss_buffer, m_ipc_stack, m_time);
    	m_fss_protocol = new FSSProtocol(m_logger, m_fss_buffer, m_hk_packets, m_conf, m_time, m_dispatcher);
    	m_hk_buffer = new HousekeepingStorage(folder);
    	m_hk_header = ByteBuffer.allocate(Constants.hk_header_size);
    	m_payload_data = new PayloadDataBlock();
    	
    	/* Dispatcher initialization */
    	m_rfisltelemetry_buffer = new SynchronizedBuffer(m_logger, "manager-telemetry");
    	m_prot_num = Constants.manager_prot_num;
    	m_dispatcher.addProtocolBuffer(m_prot_num, m_rfisltelemetry_buffer);
    	
    	/* Manager attributes */
    	accessToStatus(true, Constants.REPLY_STATUS_ERROR);
    	m_sat_id = m_conf.satellite_id;
    	m_generator_counter = 0;
    	m_fss_protocol_counter = 0;
    	m_exit = false;
    	m_initial_time = 0;
    	m_exp_number = 0;
    	m_start_time = 0;
    	m_err_finished = false;
    	m_err_message = "";
    	m_rf_isl_counter = 0;
    	m_rf_isl_alive = true;
    	m_persistent_file = folder.prst_name;
        m_persistent_reader = null;
        m_persistent_writer = null;
    	
    	/* Housekeeping */
    	m_time_tick = 0;
    	m_next_hk = 0;
    	m_data_generator_polling = 0;
    	m_fss_protocol_polling = 0;
    	m_command_time = 0;
    	m_number_sc_commands = 0;
    	m_rf_isl_hk = new byte[Constants.data_rf_isl_hk_size];
    	
    	m_data_generator_error_notification = false;
    	m_fss_protocol_error_notification = false;
    	
    	/* Interface with TCPInterface */
    	m_command = "";
    	m_ack_reply = -1;
    	
    	/* Dispatcher parameters */
    	m_ipc_header_stream = new byte[Packet.getHeaderSize()];
        m_ipc_checksum_stream = new byte[Short.SIZE / 8];
    }
    
    public synchronized String accessToCommand(boolean write, String command) {
    	
    	String temp_command = ""; 
    	if(write == true) {
    		m_command = command;
    	} else {
    		temp_command = m_command;
    		m_command = "";
    	}
    	
    	return temp_command;
    }
    
    public synchronized int accessToACKReply(boolean write, int value) {
    	
    	int temp_reply = -1; 
    	if(write == true) {
    		m_ack_reply = value;
    	} else {
    		temp_reply = m_ack_reply;
    		m_ack_reply = -1;
    	}
    	
    	return temp_reply;
    }
    
    private void writeHKFileHeader() 
    {
    	String str = "";
    	str += Constants.sw_version + ",";
        str += m_exp_number + ",";
        str += m_conf.satellite_id + ",";
        str += m_initial_time;
        str += "\n";
        m_hk_buffer.writeString(str);
    }
    
    private boolean transitFromReadyToRunning() 
    {
    	m_logger.info(TAG + "Transition from READY to RUNNING");    	
    	
    	/* Lock the starting Time; it is interesting to include this here, because 
    	 * the time has already set up by the platform with the first command, thus
    	 * at least this command is executed. This time is simple used for HK
    	 * which starts to be stored when a START command is received. */
        m_initial_time = m_time.getTimeMillis();
    	
    	
    	/* Parse configuration */
    	m_conf.parseConf();
    	m_logger.info(TAG + "Configuration version: " + m_conf.version);
    	//m_ipc_stack.setConfiguration();
    	m_fss_buffer.setConfiguration();
    	//m_generator.setConfiguration();
    	m_fss_protocol.setConfiguration();
    	
    	/* Generate the HK header */
    	writeHKFileHeader();
    	
    	
    	/* Configure the RF ISL Module */
    	//ByteBuffer temp = ByteBuffer.allocate(Float.SIZE / 8).putFloat(m_conf.rf_isl_freq);
    	//int counter = 0;
    	//m_logger.info(TAG + "Trying to send the Configuration of the RF ISL Module");
    	//while(m_ipc_stack.updateConfiguration(temp.array()) == false
    	//		&& counter <= Constants.rf_isl_max_configuration) {
    	//	counter ++;
    	//	m_logger.warning(TAG + "Failed to send the RF ISL Configuration; Try number " + counter);
    	//}
    	
    	//if(counter >= Constants.rf_isl_max_configuration) {
    	//	m_logger.error(TAG + "Impossible to send the RF ISL Configuration");
    		
    		/* Notify the error to the platform */
    	//	accessToErrorMessage(true, TAG + "Impossible to send the RF ISL Configuration - No communication");
    	//	accessToError(true, true);
    	//	return false;
    	//}
    	
    	/* Create Files */
    	/** FSS_BUFFER.data **/
    	m_fss_buffer.resetBuffer();
    	/** RX_PACKETS.data & TX_PACKETS.data **/
    	m_hk_packets.resetBuffer();
    	
    	/* Indicate the starting time */
        m_start_time = m_time.getTimeMillis();
        
        /* Clear Uart Buffer */
        //m_uart_rx_buffer.clear();
        //m_uart_tx_buffer.clear();
        
    	/* Start Threads */
        //m_generator = new Payload(m_logger, m_conf, m_fss_buffer, m_ipc_stack, m_time);
        m_fss_protocol = new FSSProtocol(m_logger, m_fss_buffer, m_hk_packets, m_conf, m_time, m_dispatcher);
        
        //m_generator.start();
        m_fss_protocol.start();
        
        /* Clean possible error status */
        accessToErrorMessage(true, "");
		accessToError(true, false);
		
        return true;
    }
    
    private void transitFromRunningToFinished() throws IOException 
    {
    	m_logger.info(TAG + "Transition from RUNNING to FINISHED");
    	/* Close the Threads */
    	stopChilds();
    	
        /* Retrieve the final HK */
        storeHK();
        m_logger.info(TAG + "Final HK buffered");
        
        /* Close the folder system */
        // TODO:
    }
    
    private void transitToErroneousFinished() {
    	m_logger.info(TAG + "An Error detected, transition to erroneous FINISHED");
    	
    	try {
			transitFromRunningToFinished();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	m_err_finished = true;
    }
    
    private void stopChilds() 
    {
    	/* Close the Threads */
    	m_fss_protocol.controlledStop();
       // m_generator.controlledStop();
    	
    	/* Verify that the Threads are dead */
    	int m_exit_counter = 0;
        //while(m_generator.getState() != Thread.State.TERMINATED 
        //   && m_generator.getState() != Thread.State.NEW
        //   && m_exit_counter < Constants.manager_exit_max) {
        //    m_logger.info(TAG + "DataGenerator not terminated (status " + m_generator.getState() + "), waiting a little more");
        //    m_exit_counter ++;
        //    try {
        //        Thread.sleep(Constants.manager_sleep);
        //    } catch (InterruptedException e) {
        //        m_logger.error(e);
        //    }
        // }
        //if(m_exit_counter == Constants.manager_exit_max) {
        //	m_logger.error(TAG + "Impossible to courteously terminate DataGenerator (kill it!)");
        //}
        
        m_exit_counter = 0;
        while(m_fss_protocol.getState() != Thread.State.TERMINATED 
                && m_fss_protocol.getState() != Thread.State.NEW
                && m_exit_counter < Constants.manager_exit_max) {
                m_logger.info(TAG + "FSSProtocol not terminated (status " + m_fss_protocol.getState() + "), waiting a little more");
                 m_exit_counter ++;
                 try {
                     Thread.sleep(Constants.manager_sleep);
                 } catch (InterruptedException e) {
                     m_logger.error(e);
                 }
             }
        if(m_exit_counter == Constants.manager_exit_max) {
        	m_logger.error(TAG + "Impossible to courteously terminate FSSProtocol (kill it!)");
        }
    }
    
    private void updateHK()
    {
    	m_payload_data.exp_hk.timestamp = m_time.getTimeMillis();
    	m_payload_data.exp_hk.exec_status = m_status;
    	m_payload_data.exp_hk.payload_poll = m_data_generator_polling;
    	m_payload_data.exp_hk.fss_poll = m_fss_protocol_polling;
    	m_payload_data.exp_hk.payload_generated_items = m_number_generated;
    	m_payload_data.exp_hk.fss_status = m_fss_protocol.getFederationStatus();
    	m_payload_data.exp_hk.fss_role = m_fss_protocol.getFederationRole();
    	m_payload_data.exp_hk.fss_tx = m_fss_protocol.getTXs();
    	m_payload_data.exp_hk.fss_rx = m_fss_protocol.getRXs();
    	m_payload_data.exp_hk.fss_err_rx = m_fss_protocol.getErrRXs();
    	m_payload_data.exp_hk.isl_buffer_size = m_fss_buffer.getSize();
    	m_payload_data.exp_hk.isl_buffer_drops = m_fss_buffer.getDrops();
    	if(m_rf_isl_hk != null) {
    		m_payload_data.exp_hk.rf_isl_hk.parseFromBytes(m_rf_isl_hk);
    	}
    }
    
    private void storeHK()
    {
		m_hk_buffer.writeHK(m_payload_data.exp_hk);
    }
    
    private void updateBootCount() throws IOException
    {
    	m_persistent_reader = new FileInputStream(new File(m_persistent_file));
    	if(m_persistent_reader != null) {
            try {
            	if(m_persistent_reader.available() > 0) {
            		m_exp_number = m_persistent_reader.read();
                    m_exp_number ++;
            	}
            	m_persistent_writer = new FileOutputStream(m_persistent_file);
            	m_persistent_writer.write(m_exp_number);
            } catch (IOException e) {
                m_logger.error(e);
            } finally {
            	m_persistent_reader.close();
            	m_persistent_writer.close();
            }
        }
    }
    
    private void checkOperationsCommand()
    {
		String command;
    	command = accessToCommand(false, "");
    	if(command.equals("") == false) {
    		System.out.println(TAG + "Received Command " + command);
    		m_logger.info(TAG + "Received Command " + command);
    		m_command_time = (int)(m_time.getTimeMillis() - m_initial_time);
    		m_number_sc_commands ++;
    		if(command.equalsIgnoreCase(Constants.COMMAND_EXIT) == true) {
            	m_logger.info(TAG + "EXIT command received!");
            	m_exit = true;
                accessToACKReply(true, 1); //GOOD ACK
            } else {
            	m_logger.warning(TAG + "Unknown received command from the platform");
            	accessToACKReply(true, 0); //BAD ACK
            } 
        }
    }
    
    public void run() 
    {
        
        try {            
            /* Common variables */
            long spent_time;
            long time_to_sleep;
            
            /* Update the boot count */
            updateBootCount();
                       
            /* Start PacketDispatcher */
            m_dispatcher.start();
            
            /* Start the main loop and indicate that we are ready */
            m_logger.info(TAG + "Software version " + Constants.sw_version);
            m_logger.info(TAG + "Start Main Loop");
            accessToStatus(true, Constants.REPLY_STATUS_RUNNING);
            
            while(m_exit == false) {
            	
            	/* Get current time to compute the rate */
                m_time_tick = m_time.getTimeMillis(); /* Time in milliseconds */
                
                /* command */
                checkOperationsCommand();
                
                if(m_exit == false) {
                    
                    /* Pool Generator and FSSProtocol Threads - Only when they should be executing*/
                    /* Data Generator */
                    //if(m_generator.getState() != Thread.State.TERMINATED) {
                    //    
                    //	if(!m_generator.polling(false)) {
                    //        m_generator_counter ++;
                     //       m_data_generator_polling = 1;
                    //    } else {
                    //        m_generator_counter = 0;
                    //        m_data_generator_polling = 0;
                    //    }
                    //    
                     //   if (m_generator_counter >= Constants.generator_max_polling
                     //   	&& m_data_generator_error_notification == false) {
                       //     /* ERROR m_generator does not reply */
                    //    	m_logger.error(TAG + "DataGenerator is not polling");
                    //    	m_data_generator_error_notification = true;
                    //    	accessToErrorMessage(true, TAG + "DataGenerator is not polling");
                   //     }
                   // } else if(m_data_generator_error_notification == false){
                   // 	m_logger.warning(TAG + "The DataGenerator is TERMINATED. Is this coherent?");
                   // 	m_data_generator_error_notification = true;
                   // }
                    
                    /* FSS Protocol */
                    if(m_fss_protocol.getState() != Thread.State.TERMINATED) {
                        if(!m_fss_protocol.polling(false)) {
                            m_fss_protocol_counter ++;
                            m_fss_protocol_polling = 1;
                            m_logger.error(TAG + "FSSProtocol pol down - Try again");
                        } else {
                            m_fss_protocol_counter = 0;
                            m_fss_protocol_polling = 0;
                        }
                        
                        if (m_fss_protocol_counter >= Constants.fss_protocol_max_polling
                        	&& m_fss_protocol_error_notification == false) {
                            /* ERROR m_generator does not reply */
                            m_logger.error(TAG + "FSSProtocol is not polling");
                            m_fss_protocol_error_notification = true;
                            accessToErrorMessage(true, TAG + "FSSProtocol is not polling");
                        }
                    } else if(m_fss_protocol_error_notification == false) {
                    	m_logger.warning(TAG + "The FSSProtocol is TERMINATED. Is it coherent?");
                    	m_fss_protocol_error_notification = true;
                    }
                    
                    if(m_data_generator_error_notification == true 
                		&& m_fss_protocol_error_notification == true) {
                    	m_logger.info(TAG + "All threads are terminated, the experiment has finished.");
                    	transitFromRunningToFinished();
                    }
                }
                	
                
                /* Retrieve Housekeeping */ 
                if(m_time_tick >= m_next_hk) {
                	/* Request dispatcher */
                	m_dispatcher.requestHK(m_prot_num);
                	while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
                		try {
                			/* Wait and release the processor */
                        	Thread.sleep(10);
                        } catch(InterruptedException e) {
                        	m_logger.error(e);
                        }
                	}
                	if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 1) {
	                	/* Only in the Telemetry case, the data is useful; but header and checksum must 
	                	 * be read to decrease the buffer */
                		m_rfisltelemetry_buffer.read(m_ipc_header_stream);
	                	m_rfisltelemetry_buffer.read(m_rf_isl_hk);
	                	m_rfisltelemetry_buffer.read(m_ipc_checksum_stream);
	                	/* Update the counter */
                		m_rf_isl_counter = 0;
                		m_rf_isl_alive = true;
                	} else {
                		/* Problem of IPC communications */
                		if(m_rf_isl_counter >= Constants.rf_isl_max_polling) {
                			/* The RF ISL Module does not reply - Comms error? */
                			m_logger.error(TAG + "RF ISL Module does not reply correctly - wrong polling");
                			accessToErrorMessage(true, TAG + "RF ISL Module does not reply correctly - wrong polling");
                			m_rf_isl_alive = false;
                			Arrays.fill(m_rf_isl_hk, (byte) 0); /* Set all the parameters to 0 */
                		} else {
                			m_rf_isl_counter ++;
                		}
                	}
                	updateDataBlock();
                	m_number_generated += 1;
                	storeHK();
                	storeDataBlock();
	                m_next_hk = m_time_tick + m_conf.manager_hk_period;
                }
                
                if((m_fss_protocol_counter >= Constants.fss_protocol_max_polling || 
                		m_generator_counter >= Constants.generator_max_polling ||
        				m_rf_isl_alive == false)) {
                	transitToErroneousFinished();
                }
                
                /* Flush the LOG */
                //m_logger.flush();
                
                /* Sleep */
                if(m_exit == false) {
                    spent_time = m_time.getTimeMillis() - m_time_tick;
                    if(spent_time < Constants.manager_sleep) {
                        try {
                        	time_to_sleep = Constants.manager_sleep - spent_time;
                        	if(time_to_sleep <= Constants.manager_sleep && time_to_sleep > 0) {
                        		Thread.sleep(time_to_sleep);
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
            }
            
        } catch(Exception e) {
        	e.printStackTrace();
        	m_logger.error(e);
        }
        
        /* Close IPC stack */
        m_dispatcher.controlledStop();
        int m_exit_counter = 0;
        while(m_dispatcher.getState() != Thread.State.TERMINATED 
                && m_dispatcher.getState() != Thread.State.NEW
                && m_exit_counter < Constants.manager_exit_max) {
                m_logger.info(TAG + "PacketDispatcher not terminated (status " + m_dispatcher.getState() + "), waiting a little more");
                m_exit_counter ++;
                try {
                    Thread.sleep(Constants.manager_sleep);
                } catch (InterruptedException e) {
                    m_logger.error(e);
                }
             }
        if(m_exit_counter == Constants.manager_exit_max) {
        	m_logger.error(TAG + "Impossible to courteously terminate PacketDispatcher (kill it!)");
        }
        
        System.out.println(TAG + "Bye Bye!");
    }

    private void updateDataBlock()
    {
    	m_payload_data.sat_id = m_sat_id;
    	m_payload_data.timestamp = m_time.getTimeMillis();
    	updateHK();
    }
    
    private void storeDataBlock()
    {
    	m_fss_buffer.insertData(m_payload_data.getBytes());
    }
    
    public synchronized int accessToStatus(boolean write, int status) { 
        
    	if(write == true) {
            m_status = status;
        }
        
        return m_status;
    }

    public synchronized boolean accessToError(boolean write, boolean err_status) { 
        
        if(write == true) {
            m_err_finished = err_status;
        }
        
        return m_err_finished;
    }
    
    public synchronized String accessToErrorMessage(boolean write, String message) { 
        
        if(write == true) {
            m_err_message = message;
        }
        
        return m_err_message;
    }
    
}


