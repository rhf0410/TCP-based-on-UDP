package com.example.tcp;

import java.io.IOException;

public class Main {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String receiver_host_ip = args[0];
		int receiver_port = Integer.valueOf(args[1]);
		String file = args[2];
		int MWS = Integer.valueOf(args[3]);
		int MSS = Integer.valueOf(args[4]);
		int gamma = Integer.valueOf(args[5]);
		float pDrop = Float.valueOf(args[6]);
		float pDuplicate = Float.valueOf(args[7]);
		float pCorrupt = Float.valueOf(args[8]);
		float pOrder = Float.valueOf(args[9]);
		int maxOrder = Integer.valueOf(args[10]);
		float pDelay = Float.valueOf(args[11]);
		int maxDelay = Integer.valueOf(args[12]);
		int seed = Integer.valueOf(args[13]);
		
		Sender sender = new Sender(receiver_host_ip, receiver_port, file, MWS, MSS, gamma, pDrop, pDuplicate, pCorrupt, pOrder, maxOrder, pDelay, maxDelay, seed);
		Receiver receiver = new Receiver(receiver_port, "r_file_1.pdf");
		
		new Thread(sender).start();
		new Thread(receiver).start();
	}

}
