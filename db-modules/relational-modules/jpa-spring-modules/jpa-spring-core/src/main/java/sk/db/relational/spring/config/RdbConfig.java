package sk.db.relational.spring.config;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import sk.db.relational.spring.proprties.RdbProperties;
import sk.db.relational.spring.services.RdbIterator;
import sk.db.relational.spring.services.impl.RdbTransactionWrapperImpl;
import sk.db.relational.utils.RdbIntegratorProvider4Context;
import sk.spring.services.CoreServices;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Properties;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringFacetCodeInspection"})
@Configuration
public class RdbConfig {
    public static final String _CTX = "__JSK_INJECTOR__";

    @Bean
    public DataSource mpDataSource(RdbProperties conf) {
        ComboPooledDataSource cpds = new ComboPooledDataSource();
        try {
            cpds.setDriverClass(conf.getDriver());
        } catch (java.beans.PropertyVetoException e) {
            System.err.println("Cannot find driver:" + conf.getDriver());
            System.exit(1);
        }
        cpds.setJdbcUrl(conf.getUrl());
        cpds.setUser(conf.getUser());
        cpds.setPassword(conf.getPass());
        cpds.setMaxPoolSize(conf.getMaxPoolSize());

        Properties properties = cpds.getProperties();
        properties.put("stringtype", "unspecified");
        cpds.setProperties(properties);

        return cpds;
        //HikariConfig cpds = new HikariConfig();
        //
        //cpds.setDriverClassName(conf.getDriver());
        //
        //cpds.setJdbcUrl(conf.getUrl());
        //cpds.setUsername(conf.getUser());
        //cpds.setPassword(conf.getPass());
        //cpds.setMaximumPoolSize(conf.getMaxPoolSize());
        //
        //Properties properties = cpds.getDataSourceProperties();
        //properties.put("stringtype", "unspecified");
        //cpds.setDataSourceProperties(properties);
        //
        //return new HikariDataSource(cpds);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            RdbProperties conf, DataSource source, CoreServices serviceProvider) {
        Properties p = new Properties();
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setDataSource(source);
        if (conf.getJpaPackages() != null) {
            factory.setPackagesToScan(conf.getJpaPackages());
        } else {
            factory.setPackagesToScan("nope");
        }

        p.put("hibernate.dialect", conf.getDialect());
        p.put("hibernate.show_sql", conf.getShowInfo());
        p.put("hibernate.generate_statistics", conf.getShowInfo());
        p.put("hibernate.hbm2ddl.auto", "validate");
        p.put("hibernate.enable_lazy_load_no_trans", "true");
        p.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
        p.put("hibernate.integrator_provider", new RdbIntegratorProvider4Context());
        p.put("org.hibernate.flushMode", "COMMIT");

        /* Context injection for entities and converters */
        p.put(_CTX, serviceProvider);

        factory.setJpaProperties(p);

        return factory;
    }

    @Bean
    public JpaTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean factory) {
        return new JpaTransactionManager(Objects.requireNonNull(factory.getObject()));
    }

    @Bean
    public RdbIterator RelDbIterator() {
        return new RdbIterator();
    }

    @Bean
    public RdbTransactionWrapperImpl RdbTransactionWrapperImpl() {
        return new RdbTransactionWrapperImpl();
    }

    @Bean
    public NamedParameterJdbcTemplate NamedParameterJdbcTemplate(DataSource ds) {
        return new NamedParameterJdbcTemplate(ds);
    }
}
