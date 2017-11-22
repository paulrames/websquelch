package websquelch.player.watcher;

import java.nio.file.Path;

public interface FileEventListener {
	
	void fileCreated(Path path);

}
