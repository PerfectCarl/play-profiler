package com.google.appengine.tools.appstats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import play.modules.profiler.ProfilerExtra;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.appstats.StatsProtos.AggregateRpcStatsProto;
import com.google.appengine.tools.appstats.StatsProtos.IndividualRpcStatsProto;
import com.google.appengine.tools.appstats.StatsProtos.StackFrameProto;

public enum GaeProfilerExtra implements ProfilerExtra {
    INSTANCE;

    public Map<String, Object> getExtraData(String param) {
        return null; // getAppstatsDataFor(param, 2);
    }

    /**
     * Get the Appstats data for the specified id.
     * 
     * @param appstatsId
     *            The id of the Appstats request.
     * @param maxStackFrames
     *            The maximum number of stack frames to include in each RPC
     *            stack trace.
     * @return The appstats data.
     */
    public static Map<String, Object> getAppstatsDataFor(String appstatsId, Integer maxStackFrames)
    {
        Map<String, Object> appstatsMap = null;
        MemcacheWriter writer = new MemcacheWriter(null, MemcacheServiceFactory.getMemcacheService("__appstats__"));
        StatsProtos.RequestStatProto appstats = writer.getFull(Long.parseLong(appstatsId));
        if (appstats != null)
        {
            appstatsMap = new HashMap<String, Object>();
            appstatsMap.put("totalTime", appstats.getDurationMilliseconds());

            Map<String, Map<String, Object>> rpcInfoMap = new LinkedHashMap<String, Map<String, Object>>();
            for (AggregateRpcStatsProto rpcStat : appstats.getRpcStatsList())
            {
                Map<String, Object> rpcInfo = rpcInfoMap.get(rpcStat.getServiceCallName());
                if (rpcInfo == null)
                {
                    rpcInfo = new LinkedHashMap<String, Object>();
                    rpcInfoMap.put(rpcStat.getServiceCallName(), rpcInfo);
                }

                rpcInfo.put("totalCalls", rpcStat.getTotalAmountOfCalls());
                rpcInfo.put("totalTime", Long.valueOf(0));
            }

            List<Map<String, Object>> callInfoMap = new ArrayList<Map<String, Object>>();
            for (IndividualRpcStatsProto rpcStat : appstats.getIndividualStatsList())
            {
                // Update the total time for the RPC method
                Map<String, Object> rpcInfo = rpcInfoMap.get(rpcStat.getServiceCallName());
                rpcInfo.put("totalTime", ((Long) rpcInfo.get("totalTime")) + rpcStat.getDurationMilliseconds());

                // Get info about this specific call
                Map<String, Object> callInfo = new LinkedHashMap<String, Object>();
                callInfoMap.add(callInfo);
                callInfo.put("serviceCallName", rpcStat.getServiceCallName());
                callInfo.put("totalTime", rpcStat.getDurationMilliseconds());
                callInfo.put("startOffset", rpcStat.getStartOffsetMilliseconds());
                callInfo.put("request", truncate(rpcStat.getRequestDataSummary(), 100));
                callInfo.put("response", truncate(rpcStat.getResponseDataSummary(), 100));
                // Get the stack trace
                List<String> callStack = new ArrayList<String>();
                int i = 0;
                for (StackFrameProto frame : rpcStat.getCallStackList())
                {
                    if (maxStackFrames != null && i == maxStackFrames)
                    {
                        break;
                    }
                    callStack.add(String.format("%s.%s:%d", frame.getClassOrFileName(), frame.getFunctionName(),
                            frame.getLineNumber()));
                    i++;
                }
                callInfo.put("callStack", callStack);
            }
            appstatsMap.put("rpcStats", !rpcInfoMap.isEmpty() ? rpcInfoMap : null);
            appstatsMap.put("rpcCalls", !callInfoMap.isEmpty() ? callInfoMap : null);
        }
        return appstatsMap;
    }

    private static String truncate(String s, int maxLength)
    {
        if (s.length() > maxLength)
        {
            return s.substring(0, maxLength);
        } else
        {
            return s;
        }
    }

}
