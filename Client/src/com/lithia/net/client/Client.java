package com.lithia.net.client;

import static com.lithia.net.util.ReaderUtil.read;
import static com.lithia.net.util.WriterUtil.write;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JOptionPane;

import com.lithia.net.util.Logger;
import com.lithia.net.util.ReaderUtil;
import com.lithia.net.util.WriterUtil;

public class Client
{
	
	static Socket s;
	private static int id;
	private static String name;
	private static Scanner scan = new Scanner(System.in);
	
	private static String prefix = "Client";
	
	public static void main(String[] args) throws InterruptedException, IOException
	{
		ReaderUtil.debug = Boolean.parseBoolean(args[0]);
		WriterUtil.debug = Boolean.parseBoolean(args[0]);
		
		Logger.log(prefix, "Creating client...");
		
		try
		{
			int port = Integer.parseInt(JOptionPane.showInputDialog("Enter Port:"));
			Logger.log(prefix, "Connecting on port " + port + ".");
			s = new Socket(JOptionPane.showInputDialog("Enter IP:"), port);
			Logger.log(prefix, "Socket created, connection complete.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			Logger.log(prefix, "Listening for client assignment from server...");
			
			boolean[] chatting = new boolean[1];
			chatting[0] = false;
			while(!s.isClosed())
			{
				String line = read(s);
				
				if(line.length() < 1) continue;
				
				if(line.contains("chpr:"))
				{
					Logger.log(prefix, "Received client assignment from server!");
					name = line.substring(5, line.indexOf('|'));
					Logger.log(prefix, "Name=" + name);
					id = Integer.parseInt(line.substring(line.indexOf('|') + 1, line.length()));
					Logger.log(prefix, "Id=" + id);
					
					Logger.log(prefix, "Sending heartbeat...");
					write(s, line.substring(5, line.length()));
					Logger.log(prefix, "...heartbeat complete!");
				}
				
				if(line.contains("chat:"))
				{
					line = line.substring(5);
					String name = line.substring(0, line.indexOf(':'));
					String msg = line.substring(line.indexOf(':') + 1);
					
					Logger.log("Chat", name + " > " + msg);
				}
				
				if(line.contains("cnct:"))
				{
					String name = line.substring(5);
					
					Logger.log("Chat", name + " connected.");
				}
				
				if(!chatting[0])
				{
					chatting[0] = true;
					Runnable r = () ->
					{
						try
						{
							String out = "";
							while((out = scan.nextLine()).equals(""))
							{
							}
							
							write(s, name + "|" + id + ":chat:" + out);
							chatting[0] = false;
						}
						catch(IOException e)
						{
							e.printStackTrace();
						}
					};
					
					new Thread(r).start();
				}
				
				if(line.equals("abort"))
				{
					Logger.log(prefix, "Terminating at request of server...");
					write(s, "abort " + name + "|" + id);
					
					s.close();
				}
				
				Thread.sleep(50);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		scan.close();
	}
	
}
