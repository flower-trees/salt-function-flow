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
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.node.IFlowNode;

import java.util.HashMap;
import java.util.Map;

@Data
@Slf4j
public class FlowNodeManager {

    private Map<String, IFlowNode> flowNodeMap = new HashMap<>();

    public void doRegistration(IFlowNode iFlowNode) {
        String nodeId = iFlowNode.nodeId();
        if (StringUtils.isEmpty(nodeId)) {
            throw new RuntimeException("nodeId or extConfig must not be all null ");
        }
        if (flowNodeMap.containsKey(nodeId)) {
            throw new RuntimeException("repeat node " + iFlowNode.nodeId());
        }
        flowNodeMap.put(nodeId, iFlowNode);
    }

    public IFlowNode getIFlowNode(String nodeId) {
        return flowNodeMap.get(nodeId);
    }

    public <R> R execute(String nodeId) {
        IFlowNode iFlowNode = flowNodeMap.get(nodeId);
        if (iFlowNode != null) {
            iFlowNode.process();
            R result = ContextBus.get().getPreResult();
            ((ContextBus) ContextBus.get()).roolbackExec(iFlowNode);
            return result;
        }
        return null;
    }

    public void executeVoid(String nodeId) {
        IFlowNode iFlowNode = flowNodeMap.get(nodeId);
        if (iFlowNode != null) {
            iFlowNode.process();
            ((ContextBus) ContextBus.get()).roolbackExec(iFlowNode);
        }
    }

    public void executeVoidSingle(String nodeId) {
        IFlowNode iFlowNode = flowNodeMap.get(nodeId);
        if (iFlowNode != null) {
            iFlowNode.process();
        }
    }
}
