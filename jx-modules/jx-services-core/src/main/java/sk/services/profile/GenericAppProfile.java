package sk.services.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenericAppProfile implements IAppProfileType {
    /** local run */
    LOC(true, false),
    /** functional tests */
    FUT(true, false),
    /** dev */
    DEV(false, false),
    /** test */
    TST(false, false),
    /** stage */
    STG(false, false),
    /** prod */
    PRD(false, true);

    public static final String LOCAL = "loc";

    final boolean forDefaultTesting;
    final boolean forProductionUsage;
}
