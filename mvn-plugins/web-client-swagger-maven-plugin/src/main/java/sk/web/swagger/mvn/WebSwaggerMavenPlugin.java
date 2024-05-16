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
import sk.services.CoreServicesRaw;
import sk.services.except.IExcept;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.O;
import sk.utils.statics.*;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;
import sk.web.infogatherer.WebMethodInfoProviderImpl;
import sk.web.swagger.WebSwaggerGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "CREATE_META", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class WebSwaggerMavenPlugin extends AbstractMojo {
    @Parameter String[] apiClasses;
    @Parameter String[] generators = new String[0];

    private static final JLock lock = new JLockDecorator();

    @Override
    @SneakyThrows
    public void execute() throws MojoExecutionException, MojoFailureException {
        lock.runInLockRE(() -> {
            MavenProject project = (MavenProject) getPluginContext().get("project");

            final String out = St.endWith(project.getRuntimeClasspathElements().get(0), "/");
            getLog().debug("OUTPATH->" + out);

            WebSwaggerGenerator wsg = new WebSwaggerGenerator(
                    new WebMethodInfoProviderImpl(new IExcept() {},
                            new ApiClassUtil(CoreServicesRaw.services().json(), O.of(out), s -> Io.sRead(s).oString())));

            final Map<String, X2<String, Class>> fileContents = Cc.stream(this.apiClasses)
                    .map($1 -> Ex.toRuntime(() -> Class.forName($1)))
                    .map($ -> X.x(out + "__jsk_util/swagger/api_specs/" + $.getSimpleName() + ".json",
                            X.x(wsg.generateSwaggerSpec($, O.empty()), (Class) $)))
                    .collect(Cc.toMX2());

            final List<X2<WebSwaggerMavenGeneratorTypes, String>> generators = Cc.stream(this.generators)
                    .map($ -> {
                        final String[] lr = $.split(":");
                        final WebSwaggerMavenGeneratorTypes type =
                                Re.findInEnum(WebSwaggerMavenGeneratorTypes.class, lr[0])
                                        .orElseThrow(() -> new RuntimeException("Can't find generator for: " + lr[0]));

                        return X.x(type, lr[1]);
                    })
                    .collect(Collectors.toList());

            fileContents.forEach((k, v) -> {
                synchronized (k.intern()) {
                    Io.reWrite(k, w -> w.append(v.i1()));
                    for (X2<WebSwaggerMavenGeneratorTypes, String> generator : generators) {

                        Io.deleteIfExists(St.endWith(generator.i2(), "/") + v.i2().getSimpleName());
                        final String pckg = v.i2().getName().replace("." + v.i2().getSimpleName(), "");
                        final List<String> params = Cc.l("generate",
                                "-i", k,
                                "-o", St.endWith(generator.i2(), "/") + v.i2().getSimpleName(),
                                "-g", generator.i1().getGeneratorName(),
                                "--global-property", "skipFormModel=false"
                        );

                        params.add("--additional-properties=pubName=" + v.i2().getSimpleName());

                        generator.i1.getTemplatePath().ifPresent(path -> params.addAll(Cc.l("-t", path)));

                        OpenAPIGenerator.main(params.toArray(new String[0]));
                    }
                }
            });
        });
    }
}
