package sk.utils.semver;

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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import sk.utils.functional.O;
import sk.utils.statics.Cc;
import sk.utils.statics.Ma;
import sk.utils.statics.St;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@Slf4j
public class Semver200Template {
    private final List<Semver200TemplatePart> checkingParts;

    public static O<Semver200Template> template(String template) {
        String[] parts = template.split("\\.");
        try {
            return O.of(new Semver200Template(Cc.stream(parts)
                    .map(new Function<String, Semver200TemplatePart>() {
                        @Override
                        public Semver200TemplatePart apply(String $) {
                            if ("*".equals($)) {
                                return new TemplateAnyValuePart();
                            } else if ($.contains(",")) {
                                int count = St.count($, ",");
                                String[] parts = $.split(",");
                                if (parts.length <= count) {
                                    throw new RuntimeException("Wrong part template:" + $);
                                }
                                return new TemplateSequenceValuePart(
                                        Cc.stream(parts).map($$ -> getInner($$)).collect(Collectors.toList()));
                            } else {
                                return getInner($);
                            }
                        }


                        private TemplateInnerValuePart getInner(String $) {
                            if ($.contains("-")) {
                                String[] lr = $.split("-");
                                if (lr.length != 2) {
                                    throw new RuntimeException("wrong range:" + $);
                                }
                                return new TemplateRangeValuePart(Ma.pi(lr[0].trim()), Ma.pi(lr[1].trim()));
                            } else {
                                return new TemplateFixedPart(Ma.pi($));
                            }
                        }
                    }).collect(Cc.toL())));
        } catch (Exception e) {
            log.error("Wrong template:" + template, e);
            return O.empty();
        }
    }

    public O<List<TemplateCheckError>> check(String version) {
        return Semver200.parse(version).map(v -> {
            if (v.getMs().getParts().size() != checkingParts.size()) {
                return Cc.l(new TemplateCheckError(TemplateCheckErrorType.WRONG_SIZES));
            }


            List<O<TemplateCheckError>> optionals = Cc.mapSync(v.getMs().getParts(), checkingParts,
                    (var, partCheck, i) -> partCheck.check(var));

            return optionals.stream()
                    .filter(O::isPresent)
                    .map(O::get)
                    .collect(Cc.toL());
        });
    }

    @Data
    public abstract static class Semver200TemplatePart {
        public abstract O<TemplateCheckError> check(Integer var);
    }

    @Data
    public abstract static class TemplateInnerValuePart extends Semver200TemplatePart {
    }

    public static class TemplateAnyValuePart extends Semver200TemplatePart {
        @Override
        public O<TemplateCheckError> check(Integer var) {
            return O.empty();
        }
    }

    @Data
    @AllArgsConstructor
    public static class TemplateSequenceValuePart extends Semver200TemplatePart {
        List<TemplateInnerValuePart> inner;

        @Override
        public O<TemplateCheckError> check(Integer var) {
            return inner.stream().allMatch($ -> $.check(var).isPresent())
                    ? O.of(new TemplateCheckError(TemplateCheckErrorType.PART_ERROR))
                    : O.empty();
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    public static class TemplateFixedPart extends TemplateInnerValuePart {
        final int fix;

        @Override
        public O<TemplateCheckError> check(Integer var) {
            return var != fix
                    ? O.of(new TemplateCheckError(TemplateCheckErrorType.PART_ERROR, fix + "≠" + var))
                    : O.empty();
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    public static class TemplateRangeValuePart extends TemplateInnerValuePart {
        //range is included
        final int left, right;

        @Override
        public O<TemplateCheckError> check(Integer var) {
            return Ma.inside(var, left, right)
                    ? O.empty()
                    : O.of(new TemplateCheckError(TemplateCheckErrorType.PART_ERROR,
                            String.format("%d∉[%d,%d]", var, left, right)));
        }
    }

    @Data
    @AllArgsConstructor
    public static class TemplateCheckError {
        final TemplateCheckErrorType type;
        final String error;

        public TemplateCheckError(TemplateCheckErrorType type) {
            this.type = type;
            error = "";
        }
    }

    public static enum TemplateCheckErrorType {
        WRONG_SIZES,
        PART_ERROR
    }
}
