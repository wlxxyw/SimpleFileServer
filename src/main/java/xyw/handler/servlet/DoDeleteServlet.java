package xyw.handler.servlet;

import static xyw.Constant.*;
import java.io.File;
import java.io.IOException;
import xyw.Logger;
import xyw.Request;
import xyw.Response;
import xyw.Tool;
import xyw.Response.ResponseCode;

public class DoDeleteServlet extends Servlet{
	public DoDeleteServlet(ServletConfig config){
		super(config);
	}
	@Override
	public boolean doServlet(Request req, Response res) {
		if(matchContext(req)) {
			if (METHOD_DELETE.equals(req.getMethod())) {
				String dir;
				try {
					dir = new String(Tool.readAsBytes(req.getBody(), false), UTF8);
				} catch (IOException e) {
					e.printStackTrace();
					return quickFinish(res, ResponseCode.ERROR, e.getLocalizedMessage());
				}
				Logger.info("删除文件请求:{}", dir);
				File f = new File(baseFile, dir);
				if (!f.exists()) {
					Logger.warn("删除文件请求,文件路径不存在:{}", dir);
					return quickFinish(res, ResponseCode.ERROR, "文件路径不存在!");
				} else {
					try {
						if (f.delete()) {
							return quickFinish(res, ResponseCode.OK, "文件删除成功!");
						} else {
							Logger.error("删除文件删除失败:{}", dir);
							return quickFinish(res, ResponseCode.ERROR, "文件删除失败!");
						}
					} catch (Throwable t) {
						Logger.error("删除文件删除出错:{}", dir);
						return quickFinish(res, ResponseCode.ERROR, t.getLocalizedMessage());
					}
				}
			}
		}
		return false;
	}
}
