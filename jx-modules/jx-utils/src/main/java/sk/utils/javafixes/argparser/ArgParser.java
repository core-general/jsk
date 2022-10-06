package sk.utils.javafixes.argparser;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import lombok.Data;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.tuples.X;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ArgParser<T extends Enum<T> & ArgParserConfig<T>> {
    final Map<T, String> finalValues;

    public O<String> getArg(T t) {
        return O.ofNull(finalValues.get(t));
    }

    public String getRequiredArg(T t) {
        return getArg(t).get();
    }

    public static <T extends Enum<T> & ArgParserConfig<T>> ArgParser<T> parse(String[] args,
            ArgParserConfigProvider<T> configProvider) {
        return new ArgParser<>(args, configProvider);
    }

    private ArgParser(String[] args, ArgParserConfigProvider<T> configProvider) {
        try {
            List<T> noArgConfigs = configProvider.getConfigs().stream()
                    .filter($ -> $.getCommandPrefix().isEmpty())
                    .toList();
            if (noArgConfigs.size() > 1) {
                throw new RuntimeException("No arg prefix configs should not exceed 1!");
            }

            final Map<String, T> commandsToConfigs = configProvider.getConfigs().stream()
                    .filter($ -> $.getCommandPrefix().isPresent())
                    .flatMap($ -> $.getCommandPrefix().get().stream().map($$ -> X.x($$, $)))
                    .collect(Cc.toMX2());

            Set<T> required = configProvider.getConfigs().stream().filter($ -> $.isRequired()).collect(Collectors.toSet());

            ProcessedConfig conf = new ProcessedConfig(Cc.first(noArgConfigs), commandsToConfigs, required);

            finalValues = Cc.m();

            O<T> oCurCommand = O.empty();
            for (String curArg : args) {
                if (oCurCommand.isPresent()) {
                    final T curCommand = oCurCommand.get();
                    finalValues.put(curCommand, curArg);
                    conf.getRequired().remove(curCommand);
                    oCurCommand = O.empty();
                } else {
                    final O<T> curCommand = O.ofNull(conf.getByPrefixes().get(curArg));
                    if (curCommand.isEmpty()) {
                        //cant find command, then no command arg, must be only 1
                        if (!conf.isUsedNoArgParam()) {
                            conf.getNoPrefixArg().map($ -> {
                                finalValues.put($, curArg);
                                conf.getRequired().remove($);
                                conf.setUsedNoArgParam(true);
                                return $;
                            }).orElseThrow(() -> new RuntimeException("Not configured no arg parameter! Here:" + curArg));
                        } else {
                            throw new RuntimeException("More than one no arg parameter used. Second is: " + curArg);
                        }
                    } else {
                        //we found command
                        oCurCommand = curCommand;
                    }
                }
            }

            if (conf.getRequired().size() > 0) {
                throw new RuntimeException("Required parameters: " + Cc.join(conf.getRequired()));
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(fillArgInfo(configProvider.getConfigs()), e);
        }
    }

    private String fillArgInfo(List<T> configs) {
        final ArrayList<T> confs = Cc.sort(new ArrayList<>(configs));
        return "\n" + Cc.join("\n", confs.stream().map($ -> $.asString())) + "\n\n";
    }

    @Data
    private class ProcessedConfig {
        boolean usedNoArgParam = false;
        final O<T> noPrefixArg;
        final Map<String, T> byPrefixes;
        final Set<T> required;
    }
}
