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
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.util.FlowUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.annotation.PostConstruct;
import java.util.Map;

@Data
public class FlowNodeScanner implements ApplicationContextAware {

    private FlowNodeManager flowNodeManager;

    private ApplicationContext applicationContext;

    public FlowNodeScanner(FlowNodeManager flowNodeManager) {
        this.flowNodeManager = flowNodeManager;
    }

    @PostConstruct
    public void init() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        Map<String, Object> extensionBeans = applicationContext.getBeansWithAnnotation(NodeIdentity.class);
        extensionBeans.values().forEach(
            node -> {
                FlowNode<?,?> flowNode = null;
                try {
                    flowNode = ((FlowNode<?,?>) FlowUtil.getTarget(node));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                flowNode.setNodeId(getNodeId(flowNode));
                flowNodeManager.doRegistration(flowNode);
            }
        );
    }

    private String getNodeId(Object node) {
        String nodeId = null;
        NodeIdentity nodeIdentity = node.getClass().getAnnotation(NodeIdentity.class);
        if (nodeIdentity != null) {
            if (StringUtils.isNotEmpty(nodeIdentity.value())) {
                nodeId = nodeIdentity.value();
            } else if(StringUtils.isNotEmpty(nodeIdentity.nodeId())) {
                nodeId = nodeIdentity.nodeId();
            } else {
                nodeId = node.getClass().getName();
            }
        }
        return nodeId;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
