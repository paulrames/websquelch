package websquelch.player;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import websquelch.player.handlers.RequestHandler;
import websquelch.player.watcher.DirectoryWatcher;

public class Main {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: java -jar websquelch.jar <directory>");
			return;
		}
		Path baseDir = Paths.get(args[0]);
		if (Files.notExists(baseDir) || !Files.isReadable(baseDir)) {
			throw new FileNotFoundException("Cannot access " + args[0]);
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
		Undertow server = Undertow.builder().addHttpListener(8080, "localhost")
				.setHandler(Handlers.path()
				.addPrefixPath("/songs", squelchHandler)
				.addPrefixPath("/song", squelchHandler)
				.addPrefixPath("/sse", sseHandler)
				.addPrefixPath("/", Handlers.resource(
						new ClassPathResourceManager(Main.class.getClassLoader(), "view"))
						.addWelcomeFiles("player.html")))
				.build();
		server.start();
	}
	
}
