package com.yamma.api;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import tigase.server.test.TestAPI;

public class WebAPI extends AbstractHandler
{
	private String path;
	
	public WebAPI(String path) {
		this.path = path;
	}
	
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) 
        throws IOException, ServletException
    {
        response.setContentType("text/html;charset=utf-8");
        baseRequest.setHandled(true);
        if(this.path.equals("sendNotify")) {
        		response.setStatus(HttpServletResponse.SC_OK);
//        		request.setCharacterEncoding("UTF-8");
        		String notify = request.getParameter("notify");
        		String receiver = request.getParameter("receiver");
        		if(notify == null || "".equals(notify) 
        				|| receiver == null || "".equals(receiver)) {
        			response.getWriter().print("false");
        			return;
        		}
        		System.out.println("Notify is "+notify+" and receiver is "+receiver);
        		TestAPI.sendMessage(receiver, "admin", "Test", notify, "test");
        		response.getWriter().print("true");
        } else {
        		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    public static void startWebAPIService() throws Exception {
        Server server = new Server(6666);
        // path:/sn
        ContextHandler sendNotifyHandler = new ContextHandler();
        sendNotifyHandler.setContextPath("/pushMsg");
        sendNotifyHandler.setHandler(new WebAPI("sendNotify"));
        // path:/gi
        ContextHandler getInfoHandler = new ContextHandler();
        getInfoHandler.setContextPath("/gi");
        getInfoHandler.setHandler(new WebAPI("/getInfo"));
        
        // 注意:这里所有的handler都没有修改maxFormContentSize参数
        // 这个参数限制了表单数据量的上线,如果参数的值超过200KB就会报错

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        handlers.setHandlers(new Handler[]{sendNotifyHandler, getInfoHandler});
        server.setHandler(handlers);
        server.start();
        server.join();
        
        System.out.println("WebAPI已经启动!");
    }
    
    public static void main(String[] args) throws Exception {
		startWebAPIService();
	}
    
}