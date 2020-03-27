package com.example.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sending Thread.
 * @author lenovo
 *
 */
public class Sender implements Runnable{
	private long EstimateRTT = 500;
	private long DevRTT = 250;
	private long TimeoutInterval;
	//Current time.
	private long current_time;
	//Sender socket.
	private DatagramSocket ds;
	//Store file content.
	private BufferedInputStream file_bytes;
	//Store data transfered from PDF.
	private byte [] data_bytes;
	//The name of the pdf file that has to be transferred from sender to receiver using your STP.
	private String file;
	//The IP address of the host machine on which the Receiver is running.
	private String receiver_host_ip;
	//The port number on which Receiver is expecting to receive packets from the sender.
	private int receiver_port;
	//The maximum window size used by your STP protocol in bytes.
	private int MWS;
	//Maximum Segment Size which is the maximum amount of data (in bytes) carried in each STP segment.
	private int MSS;
	//The probability that a STP data segment which is ready to be transmitted will be dropped.
	private float pDrop;
	//The probability that a data segment which is not dropped will be duplicated. 
	private float pDuplicate;
	//The probability that a data segment which is not dropped/duplicated will be corrupted.
	private float pCorrupt;
	//The probability that a data segment which is not dropped, duplicated and corrupted will be re-ordered. 
	private float pOrder;
	//The maximum number of packets a particular packet is held back for re-ordering purpose.
	private int maxOrder;
	//The probability that a data segment which is not dropped, duplicated, corrupted or re-ordered will be delayed. 
	private float pDelay;
	//The maximum delay (in milliseconds) experienced by those data segments that are delayed.
	private int maxDelay;
	//The seed for your random number generator.
	private Random random;
	private int seed;
	//This value is used for calculation of timeout value.
	private int gamma;
	//Send Log.
	private File send_log;
	//Counter.
	private int count;
	
	//The last byte acked.
	private int seqNum;
	//The last byte sent.
	private int ack;
	//The ACK number.
	private int ackNum;
	//The last sent packet.
	private int theLastSentNum;
	//The last acked packet.
	private int theLastAckedNum;
	//Send packet
	private Packet packet;

	//Thread pool
	private ExecutorService pool;
	//Lock object
	private final Lock lock;
	//Condition object
	private final Condition cond;
	private boolean flag;
	private boolean end;
	private boolean isTimeout;
	private long start_time;
	private long end_time;
	//Timer array.
	private List<Timer> timers;
	private boolean dup_flag;
	private boolean cor_flag;
	private boolean is_delay;
	private Packet theLastSentPacket;
	//Reorder packet.
	private List<Packet>rorder_packet;
	
	/**
	 * Sender constructing function. 
	 * @throws IOException 
	 */
	public Sender(String receiver_host_ip, int receiver_port, String file, int MWS, int MSS, int gamma,
			  float pDrop, float pDuplicate, float pCorrupt, float pOrder, int maxOrder, float pDelay,
			  int maxDelay, int seed) throws IOException {
		this.current_time = System.currentTimeMillis();
		//Create DatagramSocket.
		this.ds = new DatagramSocket();
		this.file = file;
		FileInputStream dis = new FileInputStream(new File(this.file));
		this.file_bytes = new BufferedInputStream(dis);
		this.data_bytes = this.toByteArray();
		this.TimeoutInterval = EstimateRTT + gamma * DevRTT;
		this.receiver_host_ip = receiver_host_ip;
		this.receiver_port = receiver_port;
		this.MWS = MWS;
		this.MSS = MSS;
		this.gamma = gamma;
		this.seed = seed;
		this.send_log = new File("Sender_log.txt");
		
		this.pDrop = pDrop;
		this.pDuplicate = pDuplicate;
		this.pCorrupt = pCorrupt;
		this.pOrder = pOrder;
		this.pDelay = pDelay;
		
		this.maxOrder = maxOrder;
		this.maxDelay = maxDelay;
		
		this.seqNum = 0;
		this.ack = 0;
		this.ackNum = 0;
		this.theLastSentNum = 0;
		this.theLastAckedNum = 0;
		this.random = new Random(this.seed);
		this.timers = new LinkedList<Timer>();
		
		//Define thread pool used for sending message.
		//send = new Send(receiver_host_ip, receiver_port, seed, send_log, file, MWS, MSS);
		this.pool = Executors.newCachedThreadPool();
		this.lock = new ReentrantLock();
		this.cond = this.lock.newCondition();
		this.flag = false;
		this.end = false;
		this.isTimeout = false;
		
		this.packet = new Packet();
		this.dup_flag = false;
		this.cor_flag = false;
		this.is_delay = false;
		this.theLastSentPacket = new Packet();
		//Reorder list used for storing reorder packet. The size is 1.
		this.rorder_packet = new ArrayList<Packet>(1);
		this.count = 0;
	}
	
	/**
	 * three-way handshake (SYN, SYN+ACK, ACK) for the connection establishment.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void handshake() throws IOException, ClassNotFoundException{
		//Send SYN.
		Packet syn = new Packet(this.seqNum, this.ack, 1, 0, 0, "".getBytes());
		ByteArrayOutputStream syn_baos = new ByteArrayOutputStream();
		ObjectOutputStream syn_ois = new ObjectOutputStream(syn_baos);
		syn_ois.writeObject(syn);
		byte [] syn_arr = syn_baos.toByteArray();
		DatagramPacket syn_packet = new DatagramPacket(syn_arr, syn_arr.length, Inet4Address.getByName(this.receiver_host_ip), this.receiver_port);
		this.ds.send(syn_packet);
		this.seqNum = this.seqNum + 1;
		Log.log(this.send_log, syn, "snd", this.current_time);
		
		//Receive SYN and ACK.
		byte [] received_data = new byte [1024];
		DatagramPacket ACK_SYN = new DatagramPacket(received_data, received_data.length);
		this.ds.receive(ACK_SYN);
		ByteArrayInputStream ack_syn_bais = new ByteArrayInputStream(received_data);
		ObjectInputStream ack_syn_ois = new ObjectInputStream(ack_syn_bais);
		Packet ack_syn_packet = (Packet) ack_syn_ois.readObject();
		this.ack = ack_syn_packet.getSeqNum() + 1;
		Log.log(this.send_log, ack_syn_packet, "rcv", this.current_time);
		
		//Send ACK
		Packet ackPacket = new Packet(this.seqNum, this.ack, 0, 1, 0, "".getBytes());
		ByteArrayOutputStream ack_baos = new ByteArrayOutputStream();
		ObjectOutputStream ack_ois = new ObjectOutputStream(ack_baos);
		ack_ois.writeObject(ackPacket);
		byte [] ack_arr = ack_baos.toByteArray();
		DatagramPacket ack_packet = new DatagramPacket(ack_arr, ack_arr.length, Inet4Address.getByName(this.receiver_host_ip), this.receiver_port);
		this.ds.send(ack_packet);
		Log.log(this.send_log, ackPacket, "snd", this.current_time);
	}
	
	/**
	 * A four-segment (FIN, ACK, FIN, ACK) connection termination. 
	 * The Sender will initiate the connection close 
	 * once the entire file has been successfully transmitted.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void termination() throws IOException, ClassNotFoundException{
		//Send FIN.
		Packet finPacket = new Packet(this.seqNum, this.ack, 0, 0, 1, "".getBytes());
		ByteArrayOutputStream fin_baos = new ByteArrayOutputStream();
		ObjectOutputStream fin_ois = new ObjectOutputStream(fin_baos);
		fin_ois.writeObject(finPacket);
		byte [] fin_arr = fin_baos.toByteArray();
		DatagramPacket fin_packet = new DatagramPacket(fin_arr, fin_arr.length, Inet4Address.getByName(this.receiver_host_ip), this.receiver_port);
		this.ds.send(fin_packet);
		Log.log(this.send_log, finPacket, "snd", this.current_time);
		
		//Receive ACK.
		byte [] recv_ack = new byte [1024];
		DatagramPacket ACK_SYN = new DatagramPacket(recv_ack, recv_ack.length);
		this.ds.receive(ACK_SYN);
		ByteArrayInputStream ack_bais = new ByteArrayInputStream(recv_ack);
		ObjectInputStream ack_ois = new ObjectInputStream(ack_bais);
		Packet ack_packet = (Packet) ack_ois.readObject();
		Log.log(this.send_log, ack_packet, "rcv", this.current_time);
		
		//Receive FIN.
		byte [] recv_fin = new byte [1024];
		DatagramPacket FIN_PACKET = new DatagramPacket(recv_fin, recv_fin.length);
		this.ds.receive(FIN_PACKET);
		ByteArrayInputStream fin_bais = new ByteArrayInputStream(recv_fin);
		ObjectInputStream fin_ois_2 = new ObjectInputStream(fin_bais);
		Packet recv_fin_packet = (Packet) fin_ois_2.readObject();
		Log.log(this.send_log, recv_fin_packet, "rcv", this.current_time);
		this.seqNum += 1;
		this.ack = recv_fin_packet.getSeqNum() + 1;
		
		//Send ACK back.
		Packet ackPacketBack = new Packet(this.seqNum, this.ack, 0, 1, 0, "".getBytes());
		ByteArrayOutputStream ack_baos = new ByteArrayOutputStream();
		ObjectOutputStream snd_ack_ois = new ObjectOutputStream(ack_baos);
		snd_ack_ois.writeObject(ackPacketBack);
		byte [] ack_arr = ack_baos.toByteArray();
		DatagramPacket snd_ack_packet = new DatagramPacket(ack_arr, ack_arr.length, Inet4Address.getByName(this.receiver_host_ip), this.receiver_port);
		this.ds.send(snd_ack_packet);
		Log.log(this.send_log, ackPacketBack, "snd", this.current_time);
	}
	
	/**
	 * Sending thread.
	 * @author lenovo
	 *
	 */
	class Send extends Thread{
		@Override
		public void run() {
			try {
				send();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CloneNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void send() throws ClassNotFoundException, IOException, CloneNotSupportedException{
			while(true){
				lock.lock();
				try {
					if(flag){
						cond.await();
					}
					long current_time = System.currentTimeMillis();
					if(timers.size() > 0){
						System.out.println(current_time + ":" + timers.get(0).deadline + ":" + timers.size());
					}
					if(timers.size() > 0 && current_time >= timers.get(0).deadline){
						isTimeout = true;
						Timer timer = timers.remove(0);
						Packet snd_packet = timer.packet;
						current_time = System.currentTimeMillis();
						Timer new_timer = new Timer(current_time + TimeoutInterval, snd_packet);
						timers.add(new_timer);
						if(!PLD(snd_packet)){
							continue;
						}
						flag = true;
						cond.signal();
					}
					
					if(theLastSentNum - theLastAckedNum <= MWS || ackNum == 3){
						int size = 0;
						if(ackNum == 3){
							size = MSS;
						}else{
							size = MWS - (theLastSentNum - theLastAckedNum);
							size = Math.min(size, MSS);
						}
						Packet snd_packet = new Packet();
						byte [] snd_data = new byte[0];
						if(ackNum == 3){
							snd_data = splitByteArray(data_bytes, theLastAckedNum, size);
							snd_packet = new Packet(theLastAckedNum + 1, ack, 0, 0, 0, snd_data);
						}else{
							snd_data = splitByteArray(data_bytes, seqNum - 1, size);
							snd_packet = new Packet(seqNum, ack, 0, 0, 0, snd_data);
							seqNum += snd_data.length;
						}
						packet = snd_packet;
						if(snd_data.length == 0){
							//End thread.
							if(timers.size() == 0){
								termination();
								end = true;
								cond.signal();
								Thread.currentThread().interrupt();
								break;
							}
							continue;
						}

						current_time = System.currentTimeMillis();
						long deadline = current_time + TimeoutInterval;
						Timer timer = new Timer(deadline, snd_packet);
						timers.add(timer);
						
						//Sending data packet.
						if(!PLD(snd_packet)){
							continue;
						}
						flag = true;
						cond.signal();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					lock.unlock();
				}
			}
		}
	}
	
	/**
	 * Receiving thread.
	 * @author lenovo
	 *
	 */
	class Receive extends Thread{
		@Override
		public void run() {
			receive();
		}
		
		private void receive(){
			while(true){
				lock.lock();
				try {
					if(!flag){
						cond.await();
					}
					if(end){
						Thread.currentThread().interrupt();
						break;
					}
					//Receive ACK
					byte [] recv_ack_data = new byte [1024];
					DatagramPacket ack_packet = new DatagramPacket(recv_ack_data, recv_ack_data.length);
					ds.receive(ack_packet);
					ByteArrayInputStream ack_bais = new ByteArrayInputStream(recv_ack_data);
					ObjectInputStream ack_ois = new ObjectInputStream(ack_bais);
					Packet recv_ack_packet = (Packet) ack_ois.readObject();
					
					//Remove timer from timers
					if(!cor_flag){
						removeFromTimers(recv_ack_packet);
						removeFromTimers2(packet);
					}
					
					end_time = System.currentTimeMillis();
					if(recv_ack_packet.getAckNum() == theLastAckedNum + 1){
						if(!dup_flag && !cor_flag){
							ackNum++;
						}
						Log.log(send_log, recv_ack_packet, "rcv/DA", current_time);
					}else{
						Log.log(send_log, recv_ack_packet, "rcv", current_time);
					}
					if(recv_ack_packet.getAckNum() == packet.getSeqNum() + packet.getLength()){
						calculateTimeout(start_time, end_time);
					}
					theLastAckedNum = recv_ack_packet.getAckNum() - 1;
					seqNum = Math.max(seqNum, recv_ack_packet.getAckNum());
					theLastSentPacket = packet;
					
					//Avoid error caused by retransmission.
					if(dup_flag){
						if(ackNum != 3){
							seqNum -= packet.getLength();
						}
						dup_flag = false;
					}
					
					if(cor_flag){
						cor_flag = false;
					}
					
					//Counting in re-order queue.
					addCount();
					
					flag = false;
					cond.signal();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}finally{
					lock.unlock();
				}
			}
		}
	}
	
	/**
	 * Send and receive process.
	 */
	private void sendAndReceive(){
		Send send = new Send();
		Receive receive = new Receive();
		send.start();
		receive.start();
	}
	
	@Override
	public void run() {
		try {
			this.handshake();
			this.sendAndReceive();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * The function of the PLD is to emulate some of the events that 
	 * can occur in the Internet such as loss of packets, packet corruption, 
	 * packet re-ordering and delays. 
	 * @param packet
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws CloneNotSupportedException 
	 */
	private boolean PLD(Packet packet) throws IOException, CloneNotSupportedException, ClassNotFoundException{
		//Record the last send number.
		this.theLastSentNum = Math.max(this.theLastSentNum, packet.getSeqNum() + packet.getLength() - 1);
		if(this.count == this.maxOrder && this.rorder_packet.size() != 0){
			this.handleRorderSndPacket();
			return true;
		}else if(this.randomGenerator(this.pDrop)){
			this.handleDropPacket(packet);
		}else if(this.randomGenerator(this.pDuplicate)){
			this.dup_flag = true;
			sendData(this.theLastSentPacket);
			return true;
		}else if(this.randomGenerator(this.pCorrupt)){
			this.cor_flag = true;
			handleCorruptPacket(packet);
			return true;
		}else if(this.randomGenerator(this.pOrder)){
			return this.handleRorderPacket(packet);
		}else if(this.randomGenerator(this.pDelay)){
			this.handleDelayPacket(packet);
			return true;
		}else{
			sendData(packet);
			return true;
		}
		return false;
	}
	
	/**
	 * Send Data to server.
	 * @param packet
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void sendData(Packet packet) throws IOException{
		//Send Data.
		ByteArrayOutputStream data_baos = new ByteArrayOutputStream();
		ObjectOutputStream data_ois = new ObjectOutputStream(data_baos);
		data_ois.writeObject(packet);
		byte [] snd_packet_dec = data_baos.toByteArray();
		DatagramPacket snd_data_packet = new DatagramPacket(snd_packet_dec, snd_packet_dec.length, Inet4Address.getByName(this.receiver_host_ip), this.receiver_port);
		this.ds.send(snd_data_packet);
		//Noting into log.
		if(this.is_delay){
			Log.log(this.send_log, packet, "snd/dely", this.current_time);
			this.is_delay = false;
		}else if(this.dup_flag){
			Log.log(this.send_log, packet, "snd/dup", this.current_time);	
		}else if(this.cor_flag){
			Log.log(this.send_log, packet, "snd/corr", this.current_time);
		}else if(this.rorder_packet.size() != 0 && this.count == this.maxOrder){
			Log.log(this.send_log, packet, "snd/rord", this.current_time);
		}else if(this.isTimeout){
			Log.log(this.send_log, packet, "snd/RTX", this.current_time);
			this.isTimeout = false;
		}else if(ackNum == 3){
			Log.log(this.send_log, packet, "snd/RTX", this.current_time);
			this.ackNum = 0;
		}else{
			Log.log(this.send_log, packet, "snd", this.current_time);
		}
	}
	
	/**
	 * Modify one bit of bytes.
	 * @param bytes
	 */
	public void modifyOneBit(Packet packet){
		Random random = new Random();
		String binary = converBinStr(packet.getContent());
		int num = random.nextInt(binary.length());
		while(binary.charAt(num) == ','){
			num = random.nextInt(binary.length());
		}
		char [] chars = binary.toCharArray();
		if(chars[num] == '0'){
			chars[num] = '1';
		}else{
			chars[num] = '0';
		}
		String new_binary = new String(chars);
		byte [] new_bytes = converStrBin(new_binary);
		packet.setContent(new_bytes);
	}
	
	/**
	 * Convert binary string to byte arrays.
	 * @param b
	 * @return
	 */
	private String converBinStr(byte[] b) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            result.append(Long.toString(b[i] & 0xff, 2) + ",");
        }
        return result.toString().substring(0, result.length() - 1);
    }
	
	/**
	 * Convert binary string to byte arrays.
	 * @param hex2Str
	 * @return
	 */
	private byte[] converStrBin(String hex2Str) {
        String[] temp = hex2Str.split(",");
        byte[] b = new byte[temp.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = Long.valueOf(temp[i], 2).byteValue();
        }
        return b;
    }
	
	/**
	 * Return byte array.
	 * @return
	 * @throws IOException
	 */
	private byte [] toByteArray() throws IOException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		BufferedOutputStream bout = new BufferedOutputStream(baos);
		byte[] buffer = new byte[1024];
		int len = this.file_bytes.read(buffer);
		while(len != -1){
			bout.write(buffer, 0, len);
			len = this.file_bytes.read(buffer);
		}
		bout.close();
		return baos.toByteArray();
	}
	
	/**
	 * Split byte array.
	 * @param bytes
	 * @param num
	 * @return
	 */
	private byte [] splitByteArray(byte [] bytes, int begin, int num){
		int number = num;
		if(begin >= bytes.length){
			return new byte[0];
		}
		if(begin + num > bytes.length){
			number = bytes.length - begin;
		}
		byte [] res = new byte[number];
		for(int i=begin, j=0;i<begin+number;i++,j++){
			res[j] = bytes[i];
		}
		return res;
	}
	
	/**
	 * Calculate timeout if data send successfully.
	 * @param start
	 * @param end
	 */
	private void calculateTimeout(long start, long end){
		long sampleRTT = end - start;
		this.EstimateRTT = (long)(0.875 * this.EstimateRTT + 0.125 * sampleRTT);
		this.DevRTT = (long)(0.75 * (double)this.DevRTT + 0.25 * (double)Math.abs(sampleRTT - this.EstimateRTT));
		this.TimeoutInterval = this.EstimateRTT + this.gamma * this.DevRTT;
		if(this.TimeoutInterval < 200){
			this.TimeoutInterval = 200;
		}
		if(this.TimeoutInterval > 60000){
			this.TimeoutInterval = 60000;
		}
	}
	
	/**
	 * Random number generator, used for checking which actions in PLD to implement.
	 * @param probability
	 * @return
	 */
	private boolean randomGenerator(float probability){
		float x = this.random.nextFloat();
		if(x < probability){
			return true;
		}
		return false;
	}
	
	/**
	 * Handle for dropped data packet.
	 * @param packet
	 */
	private void handleDropPacket(Packet packet){
		Log.log(this.send_log, packet, "drop", this.current_time);
		//Counting in re-order queue.
		this.addCount();
	}
	
	/**
	 * Handle for corrupt packet.
	 * @param packet
	 * @throws CloneNotSupportedException 
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private void handleCorruptPacket(Packet packet) throws CloneNotSupportedException, IOException, ClassNotFoundException{
		Packet corrupt_packet = (Packet) packet.clone();
		this.modifyOneBit(corrupt_packet);
		sendData(corrupt_packet);
		//Counting in re-order queue.
		this.addCount();
	}
	
	/**
	 * Handle for re-order packet.
	 * If rorder queue is empty, store it. Otherwise, send it directly.
	 * @param packet
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private boolean handleRorderPacket(Packet packet) throws IOException, ClassNotFoundException{
		if(this.rorder_packet.size() == 0){
			this.rorder_packet.add(packet);
			this.removeFromTimers2(packet);
			return false;
		}else{
			this.sendData(packet);
			return true;
		}
	}
	
	/**
	 * Re-send out-of-order data packet.
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	private void handleRorderSndPacket() throws IOException, ClassNotFoundException{
		Packet snd_packet = this.rorder_packet.get(0);
		this.sendData(snd_packet);
		this.rorder_packet.remove(0);
		//Reset counter.
		this.count = 0;
	}
	
	/**
	 * Handle for delay packet.
	 * @param packet
	 */
	private void handleDelayPacket(Packet packet){
		this.is_delay = true;
		int interval = random.nextInt(this.maxDelay);
		try {
			Thread.sleep(interval);
			this.sendData(packet);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Remove timer whose seqNum is less than received ACK number from timers.
	 */
	private void removeFromTimers(Packet packet){
		for(int i=0;i<this.timers.size();i++){
			if(this.timers.get(i).packet.getSeqNum() < packet.getAckNum()){
				this.timers.remove(i);
				i--;
			}
		}
	}
	
	/**
	 * Removve RTX packet from timers.
	 * @param packet
	 */
	private void removeFromTimers2(Packet packet){
		for(int i=this.timers.size()-1;i>=0;i--){
			if(this.timers.get(i).packet.getSeqNum() == packet.getSeqNum()){
				this.timers.remove(i);
				break;
			}
		}
	}
	
	/**
	 * If it is out of order, increase count by 1.
	 */
	private void addCount(){
		if(this.rorder_packet.size() != 0){
			this.count++;
		}
	}
}
