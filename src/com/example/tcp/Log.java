package com.example.tcp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;

/**
 * Log class, used for noting log.
 * @author lenovo
 *
 */
public class Log {
	/**
	 * Noting in log file.
	 * @param filename
	 * @param packet
	 * @param log
	 */
	public static void log(File filename, Packet packet, String log, long currentTime){
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
			
			long runningTime = System.currentTimeMillis();
			double timeInterval = (double)(runningTime - currentTime);
			double seconds = timeInterval/1000;
			String time = String.format("%.2f", seconds);

			//Get type of packet.
			String typeOfPacket = new String("");
			if(packet.getLength() > 0){
				typeOfPacket = "D";
			}
			if(packet.getSynFlag() == 1){
				typeOfPacket = "S";
			}
			if(packet.getFinFlag() == 1){
				typeOfPacket = "F";
			}
			if(packet.getAckFlag() == 1){
				typeOfPacket += "A";
			}
			
			//Log content.
			String con = String.format("%-10s%15s%8s%12s%10s%10s\n", log, time, typeOfPacket, packet.getSeqNum(), packet.getLength(), packet.getAckNum());
			out.write(con);
			out.flush();
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Record statistic information into snd log file.
	 * @param filename: filename of send log.
	 * @param size_of_file: Size of the file (in Bytes).
	 * @param retrans: Segments transmitted (including drop & RXT).
	 * @param pld: Number of Segments handled by PLD.
	 * @param num_of_drop: Number of Segments dropped.
	 * @param num_of_corrupt: Number of Segments Corrupted.
	 * @param num_of_rorder: Number of Segments Re-ordered.
	 * @param num_of_duplicate: Number of Segments Duplicated.
	 * @param num_of_delay: Number of Segments Delayed.
	 * @param num_of_retran: Number of Retransmissions due to TIMEOUT.
	 * @param num_of_ftran: Number of FAST RETRANSMISSION.
	 * @param num_of_dup: Number of DUP ACKS received.
	 */
	public static void snd_log(File filename, int size_of_file, int retrans, int pld, int num_of_drop, int num_of_corrupt,
			                   int num_of_rorder, int num_of_duplicate, int num_of_delay, int num_of_retran, int num_of_ftran,
			                   int num_of_dup){
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
			String line = "=============================================================\n";
			String SOF = String.format("%-50s%8s\n", "Size of the file (in Bytes)", size_of_file);
			String ST = String.format("%-50s%8s\n", "Segments transmitted (including drop & RXT)", retrans);
			String NOSHP = String.format("%-50s%8s\n", "Number of Segments handled by PLD", pld);
			String NOSD = String.format("%-50s%8s\n", "Number of Segments dropped ", num_of_drop);
			String NOSC = String.format("%-50s%8s\n", "Number of Segments Corrupted", num_of_corrupt);
			String NOSR = String.format("%-50s%8s\n", "Number of Segments Re-ordered ", num_of_rorder);
			String NOSDUP = String.format("%-50s%8s\n", "Number of Segments Duplicated ", num_of_duplicate);
			String NOSDLY = String.format("%-50s%8s\n", "Number of Segments Delayed ", num_of_delay);
			String NOSRT = String.format("%-50s%8s\n", "Number of Retransmissions due to TIMEOUT", num_of_retran);
			String NOFR = String.format("%-50s%8s\n", "Number of FAST RETRANSMISSION ", num_of_ftran);
			String NODA = String.format("%-50s%8s\n", "Number of DUP ACKS received", num_of_dup);
			out.write(line);
			out.write(SOF);
			out.write(ST);
			out.write(NOSHP);
			out.write(NOSD);
			out.write(NOSC);
			out.write(NOSR);
			out.write(NOSDUP);
			out.write(NOSDLY);
			out.write(NOSRT);
			out.write(NOFR);
			out.write(NODA);
			out.write(line);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Record statistic information into recv log file.
	 * @param filename: filename of recv log.
	 * @param amount_of_data_recv: Amount of data received (bytes).
	 * @param total_recv_seg: Total Segments Received.
	 * @param data_seg_recv: Data segments received.
	 * @param data_seg_error: Data segments with Bit Errors.
	 * @param dup_data_recv: Duplicate data segments received.
	 * @param dup_ack_sent:Duplicate ACKs sent.
	 */
	public static void recv_log(File filename, int amount_of_data_recv, int total_recv_seg, int data_seg_recv, int data_seg_error, int dup_data_recv,
            int dup_ack_sent){
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename, true)));
			String line = "==============================================\n";
			String AODR = String.format("%-35s%11s\n", "Amount of data received (bytes)", amount_of_data_recv);
			String TSR = String.format("%-35s%11s\n", "Total Segments Received", total_recv_seg);
			String DSR = String.format("%-35s%11s\n", "Data segments received", data_seg_recv);
			String DSBE = String.format("%-35s%11s\n", "Data segments with Bit Errors", data_seg_error);
			String DDSR = String.format("%-35s%11s\n", "Duplicate data segments received", dup_data_recv);
			String DAS = String.format("%-35s%11s\n", "Duplicate ACKs sent", dup_ack_sent);
			out.write(line);
			out.write(AODR);
			out.write(TSR);
			out.write(DSR);
			out.write(DSBE);
			out.write(DDSR);
			out.write(DAS);
			out.write(line);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
