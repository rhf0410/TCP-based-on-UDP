package com.example.tcp;

import java.io.Serializable;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

/**
 * Data Packet.
 * @author lenovo
 *
 */
public class Packet implements Serializable, Cloneable{
	//Sequence Number
	private int seqNum;
	//ACK Number
	private int ackNum;
	//SYN Flag
	private int synFlag;
	//ACK Flag
	private int ackFlag;
	//FIN Flag
	private int finFlag;
	//Padding
	private int padding;
	//Content length
	private int length;
	//Content
	private byte [] content;
	//Checksum
	private long checksum;
	
	/**
	 * Constructing Function.
	 * @param seqNum
	 * @param ackNum
	 * @param synFlag
	 * @param ackFlag
	 * @param finFlag
	 * @param content
	 */
	public Packet(int seqNum, int ackNum, int synFlag, int ackFlag, int finFlag, byte [] content) {
		this.seqNum = seqNum;
		this.ackNum = ackNum;
		this.synFlag = synFlag;
		this.ackFlag = ackFlag;
		this.finFlag = finFlag;
		this.padding = 0;
		this.content = content;
		this.length = content.length;
		//Calculate checksum
		this.checksum = this.calculateCheckSum();
	}
	
	public Packet() {
		
	}
	
	/**
	 * Calculate checksum.
	 */
	public long calculateCheckSum(){
		Checksum checksumEngine = new Adler32();
		checksumEngine.update(this.content, 0, this.content.length);
		long checksum = checksumEngine.getValue();
		return checksum;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		Packet packet = null;
		try {
			packet = (Packet) super.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return packet;
	}
	
	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}

	public int getSynFlag() {
		return synFlag;
	}

	public void setSynFlag(int synFlag) {
		this.synFlag = synFlag;
	}

	public int getAckFlag() {
		return ackFlag;
	}

	public void setAckFlag(int ackFlag) {
		this.ackFlag = ackFlag;
	}

	public int getFinFlag() {
		return finFlag;
	}

	public void setFinFlag(int finFlag) {
		this.finFlag = finFlag;
	}

	public int getPadding() {
		return padding;
	}

	public void setPadding(int padding) {
		this.padding = padding;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public byte[] getContent() {
		return content;
	}

	public void setContent(byte [] content) {
		this.content = content;
	}


	public long getChecksum() {
		return this.checksum;
	}

	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}
	
}
