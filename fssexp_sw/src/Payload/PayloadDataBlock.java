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
	
	/* Amateur Signal */
	public String amateur_id;
	
	/* Padding data */
	public byte[] padding_data; 
	
	public PayloadDataBlock()
	{
		exp_hk = new HousekeepingItem();
		stream = ByteBuffer.allocate(getSize());
		m_exp_hk_stream = ByteBuffer.allocate(HousekeepingItem.getSize());
		amateur_id = "operator: RAIU1MYV";
		padding_data = new byte[getPaddingSize()];
		for(int i = 0; i < padding_data.length; i ++) {
			padding_data[i] = (byte)(i & 0xFF);
		}
	}
	
	public void resetValues()
	{
		sat_id = -1;
		timestamp = -1;
		exp_hk.resetValues();
	}
	
	public static int getSize()
	{
		return Constants.data_mtu;
	}
	
	public static int getPaddingSize()
	{
		System.out.println(getUsefulSize());
		return Constants.data_mtu - getUsefulSize();
	}
	
	public static int getUsefulSize()
	{
		return ((Integer.SIZE / 8) + (Long.SIZE / 8) + HousekeepingItem.getSize() + 18);	/* amateur ID is 18 Bytes */ 
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
		s += timestamp + "::";
		s += exp_hk.toString();
		s += "::" + amateur_id;
		return s;
	}
}
