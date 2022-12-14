package xyw;

import xyw.handler.AuthHandler;
import xyw.handler.Handler;
import xyw.handler.NotFoundHandler;
import xyw.handler.ServletHandler;
import xyw.handler.servlet.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static xyw.Constant.TEMPLATE_DIR;
import static xyw.Tool.*;

public class SimpleDynamicWebServer {
    private static final String WORK_THREAD_NAME = "slave";
	private static final Integer DEFAULT_PORT = 8088;
	private static final ThreadGroup WORK_GROUP = new ThreadGroup(WORK_THREAD_NAME);
	private static final ExecutorService WORK_POOL = Executors.newFixedThreadPool(8,new ThreadFactory() {
		int i = 0;
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(WORK_GROUP, r,WORK_THREAD_NAME+(i++));
		}
	});
    static ServerSocket server;
    boolean run = false;
    final List<Handler> handlers = new ArrayList<Handler>();
    final int port;
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
        	throw new RuntimeException("Unsupported multiple server!");
        }
    }
    public SimpleDynamicWebServer(int port, List<Handler> handlers){
    	this.port = port;
    	this.handlers.addAll(handlers);
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
        final boolean devMode = Boolean.getBoolean("DEV");
        while (!server.isClosed()) {
            try {
                final Socket socket = server.accept();
                WORK_POOL.execute(new Runnable() {
                    public void run() {
                        try {
                            InputStream is = waitTimeoutInputStream(socket.getInputStream(), 1000L);
                            if(devMode)is = logInputStream(is,1024);
                        	Request request = new Request(is);
                        	if(request.skip()){
                                Logger.info("skip request:{} {}",request.method,request.path);
                                socket.close();return;
                            }
                        	Response response = new Response();
                        	response.getHeaders().put("Connection", "close");//??????????????????
                        	for(Handler handler:handlers){
                        		try{
                        			if(handler.handler(request, response)){break;}
                        		}catch(Throwable t){
                                    Logger.error("handler.handler????????????:{}",t.getMessage(),t);
                        		}
                        	}
                            response.write(socket.getOutputStream());
                            is.close();
                            socket.close();
                        } catch (IOException e) {
                            Logger.error("??????IO??????:{}",e.getMessage(),e);
                            try {
                                socket.close();
                            } catch (IOException ignored) {}
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
        
    public static void main(String[] args) {
        //System.setProperty("DEV","true");
    	System.out.println("java -jar this.jar [port [workpath [context [username password]]]] \ndefault port: 8088\ndefault workpath: ./\ndefault context: /\ndefault no username&password");
    	List<Handler> handlers = new ArrayList<Handler>();
        int port = DEFAULT_PORT;
        String workPath = System.getProperty("user.dir");
        String context = "/";
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                Logger.error("???????????????????????????!");
                return;
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
        Logger.info("Port:{}, ShareDir:{}",port,workPath);
        ServletConfig resourceConfig = new ServletConfig(Boolean.getBoolean("DEV")?(Thread.currentThread().getContextClassLoader().getResource(".").getPath()+"resource"):TEMPLATE_DIR,"/",false,true,false);
        ServletConfig defaultConfig = new ServletConfig(workPath,context,true,true,true);
        handlers.add(
                new ServletHandler(
                        new DoOptionsServlet(defaultConfig),
                        new DoGetServlet(resourceConfig),
                        new DoGetServlet(defaultConfig),
                        new DoPostServlet(defaultConfig),
                        new DoPutServlet(defaultConfig),
                        new DoDeleteServlet(defaultConfig)
                )
        );
        handlers.add(new NotFoundHandler());
        new SimpleDynamicWebServer(port,handlers).start();
    }
}
