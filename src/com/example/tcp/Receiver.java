package com.example.tcp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Receiving Thread.
 * @author lenovo
 *
 */
public class Receiver implements Runnable{
	//Current time;
	private long current_time;
	//Receiver Socket.
	private DatagramSocket ds;
	//the port number on which the Receiver will open a UDP socket for receiving datagrams from the Sender.
	private int receiver_port;
	//the name of the pdf file into which the data sent by the sender should be stored.
	private String file_r;
	//Receiver Log
	private File receiver_log;
	//The last byte acked
	private int ack;
	//The last byte sent
	private int seqNum;
	//Expected sequence number.
	private int expectedSeqNum;
	//Repeated seqNum.
	private int getSeqNum;
	//Buffer
	private Queue<Packet>buffer;
	//Byte Queue used for collecting data.
	private List<Packet>byte_queue;
	//Insert location.
	private int index;
	//The last got packet.
	private Packet theLastGotPacket;
	private int theLastRecvACK;
	
	/**
	 * Constructing function.
	 * @param receiver_port
	 * @param file_r
	 * @throws SocketException 
	 */
	public Receiver(int receiver_port, String file_r) throws SocketException{
		//Create DatagramSocket.
		this.ds = new DatagramSocket(receiver_port);
		this.current_time = System.currentTimeMillis();
		this.receiver_port = receiver_port;
		this.file_r = file_r;
		this.receiver_log = new File("Receiver_log.txt");
		this.ack = 0;
		this.seqNum = 0;
		this.expectedSeqNum = 0;
		this.getSeqNum = 0;
		//Queue used for storing data packet out of order.
		this.buffer = new LinkedList<Packet>();
		this.byte_queue = new LinkedList<Packet>();
		this.theLastGotPacket = new Packet();
	}
	
	/**
	 * three-way handshake (SYN, SYN+ACK, ACK) for the connection establishment.
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 */
	public void handshake() throws IOException, ClassNotFoundException{
		//Receive SYN.
		byte [] recv_data = new byte [1024];
		DatagramPacket SYN = new DatagramPacket(recv_data, recv_data.length);
		this.ds.receive(SYN);
		ByteArrayInputStream syn_bais = new ByteArrayInputStream(recv_data);
		ObjectInputStream syn_ois = new ObjectInputStream(syn_bais);
		Packet syn_packet = (Packet) syn_ois.readObject();
		this.ack = syn_packet.getAckNum() + 1;
		Log.log(this.receiver_log, syn_packet, "rcv", this.current_time);
		
		//Send ACK and SYN
		Packet ack_and_syn = new Packet(this.seqNum, this.ack, 1, 1, 0, "".getBytes());
		ByteArrayOutputStream ack_syn_baos = new ByteArrayOutputStream();
		ObjectOutputStream ack_syn_ois = new ObjectOutputStream(ack_syn_baos);
		ack_syn_ois.writeObject(ack_and_syn);
		byte [] ack_syn_arr = ack_syn_baos.toByteArray();
		DatagramPacket sendAckSyn = new DatagramPacket(ack_syn_arr, ack_syn_arr.length, SYN.getAddress(), SYN.getPort());
		this.ds.send(sendAckSyn);
		Log.log(this.receiver_log, ack_and_syn, "snd", this.current_time);

		//Receive ACK
		byte [] ack_data = new byte [1024];
		DatagramPacket ACK = new DatagramPacket(ack_data, ack_data.length);
		this.ds.receive(ACK);
		ByteArrayInputStream ack_bais = new ByteArrayInputStream(ack_data);
		ObjectInputStream ack_ois = new ObjectInputStream(ack_bais);
		Packet ack_packet = (Packet) ack_ois.readObject();
		Log.log(this.receiver_log, ack_packet, "rcv", this.current_time);
		this.expectedSeqNum = 1;
		this.theLastRecvACK = 1;
		this.seqNum += 1;
	}
	
	/**
	 * A four-segment (FIN, ACK, FIN, ACK) connection termination. 
	 * The Sender will initiate the connection close once 
	 * the entire file has been successfully transmitted.
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public void termination(Packet packet, InetAddress ip, int port) throws IOException, ClassNotFoundException{
		//Receive FIN datagram.
		this.ack = packet.getSeqNum() + 1;
		Log.log(this.receiver_log, packet, "rcv", this.current_time);
		
		//Send ACK.
		Packet ackPacket = new Packet(this.seqNum, this.ack, 0, 1, 0, "".getBytes());
		ByteArrayOutputStream ack_baos = new ByteArrayOutputStream();
		ObjectOutputStream ack_ois = new ObjectOutputStream(ack_baos);
		ack_ois.writeObject(ackPacket);
		byte [] ack_arr = ack_baos.toByteArray();
		DatagramPacket ack_datagram = new DatagramPacket(ack_arr, ack_arr.length, ip, port);
		this.ds.send(ack_datagram);
		Log.log(this.receiver_log, ackPacket, "snd", this.current_time);
		
		//Send FIN.
		Packet finPacket = new Packet(this.seqNum, this.ack, 0, 0, 1, "".getBytes());
		ByteArrayOutputStream fin_baos = new ByteArrayOutputStream();
		ObjectOutputStream fin_ois = new ObjectOutputStream(fin_baos);
		fin_ois.writeObject(finPacket);
		byte [] fin_arr = fin_baos.toByteArray();
		DatagramPacket fin_datagram = new DatagramPacket(fin_arr, fin_arr.length, ip, port);
		this.ds.send(fin_datagram);
		Log.log(this.receiver_log, finPacket, "snd", this.current_time);
		
		//Receive ACK backed.
		byte [] recv_ack_back = new byte [1024];
		DatagramPacket RECV_ACK_BACK = new DatagramPacket(recv_ack_back, recv_ack_back.length);
		this.ds.receive(RECV_ACK_BACK);
		ByteArrayInputStream ack_bais = new ByteArrayInputStream(recv_ack_back);
		ObjectInputStream ack_ois_2 = new ObjectInputStream(ack_bais);
		Packet recvACKBack = (Packet) ack_ois_2.readObject();
		Log.log(this.receiver_log, recvACKBack, "rcv", this.current_time);
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			this.handshake();
			while(true){
				//Receive datagram.
				byte [] recv_data = new byte [1024];
				DatagramPacket recv_packet = new DatagramPacket(recv_data, recv_data.length);
				this.ds.receive(recv_packet);
				InetAddress ip = recv_packet.getAddress();
				int port = recv_packet.getPort();
				ByteArrayInputStream data_bais = new ByteArrayInputStream(recv_data);
				ObjectInputStream data_ois = new ObjectInputStream(data_bais);
				Packet recv_de_packet = (Packet) data_ois.readObject();
				boolean flag = this.isCorrupted(recv_de_packet);
				if(recv_de_packet.getLength() != 0){
					if(flag){
						Log.log(this.receiver_log, recv_de_packet, "rcv/corr", this.current_time);
					}else{
						Log.log(this.receiver_log, recv_de_packet, "rcv", this.current_time);
					}
				}
				
				//Check data send is done or not.
				if(recv_de_packet.getFinFlag() == 1){
					this.termination(recv_de_packet, ip, port);
					break;
				}
				
				//Collecting data.
				if(this.theLastGotPacket.getSeqNum() != recv_de_packet.getSeqNum() && !flag){
					this.insert(recv_de_packet);
					//Check seqNum received is expected or not.
					this.modifyNumber(recv_de_packet);
				}
				
				Packet ack_and_data = new Packet(this.seqNum, this.ack, 0, 1, 0, "".getBytes());
				ByteArrayOutputStream ack_baos = new ByteArrayOutputStream();
				ObjectOutputStream ack_ois = new ObjectOutputStream(ack_baos);
				ack_ois.writeObject(ack_and_data);
				byte [] ack_data = ack_baos.toByteArray();
				DatagramPacket sendAssureAck = new DatagramPacket(ack_data, ack_data.length, ip, port);
				this.ds.send(sendAssureAck);
				if(ack_and_data.getAckNum() == this.theLastRecvACK){
					Log.log(this.receiver_log, ack_and_data, "snd/DA", this.current_time);
				}else{
					Log.log(this.receiver_log, ack_and_data, "snd", this.current_time);
				}
				this.theLastRecvACK = ack_and_data.getAckNum();
				if(!flag){
					this.theLastGotPacket = recv_de_packet;
				}
			}
			this.binaryToPDF();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Convert bytes received to pdf file.
	 */
	private void binaryToPDF(){
		byte [] buffer = this.toBuffer();
		FileOutputStream fout = null;
    	BufferedOutputStream bout = null;
    	File file = new File(this.file_r);
    	try {
			fout = new FileOutputStream(file);
			bout = new BufferedOutputStream(fout);
			bout.write(buffer);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(bout != null){
				try {
					bout.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Put elements in list to array.
	 * @return
	 */
	private byte [] toBuffer(){
		Iterator<Packet>ite = this.byte_queue.iterator();
		List<Byte>mid_byte_queue = new ArrayList<Byte>();
		while(ite.hasNext()){
			Packet packet = ite.next();
			byte [] bytes = packet.getContent();
			for(int i=0;i<bytes.length;i++){
				mid_byte_queue.add(bytes[i]);
			}
		}
		byte [] buffer = new byte[mid_byte_queue.size()];
		for(int i=0;i<mid_byte_queue.size();i++){
			buffer[i] = mid_byte_queue.get(i);
		}
		return buffer;
	}
	
	/**
	 * Insert packet into queue by sort.
	 * @param packet
	 */
	private void insert(Packet packet){
		//If the size is 0, insert it directly.
		if(this.byte_queue.size() == 0){
			this.byte_queue.add(packet);
			return;
		}
		int i = this.byte_queue.size()-1;
		for(;i>=0;i--){
			if(this.byte_queue.get(i).getSeqNum() == packet.getSeqNum()){
				return;
			}else if(this.byte_queue.get(i).getSeqNum() < packet.getSeqNum()){
				break;
			}
		}
		//Note location where it insert.
		this.index = i+1;
		this.byte_queue.add(i+1, packet);
	}
	
	/**
	 * Retransmission number.
	 * @param packet
	 * @return
	 */
	private Packet returnSeq(Packet packet){
		int i=0;
		int ref = this.index;
		while(i<3&&(ref+1)<this.byte_queue.size()){
			int seq1 = this.byte_queue.get(ref).getSeqNum();
			int seq2 = this.byte_queue.get(ref+1).getSeqNum();
			int len = this.byte_queue.get(ref).getLength();
			if(seq2 - seq1 <= len){
				ref++;
			}else{
				break;
			}
			i++;
		}
		return this.byte_queue.get(ref);
	}
	
	/**
	 * Return expected ACK number in norms.
	 * @param packet
	 * @return
	 */
	private Packet returnNomSeq(Packet packet){
		int ref = this.index;
		for(int i=ref;i+1<this.byte_queue.size();i++){
			int seq1 = this.byte_queue.get(i).getSeqNum();
			int seq2 = this.byte_queue.get(i+1).getSeqNum();
			int len = this.byte_queue.get(i).getLength();
			if(seq2 - seq1 <= len){
				ref++;
			}else{
				ref=i;
				break;
			}
		}
		return this.byte_queue.get(ref);
	}
	
	/**
	 * After receiving packet, modifying the seq and ack number.
	 * @param packet
	 */
	private void modifyNumber(Packet packet){
		//Check seqNum received is expected or not.
		if(this.expectedSeqNum == packet.getSeqNum()){
			//Check retransmission or not.
			if(this.getSeqNum == 3){
				//Get the seq number of last packet.
				Packet aid_packet = this.returnSeq(packet);
				this.ack = aid_packet.getSeqNum() + aid_packet.getLength();
				this.expectedSeqNum = this.ack;
				this.getSeqNum = 0;
			}else{
				Packet aid_packet = this.returnNomSeq(packet);
				this.ack = aid_packet.getSeqNum() + aid_packet.getLength();
				this.expectedSeqNum = this.ack;
			}
		}else{
			this.getSeqNum++;
		}
	}
	
	/**
	 * Check recv packet is corrupted or not.
	 * @param packet
	 * @return
	 */
	private boolean isCorrupted(Packet packet){
		if(packet.calculateCheckSum() == packet.getChecksum()){
			return false;
		}else{
			return true;
		}
	}
}
