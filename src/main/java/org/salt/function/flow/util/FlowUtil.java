/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.salt.function.flow.util;

import com.google.gson.Gson;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.lang.StringUtils;
import org.salt.function.flow.Info;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.context.IContextBus;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.AopProxy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class FlowUtil {

    static ExpressRunner runner = new ExpressRunner();

    public static boolean el(String condition, Map<String, Object> conditionMap) {
        DefaultContext<String, Object> defaultContext = new DefaultContext<>();
        defaultContext.putAll(conditionMap);
        try {
            return (boolean) runner.execute(condition, defaultContext, null, true, false);
        } catch (Exception e) {
            log.warn("el exception. include:{}, conditionMap:{}, exception:", condition, FlowUtil.toJson(conditionMap), e);
        }
        return false;
    }

    public static String getNodeInfoKey(String nodeId) {
        return String.format("node_info_%s", nodeId);
    }

    public static boolean isExe(IContextBus iContextBus, Info info) {
        ContextBus contextBus = ((ContextBus) iContextBus);
        return ((StringUtils.isEmpty(info.getInclude())
                    && info.getMatch() == null)
                || (StringUtils.isNotEmpty(info.getInclude())
                    && FlowUtil.el(info.getInclude(), contextBus.getConditionMap()))
                || (info.getMatch() != null
                    && info.getMatch().apply(contextBus)));
    }

    private static final Gson gson = new Gson();
    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    public static Map<String, Object> toMap(Object o) {
        Map<String, Object> map = new HashMap<>();
        (new BeanMap(o)).forEach((key, value) -> map.put(key.toString(), value));
        map.remove("class");
        return map;
    }

    public static String id() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static Object getTarget(Object proxy) throws Exception {

        if (!org.springframework.aop.support.AopUtils.isAopProxy(proxy)) {
            return proxy;
        }

        if (org.springframework.aop.support.AopUtils.isJdkDynamicProxy(proxy)) {
            return getJdkDynamicProxyTargetObject(proxy);
        } else {
            return getCglibProxyTargetObject(proxy);
        }

    }

    public static Object getCglibProxyTargetObject(Object proxy) {
        try {
            Field h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            h.setAccessible(true);
            Object dynamicAdvisedInterceptor = h.get(proxy);
            Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            return ((AdvisedSupport) advised.get(dynamicAdvisedInterceptor)).getTargetSource().getTarget();
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getJdkDynamicProxyTargetObject(Object proxy) throws Exception {
        Field h = proxy.getClass().getSuperclass().getDeclaredField("h");
        h.setAccessible(true);
        AopProxy aopProxy = (AopProxy) h.get(proxy);
        Field advised = aopProxy.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return ((AdvisedSupport) advised.get(aopProxy)).getTargetSource().getTarget();
    }

    public static boolean isPlainObject(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        return !clazz.equals(Boolean.class) &&
                !clazz.equals(Character.class) &&
                !clazz.equals(Byte.class) &&
                !clazz.equals(Short.class) &&
                !clazz.equals(Integer.class) &&
                !clazz.equals(Long.class) &&
                !clazz.equals(Float.class) &&
                !clazz.equals(Double.class) &&
                !clazz.isArray() &&
                !clazz.equals(String.class);
    }

    public static boolean isBaseType(Object obj) {
        if (obj == null) {
            return false; // 或者根据需求返回 true
        }
        Class<?> clazz = obj.getClass();
        return clazz.equals(Boolean.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Short.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(String.class);
    }
}
