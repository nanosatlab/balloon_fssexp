package IPCStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.security.auth.login.Configuration;

import com.fazecast.jSerialComm.SerialPort;

import Common.Constants;
import Common.TimeUtils;
import Lockers.UartBuffer;
import Storage.Log;
import Configuration.ExperimentConf;

public class UartInterface extends Thread{
	
	private SerialPort m_port;
	private Log m_logger;
	private InputStream m_input_stream;
	private OutputStream m_output_stream;
	private UartBuffer m_rx_buffer;
	private UartBuffer m_tx_buffer;
	private TimeUtils m_time;
	private String m_port_desc;
	
	private boolean m_exit; 
	private boolean m_poll_token;
	
	private boolean m_is_open;
	private long m_time_limit;
	
	private final static String TAG = "[UartInterface] ";
	
	public UartInterface(Log log, UartBuffer tx_buffer, UartBuffer rx_buffer, TimeUtils timer, ExperimentConf conf) {
		m_logger = log;
		m_time = timer;
		m_is_open = false;
		m_rx_buffer = rx_buffer;
		m_tx_buffer = tx_buffer;
		m_exit = false;
		m_time_limit = 0;
		m_poll_token = false;
		m_port_desc = conf.port_desc;
		open();
	}
	
	public synchronized boolean isOpen() {
		return m_port.isOpen();
	}
	
	public boolean open() {
		/* With Linux Computer uncomment the following two lines */
		System.out.println("Opening SerialPort " + m_port_desc);
		m_port = SerialPort.getCommPort(m_port_desc);
		if(m_port.openPort() == true) {
			System.out.println("Port " + m_port_desc + " correctly opened");
		} else {
			System.out.println("Impossible to open Port " + m_port_desc);
		}
		m_port.setBaudRate(Constants.uart_bps);
		m_port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		m_is_open = true;
		m_input_stream = m_port.getInputStream();
		if(m_input_stream == null) {
			System.out.println("Null pointer of InputStream");
		}
		m_output_stream = m_port.getOutputStream();
		if(m_input_stream == null) {
			System.out.println("Null pointer of OutputStream");
		}
		return m_is_open;
	}
	
	public void run() {
		
		byte[] data;
		byte[] removed_data;
		int data_read;
		
		try {
			while(m_exit == false) {
				
				/* Pool! */
				if(m_poll_token == false) {
					polling(true);
				}
					
				/* Read TX BUFFER */
				try {
					if(m_tx_buffer.bytesAvailable() > 0) {
						
						/* TODO: this can be improved */
						data = new byte[m_tx_buffer.bytesAvailable()];
						data_read = m_tx_buffer.read(data);
						
						if(data_read == data.length) {
							/* Remove the data that can be in the stream */
							if(m_input_stream.available() > 0) {
								removed_data = new byte[m_input_stream.available()];
								m_logger.info(TAG + "Removed useless data that is in the InputStream when a command shall be sent");
							}
							
							if(m_rx_buffer.bytesAvailable() > 0) {
								m_rx_buffer.clear();
								m_logger.info(TAG + "Removed useless data that is in the RXBuffer when a command shall be sent");
							}
							
							m_output_stream.write(data);
						} else {
							m_logger.warning(TAG + "Read in the TX buffer not all the data (something get wrong)");
						}
					}
				} catch (IOException e) {
					m_logger.error(TAG + "Impossible to send through UART: IOException");
					m_logger.error(e);
				}
				
				/* Read the UART */
				try {
					
					if(m_input_stream.available() > 0) {
						data = new byte[m_input_stream.available()];
						m_input_stream.read(data);
						m_rx_buffer.write(data);
					}
					
				} catch (IOException e) {
					m_logger.error(TAG + "Impossible to read from UART: IOException");
					m_logger.error(e);
				} 
				
				sleep(Constants.uart_comms_sleep);
			}
		} catch(InterruptedException e) {
			m_logger.error(TAG + "During sleep of the thread, an interrupted exception appeared. The thread is closed");
			m_logger.error(e);
		}
		
		m_logger.info(TAG + "Stopped Thread");
		
	}
	
	public synchronized void close() {
		m_port.closePort();
		m_logger.info(TAG + "UART port closed");
	}
	
	public synchronized boolean polling(boolean poll) {
        boolean previous_poll = m_poll_token;
        m_poll_token = poll;
        return previous_poll;
    }
}
