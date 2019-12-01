package Common;

public class SynchronizedBuffer {

	private byte[] m_buffer;
	private int m_read_pointer;
	private int m_write_pointer;
	private int m_size;
	private Log m_logger;
	private String TAG = "[SynchronizedBuffer] ";
	private String m_id = "";
	
	public SynchronizedBuffer(Log logger, String id) 
	{
		m_logger = logger;
		m_buffer = new byte[Constants.uart_max_buffer_size];
		m_size = 0;
		m_read_pointer = 0;
		m_write_pointer = 0;
		m_id = id;
		TAG += "[" + id + "]";
	}
	
	public synchronized int bytesAvailable() 
	{
		return m_size;
	}
	
	public int getMaxSize() 
	{
		return m_buffer.length;
	}
	
	public synchronized byte readByte() 
	{	
		if(m_size > 0) {
			byte container = m_buffer[m_read_pointer];
			m_read_pointer = (m_read_pointer + 1 ) % getMaxSize();
			m_size --;
			return container;
		} else {
			throw new IndexOutOfBoundsException(TAG + "Trying to read the buffer when it is empty");
		}
	}
	
	public synchronized int read(byte[] container) 
	{	
		int length; 
		
		if(m_size >= container.length) {
			length = container.length;
		} else if(m_size > 0) {
			length = m_size;
		} else {
			m_logger.warning(TAG + "Trying to read the buffer when it is empty");
			throw new IndexOutOfBoundsException(TAG + "Trying to read the buffer when it is empty");
		}
		
		if(m_read_pointer + length <= m_buffer.length) {
			System.arraycopy(m_buffer, m_read_pointer, container, 0, length);
			m_read_pointer += length;
			m_size -= length;
			return length;
		} else {
			int first = m_buffer.length - m_read_pointer;
			int second = length - first;
			System.arraycopy(m_buffer, m_read_pointer, container, 0, first);
			System.arraycopy(m_buffer, 0, container, first, second);
			m_read_pointer = second;
			m_size -= length;
			return length;
		}
	}
	
	public synchronized int write(byte[] data) 
	{
		int length;
		if(m_size >= m_buffer.length) {
			m_logger.warning(TAG + "Trying to write in a full buffer (i.e. Buffer overflow)");
			return 0;
		} else if(m_size + data.length <= m_buffer.length) {
			length = data.length;
		} else {
			length = m_buffer.length - m_size;
		}
		
		if(m_write_pointer + length <= m_buffer.length) {
			System.arraycopy(data, 0, m_buffer, m_write_pointer, length);
			m_write_pointer += length;
			m_size += length;
			//m_logger.debug(TAG + "RX Buffer size: " + m_size);
			return length;
		} else {
			int first = m_buffer.length - m_write_pointer;
			int second = length - first;
			System.arraycopy(data, 0, m_buffer, m_write_pointer, first);
			System.arraycopy(data, first, m_buffer, 0, second);
			m_write_pointer = second;
			m_size += length;
			//m_logger.debug(TAG + "RX Buffer size: " + m_size);
			return length;
		}
	}
	
	public synchronized void clear() 
	{
		m_size = 0;
		m_read_pointer = 0;
		m_write_pointer = 0;
	}
}
