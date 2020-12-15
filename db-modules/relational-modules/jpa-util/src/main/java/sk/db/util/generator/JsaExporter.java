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
import sk.db.util.generator.model.entity.JsaEntityModel;
import sk.db.util.generator.model.output.JsaEntityOutput;
import sk.db.util.generator.model.output.JsaPrimaryKeyOutput;
import sk.db.util.generator.model.output.JsaStorageOutput;
import sk.services.bytes.BytesImpl;
import sk.services.bytes.IBytes;
import sk.services.free.Freemarker;
import sk.services.free.IFree;
import sk.services.ids.IIds;
import sk.services.ids.IdsImpl;
import sk.services.rand.IRand;
import sk.services.rand.RandImpl;
import sk.utils.functional.C1;
import sk.utils.statics.Cc;
import sk.utils.statics.St;

import java.util.List;

public class JsaExporter {
    static IRand rnd = new RandImpl();
    static IBytes bytes = new BytesImpl();
    static IIds ids = new IdsImpl(rnd, bytes);
    static IFree free = new Freemarker();

    public static void export(C1<JsaFileInfo> fileProcessor, List<JsaEntityModel> entities, String schema) {
        final String pkg = "__" + rnd.rndString(10, St.eng);
        final String pkgFolder = St.endWith(pkg, "/");

        final String prefix = St.capFirst(schema);
        if (entities.size() > 0) {
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + prefix + "StorageFacade.java",
                    free.process("jsa_storage_facade.ftl",
                            Cc.m("model", new JsaStorageOutput(pkg, prefix, entities)))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + prefix + "StorageFacadeImpl.java",
                    free.process("jsa_storage_facade_impl.ftl",
                            Cc.m("model", new JsaStorageOutput(pkg, prefix, entities)))
            ));
        }

        entities.forEach((entity) -> {
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getCls() + ".java",
                    free.process("jsa_entity_impl.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getInterfce() + ".java",
                    free.process("jsa_entity_iface.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getIdField().getMainType() + ".java",
                    free.process("jsa_entity_id.ftl", Cc.m("model", new JsaPrimaryKeyOutput(pkg, entity.getIdField())))
            ));
            fileProcessor.accept(new JsaFileInfo(
                    pkgFolder + entity.getCls() + "Repo.java",
                    free.process("jsa_entity_repo.ftl", Cc.m("model", new JsaEntityOutput(pkg, schema, entity)))
            ));
        });

    }
}
