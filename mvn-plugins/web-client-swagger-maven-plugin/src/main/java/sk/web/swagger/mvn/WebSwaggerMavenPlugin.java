package sk.web.swagger.mvn;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import sk.mvn.ApiClassUtil;
import sk.services.except.IExcept;
import sk.services.json.JGsonImpl;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.tuples.X;
import sk.web.infogatherer.WebMethodInfoProviderImpl;
import sk.web.swagger.WebSwaggerGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "CREATE_META", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class WebSwaggerMavenPlugin extends AbstractMojo {
    @Parameter String[] apiClasses;

    @Override
    @SneakyThrows
    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenProject project = (MavenProject) getPluginContext().get("project");

        final String out = St.endWith(project.getRuntimeClasspathElements().get(0), "/");
        getLog().debug("OUTPATH->" + out);

        WebSwaggerGenerator wsg = new WebSwaggerGenerator(
                new WebMethodInfoProviderImpl(new IExcept() {},
                        new ApiClassUtil(new JGsonImpl().init(), O.of(out), s -> Io.sRead(s).oString()))
        );

        final List<? extends Class<?>> apiClasses =
                Cc.stream(this.apiClasses).map($ -> Ex.toRuntime(() -> Class.forName($))).collect(Collectors.toList());

        final Map<String, String> fileContents = apiClasses.stream()
                .map($ -> X.x($.getSimpleName(), wsg.generateSwaggerSpec($, O.empty())))
                .collect(Cc.toMX2());

        fileContents.forEach((k, v) -> {
            Io.reWrite(out + "__jsk_util/swagger/api_specs/" + k + ".json", w -> w.append(v));
        });
    }
}
