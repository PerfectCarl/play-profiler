package play.modules.profiler.enhancer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.ControllersEnhancer.ControllerSupport;
import play.classloading.enhancers.Enhancer;
import play.exceptions.TemplateNotFoundException;
import play.modules.profiler.CacheProfilerService;
import play.modules.profiler.MiniProfiler;
import play.modules.profiler.Profile;
import play.modules.profiler.ProfilerUtil;
import play.modules.profiler.Step;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.vfs.VirtualFile;

public class ProfilerEnhancer extends Enhancer {

    private static final String PLUGIN_NAME = "mini-profiler";

    private static CacheProfilerService cacheProfilerService = new CacheProfilerService();

    public static void after(Profile profile, boolean shouldProfile, String requestId, long startTime) {
        // Logger.info("af: shouldProfile    " + shouldProfile);
        // Logger.info("af: requestId        " + requestId);
        // Logger.info("af: startTime        " + startTime);
        // Logger.info("af: profile          " + profile);

        if (shouldProfile)
        {
            Request request = Request.current();
            java.util.Map<String, Object> requestData = new java.util.HashMap<String, Object>();
            requestData.put("requestURL",
                    request.url + ((request.querystring != null) ? "?" + request.querystring : ""));
            requestData.put("timestamp", startTime);
            requestData.put("profile", profile);
            cacheProfilerService.put(requestId, requestData);
        }
    }

    public static void before(String requestId) {
        Response.current().setHeader(RESPONSE_ID_HEADER, requestId);
        addHeader(Request.current(), REQUEST_ID_ATTRIBUTE, requestId);
    }

    public static boolean shouldProfile(String url) {
        boolean dontProfile = url.startsWith("/@profiler");
        if (dontProfile)
            return false;
        dontProfile = url.startsWith("/@documentation");
        if (dontProfile)
            return false;
        return true;
    }

    public static boolean shouldProfile(Request current) {
        return shouldProfile(current.path);
    }

    private static final String RESPONSE_ID_HEADER = "X-Mini-Profile-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "mini_profile_request_id";
    private static final String JS_START = "<!-- miniprofiler js start -->";
    private static final String JS_END = "<!-- miniprofiler js end -->";
    private static final String LOAD_JS = "loadJS";

    /**
     * Whether to load js or not - useful if doing funky js lazy loading
     */
    static private boolean loadJS = true;

    public static ProfilerUtil addIncludes()
    {
        // Step step = MiniProfiler.step("include");
        // Request request = Request.current();
        String result = "";
        String includeCss = "";
        // String requestId = request.headers.get(REQUEST_ID_ATTRIBUTE).value();
        // if (requestId != null) {
        String includeJs = loadJs();
        includeCss = loadCss();

        if (includeJs != null) {

            // result = includeJs.replace("@@requestId@@", requestId);
            result = includeJs;
            // check if we need to strip out js
            if (!loadJS) {
                int startIndex = result.indexOf(JS_START);
                int endIndex = result.indexOf(JS_END);

                String contentsStart = result.substring(0, startIndex);
                String contentsEnd = result.substring(endIndex + JS_END.length(), result.length());

                result = contentsStart + contentsEnd;
            }
        }
        // }
        if (StringUtils.isNotEmpty(result)) {
            // addHeader(request, REQUEST_ID_ATTRIBUTE, requestId);
            // addHeader(request, INCLUDES_ATTRIBUTE, result);
            // play.mvc.Scope.Session.current().put(INCLUDES_ATTRIBUTE, result);
            // Flash.current().put("profiler_scripts", result);
            // Flash.current().put("profiler_styles", includeCss);
            // String set = "<script type=\"text/javascript\">\r\n" +
            // "        var currentRequestId = \"" + requestId + "\" ;\r\n" +
            // "</script>";

            return new ProfilerUtil(result, includeCss);
        }
        else
            return ProfilerUtil.empty;
        // step.close();
    }

    public static String loadJs() {
        String includeJs = (String) Cache.get("includeJs");
        if (StringUtils.isEmpty(includeJs)) {
            includeJs = load("public/includes/profiler_scripts.html");
            Cache.set("includeJs", includeJs);
        }
        return includeJs;
    }

    public static String loadCss() {
        String includeCss = (String) Cache.get("includeCss");
        if (StringUtils.isEmpty(includeCss)) {
            includeCss = load("public/includes/profiler_styles.html");
            Cache.set("includeCss", includeCss);
        }
        return includeCss;
    }

    private static String load(String filepath) {
        // Logger.info("Loading " + filepath);
        // for (String m : Play.modules.keySet())
        // {
        // Logger.info("module :" + m + " " +
        // Play.modules.get(m).getRealFile().getAbsolutePath());
        // }
        // The module is named play in development mode (local) but profiler
        // one packaged.
        VirtualFile mod = Play.modules.get("profiler");
        if (mod == null)
        {
            mod = Play.modules.get("play");
        }
        VirtualFile vf = mod.child(filepath);
        // VirtualFile vf =
        // VirtualFile.fromRelativePath("modules/mini-profiler/" + filepath);
        // File realFile = Play.getFile(filepath);

        File realFile = vf.getRealFile();
        String result = "";
        try {
            result = FileUtils.readFileToString(realFile, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // result = result.replace("@@prefix@@", htmlIdPrefix);
        // result = result.replace("@@baseURL@@", servletURL);

        return result;
    }

    public static void addHeader(Request request, String name, String requestId) {
        play.mvc.Http.Header h = new play.mvc.Http.Header();
        h.name = name;
        h.values = new java.util.ArrayList<String>(1);
        h.values.add(requestId);
        request.headers.put(name, h);

    }

    static public String currentRequestId()
    {
        Header header = Request.current().headers.get(REQUEST_ID_ATTRIBUTE);
        if (header == null)
            return "";
        return header.value();
    }

    static public void setCurrentRequestId(long requestId)
    {
        addHeader(Request.current(), REQUEST_ID_ATTRIBUTE, requestId + "");
    }

    @Override
    public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {

        final CtClass ctClass = makeClass(applicationClass);
        // Logger.info(PLUGIN_NAME + ": list " + ctClass.getName());
        // enhances only Controller classes
        if (!ctClass.subtypeOf(classPool.get(ControllerSupport.class.getName()))) {
            return;
        }

        String entityName = ctClass.getName();
        // Don't enhance ProfilerController
        if (entityName.equals("controllers.ProfilerActions"))
            return;

        if (entityName.equals("controllers.PlayDocumentation"))
            return;

        for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            // Logger.info("method: " + ctMethod.getLongName());
            // Only enhance action
            if (isAction(ctMethod)) {
                String name = ctMethod.getName();
                // Logger.info(PLUGIN_NAME + ": enhancing " + entityName + "." +
                // name);
                System.out.println();
                String controllerName = ctClass.getSimpleName() + "." + name;
                String before = " {     /* System.out.println(\"00\"); */\r\n"
                        +
                        "        play.modules.profiler.Step step = null;\r\n"
                        +
                        "        String stepName = \""
                        + controllerName
                        + "\";\r\n"
                        +
                        "        /*System.out.println(\"01\");*/ boolean shouldProfile = play.modules.profiler.enhancer.ProfilerEnhancer.shouldProfile(request.url);\r\n"
                        +
                        "        if (shouldProfile) {\r\n"
                        +
                        "            step = play.modules.profiler.MiniProfiler.step(stepName);\r\n"
                        +
                        "        }\r\n" +
                        "        try {\r\n" +
                        "";

                String after = "  } finally {\r\n"
                        +
                        "            if (shouldProfile && step != null){\r\n"
                        +
                        "                /*System.out.println(\"02\");*/ step.close();\r\n"
                        +
                        "              }\r\n"
                        +
                        "         }\r\n"
                        +
                        "}";

                String oldName = name + "__prof";
                ctMethod.setName(oldName);
                CtMethod mnew = CtNewMethod.copy(ctMethod, name, ctClass,
                        null);
                mnew.setModifiers(Modifier.PUBLIC + Modifier.STATIC);
                StringBuilder body = new StringBuilder();
                body.append(before);
                body.append(oldName + "($$);\n");
                body.append(after);

                mnew.setBody(body.toString());
                ctClass.addMethod(mnew);
            }
        }
        /*CtMethod m = CtNewMethod.make(
                        "protected static void renderTemplate2(java.lang.String templateName, java.util.Map<java.lang.String,java.lang.Object> args) \r\n"
                                +
                                "{     System.out.println(\"10\"); \r\n" +
                                "      play.mvc.Controller.renderTemplate( templateName, args) ; \r\n" +
                                "}",
                ctClass);
        // m.setModifiers(Modifier.PROTECTED + Modifier.STATIC);
        ctClass.addMethod(m);
        */
        //CtMethod m = new 
        for (final CtMethod ctMethod : ctClass.getMethods()) {
            //if (ctMethod.getName().startsWith("render"))
             if
             ("play.mvc.Controller.renderTemplate(java.lang.String,java.util.Map)".equals(ctMethod.getLongName()))
            {
                String name = ctMethod.getName();
                Logger.info(PLUGIN_NAME + ": enhancing " + entityName + "." +
                        name);

                String before = " {      System.out.println(\"10\"); \r\n"
                        +
                        "        try {\r\n" +
                        "";
                String after = "  } catch (play.exceptions.TemplateNotFoundException ex) {" +

                        "                System.out.println(\"13\"); throw ex ; \r\n"
                        + "}" +
                        "finally {\r\n"
                        +
                        "                System.out.println(\"12\"); \r\n"
                        +
                        "         }\r\n"
                        +
                        "}";

                //String oldName = name + "__prof";
                //ctMethod.setName(oldName);
                CtMethod mnew = CtNewMethod.copy(ctMethod, name, ctClass,
                        null);
                // mnew.setModifiers(Modifier.PROTECTED + Modifier.STATIC +
                // Modifier.);
                StringBuilder body = new StringBuilder();
                // body.append(before);
                // body.append("super.($$);\n");
                // body.append(after);
                body.append("System.out.println(\"12\");");
                mnew.setBody(body.toString());
                // ctClass.addMethod(mnew);

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