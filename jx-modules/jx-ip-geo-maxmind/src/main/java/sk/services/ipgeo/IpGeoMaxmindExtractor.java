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
import lombok.SneakyThrows;
import sk.services.except.IExcept;
import sk.services.http.IHttp;
import sk.utils.functional.O;
import sk.utils.functional.OneOf;
import sk.utils.statics.Io;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.time.ZoneId;
import java.util.Optional;

public class IpGeoMaxmindExtractor implements IIpGeoExtractor {
    @Inject IExcept except;
    @Inject IHttp http;

    final O<String> url;
    DatabaseReader reader;

    public IpGeoMaxmindExtractor() {
        url = O.empty();
    }

    public IpGeoMaxmindExtractor(String url) {
        this.url = O.of(url);
    }

    @PostConstruct
    @SneakyThrows
    public IpGeoMaxmindExtractor init() {
        /*
        Db search:
        1. If URL is set - from URL
        2. In resources: "jsk/ip2country.mmdb"
        3. Throw exception if neither 1 or 2
         */
        try (var is = url.<InputStream>flatMap(
                u -> http.get(url.get()).goBytes().collect(by -> O.of(new ByteArrayInputStream(by)), ex -> O.empty()))
                .or(() -> Io.getResourceStream("jsk/ip2country.mmdb"))
                .orElseGet(() -> except.throwByCode("CAN'T INIT IPGEO SUBSYSTEM"))) {
            DatabaseReader.Builder b = new DatabaseReader.Builder(is);
            b.withCache(new IpGeoCache());
            b.fileMode(Reader.FileMode.MEMORY);
            reader = b.build();
        }

        return this;
    }

    @Override
    @SneakyThrows
    public O<IpGeoData> ipToGeoData(String ip) {
        try {
            final Optional<CountryResponse> countryResponse = reader.tryCountry(InetAddress.getByName(ip));
            return O.of(countryResponse).map($ -> {
                final CountryType country = CountryType.valueOf($.getCountry().getIsoCode().toUpperCase());
                return new IpGeoData(ip, country, OneOf.left(ZoneId.of(country.getApproxTimeZone())));
            });
        } catch (Exception e) {
            return O.empty();
        }
    }
}
