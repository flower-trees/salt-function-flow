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

package org.salt.function.flow.node;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.Info;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.node.register.NodeIdentity;
import org.salt.function.flow.util.FlowUtil;
import org.springframework.beans.factory.InitializingBean;

@Setter
@Slf4j
public abstract class FlowNode<O, I> implements IFlowNode, InitializingBean {

    protected String nodeId;

    @Override
    public void afterPropertiesSet() {
        NodeIdentity nodeIdentity = this.getClass().getAnnotation(NodeIdentity.class);
        if (nodeIdentity != null) {
            if (StringUtils.isNotEmpty(nodeIdentity.value())) {
                nodeId = nodeIdentity.value();
            } else if(StringUtils.isNotEmpty(nodeIdentity.nodeId())) {
                nodeId = nodeIdentity.nodeId();
            } else if (nodeIdentity.nodeClass() != IFlowNode.class) {
                nodeId = nodeIdentity.nodeClass().getName();
            } else {
                nodeId = this.getClass().getName();
            }
        }
    }

    @Override
    public String nodeId() {
        return nodeId;
    }

    protected IContextBus getContextBus() {
        return ContextBus.get();
    }

    public void process() {
        ContextBus contextBus = ((ContextBus) getContextBus());
        I input = contextBus.getPreResult();
        Info info = ContextBus.getNodeInfo(FlowUtil.getNodeInfoKey(nodeId));
        if (info != null && info.input != null) {
            input = (I) info.input.apply(contextBus);
        }
        O result = doProcess(input);
        if (result != null) {
            String idTmp = nodeId;
            Object adapterResult = null;
            if (info != null) {
                if (info.output != null) {
                    adapterResult = info.output.apply(contextBus, result);
                }
                if (StringUtils.isNotEmpty(info.getIdAlias())) {
                    idTmp = info.getIdAlias();
                }
            }
            if (adapterResult != null) {
                contextBus.putPassResult(idTmp, adapterResult);
            } else {
                contextBus.putPassResult(idTmp, result);
            }
            contextBus.putPreResult(result);
            contextBus.setResult(result);
        }
    }

    public abstract O doProcess(I input);
}
