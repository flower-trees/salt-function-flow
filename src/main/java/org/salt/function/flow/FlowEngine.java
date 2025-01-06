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
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.IResult;
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
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
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
        public Builder next(Class<?>... clazzs) {
            return next(toInfos(clazzs));
        }

        public Builder next(String... ids) {
            return next(toInfos(ids));
        }

        public Builder next(FlowInstance... flows) {
            return next(toInfos(flows));
        }

        public Builder next(FlowNode<?,?>... flowNodes) {
            return next(toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder next(Function<Object, ?>... funNodes) {
            return next(toInfos(funNodes));
        }

        public Builder next(Info... infos) {
            return next(InitParam.builder().infos(infos).build());
        }

        public Builder next(IResult<?> result, Info... infos) {
            return next(InitParam.builder().infos(infos).result(result).build());
        }

        private Builder next(InitParam initParam) {
            init(tempName("next", initParam.idTmp), new FlowNodeNext(), initParam);
            return this;
        }

        //all
        public Builder all(Class<?>... clazzs) {
            return all(toInfos(clazzs));
        }

        public Builder all(String... ids) {
            return all(toInfos(ids));
        }

        public Builder all(FlowInstance... flows) {
            return all(toInfos(flows));
        }

        public Builder all(FlowNode<?,?>... flowNodes) {
            return all(toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder all(Function<Object, ?>... funNodes) {
            return all(toInfos(funNodes));
        }

        public Builder all(IResult<?> result, Class<?>... clazzs) {
            return all(result, toInfos(clazzs));
        }

        public Builder all(IResult<?> result, String... ids) {
            return all(result, toInfos(ids));
        }

        public Builder all(IResult<?> result, FlowInstance... flows) {
            return all(result, toInfos(flows));
        }

        public Builder all(IResult<?> result, FlowNode<?,?>... flowNodes) {
            return all(result, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder all(IResult<?> result, Function<Object, ?>... funNodes) {
            return all(toInfos(funNodes));
        }

        public Builder all(Info... infos) {
            return all(InitParam.builder().infos(infos).build());
        }

        public Builder all(IResult<?> result, Info... infos) {
            return all(InitParam.builder().infos(infos).result(result).build());
        }

        private Builder all(InitParam initParam) {
            init(tempName("all", initParam.idTmp), new FlowNodeAll(), initParam);
            return this;
        }

        //concurrent
        public Builder concurrent(Class<?>... clazzs) {
            return concurrent(toInfos(clazzs));
        }

        public Builder concurrent(String... ids) {
            return concurrent(toInfos(ids));
        }

        public Builder concurrent(FlowInstance... flows) {
            return concurrent(toInfos(flows));
        }

        public Builder concurrent(FlowNode<?,?>... flowNodes) {
            return concurrent(toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(Function<Object, ?>... funNodes) {
            return concurrent(toInfos(funNodes));
        }

        public Builder concurrent(long timeout, Class<?>... clazzs) {
            return concurrent(timeout, toInfos(clazzs));
        }

        public Builder concurrent(long timeout, String... ids) {
            return concurrent(timeout, toInfos(ids));
        }

        public Builder concurrent(long timeout, FlowInstance... flows) {
            return concurrent(timeout, toInfos(flows));
        }

        public Builder concurrent(long timeout, FlowNode<?,?>... flowNodes) {
            return concurrent(timeout, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(long timeout, Function<Object, ?>... funNodes) {
            return concurrent(timeout, toInfos(funNodes));
        }

        public Builder concurrent(IResult<?> result, Class<?>... clazzs) {
            return concurrent(result, toInfos(clazzs));
        }

        public Builder concurrent(IResult<?> result, String... ids) {
            return concurrent(result, toInfos(ids));
        }

        public Builder concurrent(IResult<?> result, FlowInstance... flows) {
            return concurrent(result, toInfos(flows));
        }

        public Builder concurrent(IResult<?> result, FlowNode<?,?>... flowNodes) {
            return concurrent(result, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(IResult<?> result, Function<Object, ?>... funNodes) {
            return concurrent(result, toInfos(funNodes));
        }

        public Builder concurrent(IResult<?> result, long timeout, Class<?>... clazzs) {
            return concurrent(result, timeout, toInfos(clazzs));
        }

        public Builder concurrent(IResult<?> result, long timeout, String... ids) {
            return concurrent(result, timeout, toInfos(ids));
        }

        public Builder concurrent(IResult<?> result, long timeout, FlowInstance... flows) {
            return concurrent(result, timeout, toInfos(flows));
        }

        public Builder concurrent(IResult<?> result, long timeout, FlowNode<?,?>... flowNodes) {
            return concurrent(result, timeout, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(IResult<?> result, long timeout, Function<Object, ?>... funNodes) {
            return concurrent(result, timeout, toInfos(funNodes));
        }

        public Builder concurrent(IResult<?> result, ExecutorService isolate, Class<?>... clazzs) {
            return concurrent(result, isolate, toInfos(clazzs));
        }

        public Builder concurrent(IResult<?> result, ExecutorService isolate, String... ids) {
            return concurrent(result, isolate, toInfos(ids));
        }

        public Builder concurrent(IResult<?> result, ExecutorService isolate, FlowInstance... flows) {
            return concurrent(result, isolate, toInfos(flows));
        }

        public Builder concurrent(IResult<?> result, ExecutorService isolate, FlowNode<?,?>... flowNodes) {
            return concurrent(result, isolate, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(IResult<?> result, ExecutorService isolate, Function<Object, ?>... funNodes) {
            return concurrent(result, isolate, toInfos(funNodes));
        }

        public Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, Class<?>... clazzs) {
            return concurrent(result, timeout, isolate, toInfos(clazzs));
        }

        public Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, String... ids) {
            return concurrent(result, timeout, isolate, toInfos(ids));
        }

        public Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, FlowInstance... flows) {
            return concurrent(result, timeout, isolate, toInfos(flows));
        }

        public Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, FlowNode<?,?>... flowNodes) {
            return concurrent(result, timeout, isolate, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, Function<Object, ?>... funNodes) {
            return concurrent(result, timeout, isolate, toInfos(funNodes));
        }

        public Builder concurrent(Info... infos) {
            return concurrent(InitParam.builder().infos(infos).build());
        }

        public Builder concurrent(long timeout, Info... infos) {
            return concurrent(InitParam.builder().infos(infos).timeout(timeout).build());
        }

        public Builder concurrent(IResult<?> result, Info... infos) {
            return concurrent(InitParam.builder().infos(infos).result(result).build());
        }

        public Builder concurrent(IResult<?> result, long timeout, Info... infos) {
            return concurrent(InitParam.builder().infos(infos).result(result).timeout(timeout).build());
        }

        public Builder concurrent(IResult<?> result, ExecutorService isolate, Info... infos) {
            return concurrent(InitParam.builder().infos(infos).result(result).isolate(isolate).build());
        }

        public Builder concurrent(IResult<?> result, long timeout, ExecutorService isolate, Info... infos) {
            return concurrent(InitParam.builder().infos(infos).result(result).timeout(timeout).isolate(isolate).build());
        }

        private Builder concurrent(InitParam initParam) {
            init(tempName("concurrent", initParam.idTmp), new FlowNodeConcurrent<>(), initParam);
            return this;
        }

        //notify
        public Builder notify(Class<?>... clazzs) {
            return notify(toInfos(clazzs));
        }

        public Builder notify(String... ids) {
            return notify(toInfos(ids));
        }

        public Builder notify(FlowInstance... flows) {
            return notify(toInfos(flows));
        }

        public Builder notify(FlowNode<?,?>... flowNodes) {
            return notify(toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder notify(Function<Object, ?>... funNodes) {
            return notify(toInfos(funNodes));
        }

        public Builder notify(ExecutorService isolate, Class<?>... clazzs) {
            return notify(isolate, toInfos(clazzs));
        }

        public Builder notify(ExecutorService isolate, String... ids) {
            return notify(isolate, toInfos(ids));
        }

        public Builder notify(ExecutorService isolate, FlowInstance... flows) {
            return notify(isolate, toInfos(flows));
        }

        public Builder notify(ExecutorService isolate, FlowNode<?,?>... flowNodes) {
            return notify(isolate, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder notify(ExecutorService isolate, Function<Object, ?>... funNodes) {
            return notify(isolate, toInfos(funNodes));
        }

        public Builder notify(Info... infos) {
            return notify(InitParam.builder().infos(infos).build());
        }

        public Builder notify(ExecutorService isolate, Info... infos) {
            return notify(InitParam.builder().infos(infos).isolate(isolate).build());
        }

        private Builder notify(InitParam initParam) {
            init(tempName("notify", initParam.idTmp), new FlowNodeNotify(), initParam);
            return this;
        }

        //future
        public Builder future(Class<?>... clazzs) {
            return future(toInfos(clazzs));
        }

        public Builder future(String... ids) {
            return future(toInfos(ids));
        }

        public Builder future(FlowInstance... flows) {
            return future(toInfos(flows));
        }

        public Builder future(FlowNode<?,?>... flowNodes) {
            return future(toInfos(flowNodes));
        }

        public Builder future(ExecutorService isolate, Class<?>... clazzs) {
            return future(isolate, toInfos(clazzs));
        }

        public Builder future(ExecutorService isolate, String... ids) {
            return future(isolate, toInfos(ids));
        }

        public Builder future(ExecutorService isolate, FlowInstance... flows) {
            return future(isolate, toInfos(flows));
        }

        public Builder future(ExecutorService isolate, FlowNode<?,?>... flowNodes) {
            return future(isolate, toInfos(flowNodes));
        }

        public Builder future(Info... infos) {
            return future(InitParam.builder().infos(infos).build());
        }

        public Builder future(ExecutorService isolate, Info... infos) {
            return future(InitParam.builder().infos(infos).isolate(isolate).build());
        }

        private Builder future(InitParam initParam) {
            init(tempName("future", initParam.idTmp), new FlowNodeFuture(), initParam);
            return this;
        }

        //wait
        public Builder wait(Class<?>... clazzs) {
            return wait(toInfos(clazzs));
        }

        public Builder wait(String... ids) {
            return wait(toInfos(ids));
        }

        public Builder wait(FlowInstance... flows) {
            return wait(toInfos(flows));
        }

        public Builder wait(FlowNode<?,?>... flowNodes) {
            return wait(toInfos(flowNodes));
        }

        public Builder wait(IResult<?> result, Class<?>... clazzs) {
            return wait(result, toInfos(clazzs));
        }

        public Builder wait(IResult<?> result, String... ids) {
            return wait(result, toInfos(ids));
        }

        public Builder wait(IResult<?> result, FlowInstance... flows) {
            return wait(result, toInfos(flows));
        }

        public Builder wait(IResult<?> result, FlowNode<?,?>... flowNodes) {
            return wait(result, toInfos(flowNodes));
        }

        public Builder wait(long timeout, Class<?>... clazzs) {
            return wait(timeout, toInfos(clazzs));
        }

        public Builder wait(long timeout, String... ids) {
            return wait(timeout, toInfos(ids));
        }

        public Builder wait(long timeout, FlowInstance... flows) {
            return wait(timeout, toInfos(flows));
        }

        public Builder wait(long timeout, FlowNode<?,?>... flowNodes) {
            return wait(timeout, toInfos(flowNodes));
        }

        public Builder wait(IResult<?> result, long timeout, Class<?>... clazzs) {
            return wait(result, timeout, toInfos(clazzs));
        }

        public Builder wait(IResult<?> result, long timeout, String... ids) {
            return wait(result, timeout, toInfos(ids));
        }

        public Builder wait(IResult<?> result, long timeout, FlowInstance... flows) {
            return wait(result, timeout, toInfos(flows));
        }

        public Builder wait(IResult<?> result, long timeout, FlowNode<?,?>... flowNodes) {
            return wait(result, timeout, toInfos(flowNodes));
        }

        public Builder wait(Info... infos) {
            return wait(InitParam.builder().infos(infos).build());
        }

        public Builder wait(IResult<?> result, Info... infos) {
            return wait(InitParam.builder().infos(infos).result(result).build());
        }

        public Builder wait(long timeout, Info... infos) {
            return wait(InitParam.builder().infos(infos).timeout(timeout).build());
        }

        public Builder wait(IResult<?> result, long timeout, Info... infos) {
            return wait(InitParam.builder().infos(infos).result(result).timeout(timeout).build());
        }

        private Builder wait(InitParam initParam) {
            init(tempName("wait", initParam.idTmp), new FlowNodeWait<>(), initParam);
            return this;
        }

        //loop
        public Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, Class<?>... clazzs) {
            return loop(loopCondition, toInfos(clazzs));
        }

        public Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, String... ids) {
            return loop(loopCondition, toInfos(ids));
        }

        public Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, FlowInstance... flows) {
            return loop(loopCondition, toInfos(flows));
        }

        public Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, FlowNode<?,?>... flowNodes) {
            return loop(loopCondition, toInfos(flowNodes));
        }

        @SafeVarargs
        public final Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, Function<Object, ?>... funNodes) {
            return loop(loopCondition, toInfos(funNodes));
        }

        public Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, Info... infos) {
            return loop(loopCondition, InitParam.builder().infos(infos).build());
        }

        private Builder loop(BiFunction<IContextBus, Integer, Boolean> loopCondition, InitParam initParam) {
            init(tempName("all", initParam.idTmp), new FlowNodeLoop(loopCondition), initParam);
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

        private Info[] toInfos(String... ids) {
            return Arrays.stream(ids)
                    .map(id -> Info.builder().id(id).build())
                    .toList()
                    .toArray(new Info[ids.length]);
        }

        private Info[] toInfos(Class<?>... clazzs) {
            return Arrays.stream(clazzs)
                    .map(clazz -> Info.builder().node(clazz).build())
                    .toList()
                    .toArray(new Info[clazzs.length]);
        }

        private Info[] toInfos(FlowInstance... flows) {
            return Arrays.stream(flows)
                    .map(flow -> Info.builder().flow(flow).build())
                    .toList()
                    .toArray(new Info[flows.length]);
        }

        private Info[] toInfos(FlowNode<?,?>... flowNodes) {
            return Arrays.stream(flowNodes)
                    .map(flowNode -> Info.builder().flowNode(flowNode).build())
                    .toList()
                    .toArray(new Info[flowNodes.length]);
        }

        @SafeVarargs
        private Info[] toInfos(Function<Object,?>... funNodes) {
            return Arrays.stream(funNodes)
                    .map(funNode -> Info.builder().funNode(funNode).build())
                    .toList()
                    .toArray(new Info[funNodes.length]);
        }

        private void init(String id, FlowNode<?,?> flowNode, InitParam initParam) {

            flowNode.setNodeId(id);
            if (flowNode instanceof FlowNodeStructure) {
                FlowNodeStructure<?> flowNodeStructure = (FlowNodeStructure) flowNode;
                flowNodeStructure.setFlowEngine(flowEngine);
                flowNodeStructure.setFlowNodeManager(flowEngine.flowNodeManager);
                flowNodeStructure.setResult(initParam.result);
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
        IResult result;
        ExecutorService isolate;
        Info[] infos;
    }
}
