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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.node.FlowNode;

import java.util.function.BiFunction;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Info {
    private String id;
    private String include;
    private Function<IContextBus, Boolean> match;
    private Function<IContextBus, Object> input;
    private BiFunction<IContextBus, Object, Object> output;
    private String idAlias;
    private Class<?> node;
    private FlowInstance flow;
    private FlowNode<?, ?> flowNode;
    private Function<Object, ?> funNode;

    public String getId() {
        if (StringUtils.isNotEmpty(id)) {
            return id;
        }
        if (node != null) {
            return node.getName();
        }
        if (flow != null) {
            return flow.getFlowId();
        }
        if (flowNode != null) {
            return flowNode.getNodeId();
        }
        if (funNode!= null) {
            return funNode.getClass().getName();
        }
        throw new RuntimeException("id is empty");
    }

    public String getIdOrAlias() {
        if (StringUtils.isNotEmpty(idAlias)) {
            return idAlias;
        } else {
            return getId();
        }
    }

    public static Info c(String id) {
        return Info.builder().id(id).build();
    }

    public static Info c(String include, String id) {
        return Info.builder().id(id).include(include).build();
    }

    public static Info c(Function<IContextBus, Boolean> match, String id) {
        return Info.builder().id(id).match(match).build();
    }

    public static Info c(Class<?> node) {
        return Info.builder().node(node).build();
    }

    public static Info c(String include, Class<?> node) {
        return Info.builder().node(node).include(include).build();
    }

    public static Info c(Function<IContextBus, Boolean> match, Class<?> node) {
        return Info.builder().node(node).match(match).build();
    }

    public static Info c(FlowInstance flow) {
        return Info.builder().flow(flow).build();
    }

    public static Info c(String include, FlowInstance flow) {
        return Info.builder().flow(flow).include(include).build();
    }

    public static Info c(Function<IContextBus, Boolean> match, FlowInstance flow) {
        return Info.builder().flow(flow).match(match).build();
    }

    public static Info c(FlowNode<?, ?> flowNode) {
        return Info.builder().flowNode(flowNode).build();
    }

    public static Info c(String include, FlowNode<?, ?> flowNode) {
        return Info.builder().flowNode(flowNode).include(include).build();
    }

    public static Info c(Function<IContextBus, Boolean> match, FlowNode<?, ?> flowNode) {
        return Info.builder().flowNode(flowNode).match(match).build();
    }

    public static Info c(Function<Object, ?> funNode) {
        return Info.builder().funNode(funNode).build();
    }

    public static Info c(String include, Function<Object, ?> funNode) {
        return Info.builder().funNode(funNode).include(include).build();
    }

    public static Info c(Function<IContextBus, Boolean> match, Function<Object, ?> funNode) {
        return Info.builder().funNode(funNode).match(match).build();
    }

    public Info cAlias(String id) {
        this.idAlias = id;
        return this;
    }

    public Info cInput(Function<IContextBus, Object> input) {
        this.input = input;
        return this;
    }

    public Info cOutput(BiFunction<IContextBus, Object, Object> output) {
        this.output = output;
        return this;
    }
}
