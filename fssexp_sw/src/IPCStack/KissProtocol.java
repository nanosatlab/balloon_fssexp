package IPCStack;

import java.util.ArrayList;

import Common.Constants;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Storage.Log;

public class KissProtocol {

	private ArrayList<Byte> m_frame;
	private Log m_logger;
	private TimeUtils m_time;
	private UartInterface m_uart_driver;
	
	private final static String TAG = "[KissProtocol] ";
	
	public KissProtocol(Log log, TimeUtils timer, ExperimentConf conf) 
	{
		m_logger = log;
		m_frame = new ArrayList<Byte>();
		m_time = timer;
		m_uart_driver = new UartInterface(log, timer, conf);
	}
	
	public boolean open()
	{
		return m_uart_driver.open();
	}
	
	public void close()
	{
		m_uart_driver.close();
	}
	
	public boolean send(byte[] data) {
		
		byte[] frame;
		m_frame.clear();
		
		/* Include first FEND */
		m_frame.add((byte)Constants.KISS_FEND);
		/* Include content */
		for(byte b : data) {
			if(b == (byte)Constants.KISS_FEND) {
				m_frame.add((byte)Constants.KISS_FESC);
				m_frame.add((byte)Constants.KISS_TFEND);
			} else if(b == (byte)Constants.KISS_FESC) {
				m_frame.add((byte)Constants.KISS_FESC);
				m_frame.add((byte)Constants.KISS_TFESC);
			} else {
				m_frame.add(b);
			}
		}
		/* Close the frame with FEND */
		m_frame.add((byte)Constants.KISS_FEND);
		
		/* Send through the UART */
		frame = new byte[m_frame.size()];
		for(int i = 0; i < m_frame.size(); i++) {
			frame[i] = m_frame.get(i);
		}
		m_uart_driver.writeData(frame);
		
		return true;
	}
	
	public byte[] receive() {
		byte[] bytes;
		byte b;
		byte[] data = new byte[0];
		boolean started_frame = false;
		boolean finished_frame = false;
		boolean fesc_found = false;
		m_frame.clear();
		double start_time;
		
		if(m_uart_driver.bytesAvailable() > 0) {
			start_time = m_time.getTimeMillis();
			
			while(finished_frame == false) {
				if(m_uart_driver.bytesAvailable() > 0) {
					bytes = m_uart_driver.readByte();
					if(bytes.length == 1) {
						b = bytes[0];
						/* There is data to process */
						if((b & 0xFF) == Constants.KISS_FEND && started_frame == false) {
							started_frame = true;
						} else if((b & 0xFF) == Constants.KISS_FEND && finished_frame == false) {
							finished_frame = true;
						} else if((b & 0xFF) == Constants.KISS_FESC && fesc_found == false) {
							fesc_found = true;
						} else if((b & 0xFF) == Constants.KISS_TFEND && fesc_found == true) {
							m_frame.add((byte)Constants.KISS_FEND);
							fesc_found = false;
						} else if((b & 0xFF) == Constants.KISS_TFESC && fesc_found == true) {
							m_frame.add((byte)Constants.KISS_FESC);
							fesc_found = false;
						} else if(started_frame == true){
							m_frame.add(b);
						}
						
						if(started_frame == true && finished_frame == false) {
							start_time = m_time.getTimeMillis();
						}
					}
					
				} else if(m_time.getTimeMillis() >= start_time + Constants.kiss_max_byte_waiting) {
					m_logger.warning(TAG + "Started KISS frame, but no byte received; cleanning frame.");
					m_frame.clear();
					return new byte[0];
				}
			} 
			
			data = new byte[m_frame.size()];
			for(int i = 0; i < m_frame.size(); i++) {
				data[i] = m_frame.get(i);
			}
		}
		return data;
	}
	
	public int bytesAvailable() {
		return m_uart_driver.bytesAvailable();
	}
}
