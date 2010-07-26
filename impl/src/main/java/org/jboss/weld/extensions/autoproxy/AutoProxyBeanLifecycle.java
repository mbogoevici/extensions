/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.extensions.autoproxy;

import java.lang.reflect.Method;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.extensions.bean.BeanLifecycle;

public class AutoProxyBeanLifecycle<T, H> implements BeanLifecycle<T>
{
   private final ProxyFactory factory;
   private final Class<? extends T> proxyClass;
   private final AutoProxyHandler<H> handler;

   public AutoProxyBeanLifecycle(Class<? extends T> classToImplement, Class<H> handlerClass, BeanManager manager)
   {
      handler = new AutoProxyHandler<H>(handlerClass, manager);

      // create the proxy
      factory = new ProxyFactory();
      if (classToImplement.isInterface())
      {
         Class[] interfaces = new Class[1];
         interfaces[0] = classToImplement;
         factory.setInterfaces(interfaces);
      }
      else
      {
         factory.setSuperclass(classToImplement);
      }
      factory.setFilter(new MethodFilter()
      {
         public boolean isHandled(Method m)
         {
            // ignore finalize()
            return !m.getName().equals("finalize");
         }
      });
      proxyClass = factory.createClass();
   }

   public T create(Bean<T> bean, CreationalContext<T> creationalContext)
   {
      try
      {
         H handlerInstance = handler.create((CreationalContext) creationalContext);
         AutoProxyMethodHandler methodHandler = new AutoProxyMethodHandler(handler, handlerInstance);
         T instance = proxyClass.newInstance();
         ((ProxyObject) instance).setHandler(methodHandler);
         return instance;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(Bean<T> bean, T instance, CreationalContext<T> creationalContext)
   {
      // handler.dispose(instance, (CreationalContext) creationalContext);
   }

}
