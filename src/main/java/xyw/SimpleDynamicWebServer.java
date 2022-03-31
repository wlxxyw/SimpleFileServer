package xyw;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import xyw.handler.AuthHandler;
import xyw.handler.FaviconHandler;
import xyw.handler.FileHandler;

public class SimpleDynamicWebServer {
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
                new Thread(new Runnable() {
                    public void run() {
                        try {
                        	InputStream is = socket.getInputStream();
                        	Request request = new Request(is);
                        	if(request.skip()){socket.close();return;}
                        	Response response = new Response();
                        	for(Handler handler:handlers){
                        		try{
                        			if(handler.handler(request, response)){break;}
                        		}catch(Throwable t){
                        			t.printStackTrace();
                        		}
                        	}
                            OutputStream os = socket.getOutputStream();
                            os.write(response.toByte());
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
        
    public static void main(String[] args) throws IOException {
    	List<Handler> handlers = new ArrayList<Handler>();
        int port = 8080;
        String path = System.getProperty("user.dir");
        if (args.length >= 2) {
            path = args[1];
        }
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        if(args.length >= 4){
        	handlers.add(new AuthHandler(args[2], args[3],"[/]{0,1}favicon\\.ico"));
        }
        try {
			Handler faviconHandler = new FaviconHandler("favicon.ico");
			handlers.add(faviconHandler);
		} catch (IOException e) {
			e.printStackTrace();
		}
        handlers.add(new FileHandler(path, "/"));
        new SimpleDynamicWebServer(port,handlers).start();
    }
}
