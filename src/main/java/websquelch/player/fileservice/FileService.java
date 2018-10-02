package websquelch.player.fileservice;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import websquelch.player.file.Directory;
import websquelch.player.file.DiskFile;
import websquelch.player.file.Song;

public class FileService extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	private static final List<String> validExtensions = Arrays.asList(".ogg", ".mp3");

	private final Path baseDir;
	private List<FileEventListener> listeners = new ArrayList<>();

	public FileService(Path baseDir) {
		setDaemon(true);
		this.baseDir = Paths.get(baseDir.toUri());
	}

	public void addListener(FileEventListener listener) {
		this.listeners.add(listener);
	}

	public List<DiskFile> listFiles(String requested) {
		Path dir = Paths.get(baseDir.toString(), requested);
		if (!dir.normalize().startsWith(baseDir)) {
			return null;
		}
		List<DiskFile> files = new ArrayList<>();
		if (!dir.equals(baseDir)) {
			files.add(new Directory("..", baseDir.relativize(dir.getParent()).toString()));
		}
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
			for (Path path : directoryStream) {
				DiskFile file = toFile(path);
				if (file != null) {
					files.add(file);
				}
			}
		} catch (IOException e) {
			logger.error("Error listing directory {}",  dir, e);
		}
		return files;
	}

	public void startMonitoring(FileEventListener... listeners) {
		for (FileEventListener listener : listeners) {
			addListener(listener);
		}
		start();
	}

	public boolean allowed(String file) {
		if (file != null && file.startsWith("/")) {
			file = file.substring(1);
		}
		return toFile(baseDir.resolve(file)) != null;
	}

	@Override
	public void run() {
		try {
			WatchService watchService = baseDir.getFileSystem().newWatchService();
			monitor(watchService, baseDir);
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
				List<Path> newDirectories = new ArrayList<>();
				for (Path filename : filenames) {
					Path path = ((Path) watchKey.watchable()).resolve(filename);
					DiskFile file = toFile(path);
					if (file != null) {
						logger.info("New file: {}", file.getPath());
						if (Files.isDirectory(path)) {
							newDirectories.add(path);
						}
						for (FileEventListener listener : listeners) {
							listener.fileCreated(file);
						}
					}
				}
				if (!watchKey.reset()) {
					watchKey.cancel();
					watchService.close();
					logger.error("Closing WatchService");
					break;
				}
				for (Path path : newDirectories) {
					logger.info("Monitoring new directory {}", path);
					monitor(watchService, path);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void monitor(WatchService watchService, Path path) throws IOException {
		path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private DiskFile toFile(Path path) {
		try {
			if (Files.isHidden(path)) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		String name = path.getFileName().toString();
		String src = baseDir.relativize(path).toString();
		if (Files.isRegularFile(path)) {
			for (String extension : validExtensions) {
				if (path.getFileName().toString().endsWith(extension)) {
					return new Song(name, src);
				}
			}
		} else if (Files.isDirectory(path)) {
			return new Directory(name, src);
		}
		return null;
	}

}
