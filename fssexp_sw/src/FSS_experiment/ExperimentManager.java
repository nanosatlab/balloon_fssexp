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
import Storage.FSSDataBuffer;
import Storage.HousekeepingBuffer;
import Storage.Log;
import Storage.PacketExchangeBuffer;
import TAR.TarEntry;
import TAR.TarOutputStream;
import jZLib.GZIPOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import Common.Constants;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import FSS_protocol.FSSProtocol;

/* When working in Linux Computer */
import java.io.FileInputStream;
import java.io.File;
import java.io.FileOutputStream;

import IPCStack.SimpleLinkProtocol;
import IPCStack.UartInterface;
import Lockers.UartBuffer;

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
    private FSSDataBuffer m_fss_buffer;
    private DataGenerator m_generator;
    private FSSProtocol m_fss_protocol;
    private HousekeepingBuffer m_hk_buffer;
    private SimpleLinkProtocol m_ipc_stack;
    private TimeUtils m_time;
    private UartBuffer m_uart_rx_buffer;
    private UartBuffer m_uart_tx_buffer;
    private UartInterface m_uart_driver;
    
    /* Manager attributes */
    private int m_status;                       /**< State of the experiment */
    private int m_generator_counter;
    private int m_fss_protocol_counter;
    private boolean m_exit;
    private long m_initial_time;
    private int m_exp_number;
    private int m_command_hk;
    private int m_hk_timestamp;
    private long m_start_time;
    private boolean m_err_finished;
    private String m_err_message;
    private int m_rf_isl_counter;
    private boolean m_rf_isl_alive;
    private File m_persistent_file;
    private FileInputStream m_persistent_reader;
    private FileOutputStream m_persistent_writer;
    
    /* Housekeeping */
    ByteBuffer m_hk_header;
    ByteBuffer m_hk_item;
    private long m_time_tick;
    private long m_next_hk;
    private int m_data_generator_polling;
    private int m_fss_protocol_polling;
    private int m_command_time = 0;
    private int m_number_sc_commands = 0;
    private byte[] m_rf_isl_hk;
    
    private int m_uart_driver_counter;
    private int m_uart_driver_polling;
    
    private boolean m_data_generator_error_notification;
    private boolean m_fss_protocol_error_notification;
    private boolean m_uart_driver_error_notification;
    
    private String m_command;
    private int m_ack_reply;
    
    public ExperimentManager(Log logger, TimeUtils timer) throws FileNotFoundException {
    	
    	/* Internal objects */
    	m_logger = logger;
    	m_time = timer;
    	m_conf = new ExperimentConf(m_logger);
    	m_hk_packets = new PacketExchangeBuffer(m_logger);
    	m_fss_buffer = new FSSDataBuffer(m_logger, m_conf);
    	m_uart_rx_buffer = new UartBuffer(m_logger, "rx_buffer");
    	m_uart_tx_buffer = new UartBuffer(m_logger, "tx_buffer");
    	m_uart_driver = new UartInterface(m_logger, m_uart_tx_buffer, m_uart_rx_buffer, m_time, m_conf);
    	m_ipc_stack = new SimpleLinkProtocol(m_logger, m_conf, m_time, m_uart_tx_buffer, m_uart_rx_buffer);
    	m_generator = new DataGenerator(m_logger, m_conf, m_fss_buffer, m_ipc_stack, m_time);
    	m_fss_protocol = new FSSProtocol(m_logger, m_fss_buffer, m_hk_packets, m_conf, m_ipc_stack, m_time);
    	m_hk_buffer = new HousekeepingBuffer(m_logger);
    	m_hk_header = ByteBuffer.allocate(Constants.hk_header_size);
    	m_hk_item = ByteBuffer.allocate(Constants.hk_item_size);
    	
    	/* Manager attributes */
    	accessToStatus(true, Constants.REPLY_STATUS_ERROR);
    	m_generator_counter = 0;
    	m_fss_protocol_counter = 0;
    	m_exit = false;
    	m_initial_time = 0;
    	m_exp_number = 0;
    	m_command_hk = 0;
    	m_start_time = 0;
    	m_err_finished = false;
    	m_err_message = "";
    	m_rf_isl_counter = 0;
    	m_rf_isl_alive = true;
    	m_persistent_file = new File(Constants.persistent_file);
        m_persistent_reader = null;
        m_persistent_writer = null;
        
        if(m_persistent_file.exists() == false) {
            try {
            	m_persistent_file.createNewFile();
            	m_persistent_reader = new FileInputStream(m_persistent_file);
            } catch (IOException e) {
                m_logger.error(e);
            }
        } else {
        	m_persistent_reader = new FileInputStream(m_persistent_file);
        }
    	
    	/* Housekeeping */
    	m_time_tick = 0;
    	m_next_hk = 0;
    	m_data_generator_polling = 0;
    	m_fss_protocol_polling = 0;
    	m_command_time = 0;
    	m_hk_timestamp = 0;
    	m_number_sc_commands = 0;
    	m_rf_isl_hk = new byte[Constants.rf_isl_tostorehk_size];
    	
    	m_uart_driver_counter = 0;
    	m_uart_driver_polling = 0;
    	
    	m_data_generator_error_notification = false;
    	m_fss_protocol_error_notification = false;
    	m_uart_driver_error_notification = false;
    	
    	/* Interface with TCPInterface */
    	m_command = "";
    	m_ack_reply = -1;
    	
    	/* Exceptionally, initialize the IPC Stack to retrieve status - the only
    	 * configuration parameter is the redundancy, and it is no explicitly needed
    	 * to retrieve RF ISL Module status.
    	 */
    	m_ipc_stack.setConfiguration();
    	//m_ipc_stack.open();
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
    
    private boolean transitFromReadyToRunning() {
    	m_logger.info(TAG + "Transition from READY to RUNNING");    	
    	
    	/* Lock the starting Time; it is interesting to include this here, because 
    	 * the time has already set up by the platform with the first command, thus
    	 * at least this command is executed. This time is simple used for HK
    	 * which starts to be stored when a START command is received. */
        m_initial_time = m_time.getTimeMillis();
    	
    	
    	/* Parse configuration */
    	m_conf.parseConf();
    	m_logger.info(TAG + "Configuration version: " + m_conf.version);
    	m_ipc_stack.setConfiguration();
    	m_fss_buffer.setConfiguration();
    	m_generator.setConfiguration();
    	m_fss_protocol.setConfiguration();
    	
    	/* Generate the HK header */
    	m_hk_header.clear();
        m_hk_header.put((byte)(Constants.sw_version & 0xFF));
        m_hk_header.put((byte)((m_exp_number & 0x7F) | ((m_conf.satellite_id & 0x01) << 7)));
        m_hk_header.putLong(m_initial_time);
        m_hk_buffer.writeHK(m_hk_header.array());
    	
    	
    	/* Configure the RF ISL Module */
    	ByteBuffer temp = ByteBuffer.allocate(Float.SIZE / 8).putFloat(m_conf.rf_isl_freq);
    	int counter = 0;
    	m_logger.info(TAG + "Trying to send the Configuration of the RF ISL Module");
    	while((Boolean)(m_ipc_stack.accessToIPCStack(Constants.SLP_ACCESS_CONF, temp.array())) == false
    			&& counter <= Constants.rf_isl_max_configuration) {
    		counter ++;
    		m_logger.warning(TAG + "Failed to send the RF ISL Configuration; Try number " + counter);
    	}
    	
    	if(counter >= Constants.rf_isl_max_configuration) {
    		m_logger.error(TAG + "Impossible to send the RF ISL Configuration");
    		
    		/* Notify the error to the platform */
    		accessToErrorMessage(true, TAG + "Impossible to send the RF ISL Configuration - No communication");
    		accessToError(true, true);
    		accessToStatus(true, Constants.REPLY_STATUS_FINISHED);
    		return false;
    	}
    	
    	/* Create Files */
    	/** FSS_BUFFER.data **/
    	m_fss_buffer.resetBuffer();
    	/** RX_PACKETS.data & TX_PACKETS.data **/
    	m_hk_packets.resetBuffer();
    	
    	/* Indicate the starting time */
        m_start_time = m_time.getTimeMillis();
        
        /* Clear Uart Buffer */
        m_uart_rx_buffer.clear();
        m_uart_tx_buffer.clear();
        
    	/* Start Threads */
        m_generator = new DataGenerator(m_logger, m_conf, m_fss_buffer, m_ipc_stack, m_time);
        m_fss_protocol = new FSSProtocol(m_logger, m_fss_buffer, m_hk_packets, m_conf, m_ipc_stack, m_time);
        
        m_generator.start();
        m_fss_protocol.start();
        
        /* Clean possible error status */
        accessToErrorMessage(true, "");
		accessToError(true, false);
		
        return true;
    }
    
    private void transitFromRunningToFinished() throws IOException {
    	m_logger.info(TAG + "Transition from RUNNING to FINISHED");
    	/* Close the Threads */
    	stopChilds();
    	
    	/* Close IPC stack */
    	//m_ipc_stack.close();
    	m_logger.info(TAG + "IPC stack closed");
    	
        /** Retrieve the final HK **/
        storeHK();
        m_logger.info(TAG + "Final HK buffered");
        
    	/* Store the resulting files */
        /** Include Footer in the HK file **/
        ByteBuffer footer = ByteBuffer.allocate(Integer.SIZE / 8);
        footer.putInt(m_fss_protocol.getDuration());
        footer.flip();
        m_hk_buffer.writeHK(footer.array());
        footer.clear();
        m_logger.info(TAG + "Footer HK stored");
        
        /** Transfer TX and RX files **/
        m_fss_buffer.moveToDownload();
        m_hk_packets.moveToDownload();
        m_logger.info(TAG + "Moved HK packets to_download folder");
        m_logger.info(TAG + "Moved FSS buffer to_download folder");
        
        /** Transfer HK **/
        m_hk_buffer.moveToDownload();
        m_logger.info(TAG + "Moved HK buffer to_download folder");
        
        /** Transfer Log **/
        m_logger.moveToDownload();
        
        /** Compress **/
        String name = compressResults();
        
        /** Reset the Log **/
        m_logger.resetLog();
        m_logger.info(TAG + "Moved LOG to_download folder");
        m_logger.info(TAG + name + " file created to be download");
        
        
        /** Change status **/
        m_logger.info(TAG + "Changing status to FINISHED");
        accessToStatus(true, Constants.REPLY_STATUS_FINISHED);
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
    
    private void stopChilds() {
    	/* Close the Threads */
    	m_fss_protocol.controlledStop();
        m_generator.controlledStop();
    	
    	/* Verify that the Threads are dead */
    	int m_exit_counter = 0;
        while(m_generator.getState() != Thread.State.TERMINATED 
           && m_generator.getState() != Thread.State.NEW
           && m_exit_counter < Constants.manager_exit_max) {
            m_logger.info(TAG + "DataGenerator not terminated (status " + m_generator.getState() + "), waiting a little more");
            m_exit_counter ++;
            try {
                Thread.sleep(Constants.manager_sleep);
            } catch (InterruptedException e) {
                m_logger.error(e);
            }
        }
        if(m_exit_counter == Constants.manager_exit_max) {
        	m_logger.error(TAG + "Impossible to courteously terminate DataGenerator (kill it!)");
        }
        
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
    
    private String compressResults() throws IOException {
    
    	/** Compress files **/
        //String tar_name = Constants.download_path + String.format("%03d", m_exp_number) + "_" + space.golbriak.lang.System.currentTimeMillis() + "_" + Constants.dwn_file_pattern;
        String tar_name = Constants.download_path + String.format("%03d", m_exp_number) + "_" + m_time.getTimeMillis() + "_" + Constants.dwn_file_pattern;
        
        String gzip_name = tar_name + ".gz";
        File tar_file = new File(tar_name);
        File gzip_file = new File(gzip_name);

        /*** TAR file ***/
        TarOutputStream tar_stream = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(tar_file)));
        File folder = new File(Constants.download_path);
        int count;
        byte data[] = new byte[2048];
        for(File f : folder.listFiles()){
            if(f.getName().contains(".data")) {
                //tar_stream.putNextEntry(new TarEntry(f, f.getName()));
                BufferedInputStream origin = new BufferedInputStream(new FileInputStream(f));
                while((count = origin.read(data)) != -1) {
                    tar_stream.write(data, 0, count);
                    //tar_stream.flush();
                }
                origin.close();
                f.delete();
            }
        }
        tar_stream.close();
        
        /*** GZIP file ***/
        GZIPOutputStream gzip_stream = new GZIPOutputStream(new FileOutputStream(gzip_file));
        FileInputStream tar_input_stream = new FileInputStream(tar_name);
        while((count = tar_input_stream.read(data)) != -1){
            gzip_stream.write(data, 0, count);
            //gzip_stream.flush();
        }
        gzip_stream.close();
        tar_input_stream.close();
        tar_file.delete();
        
        return gzip_name;
    }
    
    private void storeHK() {
    	m_hk_timestamp = (int)((m_time.getTimeMillis() - m_initial_time) & 0xFFFFFFFF);
        
    	m_hk_item.putInt(m_hk_timestamp);
        /* Manager */
        if(m_status == Constants.REPLY_STATUS_READY) {
        	m_hk_item.put((byte)(-1 & 0xFF));
        } else {
        	m_hk_item.put((byte)(m_conf.version & 0xFF));
        }
        m_hk_item.put((byte)((((accessToStatus(false, 0) << 5) & 0xE0)) | ((m_command_hk << 3) & 0x18) | (m_data_generator_polling & 0x02) | (m_fss_protocol_polling & 0x01)));
        m_hk_item.putInt(m_command_time);
        m_hk_item.put((byte)(m_number_sc_commands & 0xFF));
        /* DataGenerator */
        m_hk_item.putInt(m_generator.getGenerated());
        if(m_generator.getStatus() == true) {
        	m_hk_item.put((byte)(Constants.generator_STATUS_RUNNING & 0xFF));
        } else {
        	m_hk_item.put((byte)(Constants.generator_STATUS_STOPPED & 0xFF));
        }
        /* FSSDataBuffer */
        m_hk_item.putShort((short)(m_fss_buffer.getSize() & 0xFFFF));
        m_hk_item.putShort((short)(m_fss_buffer.getDrops() & 0xFFFF));
        /* FSSProtocol */
        m_hk_item.put((byte)((m_fss_protocol.getFederationStatus() & 0xFF) | ((m_fss_protocol.getFederationRole() & 0xFF) << 2)));
        m_hk_item.putShort((short)(m_fss_protocol.getTXs() & 0xFFFF));
        m_hk_item.putShort((short)(m_fss_protocol.getRXs() & 0xFFFF));
        m_hk_item.putShort((short)(m_fss_protocol.getErrRXs() & 0xFFFF));
        /* RF ISL Module */
        if(m_rf_isl_alive == true) {
        	m_hk_item.put((byte)(0 & 0xFF));
        } else {
        	m_hk_item.put((byte)(1 & 0xFF));
        }
        m_hk_item.put(m_rf_isl_hk);
        m_hk_buffer.writeHK(m_hk_item.array());
        m_hk_item.clear();
    }
    
    public void run() {
        
        try {            
        	
            /* Common variables */
            String command;
            long spent_time;
            long time_to_sleep;
            byte[] data = new byte[Constants.data_rf_isl_hk_size];
            
            /* Start Uart interface Thread */
            m_uart_driver.start();
            
            /* Update the boot count */
            if(m_persistent_reader != null) {
                try {
                	if(m_persistent_file.length() > 0) {
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
                       
            /* Start the main loop and indicate that we are ready */
            m_logger.info(TAG + "Software version " + Constants.sw_version);
            m_logger.info(TAG + "Start Main Loop");
            accessToStatus(true, Constants.REPLY_STATUS_READY);
            
            while(m_exit == false) {
            	
                /* Get current time to compute the rate */
                m_time_tick = m_time.getTimeMillis(); /* Time in milliseconds */
                
                if(m_time_tick >= m_start_time + Constants.manager_max_exec_time 
            		&& m_start_time != 0 
            		&& accessToStatus(false, 0) != Constants.REPLY_STATUS_FINISHED) {
                    m_logger.info(TAG + "Reached maximum execution time: " + Constants.manager_max_exec_time);
                    transitFromRunningToFinished();
                    /*m_generator.controlledStop();
                    m_fss_protocol.controlledStop();
                    accessToStatus(true, Constants.REPLY_STATUS_FINISHED);*/
                }
                
                /* command */
                try {
                	
                	command = accessToCommand(false, "");
              
                	if(command.equals("") == false) {
                	
                		System.out.println(TAG + "Received Command " + command);
                		m_logger.info(TAG + "Received Command " + command);
                		
                		m_command_time = (int)(m_time.getTimeMillis() - m_initial_time);
                		m_number_sc_commands ++;
                		
                		if(command.equalsIgnoreCase(Constants.COMMAND_EXIT) == true) {
                        	m_logger.info(TAG + "EXIT command received!");
                        	m_command_hk = Constants.COMMAND_PARSED_EXIT;
                        	if(accessToStatus(false, 0) == Constants.REPLY_STATUS_FINISHED) {
                        		m_logger.info(TAG + "EXITing from FINISHED");
                            	
                        		m_exit = true;
                            	accessToACKReply(true, 1); //GOOD ACK
                            } else if(accessToStatus(false, 0) == Constants.REPLY_STATUS_READY) {
                            	m_logger.info(TAG + "EXITing from READY");
                            	m_exit = true;
                            	accessToACKReply(true, 1); //GOOD ACK
                            } else if(accessToStatus(false, 0) == Constants.REPLY_STATUS_NEGOTIATION 
                            		|| accessToStatus(false, 0) == Constants.REPLY_STATUS_FEDERATION
                            		|| accessToStatus(false, 0) == Constants.REPLY_STATUS_CLOSURE) {
                            	m_exit = true;
                            	transitFromRunningToFinished();
                            	accessToACKReply(true, 1); //GOOD ACK
                            }
                        } else if(command.equalsIgnoreCase(Constants.COMMAND_STATUS) == true) {
                        	m_logger.info(TAG + "STATUS command received!");
                        	m_command_hk = Constants.COMMAND_PARSED_STATUS;
                        } else {
                        	m_command_hk = Constants.COMMAND_PARSED_UNKNOWN;
                        	m_logger.warning(TAG + "Unknown received command from the platform");
                        	accessToACKReply(true, 0); //BAD ACK
                        }
                        
                    }
                } catch (IOException e) {
                    m_logger.error(e);
                }
                
                
                if(m_exit == false) {
                	
                    /* Define Experiment Status */
                    if((accessToStatus(false, 0) == Constants.REPLY_STATUS_READY || accessToStatus(false, 0) == Constants.REPLY_STATUS_FINISHED)
                        && (m_fss_protocol.getState() == Thread.State.TIMED_WAITING || m_fss_protocol.getState() == Thread.State.RUNNABLE)
                        && m_fss_protocol.getFederationStatus() == Constants.FSS_NEGOTIATION) {
                        m_logger.info(TAG + "In NEGOTIATION mode");
                    	accessToStatus(true, Constants.REPLY_STATUS_NEGOTIATION);
                    } else if(accessToStatus(false, 0) == Constants.REPLY_STATUS_NEGOTIATION
                    	&& (m_fss_protocol.getState() == Thread.State.TIMED_WAITING || m_fss_protocol.getState() == Thread.State.RUNNABLE)
                    	&& m_fss_protocol.getFederationStatus() == Constants.FSS_EXCHANGE) {
                        
                    	accessToStatus(true, Constants.REPLY_STATUS_FEDERATION);
                    } else if(accessToStatus(false, 0) == Constants.REPLY_STATUS_FEDERATION
                    	&& (m_fss_protocol.getState() == Thread.State.TIMED_WAITING || m_fss_protocol.getState() == Thread.State.RUNNABLE)
                    	&& m_fss_protocol.getFederationStatus() == Constants.FSS_CLOSURE) {
                        
                    	accessToStatus(false, Constants.REPLY_STATUS_CLOSURE);
                    } else if(accessToStatus(false, 0) == Constants.REPLY_STATUS_CLOSURE
                    	&& m_fss_protocol.getState() == Thread.State.TERMINATED) {
                        transitFromRunningToFinished();
                    }
                    
                    /* Pool Generator and FSSProtocol Threads - Only when they should be executing*/
                    if(accessToStatus(false, 0) == Constants.REPLY_STATUS_NEGOTIATION || 
                    	accessToStatus(false, 0) == Constants.REPLY_STATUS_FEDERATION ||
                    	accessToStatus(false, 0) == Constants.REPLY_STATUS_CLOSURE) {
                        
                        /* Data Generator */
                        if(m_generator.getState() != Thread.State.TERMINATED) {
                            
                        	if(!m_generator.polling(false)) {
                                m_generator_counter ++;
                                m_data_generator_polling = 1;
                            } else {
                                m_generator_counter = 0;
                                m_data_generator_polling = 0;
                            }
                            
                            if (m_generator_counter >= Constants.generator_max_polling
                            	&& m_data_generator_error_notification == false) {
                                /* ERROR m_generator does not reply */
                            	m_logger.error(TAG + "DataGenerator is not polling");
                            	m_data_generator_error_notification = true;
                            	accessToErrorMessage(true, TAG + "DataGenerator is not polling");
                            }
                        } else if(m_data_generator_error_notification == false){
                        	m_logger.warning(TAG + "The DataGenerator is TERMINATED. Is this coherent?");
                        	m_data_generator_error_notification = true;
                        }
                        
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
                    		&& m_fss_protocol_error_notification == true
                    		&& (accessToStatus(false, 0) == Constants.REPLY_STATUS_NEGOTIATION 
                    		|| accessToStatus(false, 0) == Constants.REPLY_STATUS_FEDERATION
                    		|| accessToStatus(false, 0) == Constants.REPLY_STATUS_CLOSURE)) {
                        	m_logger.info(TAG + "All threads are terminated, the experiment has finished.");
                        	transitFromRunningToFinished();
                        }
                        
                        
                    } else {
                        m_data_generator_polling = 0;
                        m_fss_protocol_polling = 0;
                    }
                }
                
                /* Pool UartInterface */
                if(m_uart_driver.getState() != Thread.State.TERMINATED) {
                	
                	if(m_uart_driver.polling(false) == false) {
                        m_uart_driver_counter ++;
                        m_uart_driver_polling = 1;
                    } else {
                    	m_uart_driver_counter = 0;
                        m_uart_driver_polling = 0;
                    }
                    
                    if (m_uart_driver_counter >= Constants.uart_interface_max_polling
                    	&& m_uart_driver_error_notification == false) {
                        /* ERROR m_generator does not reply */
                    	m_logger.error(TAG + "UartInterface is not polling");
                    	m_uart_driver_error_notification = true;
                    	accessToErrorMessage(true, TAG + "UartInterface is not polling");
                    }
                	
                } else {
                	m_logger.error(TAG + "UartInterface is Terminated, it should be working");
                }
                
                /* Retrieve Housekeeping */  
                if(accessToStatus(false, 0) != Constants.REPLY_STATUS_FINISHED 
                		&& accessToStatus(false, 0) != Constants.REPLY_STATUS_READY
                		&& m_time_tick >= m_next_hk) {
                	
                	data = (byte[])m_ipc_stack.accessToIPCStack(Constants.SLP_ACCESS_TELEMETRY, null);
                	if(data.length == 0) {
                		if(m_rf_isl_counter >= Constants.rf_isl_max_polling) {
                			/* The RF ISL Module does not reply - Comms error? */
                			m_logger.error(TAG + "RF ISL Module does not reply correctly - wrong polling");
                			accessToErrorMessage(true, TAG + "RF ISL Module does not reply correctly - wrong polling");
                			m_rf_isl_alive = false;
                			Arrays.fill(m_rf_isl_hk, (byte) 0); /* Set all the parameters to 0 */
                		} else {
                			m_rf_isl_counter ++;
                		}
                	} else {
                		m_rf_isl_counter = 0;
                		m_rf_isl_alive = true;
                		System.arraycopy(data, 0, m_rf_isl_hk, 0, Constants.rf_isl_tostorehk_size);
                	}
                	storeHK();
                	m_next_hk = m_time_tick + m_conf.manager_hk_period;
                }
                
                if((m_fss_protocol_counter >= Constants.fss_protocol_max_polling || 
                		m_generator_counter >= Constants.generator_max_polling ||
        				m_uart_driver_counter >= Constants.uart_interface_max_polling ||
                		m_rf_isl_alive == false) && 
                		accessToStatus(false, 0) != Constants.REPLY_STATUS_FINISHED) {
                	transitToErroneousFinished();
                }
                
                /* Flush the LOG */
                m_logger.flush();
                
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

        /* Close the UART Interface */
        m_uart_driver.close();
        int counter = 0;
        while(m_uart_driver.getState() != Thread.State.TERMINATED
        		&& counter < 50) {
	        try {
				sleep(10);
				counter ++;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        System.out.println(TAG + "Bye Bye!");
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


