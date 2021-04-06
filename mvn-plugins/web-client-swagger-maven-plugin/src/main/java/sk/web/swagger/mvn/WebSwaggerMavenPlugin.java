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
import org.openapitools.codegen.OpenAPIGenerator;
import sk.mvn.ApiClassUtil;
import sk.services.except.IExcept;
import sk.services.json.JGsonImpl;
import sk.utils.functional.O;
import sk.utils.statics.*;
import sk.utils.tuples.X;
import sk.web.infogatherer.WebMethodInfoProviderImpl;
import sk.web.swagger.WebSwaggerGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "CREATE_META", defaultPhase = LifecyclePhase.PACKAGE)
public class WebSwaggerMavenPlugin extends AbstractMojo {
    @Parameter String[] apiClasses;
    @Parameter String[] generators = new String[0];

    @Override
    @SneakyThrows
    public void execute() throws MojoExecutionException, MojoFailureException {
        MavenProject project = (MavenProject) getPluginContext().get("project");

        final String out = St.endWith(project.getRuntimeClasspathElements().get(0), "/");
        getLog().debug("OUTPATH->" + out);

        WebSwaggerGenerator wsg = new WebSwaggerGenerator(
                new WebMethodInfoProviderImpl(new IExcept() {},
                        new ApiClassUtil(new JGsonImpl().init(), O.of(out), s -> Io.sRead(s).oString())));

        final Map<String, String> fileContents = Cc.stream(this.apiClasses)
                .map($1 -> Ex.toRuntime(() -> Class.forName($1)))
                .map($ -> X.x(out + "__jsk_util/swagger/api_specs/" + $.getSimpleName() + ".json",
                        wsg.generateSwaggerSpec($, O.empty())))
                .collect(Cc.toMX2());

        final List<WebSwaggerMavenGeneratorTypes> generators = Cc.stream(this.generators)
                .map($ -> Re.findInEnum(WebSwaggerMavenGeneratorTypes.class, $)
                        .orElseThrow(() -> new RuntimeException("Can't find generator for: " + $)))
                .collect(Collectors.toList());

        fileContents.forEach((k, v) -> {
            Io.reWrite(k, w -> w.append(v));
            for (WebSwaggerMavenGeneratorTypes generator : generators) {
                OpenAPIGenerator.main(new String[]{
                        "generate",
                        "-i", k,
                        "-o", out + "__jsk_util/swagger/generators/" + generator.name(),
                        "-g", generator.getGeneratorName(),
                        });
            }
        });
    }
}
