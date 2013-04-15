/*
 * @(#)XMPPServer.java   2010.01.15 at 08:51:06 PST
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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

package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import com.yamma.api.WebAPI;

import tigase.conf.ConfigurationException;
import tigase.conf.ConfiguratorAbstract;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class XMPPServer here.
 *
 *
 * Created: Wed Nov 23 07:04:18 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public final class XMPPServer {

	/** Field description */
	@SuppressWarnings("PMD")
	public static final String  CONFIGURATOR_PROP_KEY = "tigase-configurator";
	private static final String DEF_CONFIGURATOR      = "tigase.conf.Configurator";

	/** Field description */
	public static final String NAME       = "Tigase";
	private static String      serverName = "message-router";

	//~--- constructors ---------------------------------------------------------

//private static boolean     monit         = false;
//private static boolean     debug         = false;
//private static String      tigaseVersion = null;
//private static boolean     genConfig     = false;

	/**
	 * Creates a new <code>XMPPServer</code> instance.
	 *
	 */
	private XMPPServer() {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public static String getImplementationVersion() {
		String version = XMPPServer.class.getPackage().getImplementationVersion();

		return (version == null) ? "0.0.0-0" : version;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public static String help() {
		return "\n" + "Parameters:\n" + " -h               this help message\n"
					 + " -v               prints server version info\n"
					 + " -d [true|false]    turn on|off debug mode\n"
					 + " -m               turn on server monitor\n"
					 + " -n server-name    sets server name\n"
		;
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	@SuppressWarnings("PMD")
	public static void main(final String[] args) {

		// getImplementationVersion();
		Thread.setDefaultUncaughtExceptionHandler(new ThreadExceptionHandler());
		// 解析简单参数 -h/-v/-n
		parseParams(args);

		String initial_config =
			"tigase.level=ALL\n" + "tigase.xml.level=INFO\n"
			+ "handlers=java.util.logging.ConsoleHandler\n"
			+ "java.util.logging.ConsoleHandler.level=ALL\n"
			+ "java.util.logging.ConsoleHandler.formatter=tigase.util.LogFormatter\n"
		;

		ConfiguratorAbstract.loadLogManagerConfig(initial_config);

		/* 整个启动过程,为了初始化整个系统所以一直在传递ConfiguratorAbstract类型的实例 */
		try {
			// 这个软件中配置类只有两个实现,默认使用tigase.conf.Configurator
			String config_class_name = System.getProperty(CONFIGURATOR_PROP_KEY,
							DEF_CONFIGURATOR);

			ConfiguratorAbstract config = (ConfiguratorAbstract) Class.forName(config_class_name).newInstance();
			config.init(args);

			// config = new ConfiguratorOld(config_file, args);
			config.setName("basic-conf");

			// MessageRouter类是tigase.server.MessageRouter
			String mr_class_name = config.getMessageRouterClassName();
			// MessageRouterIfc是一个接口
			MessageRouterIfc router = (MessageRouterIfc) Class.forName(mr_class_name).newInstance();
			
			router.setName(serverName);
			router.setConfig(config);
			router.start();
			
			// 启动WebAPI的Service
			WebAPI.startWebAPIService();
		} catch (ConfigurationException e) {
			System.err.println("");
			System.err.println("  --------------------------------------");
			System.err.println("  ERROR! Terminating the server process.");
			System.err.println("  Invalid configuration data: " + e);
			System.err.println("  Please fix the problem and start the server again.");
			System.exit(1);
		} catch (Exception e) {
			System.err.println("");
			System.err.println("  --------------------------------------");
			System.err.println("  ERROR! Terminating the server process.");
			System.err.println("  Problem initializing the server: " + e);
			System.err.println("  Please fix the problem and start the server again.");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Method description
	 *
	 * 解析简单参数-h/-v/-n
	 *
	 * @param args
	 */
	@SuppressWarnings("PMD")
	public static void parseParams(final String[] args) {
		if ((args != null) && (args.length > 0)) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-h")) {
					System.out.print(help());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-v")) {
					System.out.print(version());
					System.exit(0);
				}      // end of if (args[i].equals("-h"))

				if (args[i].equals("-n")) {
					if (i + 1 == args.length) {
						System.out.print(help());
						System.exit(1);
					}    // end of if (i+1 == args.length)
									else {
						serverName = args[++i];
					}    // end of else
				}      // end of if (args[i].equals("-h"))

//      if (args[i].equals("-d")) {
//        if (i + 1 == args.length) {
//          debug = true;
//        }    // end of if (i+1 == args.length)
//                else {
//          ++i;
//          debug = (args[i].charAt(0) != '-')
//                  && (args[i].equals("true") || args[i].equals("yes"));
//        }    // end of else
//      }      // end of if (args[i].equals("-d"))
//
//      if (args[i].equals("-m")) {
//        monit = true;
//      }      // end of if (args[i].equals("-m"))
//
//      if (args[i].startsWith("--gen-config")) {
//        genConfig = true;
//      }
			}        // end of for (int i = 0; i < args.length; i++)
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public static String version() {
		return "\n" + "-- \n" + NAME + " XMPP Server, version: " + getImplementationVersion()
					 + "\n" + "Author:  Artur Hefczyc <artur.hefczyc@tigase.org>\n" + "-- \n"
		;
	}
}    // XMPPServer


//~ Formatted in Sun Code Convention on 2010.01.15 at 08:51:06 PST


//~ Formatted by Jindent --- http://www.jindent.com
