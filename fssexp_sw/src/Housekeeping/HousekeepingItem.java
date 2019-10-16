package Housekeeping;

import Common.Constants;

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
	
	public String toString()
	{
		String str = "";
		/* Manager hk */
		str += timestamp + ",";
		str += exec_status + ",";
		str += payload_poll + ",";
		str += fss_poll + ",";
		str += service_poll + ",";
		str += rf_isl_poll + ",";
		/* Payload hk */
		str += payload_generated_items + ",";
		/* FSS Protocol */
		str += fss_status + ",";
		str += fss_role + ",";
		str += fss_tx + ",";
		str += fss_rx + ",";
		str += fss_err_rx + ",";
		/* Buffers */
		str += isl_buffer_size + ",";
		str += isl_buffer_drops + ",";
		/* RF ISL hk */
		str += rf_isl_hk.toString();
		/* EOL */
		str += "\n";
		return str;
	}
}
