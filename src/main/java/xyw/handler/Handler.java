package xyw.handler;

import xyw.Request;
import xyw.Response;

public interface Handler {
	boolean handler(Request req, Response res);
}
