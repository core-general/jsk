package sk.services.mapping;

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

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Conditions;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import sk.services.time.ITime;
import sk.utils.functional.C1;
import sk.utils.functional.O;
import sk.utils.statics.Cc;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

@NoArgsConstructor
public class ModelMapperImpl implements IMapper {
    @Inject ITime times;
    @Inject protected Optional<List<ModelMapperConfig>> configs = Optional.empty();

    public ModelMapperImpl(ITime times, Optional<List<ModelMapperConfig>> configs) {
        this.times = times;
        this.configs = configs;
    }

    protected ModelMapper mapper;
    protected ModelMapper mapperDeep;
    protected ModelMapper mapperDoNotCopyNulls;
    protected ModelMapper mapperDoNotCopyNullsDeep;

    @Override
    public <ME> O<ME> clone(ME in, boolean deep) {
        return in == null ? O.empty() : O.ofNullable(getMapper(deep, true).map(in, (Class<ME>) in.getClass()));
    }

    @Override
    public <IN, OUT> O<OUT> map(IN in, Class<OUT> outCls, boolean deep) {
        return in == null ? O.empty() : O.ofNullable(getMapper(deep, true).map(in, outCls));
    }

    @Override
    public <IN, OUT> O<OUT> map(IN in, OUT out, boolean deep, boolean copyNulls) {
        if (in == null) {
            return O.empty();
        }
        getMapper(deep, copyNulls).map(in, out);
        return O.of(out);
    }

    @PostConstruct
    protected ModelMapperImpl initModelMapperImpl() {
        mapper = new ModelMapper();
        mapperDeep = new ModelMapper();
        mapperDoNotCopyNulls = new ModelMapper();
        mapperDoNotCopyNullsDeep = new ModelMapper();

        mapperDeep.getConfiguration().setDeepCopyEnabled(true);
        mapperDoNotCopyNullsDeep.getConfiguration().setDeepCopyEnabled(true);

        mapperDoNotCopyNulls.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        mapperDoNotCopyNullsDeep.getConfiguration().setPropertyCondition(Conditions.isNotNull());

        allMappers(this::defaultConverters);

        Map<Class<?>, Map<Class<?>, Boolean>> propertyMap = Cc.m();
        Map<Class<?>, Map<Class<?>, Boolean>> converterMap = Cc.m();
        configs.stream()
                .flatMap(oMapping -> oMapping.stream())
                .flatMap(mapping -> mapping.getMappings().stream())
                .forEach(pm -> {
                    //if mapping exist - throw exception
                    Cc.compute(propertyMap, pm.getInType(), (a, b) -> {
                        Cc.compute(b, pm.getOutType(), (d, e) -> {
                            throw new RuntimeException(
                                    format("Mapping is already processed: %s, %s", pm.getInType(), pm.getOutType()));
                        }, () -> true);
                        return b;
                    }, () -> Cc.m(pm.getOutType(), true));

                    allMappers(mapper -> mapper.addMappings(pm));
                });

        configs.stream()
                .flatMap(oMapping -> oMapping.stream())
                .flatMap(mapping -> mapping.getConverters().stream())
                .forEach(c -> {
                    //if mapping exist - throw exception
                    Cc.compute(converterMap, c.getSourceType(), (a, b) -> {
                        Cc.compute(b, c.getDestType(), (d, e) -> {
                            throw new RuntimeException(
                                    format("Converter is already processed: %s, %s", c.getSourceType(), c.getDestType()));
                        }, () -> true);
                        return b;
                    }, () -> Cc.m(c.getDestType(), true));

                    allMappers(mapper -> mapper.addConverter(c));
                });

        return this;
    }

    private void allMappers(C1<ModelMapper> toDoWithMapper) {
        toDoWithMapper.accept(mapper);
        toDoWithMapper.accept(mapperDeep);
        toDoWithMapper.accept(mapperDoNotCopyNulls);
        toDoWithMapper.accept(mapperDoNotCopyNullsDeep);
    }

    private void defaultConverters(ModelMapper mapper) {
        mapper.addConverter(new AbstractConverter<Long, ZonedDateTime>() {
            @Override
            protected ZonedDateTime convert(Long source) {
                return O.ofNull(source).map(aLong -> Instant.ofEpochMilli(aLong).atZone(times.getZone())).orElse(null);
            }
        });
        mapper.addConverter(new AbstractConverter<ZonedDateTime, Long>() {
            @Override
            protected Long convert(ZonedDateTime source) {
                return O.ofNull(source).map(zdt -> zdt.toInstant().toEpochMilli()).orElse(null);
            }
        });

        mapper.addConverter(new AbstractConverter<Enum, String>() {
            protected String convert(Enum source) {
                return source.name();
            }
        });
        mapper.addConverter((Converter<String, Enum>) context ->
                Enum.valueOf(context.getDestination().getDeclaringClass(), context.getSource()));

        mapper.addConverter(new AbstractConverter<Long, Instant>() {
            @Override
            protected Instant convert(Long source) {
                return O.ofNull(source).map(Instant::ofEpochMilli).orElse(null);
            }
        });
        mapper.addConverter(new AbstractConverter<Instant, Long>() {
            @Override
            protected Long convert(Instant source) {
                return O.ofNull(source).map(Instant::toEpochMilli).orElse(null);
            }
        });

        mapper.addConverter(new AbstractConverter<ZonedDateTime, Instant>() {
            @Override
            protected Instant convert(ZonedDateTime source) {
                return O.ofNull(source).map(ChronoZonedDateTime::toInstant).orElse(null);
            }
        });
        mapper.addConverter(new AbstractConverter<Instant, ZonedDateTime>() {
            @Override
            protected ZonedDateTime convert(Instant source) {
                return O.ofNull(source).map(inst -> inst.atZone(times.getZone())).orElse(null);
            }
        });

        mapper.addConverter(new AbstractConverter<Optional, Optional>() {
            @Override
            protected Optional convert(Optional source) {
                return source != null ? source : Optional.empty();
            }
        });
        mapper.addConverter(new AbstractConverter<O, O>() {
            @Override
            protected O convert(O source) {
                return source != null ? source : O.empty();
            }
        });
    }

    private ModelMapper getMapper(boolean deep, boolean copyNulls) {
        if (!deep && copyNulls) {
            return mapper;
        } else if (deep && copyNulls) {
            return mapperDeep;
        } else if (!deep) {
            return mapperDoNotCopyNulls;
        } else {
            return mapperDoNotCopyNullsDeep;
        }
    }

    //public static void main(String[] args) {
    //    Y y = new Y(new GG(4), 5, "c", 7);
    //
    //    ModelMapperImpl mi = new ModelMapperImpl(new TimeUtcImpl(), Optional.empty()).initModelMapperImpl();
    //
    //    final X x = mi.map(y, X.class, false).get();
    //    int i = 0;
    //
    //}
    //
    //static class GG {
    //    int II;
    //
    //    public GG() {
    //    }
    //
    //    public GG(int II) {
    //        this.II = II;
    //    }
    //}
    //
    //@Data
    //@NoArgsConstructor
    //public static abstract class A {
    //    GG ctx;
    //
    //    public A(GG ctx) {
    //        this.ctx = ctx;
    //    }
    //}
    //
    //@Data
    //@NoArgsConstructor
    //public static abstract class AA extends A {
    //    int b;
    //    String c;
    //
    //    public AA(GG ctx, int b, String c) {
    //        super(ctx);
    //        this.b = b;
    //        this.c = c;
    //    }
    //}
    //
    //@Data
    //@NoArgsConstructor
    //public static class X extends AA {
    //    public X(GG ctx, int b, String c) {
    //        super(ctx, b, c);
    //    }
    //}
    //
    //@Data
    //@NoArgsConstructor
    //public static class Y extends AA {
    //    int z;
    //
    //    public Y(GG ctx, int b, String c, int z) {
    //        super(ctx, b, c);
    //        this.z = z;
    //    }
    //}
}
