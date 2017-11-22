package websquelch.player.watcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryWatcher extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

	private final Path path;
	private List<FileEventListener> listeners = new ArrayList<>();

	public DirectoryWatcher(Path path) {
		setDaemon(true);
		this.path = Paths.get(path.toUri());
	}

	public void addListener(FileEventListener listener) {
		this.listeners.add(listener);
	}

	@Override
	public void run() {
		try {
			WatchService watchService = path.getFileSystem().newWatchService();
			path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
			while (true) {
				WatchKey watchKey = watchService.take();
				List<Path> filenames = new ArrayList<>();
				for (WatchEvent<?> event : watchKey.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					Path filename = (Path) event.context();
					filenames.add(filename);
				}
				// If multiple files are notified within the same event, we sort them to ensure
				// directory ordering
				if (filenames.size() > 1) {
					Collections.sort(filenames);
				}
				for (Path filename : filenames) {
					logger.info("New file: {}", filename);
					for (FileEventListener listener : listeners) {
						listener.fileCreated(filename);
					}
				}
				if (!watchKey.reset()) {
					watchKey.cancel();
					watchService.close();
					logger.warn("Closing WatchService");
					break;
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
