package sk.db.util.generator;

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

import sk.db.util.generator.model.JsaFileInfo;
import sk.db.util.generator.model.entity.JsaEntityCompositeKey;
import sk.db.util.generator.model.entity.JsaEntityField;
import sk.db.util.generator.model.entity.JsaFullEntityModel;
import sk.db.util.generator.model.output.JsaEmbeddedKeyOutput;
import sk.db.util.generator.model.output.JsaEntityOutput;
import sk.db.util.generator.model.output.JsaPrimaryKeyOutput;
import sk.db.util.generator.model.output.JsaStorageOutput;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.services.bytes.IBytes;
import sk.services.free.IFree;
import sk.services.ids.IIds;
import sk.services.rand.IRand;
import sk.utils.functional.C1;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

public class JsaExporter {
    static ICoreServices core = new CoreServicesRaw();
    static IRand rnd = core.rand();
    static IBytes bytes = core.bytes();
    static IIds ids = core.ids();
    static IFree free = core.free();

    public static void export(C1<JsaFileInfo> fileProcessor, JsaFullEntityModel entities, String schema) {
        final String pkg = "__" + rnd.rndString(10, St.eng);
        final String pkgFolder = St.endWith(pkg, "/");

        final String prefix = St.capFirst(schema);
        if (entities.entities().size() > 0) {
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + prefix + "StorageFacade.java",
                    free.process("jsa_storage_facade.ftl",
                            Cc.m("model", new JsaStorageOutput(pkg, prefix, entities.entities())))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + prefix + "StorageFacadeImpl.java",
                    free.process("jsa_storage_facade_impl.ftl",
                            Cc.m("model", new JsaStorageOutput(pkg, prefix, entities.entities())))
            ));
        }

        entities.entities().forEach((entity) -> {
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getCls() + ".java",
                    free.process("jsa_entity_impl.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getIFace() + ".java",
                    free.process("jsa_entity_iface.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
            if (entity.isComposite()) {
                JsaEntityCompositeKey composite = entity.getCompositeId().get();
                for (JsaEntityField insideCompositeField : composite.getCompositeFields()) {
                    if (insideCompositeField.isNeedSeparateIdFile()) {
                        fileProcessor.accept(new JsaFileInfo(
                                pkgFolder + insideCompositeField.getMainType() + ".java",
                                free.process("jsa_entity_id.ftl",
                                        Cc.m("model", new JsaPrimaryKeyOutput(pkg, insideCompositeField)))
                        ));
                    }
                }
                fileProcessor.accept(new JsaFileInfo(
                        pkgFolder + entity.getIdField().getMainType() + "JpaImpl.java",
                        free.process("jsa_composite_entity_id_impl.ftl",
                                Cc.m("model", new JsaEmbeddedKeyOutput(pkg, composite.getClassName(),
                                        entity.getCompositeId().get().getCompositeFields())))
                ));
                fileProcessor.accept(new JsaFileInfo(
                        pkgFolder + entity.getIdField().getMainType() + ".java",
                        free.process("jsa_composite_entity_id.ftl",
                                Cc.m("model", new JsaEmbeddedKeyOutput(pkg, composite.getClassName(),
                                        entity.getCompositeId().get().getCompositeFields())))
                ));
            } else {
                fileProcessor.accept(new JsaFileInfo(
                        pkgFolder + entity.getIdField().getMainType() + ".java",
                        free.process("jsa_entity_id.ftl", Cc.m("model", new JsaPrimaryKeyOutput(pkg, entity.getIdField())))
                ));
            }
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getCls() + "Repo.java",
                    free.process("jsa_entity_repo.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
        });
    }
}
