package sk.mvn;

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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.javadoc.Javadoc;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import sk.mvn.model.*;
import sk.services.CoreServicesRaw;
import sk.services.nodeinfo.model.ApiBuildInfo;
import sk.utils.async.locks.JLock;
import sk.utils.async.locks.JLockDecorator;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.statics.Io;
import sk.utils.statics.St;
import sk.utils.tuples.X1;
import sk.web.annotations.WebParamsToObject;
import sk.web.annotations.WebPath;

import java.util.*;
import java.util.stream.Collectors;

import static sk.utils.functional.O.*;
import static sk.utils.statics.Fu.equal;
import static sk.web.utils.WebUtils.*;

@Mojo(name = "CREATE_META", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, threadSafe = true)
public class WebMvnApiInfoGenerator extends AbstractMojo {
    private MavenProject project = null;

    public static void main(String[] args) throws MojoFailureException, MojoExecutionException {
        WebMvnApiInfoGenerator webMvnApiInfoGenerator = new WebMvnApiInfoGenerator() {
            @Override
            public Log getLog() {
                return new DefaultLog(new ConsoleLogger());
            }
        };
        webMvnApiInfoGenerator.apiClasses = new String[]{"jsk.spark.TestApi1"};
        webMvnApiInfoGenerator.setPluginContext(Cc.m("project", new MavenProject() {
            @Override
            public List<String> getCompileSourceRoots() {
                return Cc.l("/home/kivan/projects/Actual/jsk/z-module-4-experiments-only/src/main/java");
            }

            @Override
            public List<String> getRuntimeClasspathElements() throws DependencyResolutionRequiredException {
                return Cc.l("/home/kivan/projects/Actual/jsk/z-module-4-experiments-only/target/classes");
            }

            @Override
            public List<String> getTestCompileSourceRoots() {
                return Cc.l("/home/kivan/projects/Actual/jsk/z-module-4-experiments-only/src/test/java");
            }

            @Override
            public List<String> getTestClasspathElements() throws DependencyResolutionRequiredException {
                return Cc.l("/home/kivan/projects/Actual/jsk/z-module-4-experiments-only/target/test-classes");
            }
        }));
        webMvnApiInfoGenerator.execute();
    }

    /**
     * Api classes in the form com.apache.....c.ApiClass
     */
    @Parameter String[] apiClasses;

    /**
     * if set - we get paths from both compile source root and from here
     */
    @Parameter String[] basePaths;

    private static final JLock lock = new JLockDecorator();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        lock.runInLock(() -> {
            ApiClassUtil util = getApiClassUtil();
            generateGitVersion(util);
            generateApiClasses(util);
        });
    }

    private void generateGitVersion(ApiClassUtil util) {
        try {
            final Io.ExecuteInfo hash = Io.execute("git rev-parse --short HEAD");
            final Io.ExecuteInfo order = Io.execute("git rev-list --count HEAD");

            String version = Cc.join("-", Cc.l(hash.getOutput(), order.getOutput()));
            long date = System.currentTimeMillis();

            MavenProject project = (MavenProject) getPluginContext().get("project");
            String outPath = project.getRuntimeClasspathElements().get(0);

            util.saveVersionAndBuildTime(outPath, new ApiBuildInfo(version, date));
        } catch (Exception e) {
            getLog().error(e);
        }
    }

    private void generateApiClasses(ApiClassUtil util) {
        getLog().info(Arrays.toString(apiClasses));
        getLog().info(Cc.joinMap("\n", " : ", getPluginContext(), (k, v) -> k.toString(),
                (k, v) -> v.getClass() + " = " + v.toString()));
        try {
            MavenProject project = (MavenProject) getPluginContext().get("project");
            List<String> compileSourceRoots = Cc.addAll(new ArrayList<>(project.getCompileSourceRoots()),
                    basePaths != null ? Cc.l(basePaths) : Cc.lEmpty());
            String outPath = project.getRuntimeClasspathElements().get(0);
            List<String> compileSourceRootsTest = project.getTestCompileSourceRoots();
            String outPathTest = project.getTestClasspathElements().get(0);

            Cc.stream(apiClasses).forEach(apiCls -> {
                try {
                    processApiClass(util, compileSourceRoots, outPath, apiCls);
                } catch (IllegalArgumentException e) {
                    processApiClass(util, compileSourceRootsTest, outPathTest, apiCls);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void processApiClass(ApiClassUtil util, List<String> compileSourceRoots, String outPath, String apiCls) {
        synchronized (apiCls.intern()) {
            //1 find source
            String apiClassContents = findApiClassContents(apiCls, compileSourceRoots)
                    .orElseGet(() -> error("Can't find api class " + apiCls + " in paths: " + Cc.join(compileSourceRoots)));
            //2 compile it
            CompilationUnit cc = compile(apiClassContents);

            //3 gather all needed information to data structures
            Objects.requireNonNull(cc);
            ClassOrInterfaceDeclaration apiDesc = cc.getInterfaceByName(St.subLL(apiCls, "."))
                    .orElseGet(() -> error("Can't find interface:" + apiCls));

            Map<String, ApiMethodModel> methods = getApiMethodModel(util, apiDesc);
            Map<String, ApiDtoClassModel> classes = getApiClassesModel(compileSourceRoots, methods.values());

            ApiClassModel apiClassModel = new ApiClassModel(apiCls,
                    getJavadocNoParams(apiDesc.getJavadoc()),
                    methods, classes);

            //4 write them into the model file
            util.saveApiClassToResources(outPath, apiClassModel);
        }
    }

    private Map<String, ApiDtoClassModel> getApiClassesModel(List<String> compileSourceRoots,
            Collection<ApiMethodModel> methods) {

        final Map<String, ApiDtoClassModel> processedTypes = Cc.m();
        final Queue<String> typesToProcess = new ArrayDeque<>();
        C1<O<String>> typeProcessor = (otype) -> {
            if (otype.isEmpty()) {
                return;
            }
            String type = otype.get();
            final List<String> newTypes = dtoTypeProcessorOrFilter(type);
            for (String newType : newTypes) {
                if (!(processedTypes.containsKey(newType) || typesToProcess.contains(newType))) {
                    typesToProcess.add(newType);
                }
            }
        };
        methods.stream().forEach($ -> {
            $.getReturnInfo().ifPresent(x -> typeProcessor.accept(x.getTypeSimpleName()));
            $.getParams().forEach(x -> typeProcessor.accept(x.getTypeSimpleName()));
        });


        while (typesToProcess.size() > 0) {
            final String currentType = typesToProcess.poll();
            searchTypeFileContents(currentType, compileSourceRoots).ifPresentOrElse(
                    typeContents -> {
                        try {
                            final CompilationUnit compile = compile(typeContents);
                            boolean isEnum = compile.getEnumByName(currentType).isPresent();

                            processedTypes.put(currentType, isEnum
                                                            ? getEnumType(currentType, compile.getEnumByName(currentType).get())
                                                            : getClassTypeAndAddNewTypesToQueue(currentType,
                                                                    compile.getClassByName(currentType).get(),
                                                                    typeProcessor::accept));
                        } catch (Exception e) {
                            processedTypes.put(currentType, emptyDtoTypeCreator(currentType));
                        }
                    },
                    () -> processedTypes.put(currentType, emptyDtoTypeCreator(currentType))
            );
        }

        return processedTypes;
    }

    private ApiDtoClassModel getClassTypeAndAddNewTypesToQueue(String currentType, ClassOrInterfaceDeclaration clz,
            C1<O<String>> tryAddToQueue) {
        O<String> oParentType = clz.getExtendedTypes().size() > 0
                                ? O.ofNull(clz.getExtendedTypes().get(0).asString())
                                : empty();

        List<ApiFieldOrParameterModel> fields = clz.getFields().stream()
                .filter($ -> !$.isStatic() && !$.isFinal() && !$.isTransient() &&
                             !$.getVariable(0).getNameAsString().equals("__class"))
                .map($ -> new ApiFieldOrParameterModel($.getVariable(0).getNameAsString(),
                        false,
                        getJavadocNoParams($.getJavadoc()),
                        O.ofNull($.getVariable(0).getTypeAsString())
                ))
                .peek($ -> tryAddToQueue.accept($.getTypeSimpleName()))
                .collect(Cc.toL());

        return new ApiDtoClassModel(currentType, false, fields, oParentType.map(parentType -> {
            tryAddToQueue.accept(ofNull(parentType));
            return parentType;
        }), getJavadocNoParams(clz.getJavadoc()));
    }

    private ApiDtoClassModel getEnumType(String name, EnumDeclaration enumDeclaration) {
        List<ApiFieldOrParameterModel> fields = enumDeclaration.getEntries().stream()
                .map($ -> new ApiFieldOrParameterModel($.getNameAsString(), false, getJavadocNoParams($.getJavadoc()), null))
                .collect(Cc.toL());
        return new ApiDtoClassModel(name, true, fields, empty(), getJavadocNoParams(enumDeclaration.getJavadoc()));
    }

    private O<String> searchTypeFileContents(String type, List<String> compileSourceRoots) {
        X1<String> foundText = new X1<>(null);
        for (String compileSourceRoot : compileSourceRoots) {
            Io.visitEachFileWithFinish(compileSourceRoot, file -> {
                if (equal(type, file.getName().replace(".java", ""))) {
                    foundText.set(Io.sRead(file.getAbsolutePath()).string());
                    return false;
                } else {
                    return true;
                }
            });
        }
        return O.ofNull(foundText.get());
    }

    protected ApiClassUtil getApiClassUtil() {
        return new ApiClassUtil(CoreServicesRaw.services().json());
    }


    private Map<String, ApiMethodModel> getApiMethodModel(ApiClassUtil apiUtil, ClassOrInterfaceDeclaration apiDesc) {
        O<String> clsPath = getWebPathValue(apiDesc.getAnnotations());

        return apiDesc.getMethods().stream().map(methodDesc -> {
            Map<String, String> paramComments = getJavadocMapBlock(methodDesc.getJavadoc(), "param");
            List<ApiFieldOrParameterModel> parameters = methodDesc.getParameters().stream()
                    .map(param -> new ApiFieldOrParameterModel(param.getNameAsString(),
                            param.getAnnotations().getFirst()
                                    .map($ -> Fu.equalIgnoreCase($.getName().getIdentifier(),
                                            WebParamsToObject.class.getSimpleName()))
                                    .orElse(false),
                            ofNull(paramComments.get(param.getNameAsString())),
                            O.ofNull(param.getTypeAsString())
                    ))
                    .collect(Cc.toL());

            Map<String, String> exceptComments = Cc.putAll(getJavadocMapBlock(methodDesc.getJavadoc(), "exception"),
                    getJavadocMapBlock(methodDesc.getJavadoc(), "throws"));
            List<ApiExceptionInfo> exceptions = exceptComments.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map($ -> new ApiExceptionInfo($.getKey(), $.getValue()))
                    .collect(Cc.toL());

            O<String> methodPath = getWebPathValue(methodDesc.getAnnotations());
            boolean isMethodAppended = getIsMethodAppended(methodDesc.getAnnotations());
            String path =
                    getMethodApiPath(clsPath.map($ -> St.endWith($, "/")).orElse(""), methodDesc.getNameAsString(), methodPath,
                            isMethodAppended);

            ApiMethodModel apiMethodModel = new ApiMethodModel(methodDesc.getNameAsString(),
                    path,
                    getJavadocNoParams(methodDesc.getJavadoc()),
                    exceptions,
                    O.ofNull(methodDesc.getTypeAsString()).map(x -> new ApiFieldOrParameterModel(null, false,
                            getJavadocOneParam(methodDesc.getJavadoc(), "return"), ofNull(x))),
                    parameters);
            String hash = apiUtil.calculateHashCode(
                    apiMethodModel.getMethodFullPath(),
                    apiMethodModel.getReturnInfo().flatMap($ -> $.getTypeSimpleName()).orElse(""),
                    apiMethodModel.getParams().stream().map($ -> $.getTypeSimpleName().orElse("")).collect(Cc.toL())
            );

            apiMethodModel.setNameAndParamHash(hash);

            return apiMethodModel;
        }).collect(Cc.toM(ApiMethodModel::getMName, $ -> $));
    }

    private boolean getIsMethodAppended(NodeList<AnnotationExpr> annotations) {
        return annotations.stream()
                .filter($ -> equal($.getName().asString(), WebPath.class.getSimpleName()))
                .map($ -> {
                    if ($ instanceof NormalAnnotationExpr) {
                        return ((NormalAnnotationExpr) $).getPairs().stream()
                                .filter(x -> equal(x.getName().toString(), "appendMethodName"))
                                .map(x -> x.getValue().asBooleanLiteralExpr().getValue())
                                .findAny().orElse(true);
                    } else if ($ instanceof SingleMemberAnnotationExpr) {
                        return true;
                    } else {
                        return true;
                    }
                })
                .findAny().orElse(true);
    }


    private O<String> getWebPathValue(NodeList<AnnotationExpr> annotations) {
        String val = annotations.stream()
                .filter($ -> equal($.getName().asString(), WebPath.class.getSimpleName()))
                .map($ -> {
                    if ($ instanceof NormalAnnotationExpr) {
                        return ((NormalAnnotationExpr) $).getPairs().stream()
                                .filter(x -> equal(x.getName().toString(), "value"))
                                .map(x -> x.getValue().asStringLiteralExpr().asString())
                                .findAny().orElse("");
                    } else if ($ instanceof SingleMemberAnnotationExpr) {
                        return ((SingleMemberAnnotationExpr) $).getMemberValue().asStringLiteralExpr().asString();
                    } else {
                        return "/";
                    }
                })
                .findAny().orElse("");
        return St.isNullOrEmpty(val) ? empty() : of(val);
    }

    private CompilationUnit compile(String apiClassContents) {
        ParseResult<CompilationUnit> res = new JavaParser().parse(apiClassContents);
        CompilationUnit cc = null;
        if (res.getResult().isPresent()) {
            cc = res.getResult().get();
        } else {
            error(res.getProblems().stream().map($ -> $.getVerboseMessage()).collect(Collectors.joining("\n")));
        }
        return cc;
    }

    private <T> T error(String error) {
        getLog().error(error);
        throw new IllegalArgumentException(error);
    }

    private O<String> findApiClassContents(String apiCls, List<String> compileSourceRoots) {
        apiCls = apiCls.replace(".", "/") + ".java";
        for (String compileSourceRoot : compileSourceRoots) {
            String path = St.endWith(compileSourceRoot, "/") + apiCls;
            getLog().info("Checking: " + path);
            O<String> classFile = Io.sRead(path).oString();
            if (classFile.isPresent()) {
                getLog().info("Found api class");
                return classFile;
            }
        }
        return empty();
    }

    private Map<String, String> getJavadocMapBlock(Optional<Javadoc> javadoc, String javadocParam) {
        return javadoc.map(c -> c.getBlockTags().stream()
                        .filter(t -> t.getTagName().equalsIgnoreCase(javadocParam) && t.getName().isPresent())
                        .collect(Cc.toM(t -> t.getName().get(), t -> t.getContent().toText(), (a, b) -> a)))
                .orElse(Cc.m());
    }

    private O<String> getJavadocNoParams(Optional<Javadoc> javadoc) {
        return O.of(javadoc.map(c -> c.getDescription().toText().trim()).filter(c -> !c.isEmpty()));
    }

    private O<String> getJavadocOneParam(Optional<Javadoc> javadoc, String javadocParam) {
        return O.of(javadoc.flatMap(c -> c.getBlockTags().stream()
                .filter(t -> t.getTagName().equalsIgnoreCase(javadocParam))
                .map($ -> $.getContent().toText())
                .findFirst()
        ));
    }
}
