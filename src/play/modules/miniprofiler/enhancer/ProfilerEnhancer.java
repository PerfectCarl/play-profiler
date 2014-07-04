package play.modules.miniprofiler.enhancer;

import java.io.File;
import java.io.IOException;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import javax.persistence.PersistenceException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.classloading.enhancers.Enhancer;
import play.modules.miniprofiler.CacheProfilerService;
import play.mvc.Http.Request;

public class ProfilerEnhancer extends Enhancer {

    private static final String PLUGIN_NAME = "mini-profiler";

    /**
     * A counter used to generate request ids that are then used to construct
     * memcache keys for the profiling data.
     */
    private static java.util.concurrent.atomic.AtomicLong counter;

    private static play.modules.miniprofiler.CacheProfilerService cacheProfilerService = new CacheProfilerService();

    public static void processBefore() throws PersistenceException {



        play.mvc.Http.Request request = play.mvc.Http.Request.current();
        play.mvc.Http.Response response = play.mvc.Http.Response.current();

        play.modules.miniprofiler.Profile profile = null;
        boolean shouldProfile = play.modules.miniprofiler.enhancer.ProfilerEnhancer.shouldProfile(request.url);
        String requestId = "";
        long startTime = 0;

        if (shouldProfile) {
            requestId = String.valueOf(counter.incrementAndGet());
            bef(request, response, requestId);

            // TODO addIncludes(req);

            startTime = System.currentTimeMillis();
            play.modules.miniprofiler.MiniProfiler.start();
        }
        try {
            // TODO chain.doFilter(servletRequest, res);
        } finally {
            if (shouldProfile)
                profile = play.modules.miniprofiler.MiniProfiler.stop();
        }
        af(request, profile, shouldProfile, requestId, startTime);

    }

    public static void af(play.mvc.Http.Request request,
            play.modules.miniprofiler.Profile profile, boolean shouldProfile, String requestId, long startTime) {
        Logger.info("af: shouldProfile    " + shouldProfile);
        Logger.info("af: requestId        " + requestId);
        Logger.info("af: startTime        " + startTime);
        Logger.info("af: profile          " + profile);

        if (shouldProfile)
        {
            java.util.Map<String, Object> requestData = new java.util.HashMap<String, Object>();
            requestData.put("requestURL",
                    request.url + ((request.querystring != null) ? "?" + request.querystring : ""));
            requestData.put("timestamp", startTime);
            requestData.put("profile", profile);
            cacheProfilerService.put(requestId, requestData);
        }
    }

    public static void bef(play.mvc.Http.Request request, play.mvc.Http.Response response,
            String requestId) {
        Logger.info("bef: requestId        " + requestId);
        Logger.info("bef: request.url      " + request.url);
        Logger.info("bef: response.status  " + response.status);
        final String REQUEST_ID_HEADER = "X-Mini-Profile-Request-Id";
        final String REQUEST_ID_ATTRIBUTE = "mini_profile_request_id";

        // request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        addHeader(request, REQUEST_ID_ATTRIBUTE, requestId);

        response.setHeader(REQUEST_ID_HEADER, requestId);
    }

    public static boolean shouldProfile(String url) {
        Logger.info("profiling: " + url);
        return true;
    }

    private static final String REQUEST_ID_HEADER = "X-Mini-Profile-Request-Id";
    private static final String REQUEST_ID_ATTRIBUTE = "mini_profile_request_id";
    private static final String REQUEST_BASE_URL = "mini_profile_base_url";
    private static final String JS_START = "<!-- miniprofiler js start -->";
    private static final String JS_END = "<!-- miniprofiler js end -->";
    private static final String LOAD_JS = "loadJS";
    private static final String INCLUDES_ATTRIBUTE = "mini_profile_includes";

    /**
     * The URL that the {@link MiniProfilerServlet} is mapped to.
     */
    static private String servletURL = "/java_mini_profile/";

    /**
     * Whether to load js or not - useful if doing funky js lazy loading
     */
    static private boolean loadJS = true;

    public static void addIncludes(play.mvc.Http.Request request)
    {
        String result = null;
        String requestId = request.headers.get(REQUEST_ID_ATTRIBUTE).value();
        if (requestId != null) {
            String includesTemplate = load("app/mini_profiler.html");

            if (includesTemplate != null) {

                result = includesTemplate.replace("@@requestId@@", requestId);

                // check if we need to strip out js
                if (!loadJS) {
                    int startIndex = result.indexOf(JS_START);
                    int endIndex = result.indexOf(JS_END);

                    String contentsStart = result.substring(0, startIndex);
                    String contentsEnd = result.substring(endIndex + JS_END.length(), result.length());

                    result = contentsStart + contentsEnd;
                }

            }
        }
        if (StringUtils.isNotEmpty(result)) {
            addHeader(request, REQUEST_BASE_URL, servletURL);
            addHeader(request, REQUEST_ID_ATTRIBUTE, requestId);
            addHeader(request, INCLUDES_ATTRIBUTE, result);
        }
    }

    /**
     * The prefix for all HTML element ids/classes used in the profiler UI. This
     * must be the same value as the {@code htmlIdPrefix} field in
     * {@link MiniProfilerFilter}.
     */
    private static String htmlIdPrefix = "mp";

    private static String load(String filepath) {

        File realFile = Play.getFile(filepath);
        String result = "";
        try {
            result = FileUtils.readFileToString(realFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        result = result.replace("@@prefix@@", htmlIdPrefix);
        result = result.replace("@@baseURL@@", servletURL);

        return result;
    }

    private static void addHeader(Request request, String name, String requestId) {
        play.mvc.Http.Header h = new play.mvc.Http.Header();
        h.name = name;
        h.values = new java.util.ArrayList<String>(1);
        h.values.add(requestId);
        request.headers.put(name, h);
        
    }

    @Override
    public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {

        final CtClass ctClass = makeClass(applicationClass);

        // enhances only Controller classes
        if (!ctClass.subtypeOf(classPool.get(ControllerSupport.class.getName()))) {
            return;
        }

        String entityName = ctClass.getName();
        // Don't enhance ProfilerController
        if (entityName.equals("controllers.MiniProfilerActions"))
            return;

        if (entityName.equals("controllers.PlayDocumentation"))
            return;
        CtField field = new CtField(classPool.getCtClass("java.util.concurrent.atomic.AtomicLong"), "counter", ctClass);
        field.setModifiers(Modifier.STATIC + Modifier.PRIVATE);
        ctClass.addField(field, "new java.util.concurrent.atomic.AtomicLong(1L);");

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {

            // Only enhance action
            if (isAction(ctMethod)) {
                String name = ctMethod.getName();
                Logger.info(PLUGIN_NAME + ": enhancing " + entityName + "." + name);


                String before = " {       play.mvc.Http.Request request = play.mvc.Http.Request.current();\r\n"
                        +
                        "        play.mvc.Http.Response response = play.mvc.Http.Response.current();\r\n"
                        +
                        "        play.modules.miniprofiler.Profile profile = null;\r\n"
                        +
                        "        boolean shouldProfile = play.modules.miniprofiler.enhancer.ProfilerEnhancer.shouldProfile(request.url);\r\n"
                        +
                        "        String requestId = \"\";\r\n" +
                        "        long startTime = 0;\r\n"
                        +
                        "        if (shouldProfile) {\r\n"
                        +
                        "            requestId = String.valueOf(counter.incrementAndGet());\r\n" +
                        "            play.modules.miniprofiler.enhancer.ProfilerEnhancer.bef(request, response, requestId);\r\n"
                        +
                        "            play.modules.miniprofiler.enhancer.ProfilerEnhancer.addIncludes(request);\r\n" +
                        "            startTime = System.currentTimeMillis();\r\n" +
                        "            play.modules.miniprofiler.MiniProfiler.start();\r\n" +
                        "        }\r\n" +
                        "        try {\r\n" +
                        "";

                String after = "  } finally {\r\n"
                        +
                        "            if (shouldProfile){\r\n"
                        +
                        "                profile = play.modules.miniprofiler.MiniProfiler.stop();\r\n"
                        +
                        "                play.modules.miniprofiler.enhancer.ProfilerEnhancer.af(request, profile, shouldProfile, requestId, startTime);\r\n"
                        +
                        "              }\r\n"
                        +
                        "         }\r\n"
                        +
                        "}";

                String oldName = name + "$prof";
                ctMethod.setName(oldName);
                // System.out.println(ctMethod.getLongName());
                // System.out.println(ctMethod.getSignature());
                // System.out.println(ctMethod.getGenericSignature());
                CtMethod mnew = CtNewMethod.copy(ctMethod, name, ctClass,
                        null);
                mnew.setModifiers(Modifier.PUBLIC + Modifier.STATIC);
                StringBuilder body = new StringBuilder();
                body.append(before);
                body.append(oldName + "($$);\n");
                body.append(after);

                mnew.setBody(body.toString());
                ctClass.addMethod(mnew);
                // ctMethod.insertBefore(before);
                // ctMethod.insertAfter(after, true);

            }
        }
        // Done.
        applicationClass.enhancedByteCode = ctClass.toBytecode();

        ctClass.defrost();
    }

    private boolean isAction(CtMethod ctMethod) throws NotFoundException {
        return Modifier.isPublic(ctMethod.getModifiers()) && Modifier.isStatic(ctMethod.getModifiers())
                && ctMethod.getReturnType().equals(CtClass.voidType);
    }
}