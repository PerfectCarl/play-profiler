package play.modules.profiler;

public class ProfilerUtil {

    public String scripts = "";

    public String styles = "";

    public boolean profiling = true;

    public ProfilerUtil(String script, String style)
    {
        this.scripts = script;
        this.styles = style;
    }

    public ProfilerUtil() {
    }
}
