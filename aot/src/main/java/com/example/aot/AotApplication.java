package com.example.aot;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;

//@Configuration
//class FooConfig {
//
//    @Bean
//    Foo foo() {
//        IO.println("creating bean foo");
//        return num -> IO.println("bean foo: " + num);
//    }
//
//    @Bean
//    ApplicationRunner runner() {
//        return _ -> {
//            var foo = foo();
//            foo.bar(42);
//            for (var i = 0; i < 1000000000; i++)
//                this.foo();
//            IO.println("running");
//        };
//    }
//}

@SpringBootApplication
public class AotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AotApplication.class, args);
    }

   /* private void doInvocationForBar(Method method, Object[] args) {

    }

    //    @PostConstruct
    void init() {
        var foo = (Foo) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{Foo.class}, (proxy, method, args) -> {
                    if (method.getName().equals("bar")) {
                        IO.println("invoking " + method.getName() + " with arguments " +
                                Arrays.toString(args));
                        return null;
                    }
                    if (method.getName().equals("toString")) {
                        return "proxy";
                    }
                    if (method.getName().equals("hashCode")) {
                        return 42;
                    }
                    if (method.getName().equals("equals")) {
                        return false;
                    }
                    return null;
                });
        foo.bar(42);

        var actualFoo = new Foo() {

            @Override
            public void bar(int num) {
                IO.println("target invocatino in concrete class: " + num);
            }
        };

        var pfb = new ProxyFactoryBean();
//        pfb.setProxyTargetClass(true);
        pfb.addAdvice(new MethodInterceptor() {
            @Override
            public @Nullable Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
                IO.println("invoking " + invocation.getMethod().getName() +
                        " with arguments " + Arrays.toString(invocation.getArguments()));
                if (invocation.getMethod().getName().equals("bar")) {
                    IO.println("proxied bar!");
                    return "bar!";
                }
                return invocation.proceed();
            }
        });
        pfb.addInterface(Foo.class);
//        pfb.setTarget(actualFoo);
        var fooConcrete = (Foo) pfb.getObject();
        fooConcrete.bar(43);
    }*/
}

interface Foo {
    void bar(int num);
}
// reflection
// serialization
// jdk proxies
// jni
// resources

@Configuration
class MyConfig {

    @Bean
    static MyBFIAOP myBFIAOP() {
        return new MyBFIAOP();
    }

    static class MyBFIAOP implements BeanFactoryInitializationAotProcessor {

        @Override
        public @Nullable BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {

            var serializable = new HashSet<String>();
            for (var beanName : beanFactory.getBeanDefinitionNames()) {
                var type = beanFactory.getType(beanName);
                Assert.notNull(type, "type: " + type + " is not null");
                if (Serializable.class.isAssignableFrom(type)) {
                    IO.println("s: " + type);
                    serializable.add(type.getName());
                }
            }

            return (generationContext, code ) -> {
                var runtimeHints = generationContext.getRuntimeHints();
                for (var type : serializable) {
                    runtimeHints.reflection().registerType(TypeReference.of(type));
                }

                code.getMethods().add("foo" , s-> s.addStatement("""
                        IO.println("foo");
                        """)) ;

            };

        }
    }

    @Bean
    static MyBFPP myBFPP() {
        return new MyBFPP();
    }
}

@Component
class MyCart implements Serializable {

}

class MyBFPP implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (var beanName : beanFactory.getBeanDefinitionNames()) {
            var bd = beanFactory.getBeanDefinition(beanName);
            var clzz = beanFactory.getType(beanName);
            IO.println(beanName + ":" + (clzz.getName()));
        }
    }
}

@Component
@ImportRuntimeHints(Runner.Hints.class)
class Runner implements ApplicationRunner {

    // 1. ingest (xml, component scanning, java config, BeanRegistrar)
    // 2. BeanDefinitions
    // 2.1 BeanFactoryPostProcessor
    // 3. beans
    // 3.1 afterPropertiesSet


    static class Hints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
            IO.println("registering hints!!");
            hints.reflection().registerType(Customer.class, MemberCategory.values());
            hints.resources().registerResource(MESSSAGE);
        }
    }

    private static final Resource MESSSAGE = new ClassPathResource("/message");

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {

        var txt = MESSSAGE.getContentAsString(Charset.defaultCharset());
        IO.println(txt);

        var customer = new Customer("John", 42);
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(customer);

        IO.println(json);

    }
}

record Customer(String name, int age) {
}

@Controller
@ResponseBody
class HelloController {

    @GetMapping("/")
    Map<String, String> hello() {
        return Map.of("message", "apa kubar?");
    }
}