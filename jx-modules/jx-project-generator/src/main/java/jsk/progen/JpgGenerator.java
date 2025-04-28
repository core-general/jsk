package jsk.progen;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2025 Core General
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

import jsk.progen.model.JpgModel;
import jsk.progen.model.JpgPackage;
import jsk.progen.model.JpgServiceDefinition;
import jsk.progen.model.JpgServiceId;
import jsk.progen.model.creation.*;
import jsk.progen.model.enums.JpgCommonModule;
import jsk.progen.model.enums.JpgFileTemplates;
import jsk.progen.model.enums.JpgServiceModule;
import sk.services.comparer.MapCompareTool;
import sk.services.comparer.SetCompareTool;
import sk.services.comparer.model.MapCompareResult;
import sk.services.comparer.model.SetCompareResult;
import sk.utils.files.PathWithBase;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Fu;
import sk.utils.tuples.X2;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

public class JpgGenerator {
    public void generate(JpgModel newModel, PathWithBase outputDir, JpgPackage packageRoot) {
        JpgModel existingModel = parseModelFromDir(outputDir);

        MapCompareResult<JpgServiceId, JpgServiceDefinition> difServices =
                MapCompareTool.compare(newModel.getServices(), existingModel.getServices());
        Map<JpgServiceId, JpgServiceDefinition> newServices = difServices.getIn1NotIn2();
        Map<JpgServiceId, X2<JpgServiceDefinition, JpgServiceDefinition>> updatedDefinitions = difServices.getExistButDifferent();

        List<JpgCreationTaskBase<?>> toCreate = Cc.l();
        newServices.forEach((k, v) -> {
            JpgTaskContextService ctx = new JpgTaskContextService(outputDir, packageRoot, k);
            v.getUsedCommonModules().forEach(module -> toCreate.add(new JpgCreationTaskNormalModule(ctx, module)));
            toCreate.add(new JpgCreationTaskTopLevelServiceBuild(ctx));
        });

        updatedDefinitions.forEach((k, v) -> {
            JpgTaskContextService ctx = new JpgTaskContextService(outputDir, packageRoot, k);
            if (v.i1().isHasRootBuild() && !v.i2().isHasRootBuild()) {
                toCreate.add(new JpgCreationTaskTopLevelServiceBuild(ctx));
            }

            SetCompareResult<JpgServiceModule> dif =
                    SetCompareTool.compare(v.i1().getUsedCommonModules(), v.i2().getUsedCommonModules());
            Set<JpgServiceModule> newModules = dif.getIn1NotIn2();
            newModules.forEach(nm -> toCreate.add(new JpgCreationTaskNormalModule(ctx, nm)));
        });


        List<File> needPomTuning = findNeedPomTuning(outputDir);
        for (File file : needPomTuning) {
            toCreate.add(new JpgCreationTaskTopLevelPomRewrite(
                    new JpgTaskContextFolder(outputDir, PathWithBase.pwb(file.getAbsolutePath()), packageRoot, newModel),
                    JpgFileTemplates.L0_TOP_BUILD_POM));
        }

        executeTasks(toCreate, newModel);
    }

    private static List<File> findNeedPomTuning(PathWithBase outputDir) {
        List<File> needPomTuning = Cc.l();
        FileFilter isFolderAndIsNotLeafFolder = f -> {
            if (!f.isDirectory()) {return false;}
            File[] files = f.listFiles();
            return files != null && Arrays.stream(files).noneMatch($ -> "src".equals($.getName()));
        };
        Queue<File> folderQueue = new LinkedList<>();

        C1<File> addToQueu = file -> {
            folderQueue.add(file);
            needPomTuning.add(file);

        };
        addToQueu.accept(new File(outputDir.getPathWithSlash()));
        while (folderQueue.size() > 0) {
            File curFolder = folderQueue.poll();

            File[] childOkFolders = curFolder.listFiles(isFolderAndIsNotLeafFolder);
            for (File childOkFolder : childOkFolders) {
                addToQueu.accept(childOkFolder);
            }
        }
        return needPomTuning;
    }

    private void executeTasks(List<JpgCreationTaskBase<?>> toCreate, JpgModel newModel) {
        Queue<JpgCreationTaskBase<?>> tasks = new LinkedList<>(toCreate);
        Set<JpgServiceModule> doneServiceModules = Cc.s();
        Set<JpgCommonModule> doneCommonModules = Cc.s();
        while (!tasks.isEmpty()) {
            JpgCreationTaskBase<?> task = tasks.poll();
            //todo not needs some work
            //    boolean result = switch (task) {
            //        case JpgCreationTaskNormalModule normalTask -> {
            //            JpgServiceModule module = normalTask.getModule();
            //            if (!doneServiceModules.contains(module)) {
            //                doneServiceModules.add(module);
            //                module.getDependentCommonModules().forEach(commonModule -> {
            //                    if (!doneCommonModules.contains(commonModule)) {
            //                        doneCommonModules.add(commonModule);
            //                        tasks.add(new JpgCreationTaskCommonModule(normalTask.getCtx(), commonModule));
            //                    }
            //                });
            //                module.getDependentServiceModules().forEach(servModule -> {
            //                    if (!doneServiceModules.contains(servModule)) {
            //                        doneServiceModules.add(servModule);
            //                        tasks.add(new JpgCreationTaskNormalModule(normalTask.getCtx(), servModule));
            //                    }
            //                });
            //            }
            //            //module
            //        }
            //        case JpgCreationTaskCommonModule jpgCreationTaskCommonModule -> {
            //
            //        }
            //        case JpgCreationTaskTopLevelServiceBuild topBuildTask -> {
            //
            //        }
            //        case JpgCreationTaskTopLevelPomRewrite topLevelPomTask -> {
            //
            //        }
            //    };
        }
    }

    private void writeFile(PathWithBase path, String template) {

    }

    private JpgModel parseModelFromDir(PathWithBase outputDir) {
        List<File> files = Arrays.asList(O.ofNull(new File(outputDir.getPathWithSlash()).listFiles()).orElse(new File[0]));

        Set<JpgServiceId> topLevelBuilds = files.stream()
                .filter($ -> Fu.equal($.getName(), "builds"))
                .flatMap($ -> Cc.stream($.listFiles(path -> path.isDirectory() && path.getName().contains("-build"))))
                .map($ -> new JpgServiceId($.getName().replace("-build", "")))
                .collect(Cc.toS());

        FileFilter directoryAndNotTarget = path -> path.isDirectory() && !path.getName().contains("target");
        return new JpgModel(files.stream()
                .filter($ -> Fu.equal($.getName(), "modules"))
                .flatMap($ -> Cc.stream($.listFiles(directoryAndNotTarget)))
                .collect(Cc.toM(moduleFolder -> new JpgServiceId(moduleFolder.getName()),
                        moduleFolder -> new JpgServiceDefinition(
                                topLevelBuilds.contains(new JpgServiceId(moduleFolder.getName())),
                                Cc.stream(moduleFolder.listFiles(directoryAndNotTarget))
                                        .map(subModule -> JpgServiceModule.parse(new JpgServiceId(moduleFolder.getName()),
                                                subModule.getName()))
                                        .filter(om -> om.isPresent())
                                        .map(om -> om.get())
                                        .collect(Cc.toS())))));
    }
}
