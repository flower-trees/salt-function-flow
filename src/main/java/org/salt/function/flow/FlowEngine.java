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

package org.salt.function.flow;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.config.IFlowInit;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.register.FlowNodeManager;
import org.salt.function.flow.node.structure.FlowNodeStructure;
import org.salt.function.flow.node.structure.internal.*;
import org.salt.function.flow.thread.TheadHelper;
import org.salt.function.flow.util.FlowUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class FlowEngine implements InitializingBean {

    protected FlowNodeManager flowNodeManager;

    protected IFlowInit flowInit;

    protected ThreadPoolTaskExecutor flowThreadPool;

    private static ConcurrentMap<String, FlowInstance> processInstanceMap = new ConcurrentHashMap<>();

    public FlowEngine(FlowNodeManager flowNodeManager, IFlowInit flowInit, ThreadPoolTaskExecutor flowThreadPool) {
        this.flowNodeManager = flowNodeManager;
        this.flowInit = flowInit;
        this.flowThreadPool = flowThreadPool;
    }

    @Override
    public void afterPropertiesSet() {
        if (flowInit != null) {
            flowInit.configure(this);
        }
    }

    public <T, R> R execute(String flowId, T param) {
        return execute(flowId, param, null);
    }

    public <T, R> R execute(String flowId, T param, Map<String, Object> conditionMap) {
        return execute(flowId, param, conditionMap, null);
    }

    public <T, R> R execute(String flowId, T param, Map<String, Object> conditionMap, Map<String, Object> transmitMap) {
        FlowInstance flowInstance = processInstanceMap.get(flowId);
        if (flowInstance != null) {
            return flowInstance.execute(param, transmitMap, conditionMap);
        }
        throw new RuntimeException("no have this process");
    }

    public <T, R> R execute(FlowInstance flowInstance, T param) {
        return execute(flowInstance, param, null);
    }

    public <T, R> R execute(FlowInstance flowInstance, T param, Map<String, Object> transmitMap) {
        return flowInstance.execute(param, transmitMap, null);
    }

    public <T, R> R execute(FlowInstance flowInstance, T param, Map<String, Object> transmitMap, Map<String, Object> conditionMap) {
        return flowInstance.execute(param, transmitMap, conditionMap);
    }

    public <T, R> R execute(FlowInstance flowInstance, T param, Map<String, Object> transmitMap, Map<String, Object> conditionMap, Consumer<T> beforeRun, Consumer<R> afterRun) {
        return flowInstance.execute(param, transmitMap, conditionMap, beforeRun, afterRun);
    }

    public <R> R execute(String flowId) {
        FlowInstance flowInstance = processInstanceMap.get(flowId);
        if (flowInstance != null) {
            R result = flowInstance.execute();
            if (result != null) {
                ((ContextBus) ContextBus.get()).putResult(flowId, result);
            }
            return result;
        }
        throw new RuntimeException("no have this process");
    }

    public <R> R execute(FlowInstance flowInstance) {
        if (flowInstance != null) {
            R result = flowInstance.execute();
            if (result != null) {
                ((ContextBus) ContextBus.get()).putResult(flowInstance.getFlowId(), result);
            }
            return result;
        }
        throw new RuntimeException("no have this process");
    }

    public Builder builder() {
        return new Builder(this).id(FlowUtil.id());
    }

    public static class Builder {
        String flowId;
        List<FlowNode<?,?>> nodeList;
        private final FlowEngine flowEngine;

        public Builder(FlowEngine flowEngine) {
            this.flowEngine = flowEngine;
            this.nodeList = new ArrayList<>();
        }

        public Builder id(String flowId) {
            this.flowId = flowId;
            return this;
        }

        //next
        public Builder next(Object... node) {
            return next(InitParam.builder().infos(toInfos(node)).build());
        }

        @SafeVarargs
        public final Builder next(Function<Object, ?>... funNodes) {
            return next(InitParam.builder().infos(toInfos(funNodes)).build());
        }

        private Builder next(InitParam initParam) {
            init(tempName("next", initParam.idTmp), new FlowNodeNext(), initParam);
            return this;
        }

        //all
        public Builder all(Object... node) {
            return all(InitParam.builder().infos(toInfos(node)).build());
        }

        @SafeVarargs
        public final Builder all(Function<Object, ?>... funNodes) {
            return all(InitParam.builder().infos(toInfos(funNodes)).build());
        }

        private Builder all(InitParam initParam) {
            init(tempName("all", initParam.idTmp), new FlowNodeAll(), initParam);
            return this;
        }

        //concurrent
        public Builder concurrent(Object... node) {
            List<Object> infos = new ArrayList<>();
            long timeout = InitParam.MAP_WAIT_TIMEOUT;
            ExecutorService executor = null;
            for (Object o : node) {
                if (o instanceof Long) {
                    if ((long) o > 0) {
                        timeout = (Long) o;
                    }
                } else if (o instanceof Integer) {
                    if ((int) o > 0) {
                        timeout = (long) (int) o;
                    }
                } else if (o instanceof Executor) {
                    executor = (ExecutorService) o;
                } else {
                    infos.add(o);
                }
            }
            return concurrent(InitParam.builder().infos(toInfos(infos.toArray())).isolate(executor).timeout(timeout).build());
        }

        @SafeVarargs
        public final Builder concurrent(Function<Object, ?>... funNodes) {
            return concurrent(InitParam.builder().infos(toInfos(funNodes)).build());
        }

        @SafeVarargs
        public final Builder concurrent(long timeout, Function<Object, ?>... funNodes) {
            return concurrent(InitParam.builder().infos(toInfos(funNodes)).timeout(timeout).build());
        }

        @SafeVarargs
        public final Builder concurrent(ExecutorService executor, long timeout, Function<Object, ?>... funNodes) {
            return concurrent(InitParam.builder().infos(toInfos(funNodes)).timeout(timeout).isolate(executor).build());
        }

        private Builder concurrent(InitParam initParam) {
            init(tempName("concurrent", initParam.idTmp), new FlowNodeConcurrent(), initParam);
            return this;
        }

        //notify
        public Builder notify(Object... node) {
            List<Object> infos = new ArrayList<>();
            ExecutorService executor = null;
            for (Object o : node) {
                if (o instanceof Executor) {
                    executor = (ExecutorService) o;
                } else {
                    infos.add(o);
                }
            }
            return notify(InitParam.builder().infos(toInfos(infos.toArray())).isolate(executor).build());
        }

        @SafeVarargs
        public final Builder notify(Function<Object, ?>... funNodes) {
            return notify(InitParam.builder().infos(toInfos(toInfos(funNodes))).build());
        }

        @SafeVarargs
        public final Builder notify(ExecutorService executor, Function<Object, ?>... funNodes) {
            return notify(InitParam.builder().infos(toInfos(toInfos(funNodes))).isolate(executor).build());
        }

        private Builder notify(InitParam initParam) {
            init(tempName("notify", initParam.idTmp), new FlowNodeNotify(), initParam);
            return this;
        }

        //future
        public Builder future(Object... node) {
            List<Object> infos = new ArrayList<>();
            ExecutorService executor = null;
            for (Object o : node) {
                if (o instanceof Executor) {
                    executor = (ExecutorService) o;
                } else {
                    infos.add(o);
                }
            }
            return future(InitParam.builder().infos(toInfos(infos.toArray())).isolate(executor).build());
        }

        private Builder future(InitParam initParam) {
            init(tempName("future", initParam.idTmp), new FlowNodeFuture(), initParam);
            return this;
        }

        //wait
        public Builder wait(Object... node) {
            List<Object> infos = new ArrayList<>();
            long timeout = InitParam.MAP_WAIT_TIMEOUT;
            for (Object o : node) {
                if (o instanceof Long) {
                    if ((long) o > 0) {
                        timeout = (Long) o;
                    }
                } else if (o instanceof Integer) {
                    if ((int) o > 0) {
                        timeout = (long) (int) o;
                    }
                } else {
                    infos.add(o);
                }
            }
            return wait(InitParam.builder().infos(toInfos(infos.toArray())).timeout(timeout).build());
        }

        private Builder wait(InitParam initParam) {
            init(tempName("wait", initParam.idTmp), new FlowNodeWait(), initParam);
            return this;
        }

        //loop
        public Builder loop(Function<Integer, Boolean> condition, Object... node) {
            return loop(condition, InitParam.builder().infos(toInfos(node)).build());
        }

        @SafeVarargs
        public final Builder loop(Function<Integer, Boolean> condition, Function<Object, ?>... funNodes) {
            return loop(condition, InitParam.builder().infos(toInfos(funNodes)).build());
        }

        private Builder loop(Function<Integer, Boolean> condition, InitParam initParam) {
            init(tempName("all", initParam.idTmp), new FlowNodeLoop(condition), initParam);
            return this;
        }

        public String register() {
            check();
            if (processInstanceMap.containsKey(flowId)) {
                throw new RuntimeException("flow already exists. flowId:" + flowId);
            }
            processInstanceMap.put(flowId, new FlowInstance(flowId, nodeList, flowEngine.flowNodeManager));
            return flowId;
        }

        public FlowInstance build() {
            check();
            return new FlowInstance(flowId, nodeList, flowEngine.flowNodeManager);
        }

        private void check() {
            if (StringUtils.isEmpty(flowId)) {
                throw new RuntimeException("flow flowId is empty.");
            }
            if (CollectionUtils.isEmpty(nodeList)) {
                throw new RuntimeException("flow node list is empty.");
            }
        }

        private String tempName(String type, String idTmp) {
            if (StringUtils.isNotEmpty(idTmp)) {
                return idTmp;
            } else {
                return StringUtils.join(type, "-", UUID.randomUUID().toString().replaceAll("-", ""));
            }
        }

        @SafeVarargs
        private Info[] toInfos(Function<Object,?>... funNodes) {
            return Arrays.stream(funNodes)
                    .map(funNode -> Info.builder().funNode(funNode).build())
                    .toList()
                    .toArray(new Info[funNodes.length]);
        }

        private Info[] toInfos(Object[] objects) {
            List<Info> infoList = new ArrayList<>();
            for (Object o : objects) {
                if (o instanceof Info) {
                    infoList.add((Info) o);
                } else {
                    Info info = new Info();
                    info.set(o);
                    infoList.add(info);
                }
            }
            return infoList.toArray(new Info[0]);
        }


        private void init(String id, FlowNode<?,?> flowNode, InitParam initParam) {

            flowNode.setNodeId(id);
            if (flowNode instanceof FlowNodeStructure) {
                FlowNodeStructure<Map<String, Object>> flowNodeStructure = (FlowNodeStructure<Map<String, Object>>) flowNode;
                flowNodeStructure.setFlowEngine(flowEngine);
                flowNodeStructure.setFlowNodeManager(flowEngine.flowNodeManager);
                flowNodeStructure.setTheadHelper(creatThreadHelper(initParam));
                if (initParam.infos != null) {
                    flowNodeStructure.setNodeInfoList(Arrays.asList(initParam.infos));
                }
            }
            nodeList.add(flowNode);
        }

        private TheadHelper creatThreadHelper(InitParam initParam) {
            return TheadHelper.builder()
                        .timeout(initParam.timeout)
                        .executor(
                                initParam.isolate != null ?
                                        initParam.isolate :
                                        flowEngine.flowThreadPool.getThreadPoolExecutor()
                        ).build();
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class InitParam {
        private static long MAP_WAIT_TIMEOUT = 3000;
        String id;
        @lombok.Builder.Default
        String idTmp = "";
        @lombok.Builder.Default
        long timeout = MAP_WAIT_TIMEOUT;
        ExecutorService isolate;
        Info[] infos;
    }
}
