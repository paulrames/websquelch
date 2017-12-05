package websquelch.player;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import websquelch.player.handlers.RequestHandler;
import websquelch.player.handlers.auth.BasicAuth;
import websquelch.player.watcher.DirectoryWatcher;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		Parameters params = Parameters.parse(args);
		if (params.isNotValid()) {
			Parameters.usage();
			return;
		}
		Path baseDir = Paths.get(params.getBaseDir());
		if (Files.notExists(baseDir) || !Files.isReadable(baseDir)) {
			throw new FileNotFoundException("Cannot access " + params.getBaseDir());
		}
		ServerSentEventHandler sseHandler = Handlers.serverSentEvents(new ServerSentEventConnectionCallback() {
			@Override
			public void connected(ServerSentEventConnection connection, String lastEventId) {
				// Keepalive to avoid losing connection on mobile devices
				connection.setKeepAliveTime(15000);
			}
		});
		RequestHandler squelchHandler = new RequestHandler(baseDir, sseHandler);
		DirectoryWatcher watcher = new DirectoryWatcher(baseDir);
		watcher.addListener(squelchHandler);
		watcher.start();
		Undertow server = Undertow.builder().addHttpListener(params.getPort(), "localhost")
				.setHandler(addSecurity(params, Handlers.path()
				.addPrefixPath("/songs", squelchHandler)
				.addPrefixPath("/song", squelchHandler)
				.addPrefixPath("/sse", sseHandler)
				.addPrefixPath("/", Handlers.resource(
						new ClassPathResourceManager(Main.class.getClassLoader(), "view"))
						.addWelcomeFiles("player.html"))))
				.build();
		server.start();
		log.info("Base directory = {}", baseDir.toAbsolutePath());
		log.info("Server started on port {}", params.getPort());
	}
	
	private static HttpHandler addSecurity(Parameters params, HttpHandler handler) {
		if (params.getUsername() != null && params.getPassword() != null) {
			return BasicAuth.wrap(handler, params.getUsername(), params.getPassword());
		} else {
			return handler;
		}
	}
	
}
