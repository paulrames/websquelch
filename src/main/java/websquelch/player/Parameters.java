package websquelch.player;

public class Parameters {

	private Integer port = 8080;
	
	private String username;
	private String password;
	
	private String baseDir;
	
	private boolean showUsage = false;

	public static void usage() {
		String msg = "Usage: java -jar websquelch.jar [options] <base directory>\n" +
				"\nOptions:\n" +
				"  --port port\t\tlisten on port (defaults to 8080)\n" +
				"  --auth user:password\tenable HTTP basic auth, with defined username and password\n" +
				"  --help\t\tthis help";
		System.out.println(msg);
	}
	
	private Parameters() { }

	public static Parameters parse(String[] args) {
		Parameters params = new Parameters();
		for (int i = 0; i < args.length; i++) {
			if ("--port".equals(args[i]) && i + 1 < args.length) {
				params.port = Integer.parseInt(args[++i]);
			} else if ("--auth".equals(args[i]) && i + 1 < args.length) {
				String[] parts = args[++i].split(":");
				params.username = parts[0];
				params.password = parts[1];
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
		return showUsage || baseDir == null || baseDir.isEmpty();
	}

	public Integer getPort() {
		return port;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}
	
}
