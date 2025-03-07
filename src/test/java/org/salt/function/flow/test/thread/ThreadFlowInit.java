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

package org.salt.function.flow.test.thread;

import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.config.IFlowInit;
import org.salt.function.flow.demo.math.node.AddNode;
import org.salt.function.flow.demo.math.node.DivisionNode;
import org.salt.function.flow.demo.math.node.MultiplyNode;
import org.salt.function.flow.demo.math.node.ReduceNode;
import org.salt.function.flow.test.thread.node.BitLeftNode;
import org.salt.function.flow.test.thread.node.BitRightNode;

import java.util.Map;
import java.util.concurrent.Executors;

public class ThreadFlowInit implements IFlowInit {

    @Override
    public void configure(FlowEngine flowEngine) {

        flowEngine.builder().id("demo_flow_concurrent_timeout")
                .next(AddNode.class)
                .concurrent(10, ReduceNode.class, BitRightNode.class)
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_future_timeout")
                .next(AddNode.class)
                .future(ReduceNode.class, BitRightNode.class)
                .wait(10, ReduceNode.class, BitRightNode.class)
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_concurrent_isolate")
                .next(AddNode.class)
                .concurrent(Executors.newFixedThreadPool(3), 300, ReduceNode.class, BitRightNode.class)
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_concurrent_threadlocal")
                .next(AddNode.class)
                .concurrent(ReduceNode.class, BitLeftNode.class)
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_right").next(ReduceNode.class).next(BitRightNode.class).register();
        flowEngine.builder().id("demo_branch_bit_left").next(MultiplyNode.class).next(BitLeftNode.class).register();

        flowEngine.builder().id("demo_branch_flow_concurrent_timeout")
                .next(AddNode.class)
                .concurrent(10, "demo_branch_bit_right", "demo_branch_bit_left")
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_future_timeout")
                .next(AddNode.class)
                .future("demo_branch_bit_right", "demo_branch_bit_left")
                .wait(20,"demo_branch_bit_right", "demo_branch_bit_left")
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_concurrent_isolate")
                .next(AddNode.class)
                .concurrent(Executors.newFixedThreadPool(3), 300, "demo_branch_bit_right", "demo_branch_bit_left")
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_concurrent_threadlocal")
                .next(AddNode.class)
                .concurrent("demo_branch_bit_right", "demo_branch_bit_left")
                .next(ThreadFlowInit::addResult)
                .next(DivisionNode.class)
                .register();
    }

    @SuppressWarnings("unchecked")
    public static Object addResult(Object map) {
        assert map instanceof Map;
        return ((Map<String, Object>) map).values().stream()
                .filter(value -> value instanceof Integer)
                .mapToInt(value -> (Integer) value)
                .sum();
    }
}
