package sk.services.free;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.Version;
import lombok.extern.log4j.Log4j2;
import sk.services.ids.IIds;
import sk.utils.functional.F0E;
import sk.utils.javafixes.BuilderStringWriter;
import sk.utils.statics.Ex;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
@Log4j2
public class Freemarker implements IFree {
    @Inject IIds ids;
    @Inject Optional<TemplateLoader> templateLoader = Optional.empty();

    private final Configuration resourceFreemarker;
    private final Configuration textFreemarker;
    private final Configuration resourceFreemarkerHtml;
    private final Configuration textFreemarkerHtml;

    final ConcurrentMap<String, Template> templateCache = new ConcurrentHashMap<>();

    public Freemarker(IIds ids) {
        this(ids, null);
    }

    public Freemarker(IIds ids, TemplateLoader tl) {
        this();
        this.ids = ids;
        templateLoader = Optional.ofNullable(tl);
        postInit();
    }

    public Freemarker() {
        resourceFreemarker = new Configuration(new Version("2.3.23"));
        resourceFreemarker.setLocalizedLookup(false);
        resourceFreemarker.setClassForTemplateLoading(this.getClass(), "/");

        resourceFreemarkerHtml = new Configuration(new Version("2.3.23"));
        resourceFreemarkerHtml.setLocalizedLookup(false);
        resourceFreemarkerHtml.setClassForTemplateLoading(this.getClass(), "/");
        resourceFreemarkerHtml.setOutputFormat(HTMLOutputFormat.INSTANCE);

        textFreemarker = new Configuration(new Version("2.3.23"));
        textFreemarker.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version("2.3.23")).build());

        textFreemarkerHtml = new Configuration(new Version("2.3.23"));
        textFreemarkerHtml.setObjectWrapper(new DefaultObjectWrapperBuilder(new Version("2.3.23")).build());
        textFreemarkerHtml.setOutputFormat(HTMLOutputFormat.INSTANCE);
    }

    @PostConstruct
    public Freemarker postInit() {
        templateLoader.ifPresent($ -> {
            textFreemarker.setTemplateLoader($);
            textFreemarkerHtml.setTemplateLoader($);
        });
        ClassTemplateLoader classTemplateLoader = new ClassTemplateLoader(this.getClass(), "/");
        resourceFreemarker.setTemplateLoader(classTemplateLoader);
        resourceFreemarkerHtml.setTemplateLoader(classTemplateLoader);
        return this;
    }

    @Override
    public String process(String templatePath, Object model) {
        return processInner(templatePath, model, false);
    }

    @Override
    public String processByText(String templateText, Object model, boolean templateTextIsCacheable) {
        return processByTextInner(templateText, model, false, templateTextIsCacheable);
    }

    @Override
    public String processHtml(String templatePath, Object model) {
        return processInner(templatePath, model, true);
    }

    @Override
    public String processByTextHtml(String templateText, Object model, boolean templateTextIsCacheable) {
        return processByTextInner(templateText, model, true, templateTextIsCacheable);
    }

    private String processInner(String templatePath, Object model, boolean html) {
        BuilderStringWriter sw = new BuilderStringWriter();
        try {
            (html ? resourceFreemarkerHtml : resourceFreemarker).getTemplate(templatePath).process(model, sw);
        } catch (Exception e) {
            return Ex.thRow(e);
        }
        return sw.toString();
    }


    private String processByTextInner(String templateText, Object model, boolean html, boolean templateIsCacheable) {
        BuilderStringWriter sw = new BuilderStringWriter();
        try {
            F0E<Template> templater =
                    () -> new Template("_", new StringReader(templateText), (html ? textFreemarkerHtml : textFreemarker));
            Template template;
            if (templateIsCacheable) {
                template = templateCache.computeIfAbsent(ids.unique(templateText), (k) -> {
                    try {
                        return templater.apply();
                    } catch (Exception e) {
                        return Ex.thRow(e);
                    }
                });
            } else {
                template = templater.apply();
            }
            template.process(model, sw);
        } catch (Exception e) {
            return Ex.thRow(e);
        }
        return sw.toString();
    }
}
