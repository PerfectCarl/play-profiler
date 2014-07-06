package controllers;

import play.*;
import play.modules.profiler.MiniProfiler;
import play.modules.profiler.Step;
import play.mvc.*;

import java.util.*;

import models.*;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void page() {
        render();
    }

    public static void db() {
        Step step = MiniProfiler.step("database", "add data");
        try {
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
        } finally {
            step.close();
        }
        step = MiniProfiler.step("database", "findAll");
        try {
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            new Data(System.currentTimeMillis()).save();
            Data.findAll();
        } finally {
            step.close();
        }
        render();
    }

    public static void welcome() {
        render();
    }

    public static void messages() {
        ArrayList<Message> results = new ArrayList<Message>();

        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));

        renderJSON(results);
    }
}