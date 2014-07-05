package play.modules.profiler;

import java.util.Map;

import play.cache.Cache;

public class CacheProfilerService {

    private final String CACHE_KEY_FORMAT_STRING = "mini_profile_request_%s";

    public void put(String key, Map<String, Object> data) {
        Cache.set(String.format(CACHE_KEY_FORMAT_STRING, key), data);
    }

    public Map<String, Object> get(String key) {
        return (Map<String, Object>) Cache.get(String.format(CACHE_KEY_FORMAT_STRING, key));
    }

}
