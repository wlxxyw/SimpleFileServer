package xyw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
	private static final PrintStream DEFAULT_LOG = System.out;
	private static final PrintStream LOG_PRINT;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss.SSS");
	private static final Pattern paramPattern = Pattern.compile("\\{(\\d*)}");
	private static final int ERROR = 0;
	private static final int WRAN = 10;
	private static final int INFO = 20;
	private static final int DEBUG = 30;
	private static final Map<Integer,String> LOG_FILE_LEVEL = new HashMap<Integer, String>(4);
	private static final Map<Integer,String> DEFAULT_LEVEL = new HashMap<Integer, String>(4);
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
		LOG_PRINT = logOutputStream != null ? new PrintStream(logOutputStream):DEFAULT_LOG;
		sdf.setTimeZone(TimeZone.getTimeZone("GMT+08:00"));
		LOG_FILE_LEVEL.put(ERROR,"E ");LOG_FILE_LEVEL.put(WRAN,"W ");LOG_FILE_LEVEL.put(INFO,"I ");LOG_FILE_LEVEL.put(DEBUG,"D ");
		DEFAULT_LEVEL.put(ERROR,"\033[1;31m[ERROR]\033[0m ");DEFAULT_LEVEL.put(WRAN,"\033[1;31m[WRAN]\033[0m ");DEFAULT_LEVEL.put(INFO,"[INFO] ");DEFAULT_LEVEL.put(DEBUG,"\033[1;32m[DEBUG]\033[0m ");
	}

	public static void debug(final String regex, Object... args) {
		print(Thread.currentThread(), DEBUG, new Date(), regex, args);
	}

	public static void info(final String regex, Object... args) {
		print(Thread.currentThread(), INFO, new Date(), regex, args);
	}

	public static void warn(final String regex, Object... args) {
		print(Thread.currentThread(), WRAN, new Date(), regex, args);
	}

	private static Throwable formatter(StringBuffer sb, String template, Object... args) {
		Matcher matcher = paramPattern.matcher(template);
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

	private static void print(final Thread t,final int level,final Date time,final String msg,final Object...args) {
		LOG_POOL.execute(new Runnable() {
			@Override
			public void run() {
				StringBuilder cache = new StringBuilder();
				cache.append(t.getName());
				cache.append(" ");
				synchronized (sdf){
					cache.append(sdf.format(time));
				}
				cache.append(" ");
				StringBuffer sub = new StringBuffer();
				Throwable t = formatter(sub,msg,args);
				cache.append(sub);
				synchronized (DEFAULT_LOG){
					DEFAULT_LOG.print(DEFAULT_LEVEL.get(level));
					DEFAULT_LOG.print(cache);
					if(null!=t){
						t.printStackTrace(DEFAULT_LOG);
					}
				}
				if(LOG_PRINT!=DEFAULT_LOG &&(debug || level < DEBUG)){
					synchronized (LOG_PRINT){
						LOG_PRINT.print(LOG_FILE_LEVEL.get(level));
						LOG_PRINT.print(cache);
						if(null!=t){
							t.printStackTrace(LOG_PRINT);
						}
					}
				}
			}
		});
	}
}
