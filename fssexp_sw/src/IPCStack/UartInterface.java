package IPCStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;

import Common.Constants;
import Common.TimeUtils;
import Lockers.UartBuffer;
import Storage.Log;
import Configuration.ExperimentConf;

public class UartInterface {
	
	private SerialPort m_port;
	private Log m_logger;
	private InputStream m_input_stream;
	private OutputStream m_output_stream;
	private String m_port_desc;
	
	private final static String TAG = "[UartInterface] ";
	
	public UartInterface(Log log, TimeUtils timer, ExperimentConf conf) {
		m_logger = log;
		m_port_desc = conf.port_desc;
	}
	
	public boolean isOpen() {
		return m_port.isOpen();
	}
	
	public boolean open() {
		m_port = SerialPort.getCommPort(m_port_desc);
		if(m_port.openPort() == true) {
			m_logger.info(TAG + "Port " + m_port_desc + " correctly opened");
			System.out.println(TAG + "Port " + m_port_desc + " correctly opened");
			m_port.setBaudRate(Constants.uart_bps);
			//m_port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
			m_input_stream = m_port.getInputStream();
			m_output_stream = m_port.getOutputStream();
		} else {
			m_logger.error(TAG + "Impossible to open Port " + m_port_desc);
			System.out.println(TAG + "Impossible to open Port " + m_port_desc);
		}
		return m_port.isOpen();
	}
	
	public boolean writeData(byte[] data) 
	{
		boolean correct = false;
		if(m_port.isOpen() == true) {
			try {
				m_output_stream.write(data);
				correct = true;
			} catch (IOException e) {
				m_logger.error(TAG + "[ERROR] Impossible to write data to the UART port");
			}
		}
		return correct;
	}
	
	public byte[] readAllData() {
		byte[] data = new byte[0];
		if(m_port.isOpen() == true) {
			try {
				if(m_input_stream.available() > 0) {
					data = new byte[m_input_stream.available()];
					m_input_stream.read(data);
				}
			} catch (IOException e) {
				m_logger.error(TAG + "[ERROR] Impossible to read all data from the UART port");
			}
		}
		return data;
	}
	
	public byte[] readData(int length) {
		byte[] data = new byte[0];
		if(m_port.isOpen() == true) {
			try {
				if(m_input_stream.available() >= length) {
					data = new byte[length];
					m_input_stream.read(data);
				}
			} catch (IOException e) {
				m_logger.error(TAG + "[ERROR] Impossible to read " + length + " bytes from the UART port");
			}
		}
		return data;
	}
	
	public void close() 
	{
		if(m_port.isOpen() == true) {
			m_port.closePort();
		}
		m_logger.info(TAG + "UART port closed");
	}
	
	public int bytesAvailable() 
	{
		int bytes = 0;
		if(m_port.isOpen() == true) {
			bytes = m_port.bytesAvailable();
		}
		return bytes;
	}
	
	public byte[] readByte()
	{
		byte[] data = new byte[0];
		if(m_port.isOpen() == true) {
			try {
				if(m_input_stream.available() > 0) {
					data = new byte[1];
					data[0] = (byte)(m_input_stream.read() & 0xFF);
				}
			} catch (IOException e) {
				m_logger.error(TAG + "[ERROR] Impossible to read one byte from the UART port");
			}
		}
		return data;
	}
}
