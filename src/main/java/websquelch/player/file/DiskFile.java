package websquelch.player.file;

public abstract class DiskFile implements Comparable<DiskFile> {

	private String name;

	private String path;

	protected DiskFile(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public int compareTo(DiskFile o) {
		int o1 = getOrder();
		int o2 = o.getOrder();
		if (o1 != o2) {
			return o1 - o2;
		}
		return getName().compareTo(o.getName());
	}

	public abstract String toJson();

	public abstract int getOrder();

}
