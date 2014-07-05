package controllers;

import play.*;
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

    public static void messages() {
        ArrayList<Message> results = new ArrayList<Message>();

        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));
        results.add(new Message(" hello : " + System.currentTimeMillis()));

        renderJSON(results);
    }
}