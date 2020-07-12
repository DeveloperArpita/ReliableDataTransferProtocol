import java.net.*;
import java.io.*;
import java.util.*;
 
public class SecretClient {
 
	public static void main(String[] args) {
	 
	    DatagramSocket cs = null;

		try {
			cs = new DatagramSocket();

			byte[] rd, sd;
			String GREETING = "REQUESTHELLOCRLF";
			String reply;
			DatagramPacket sp,rp;
			boolean end = false;
			int track=1;

			while(!end)
			{   	  
				// send Greeting      
			    sd=GREETING.getBytes();	 
			    sp=new DatagramPacket(sd,sd.length, 
									InetAddress.getByName(args[0]),
  									Integer.parseInt(args[1]));	 
				cs.send(sp);	
				System.out.println("sent"+GREETING);

				// get next consignment
				rd=new byte[512];
				rp=new DatagramPacket(rd,rd.length); 
			    cs.receive(rp);	

				// print SECRET
				reply=new String(rp.getData());	 
				System.out.println(reply);
				track=Integer.parseInt(reply.substring(3,3+(int)Math.floor(Math.log10(track)+1)))+1;

				if (reply.trim().contains("END")) // last consignment
					end = true;
				else 
					GREETING="ACK"+ Integer.toString(track++)+"CRLF";

			}
		 
			cs.close();

		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
	}
 
}