package Payload;

import java.nio.ByteBuffer;

import Common.Constants;
import Housekeeping.HousekeepingItem;


public class PayloadDataBlock {

	private ByteBuffer stream;
	private ByteBuffer m_exp_hk_stream;
	
	/* Header of PayloadDataBlock */
	public int sat_id;
	public long timestamp;
	
	/* NanoPi hk */
	// TODO:
	
	/* Experiment hk + RF ISL hk */
	public HousekeepingItem exp_hk;
	
	/* Padding data */
	public byte[] padding_data; 
	
	public PayloadDataBlock()
	{
		exp_hk = new HousekeepingItem();
		stream = ByteBuffer.allocate(getSize());
		m_exp_hk_stream = ByteBuffer.allocate(exp_hk.getSize());
		padding_data = new byte[getPaddingSize()];
		for(int i = 0; i < padding_data.length; i ++) {
			padding_data[i] = (byte)(i & 0xFF);
		}
	}
	
	public int getSize()
	{
		return Constants.data_mtu;
	}
	
	public int getPaddingSize()
	{
		return getSize() - getUsefulSize();
	}
	
	public int getUsefulSize()
	{
		return (4 + 8 + exp_hk.getSize());
	}
	
	public byte[] getBytes()
	{
		stream.clear();
		stream.putInt(sat_id);
		stream.putLong(timestamp);
		stream.put(exp_hk.getBytes());
		stream.put(padding_data);
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
			sat_id = stream.getInt();
			timestamp = stream.getLong();
			m_exp_hk_stream.clear();
			stream.get(m_exp_hk_stream.array());
			m_exp_hk_stream.rewind();
			if(exp_hk.fromBytes(m_exp_hk_stream.array()) == true) {
				done = true;
			}
		}
		return done;
	}

	public String toString()
	{
		String s = "";
		s += sat_id + ",";
		s += timestamp + ",";
		s += exp_hk.toString();
		return s;
	}
}
