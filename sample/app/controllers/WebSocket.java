package controllers;

import play.Logger;
import play.modules.profiler.CacheProfilerService;
import play.modules.profiler.MiniProfiler;
import play.modules.profiler.Profile;
import play.modules.profiler.Step;
import play.mvc.Http.WebSocketClose;
import play.mvc.Http.WebSocketEvent;
import play.mvc.Http.WebSocketFrame;
import play.mvc.WebSocketController;

public class WebSocket extends WebSocketController {

    private static long startTime;
    private static CacheProfilerService cacheProfilerService = new CacheProfilerService();
    private static String requestId = "websocket";

    public static void hello(String name) {
        outbound.send("Hello %s!", name);
    }

    public static void echo() {

        while (inbound.isOpen()) {
            WebSocketEvent e = await(inbound.nextEvent());
            if (e instanceof WebSocketFrame) {
                WebSocketFrame frame = (WebSocketFrame) e;
                before();
                if (!frame.isBinary) {
                    if (frame.textData.equals("quit")) {
                        outbound.send("Bye!");
                        disconnect();
                    } else {
                        sendData(frame);
                    }
                }
                after();
            }
            if (e instanceof WebSocketClose) {
                Logger.info("Socket closed!");
            }
        }

    }

    private static void after() {
        Profile profile = MiniProfiler.stop();

        java.util.Map<String, Object> requestData = new java.util.HashMap<String, Object>();
        requestData.put("requestURL", "Websocket");
        requestData.put("timestamp", startTime);
        requestData.put("profile", profile);
        cacheProfilerService.put(requestId, requestData);
    }

    private static void sendData(WebSocketFrame frame) {
        Step step = MiniProfiler.step("sendData");
        outbound.send("Echo: %s", frame.textData);
        step.close();
    }

    private static void before() {
        startTime = System.currentTimeMillis();
        MiniProfiler.start();
    }
}