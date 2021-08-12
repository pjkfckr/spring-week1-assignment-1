package com.codesoom.assignment.web;

import com.codesoom.assignment.TaskManager;
import com.codesoom.assignment.TaskMapper;
import com.codesoom.assignment.errors.TaskIdNotFoundException;
import com.codesoom.assignment.models.Task;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

public class TaskHttpHandler implements HttpHandler {

    private static final String NOT_FOUND_MESSAGE = "Not Found.";
    private static final String NOT_ALLOWED_METHOD_MESSAGE = "허용되지 않은 메서드 입니다.";

    private final TaskManager taskManager = TaskManager.getInstance();
    private final TaskMapper taskMapper = new TaskMapper();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        HttpRequest httpRequest = getHttpRequest(httpExchange);
        System.out.println(httpRequest);

        InputStream httpRequestBody = httpExchange.getRequestBody();
        String body = new BufferedReader(new InputStreamReader(httpRequestBody))
            .lines()
            .collect(Collectors.joining("\n"));

        Long taskId;
        try {
            taskId = httpRequest.getTaskIdFromPath();
        } catch (NumberFormatException ne) {
            taskId = null;
        }

        if (httpRequest.isReadAll()) {
            new HttpResponseOK(httpExchange).send(taskMapper.toJson());
        }

        if (httpRequest.isReadOne()) {
            try {
                new HttpResponseOK(httpExchange).send(taskMapper.toJsonWith(taskId));
            } catch (TaskIdNotFoundException error) {
                new HttpResponseNotFound(httpExchange).send(error.getMessage());
            }
        }

        if (httpRequest.isCreateOne()) {
            if (body.isEmpty()) {
                new HttpResponseNoContent(httpExchange).send();
            }

            Task createdTask = taskManager.createTask(body);

            new HttpResponseCreated(httpExchange).send(taskMapper.toJsonWith(createdTask));
        }

        if (httpRequest.isUpdateOne()) {
            try {
                Task updatedTask = taskManager.updateTask(taskId, body);

                new HttpResponseOK(httpExchange).send(taskMapper.toJsonWith(updatedTask));
            } catch (TaskIdNotFoundException error) {
                new HttpResponseNotFound(httpExchange).send(error.getMessage());
            }
        }

        if (httpRequest.isDeleteOne()) {
            try {
                Task deletedTask = taskManager.deleteTask(taskId);

                new HttpResponseNoContent(httpExchange).send(taskMapper.toJsonWith(deletedTask));
            } catch (TaskIdNotFoundException error) {
                new HttpResponseNotFound(httpExchange).send(error.getMessage());
            }
        }

        new HttpResponseNotFound(httpExchange).send(NOT_FOUND_MESSAGE);
    }

    private HttpRequest getHttpRequest(HttpExchange httpExchange) throws IOException {
        URI requestURI = httpExchange.getRequestURI();
        String path = requestURI.getPath();
        String method = httpExchange.getRequestMethod();

        HttpRequest httpRequest = new HttpRequest(path, method);
        if (!httpRequest.isAllowedMethod()) {
            new HttpResponseBadRequest(httpExchange).send(NOT_ALLOWED_METHOD_MESSAGE);
        }

        return httpRequest;
    }
}
