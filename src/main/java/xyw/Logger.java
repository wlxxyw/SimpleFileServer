package xyw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger {
	private static final Executor LOG_POOL = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r,"log");
		}
	});
	public static final boolean debug;
	private static final boolean _default;
	private static final PrintStream print;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss.SSS", Locale.CHINA);
	private static final Pattern m = Pattern.compile("\\{(\\d*)}");
	private static final int ERROR = 0;
	private static final int WRAN = 10;
	private static final int INFO = 20;
	private static final int DEBUG = 30;
	static {
		debug = Boolean.parseBoolean(System.getenv("debug"))||Boolean.getBoolean("debug");
		String logFilePath = System.getenv("logfile");
		if(null==logFilePath)logFilePath=System.getProperty("logfile");
		OutputStream logOutputStream = null;
		try {
			if (null != logFilePath) {
				File logFile = new File(logFilePath);
				if ((logFile.isFile() && logFile.canWrite())
						|| logFile.createNewFile()) {
					logOutputStream = new FileOutputStream(logFile);
				}
			}
		} catch (Throwable ignored) {
		}
		_default = null == logOutputStream;
		print = logOutputStream != null ? new PrintStream(logOutputStream)
				: System.out;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
	}

	public static void debug(final String regex, Object... args) {
		if (debug) {
			print(Thread.currentThread(), DEBUG, System.currentTimeMillis(), regex, args);
		}
	}

	public static void info(final String regex, Object... args) {
		print(Thread.currentThread(), INFO, System.currentTimeMillis(), regex, args);
	}

	public static void warn(final String regex, Object... args) {
		print(Thread.currentThread(), WRAN, System.currentTimeMillis(), regex, args);
	}

	private static Throwable formatter(StringBuffer sb, String template, Object... args) {
		Matcher matcher = m.matcher(template);
		int index = 0;
		while (matcher.find()) {
			String _index = matcher.group(1);
			if (null != _index && _index.trim().length() > 0) {
				matcher.appendReplacement(
						sb,
						String.valueOf(args[Integer.parseInt(_index)]).replace("\\", "\\\\").replace("$","\\$"));
			} else {
				matcher.appendReplacement(sb, String.valueOf(args[index])
						.replace("\\", "\\\\").replace("$","\\$"));
			}
			if (index++ == args.length) {
				break;
			}
		}
		matcher.appendTail(sb);
		sb.append("\n");
		if(args.length>0&&args[args.length-1] instanceof Throwable){
			return ((Throwable)args[args.length-1]);
		}
		return null;
	}

	private static void print(final Thread t,final int level,final Long time,final String msg,final Object...args) {
		LOG_POOL.execute(new Runnable() {
			@Override
			public void run() {
				StringBuilder cache = new StringBuilder();
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
				cache.append(t.getName());
				cache.append(" ");
				synchronized (sdf){
					cache.append(sdf.format(new Date(time)));
				}
				cache.append(" ");
				StringBuffer sub = new StringBuffer();
				Throwable t = formatter(sub,msg,args);
				cache.append(sub);
				synchronized (print){
					print.print(cache);
					if(null!=t){
						t.printStackTrace(print);
					}
				}
			}
		});
	}
}
