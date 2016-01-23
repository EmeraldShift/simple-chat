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

	public Client addClient(int id, Socket socket)
	{
		Client client = new Client(socket, id);
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

		public Client(Socket socket, int id)
		{
			Logger.log(prefix, "Generating client token...");
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
			
			try
			{
				Logger.log(prefix, "Listening on client socket...");
				
				while (active)
				{
					String line = read(socket);
					
					if(line.contains("name:"))
					{
						this.name = line.substring(5);
					}
					
					if (line.contains("abort"))
					{
						Logger.log(prefix, "Client " + line.substring(6) + " disconnected.");
						socket.close();
					}
					
					if(line.equals("ping"))
					{
						write(socket, "pong");
						if(WriterUtil.debug) Logger.log("Ping", "Received ping from " + name + ".");
					}
					
					if(line.contains(":chat:"))
					{
						String name = line.substring(0, line.indexOf('|'));
						String msg = line.substring(line.indexOf(':') + 6);
						
						if(msg.equals("quit"))
						{
							active = false;
							write(socket, "quit");
							continue;
						}

						for(Client c : handler.clients)
						{
							if(WriterUtil.debug) Logger.log(prefix, "Writing to " + c.name + "'s socket.");
							write(c.socket, "chat:" + name + ":" + msg);
						}
					}
					
					
					if(socket.isClosed() || !socket.isConnected() || !socket.isBound()) active = false;
					
					Thread.sleep(50);
				}
				
				for(Client c : handler.clients)
				{
					write(c.socket, "chdc:" + name);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					socket.close();
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
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

		Logger.log(prefix, "Creating handshake: chpr:" + c.id);
		
		write(s, "chpr:" + c.id);

		String line;
		while ((line = read(s)).length() == 0)
		{
			Thread.sleep(1000);
		}
		
		c.name = line;
		
		Logger.log(prefix, "Received heartbeat, handshake complete.");
		Logger.log(prefix, line + " connected.");
	}

}
