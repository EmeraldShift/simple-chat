package com.lithia.net.client;

import static com.lithia.net.util.ReaderUtil.*;
import static com.lithia.net.util.WriterUtil.*;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import com.lithia.net.util.*;

public class Client
{
	
	static Socket socket;
	private static int id;
	private static String name;
	private static Scanner scan = new Scanner(System.in);
	private static Thread chatThread;
	
	private static String prefix = "Client";
	
	public static void main(String[] args)
	{
		ReaderUtil.debug = Boolean.parseBoolean(args[0]);
		WriterUtil.debug = Boolean.parseBoolean(args[0]);
		
		String ip = null;
		int port = 0;
		
		try
		{
			ip = args[1];
			port = Integer.parseInt(args[2]);
		}
		catch(Exception e)
		{
		}
		
		Logger.log(prefix, "Creating client...");
		
		try
		{
			if(ip == null) ip = JOptionPane.showInputDialog("Enter IP:");
			if(port == 0) port = Integer.parseInt(JOptionPane.showInputDialog("Enter Port:"));
			Logger.log(prefix, "Connecting to " + ip + " on port " + port + ".");
			socket = new Socket(ip, port);
			Logger.log(prefix, "Socket created, connection complete.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			Logger.log(prefix, "Listening for client assignment from server...");
			
			boolean active = true;
			long pingTime = System.currentTimeMillis();
			long pongTime = System.currentTimeMillis();
			
			Runnable r = () ->
			{
				try
				{
					String chat;
					while((chat = scan.nextLine()) != null)
					{
						write(socket, name + "|" + id + ":chat:" + chat);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			};
			
			chatThread = new Thread(r, "Chat");
			chatThread.start();
			
			while(active)
			{
				String line = read(socket);
				
				if(line.contains("chpr:"))
				{
					Logger.log(prefix, "Received client assignment from server!");
					id = Integer.parseInt(line.substring(5));
					Logger.log(prefix, "Joined server with id " + id + ".");
					
					name = JOptionPane.showInputDialog("Desired client name:");
					if(name.length() == 0) name = "Anonymous";

					Logger.log(prefix, "Set name to " + name + ".");
					
					write(socket, name);
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
				
				if(line.contains("chdc:"))
				{
					String name = line.substring(5);
					Logger.log("Chat", name + " disconnected.");
				}
				
				if(line.equals("abort"))
				{
					Logger.log(prefix, "Terminating at request of server...");
					write(socket, "abort " + name + "|" + id);
					
					active = false;
				}
				
				if(System.currentTimeMillis() - pingTime > 5000)
				{
					write(socket, "ping");
					pingTime = System.currentTimeMillis();
				}
				
				if(line.equals("pong"))
				{
					pongTime = System.currentTimeMillis();
				}
				
				if(System.currentTimeMillis() - pongTime > 15000)
				{
					Logger.log(prefix, "Server is not responding...");
					Logger.log(prefix, "Client will now exit.");
					
					active = false;
				}
				
				if(socket.isClosed() || !socket.isConnected() || !socket.isBound()) active = false;
				
				Thread.sleep(50);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if(socket != null) socket.close();
				chatThread.join();
			}
			catch (IOException | InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		
		scan.close();
		System.exit(0);
	}
}
