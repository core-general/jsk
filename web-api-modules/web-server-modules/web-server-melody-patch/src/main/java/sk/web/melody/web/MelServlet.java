package sk.web.melody.web;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2020 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.AllArgsConstructor;
import okhttp3.OkHttpClient;
import sk.services.async.AsyncImpl;
import sk.services.http.HttpImpl;
import sk.services.retry.RepeatImpl;
import sk.services.time.TimeUtcImpl;
import sk.utils.tuples.X2;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static sk.web.melody.web.MelNodeManagementService.addNode;
import static sk.web.melody.web.MelNodeManagementService.removeNode;

public class MelServlet extends HttpServlet {
    volatile MelCleanTask cleanerTask;

    @Override
    public void init() throws ServletException {
        super.init();
        final AsyncImpl async = new AsyncImpl();
        final RepeatImpl repeat = new RepeatImpl(async);
        final TimeUtcImpl times = new TimeUtcImpl();
        final HttpImpl http = new HttpImpl(repeat, times, new OkHttpClient.Builder().build());
        cleanerTask = new MelCleanTask(async, http);
        cleanerTask.start();
    }

    @Override
    public void destroy() {
        super.destroy();
        cleanerTask.finishThread(1000).join();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        final String pathInfo = request.getPathInfo().replaceAll("/", "");
        switch (pathInfo) {
            case "add_node":
                new AddRequest(request, response).run();
                break;
            case "remove_node":
                new RemoveRequest(request, response).run();
                break;
            default:
                throw new RuntimeException("Unknown path for collector:" + pathInfo);
        }
    }

    @AllArgsConstructor
    private static abstract class Request {
        HttpServletRequest request;
        HttpServletResponse response;

        protected String p(String paramName) {
            return request.getParameter(paramName);
        }

        protected void respond(X2<String, Integer> resp) {
            response.setStatus(resp.i2());
            response.setContentType("text/plain");
            PrintWriter out = null;
            try {
                out = response.getWriter();
                out.println(resp.i1());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
    }

    private static class AddRequest extends Request {
        public AddRequest(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        public void run() {
            respond(addNode(p("app_name"), p("node_ip"), p("node_port"), p("node_login"), p("node_password")));
        }
    }

    private static class RemoveRequest extends Request {
        public RemoveRequest(HttpServletRequest request, HttpServletResponse response) {
            super(request, response);
        }

        public void run() {
            respond(removeNode(p("app_name"), p("node_ip"), p("node_port")));
        }
    }
}
