/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.net;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.net.InetSocketAddress;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class ConnectionOpenThread here.
 *
 *
 * Created: Wed Jan 25 23:51:28 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConnectionOpenThread implements Runnable {
	private static final Logger log = Logger.getLogger(ConnectionOpenThread.class.getName());
	private static ConnectionOpenThread acceptThread = null;

	/**
	 * 设置每个端口每1000ms连接次数阈值
	 */
	public static final long def_5222_throttling = 200;

	/** Field description */
	public static final long def_5223_throttling = 50;

	/** Field description */
	public static final long def_5280_throttling = 1000;

	/** Field description */
	public static final long def_5269_throttling = 100;

	/** 管理各端口当前秒的连接数值,和该端口的每秒连接阈值.
	 * 上述信息存储在PortThrottlingData实例中,这个变量指向存储PortThrottlingData实例的Map */
	public static Map<Integer, PortThrottlingData> throttling = new ConcurrentHashMap<Integer,
		PortThrottlingData>(10);

	//~--- fields ---------------------------------------------------------------

	// 连接计数器,但是目前仅为计数没有读取使用
	protected long accept_counter = 0;
	private Selector selector = null;
	private boolean stopping = false;
	private Timer timer = null;
	private ConcurrentLinkedQueue<ConnectionOpenListener> waiting =
		new ConcurrentLinkedQueue<ConnectionOpenListener>();

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>ConnectionOpenThread</code> instance.
	 *
	 */
	private ConnectionOpenThread() {
		// 该类为单例,所以构造器的内容会随着单例init时执行
		timer = new Timer("Connections open timer", true);
		// 定时器,每1秒将所有端口的连接计数数值清0.
		// PS:这个数值一旦超出阈值,则不会处理该端口的新请求.
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				for (PortThrottlingData portData : throttling.values()) {
					portData.lastSecondConnections = 0;
				}
			}
		}, 1000, 1000);

		try {
			// 这是nio通信机制的根源,获取作为server的唯一的selector
			selector = Selector.open();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Server I/O error, can't continue my work.", e);
			stopping = true;
		}    // end of try-catch
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * 获取一个ConnectionOpenThread实例
	 *
	 *
	 * @return
	 */
	public static ConnectionOpenThread getInstance() {

		// Long new_throttling = Long.getLong("new-connections-throttling");
//  if (new_throttling != null) {
//    throttling = new_throttling;
//    log.log(Level.WARNING, "New connections throttling set to: {0}", throttling);
//  }
		if (acceptThread == null) {
			acceptThread = new ConnectionOpenThread();
			acceptThread.start();

			Thread thrd = new Thread(acceptThread);

			thrd.setName("ConnectionOpenThread");
			thrd.start();

			if (log.isLoggable(Level.FINER)) {
				log.finer("ConnectionOpenThread started.");
			}
		}    // end of if (acceptThread == null)

		return acceptThread;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * 将连接监听器添加到处理队列中
	 *
	 * @param al
	 */
	public void addConnectionOpenListener(ConnectionOpenListener al) {
		waiting.offer(al);
		selector.wakeup();
	}

	/**
	 * 根据attach到channel的ConnectionOpenListener实例注销SelectionKey
	 *
	 *
	 * @param al
	 */
	public void removeConnectionOpenListener(ConnectionOpenListener al) {
		for (SelectionKey key : selector.keys()) {
			if (al == key.attachment()) {
				try {
					key.cancel();

					SelectableChannel channel = key.channel();

					channel.close();
				} catch (Exception e) {
					log.log(Level.WARNING, "Exception during removing connection listener.", e);
				}

				break;
			}
		}
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void run() {
		while ( !stopping) {
			try {
				// 获取当前可以处理的keys,也就是准备好了的Channel
				selector.select(); // may blocking

				Set<SelectionKey> keys = selector.selectedKeys();
				
				// Set<SelectionKey> selected_keys = selector.selectedKeys();
				// for (SelectionKey sk : selected_keys) {
				for (Iterator i = selector.selectedKeys().iterator(); i.hasNext(); ) {
					SelectionKey sk = (SelectionKey) i.next();

					// 从集合中删除要处理的SelectionKey
					i.remove();

					SocketChannel sc = null;

					if ((sk.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						ServerSocketChannel nextReady = (ServerSocketChannel) sk.channel();

						// 从这个ServerSocketChannel中获得SocketChannel
						sc = nextReady.accept();

						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_ACCEPT");
						}
						
						// 每个端口都有每秒内连接的数量限制,获取该端口的阈值和连接情况
						PortThrottlingData port_throttling = throttling.get(nextReady.socket().getLocalPort());

						if (port_throttling != null) {
							++port_throttling.lastSecondConnections;

							// 根据这个PortThrottlingDat(端口阈值Obj)获取一下当前值是否超出阈值
							// PS:这个值每秒清0一次
							if (port_throttling.lastSecondConnections > port_throttling.throttling) {
								if (log.isLoggable(Level.FINER)) {
									log.log(Level.FINER, "New connections throttling level exceeded, closing: {0}",
											sc);
								}

								sc.close();
								sc = null;
							}
						} else {
							// 汗....
							// Hm, this should not happen actually
							log.log(Level.WARNING, "Throttling not configured for port: {0}",
									nextReady.socket().getLocalPort());
						}
					}    // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)

					if ((sk.readyOps() & SelectionKey.OP_CONNECT) != 0) {
						
						System.out.println("ConnectionOpenThread.run(): will connect to Cluster............................");
						
						sk.cancel();
						sc = (SocketChannel) sk.channel();

						if (log.isLoggable(Level.FINEST)) {
							log.finest("OP_CONNECT");
						}
					}    // end of if (sk.readyOps() & SelectionKey.OP_ACCEPT)

					if (sc != null) {

						// We have to catch exception here as sometimes socket is closed
						// or connection is broken before we start configuring it here
						// then whatever we do on the socket it throws an exception
						try {
							sc.configureBlocking(false);
							sc.socket().setSoLinger(false, 0);
							sc.socket().setReuseAddress(true);

							if (log.isLoggable(Level.FINER)) {
								log.log(Level.FINER, "Registered new client socket: {0}", sc);
							}

							ConnectionOpenListener al = (ConnectionOpenListener) sk.attachment();

							sc.socket().setTrafficClass(al.getTrafficClass());
							sc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
							al.accept(sc);
						} catch (java.net.SocketException e) {
							log.log(Level.INFO, "Socket closed instantly after it had been opened?", e);

							ConnectionOpenListener al = (ConnectionOpenListener) sk.attachment();

							al.accept(sc);
						}
					} else {
						log.warning("Can't obtain socket channel from selection key.");
					}    // end of if (sc != null) else

					++accept_counter;
				}

				addAllWaiting();
			} catch (IOException e) {
				log.log(Level.SEVERE, "Server I/O error.", e);

				// stopping = true;
			}        // end of catch
					catch (Exception e) {
				log.log(Level.SEVERE, "Other service exception.", e);

				// stopping = true;
			}        // end of catch
		}
	}

	/**
	 * Method description
	 *
	 */
	public void start() {
		Thread t = new Thread(this);

		t.setName("ConnectionOpenThread");
		t.start();
	}

	/**
	 * Method description
	 *
	 */
	public void stop() {
		stopping = true;
		selector.wakeup();
	}

	/**
	 * 将waiting队列中等待的连接注册到Selector
	 * @throws IOException
	 */
	private void addAllWaiting() throws IOException {
		ConnectionOpenListener al = null;

		while ((al = waiting.poll()) != null) {
			try {
				addPort(al);
			} catch (Exception e) {
				log.log(Level.WARNING, "Error: creating connection for: " + al, e);
			}    // end of try-catch
		}      // end of for ()
	}

	/**
	 * 根据连接监听器创建连接,并注册到Selector.
	 * @param isa 要bind/connect的IP地址
	 * @param al 要添加的连接的监听器,也就是attachment
	 * @throws IOException
	 */
	private void addISA(InetSocketAddress isa, ConnectionOpenListener al) throws IOException {
		System.out.println("addISA is running..................");
		// 根据监听器的配置是server还是client
		switch (al.getConnectionType()) {
			case accept :
				long port_throttling = getThrottlingForPort(isa.getPort());

				throttling.put(isa.getPort(), new PortThrottlingData(port_throttling));

				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST,
							"Setting up throttling for the port {0} to {1} connections per second.",
								new Object[] { isa.getPort(),
							port_throttling });
				}

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Setting up 'accept' channel...");
				}

				ServerSocketChannel ssc = ServerSocketChannel.open();

				ssc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
				ssc.configureBlocking(false);
				ssc.socket().bind(isa);
				// 注册到ServerSocketChannel,将tigase.net.ConnectionOpenListener实例作为attachment
				// ConnectionOpenListener是处理网络数据的实例,即accept方法
				ssc.register(selector, SelectionKey.OP_ACCEPT, al);

				break;

			case connect :
				System.out.println("ConnectionOpenThread:360 addTask connect to Host: "+isa.getAddress()+":"+isa.getPort());
				
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Setting up ''connect'' channel for: {0}/{1}",
							new Object[] { isa.getAddress(),
							isa.getPort() });
				}

				SocketChannel sc = SocketChannel.open();

				sc.socket().setReceiveBufferSize(al.getReceiveBufferSize());
				sc.socket().setTrafficClass(al.getTrafficClass());
				sc.configureBlocking(false);
				sc.connect(isa);
				// 注册到ServerSocketChannel,将tigase.net.ConnectionOpenListener实例作为attachment
				// ConnectionOpenListener是处理网络数据的实例,即accept方法
				sc.register(selector, SelectionKey.OP_CONNECT, al);

				break;

			default :
				log.log(Level.WARNING, "Unknown connection type: {0}", al.getConnectionType());

				break;
		}    // end of switch (al.getConnectionType())
	}

	private void addPort(ConnectionOpenListener al) throws IOException {
		if ((al.getIfcs() == null) || (al.getIfcs().length == 0) || al.getIfcs()[0].equals("ifc")
				|| al.getIfcs()[0].equals("*")) {
			addISA(new InetSocketAddress(al.getPort()), al);
		} else {
			for (String ifc : al.getIfcs()) {
				addISA(new InetSocketAddress(ifc, al.getPort()), al);
			}    // end of for ()
		}      // end of if (ip == null || ip.equals("")) else
	}

	//~--- get methods ----------------------------------------------------------

	private long getThrottlingForPort(int port) {
		long result = def_5222_throttling;

		switch (port) {
			case 5223 :
				result = def_5223_throttling;

				break;

			case 5269 :
				result = def_5269_throttling;

				break;

			case 5280 :
				result = def_5280_throttling;

				break;
		}

		String throttling_prop = System.getProperty("new-connections-throttling");

		if (throttling_prop != null) {
			String[] all_ports_thr = throttling_prop.split(",");

			for (String port_thr : all_ports_thr) {
				String[] port_thr_ar = port_thr.split(":");

				if (port_thr_ar.length == 2) {
					try {
						int port_no = Integer.parseInt(port_thr_ar[0]);

						if (port_no == port) {
							return Long.parseLong(port_thr_ar[1]);
						}
					} catch (Exception e) {

						// bad configuration
						log.log(Level.WARNING,
								"Connections throttling configuration error, bad format, "
									+ "check the documentation for a correct syntax, "
										+ "port throttling config: {0}", port_thr);
					}
				} else {

					// bad configuration
					log.log(Level.WARNING, "Connections throttling configuration error, bad format, "
							+ "check the documentation for a correct syntax, "
								+ "port throttling config: {0}", port_thr);
				}
			}
		}

		return result;
	}

	//~--- inner classes --------------------------------------------------------

	/** 
	 * 每个端口的单位时间接收连接次数实体类,目前使用定时器每秒清0一次
	 * 
	 */
	private class PortThrottlingData {

		/** 这一秒内的连接次数,这个数值会不断增加,每秒由定时器清0一次 */
		protected long lastSecondConnections = 0;

		/** 该端口的每秒连接阈值,这个数值在注册ACCEPT的KEY到Selector时指定 */
		protected long throttling;

		//~--- constructors -------------------------------------------------------

		/**
		 * Constructs ...
		 *
		 *
		 * @param throttling_prop
		 */
		private PortThrottlingData(long throttling_prop) {
			throttling = throttling_prop;
		}
	}
}    // ConnectionOpenThread


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
