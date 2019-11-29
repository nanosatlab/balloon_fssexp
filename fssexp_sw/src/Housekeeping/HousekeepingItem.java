package Housekeeping;

import java.nio.ByteBuffer;

public class HousekeepingItem 
{
	/* Manager hk */
	public long timestamp;
	public int exec_status;
	public int payload_poll;
	public int fss_poll;
	public int service_poll;
	public int rf_isl_poll;
	/* Payload hk */
	public int payload_generated_items;
	/* FSS Protocol */
	public int fss_status;
	public int fss_role;
	public int fss_tx;
	public int fss_rx;
	public int fss_err_rx;
	/* Buffers */
	public int isl_buffer_size;
	public int isl_buffer_drops;
	/* RF ISL hk */
	public RFISLHousekeepingItem rf_isl_hk;
	
	private ByteBuffer stream;
	private ByteBuffer m_rf_isl_hk_stream;
	
	public HousekeepingItem()
	{
		rf_isl_hk = new RFISLHousekeepingItem();
		stream = ByteBuffer.allocate(getSize());
		m_rf_isl_hk_stream = ByteBuffer.allocate(RFISLHousekeepingItem.getSize());
	}
	
	public int getSize()
	{
		return (8 + 4 * 13 + RFISLHousekeepingItem.getSize());
	}
	
	public byte[] getBytes()
	{
		stream.clear();
		stream.putLong(timestamp);
		stream.putInt(exec_status);
		stream.putInt(payload_poll);
		stream.putInt(fss_poll);
		stream.putInt(service_poll);
		stream.putInt(rf_isl_poll);
		stream.putInt(payload_generated_items);
		stream.putInt(fss_status);
		stream.putInt(fss_role);
		stream.putInt(fss_tx);
		stream.putInt(fss_rx);
		stream.putInt(fss_err_rx);
		stream.putInt(isl_buffer_size);
		stream.putInt(isl_buffer_drops);
		stream.put(rf_isl_hk.getBytes());
		stream.rewind();
		return stream.array();
	}
	
	public boolean fromBytes(byte[] data) 
	{
		boolean done = false;
		if(data.length == stream.capacity()) {
			stream.clear();
			stream.put(data);
			stream.rewind();
			timestamp = stream.getLong();
			exec_status = stream.getInt();
			payload_poll = stream.getInt();
			fss_poll = stream.getInt();
			service_poll = stream.getInt();
			rf_isl_poll = stream.getInt();
			payload_generated_items = stream.getInt();
			fss_status = stream.getInt();
			fss_role = stream.getInt();
			fss_tx = stream.getInt();
			fss_rx = stream.getInt();
			fss_err_rx = stream.getInt();
			isl_buffer_size = stream.getInt();
			isl_buffer_drops = stream.getInt();
			m_rf_isl_hk_stream.clear();
			stream.get(m_rf_isl_hk_stream.array());
			m_rf_isl_hk_stream.rewind();
			if(rf_isl_hk.fromBytes(m_rf_isl_hk_stream.array()) == true) {
				done = true;
			}
		}
		return done;
	}
	
	public String toString()
	{
		String str = "";
		/* Manager hk */
		str += timestamp + ",";
		str += exec_status + ",";
		str += payload_poll + ",";
		str += fss_poll + ",";
		str += service_poll + ",";
		str += rf_isl_poll + "::";
		/* Payload hk */
		str += payload_generated_items + "::";
		/* FSS Protocol */
		str += fss_status + ",";
		str += fss_role + ",";
		str += fss_tx + ",";
		str += fss_rx + ",";
		str += fss_err_rx + "::";
		/* Buffers */
		str += isl_buffer_size + ",";
		str += isl_buffer_drops + "::";
		/* RF ISL hk */
		str += rf_isl_hk.toString();
		/* EOL */
		str += "\n";
		return str;
	}
}
