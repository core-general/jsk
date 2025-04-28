package jsk.progen;

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
