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
import org.salt.function.flow.node.IFlowNode;
import org.salt.function.flow.node.structure.FlowNodeStructure;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class FlowNodeManager {

    private Map<String, IFlowNode> flowNodeMap = new HashMap<>();

    protected void doRegistration(IFlowNode iFlowNode, String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            throw new RuntimeException("nodeId or extConfig must not be all null ");
        }
        if (flowNodeMap.containsKey(nodeId)) {
            throw new RuntimeException("loop node " + nodeId);
        }
        flowNodeMap.put(nodeId, iFlowNode);
    }

    public IFlowNode getIFlowNode(String nodeId) {
        return flowNodeMap.get(nodeId);
    }

    public <O, I> O execute(String nodeId, Info info) {
        IFlowNode iFlowNode = flowNodeMap.get(nodeId);
        return execute((FlowNode<O, I>) iFlowNode, info);
    }

    public <O, I> O execute(FlowNode<O, I> flowNode) {
        return execute(flowNode, null);
    }

    public <O, I> O execute(FlowNode<O, I> flowNode, Info info) {
        if (flowNode != null) {

            ContextBus contextBus = (ContextBus) ContextBus.get();

            I input = ContextBus.get().getPreResult();
            if (info != null && info.getInput() != null) {
                input = (I) info.getInput().apply(ContextBus.get());
            }

            O result = flowNode.doProcess(input);

            if (result != null) {

                String idTmp = flowNode.getNodeId();

                if (info != null && StringUtils.isNotEmpty(info.getIdAlias())) {
                    idTmp = info.getIdAlias();
                }

                if (info != null && info.getOutput() != null) {
                    contextBus.putPassResult(idTmp, info.getOutput().apply(contextBus, result));
                } else {
                    contextBus.putPassResult(idTmp, result);
                }

                contextBus.putPreResult(result);
                contextBus.setResult(result);
            }

            if (!(flowNode instanceof FlowNodeStructure)) {
                ((ContextBus) ContextBus.get()).roolbackExec(flowNode);
            }

            return result;
        }
        return null;
    }
}
