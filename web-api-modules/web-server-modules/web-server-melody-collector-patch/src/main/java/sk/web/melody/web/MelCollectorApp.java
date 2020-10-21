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

import spark.Request;
import spark.Response;
import spark.Route;
import spark.servlet.SparkApplication;

import static spark.Spark.get;
import static spark.Spark.path;

public class MelCollectorApp implements SparkApplication {
    @Override
    public void init() {
        path("api", () -> {
            get("add_node", new AddNodeRoute());
            get("remove_node", new RemoveNodeRoute());
        });
    }

    private static class AddNodeRoute implements Route {
        @Override
        public Object handle(Request request, Response response) throws Exception {
            return "ok";
        }
    }

    private class RemoveNodeRoute implements Route {
        @Override
        public Object handle(Request request, Response response) throws Exception {
            return "ok";
        }
    }
}
