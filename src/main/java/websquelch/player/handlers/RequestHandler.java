package websquelch.player.handlers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import websquelch.player.watcher.FileEventListener;

public class RequestHandler implements HttpHandler, FileEventListener {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
	
	private static final List<String> extensions = Arrays.asList(".ogg", ".mp3");
	
	private final Path baseDir;
	private final ServerSentEventHandler sseHandler;
	private final ResourceHandler resourceHandler;

	public RequestHandler(Path base, ServerSentEventHandler sseHandler) {
		this.baseDir = base;
		this.sseHandler = sseHandler;
		this.resourceHandler = new ResourceHandler(new PathResourceManager(base));
	}
	
	@Override
	public void fileCreated(Path file) {
		Path fullPath = baseDir.resolve(file);
		if (isSong(fullPath)) {
			for (ServerSentEventConnection connection : sseHandler.getConnections()) {
				connection.send(toJson(fullPath));
			}
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		String path = exchange.getResolvedPath();
		if ("/songs".equals(path)) {
			listFiles(exchange);
		} else if ("/song".equals(path)) {
			serveFile(exchange);
		} else {
			exchange.setStatusCode(StatusCodes.NOT_FOUND);
		}
	}
	
	private void serveFile(HttpServerExchange exchange) throws Exception {
		logger.info("Serving {} to {}", exchange.getRelativePath(), exchange.getHostName());
		resourceHandler.handleRequest(exchange);
	}
	
	private void listFiles(HttpServerExchange exchange) throws Exception {
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		StringBuilder out = new StringBuilder("[");
		out.append(Files.list(baseDir)
			.filter(RequestHandler::isSong)
			.map(RequestHandler::toJson)
			.sorted()
			.collect(Collectors.joining(",")));
		out.append("]");
		exchange.getResponseSender().send(out.toString());
	}
	
	private static String toJson(Path path) {
		String source = path.getFileName().toString();
		String title = source;
		int lastDot = source.lastIndexOf(".");
		if (lastDot > 0) {
			title = title.substring(0, lastDot);
		}
		return String.format("{\"title\": \"%s\", \"src\": \"%s\"}", title, source);
	}
	
	private static boolean isSong(Path path) {
		if (Files.isRegularFile(path)) {
			String fileName = path.getFileName().toString();
			for (String extension : extensions) {
				if (fileName.endsWith(extension)) {
					return true;
				}
			}
		}
		return false;
	}

}