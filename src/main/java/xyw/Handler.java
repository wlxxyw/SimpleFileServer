package xyw;

public interface Handler {
	final static String METHOD_GET = "GET";
	final static String METHOD_POST = "POST";
	final static String METHOD_OPTION = "OPTION";
	boolean handler(Request req, Response res);
}
