package com.lithia.net.server;

import static com.lithia.net.util.ReaderUtil.read;
import static com.lithia.net.util.WriterUtil.write;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import com.lithia.net.util.Logger;
import com.lithia.net.util.WriterUtil;

public class ClientHandler
{

	private static ClientHandler instance;
	private Vector<Client> clients;
	
	private static String prefix = "Client Handler";
	
	private ClientHandler()
	{
		clients = new Vector<>();
	}

	public static ClientHandler getInstance()
	{
		if (instance == null) instance = new ClientHandler();

		return instance;
	}

	public Client addClient(int id, Socket socket, String name)
	{
		Client client = new Client(name, socket, id);
		client.handler = this;

		clients.add(client);
		return client;
	}

	public static class Client implements Runnable
	{
		private static int index;

		private String name;
		private Socket socket;
		private int id;
		private volatile boolean active;
		private ClientHandler handler;
		
		BufferedReader in;
		PrintWriter out;

		public Client(String name, Socket socket, int id)
		{
			Logger.log(prefix, "Generating client token...");
			this.name = name;
			this.socket = socket;
			this.id = id;

			active = true;
			Logger.log(prefix, "...complete!");
		}

		public void run()
		{
			try
			{
				handshake(this);
				
				for(Client c : handler.clients)
				{
					if(WriterUtil.debug) Logger.log(prefix, "Writing to " + c.name + "'s socket.");
					write(c.socket, "cnct:" + name);
				}
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			Logger.log(prefix, "Client initialized.");
			
			int tick = 0;
			try
			{
				Logger.log(prefix, "Listening on client socket...");
				
				while (active)
				{
					String line = read(socket);
					
					if(tick == 0) write(socket, "ymc");

					if(line.length() > 0) Logger.log(prefix, "Received: [" + line + "]");

					if (line.contains("abort"))
					{
						Logger.log(prefix, "Client " + line.substring(6) + " disconnected.");
						socket.close();
					}
					
					if(line.contains(":chat:"))
					{
						String name = line.substring(0, line.indexOf('|'));
						String msg = line.substring(line.indexOf(':') + 6);
						
						for(Client c : handler.clients)
						{
							if(WriterUtil.debug) Logger.log(prefix, "Writing to " + c.name + "'s socket.");
							write(c.socket, "chat:" + name + ":" + msg);
						}
					}
					
					if(socket.isClosed()) active = false;
					
					Thread.sleep(50);
					tick++;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			Logger.log(prefix, "Client with id " + id + " terminated.");
		}

		public static int generateNewClientID()
		{
			return index++;
		}
		
		public void setHandler(ClientHandler handler)
		{
			this.handler = handler;
		}
		
		public String getName()
		{
			return name;
		}

		public int getID()
		{
			return id;
		}

	}

	private static void handshake(Client c) throws InterruptedException, IOException
	{
		Socket s = c.socket;

		Logger.log(prefix, "Creating handshake: chpr:" + c.name + "|" + c.id);
		
		write(s, "chpr:" + c.name + "|" + c.id);

		String line;
		while ((line = read(s)) == null || !line.equals(c.name + "|" + c.id))
		{
			Thread.sleep(20);
		}
		
		Logger.log(prefix, "Received heartbeat, handshake complete.");
		Logger.log(prefix, line + " connected.");
	}

}
