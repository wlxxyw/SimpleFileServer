package xyw.handler.servlet;

import static xyw.Constant.*;
import java.io.File;
import java.io.IOException;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;

public class DoPutServlet extends Servlet{
	public DoPutServlet(ServletConfig config){
		super(config);
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)) {
			if (METHOD_PUT.equals(req.getMethod())) {
				String path = req.getPath();
				String dir;
				try {
					dir = new String(Tool.readAsBytes(req.getBody(), false), UTF8);
				} catch (IOException e) {
					e.printStackTrace();
					return quickFinish(res, ResponseCode.ERROR, e.getLocalizedMessage());
				}
				Logger.info("新建文件夹请求:{}", dir);
				File f = new File(baseFile, path.substring(config.context.length()));
				f = new File(f,dir);
				if (f.exists()) {
					Logger.warn("文件路径已存在:{}", dir);
					return quickFinish(res, ResponseCode.ERROR, "文件路径已存在!");
				} else {
					try {
						if (f.mkdirs()) {
							return quickFinish(res, ResponseCode.OK, "文件路径创建成功!");
						} else {
							Logger.error("文件路径创建失败:{}", dir);
							return quickFinish(res, ResponseCode.ERROR, "文件路径创建失败!");
						}
					} catch (Throwable t) {
						Logger.error("文件路径创建出错:{}", dir,t);
						return quickFinish(res, ResponseCode.ERROR, t.getLocalizedMessage());
					}
				}
			}
		}
		return false;
	}
}
