package com.example.tcp;

/**
 * Timer class.
 * @author lenovo
 *
 */
public class Timer {
	//Deadline
	public long deadline;
	//Data Packet.
	public Packet packet;
	
	public Timer() {
		this.packet = new Packet();
	}
	
	/**
	 * Constructing function.
	 * @param time
	 * @param packet
	 */
	public Timer(long deadline, Packet packet) {
		this.deadline = deadline;
		this.packet = packet;
	}
}
