/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.spring.security5;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseNewSecurityMatchers extends Recipe {

    public static final String HTTP_SECURITY_CLASS = "org.springframework.security.config.annotation.web.builders.HttpSecurity";
    private static final MethodMatcher HTTP_SECURITY_MATCHER = new MethodMatcher(HTTP_SECURITY_CLASS + " *Matcher(String)");


    @Override
    public String getDisplayName() {
        return "Use the new `securityMatcher()` method";
    }

    @Override
    public String getDescription() {
        return "In Spring Security 5.8, the `HttpSecurity#antMatcher()`, `HttpSecurity#mvcMatcher()`, and `HttpSecurity#regexMatcher()` methods were deprecated " +
                "in favor of new `HttpSecurity#securityMatcher()` method. Refer to the [Spring Security docs](https://docs.spring.io/spring-security/reference/5.8/migration/servlet/config.html#use-new-security-matchers) " +
                "for more information.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>(HTTP_SECURITY_CLASS, true);
    }

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (HTTP_SECURITY_MATCHER.matches(mi) && mi.getMethodType() != null) {
                    return mi.withTemplate(securityMatcherTemplate(ctx), mi.getCoordinates().replaceMethod(), mi.getArguments().get(0));
                }
                return mi;
            }

            private JavaTemplate securityMatcherTemplate(ExecutionContext ctx) {
                return JavaTemplate.builder(this::getCursor, "securityMatcher(#{any(String)})")
                        .javaParser(JavaParser.fromJavaVersion()
                                .classpathFromResources(ctx, "spring-security-web-5.8.+", "spring-security-config-5.8.+"))
                        .build();
            }
        };
    }
}
