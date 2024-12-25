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

package org.salt.function.flow.context;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.thread.TheadHelper;
import org.salt.function.flow.util.FlowUtil;

import java.util.*;
import java.util.concurrent.*;

@Builder
@Slf4j
public class ContextBus implements IContextBus {

    private static String LAST_RESULT_KEY = "thead_last_result_key";
    private static String RESULT_KEY = "thead_result_key";

    /**
     * ContextBus id
     */
    @Getter
    private String id;

    /**
     * Flow call parameters
     */
    private Object param;

    /**
     * Flow call result
     */
    private Object result;

    /**
     * Store additional transmission context information
     */
    private ConcurrentMap<String, Object> transmitMap;

    /**
     * Store the returned results of execution nodes
     */
    private ConcurrentMap<String, Object> nodeResultMap;

    /**
     * Store the exception information of each node (asynchronous execution node)
     */
    private ConcurrentMap<String, Exception> nodeExceptionMap;

    /**
     * Store the parameters involved in condition judgment, initially flow param
     */
    private ConcurrentMap<String, Object> conditionMap;

    /**
     * Flow execution instance ID
     */
    private String runtimeId;

    /**
     * Flow stop flag
     */
    private volatile boolean stopFlag;

    /**
     * Flow rollback flag
     */
    private boolean rollbackFlag;
    /**
     * Executed node list
     */
    private Deque<FlowNode<?,?>> rollbackList;


    public <P> P getFlowParam() {
        return (P) this.param;
    }

    public <P> P getFlowResult() {
        return TheadHelper.getThreadLocal(RESULT_KEY);
    }

    public <R> void setFlowResult(R result) {
        TheadHelper.putThreadLocal(RESULT_KEY, result);
    }

    @Override
    public <P> void putTransmit(String key, P content) {
        transmitMap.put(key, content);
    }

    @Override
    public <P> P getTransmit(String key) {
        return (P) transmitMap.get(key);
    }

    @Override
    public <P> void addCondition(String key, P value) {
        if (key == null || value == null) {
            return;
        }
        if (conditionMap.containsKey(key)) {
            log.debug("process addCondition param loop. key:{}, value:{}, traceId:{}", key, value, runtimeId);
        }
        conditionMap.put(key, value);
    }

    public Map<String, Object> getConditionMap() {
        return conditionMap;
    }

    @Override
    public <P> P getResult(String nodeId) {
        return (P) nodeResultMap.get(nodeId);
    }

    @Override
    public <P> P getResult(Class<?> clazz) {
        return (P) nodeResultMap.get(clazz.getName());
    }

    public <P> void putResult(String nodeId, P result) {
        nodeResultMap.put(nodeId, result);
    }

    public void removeResult(String nodeId) {
        nodeResultMap.remove(nodeId);
    }

    public static void clean() {
        TheadHelper.clean();
    }

    public <P> P getResult(String nodeId, long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        Object result = nodeResultMap.get(nodeId);
        if (result != null && result instanceof Future) {
            return ((Future<P>) result).get(timeout, TimeUnit.MILLISECONDS);
        }
        throw new RuntimeException("node is not Future");
    }

    @Override
    public Exception getException(String nodeId) {
        return nodeExceptionMap.get(nodeId);
    }

    @Override
    public Exception getException(Class<?> clazz) {
        return nodeExceptionMap.get(clazz.getName());
    }

    public void putException(String nodeId, Exception e) {
        nodeExceptionMap.put(nodeId, e);
    }

    @Override
    public String getRuntimeId() {
        return runtimeId;
    }

    public <P> void putPreResult(P result) {
        TheadHelper.putThreadLocal(LAST_RESULT_KEY, result);
    }

    @Override
    public <P> P getPreResult() {
        return TheadHelper.getThreadLocal(LAST_RESULT_KEY);
    }

    public void copy() {
        ContextBus contextBus = ContextBus.builder()
                .id("context-bus-" + UUID.randomUUID().toString().replaceAll("-", ""))
                .param(param)
                .conditionMap(new ConcurrentHashMap<>(conditionMap))
                .nodeResultMap(new ConcurrentHashMap<>(nodeResultMap))
                .nodeExceptionMap(new ConcurrentHashMap<>(nodeExceptionMap))
                .transmitMap(new ConcurrentHashMap<>(transmitMap))
                .runtimeId(runtimeId)
                .rollbackList(new LinkedList<>())
                .build();
        TheadHelper.putThreadLocal(IContextBus.class.getName(), contextBus);
    }

    public static ContextBus create(Object param) {
        ConcurrentMap<String, Object> conditionMap = new ConcurrentHashMap<>();
        try {
            if (param instanceof Map) {
                ((Map<String, Object>) param).values().removeIf(Objects::isNull);
                conditionMap = new ConcurrentHashMap<>((Map<String, Object>) param);
            } else if (FlowUtil.isBaseType(param)) {
                conditionMap = new ConcurrentHashMap<>();
                conditionMap.put("param", param);
            } else if (FlowUtil.isPlainObject(param)) {
                Map<String, Object> map = FlowUtil.toMap(param);
                map.values().removeIf(Objects::isNull);
                conditionMap = new ConcurrentHashMap<>(map);
            }
        } catch (Exception e) {
            log.error("param to conditionMap error", e);
            throw new RuntimeException("param to conditionMap error");
        }
        ContextBus contextBus = ContextBus.builder()
                .id("context-bus-" + UUID.randomUUID().toString().replaceAll("-", ""))
                .param(param)
                .conditionMap(conditionMap)
                .nodeResultMap(new ConcurrentHashMap<>())
                .nodeExceptionMap(new ConcurrentHashMap<>())
                .transmitMap(new ConcurrentHashMap<>())
                .runtimeId(UUID.randomUUID().toString().replaceAll("-", ""))
                .rollbackList(new LinkedList<>())
                .build();
        ContextBus.clean();
        contextBus.putPreResult(param);
        TheadHelper.putThreadLocal(IContextBus.class.getName(), contextBus);
        return contextBus;
    }

    public void stopProcess() {
        this.stopFlag = true;
    }
    public boolean isStopProcess() {
        return this.stopFlag;
    }

    public synchronized void rollbackProcess() {
        this.rollbackFlag = true;
    }

    public synchronized boolean isRollbackProcess() {
        return this.rollbackFlag;
    }

    public synchronized void roolbackAll() {
        for(int i=rollbackList.size()-1; i>=0; i--) {
            FlowNode<?,?> execNode = rollbackList.pop();
            try {
                execNode.rollback();
            } catch (Exception e) {
                log.warn("roolbackAll fail", e);
            }
        }
    }

    public synchronized boolean roolbackExec(FlowNode<?,?> flowNode) {
        if (rollbackFlag) {
            try {
                flowNode.rollback();
            } catch (Exception e) {
            }
            return true;
        } else {
            rollbackList.push(flowNode);
            return false;
        }
    }

    public static IContextBus get() {
        return TheadHelper.getThreadLocal(IContextBus.class.getName());
    }
}
