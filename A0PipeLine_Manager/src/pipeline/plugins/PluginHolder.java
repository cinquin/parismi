package pipeline.plugins;

public class PluginHolder {
	public Class<? extends PipelinePlugin> pluginClass;
	public String name;
	public String longName;
	public String toolTip;
	public boolean display;
	public boolean obsolete;

	public PluginHolder(Class<? extends PipelinePlugin> pluginClass, String name, String longName, String toolTip,
			boolean display, boolean obsolete) {
		this.pluginClass = pluginClass;
		this.name = name;
		this.longName = longName;
		this.toolTip = toolTip;
		this.display = display;
	}
}
