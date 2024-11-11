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
import org.salt.function.flow.context.ContextBus;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class FlowInstance {
    protected String flowId;
    private List<String> nodeList;
    private FlowEngine flowEngine;

    protected FlowInstance() {
    }

    protected FlowInstance(String flowId, List<String> nodeList, FlowEngine flowEngine) {
        this.flowId = flowId;
        this.nodeList = nodeList;
        this.flowEngine = flowEngine;
    }

    protected <T, R> R execute(T param, Map<String, Object> transmitMap, Map<String, Object> conditionMap) {
        ContextBus contextBus = ContextBus.create(param, conditionMap);
        if (transmitMap != null && !transmitMap.isEmpty()) {
            transmitMap.forEach(contextBus::putTransmitInfo);
        }
        return execute();
    }

    protected <R> R execute() {
        if (!CollectionUtils.isEmpty(nodeList)) {
            ContextBus contextBus = (ContextBus) ContextBus.get();
            for (String nodeId : nodeList) {
                flowEngine.flowNodeManager.executeVoidSingle(nodeId);
                if (contextBus.isRollbackProcess()) {
                    contextBus.roolbackAll();
                    contextBus.setResult(null);
                    break;
                }
                if (contextBus.isStopProcess()) {
                    contextBus.setResult(null);
                    break;
                }
            }
            return (R) contextBus.getResult();
        }
        throw new RuntimeException("processInstance node list is empty.");
    }
}
