package Common;

/* With Golbriak computer */

import space.golbriak.lang.System;


public class TimeUtils {

	private boolean m_already_set;
	
	public TimeUtils() {
		m_already_set = false;
	}
	
	public void setTimeMillis(long time) {
		if(m_already_set == false) {
			/* With Golbriak computer */
			
			System.setCurrentTimeMillis(time);
		    
			m_already_set = true;
		}
	}
	
	public long getTimeMillis() {
		return System.currentTimeMillis();
	}
	
}
