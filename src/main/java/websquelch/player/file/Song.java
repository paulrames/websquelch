package websquelch.player.file;

public class Song extends DiskFile {

	private static final String JSON_TEMPLATE = "{\"title\": \"%s\", \"src\": \"%s\", \"type\": \"file\"}";

	public Song(String name, String path) {
		super(name, path);
	}

	@Override
	public String toJson() {
		String name = getName();
		int lastDot = name.lastIndexOf('.');
		if (lastDot > 0) {
			name = name.substring(0, lastDot);
		}
		return String.format(JSON_TEMPLATE, name, getPath());
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
