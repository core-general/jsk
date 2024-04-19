package jsk.gcl.deployer;

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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import sk.services.CoreServicesRaw;
import sk.services.ICoreServices;
import sk.services.async.IAsync;
import sk.services.http.IHttp;
import sk.services.http.model.CoreHttpResponse;
import sk.services.time.ITime;
import sk.utils.functional.F0;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.javafixes.argparser.ArgParser;
import sk.utils.javafixes.argparser.ArgParserConfigProvider;
import sk.utils.statics.*;
import sk.utils.tuples.X;
import sk.utils.tuples.X2;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.time.Duration.between;
import static java.time.Duration.ofSeconds;
import static sk.utils.functional.O.of;
import static sk.utils.statics.Cc.ts;

public class GcdDeployCheckerMain {
    static IHttp http;
    static IAsync async;
    static ITime times;
    static ArgParser<ARGS> prop;

    public static void main(String[] ___) {
        java.security.Security.setProperty("networkaddress.cache.ttl", "0");
        java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");
        prop = ArgParser.parse(___, ARGS.URLS);
        ICoreServices core = new CoreServicesRaw();
        http = core.http();
        async = core.async();
        times = core.times();

        List<String> urls =
                Arrays.stream(prop.getRequiredArg(ARGS.URLS).replace(",", ";").split(";")).map(String::strip).toList();
        String nverHeader = prop.getRequiredArg(ARGS.VER_HEADER);
        O<String> oldNver = getOldNver(urls, nverHeader);

        final List<OneOf<ResultVersionInfo, Throwable>> results = async.supplyParallel(urls.stream()
                .map($ -> (F0<OneOf<ResultVersionInfo, Throwable>>) () -> waitUntilServerIsUpdated($,
                        oldNver, nverHeader, Ma.pb(prop.getRequiredArg(ARGS.RESET_TO_ZERO_IF_OLD_VERSION_MET))
                ))
                .toList());

        findErrorsOrOk(urls, results)
                .apply(ok -> System.out.println(ok), exc -> Ex.thRow(exc));
    }

    private static O<String> getOldNver(List<String> urls, String nverHeader) {
        final List<OneOf<CoreHttpResponse, Exception>> oldNverResults = async.supplyParallel(urls.stream()
                .map(u -> (F0<OneOf<CoreHttpResponse, Exception>>) () -> http.get(u).tryCount(3).timeout(of(ofSeconds(3)))
                        .goResponse()).toList());

        final List<X2<String, Long>> nVers =
                oldNverResults.stream().filter($ -> $.isLeft() && Ma.inside($.left().code(), 200, 299))
                        .map($ -> $.left())
                        .map($ -> $.getHeader(nverHeader))
                        .filter($ -> $.isPresent())
                        .map($ -> X.x($.get(), Ma.pl(St.subLL($.get(), "-"))/*build milli time*/))
                        .sorted(Comparator.comparing($ -> $.i2))
                        .toList();

        O<String> oldestNver = Cc.first(nVers).map($ -> $.i1());

        System.out.println("All nvers: " + Cc.join(",", nVers, xx -> xx.i1()));
        System.out.println("Oldest nver: " + oldestNver);
        return oldestNver;
    }

    private static OneOf<String, String> findErrorsOrOk(List<String> startingUrls,
            List<OneOf<ResultVersionInfo, Throwable>> results) {
        String errors = "";

        var notEqualNVer = results.stream().filter($ -> $.isLeft())
                .map($ -> $.left())
                .collect(Collectors.groupingBy($ -> $.newNver()));
        if (notEqualNVer.size() != 1) {
            errors += "\n\nNVers different %d:\n".formatted(notEqualNVer.size()) +
                      Cc.joinMap("\n", "\n", notEqualNVer, (k, v) -> k, (k, v) -> "   " + Cc.join("\n   ", v));
        }

        final List<String> urlsWithNoResponse = startingUrls.stream()
                .filter($ -> results.stream().filter(OneOf::isLeft).noneMatch($$ -> Fu.equal($$.left().url, $)))
                .collect(Collectors.toList());
        if (urlsWithNoResponse.size() > 0) {
            errors += "\n\nUrls with no processing: " + Cc.join("; ", urlsWithNoResponse);
        }

        var badRequests = results.stream().filter($ -> $.isRight())
                .map($ -> Ex.getInfo($.right()))
                .sorted()
                .collect(Cc.toL());
        if (badRequests.size() > 0) {
            errors += "\n\nBad requests %d:\n".formatted(badRequests.size()) + Cc.join("\n\n", badRequests);
        }

        return errors.trim().length() > 0
               ? OneOf.right(errors)
               : OneOf.left("OK\n" + results.stream().map($ -> $.left().toString()).sorted().collect(
                       Collectors.joining("\n")));
    }

    private static OneOf<ResultVersionInfo, Throwable> waitUntilServerIsUpdated(String url, O<String> onVerOld,
            String nverHeader, boolean resetToZeroIfOldVersionMet) {
        try {
            final int successRetryConf = Ma.pi(prop.getRequiredArg(ARGS.NUMBER_OF_OK_RESPONSES));

            LocalDateTime start = times.nowLDT();

            int okVersionCount = 0;


            final String nverOld = onVerOld.orElse("");
            String nVerNew = nverOld;
            while (between(start, times.nowLDT()).toSeconds() <= Ma.pi(prop.getRequiredArg(ARGS.TIME_LIMIT_S))) {
                if (!(okVersionCount < successRetryConf)) {break;}
                nVerNew = getNVer(url, nverHeader).orElse(nverOld);
                okVersionCount = Fu.notEqual(nverOld, nVerNew)
                                 ? okVersionCount + 1
                                 : resetToZeroIfOldVersionMet
                                   ? 0
                                   : okVersionCount;

                Ti.sleep(Ti.second * Ma.pi(prop.getRequiredArg(ARGS.RETRY_PERIOD_S)));
            }
            if (okVersionCount < successRetryConf) {
                throw new RuntimeException("Nver retry failed for nverold:" + nVerNew);
            }
            return OneOf.left(new ResultVersionInfo(url, nverOld, nVerNew));

        } catch (Throwable e) {
            return OneOf.right(e);
        }
    }


    private static Map<String, Integer> urlCounter = new ConcurrentHashMap<>();

    @SneakyThrows
    private static O<String> getNVer(String url, String nverHeader) {
        try {
            int retries = 5;
            CoreHttpResponse left = null;
            while (retries-- > 0) {
                left = http.get(url).tryCount(retries).trySleepMs(1000).timeout(of(ofSeconds(4))).goResponse().left();
                if (Ma.inside(left.code(), 200, 299)) {
                    break;
                }
            }
            return left.getHeader(nverHeader);
        } catch (Exception e) {
            return O.empty();
        }
    }

    private record ResultVersionInfo(String url, String oldNver, String newNver) {
        @Override
        public String toString() {
            return "url='%s', oldNver='%s', newNver='%s'".formatted(url, oldNver, newNver);
        }
    }

    @AllArgsConstructor
    @Getter
    private enum ARGS implements ArgParserConfigProvider<ARGS> {
        VER_HEADER(of(ts("-hdr")), true, "Header to get data from"),
        RESET_TO_ZERO_IF_OLD_VERSION_MET(of(ts("-reset")), true, "If true - we reset to 0 if old nver is found"),
        URLS(of(ts("-u")), true, "Urls to check. Use , or ; to separate urls to check"),
        TIME_LIMIT_S(of(ts("-tl")), true, "Time limit to check for new nver in seconds"),
        NUMBER_OF_OK_RESPONSES(of(ts("-ok")), true, "Number of new version responses to assume everything is fine"),
        RETRY_PERIOD_S(of(ts("-rp")), true, "Number of seconds between retries");

        final O<TreeSet<String>> commandPrefix;
        final boolean required;
        final String description;

        @Override
        public ARGS[] getArrConfs() {return values();}
    }
}
