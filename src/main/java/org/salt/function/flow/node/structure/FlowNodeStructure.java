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

package org.salt.function.flow.node.structure;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.Info;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.IResult;
import org.salt.function.flow.node.register.FlowNodeManager;
import org.salt.function.flow.thread.TheadHelper;
import org.salt.function.flow.util.FlowUtil;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class FlowNodeStructure<O> extends FlowNode<O, Object> {

    @Setter
    protected FlowEngine flowEngine;

    @Setter
    protected FlowNodeManager flowNodeManager;

    @Setter
    protected IResult<O> result;

    protected List<Info> infoList;

    @Setter
    protected TheadHelper theadHelper;

    public void setNodeInfoList(List<Info> infoList) {
        this.infoList = infoList;
    }

    protected boolean isFlowNode(String nodeId) {
        if (StringUtils.isEmpty(nodeId)) {
            return false;
        }
        return flowNodeManager.getIFlowNode(nodeId) != null;
    }

    public O process(Object input) {
        if (CollectionUtils.isEmpty(infoList)) {
            return null;
        }
        List<Info> infoListExe = infoList.stream().filter(info -> FlowUtil.isExe(getContextBus(), info)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(infoListExe)) {
            return null;
        }
        return doProcessGateway(infoListExe);
    }

    protected abstract O doProcessGateway(List<Info> infoList);

    protected O execute(Info info) {
        if (info.getFlowNode() != null) {
            return flowNodeManager.execute((FlowNode<O, ?>) info.getFlowNode(), info);
        } else if (info.getFunNode() != null) {
            return flowNodeManager.execute(new FlowNode<>() {
                @Override
                public O process(Object input) {
                    return (O) info.getFunNode().apply(input);
                }
            } , info);
        } else if (isFlowNode(info.getId())) {
            return flowNodeManager.execute(info.getId(), info);
        } else {
            if (info.getFlow() != null) {
                return flowEngine.execute(info.getFlow());
            }
            return flowEngine.execute(info.getId());
        }
    }

    protected boolean isSuspend(IContextBus iContextBus) {
        return ((ContextBus) iContextBus).isRollbackProcess() || ((ContextBus) iContextBus).isStopProcess();
    }
}
