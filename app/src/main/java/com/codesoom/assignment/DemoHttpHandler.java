package com.codesoom.assignment;

import com.codesoom.assignment.models.RequestInfo;
import com.codesoom.assignment.models.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DemoHttpHandler implements HttpHandler {
    private Long nextId = 1L;
    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Task> tasks = new ArrayList<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // get method, url, body
        RequestInfo requestInfo = new RequestInfo(exchange);

        System.out.println(requestInfo.getMethod() + requestInfo.getPath());

        String requestMethod = requestInfo.getMethod();
        String requestPath = requestInfo.getPath();
        String requestBody = requestInfo.getBody();

        String content = "";
        int codeStatus = 500;

        // GET ALL
        if ("GET".equals(requestMethod) && hasTask(requestPath)) {
            content = convertTasksToJson(this.tasks);
            codeStatus = 200;
        }

        // GET Detail
        if ("GET".equals(requestMethod) && hasTaskId(requestPath)) {
            try {
                Long id = getTaskIdFromPath(requestPath);
                Task resultTask = getTaskById(id);
                content = convertTaskToJson(resultTask);
                codeStatus = 200;
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                codeStatus = 404;
            }
        }

        // POST
        if ("POST".equals(requestMethod) && hasTask(requestPath)) {
            Task task = convertJsonToTask(requestPath);
            task.setId(this.nextId++);
            this.tasks.add(task);

            content = convertTaskToJson(task);
            codeStatus = 201;
        }

        // PUT & PATCH
        if (("PUT".equals(requestMethod) || "PATCH".equals(requestMethod)) && hasTaskId(requestPath)) {
            try {
                Long id = getTaskIdFromPath(requestPath);
                String inputTitle = convertJsonToTask(requestBody).getTitle();

                Task resultTask = getTaskById(id);
                resultTask.setTitle(inputTitle);

                content = convertTaskToJson(resultTask);
                codeStatus = 200;
            } catch(NoSuchElementException e) {
                e.printStackTrace();
                codeStatus = 404;
            }
        }

        // DELETE
        if ("DELETE".equals(requestMethod) && hasTaskId(requestPath)) {
            try {
                Long id = getTaskIdFromPath(requestPath);
                removeTaskById(id);
                codeStatus = 204;
            } catch (NoSuchElementException e) {
                e.printStackTrace();
                codeStatus = 404;
            }
        }

        exchange.sendResponseHeaders(codeStatus, content.getBytes().length);

        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(content.getBytes());
        outputStream.flush();
        outputStream.close();
    }

    private Task convertJsonToTask(String content) throws JsonProcessingException {
        return this.objectMapper.readValue(content, Task.class);
    }

    private String convertTasksToJson(List<Task> tasks) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        this.objectMapper.writeValue(outputStream, tasks);

        return outputStream.toString();
    }

    private String convertTaskToJson(Task task) throws IOException {
        OutputStream outputStream = new ByteArrayOutputStream();
        this.objectMapper.writeValue(outputStream, task);

        return outputStream.toString();
    }

    private Task getTaskById(Long id) throws NoSuchElementException {
        return this.tasks.stream()
                .filter(task->task.getId().equals(id))
                .findFirst()
                .orElseThrow(()->new NoSuchElementException("Not Found Task"));
    }

    private void removeTaskById(Long id) throws NoSuchElementException {
        getTaskById(id);

        this.tasks = this.tasks.stream()
                .filter(task->!(task.getId().equals(id)))
                .collect(Collectors.toList());
    }

    private boolean hasTask(String path) {
        return Pattern.matches("/tasks$", path);
    }

    private boolean hasTaskId(String path) {
        return Pattern.matches("/tasks/[0-9]+$", path);
    }

    private Long getTaskIdFromPath(String path) throws NoSuchElementException {
        Pattern pattern = Pattern.compile("/tasks/([0-9]+)$");
        Matcher matcher = pattern.matcher(path);

        if(!matcher.find())
            throw new NoSuchElementException("Not Found id");

        return Long.parseLong(matcher.group(1));
    }
}
