import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** A small local task manager. No external libraries required. */
public class TaskApp {
    private static final Path DATA = Paths.get("data", "tasks.properties");
    private static final Map<Integer, Task> tasks = new LinkedHashMap<>();
    private static int nextId = 1;
    private static class Task {
        String title, description; boolean done;
        Task(String title, String description, boolean done) {
            this.title = title; this.description = description; this.done = done;
        }
    }
    public static void main(String[] args) throws Exception {
        load();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        server.createContext("/", TaskApp::handle);
        server.start();
        System.out.println("Task Manager started successfully: http://localhost:8080");
        System.out.println("Data file: " + DATA.toAbsolutePath());
    }
    private static synchronized void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if ("POST".equals(exchange.getRequestMethod())) {
                Map<String,String> form = parse(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                int id = Integer.parseInt(form.getOrDefault("id", "0"));
                if ("/save".equals(path)) {
                    String title = form.getOrDefault("title", "").trim();
                    String description = form.getOrDefault("description", "").trim();
                    if (title.isEmpty() || title.length() > 120 || description.length() > 2000) {
                        send(exchange, 400, "Title is required (max 120 characters); description max 2000."); return;
                    }
                    if (id != 0 && !tasks.containsKey(id)) { send(exchange,404,"Task not found"); return; }
                    if (id == 0) id = nextId++;
                    tasks.put(id, new Task(title, description, form.containsKey("done")));
                } else if ("/delete".equals(path)) {
                    tasks.remove(id);
                } else { send(exchange,404,"Not found"); return; }
                save();
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(303, -1); exchange.close(); return;
            }
            if (!"GET".equals(exchange.getRequestMethod())) { send(exchange,405,"Method not allowed"); return; }
            if (!"/".equals(path)) { send(exchange,404,"Not found"); return; }
            Map<String,String> query = parse(exchange.getRequestURI().getRawQuery());
            int editId = Integer.parseInt(query.getOrDefault("edit", "0"));
            Task edit = tasks.get(editId);
            StringBuilder html = new StringBuilder("""
                <!doctype html><html lang="en"><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Task Manager · 71661</title>
                <style>
                body{font:16px system-ui;background:#eef2f8;color:#172238;margin:0}
                main{max-width:950px;margin:45px auto;padding:0 24px}
                h1{font-size:38px;margin-bottom:5px}p{color:#52617a}
                section,article{background:white;border-radius:14px;padding:24px;margin:20px 0;box-shadow:0 4px 20px #2030500a}
                input[type=text],textarea{box-sizing:border-box;width:100%;padding:12px;border:1px solid #bdc9dc;border-radius:8px;font:inherit;margin:7px 0 16px}
                textarea{height:85px}button,.button{background:#315be8;color:white;border:0;border-radius:7px;padding:10px 17px;font:inherit;cursor:pointer;text-decoration:none;display:inline-block}
                .delete{background:#b83246}.muted{background:#e7ecf5;color:#253650}
                .actions{display:flex;gap:10px;align-items:center}.actions form{margin:0}
                .badge{font-size:13px;background:#e9eefc;padding:5px 10px;border-radius:20px}
                article p{white-space:pre-wrap;overflow-wrap:anywhere}h3{overflow-wrap:anywhere}
                label{display:block}small{color:#52617a}
                </style><main><small>JAVA CRUD APPLICATION · STUDENT 71661</small>
                <h1>Task Manager</h1><p>Plan your work. Keep track of what is done.</p><section>
                """);
            html.append("<h2>").append(edit == null ? "Create a task" : "Edit task #" + editId).append("</h2>");
            html.append("<form method='post' action='/save'><input type='hidden' name='id' value='")
                .append(edit == null ? 0 : editId).append("'><label>Title<input type='text' name='title' required maxlength='120' value='")
                .append(escape(edit == null ? "" : edit.title)).append("'></label><label>Description<textarea name='description' maxlength='2000'>")
                .append(escape(edit == null ? "" : edit.description)).append("</textarea></label><label><input type='checkbox' name='done' ")
                .append(edit != null && edit.done ? "checked" : "").append("> Completed</label><br><button>Save task</button> <a class='button muted' href='/'>Cancel</a></form></section>");
                        html.append("<h2>Your tasks (").append(tasks.size()).append(", ")
                .append(tasks.values().stream().filter(task -> task.done).count()).append(" completed)</h2>");
            if (tasks.isEmpty()) html.append("<section>No tasks yet. Create your first task above.</section>");
            for (Map.Entry<Integer,Task> entry : tasks.entrySet()) {
                int id = entry.getKey(); Task t = entry.getValue();
                html.append("<article><span class='badge'>").append(t.done ? "Completed" : "To do")
                    .append("</span><h3>").append(escape(t.title)).append("</h3><p>").append(escape(t.description))
                    .append("</p><div class='actions'><a class='button muted' href='/?edit=").append(id)
                    .append("'>Edit</a><form method='post' action='/delete'><input type='hidden' name='id' value='")
                    .append(id).append("'><button class='delete'>Delete</button></form></div></article>");
            }
            html.append("</main></html>"); send(exchange,200,html.toString());
        } catch (NumberFormatException ex) { send(exchange,400,"Invalid task ID"); }
        catch (Exception ex) { ex.printStackTrace(); send(exchange,500,"Unable to process the request. Check the application console."); }
    }
    private static Map<String,String> parse(String input) {
        Map<String,String> result = new HashMap<>();
        if (input != null && !input.isEmpty()) for (String pair : input.split("&")) {
            String[] p = pair.split("=",2);
            result.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), URLDecoder.decode(p.length > 1 ? p[1] : "", StandardCharsets.UTF_8));
        }
        return result;
    }
    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
    private static void send(HttpExchange e, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type","text/html; charset=utf-8");
        e.sendResponseHeaders(status,bytes.length);
        try (OutputStream out = e.getResponseBody()) { out.write(bytes); }
    }
    private static void load() throws IOException {
        if (!Files.exists(DATA)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(DATA)) { p.load(in); }
        nextId = Integer.parseInt(p.getProperty("nextId","1"));
        for (String key : p.stringPropertyNames()) if (key.endsWith(".title")) {
            int id = Integer.parseInt(key.substring(0,key.indexOf('.')));
            tasks.put(id,new Task(p.getProperty(key),p.getProperty(id+".description",""),Boolean.parseBoolean(p.getProperty(id+".done"))));
            nextId = Math.max(nextId,id+1);
        }
    }
    private static void save() throws IOException {
        Properties p = new Properties(); p.setProperty("nextId",String.valueOf(nextId));
        tasks.forEach((id,t) -> { p.setProperty(id+".title",t.title); p.setProperty(id+".description",t.description); p.setProperty(id+".done",String.valueOf(t.done)); });
        Files.createDirectories(DATA.getParent());
        Path temp = DATA.resolveSibling("tasks.tmp");
        try (OutputStream out = Files.newOutputStream(temp)) { p.store(out,"Task Manager"); }
        Files.move(temp,DATA,StandardCopyOption.REPLACE_EXISTING);
    }
}
