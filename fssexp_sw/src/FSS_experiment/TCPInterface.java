package FSS_experiment;

/* External libraries */
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.net.ServerSocket;
import java.net.Socket;

import CBOR.CborConstants;
import CBOR.CborDecoder;
import CBOR.CborEncoder;
import Common.Constants;
import Common.FolderUtils;
import Common.TimeUtils;
import Storage.Log;

public class TCPInterface extends Thread{
	
	private Socket m_sc_socket = null;
    private ServerSocket m_fss_socket = null;
	private Log m_logger = null;
    private ExperimentManager m_experiment = null;
    private InputStream m_socket_input_stream; 
    private OutputStream m_socket_output_stream;
    private CborDecoder m_cbor_decoder;
    private CborEncoder m_cbor_encoder;
    private TimeUtils m_time_utils;
    private FolderUtils m_folder;
    private boolean m_exit;
    private boolean m_waiting_command;
    
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
		m_exit = false;
		m_length_field_bytes = new byte[Constants.LENGHT_FIELD_SIZE];
		m_intermediate_output_stream = new ByteArrayOutputStream();
    	m_waiting_command = false;
		m_length_field_converter = ByteBuffer.allocate(Constants.LENGHT_FIELD_SIZE);
    	
		try {
			m_experiment = new ExperimentManager(m_logger, m_time_utils, m_folder);
		} catch (FileNotFoundException e) {
			m_logger.error(TAG + "Impossible to create ExperimentManager: " + e.getMessage());
		}
	}
	
	public void run() {
		
		m_logger.info(TAG + "********* FSSExp SOFTWARE STARTED *********");
		
		String command;
		int reply;
		
		/* Launch ExperimentManager Thread */
		m_experiment.start();
    	
		/* TCP Server for Platform */
		try {
			m_fss_socket = new ServerSocket(Constants.server_socket);
		} catch (IOException e) {
			m_logger.error(e);
		}
		
		while(m_exit == false) {
			try {
				if(m_waiting_command == false) {
					/* Create Sockets */
				    m_logger.info(TAG + "Waiting connections...");
		            m_sc_socket = m_fss_socket.accept();
		            m_logger.info(TAG + "Connected to " + m_sc_socket);
		            m_waiting_command = true;
				
					m_socket_input_stream = m_sc_socket.getInputStream();
					m_socket_output_stream = m_sc_socket.getOutputStream();          
		            m_cbor_encoder = new CborEncoder(m_intermediate_output_stream);
		            m_cbor_decoder = new CborDecoder(m_sc_socket.getInputStream());
		            
				} else if(m_socket_input_stream.available() > Short.SIZE / 8 /* Wait command */ 
	        		&& (command = readCommand()).equals("") == false) {
	                	System.out.println(TAG + "Received Command " + command);
						m_logger.info(TAG + "Received Command " + command);
			
	            		/* Parse command */
	            		if(command.equalsIgnoreCase(Constants.COMMAND_STATUS)) {
	            			m_experiment.accessToCommand(true, command);
	            			writeReply(Constants.STATUS_REPLY_ITEMS, command, true);
	            		} else if(command.equalsIgnoreCase(Constants.COMMAND_EXIT)){
	            			
	            			/* Communicate to ExperimentManager */
	            			m_experiment.accessToCommand(true, command);
	            			Thread.sleep(100); // sleep a while to receive a reply
	            			
	            			/* Verify that it has been stopped correctly */
	            			//TODO: Clean this code
	            			for(int i = 0; i <= 10; i++) {
	            				if(m_experiment.getState() == Thread.State.TERMINATED) {
	            					System.out.println(TAG + "Experiment Manager TERMINATED");
	            					writeReply(Constants.GENERIC_REPLY_ITEMS, command, true);
	            					break;
	            				}
	            				System.out.println(TAG + "Experiment Manager not terminated yet");
	            				Thread.sleep(500);
	            			}
	            			if(m_experiment.getState() != Thread.State.TERMINATED) {
	            				writeReply(Constants.GENERIC_REPLY_ITEMS, command, false);
	            			}
	            			m_exit = true;
	            			
	            		} else {
	            			m_logger.warning(TAG + "Unknown received command from the platform: " + command);
	                    	writeReply(Constants.GENERIC_REPLY_ITEMS, command, false);
	            		}
	            		
	            		m_sc_socket.close();
	            		m_waiting_command = false;
	            		System.out.println(TAG + "Platform socket closed");
	            }
			}catch(Exception e) {
				e.printStackTrace();
				
				try {
					m_waiting_command = false;
					m_sc_socket.close();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}         
        
		/* Close the communication with Platform */
        if(m_sc_socket != null) {
            try {
                m_sc_socket.close();
            } catch (IOException e) {
                System.out.println(TAG + "Impossible to close Platform socket");
                m_logger.error(TAG + "Impossible to close Platform socket");
            }
        }
        
        if(m_fss_socket != null) {
        	try {
        		m_fss_socket.close();
        	} catch(IOException e) {
        		System.out.println(TAG + "Impossible to close TCP Server");
        		m_logger.error(TAG + "Impossible to close TCP Server");
        	}
        }
        
        System.out.println(TAG + "Bye Bye!");
        m_logger.info(TAG + "Bye Bye!");
        
        /* Close LOG */
        m_logger.info(TAG + "********* FSSExp SOFTWARE EXIT *********");
        m_logger.close();
	}
	
	private String readCommand() throws IOException {
    	
    	/* Variables */
    	long map_items_num;
    	int platform_timestamp;
    	String map_key;
    	String command = "";
    	short length;
    	
    	/* Read the LENGTH field */
    	m_socket_input_stream.read(m_length_field_bytes);
    	m_length_field_converter.clear();
        m_length_field_converter.put(m_length_field_bytes);
        m_length_field_converter.rewind();
    	length = m_length_field_converter.getShort();
        
        /* Read the CBOR Content - MAP */
    	m_cbor_content = new byte[length];
    	m_socket_input_stream.read(m_cbor_content);
    	m_intermediate_input_stream = new ByteArrayInputStream(m_cbor_content);
    	m_cbor_decoder = new CborDecoder(m_intermediate_input_stream);
    	map_items_num = m_cbor_decoder.readMapLength();
    	
        if(map_items_num == Constants.COMMAND_ITEMS) {
        	
        	/** The Command Map is composed of:
        	 *  { 
        	 *    'command': (String) START | STOP | STATUS | EXIT;
        	 *    'timestamp': (Unsigned Int) timestamp in UNIX
        	 *  }
        	 */
        	
        	for(int i = 0; i < map_items_num; i++) {
	            
        		if(m_cbor_decoder.peekType().getMajorType() == CborConstants.TYPE_TEXT_STRING) {
        			map_key = m_cbor_decoder.readTextString();
	                
	                if(map_key.equals(Constants.COMMAND_KEY) == true
	                	&& m_cbor_decoder.peekType().getMajorType() == CborConstants.TYPE_TEXT_STRING) {
	                	/* Reading the 'command' field - the command value */
	                    command = m_cbor_decoder.readTextString();
	                } else if(map_key.equals(Constants.TIMESTAMP_KEY) == true
	                			&& (m_cbor_decoder.peekType().getMajorType() == CborConstants.TYPE_UNSIGNED_INTEGER 
	                			|| m_cbor_decoder.peekType().getMajorType() == CborConstants.TYPE_NEGATIVE_INTEGER)) {
	                	
	                	/* Reading the 'timestamp' field - the UNIX timestamp value in seconds */
	                	platform_timestamp = (int)(m_cbor_decoder.readInt32());
	                	m_logger.info(TAG + "Received Plaftorm TIME: " + ((long)(platform_timestamp) * 1000));
	                } else {
	                	m_logger.error(TAG + "Bad Platform command: with key - " + map_key + " and the value type is " 
	                					+ m_cbor_decoder.peekType().getMajorType());
	                	return "";
	                } 
	            } else {
	            	m_logger.error("Bad Plaftorm command: The Key is not a String");
	            	return "";
	            }
        	} 
        } else {
        	m_logger.error("Bad Plaftorm command: The command does not have just " + Constants.COMMAND_ITEMS + " items");
        	return "";
        }
        return command;
    }
    
    private void writeReply(int items, String previous_command, boolean ack) throws IOException {
    	
    	if(items == Constants.GENERIC_REPLY_ITEMS) {
	    	/* Generate the Stream */
    		m_intermediate_output_stream.reset();
	        m_cbor_encoder.writeMapStart(Constants.GENERIC_REPLY_ITEMS);
	        m_cbor_encoder.writeTextString(Constants.COMMAND_KEY);
	        m_cbor_encoder.writeTextString(previous_command);
	        m_cbor_encoder.writeTextString(Constants.ACK_KEY);
	        if(ack == true) {
	        	m_cbor_encoder.writeTextString(Constants.REPLY_ACK_OK);
	        } else {
	        	m_cbor_encoder.writeTextString(Constants.REPLY_ACK_ERROR);
	        }
    	
    	} else if(items == Constants.STATUS_REPLY_ITEMS) {
    		/* Retrieve current status */
    		int temp_status = m_experiment.accessToStatus(false, 0);
    		boolean error = m_experiment.accessToError(false, false);
    		String err_message = m_experiment.accessToErrorMessage(false, "");
    		
    		/* Generate the Stream */
    		m_intermediate_output_stream.reset();
            if(error == false) {
            	m_cbor_encoder.writeMapStart(Constants.STATUS_REPLY_ITEMS);
            } else {
            	m_cbor_encoder.writeMapStart(Constants.STATUS_REPLY_ITEMS + 1);
            }
        	m_cbor_encoder.writeTextString(Constants.COMMAND_KEY);
            m_cbor_encoder.writeTextString(previous_command);
            m_cbor_encoder.writeTextString(Constants.ACK_KEY);
            if(ack == true) {
	        	m_cbor_encoder.writeTextString(Constants.REPLY_ACK_OK);
	        } else {
	        	m_cbor_encoder.writeTextString(Constants.REPLY_ACK_ERROR);
	        }
            m_cbor_encoder.writeTextString(Constants.MODE_KEY);
    	}
        
        /* Retrieve the CBOR length and Send it*/
        m_length_field_converter.clear();
        m_length_field_converter.putShort((short)(m_intermediate_output_stream.size()));
        m_length_field_converter.rewind();
        m_socket_output_stream.write(m_length_field_converter.array());
        m_socket_output_stream.write(m_intermediate_output_stream.toByteArray());
        m_intermediate_output_stream.reset();
    }

}
