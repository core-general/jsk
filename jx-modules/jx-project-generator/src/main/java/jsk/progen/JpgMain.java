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
import lombok.AllArgsConstructor;
import lombok.Getter;
import sk.services.CoreServicesRaw;
import sk.utils.files.PathWithBase;
import sk.utils.functional.O;
import sk.utils.javafixes.argparser.ArgParser;
import sk.utils.javafixes.argparser.ArgParserConfigProvider;
import sk.utils.statics.Io;

import java.util.TreeSet;

import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.ts;

public class JpgMain {
    // TODO UNFINISHED!
    public static void main(String[] input) {
        ArgParser<ARGS> args = ArgParser.parse(input, ARGS.class);
        new JpgGenerator().generate(
                CoreServicesRaw.services().json().from(Io.sRead(args.getRequiredArg(ARGS.CONFIG_PATH)).string(), JpgModel.class),
                new PathWithBase(args.getRequiredArg(ARGS.OUTPUT_PATH)),
                new JpgPackage(args.getRequiredArg(ARGS.PACKAGE_PREFIX))
        );
    }

    @AllArgsConstructor
    @Getter
    private static enum ARGS implements ArgParserConfigProvider<ARGS> {
        CONFIG_PATH(of(ts("--config-path", "-c")), true, "Full path to config file"),
        OUTPUT_PATH(of(ts("--out-path", "-o")), true, "Full path to output folder"),
        PACKAGE_PREFIX(of(ts("--package", "-p")), true, "Package prefix"),
        ;

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public ARGS[] getArrConfs() {return values();}
    }
}
