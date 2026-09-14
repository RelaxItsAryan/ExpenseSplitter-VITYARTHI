import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseSplitterWeb {
    private static final List<String> members = new ArrayList<>();
    private static final List<Expense> expenses = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", ExpenseSplitterWeb::handleHome);
        server.createContext("/style.css", ExpenseSplitterWeb::handleStyles);
        server.createContext("/bg.png", ExpenseSplitterWeb::handleBackground);
        server.setExecutor(null);
        server.start();
        System.out.println("Expense Splitter is running at http://localhost:8080");
    }

    private static void handleHome(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "<h1>Method not allowed</h1>", "text/html");
            return;
        }

        String message = "";
        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, String> form = parseForm(exchange);
            message = processForm(form);
        }
        send(exchange, 200, page(message), "text/html");
    }

    private static void handleStyles(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method not allowed", "text/plain");
            return;
        }
        Path css = Path.of("style.css");
        if (!Files.exists(css)) {
            send(exchange, 404, "style.css was not found", "text/plain");
            return;
        }
        byte[] content = Files.readAllBytes(css);
        exchange.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static void handleBackground(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method not allowed", "text/plain");
            return;
        }
        Path background = Path.of("bg.png");
        if (!Files.exists(background)) {
            send(exchange, 404, "bg.png was not found", "text/plain");
            return;
        }
        byte[] content = Files.readAllBytes(background);
        exchange.getResponseHeaders().set("Content-Type", "image/png");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private static String processForm(Map<String, String> form) {
        String action = form.getOrDefault("action", "");
        if ("member".equals(action)) {
            String name = form.getOrDefault("name", "").trim();
            if (name.isEmpty()) return "Member name cannot be empty.";
            if (findMember(name) != null) return "That member is already in the group.";
            members.add(name);
            return name + " was added.";
        }

        if ("expense".equals(action)) {
            if (members.size() < 2) return "Add at least two members before adding an expense.";
            String description = form.getOrDefault("description", "").trim();
            String paidBy = findMember(form.getOrDefault("paidBy", ""));
            long total = parseMoney(form.getOrDefault("total", ""));
            List<String> participants = selectedParticipants(form);
            if (description.isEmpty() || paidBy == null || total <= 0) {
                return "Enter a description, a valid amount, and a payer.";
            }
            if (participants.isEmpty()) {
                return "Select at least one person who needs to pay.";
            }
            expenses.add(new Expense(description, total, paidBy, equalShares(total, participants)));
            return "Expense added successfully.";
        }
        return "Unknown action.";
    }

    private static Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return values;
    }

    private static long parseMoney(String value) {
        try {
            return Math.round(Double.parseDouble(value) * 100);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static List<String> selectedParticipants(Map<String, String> form) {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            if ("on".equals(form.get("participant" + i))) {
                selected.add(members.get(i));
            }
        }
        return selected;
    }

    private static Map<String, Long> equalShares(long total, List<String> participants) {
        Map<String, Long> shares = new LinkedHashMap<>();
        long base = total / participants.size();
        long remainder = total % participants.size();
        for (int i = 0; i < participants.size(); i++) {
            shares.put(participants.get(i), base + (i < remainder ? 1 : 0));
        }
        return shares;
    }

    private static Map<String, Long> balances() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (String member : members) result.put(member, 0L);
        for (Expense expense : expenses) {
            result.put(expense.paidBy, result.get(expense.paidBy) + expense.total);
            for (Map.Entry<String, Long> share : expense.shares.entrySet()) {
                result.put(share.getKey(), result.get(share.getKey()) - share.getValue());
            }
        }
        return result;
    }

    private static String page(String message) {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Expense Splitter</title><link rel="stylesheet" href="/style.css"></head>
                <body><main class="container"><header><p class="eyebrow">GROUP FINANCE</p>
                <h1>Expense Splitter</h1><p>Track shared expenses and see who owes what.</p></header>
                """);
        if (!message.isEmpty()) html.append("<div class=\"notice\">").append(escape(message)).append("</div>");
        html.append("""
                <section class="grid"><article class="card"><h2>Add member</h2>
                <form method="post"><input type="hidden" name="action" value="member">
                <label>Name<input name="name" required placeholder="Enter member name"></label>
                <button>Add member</button></form></article>
                <article class="card"><h2>Add expense</h2>
                """);
        if (members.size() < 2) {
            html.append("<p class=\"muted\">Add at least two members first.</p>");
        } else {
            html.append("""
                    <form method="post"><input type="hidden" name="action" value="expense">
                    <label>Description<input name="description" required placeholder="Dinner"></label>
                    <label>Total amount<input name="total" type="number" min="0.01" step="0.01" required placeholder="0.00"></label>
                    <label>Paid by<select name="paidBy">
                    """);
            for (String member : members) html.append("<option>").append(escape(member)).append("</option>");
            html.append("</select></label><fieldset><legend>Who needs to pay?</legend><div class=\"participant-list\">");
            for (int i = 0; i < members.size(); i++) {
                html.append("<label class=\"checkbox\"><input type=\"checkbox\" name=\"participant")
                        .append(i).append("\" checked><span>").append(escape(members.get(i)))
                        .append("</span></label>");
            }
            html.append("</div></fieldset><button>Add expense</button></form>");
        }
        html.append("</article></section><section class=\"card\"><h2>Members</h2><div class=\"chips\">");
        if (members.isEmpty()) html.append("<span class=\"muted\">No members yet.</span>");
        for (String member : members) html.append("<span class=\"chip\">").append(escape(member)).append("</span>");
        html.append("</div></section><section class=\"grid\"><article class=\"card\"><h2>Balances</h2><ul>");
        for (Map.Entry<String, Long> balance : balances().entrySet()) {
            String kind = balance.getValue() >= 0 ? "receive" : "owe";
            html.append("<li><span>").append(escape(balance.getKey())).append("</span><strong class=\"")
                    .append(kind).append("\">").append(formatMoney(balance.getValue())).append("</strong></li>");
        }
        html.append("</ul></article><article class=\"card\"><h2>Expenses</h2><ul>");
        if (expenses.isEmpty()) html.append("<li class=\"muted\">No expenses recorded.</li>");
        for (Expense expense : expenses) {
            html.append("<li><span>").append(escape(expense.description)).append("<small> paid by ")
                    .append(escape(expense.paidBy)).append("</small></span><strong>")
                    .append(formatMoney(expense.total)).append("</strong></li>");
        }
        html.append("</ul></article></section></main></body></html>");
        return html.toString();
    }

    private static String findMember(String name) {
        for (String member : members) if (member.equalsIgnoreCase(name.trim())) return member;
        return null;
    }

    private static String formatMoney(long cents) {
        return String.format("$%.2f", cents / 100.0);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void send(HttpExchange exchange, int status, String body, String type) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }

    private record Expense(String description, long total, String paidBy, Map<String, Long> shares) {}
}
