/**
 * Copyright (C) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.ui.rebind;

import static org.jboss.errai.codegen.util.Stmt.invokeStatic;
import static org.jboss.errai.codegen.util.Stmt.loadLiteral;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.Dependent;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.ioc.client.api.IOCExtension;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.AbstractBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.FactoryBodyGenerator;
import org.jboss.errai.ioc.rebind.ioc.bootstrapper.IOCProcessingContext;
import org.jboss.errai.ioc.rebind.ioc.extension.IOCExtensionConfigurator;
import org.jboss.errai.ioc.rebind.ioc.graph.api.CustomFactoryInjectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraph;
import org.jboss.errai.ioc.rebind.ioc.graph.api.DependencyGraphBuilder.InjectableType;
import org.jboss.errai.ioc.rebind.ioc.graph.api.Injectable;
import org.jboss.errai.ioc.rebind.ioc.graph.api.InjectionSite;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.DefaultCustomFactoryInjectable;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.FactoryNameGenerator;
import org.jboss.errai.ioc.rebind.ioc.graph.impl.InjectableHandle;
import org.jboss.errai.ioc.rebind.ioc.injector.api.ExtensionTypeCallback;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectableProvider;
import org.jboss.errai.ioc.rebind.ioc.injector.api.InjectionContext;
import org.jboss.errai.ioc.rebind.ioc.injector.api.WiringElementType;
import org.jboss.errai.ui.shared.TemplateUtil;
import org.jboss.errai.ui.shared.api.annotations.Element;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.TagName;

import jsinterop.annotations.JsType;

/**
 * Satisfies injection points for DOM elements.
 *
 * @author Max Barkley <mbarkley@redhat.com>
 */
@IOCExtension
public class ElementProviderExtension implements IOCExtensionConfigurator {

  @Override
  public void configure(final IOCProcessingContext context, final InjectionContext injectionContext) {
  }

  @Override
  public void afterInitialization(final IOCProcessingContext context, final InjectionContext injectionContext) {
    final MetaClass gwtElement = MetaClassFactory.get(com.google.gwt.dom.client.Element.class);
    injectionContext.registerExtensionTypeCallback(new ExtensionTypeCallback() {

      @Override
      public void callback(final MetaClass type) {
        final Element elementAnno;
        final JsType jsTypeAnno;

        if (type.isAssignableTo(gwtElement)) {
          final TagName gwtTagNameAnno;
          if ((gwtTagNameAnno = type.getAnnotation(TagName.class)) != null && gwtTagNameAnno.value().length == 1) {
            processGwtUserElement(injectionContext, type, gwtTagNameAnno);
          }
        }
        else if ((elementAnno = type.getAnnotation(Element.class)) != null) {
          if ((jsTypeAnno = type.getAnnotation(JsType.class)) == null || !jsTypeAnno.isNative()) {
            throw new RuntimeException(
                    Element.class.getSimpleName() + " is only valid on native " + JsType.class.getSimpleName() + "s.");
          }

          processJsTypeElement(injectionContext, type, elementAnno);
        }
      }
    });
  }

  private static void processJsTypeElement(final InjectionContext injectionContext, final MetaClass type, final Element elementAnno) {
    registerInjectableProvider(injectionContext, type, elementAnno.value());
  }

  private static void processGwtUserElement(final InjectionContext injectionContext, final MetaClass type,
          final TagName anno) {
    final String tagName = anno.value()[0];
    registerInjectableProvider(injectionContext, type, tagName);
  }

  private static void registerInjectableProvider(final InjectionContext injectionContext, final MetaClass type, final String tagName) {
    final InjectableHandle handle = new InjectableHandle(type, injectionContext.getQualifierFactory().forDefault());
    injectionContext.registerExactTypeInjectableProvider(handle, new InjectableProvider() {

      CustomFactoryInjectable injectable;

      @Override
      public CustomFactoryInjectable getInjectable(final InjectionSite injectionSite,
              final FactoryNameGenerator nameGenerator) {
        if (injectable == null) {
          final String factoryName = nameGenerator.generateFor(handle.getType(), handle.getQualifier(),
                  InjectableType.ExtensionProvided);
          final FactoryBodyGenerator generator = new AbstractBodyGenerator() {

            @Override
            protected List<Statement> generateCreateInstanceStatements(final ClassStructureBuilder<?> bodyBlockBuilder,
                    final Injectable injectable, final DependencyGraph graph, final InjectionContext injectionContext) {
              return Collections.singletonList(invokeStatic(TemplateUtil.class, "nativeCast",
                      invokeStatic(Document.class, "get").invoke("createElement", loadLiteral(tagName))).returnValue());
            }
          };
          injectable = new DefaultCustomFactoryInjectable(handle.getType(), handle.getQualifier(), factoryName,
                  Dependent.class, Collections.singletonList(WiringElementType.DependentBean), generator);
        }

        return injectable;
      }
    });
  }

}