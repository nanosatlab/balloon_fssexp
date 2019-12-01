
/* Own package */
package InterSatelliteCommunications;

/* External imports */
import java.nio.ByteBuffer;

/* Internal imports */
import Common.Constants;
import Common.Log;
import Common.SynchronizedBuffer;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Downlink.TTC;
import IPCStack.PacketDispatcher;
import Storage.PayloadBuffer;
import Storage.PacketExchangeBuffer;

public class FSSProtocol extends Thread {

	private Packet m_tx_packet;
	private Packet m_rx_packet;
	
	private PacketExchangeBuffer m_hk_packets;

	private PayloadBuffer m_fss_buffer;
	private TTC m_ttc;
	private boolean m_running;
	private boolean m_poll_token;
	private int m_state;
	private int m_tx_num;
	private int m_rx_num;
	private int m_rx_err_num;
	private int m_role;

	private int m_buffer_thr;
	private int m_fss_interest;
	private int m_sat_source;
	private int m_sat_destination;

	private boolean m_received_packet;
	private int m_publisher_capacity;
	private int m_service_type;
	private int m_service_type_interest;
	private boolean m_waiting_packet;
	private int m_waiting_type;
	private int m_waiting_counter;
	private int m_waiting_max;
	private int m_waiting_timeout;
	private long m_waiting_next;

	private long m_download_start;
	private long m_download_end;
	private boolean m_mydata_downloaded;
	private boolean m_download_contact;

	private int m_publishing_period;
	private long m_publishing_next;

	private long m_start_time;
	private long m_end_time;
	private int m_download_rate;

	private byte[] m_empty_data;
	private byte[] m_previous_data;
	private ExperimentConf m_conf;

	private PacketDispatcher m_dispatcher;
	private SynchronizedBuffer m_isc_buffer;
	private byte[] m_ipc_header_stream;
	private byte[] m_ipc_checksum_stream;
	private int m_prot_num;
	
	private Log m_logger;
	private TimeUtils m_time;

	private final static String TAG = "[FSSProtocol] ";

	public FSSProtocol(Log log, PayloadBuffer buffer, PacketExchangeBuffer hk_packets, ExperimentConf conf,
			TimeUtils timer, PacketDispatcher dispatcher, TTC ttc) {
		super();

		m_logger = log;
		m_conf = conf;
		m_time = timer;
		m_ttc = ttc;
		
		/* Init dispatcher */
		m_dispatcher = dispatcher;
		m_isc_buffer = new SynchronizedBuffer(m_logger, "fssprotocol-rx-buffer");
		m_prot_num = Constants.fss_prot_num;
		m_dispatcher.addProtocolBuffer(m_prot_num, m_isc_buffer);

		/* Packet containers */
		m_tx_packet = new Packet();
		m_rx_packet = new Packet();

		m_tx_num = 0;
		m_rx_num = 0;
		m_rx_err_num = 0;

		m_hk_packets = hk_packets;

		m_fss_buffer = buffer;
		m_received_packet = false;
		m_waiting_packet = false;
		m_waiting_type = -1;
		m_waiting_counter = 0;
		m_waiting_next = 0;

		m_publisher_capacity = -1;
		m_start_time = 0;
		m_end_time = 0;

		m_empty_data = new byte[0];

		m_poll_token = false;
		m_state = Constants.FSS_NOT_STARTED;
		m_role = Constants.FSS_ROLE_NOT_DEFINED;

		m_publishing_period = 5000;
		m_publishing_next = 0;
		m_service_type = Constants.FSS_SERVICE_TYPE_NOT_DEFINED;
		m_service_type_interest = Constants.FSS_SERVICE_TYPE_DOWNLOAD;
		m_mydata_downloaded = false;
		m_download_contact = false;
	}

	public void setConfiguration() 
	{
		m_buffer_thr = m_conf.fss_buffer_thr;
		m_fss_interest = m_conf.fss_interest;
		m_sat_destination = Constants.FSS_BROADCAST_ADDR;
		m_waiting_timeout = m_conf.fss_timeout;
		m_waiting_max = m_conf.fss_retries;
		m_sat_source = m_conf.satellite_id;
		m_download_rate = m_conf.download_rate;
		if (m_conf.download_experiment_activated == true) {
			m_service_type_interest = Constants.FSS_SERVICE_TYPE_DOWNLOAD;
		} else {
			m_service_type_interest = Constants.FSS_SERVICE_TYPE_STORAGE;
		}
	}

	@Override
	public void run() {
		
		m_running = true;
		long time_tick;
		long spent_time;
		long time_to_sleep;
		
		/* Upload Configuration first */
		setConfiguration();

		/* Initialize parameters */
		m_logger.info(TAG + "Started Thread");
		accessToState(true, Constants.FSS_STANDBY);

		// m_start_time = space.golbriak.lang.System.currentTimeMillis();
		m_start_time = m_time.getTimeMillis();

		m_download_start = m_start_time + m_conf.download_start;
		m_download_end = m_start_time + m_conf.download_end;
		if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_STORAGE) {
			m_logger.info(TAG + "The STORAGE EXPERIMENT is selected");
		} else if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD) {
			m_logger.info(TAG + "The DOWNLOAD EXPERIMENT is selected");
			m_logger.info(TAG + "The download starts at " + m_download_start);
			m_logger.info(TAG + "The download ends at " + m_download_end);
		}

		/* main loop */
		while (m_running == true) {
			
			/* Poll myself */
			polling(true);

			/* Get current time to compute the rate */
			time_tick = m_time.getTimeMillis(); /* Time in milliseconds */

			/*
			 * In case of download experiment, verify that the download slot has started or
			 * finished
			 */
			//if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD && m_download_start != m_download_end) {
			//	
			//	if (m_time.getTimeMillis() >= m_download_start && m_time.getTimeMillis() < m_download_end
			//		&& m_download_contact == false) {
			//		m_logger.info(TAG + "Download contact has started");
			//		m_download_contact = true;
			//	} else if (m_time.getTimeMillis() >= m_download_end && m_download_contact == true) {
			//		m_logger.info(TAG + "Download contact has finished");
			//		m_download_contact = false;
			//	}
			//}
				
			/* First verify if there is a packet in the interface */
			if(receivePacket() == true) {
				accessToRXs(true, 1, 1);
				m_received_packet = true;
			} else {
				m_received_packet = false;
			}

			/* Execute depending on the mode */
			switch(getFederationStatus()) {
			case Constants.FSS_STANDBY:
				standbyPhase(m_received_packet);
				break;
			case Constants.FSS_NEGOTIATION:
				negotiationPhase(m_received_packet);
				break;
			case Constants.FSS_EXCHANGE:
				federationPhase(m_received_packet);
				break;
			case Constants.FSS_CLOSURE:
				closurePhase(m_received_packet);
				break;
			}
			
			/* Sleep */
			spent_time = m_time.getTimeMillis() - time_tick;
			if (spent_time < Constants.fss_prot_sleep) {
				try {
					time_to_sleep = Constants.fss_prot_sleep - spent_time;
					if(time_to_sleep <= Constants.fss_prot_sleep && time_to_sleep > 0) {
						sleep(time_to_sleep);
					} else {
						m_logger.error(TAG + "The time to sleep is different than the expected: " + time_to_sleep);
					}
				} catch (InterruptedException e) {
					m_logger.error(e);
					/* This error is important, it is better to stop the experiment */
					m_running = false;
				}
			} else {
				try {
					m_logger.info(TAG + "No sleep, the process consumed " + spent_time);
					sleep(0);
				} catch(InterruptedException e) {
					m_logger.error(e);
				}
			}
		}

		accessToState(true, Constants.FSS_NOT_STARTED);
		m_end_time = m_time.getTimeMillis();
		m_logger.info(TAG + "Stopped Thread");
	}

	private void retransmitPacket() {
		/* Retransmit */
		m_logger.info(TAG + "No replication - Retransmit Packet type " + m_tx_packet.type);
		Packet temp_packet = new Packet(m_tx_packet);
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, temp_packet.type, temp_packet.destination);
		m_tx_packet.length = temp_packet.length;
		m_tx_packet.setData(temp_packet.getData());
		sendPacket();
	}

	private void lockForPacket(int packet_type) {
		m_waiting_packet = true;
		m_waiting_type = packet_type;
		// m_waiting_next = space.golbriak.lang.System.currentTimeMillis() +
		// m_waiting_timeout;
		m_waiting_next = m_time.getTimeMillis() + m_waiting_timeout;
		m_logger.info(TAG + "Retransmission of packet " + packet_type + " set at " + m_waiting_next + "(backoff: "
				+ m_waiting_timeout + ")");
	}

	private void releaseForPacket() 
	{
		m_waiting_packet = false;
		m_waiting_counter = 0;
	}

	private void verifyLinkDead() {
		if (m_waiting_counter > m_waiting_max) {
			m_logger.warning(
					TAG + "Maximum reply of Packet type " + m_tx_packet.type + " reached. The link is dead!");
			m_running = false;
		} else {
			m_waiting_counter++;
			retransmitPacket();
			// m_waiting_next = space.golbriak.lang.System.currentTimeMillis() +
			// m_waiting_timeout;
			m_waiting_next = m_time.getTimeMillis() + m_waiting_timeout;

			m_logger.info(TAG + "No replication - Retransmitted packet " + m_tx_packet.type + "; next at "
					+ m_waiting_next);
		}
	}

	private void sendPUBLISHPacket(int service_type) {
		ByteBuffer data = ByteBuffer.allocate(Integer.SIZE * 2 / 8);
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_SERVICE_PUBLISH, Constants.FSS_BROADCAST_ADDR);
		data.putInt(service_type);
		data.putInt(m_buffer_thr - m_fss_buffer.getSize());
		m_tx_packet.length = Integer.SIZE * 2 / 8;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(data.array());
		sendPacket();
	}

	private void sendREQUEST(int service_type) {
		ByteBuffer data = ByteBuffer.allocate(Integer.SIZE / 8);
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_SERVICE_REQUEST, m_sat_destination);
		data.putInt(service_type);
		m_tx_packet.length = Integer.SIZE / 8;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(data.array());
		sendPacket();
	}

	private void sendACCEPT(int service_type) {
		ByteBuffer data = ByteBuffer.allocate(Integer.SIZE * 2 / 8);
		m_tx_packet.resetValues();
		m_sat_destination = m_rx_packet.source;
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_SERVICE_ACCEPT, m_sat_destination);
		data.putInt(service_type);
		data.putInt(m_buffer_thr - m_fss_buffer.getSize());
		m_tx_packet.length = Integer.SIZE * 2 / 8;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(data.array());
		sendPacket();
	}

	private void sendDATAPacket() {
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_DATA, m_sat_destination);
		m_previous_data = m_fss_buffer.getBottomData();
		m_tx_packet.length = m_previous_data.length;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(m_previous_data);
		sendPacket();
	}

	private void sendDATAACKPacket() {
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_DATA_ACK, m_sat_destination);
		ByteBuffer data;
		data = ByteBuffer.allocate(Integer.SIZE / 8);
		data.putInt(m_buffer_thr - m_fss_buffer.getSize());
		m_tx_packet.length = Integer.SIZE / 8;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(data.array());
		sendPacket();
	}

	private void sendCLOSEPacket() {
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_CLOSE, m_sat_destination);
		m_tx_packet.length = 0;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(m_empty_data);
		sendPacket();
	}

	private void sendCLOSEACKPacket() {
		m_tx_packet.resetValues();
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_CLOSE_ACK, m_sat_destination);
		m_tx_packet.length = 0;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(m_empty_data);
		sendPacket();
	}

	private void sendCLOSEDATAACKPacket() {
		m_tx_packet.resetValues();
		m_sat_destination = m_rx_packet.source;
		setHeaderPacket(m_tx_packet, Constants.PACKET_FSS_CLOSE_DATA_ACK, m_sat_destination);
		m_tx_packet.length = 0;
		m_tx_packet.prot_num = m_prot_num;
		m_tx_packet.setData(m_empty_data);
		sendPacket();
	}

	private void standbyPhase(boolean isRX) 
	{
		/* Verify if my service is available */
		if(m_ttc.accessToServiceAvailable(false, false) == true) {
			/* Publish this service */
			if(m_time.getTimeMillis() >= m_publishing_next || m_publishing_next == 0) {
				/* send PUBLISH */
				sendPUBLISHPacket(Constants.FSS_SERVICE_TYPE_DOWNLOAD);
				m_logger.info(TAG + "STANDBY PHASE: PUBLISH Packet sent with service "
						+ Constants.FSS_SERVICE_TYPE_DOWNLOAD);
				m_publishing_next = m_time.getTimeMillis() + m_publishing_period;
			}
		}
		
		/* Check what has been received */
		if(isRX == true) {
			switch(m_rx_packet.type) {
			case Constants.PACKET_FSS_SERVICE_PUBLISH:
				/* Process packet */
				ByteBuffer publish_data = ByteBuffer.wrap(m_rx_packet.getData());
				m_service_type = publish_data.getInt();
				m_publisher_capacity = publish_data.getInt();
				m_sat_destination = m_rx_packet.source;
				m_logger.info(TAG + "STANDBY PHASE: Received PUBLISH Packet with service " + m_service_type);
				if(m_ttc.accessToServiceAvailable(false, false) == false
					&& m_publisher_capacity > 0
					&& m_fss_buffer.getSize() > 0) {
					/* I need and I want the service of storage - send REQUEST */
					sendREQUEST(m_service_type_interest);
					m_logger.info(TAG + "STANDBY PHASE: REQUEST Packet sent with service "
							+ m_service_type_interest);
					/* Wait ACCEPT packet to be received */
					lockForPacket(Constants.PACKET_FSS_SERVICE_ACCEPT);
					/* Change to negotiation phase */
					accessToState(true, Constants.FSS_NEGOTIATION);
				}
				break;
			case Constants.PACKET_FSS_SERVICE_REQUEST:
				/* Check if I have a service to share */
				if(m_ttc.accessToServiceAvailable(false, false) == true) {
					m_logger.info(TAG + "NEGOTIATION PHASE: Received REQUEST Packet with service " + m_service_type_interest);
					/* Send ACCEPT because I have the service and I am interested */
					sendACCEPT(m_service_type_interest);
					m_logger.info(TAG + "NEGOTIATION PHASE: ACCEPT Packet sent with service "
							+ m_service_type_interest);
					/* Wait first DATA to confirm the correct reception */
					lockForPacket(Constants.PACKET_FSS_DATA | Constants.PACKET_FSS_CLOSE);
					/* Change to negotiation phase */
					accessToState(true, Constants.FSS_NEGOTIATION);
				}
				break;
			}
		}
	}
	
	private void negotiationPhase(boolean isRX) {

		ByteBuffer data;
		int service_type;

		if (m_fss_interest == Constants.FSS_INTEREST) {
			/* Process if a packet has been received */
			if (isRX == true) {
				if (m_waiting_packet == true && m_waiting_type == m_rx_packet.type) {
					/*
					 * I have received the packet that I was waiting - I can release and keep
					 * working
					 */
					releaseForPacket();
				}

				switch (m_rx_packet.type) {
				case Constants.PACKET_FSS_SERVICE_PUBLISH:
					ByteBuffer publish_data = ByteBuffer.wrap(m_rx_packet.getData());
					m_service_type = publish_data.getInt();
					m_publisher_capacity = publish_data.getInt();
					m_sat_destination = m_rx_packet.source;
					m_logger.info(TAG + "NEGOTIATION PHASE: Received PUBLISH Packet with service " + m_service_type);
					break;
				case Constants.PACKET_FSS_SERVICE_REQUEST:
					data = ByteBuffer.wrap(m_rx_packet.getData());
					service_type = data.getInt();
					data.clear();
					m_logger.info(TAG + "NEGOTIATION PHASE: Received REQUEST Packet with service " + service_type);
					if (service_type == m_service_type_interest) {
						if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_STORAGE) {
							if (m_fss_buffer.getSize() < m_buffer_thr) {
								/* Send ACCEPT because I have the service and I am interested */
								sendACCEPT(m_service_type_interest);
								m_logger.info(TAG + "NEGOTIATION PHASE: ACCEPT Packet sent with service "
										+ m_service_type_interest);
								/* Wait first DATA to confirm the correct reception */
								lockForPacket(Constants.PACKET_FSS_DATA | Constants.PACKET_FSS_CLOSE);
							}
						} else if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD) {
							if (m_fss_buffer.getSize() < m_buffer_thr && m_download_contact == true
									&& m_mydata_downloaded == true) {
								/* Send ACCEPT because I have the service and I am interested */
								sendACCEPT(m_service_type_interest);
								m_logger.info(TAG + "NEGOTIATION PHASE: ACCEPT Packet sent with service "
										+ m_service_type_interest);
								/* Wait first DATA to confirm the correct reception */
								lockForPacket(Constants.PACKET_FSS_DATA | Constants.PACKET_FSS_CLOSE);
							}
						}
					}
					break;
				case Constants.PACKET_FSS_SERVICE_ACCEPT:
					data = ByteBuffer.wrap(m_rx_packet.getData());
					service_type = data.getInt();
					data.clear();
					m_logger.info(TAG + "NEGOTIATION PHASE: Received ACCEPT Packet with service " + service_type);
					if (service_type == m_service_type_interest) {
						m_role = Constants.FSS_ROLE_CUSTOMER;
						accessToState(true, Constants.FSS_EXCHANGE);
						m_logger.info(TAG + "I am the CUSTOMER - Transit to FEDERATION PHASE");
						/*
						 * In order to not delay so much the DATA transmission, it is sent directly from
						 * here. Although it is part of the federationPhase function, it has been
						 * considered to do it here to clarify the code. The next iteration will be
						 * performed with the federationPhase function.
						 */
						sendDATAPacket();
						m_logger.info(TAG + "FEDERATION PHASE: DATA Packet sent");
						lockForPacket(Constants.PACKET_FSS_DATA_ACK | Constants.PACKET_FSS_CLOSE
								| Constants.PACKET_FSS_CLOSE_DATA_ACK);
					} else {
						lockForPacket(Constants.PACKET_FSS_SERVICE_ACCEPT);
					}
					break;
				case Constants.PACKET_FSS_DATA:
					m_logger.info(TAG + "NEGOTIATION PHASE: Received DATA Packet");
					m_role = Constants.FSS_ROLE_SUPPLIER;
					accessToState(true, Constants.FSS_EXCHANGE);
					m_logger.info(TAG + "I am the SUPPLIER - Transit to FEDERATION PHASE");
					/*
					 * In order to not delay so much the DATA ACK transmission, the execution of the
					 * federationPhase function is performed. As the input is true (packet
					 * received), it will directly send a DATA ACK packet. Note that after this, it
					 * will expect the DATA, then it will not enter in the condition bellow
					 * (m_waiting_packet = true). After this, in the next iteration, the
					 * federationPhase will be directly executed.
					 */
					federationPhase(true);
					break;
				}
			} else if (m_waiting_packet == true && m_time.getTimeMillis() >= m_waiting_next) {
				/*
				 * No reply has been received, and the timeout has passed - retransmit or exit
				 */
				verifyLinkDead();
			}

			/* If not waiting a packet, I will do other things */
			if (m_waiting_packet == false) {
				if (getFederationStatus() != Constants.FSS_EXCHANGE) {
					if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_STORAGE) {
						/* Do I have to publish? */
						if (m_fss_buffer.getSize() < m_buffer_thr
								// && (space.golbriak.lang.System.currentTimeMillis() >= m_publishing_next ||
								// m_publishing_next == 0)) {
								&& (m_time.getTimeMillis() >= m_publishing_next || m_publishing_next == 0)) {

							/* Send PUBLISH packet */
							sendPUBLISHPacket(Constants.FSS_SERVICE_TYPE_STORAGE);
							m_logger.info(TAG + "NEGOTIATION PHASE: PUBLISH Packet sent with service "
									+ Constants.FSS_SERVICE_TYPE_STORAGE);
							m_publishing_next = m_time.getTimeMillis() + m_publishing_period;

						} else if (m_service_type_interest == m_service_type && m_publisher_capacity > 0
								&& m_fss_buffer.getSize() >= m_buffer_thr) {

							/* I need and I want the service of storage - send REQUEST */
							sendREQUEST(m_service_type_interest);
							m_logger.info(TAG + "NEGOTIATION PHASE: REQUEST Packet sent with service "
									+ m_service_type_interest);

							/* Wait ACCEPT packet to be received */
							lockForPacket(Constants.PACKET_FSS_SERVICE_ACCEPT);
						}

					} else if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD) {

						/* If first time, download all my data */
						if (m_mydata_downloaded == false && m_download_contact == true) {
							m_logger.info(
									TAG + "First, download my data: " + m_fss_buffer.getSize() + " packets remaining");

							/*
							 * There is data to be download, then download following the corresponding rate
							 */
							int packet_count = 0;
							while (m_fss_buffer.getSize() > 0 && packet_count < m_download_rate) {
								m_fss_buffer.deleteBottomData();
								packet_count++;
							}

							if (m_fss_buffer.getSize() == 0) {
								m_logger.info(TAG + "All my data has been download");
								m_mydata_downloaded = true;
							}
						}

						/*
						 * when I have download all my data, I have to perform the federation protocol
						 */
						if (m_mydata_downloaded == true && m_download_contact == true
								&& (m_time.getTimeMillis() >= m_publishing_next || m_publishing_next == 0)) {
							/*
							 * To publish, the download shall be available, sand data to download - send
							 * PUBLISH
							 */
							sendPUBLISHPacket(Constants.FSS_SERVICE_TYPE_DOWNLOAD);
							m_logger.info(TAG + "NEGOTIATION PHASE: PUBLISH Packet sent with service "
									+ Constants.FSS_SERVICE_TYPE_DOWNLOAD);
							m_publishing_next = m_time.getTimeMillis() + m_publishing_period;
						}

						/* I should evaluate if I want the published service */
						if (m_mydata_downloaded == false && m_download_contact == false
								&& m_service_type_interest == m_service_type && m_publisher_capacity > 0
								&& m_fss_buffer.getSize() > 0) {
							/* I want the download capacity - send REQUEST */
							sendREQUEST(m_service_type_interest);
							m_logger.info(TAG + "NEGOTIATION PHASE: REQUEST Packet sent with service "
									+ m_service_type_interest);
							/* Wait Request */
							lockForPacket(Constants.PACKET_FSS_SERVICE_ACCEPT);
						}
					}
				}
			}
		}

	}

	private void federationPhase(boolean isRX) {

		if (getFederationRole() == Constants.FSS_ROLE_CUSTOMER) {

			if (isRX == true) {

				m_logger.info(TAG + "Something received in the CUSTOMER");

				if (m_waiting_packet == true && (m_waiting_type & m_rx_packet.type) != 0) {
					/*
					 * I have received the packet that I was waiting - I can release and keep
					 * working
					 */
					releaseForPacket();
				}

				switch (m_rx_packet.type) {

				case Constants.PACKET_FSS_CLOSE:
					m_logger.info(TAG + "FEDERATION PHASE: Received CLOSE Packet");
					accessToState(true, Constants.FSS_CLOSURE);
					sendCLOSEACKPacket();
					m_logger.info(TAG + "FEDERATION PHASE: CLOSE ACK Packet sent");
					lockForPacket(Constants.PACKET_FSS_CLOSE_ACK); /* Wait the CLOSE ACK */
					/* Next iteration will be the closurePhase function */
					break;

				case Constants.PACKET_FSS_DATA_ACK:
					m_logger.info(TAG + "FEDERATION PHASE: Received DATA ACK Packet");
					/*
					 * Remove the packet from the queue, because it has been correctly acknowledged.
					 */
					m_fss_buffer.deleteBottomData();

					/* Verify if I can keep sending, or if I have sent everything */
					if (m_fss_buffer.getSize() == 0) {
						accessToState(true, Constants.FSS_CLOSURE);
						sendCLOSEPacket();
						m_logger.info(TAG + "FEDERATION PHASE: CLOSE Packet sent");
						lockForPacket(Constants.PACKET_FSS_CLOSE_ACK); /* Wait the CLOSE ACK */
					} else {
						sendDATAPacket();
						m_logger.info(TAG + "FEDERATION PHASE: DATA Packet sent");
						lockForPacket(Constants.PACKET_FSS_DATA_ACK | Constants.PACKET_FSS_CLOSE
								| Constants.PACKET_FSS_CLOSE_DATA_ACK);
					}
					break;

				case Constants.PACKET_FSS_CLOSE_DATA_ACK:
					m_logger.info(TAG + "FEDERATION PHASE: Received CLOSE DATA ACK Packet");
					/*
					 * Remove the packet from the queue, because it has been correctly acknowledged.
					 */
					m_fss_buffer.deleteBottomData();
					accessToState(true, Constants.FSS_CLOSURE);
					sendCLOSEACKPacket();
					m_logger.info(TAG + "FEDERATION PHASE: CLOSE ACK Packet sent");
					lockForPacket(Constants.PACKET_FSS_CLOSE_ACK); /* Wait the CLOSE ACK */
					/* Next iteration will be the closurePhase function */
					break;
				}

				// } else if(m_waiting_packet == true &&
				// space.golbriak.lang.System.currentTimeMillis() >= m_waiting_next) {
			} else if (m_waiting_packet == true && m_time.getTimeMillis() >= m_waiting_next) {
				/*
				 * No reply has been received, and the timeout has passed - retransmit or exit
				 */
				verifyLinkDead();
			}

		} else if (getFederationRole() == Constants.FSS_ROLE_SUPPLIER) {

			if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD && m_download_contact == true
					&& m_fss_buffer.getSize() > 0) {
				/*
				 * Try to download packets. In this implementation, a packet is download when it
				 * is removed from the buffer. Note that this deletion is just an illusion, all
				 * the packets are stored in a dedicated file. Note that after download, a DATA
				 * ACK is sent to confirm the correct transference.
				 */
				m_fss_buffer.deleteBottomData();
				m_logger.info("FEDERATION PHASE: Packet downloaded");
				sendDATAACKPacket();
				m_logger.info(TAG + "FEDERATION PHASE: DATA ACK Packet sent");
				lockForPacket(Constants.PACKET_FSS_DATA);
			}

			if (isRX == true) {

				m_logger.info(TAG + "Something received in the SUPPLIER");

				if(m_waiting_packet == true && (m_waiting_type & m_rx_packet.type) != 0) {
					/*
					 * I have received the packet that I was waiting - I can release and keep
					 * working
					 */
					releaseForPacket();
				}

				switch (m_rx_packet.type) {

				case Constants.PACKET_FSS_CLOSE:
					m_logger.info(TAG + "FEDERATION PHASE: Received CLOSE Packet");
					accessToState(true, Constants.FSS_CLOSURE);
					sendCLOSEACKPacket();
					m_logger.info(TAG + "FEDERATION PHASE: CLOSE ACK Packet sent");
					lockForPacket(Constants.PACKET_FSS_CLOSE_ACK);
					break;

				case Constants.PACKET_FSS_DATA:
					m_logger.info(TAG + "FEDERATION PHASE: Received DATA Packet " + m_rx_packet.counter);

					if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_STORAGE) {

						/* Verify if there are space to store the packet */
						if (m_fss_buffer.getSize() < m_buffer_thr) {
							/* Store the packet */
							m_fss_buffer.insertData(m_rx_packet.getData());

							/* Verify if the service can still be provided */
							if (m_fss_buffer.getSize() >= m_buffer_thr) {
								accessToState(true, Constants.FSS_CLOSURE);
								sendCLOSEDATAACKPacket();
								m_logger.info(TAG + "FEDERATION PHASE: CLOSE DATA ACK Packet sent");
								lockForPacket(Constants.PACKET_FSS_CLOSE_ACK);
							} else {
								sendDATAACKPacket();
								m_logger.info(TAG + "FEDERATION PHASE: DATA ACK Packet sent");
								lockForPacket(Constants.PACKET_FSS_DATA);
							}
						} else {
							accessToState(true, Constants.FSS_CLOSURE);
							sendCLOSEPacket();
							m_logger.info(TAG + "FEDERATION PHASE: CLOSE Packet sent");
							/* In the next iteration the closePhase will be executed */
							lockForPacket(Constants.PACKET_FSS_CLOSE_ACK); /* Wait the CLOSE ACK */
						}

					} else if (m_service_type_interest == Constants.FSS_SERVICE_TYPE_DOWNLOAD) {

						if (m_download_contact == false || m_fss_buffer.getSize() >= m_buffer_thr) {
							/*
							 * The download slot is over, then I have to close the federation. Or my buffer
							 * is full. I do not have the service any more.
							 */
							accessToState(true, Constants.FSS_CLOSURE);
							sendCLOSEPacket();
							m_logger.info(TAG + "FEDERATION PHASE: CLOSE Packet sent");
							lockForPacket(Constants.PACKET_FSS_CLOSE_ACK); /* Wait the CLOSE ACK */
						} else if (m_download_contact == true) {
							m_fss_buffer.insertData(m_rx_packet.getData());
						}
					}
					break;
				}

				// } else if(m_waiting_packet == true &&
				// space.golbriak.lang.System.currentTimeMillis() >= m_waiting_next) {
			} else if (m_waiting_packet == true && m_time.getTimeMillis() >= m_waiting_next) {
				/*
				 * No reply has been received, and the timeout has passed - retransmit or exit
				 */
				verifyLinkDead();
			}
		}

	}

	private void closurePhase(boolean isRX) {

		if (isRX == true) {

			if (m_waiting_packet == true && (m_waiting_type & m_rx_packet.type) != 0) {
				/*
				 * I have received the packet that I was waiting - I can release and keep
				 * working
				 */
				releaseForPacket();
			}

			if (m_rx_packet.type == Constants.PACKET_FSS_CLOSE_ACK) {
				m_logger.info(TAG + "CLOSURE PHASE: Received CLOSE ACK Packet");

				if (m_tx_packet.type == Constants.PACKET_FSS_CLOSE) {
					sendCLOSEACKPacket();
					m_logger.info(TAG + "CLOSURE PHASE: CLOSE ACK Packet sent");
					lockForPacket(Constants.PACKET_FSS_CLOSE_ACK);
				}

				/* I have correctly closed everything, I just exit */
				m_running = false;
			}

		}

		// if(m_running == true && space.golbriak.lang.System.currentTimeMillis() >=
		// m_waiting_next) {
		if (m_running == true && m_time.getTimeMillis() >= m_waiting_next) {
			/*
			 * No reply has been received, and the timeout has passed - retransmit or exit
			 */
			verifyLinkDead();
		}
	}

	private void setHeaderPacket(Packet packet, int type, int address) {
		// packet.setTimestamp(space.golbriak.lang.System.currentTimeMillis());
		packet.timestamp = m_time.getTimeMillis();

		packet.source = m_sat_source;
		packet.destination = address;
		packet.type = type;
		packet.counter = accessToTXs(false, 0, 0);
	}

	public void controlledStop() {
		m_running = false;
	}

	public synchronized boolean polling(boolean poll) {
		boolean previous_poll = m_poll_token;
		m_poll_token = poll;
		return previous_poll;
	}

	private boolean receivePacket() 
	{
		byte[] content;
		if(m_isc_buffer.bytesAvailable() > 0) {
			m_isc_buffer.read(m_ipc_header_stream);
			m_rx_packet.setHeader(m_ipc_header_stream);
			content = new byte[m_rx_packet.length];
			m_isc_buffer.read(content);
			m_isc_buffer.read(m_ipc_checksum_stream);
			m_rx_packet.setChecksum(m_ipc_checksum_stream);
			// TODO: Interesting to include the time in which it is received...
			m_hk_packets.insertRXPacket(m_rx_packet);
			if(m_rx_packet.isPacketCorrect(m_rx_packet.checksum, content) == false) {
				m_logger.warning(TAG + "IPC Stack has bytes, but the received packet is not correct");
				accessToErrRXs(true, 1, 1);
				return false;
			} else if (m_rx_packet.destination != Constants.FSS_BROADCAST_ADDR
					&& m_rx_packet.destination != m_sat_source) {
				/* The packet is not for me; I should remove it */
				m_rx_packet.resetValues();
				return false;
			}
		} else {
			/* No packet has been received */
			return false;
		}
		return true;
	}

	private boolean sendPacket() {

		int counter = 0;
		
		/* Sending packet */
		m_logger.info(TAG + "Sending packet " + m_tx_packet.type + " count " + m_tx_packet.counter);
		
		do {
			m_dispatcher.transmitPacket(m_prot_num, m_tx_packet);
			while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 0) {
				try {
	    			/* Wait and release the processor */
	            	Thread.sleep(10);
	            } catch(InterruptedException e) {
	            	m_logger.error(e);
	            }
			}
			
			if(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 2) {
				counter ++;
	    		m_logger.warning(TAG + "Failed to send a FSS Packet to the RF ISL; Try number " + counter);
			}
		} while(m_dispatcher.accessRequestStatus(m_prot_num, 0, false) == 2 && counter <= Constants.rf_isl_max_fsspacket);
    	
    	if(counter >= Constants.rf_isl_max_fsspacket) {
    		m_logger.error(TAG + "FSS packet is not sent correctly; Problem with the RF ISL Module.");
			return false;
		} else {
			m_logger.info(TAG + "Packet sent correctly; type " + m_tx_packet.type + " count "
					+ m_tx_packet.counter);
			m_hk_packets.insertTXPacket(m_tx_packet);
			accessToTXs(true, 1, 1);
		}
		
		return true;
	}

	public int getFederationStatus() {
		return accessToState(false, 0);
	}

	public synchronized int accessToState(boolean write, int state) {

		if (write == true) {
			m_state = state;
			m_logger.info(TAG + "Changed to Federation state " + m_state);
		}

		return m_state;
	}

	public int getFederationRole() {
		return accessToRole(false, 0);
	}

	public synchronized int accessToRole(boolean write, int role) {

		if (write == true) {
			m_role = role;
		}

		return m_role;
	}

	public int getTXs() {
		return accessToTXs(false, 0, 0);
	}

	public synchronized int accessToTXs(boolean write, int sign, int value) {

		if (write == true) {
			m_tx_num = m_tx_num + sign * value;
		}

		return m_tx_num;
	}

	public int getRXs() {
		return accessToRXs(false, 0, 0);
	}

	public synchronized int accessToRXs(boolean write, int sign, int value) {

		if (write == true) {
			m_rx_num = m_rx_num + sign * value;
		}

		return m_rx_num;
	}

	public int getErrRXs() {
		return accessToErrRXs(false, 0, 0);
	}

	public synchronized int accessToErrRXs(boolean write, int sign, int value) {

		if (write == true) {
			m_rx_err_num = m_rx_err_num + sign * value;
		}

		return m_rx_err_num;
	}

	public int getDuration() {
		return (int) (m_end_time - m_start_time);
	}
}
