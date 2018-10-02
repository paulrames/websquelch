package websquelch.player;

import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.Undertow.ListenerInfo;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventConnectionCallback;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import websquelch.player.fileservice.FileService;
import websquelch.player.handlers.RequestHandler;
import websquelch.player.handlers.auth.BasicAuth;
import websquelch.player.handlers.ssl.SSLContextFactory;

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
		FileService fileService = new FileService(baseDir);
		ResourceHandler resourceHandler = new ResourceHandler(new PathResourceManager(baseDir));
		RequestHandler squelchHandler = new RequestHandler(fileService, resourceHandler, sseHandler);
		fileService.startMonitoring(squelchHandler);
		Undertow server = createListener(params, Undertow.builder())
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
		for (ListenerInfo info : server.getListenerInfo()) {
			InetSocketAddress address = (InetSocketAddress) info.getAddress();
			log.info("Server started on {} port {}", address.getHostString(), address.getPort());
		}
	}

	private static Builder createListener(Parameters params, Builder builder) throws Exception {
		if (params.getKeystoreDir() != null && params.getKeystorePassword() != null
				&& params.getKeyPassword() != null) {
			SSLContext sslContext = SSLContextFactory.createSSLContext(params.getKeystoreDir(),
					params.getKeystorePassword(), params.getKeyPassword());
			return builder.addHttpsListener(getPort(params), getBindAddress(params), sslContext);
		} else {
			return builder.addHttpListener(getPort(params), getBindAddress(params));
		}
	}

	private static HttpHandler addSecurity(Parameters params, HttpHandler handler) {
		if (params.getUsername() != null && params.getPassword() != null) {
			return BasicAuth.wrap(handler, params.getUsername(), params.getPassword());
		} else {
			return handler;
		}
	}

	private static String getBindAddress(Parameters params) {
		return params.getBindAddress() != null ? params.getBindAddress() : "0.0.0.0";
	}

	private static int getPort(Parameters params) {
		return params.getPort() != null ? params.getPort() : 8080;
	}

}
