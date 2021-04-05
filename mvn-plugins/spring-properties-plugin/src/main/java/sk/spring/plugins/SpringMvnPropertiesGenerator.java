package sk.spring.plugins;/*-
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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import sk.utils.statics.Cc;
import sk.utils.statics.Ex;
import sk.utils.statics.Io;

import java.io.File;
import java.util.List;

@Mojo(name = "CREATE_META", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class SpringMvnPropertiesGenerator extends AbstractMojo {
    /**
     * Property path to scan for properties
     * Property structure must be:
     * $propertyPathPrefixes/...property
     * or
     * $propertyPathPrefixes/$AppProfile/...property
     */
    @Parameter
    String propertyPathPrefix;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (propertyPathPrefix == null) {
                throw new RuntimeException("propertyPathPrefix must be set!");
            }

            MavenProject project = (MavenProject) getPluginContext().get("project");

            List<String> properties = Cc.l();
            final String parentFolder = project.getCompileClasspathElements().get(0);
            final String parentPath = new File(parentFolder).getAbsolutePath();
            Io.visitEachFile(parentFolder, file -> {
                String ff = file.getAbsolutePath().replace(parentPath, "");

                String name = ff.charAt(0) == '/' ? ff.substring(1) : ff;
                getLog().info("Processing file:" + name);
                if (Cc.stream(propertyPathPrefix).anyMatch(path -> name.startsWith(path)) && name.endsWith(".properties")) {
                    getLog().info("Added property file:" + name);
                    properties.add(name);
                }
            });

            getLog().info("Output to:" + parentFolder);
            PropertyMetaService.savePropertyNames(parentFolder, propertyPathPrefix, properties);
        } catch (DependencyResolutionRequiredException e) {
            Ex.thRow(e);
        }
    }
}
