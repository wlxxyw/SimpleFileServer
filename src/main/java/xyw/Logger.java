package xyw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
	private static boolean debugable = false;
	private static final boolean _default;
	private static final PrintStream print;
	static {
		String debug = System.getenv("debug");
		if (null != debug && 0 != debug.length() && !"0".equals(debug)) {
			debugable = true;
		}
		String logFilePath = System.getenv("logfile");
		OutputStream logOutputStream = null;
		try {
			if (null != logFilePath) {
				File logFile = new File(logFilePath);
				if ((logFile.isFile() && logFile.canWrite())
						|| logFile.createNewFile()) {
					logOutputStream = new FileOutputStream(logFile);
				}
			}
		} catch (Throwable e) {
		}
		_default = null == logOutputStream;
		print = logOutputStream != null ? new PrintStream(logOutputStream)
				: System.out;
	}
	private static final StringBuffer cache = new StringBuffer();
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy/MM/dd hh:mm:ss.SSS");
	private static final Pattern m = Pattern.compile("\\{([0-9]{0,})\\}");
	private static final int ERROR = 0;
	private static final int WRAN = 10;
	private static final int INFO = 20;
	private static final int DEBUG = 30;

	public static void debug(final String debug, Object... args) {
		if (debugable) {
			print(DEBUG, System.currentTimeMillis(), formatter(debug, args));
		}
	}

	public static void info(final String info, Object... args) {
		print(INFO, System.currentTimeMillis(), formatter(info, args));
	}

	public static void warn(final String wran, Object... args) {
		print(WRAN, System.currentTimeMillis(), formatter(wran, args));
	}

	private synchronized static String formatter(String template,
			Object... args) {
		StringBuffer sb = new StringBuffer();
		Matcher matcher = m.matcher(template);
		int index = 0;
		while (matcher.find()) {
			String _index = matcher.group(1);
			if (null != _index && _index.trim().length() > 0) {
				matcher.appendReplacement(
						sb,
						String.valueOf(args[Integer.valueOf(_index)]).replace(
								"\\", "\\\\"));
			} else {
				matcher.appendReplacement(sb, String.valueOf(args[index])
						.replace("\\", "\\\\"));
			}
			if (index++ == args.length) {
				break;
			}
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	private static void print(int level, Long time, String msg) {
		synchronized (cache) {
			switch (level) {
			case ERROR:
				cache.append(!_default ? "E" : "\033[1;31m[ERROR]\033[0m");
				break;
			case WRAN:
				cache.append(!_default ? "W" : "\033[1;31m[WRAN]\033[0m");
				break;
			case INFO:
				cache.append(!_default ? "I" : "[INFO]");
				break;
			case DEBUG:
				cache.append(!_default ? "D" : "\033[1;32m[DEBUG]\033[0m");
				break;
			}
			cache.append(" ");
			cache.append(sdf.format(new Date(time)));
			cache.append(" ");
			cache.append(msg);
			cache.append("\n");
			print.print(cache.toString());
			cache.setLength(0);
		}

	}
}
