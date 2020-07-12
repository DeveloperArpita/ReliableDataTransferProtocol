import java.net.*;
import java.io.*;
import java.util.*;


public class FClient159 {
	
	public static final byte[] ACK = new String ("ACK").getBytes();
    	public static final byte[] CRLF = new String ("\r\n").getBytes();
	
	public static void main (String[] args) {

		final int CONSIGNMENT = 512; // max length of payload in bytes
		final int maxACKs = 3;
		final String end_match = new String ("END\r\n");

		String reply;
		int i, port, seqLength = 1, seqNo = 1, prev_seqNo = -1, end_pos, dropACKs[] = new int [maxACKs];
		boolean sentRequest = false, end = false;
		
		DatagramSocket cs = null;
		FileOutputStream fos = null;
		DatagramPacket sp, rp;
		byte[] rd, sd = new String ("REQUEST" + args[2] + "\r\n").getBytes();
		InetAddress ip;
		byte c;
		for (i = 0; i < args.length - 3 && i < maxACKs; i++)
			dropACKs[i] = Integer.parseInt (args[3 + i]);
		for (;i < maxACKs; i++) 
			dropACKs[i] = -1;
		i = 0;

		try {
	    		cs = new DatagramSocket();
			ip = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			
			// write received data into filenameReceived.extension
			end_pos = args[2].lastIndexOf (".");
			fos = new FileOutputStream (args[2].substring (0, end_pos) + "Received" + args[2].substring (end_pos));

			while (true) {
				// send msg
			    	sp = new DatagramPacket (sd, sd.length, ip, port);
	  
				// hard-coded to drop the specified ACKs once
				if (i < dropACKs.length && dropACKs[i] != -1 && seqNo == dropACKs[i] && sentRequest) { // ensures that the 1st request is not dropped
					i++;
					System.out.println ("Forgot ACK " + seqNo);
				}
				else {
					//if (i < dropACKs.length && dropACKs[i] == 1)
						//i++;
					cs.send (sp);
					if (new String (sd).equals ("REQUEST" + args[2] + "\r\n")) {
						System.out.println ("\nRequesting " + args[2] + " from " + ip + " port " + port);
						sentRequest = true;
					}
					else {
						if (end)
							seqNo = 0;
						System.out.println ("Sent ACK " + seqNo);
						if (seqNo == 0) {
							try {
								Thread.sleep (500);
							}
							catch (InterruptedException ex) {
								System.out.println(ex.getMessage());
							}
							System.out.println ("\nEND");
							break;
						}
					}
				}

				// get next consignment
				rd = new byte [3 + CONSIGNMENT + seqLength + end_match.length()]; // length of ("RDT" + payload + seqNo + end_match)
				rp = new DatagramPacket (rd, rd.length); 
			    	cs.receive (rp);	
				port = rp.getPort();
				ip = rp.getAddress();
			    	reply = new String (rp.getData());
				
				seqNo = (rd[ACK.length]);
				c= (byte)seqNo;
				
				if (seqNo == prev_seqNo) {
					System.out.println ("Received CONSIGNMENT " + (seqNo++) + " duplicate - discarding");
					if (prev_seqNo == -1)
						seqNo = prev_seqNo + 1;
					continue;
				}

				prev_seqNo = seqNo;
				System.out.println ("\nReceived CONSIGNMENT " + (seqNo++));
				end_pos = reply.lastIndexOf (end_match);

				// intermediate consignment
				if (end_pos == -1 ||	// when payload does not contain end_match
				    ((reply.length() - end_pos - end_match.length()) == 3) ||	// when payload ends with end_match, only 3 NULL values are left at the end since payload is always CONSIGNMENT bytes long
				    !(reply.lastIndexOf ("\r\n") < (end_pos + end_match.length())) )	// when payload contains end_match in between
					fos.write (rp.getData(), 3 + seqLength, CONSIGNMENT);  // concat consignment

				// last consignment
				else {
					end = true;
					seqNo = 0;
					fos.write (rd, 3 + seqLength, end_pos - 3 - seqLength);  // concat consignment
				}
				sd = prepareMsg (new byte [] {(byte)seqNo});
			}
		}

		catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	
		finally {
			try {
				if (fos != null)
					fos.close();
				if (cs != null)
					cs.close();
			}
			catch (IOException ex) {
				System.out.println (ex.getMessage());
			}
		}

	}

	public static byte[] prepareMsg (byte[] seq) {
        	byte[] result = new byte [ACK.length + seq.length + CRLF.length]; 
        	System.arraycopy (ACK, 0, result, 0, ACK.length); 
        	System.arraycopy (seq, 0, result, ACK.length, seq.length);
        	System.arraycopy (CRLF, 0, result, ACK.length + seq.length, CRLF.length);
        	return result;
    	}
}