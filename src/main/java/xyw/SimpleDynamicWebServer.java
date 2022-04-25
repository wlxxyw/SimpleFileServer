package xyw;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import xyw.handler.AuthHandler;
import xyw.handler.Handler;
import xyw.handler.ServletHandler;
import xyw.handler.servlet.DoDeleteServlet;
import xyw.handler.servlet.DoGetServlet;
import xyw.handler.servlet.DoPostServlet;
import xyw.handler.servlet.DoPutServlet;
import xyw.handler.servlet.ResourceServlet;

public class SimpleDynamicWebServer {
	private static final Integer DEFAULT_PORT = 8088;
	private static final ThreadGroup WORK_GROUP = new ThreadGroup("slave");
	private static final ExecutorService WORK_POOL = Executors.newFixedThreadPool(4,new ThreadFactory() {
		int i = 0;
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(WORK_GROUP, r,"slave-"+i++);
		}
	});
    static ServerSocket server;
    boolean run = false;
    final List<Handler> handlers = new ArrayList<Handler>();
    int port;
    private synchronized void init(){
    	if (server == null) {
            try {
                server = new ServerSocket(port);
                Logger.info("SimpleDynamicWebServer start...\n\tPort:{}", port);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e.getLocalizedMessage());
            }
        }else{
        	throw new RuntimeException("Unsupport multiple server!");
        }
    }
    public SimpleDynamicWebServer(int port, List<Handler> handlers){
    	this.port = port;
    	this.handlers.addAll(handlers);
        init();
    }
    public SimpleDynamicWebServer(int port, Handler...handlers){
    	if(null!=handlers&&0!=handlers.length){
    		for(Handler handler:handlers){
    			if(!this.handlers.contains(handler)){
    				this.handlers.add(handler);
    			}
    		}
    	}
        init();
    }

    public void start() {
        if (null == server) {
            return;
        }
        if (run) {
            return;
        }
        run = true;
        while (!server.isClosed()) {
            try {
                final Socket socket = server.accept();
                WORK_POOL.execute(new Runnable() {
                    public void run() {
                        try {
                        	InputStream is = socket.getInputStream();
                        	Request request = new Request(is);
                        	if(request.skip()){socket.close();return;}
                        	Response response = new Response();
                        	response.getHeaders().put("Connection", "close");//不支持长连接
                        	for(Handler handler:handlers){
                        		try{
                        			if(handler.handler(request, response)){break;}
                        		}catch(Throwable t){
                        			t.printStackTrace();
                        		}
                        	}
                            response.write(socket.getOutputStream());
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
        
    public static void main(String[] args) throws IOException {
    	System.out.println("java -jar this.jar [port [workpath [context [username password]]]] \ndefault port: 8080\ndefault workpath: ./\ndefault context: /\ndefault no username&password");
    	List<Handler> handlers = new ArrayList<Handler>();
        int port = DEFAULT_PORT;
        String workPath = System.getProperty("user.dir");
        String context = "/";
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (args.length > 1) {
        	workPath = args[1];
        }
        if (args.length > 2) {
        	context = args[2];
        }
        if(args.length > 4){
        	handlers.add(new AuthHandler(args[3], args[4],"[/]{0,1}favicon\\.ico"));
        }
        handlers.add(new ServletHandler(new ResourceServlet(Tool.tempFile("static.zip"), "/"),new DoGetServlet(workPath, context),new DoPostServlet(workPath, context),new DoPutServlet(workPath, context),new DoDeleteServlet(workPath, context)));
        new SimpleDynamicWebServer(port,handlers).start();
    }
}
