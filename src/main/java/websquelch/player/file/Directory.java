package websquelch.player.file;

public class Directory extends DiskFile {

	private static final String JSON_TEMPLATE = "{\"title\": \"%s\", \"src\": \"%s\", \"type\": \"dir\"}";

	public Directory(String name, String path) {
		super(name, path);
	}

	@Override
	public String toJson() {
		String name = getName().concat("/");
		return String.format(JSON_TEMPLATE, name, getPath());
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
