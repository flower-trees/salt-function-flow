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

package org.salt.function.flow.thread;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.salt.function.flow.Info;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Data
@Builder
public class TheadHelper {

    private ExecutorService executor;

    private long timeout;

    private static ThreadLocal<Map<String, Object>> threadLocal = new ThreadLocal<>();

    private static List<ThreadLocal<?>> threadLocalUsers = new ArrayList<>();

    public static void initThreadLocal(ThreadLocal<?>... threadLocals) {
        threadLocalUsers = new ArrayList<>();
        threadLocalUsers.addAll(Arrays.asList(threadLocals));
    }

    public static <P> void putThreadLocal(String key, P value) {
        if (threadLocal.get() == null) {
            threadLocal.set(new HashMap<>());
        }
        threadLocal.get().put(key, value);
    }

    public static <P> P getThreadLocal(String key) {
        if (threadLocal.get() == null) {
            threadLocal.set(new HashMap<>());
        }
        return (P) threadLocal.get().get(key);
    }

    public static void clean() {
        threadLocal.set(null);
    }

    public Runnable getDecoratorAsync(Runnable runnable, Info info) {
        final Map<String, Object> map = new HashMap<>(threadLocal.get());
        final List<?> results = threadLocalUsers.stream().map(ThreadLocal::get).collect(Collectors.toList());
        return () -> {
            threadLocal.set(map);
            for (int i = 0; i < threadLocalUsers.size(); i++) {
                @SuppressWarnings("unchecked")
                ThreadLocal<Object> threadLocalUser = (ThreadLocal<Object>) threadLocalUsers.get(i);
                threadLocalUser.set(results.get(i));
            }
            try {
                runnable.run();
            } finally {
                for (ThreadLocal<?> threadLocalUser : threadLocalUsers) {
                    threadLocalUser.remove();
                }
                threadLocal.remove();
            }
        };
    }

    public Callable getDecoratorAsync(Callable callable, Info info) {
        final Map<String, Object> map = new HashMap<>(threadLocal.get());
        final List<?> results = threadLocalUsers.stream().map(ThreadLocal::get).collect(Collectors.toList());
        return () -> {
            threadLocal.set(map);
            for (int i = 0; i < threadLocalUsers.size(); i++) {
                @SuppressWarnings("unchecked")
                ThreadLocal<Object> threadLocalUser = (ThreadLocal<Object>) threadLocalUsers.get(i);
                threadLocalUser.set(results.get(i));
            }
            try {
                return callable.call();
            } finally {
                for (ThreadLocal<?> threadLocalUser : threadLocalUsers) {
                    threadLocalUser.remove();
                }
                threadLocal.remove();
            }
        };
    }
}
