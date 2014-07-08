package play.modules.profiler;

import java.util.Map;

public interface ProfilerExtra {
    public Map<String, Object> getExtraData(String requestId);
}
