package sk.services.ipgeo;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
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

import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import sk.services.except.IExcept;
import sk.services.http.IHttp;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
public class IpGeoMaxmindExtractor implements IIpGeoExtractor {
    @Inject IExcept except;
    @Inject IHttp http;

    final O<String> mmdbFileUrl;
    DatabaseReader reader;

    public IpGeoMaxmindExtractor() {
        mmdbFileUrl = O.empty();
    }

    public IpGeoMaxmindExtractor(String mmdbFileUrl) {
        this.mmdbFileUrl = O.of(mmdbFileUrl);
    }

    @PostConstruct
    @SneakyThrows
    public IpGeoMaxmindExtractor init() {
        /*
        Db search:
        (probably you can use https://db-ip.com/db/download/ip-to-country-lite, but see license)
        1. If URL is set - from URL
        2. In resources: "jsk/ip2country.mmdb"
        3. Throw exception if neither 1 nor 2
         */
        try (var is = mmdbFileUrl.<InputStream>flatMap(
                u -> http.get(mmdbFileUrl.get()).goBytes().collect(by -> O.of(new ByteArrayInputStream(by)), ex -> O.empty()))
                .or(() -> Io.getResourceStream("jsk/ip2country.mmdb"))
                .orElseGet(() -> except.throwByCode("CAN'T INIT IPGEO SUBSYSTEM"))) {
            DatabaseReader.Builder b = new DatabaseReader.Builder(is);
            b.withCache(new IpGeoCache());
            b.fileMode(Reader.FileMode.MEMORY);
            reader = b.build();
            //check
            if (ipToGeoData("18.133.68.153").flatMap($ -> O.ofNull($.getCountry())).orElse(null) == null) {
                except.throwByCode("IpGeoMaxmindExtractor CANT LOAD");
            }
        }

        return this;
    }

    @Override
    @SneakyThrows
    public O<IpGeoData> ipToGeoData(String ip) {
        ip = ip.trim();
        try {
            final String[] ips = ip.split(",");
            if (ips.length == 1) {
                if (ip.startsWith("192.168")) {
                    return O.empty();
                }
                final Optional<CountryResponse> countryResponse = reader.tryCountry(InetAddress.getByName(ip));
                String finalIp = ip;
                return O.of(countryResponse).map($ -> {
                    final CountryType country = CountryType.valueOf($.getCountry().getIsoCode().toUpperCase());
                    return new IpGeoData(finalIp, country, OneOf.left(ZoneId.of(country.getApproxTimeZone())));
                });
            } else {
                for (String s : ips) {
                    final O<IpGeoData> ipGeoDataO = ipToGeoData(s);
                    if (ipGeoDataO.isPresent()) {
                        return ipGeoDataO;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Problem with ip:" + ip, e);
        }
        return O.empty();
    }

    public static void main(String[] args) {
        IpGeoMaxmindExtractor ipgeo = new IpGeoMaxmindExtractor().init();
        final IpGeoData ipGeoData = ipgeo.ipToGeoData("").get();
        int i = 0;
    }
}
