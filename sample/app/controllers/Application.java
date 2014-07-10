package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Data;
import models.Message;
import play.modules.profiler.MiniProfiler;
import play.modules.profiler.Step;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void ajax() {
        render();
    }

    static protected void renderTemplate(String templateName, Map<String, Object> args) {
        System.out.println("XX");
        Controller.renderTemplate(templateName, args);
    }

    public static void custom() {
        Step step = MiniProfiler.step("database", "DB calls");
        List<Data> result;

        try {
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();

            result = Data.findAll();
        } finally {
            step.close();
        }
        render();
    }

    public static void welcome() {
        render();
    }

    public static void messagesJson() {
        ArrayList<Message> results = new ArrayList<Message>();

        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));

        renderJSON(results);
    }

    public static void websocket() {
        render();
    }

}