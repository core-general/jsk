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

import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.Template;
import freemarker.template.Version;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import sk.utils.javafixes.BuilderStringWriter;

import java.io.StringReader;

@SuppressWarnings("unused")
@Log4j2
public class Freemarker implements IFree {
    private final Configuration resourceFreemarker;
    private final Configuration textFreemarker;
    private final Configuration resourceFreemarkerHtml;
    private final Configuration textFreemarkerHtml;

    @SneakyThrows
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

    @SneakyThrows
    @Override
    public String process(String templatePath, Object model) {
        return process(templatePath, model, false);
    }

    @SneakyThrows
    @Override
    public String processByText(String templateText, Object model) {
        return processByText(templateText, model, false);
    }

    @Override
    public String processHtml(String templatePath, Object model) {
        return process(templatePath, model, true);
    }

    @Override
    public String processByTextHtml(String templateText, Object model) {
        return processByText(templateText, model, true);
    }

    @SneakyThrows
    private String process(String templatePath, Object model, boolean html) {
        BuilderStringWriter sw = new BuilderStringWriter();
        (html ? resourceFreemarkerHtml : resourceFreemarker).getTemplate(templatePath).process(model, sw);
        return sw.toString();
    }

    @SneakyThrows
    private String processByText(String templateText, Object model, boolean html) {
        BuilderStringWriter sw = new BuilderStringWriter();
        new Template("_", new StringReader(templateText), (html ? textFreemarkerHtml : textFreemarker)).process(model, sw);
        return sw.toString();
    }
}
