package org.jboss.errai.persistence.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.errai.bus.client.ErraiBus;
import org.jboss.errai.bus.server.ErraiModule;
import org.jboss.errai.bus.server.annotations.ExtensionComponent;
import org.jboss.errai.bus.server.ext.ErraiConfigExtension;
import org.jboss.errai.bus.server.service.ErraiServiceConfigurator;
import org.jboss.errai.bus.server.util.ConfigUtil;
import org.jboss.errai.bus.server.util.ConfigVisitor;
import org.jboss.errai.persistence.server.security.annotations.AuthUserEntity;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.io.File;
import java.util.List;
import java.util.Map;


/**
 * This is a prototype implementation of the persistence service.  It doesn't do anything fancy right now.
 */
@ExtensionComponent
public class ErraiPersistenceConfigurator implements ErraiConfigExtension {
    private ErraiServiceConfigurator config;

    @Inject
    public ErraiPersistenceConfigurator(ErraiServiceConfigurator config) {
        this.config = config;
    }

    public void configure(Map<Class, Provider> bindings, Map<String, Provider> resourceProviders) {
        final AnnotationConfiguration cfg = new AnnotationConfiguration();
        cfg.setProperty("hibernate.connection.driver_class", config.getProperty("errai.prototyping.persistence.connection.driver_class"));
        cfg.setProperty("hibernate.connection.url", config.getProperty("errai.prototyping.persistence.connection.url"));
        cfg.setProperty("hibernate.connection.username", config.getProperty("errai.prototyping.persistence.connection.username"));
        cfg.setProperty("hibernate.connection.password", config.getProperty("errai.prototyping.persistence.connection.password"));
        cfg.setProperty("hibernate.connection.pool_size", config.getProperty("errai.prototyping.persistence.connection.pool_size"));

        cfg.setProperty("hibernate.dialect", config.getProperty("errai.prototyping.persistence.dialect"));
        cfg.setProperty("hibernate.current_session_context_class", "thread");
        cfg.setProperty("hibernate.cache.use_second_level_cache", "false");
        cfg.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider");
        

        cfg.setProperty("hibernate.show_sql", "true");
        cfg.setProperty("hibernate.hbm2ddl.auto", "update");

        List<File> roots = config.getConfigurationRoots();

        ConfigUtil.visitAllTargets(roots, new ConfigVisitor() {
            public void visit(Class<?> clazz) {
                if (clazz.isAnnotationPresent(Entity.class)) {
                    cfg.addAnnotatedClass(clazz);
                }
            }
        });

        final SessionFactory sessionFactory = cfg.buildSessionFactory();

        Provider<Session> sessionProvider = new Provider<Session>() {
            public Session get() {
                return sessionFactory.openSession();
            }
        };
        Provider<SessionFactory> sessionFactoryProvider = new Provider() {
            public Object get() {
                return sessionFactory;
            }
        };

        bindings.put(Session.class, sessionProvider);
        bindings.put(SessionFactory.class, sessionFactoryProvider);

        resourceProviders.put("SessionProvider",  sessionProvider);
    }


}
