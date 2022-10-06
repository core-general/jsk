package jsk.gcl.migrator;

import sk.db.migrator.MigratorBase;
import sk.db.migrator.MigratorModel;

public class GclMigratorMain {
    public static void main(String[] args) {
        new GclCommonMigrator().migrate(args);
    }

    private static class GclCommonMigrator extends MigratorBase {
        @Override
        protected MigratorModel enrich(MigratorModel model) {
            model.setResourceFolder("classpath:g-cluster-common-migration");
            model.setMigrationTableName("_gcl_migrations");
            model.setFilePrefix("gcl_");
            model.setSchemaName("gcl_");
            return model;
        }
    }
}
