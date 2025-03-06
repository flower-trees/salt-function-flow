/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.salt.function.flow.example;

import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.FlowInstance;
import org.salt.function.flow.Info;
import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.register.NodeIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowBuildExample {

    @Autowired
    FlowEngine flowEngine;

    @NodeIdentity("add_node")
    public static class Add extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num + 123; // 加 123
        }
    }

    @NodeIdentity
    public static class Reduce extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num - 15;
        }
    }

    public void run() {

        FlowNode<Integer, Integer> flowNode = new FlowNode<>() {
            @Override
            public Integer process(Integer num) {
                return num * 73;
            }
        };

        FlowInstance flow = flowEngine.builder()
                .next("add_node") // 1.id
                .next(Reduce.class) // 2.class
                .next(flowNode)  // 3.new
                .next(num -> (Integer) num / 12) // 4.lambda
                .next(flowEngine.builder().next(num -> (Integer) num % 12).build()) // 5.nesting flow
                .next(Info.c(input -> (Integer) input >> 1)) // 6.Info
                .build();

        Integer result = flowEngine.execute(flow, 39);
        System.out.println("result: " + result);
    }
}