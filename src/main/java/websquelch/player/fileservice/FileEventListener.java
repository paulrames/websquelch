package websquelch.player.fileservice;

import websquelch.player.file.DiskFile;

public interface FileEventListener {
	
	void fileCreated(DiskFile file);
	
}
