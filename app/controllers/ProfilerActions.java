package controllers;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.CallStat;

import org.apache.commons.lang.StringUtils;

import play.modules.profiler.CacheProfilerService;
import play.modules.profiler.Profile;
import play.mvc.Controller;

public class ProfilerActions extends Controller {

    /** The cache Service */
    static private CacheProfilerService cacheProfilerService = new CacheProfilerService();

    static public void results()
    {
        Map<String, Object> result = new HashMap<String, Object>();

        String requestIds = params.get("ids");
        if (StringUtils.isNotEmpty(requestIds)) {
            List<Map<String, Object>> requests = new ArrayList<Map<String, Object>>();
            for (String requestId : requestIds.split(",")) {
                requestId = requestId.trim();

                Map<String, Object> requestData = cacheProfilerService.get(requestId);
                if (requestData != null) {
                    Map<String, Object> request = new HashMap<String, Object>();
                    request.put("id", requestId);
                    request.put("redirect", requestData.get("redirect"));
                    request.put("requestURL", requestData.get("requestURL"));
                    request.put("timestamp", requestData.get("timestamp"));

                    Profile rootProfile = (Profile) requestData.get("profile");
                    rootProfile.computeSelf();
                    request.put("profile", rootProfile);

                    Map<String, Object> appstatsMap = getAppstatsDataFor(rootProfile);
                    request.put("appstats", appstatsMap != null ? appstatsMap : null);

                    requests.add(request);
                }
            }
            result.put("ok", true);
            result.put("requests", requests);
        } else {
            result.put("ok", false);
        }

        renderJSON(result);

        // response.contentType = "application/json");
        // resp.setHeader("Cache-Control", "no-cache");

        // ObjectMapper jsonMapper = new ObjectMapper();
        // jsonMapper.writeValue(resp.getOutputStream(), result);

    }

    private static Map<String, Object> getAppstatsDataFor(Profile rootProfile) {
        Map<String, Object> appstatsMap = null;

        appstatsMap = new HashMap<String, Object>();
        // appstatsMap.put("totalTime", appstats.getDurationMilliseconds());
        Map<String, Map<String, Object>> rpcInfoMap = new LinkedHashMap<String, Map<String, Object>>();

        Map<String, CallStat> map = getCallStats(rootProfile.getChildren());
        Iterator<String> iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String tag = (String) iter.next();

            CallStat cs = map.get(tag);

            Map<String, Object> rpcInfo = new LinkedHashMap<String, Object>();
            rpcInfoMap.put(tag, rpcInfo);

            rpcInfo.put("totalCalls", cs.getCalls());

            DecimalFormat df = new DecimalFormat("#.##");
            rpcInfo.put("totalTime", df.format(cs.getTotalTime() / 1000000.0));
        }

        appstatsMap.put("rpcStats", !rpcInfoMap.isEmpty() ? rpcInfoMap : null);

        return appstatsMap;
    }

    static private Map<String, CallStat> getCallStats(List<Profile> calls) {
        Map<String, CallStat> map = new HashMap<String, CallStat>();

        for (Profile prof : calls) {
            if (StringUtils.isNotEmpty(prof.getTag())) {
                CallStat cs = map.get(prof.getTag());
                if (cs == null) {
                    cs = new CallStat(prof.getTag());
                    map.put(prof.getTag(), cs);
                }
                cs.setCalls(cs.getCalls() + 1);
                cs.setTotalTime(cs.getTotalTime() + prof.getDuration());
            }

            // recursive
            Map<String, CallStat> rec = getCallStats(prof.getChildren());

            // collate the maps
            Iterator<String> iter = rec.keySet().iterator();
            while (iter.hasNext()) {
                String tag = (String) iter.next();
                if (map.containsKey(tag)) {
                    CallStat csnew = rec.get(tag);
                    CallStat csexisting = map.get(tag);

                    csexisting.setCalls(csexisting.getCalls() + csnew.getCalls());
                    csexisting.setTotalTime(csexisting.getTotalTime() + csnew.getTotalTime());
                } else {
                    map.put(tag, rec.get(tag));
                }
            }

        }
        return map;
    }

}
