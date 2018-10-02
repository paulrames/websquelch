package websquelch.player.handlers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.sse.ServerSentEventConnection;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import websquelch.player.file.DiskFile;
import websquelch.player.fileservice.FileEventListener;
import websquelch.player.fileservice.FileService;

public class RequestHandler implements HttpHandler, FileEventListener {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
	
	private final ServerSentEventHandler sseHandler;
	private final ResourceHandler resourceHandler;
	private final FileService fileService;

	public RequestHandler(FileService fileService, ResourceHandler resourceHandler,
			ServerSentEventHandler sseHandler) {
		this.sseHandler = sseHandler;
		this.fileService = fileService;
		this.resourceHandler = resourceHandler;
	}
	
	@Override
	public void fileCreated(DiskFile file) {
		for (ServerSentEventConnection connection : sseHandler.getConnections()) {
			connection.send(file.toJson());
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
			exchange.endExchange();
		}
	}
	
	private void serveFile(HttpServerExchange exchange) throws Exception {
		if (fileService.allowed(exchange.getRelativePath())) {
			logger.info("Serving {} to {}", exchange.getRelativePath(), exchange.getHostName());
			resourceHandler.handleRequest(exchange);
		} else {
			logger.warn("Denying access to {} to {}", exchange.getRelativePath(), exchange.getHostName());
			exchange.setStatusCode(StatusCodes.NOT_FOUND);
			exchange.endExchange();
		}
	}
	
	private void listFiles(HttpServerExchange exchange) throws Exception {
		String requestedDir = exchange.getQueryParameters().get("dir").getFirst();
		List<DiskFile> files = fileService.listFiles(requestedDir);
		if (files == null) {
			exchange.setStatusCode(StatusCodes.NOT_FOUND);
			return;
		}
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
		StringBuilder out = new StringBuilder("[");
		out.append(files.stream().sorted().map(DiskFile::toJson).collect(Collectors.joining(",")));
		out.append("]");
		exchange.getResponseSender().send(out.toString());
	}
	
}