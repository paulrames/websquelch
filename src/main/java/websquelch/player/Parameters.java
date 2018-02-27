package websquelch.player;

public class Parameters {

	private Integer port;
	
	private String bindAddress;
	
	private String username;
	private String password;
	
	private String keystoreDir;
	private String keystorePassword;
	private String keyPassword;
	
	private String baseDir;
	
	private boolean showUsage = false;

	public static void usage() {
		String msg = "Usage: java -jar websquelch.jar [options] <base directory>\n" +
				"\nOptions:\n" +
				"  --port port\t\tlisten on port (defaults to 8080)\n" +
				"  --bind address\tbind to address (defaults to 0.0.0.0)\n" +
				"  --auth user:password\tenable HTTP basic auth, with defined username and password\n" +
				"  --keystore path\tpath to a keystore to enable HTTPS\n" +
				"  --storepass password\tkeystore password\n" +
				"  --keypass password\tprivate key password\n" +
				"  --help\t\tthis help";
		System.out.println(msg);
	}
	
	private Parameters() { }

	public static Parameters parse(String[] args) {
		Parameters params = new Parameters();
		for (int i = 0; i < args.length; i++) {
			if ("--port".equals(args[i]) && i + 1 < args.length) {
				params.port = Integer.parseInt(args[++i]);
			} else if ("--bind".equals(args[i]) && i + 1 < args.length) {
				params.bindAddress = args[++i];
			} else if ("--auth".equals(args[i]) && i + 1 < args.length) {
				String[] parts = args[++i].split(":");
				params.username = parts[0];
				params.password = parts[1];
			} else if ("--keystore".equals(args[i]) && i + 1 < args.length) {
				params.keystoreDir = args[++i];
			} else if ("--storepass".equals(args[i]) && i + 1 < args.length) {
				params.keystorePassword = args[++i];
			} else if ("--keypass".equals(args[i]) && i + 1 < args.length) {
				params.keyPassword = args[++i];
			} else if ("--help".equals(args[i])) {
				params.showUsage = true;
			} else if (i == args.length - 1) {
				params.baseDir = args[i];
			} else {
				// TODO: Ignored parameter
			}
		}
		return params;
	}

	public boolean isNotValid() {
		return showUsage || isEmpty(baseDir) || (keystoreDir != null && isEmpty(keyPassword));
	}

	public Integer getPort() {
		return port;
	}
	
	public String getBindAddress() {
		return bindAddress;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getKeystoreDir() {
		return keystoreDir;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public String getKeyPassword() {
		return keyPassword;
	}
	
	public String getBaseDir() {
		return baseDir;
	}
	
	private static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}
	
}
