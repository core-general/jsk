package net.bull.javamelody.internal.web.html;

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

import net.bull.javamelody.internal.model.Collector;
import net.bull.javamelody.internal.model.Range;
import sk.utils.statics.Ex;

import java.io.IOException;
import java.io.Writer;

public class HtmlHitsRequestGraphReport extends HtmlAbstractReport {
    private final Range range;

    HtmlHitsRequestGraphReport(Range range, Writer writer) {
        super(writer);
        this.range = range;
    }

    @Override
    void toHtml() {
        throw new UnsupportedOperationException();
    }

    int addGraphForHits(String graphName, int uniqueByPageAndGraphSequence) {
        try {
            if (graphName.length() > 40) {
                int position = graphName.length() - 40;
                String hitsGraphName = graphName.substring(0, position) + "hits" + graphName.substring(position);

                write("<br/><img src='?graph=");
                write(hitsGraphName);
                write("&amp;width=100&amp;height=50' id='");
                write("id" + ++uniqueByPageAndGraphSequence);
                write("' alt='graph'/></em>");
            }
            return uniqueByPageAndGraphSequence;
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    String addRequestGraphForHitsDetail(Collector collector, String graphName) {
        try {
            String hitsGraphName = null;
            if (graphName.length() > 40) {
                int position = graphName.length() - 40;
                hitsGraphName = graphName.substring(0, position) + "hits" + graphName.substring(position);
                writeln("<tr><td>");
                final String graphNameEncoded = urlEncode(hitsGraphName);
                writeln("<div style='font-size:15px'>Hits statistics:</div>");
                writeln("<img class='synthèse' style='margin-top:10px' id='img' src='" + "?width=960&amp;height=400&amp;graph="
                        + graphNameEncoded + "' alt='zoom'/>");
                writeDirectly("<br/><div align='right' style='color: #808080;'>");
                writeln("#graph_units#");
                writeln("</div><div align='right'>");
                writeln("<a href='?part=lastValue&amp;graph=" + graphNameEncoded
                        + "' title='#Lien_derniere_valeur#'>#derniere_valeur#</a>");
                writeln("&nbsp;&nbsp;&nbsp;<a href='?format=xml&amp;period="
                        + range.getValue().replace("|", "%7C") + "&amp;graph=" + graphNameEncoded
                        + "' title='Dump XML'>XML</a>");
                writeln("&nbsp;&nbsp;&nbsp;<a href='?format=txt&amp;period="
                        + range.getValue().replace("|", "%7C") + "&amp;graph=" + graphNameEncoded
                        + "' title='Dump TXT'>TXT</a>");
                writeln("</div></td></tr>");
            }

            return hitsGraphName;
        } catch (IOException e) {
            return Ex.thRow(e);
        }
    }

    private void writeGraphDetailScript(String graphName) throws IOException {
        writeln("<script type='text/javascript'>");
        writeln("function handleHideMaximumClick(checkbox) {");
        writeln("    var img = document.getElementById('img');");
        writeln("    if (checkbox.checked) {");
        writeln("        img.src = img.src + '\\u0026max=false\\u0026r=' + Math.random();");
        writeln("    } else {");
        writeln("        img.src = img.src.replace('\\u0026max=false','');");
        writeln("    }");
        writeln("}");
        writeln("function scaleImage(v, min, max) {");
        writeln("    var images = document.getElementsByClassName('synthèse');");
        writeln("    w = (max - min) * v + min;");
        writeln("    for (i = 0; i < images.length; i++) {");
        writeln("        images[i].style.width = w + 'px';");
        writeln("    }");
        writeln("}");

        // 'animate' our slider
        writeln("var slider = new Control.Slider('handle', 'track', {axis:'horizontal', alignX: 0, increment: 2});");

        // resize the image as the slider moves. The image quality would deteriorate, but it
        // would not be final anyway. Once slider is released the image is re-requested from the server, where
        // it is rebuilt from vector format
        writeln("slider.options.onSlide = function(value) {");
        writeln("  scaleImage(value, initialWidth, initialWidth / 2 * 3);");
        writeln("}");

        // this is where the slider is released and the image is reloaded
        // we use current style settings to work the required image dimensions
        writeln("slider.options.onChange = function(value) {");
        // chop off "px" and round up float values
        writeln("  width = Math.round(Element.getStyle('img','width').replace('px','')) - 80;");
        writeln("  height = Math.round(width * initialHeight / initialWidth) - 48;");
        // reload the images
        // rq : on utilise des caractères unicode pour éviter des warnings
        writeln("  document.getElementById('img').src = '?graph="
                + htmlEncodeButNotSpace(urlEncode(graphName))
                + "\\u0026width=' + width + '\\u0026height=' + height;");
        writeln("  document.getElementById('img').style.width = '';");
        writeln("}");
        writeln("window.onload = function() {");
        writeln("  if (navigator.appName == 'Microsoft Internet Explorer') {");
        writeln("    initialWidth = document.getElementById('img').width;");
        writeln("    initialHeight = document.getElementById('img').height;");
        writeln("  } else {");
        writeln("    initialWidth = Math.round(Element.getStyle('img','width').replace('px',''));");
        writeln("    initialHeight = Math.round(Element.getStyle('img','height').replace('px',''));");
        writeln("  }");
        writeln("}");
        writeln("</script>");
    }
}
