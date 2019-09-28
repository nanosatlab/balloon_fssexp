package IPCStack;

import java.util.ArrayList;

import Common.Constants;
import Common.TimeUtils;
import Lockers.UartBuffer;
import Storage.Log;

public class KissProtocol {

	private UartBuffer m_tx_buffer;
	private UartBuffer m_rx_buffer;
	private ArrayList<Byte> m_frame;
	private Log m_logger;
	private TimeUtils m_time;
	
	private final static String TAG = "[KissProtocol] ";
	
	public KissProtocol(Log log, TimeUtils timer, UartBuffer tx_buffer, UartBuffer rx_buffer) {
		m_logger = log;
		m_frame = new ArrayList<Byte>();
		m_tx_buffer = tx_buffer;
		m_rx_buffer = rx_buffer;
		m_time = timer;
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
		
		if(m_tx_buffer.write(frame) == 0) {
			m_logger.error(TAG + "Impossible to write in the UART buffer; Is it full?");
			return false;
		}
		
		return true;
	}
	
	public byte[] receive() {
		//byte[] b;
		byte b;
		byte[] data = new byte[0];
		boolean started_frame = false;
		boolean finished_frame = false;
		boolean fesc_found = false;
		m_frame.clear();
		double start_time;
		
		if(m_rx_buffer.bytesAvailable() > 0) {
			start_time = m_time.getTimeMillis();
			
			while(finished_frame == false) {
				if(m_rx_buffer.bytesAvailable() > 0) {
					b = m_rx_buffer.readByte();
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
		return m_rx_buffer.bytesAvailable();
	}
}
