package play.modules.profiler;

public class ProfilerUtil {

    public static ProfilerUtil empty = new ProfilerUtil();

    public String scripts = "";

    public String styles = "";

    public String requestId = "";

    private String originalScript;

    public ProfilerUtil(String script, String style)
    {
        this.scripts = script;
        this.styles = style;
        this.originalScript = script;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
        String set = "<script type=\"text/javascript\">\r\n" +
                "        var currentRequestId = \"" + requestId + "\" ;\r\n" +
                "</script>";
        scripts = set + originalScript;
    }

    public ProfilerUtil() {
    }
}
