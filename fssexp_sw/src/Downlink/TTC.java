package Downlink;

import java.lang.Thread;
import java.util.Random;

import Configuration.ExperimentConf;
import Common.TimeUtils;
import Common.Constants;
import Common.Log;

public class TTC extends Thread {

	private double cntct_min_period;
	private double cntct_max_period;
	private double cntct_max_duration;
	private double cntct_min_duration;
	private boolean m_in_contact;
	private boolean m_exit;
	private TimeUtils m_time;
	private Random m_rand;
	private Log m_logger;
	
	public TTC(TimeUtils time, ExperimentConf conf, Log logger) {
		cntct_max_period = conf.cntct_max_period;
		cntct_min_period = conf.cntct_min_period;
		cntct_max_duration = conf.cntct_max_duration;
		cntct_min_duration = conf.cntct_min_duration;
		m_time = time;
		m_rand = new Random(m_time.getTimeMillis());
		m_in_contact = false;
		m_exit = false;
		m_logger = logger;
	}
	
	public void run() {
		
		double cntct_start = 0;
		double cntct_end = 0;
		
		while(m_exit == false) {
			
			if(m_time.getTime() >= cntct_end) {
				m_in_contact = false;
				/* Compute when the next downlink contact will start */
				cntct_start = m_rand.nextDouble() * cntct_max_period; 
				if(cntct_start < cntct_min_period) {
					cntct_start = cntct_min_period;
				}
				cntct_start += m_time.getTime();
				/* Compute when the next downlink contact will stop */
				cntct_end = m_rand.nextDouble() * cntct_max_duration;
				if(cntct_end < cntct_min_duration) {
					cntct_end = cntct_min_duration;
				}
				cntct_end += cntct_start;
			}
			
			if(m_in_contact == false && m_time.getTime() >= cntct_start) {
				m_in_contact = true;
			}
			
			/* Sleep until waking up again */
			try {
				Thread.sleep(Constants.dwn_contact_sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public boolean isContact() {
		return m_in_contact;
	}
	
	public void controlledStop() {
		m_exit = true;
	}
}
