package tigase.server.test;

import java.io.BufferedInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import tigase.server.AbstractMessageReceiver;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.util.TigaseStringprepException;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
 
public class TestAPI extends AbstractMessageReceiver {
 
  public TestAPI() {
	System.out.println("TestAPI instance");
	if(TestAPI.testAPI == null)
		TestAPI.testAPI = this;
	else
		System.out.println("TestAPI重复实例");
  }
	
  public static TestAPI testAPI;
  
  private static final Logger log =
    Logger.getLogger(TestAPI.class.getName());
 
  private static final String BAD_WORDS_KEY = "bad-words";
  private static final String WHITELIST_KEY = "white-list";
  private static final String PREPEND_TEXT_KEY = "log-prepend";
  private static final String SECURE_LOGGING_KEY = "secure-logging";
  private static final String ABUSE_ADDRESS_KEY = "abuse-address";
  private static final String NOTIFICATION_FREQ_KEY = "notification-freq";
 
  private String[] badWords = {"word1", "word2", "word3"};
  private String[] whiteList = {"admin@localhost"};
  private String prependText = "Spam detected: ";
  private String abuseAddress = "ceshi@test";
  private int notificationFrequency = 10;
  private int delayCounter = 0;
  private boolean secureLogging = false;
  private long spamCounter = 0;
 
  @Override
  public void processPacket(Packet packet) {
    // Is this packet a message?
    if ("message" == packet.getElemName()) {
    
      String from = JIDUtils.getNodeID(packet.getElemFrom());
      // Is sender on the whitelist?
      if (Arrays.binarySearch(whiteList, from) < 0) {
        // The sender is not on whitelist so let's check the content
        String body = packet.getElemCData("/message/body");
        if (body != null && !body.isEmpty()) {
          body = body.toLowerCase();
          for (String word : badWords) {
            if (body.contains(word)) {
              log.finest(prependText + packet.toString(secureLogging));
              ++spamCounter;
              return;
            }
          }
        }
      }
    }
    // Not a SPAM, return it for further processing
    Packet result = packet.swapStanzaFromTo();
    addOutPacket(result);
  }
 
  @Override
  public int processingInThreads() {
    return Runtime.getRuntime().availableProcessors();
  }
 
  @Override
  public int processingOutThreads() {
    return Runtime.getRuntime().availableProcessors();
  }
 
  @Override
  public int hashCodeForPacket(Packet packet) {
    if (packet.getStanzaTo() != null) {
      return packet.getStanzaTo().toString().hashCode();
    }
    // This should not happen, every packet must have a destination
    // address, but maybe our SPAM checker is used for checking
    // strange kind of packets too....
    if (packet.getStanzaFrom() != null) {
      return packet.getStanzaTo().toString().hashCode();
    }
    // If this really happens on your system you should look carefully
    // at packets arriving to your component and decide a better way
    // to calculate hashCode
    return 1;
  }
 
  @Override
  public Map<String, Object> getDefaults(Map<String, Object> params) {
    Map<String, Object> defs = super.getDefaults(params);
    defs.put(BAD_WORDS_KEY, badWords);
    defs.put(WHITELIST_KEY, whiteList);
    defs.put(PREPEND_TEXT_KEY, prependText);
    defs.put(SECURE_LOGGING_KEY, secureLogging);
    defs.put(ABUSE_ADDRESS_KEY, abuseAddress);
    defs.put(NOTIFICATION_FREQ_KEY, notificationFrequency);
    return defs;
  }
 
  @Override
  public void setProperties(Map<String, Object> props) {
    super.setProperties(props);
    badWords = (String[])props.get(BAD_WORDS_KEY);
    whiteList = (String[])props.get(WHITELIST_KEY);
    Arrays.sort(whiteList);
    prependText = (String)props.get(PREPEND_TEXT_KEY);
    secureLogging = (Boolean)props.get(SECURE_LOGGING_KEY);
    abuseAddress = (String)props.get(ABUSE_ADDRESS_KEY);
    notificationFrequency = (Integer)props.get(NOTIFICATION_FREQ_KEY);
  }
 
//  @Override
//  public synchronized void everyMinute() {
//	System.out.println("到达1分钟了.....................");
//    super.everyMinute();
//    if ((++delayCounter) >= notificationFrequency) {
//      try {
////    	  	Message.getMessage(from, to, type, body, subject, thread, id)
////    	  	Packet packet = Packet.packetInstance("message","liqiang@test", abuseAddress, StanzaType.chat);
//  	  	JID from = JID.jidInstance("admin", "test", null);
//	  	JID to = JID.jidInstance("ceshi", "test", null);
//    	  	Packet packet = Message.getMessage(from, to, StanzaType.chat, "Detected spam messages: " + spamCounter,"Spam counter", null, newPacketId("spam-"));
//		addOutPacket(packet);
//	} catch (TigaseStringprepException e) {
//		e.printStackTrace();
//	}
//      delayCounter = 0;
//      spamCounter = 0;
//    }
//  }
  /*
	@Override
	public synchronized void everySecond() {
		super.everyMinute();
		if(++delayCounter < 10)
			return;
		try {
			JID from = JID.jidInstance("admin", "test", null);
			JID to = JID.jidInstance("ceshi", "test", null);
			if (delayCounter == 10) {
//				System.out.println("到达10s了.....................");
				// Message.getMessage(from, to, type, body, subject, thread, id)
				Packet packet = Message.getMessage(from, to, StanzaType.chat,
						"Admin send a message To u" + spamCounter,
						"Spam counter", null, newPacketId("spam-"));
				addOutPacket(packet);
			} else if (delayCounter == 20) {
//				System.out.println("到达20s了.....................");
				Packet packet = Message.getMessage(to, from, StanzaType.chat,
						"测试", "Test Sender", null, newPacketId("spam-"));
				addOutPacket(packet);
				delayCounter = 0;
			}
		} catch (TigaseStringprepException e) {
			e.printStackTrace();
		}
	}
	*/
  
	public static boolean sendMessage(String to, String from, String subject, String message, String domain) {
		TestAPI component = TestAPI.testAPI;
		if(component == null) {
			throw new NullPointerException();
		}
		try {
			JID fromJID = JID.jidInstance(from, domain, null);
			JID toJID = JID.jidInstance(to, domain, null);
			Packet packet = Message.getMessage(fromJID, toJID, StanzaType.chat, message,
					subject, null, component.newPacketId("testAPI-"));
			component.addOutPacket(packet);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
//	static {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(15000);
//				} catch(Exception e) {
//					e.printStackTrace();
//				}
//				BufferedInputStream bis = new BufferedInputStream(System.in);
//				Scanner cin = new Scanner(bis,"GBK");
//				while(true) {
//					System.out.println("------------请您输入信息------------");
//					String text = cin.nextLine();
//					String text1 = null;
//					try {
//						text1 = new String(text.getBytes(), "UTF-8");
//					} catch (UnsupportedEncodingException e) {
//						e.printStackTrace();
//					}
//					String[] string = text1.split(",");
//					String to = string[0];
//					String from = string[1];
//					String subject = string[2];
//					String message = string[3];
//					String domain = string[4];
//					TestAPI.sendMessage(to, from, subject, message, domain);
//					System.out.println("------------发送成功------------");					
//				}
//			}
//		}).start();
//	}
}
