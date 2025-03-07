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

package org.salt.function.flow.node.register;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.Info;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.structure.FlowNodeStructure;
import org.salt.function.flow.util.FlowUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
@ConfigurationProperties(prefix = "salt.function.flow.node")
public class FlowNodeManager {

    private Map<String, FlowNode<?,?>> flowNodeMap = new HashMap<>();

    private List<String> resultToConditionType = List.of("Map");

    protected void doRegistration(FlowNode<?,?> flowNode) {
        if (StringUtils.isEmpty(flowNode.getNodeId())) {
            throw new RuntimeException("nodeId or extConfig must not be all null ");
        }
        if (flowNodeMap.containsKey(flowNode.getNodeId())) {
            throw new RuntimeException("loop node " + flowNode.getNodeId());
        }
        flowNodeMap.put(flowNode.getNodeId(), flowNode);
    }

    public FlowNode<?,?> getIFlowNode(String nodeId) {
        return flowNodeMap.get(nodeId);
    }

    //execute
    public <O, I> O execute(String nodeId, Info info) {
        FlowNode<O, I> iFlowNode = (FlowNode<O, I>) flowNodeMap.get(nodeId);
        return execute(iFlowNode, info);
    }

    public <O, I> O execute(FlowNode<O, I> flowNode) {
        return execute(flowNode, null);
    }

    public <O, I> O execute(FlowNode<O, I> flowNode, Info info) {
        if (flowNode != null) {

            ContextBus contextBus = (ContextBus) ContextBus.get();

            String nodeIdOrAlias = flowNode.getNodeId();
            if (info != null) {
                nodeIdOrAlias = info.getIdOrAlias();
            }

            contextBus.setNodeIdOrAlias(nodeIdOrAlias);
            String runId = FlowUtil.id();
            contextBus.putRunId(nodeIdOrAlias, runId);

            I input = ContextBus.get().getPreResult();
            if (info != null && info.getInput() != null) {
                input = (I) info.getInput().apply(input);
            }

            O result = flowNode.process(input);

            if (result != null) {

                if (info != null && info.getOutput() != null) {
                    contextBus.putResult(nodeIdOrAlias, info.getOutput().apply(result));
                } else {
                    contextBus.putResult(nodeIdOrAlias, result);
                }

                contextBus.putPreResult(result);
                contextBus.setFlowResult(result);
                contextBus.setPreRunIds(List.of(runId));

                if (result instanceof Map && resultToConditionType.contains("Map")) {
                    ((Map<String, Object>) result).forEach(contextBus::addCondition);
                } else if (FlowUtil.isPlainObject(result) && resultToConditionType.contains("Object")) {
                    FlowUtil.toMap(result).forEach(contextBus::addCondition);
                } else if (FlowUtil.isPlainObject(result) && resultToConditionType.contains(result.getClass().getSimpleName())) {
                    FlowUtil.toMap(result).forEach(contextBus::addCondition);
                } else if (FlowUtil.isBaseType(result) && resultToConditionType.contains(result.getClass().getSimpleName())) {
                    contextBus.addCondition(nodeIdOrAlias, result);
                }
            }

            if (!(flowNode instanceof FlowNodeStructure)) {
                ((ContextBus) ContextBus.get()).roolbackExec(flowNode);
            }

            return result;
        }
        return null;
    }
}
