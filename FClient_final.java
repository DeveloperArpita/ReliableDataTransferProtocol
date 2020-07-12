import java.net.*;
import java.io.*;
import java.util.*;
 
public class FClient_final {
	public static void main (String[] args) {

		final int CONSIGNMENT = 512; // max length of payload in bytes
		final String end_match = new String ("END\t\n");// Delimiter is \t\n

		String reply, msg = new String ("REQUEST" + args[2] + "\t\n");
		int port, trackNo = 1, seqLength = 1, end_pos,no=100;
		boolean end = false,flag=true,r=false,entry=false;
		
		DatagramSocket cs = null;
		FileOutputStream fos = null;
		DatagramPacket sp, rp;
		byte[] rd, sd;
		InetAddress ip;

		try {
	    		cs = new DatagramSocket();
			ip = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			
			// write received data into demoText1.html
			fos = new FileOutputStream ("demoPDF1.pdf");

			while (!end) {

				// send msg
				sd = msg.getBytes();	
			    	sp = new DatagramPacket (sd, sd.length, ip, port);
					// hardcoded to drop Ack 3
					if(trackNo!=3)
					{
					  cs.send(sp);
					}
					else if(r==true)
                {
                    cs.send(sp);
                }
					if( trackNo==3 && flag)
					{
					  flag=false;
					  Thread.sleep (3000);
					  System.out.println("ACK that is not present"+ trackNo);
					  r=true;
					}
					
				//cs.send(sp);
				if (msg.equals ("REQUEST" + args[2] + "\t\n"))
					System.out.println ("\nRequesting " + args[2] + " from " + ip + " port " + port + "\n");
				else if(trackNo!=3 ||entry==true)	
					System.out.println ("Sent ACK " + trackNo);

				// get next consignment
				rd = new byte [3 + CONSIGNMENT + seqLength + end_match.length()]; // length of ("RDT" + payload + seqNo + end_match)
				rp = new DatagramPacket (rd, rd.length); 
			    cs.receive(rp);	
			    reply = new String (rp.getData());
				
				seqLength = (int) Math.floor (Math.log10 (trackNo) + 1); // length of seqNo
				trackNo = Integer.parseInt (reply.substring (3, 3 + seqLength));
				//System.out.println ("Delivered CONSIGNMENT " + (trackNo++));
				if(no==trackNo)
				{
	                System.out.println("Delivered Consignment "+ trackNo +  " duplicate Frame-discraded Frame ");
                    entry=true;					
				}
				else
				System.out.println("\nDelivered CONSIGNMENT "+(trackNo));
				no=trackNo;
				trackNo++;
				
				end_pos = reply.lastIndexOf (end_match);

				// intermediate consignment
				if (end_pos == -1 ||	// when payload does not contain end_match
				    ((reply.length() - end_pos - end_match.length()) == 3) ||	// when payload ends with end_match, only 3 NULL values are left at the end since payload is always CONSIGNMENT bytes long
				    !(reply.lastIndexOf ("\t\n") < (end_pos + end_match.length())) )	// when payload contains end_match in between
				{
					msg = "ACK" + Integer.toString (trackNo) + "\t\n";
					fos.write (rp.getData(), 3 + seqLength, CONSIGNMENT);  // concat consignment
				}

				// last consignment
				else {
					end = true;
					fos.write (rd, 3 + seqLength, end_pos - 3 - seqLength);  // concat consignment
					System.out.println ("\nEND");
				}
			}
		}

		catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		catch(InterruptedException ex)
		{
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
}