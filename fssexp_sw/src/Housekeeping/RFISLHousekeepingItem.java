package Housekeeping;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RFISLHousekeepingItem {
	
	private int boot_count;
	private double current_rssi;
	private double last_rx_rssi;
	private double snr;
	private double last_rx_lqi;
	private double tx_power;
	private int phy_tx_packets;
	private int phy_tx_err_packets;
	private int phy_rx_packets;
	private int phy_rx_err_packets;
	private int ll_tx_packets;
	private int ll_rx_packets;
	private double ext_temperature;
	private double int_temperature;
	private float frequency;
	private int rx_queue;
	private int tx_queue;
	private ByteBuffer stream;
	
	
	public RFISLHousekeepingItem()
	{
		stream = ByteBuffer.allocate(getSize());
	}
	
	public int getSize()
	{
		return (4 * 9 + 8 * 7 + 4);
	}
	
	private double computeRSSI(int rssi_dec)
	{
		double value;
	    if (rssi_dec < 128) {
	    	value = (rssi_dec / 2.0) - 74.0;
	    } else {
	    	value = ((rssi_dec - 256) / 2.0) - 74.0;
	    }
	    return value;
	}
	
	private double computeLqi(int lqi)
	{
	    return (1.0 - lqi / 127.0) * 100.0;
	}
	
	private double computeSnr(double last_rssi, double actual_rssi)
	{
	    return last_rssi - actual_rssi;
	}
	
	private double fromU16ToDouble(int value)
	{
		byte lsb = (byte)((value >> 8) & 0xFF);
	    byte msb = (byte)(value & 0xFF);
	    double result = 0;
	    if (msb < 0) {
	    	result = (1.0 * msb - 1.0 / 256.0 * lsb);
	    }else {
	    	result = (1.0 * msb + 1.0 / 256.0 * lsb);
	    }
	    return result;
	}
	
	public void parseFromBytes(byte[] data) {
		boot_count = data[0] & 0xFF;
		current_rssi = computeRSSI(data[1] & 0xFF);
		last_rx_rssi = computeRSSI(data[2] & 0xFF);
		snr = computeSnr(last_rx_rssi, current_rssi);
		last_rx_lqi = computeLqi(data[3] & 0xFF);
		tx_power = data[4] & 0xFF;
		phy_tx_packets = (((data[8] << 24) | (data[7] << 16) | (data[6] << 8) | (data[5] & 0xFF)) & 0xFFFFFFFF);
		phy_tx_err_packets = (((data[22] << 8) | (data[21] & 0xFF)) & 0xFFFF);
		phy_rx_packets = (((data[12] << 24) | (data[11] << 16) | (data[10] << 8) | (data[9] & 0xFF)) & 0xFFFFFFFF);
		phy_rx_err_packets = (((data[24] << 8) | (data[23] & 0xFF)) & 0xFFFF);
		ll_tx_packets = (((data[16] << 24) | (data[15] << 16) | (data[14] << 8) | (data[13] & 0xFF)) & 0xFFFFFFFF);
		ll_rx_packets = (((data[20] << 24) | (data[19] << 16) | (data[18] << 8) | (data[17] & 0xFF)) & 0xFFFFFFFF);
		ext_temperature = fromU16ToDouble(((data[25] << 8) | (data[26] & 0xFF)) & 0xFFFF);
		int_temperature = fromU16ToDouble(((data[27] << 8) | (data[28] & 0xFF)) & 0xFFFF);
		byte[] freq_bytes = new byte[4];
		freq_bytes[3] = data[29];
		freq_bytes[2] = data[30];
		freq_bytes[1] = data[31];
		freq_bytes[0] = data[32];
		frequency = ByteBuffer.wrap(freq_bytes).order(ByteOrder.BIG_ENDIAN).getFloat();
		rx_queue = data[33] & 0xFF;
		tx_queue = data[34] & 0xFF;
	}
	
	public byte[] getBytes()
	{
		stream.clear();
		stream.putInt(boot_count);
		stream.putDouble(current_rssi);
		stream.putDouble(last_rx_rssi);
		stream.putDouble(snr);
		stream.putDouble(last_rx_lqi);
		stream.putDouble(tx_power);
		stream.putInt(phy_tx_packets);
		stream.putInt(phy_tx_err_packets);
		stream.putInt(phy_rx_packets);
		stream.putInt(phy_rx_err_packets);
		stream.putInt(ll_tx_packets);
		stream.putInt(ll_rx_packets);
		stream.putDouble(ext_temperature);
		stream.putDouble(int_temperature);
		stream.putFloat(frequency);
		stream.putInt(rx_queue);
		stream.putInt(tx_queue);
		stream.rewind();
		return stream.array();
	}
	
	public String toString()
	{
		String str = "";
		str += boot_count + ",";
		str += current_rssi + ",";
		str += last_rx_rssi + ",";
		str += snr + ",";
		str += last_rx_lqi + ",";
		str += tx_power + ",";
		str += phy_tx_packets + ",";
		str += phy_tx_err_packets + ",";
		str += phy_rx_packets + ",";
		str += phy_rx_err_packets + ",";
		str += ll_tx_packets + ",";
		str += ll_rx_packets + ",";
		str += ext_temperature + ",";
		str += int_temperature + ",";
		str += frequency + ",";
		str += rx_queue + ",";
		str += tx_queue;
		return str;
	}
}
