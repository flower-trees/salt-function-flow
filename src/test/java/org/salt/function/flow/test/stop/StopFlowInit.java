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

package org.salt.function.flow.test.stop;

import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.config.IFlowInit;
import org.salt.function.flow.demo.math.node.*;
import org.salt.function.flow.test.stop.node.BitAndNode;
import org.salt.function.flow.test.stop.node.BitOrNode;
import org.salt.function.flow.test.stop.node.BitXorNode;

import java.util.Map;

public class StopFlowInit implements IFlowInit {

    /**
     * Build and register all flow
     */
    @Override
    public void configure(FlowEngine flowEngine) {

        flowEngine.builder().id("demo_bit_and")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(BitAndNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_and_concurrent")
                .next(AddNode.class)
                .concurrent(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_and_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .wait(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_and_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitAndNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_and_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_or")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(BitOrNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_or_concurrent")
                .next(AddNode.class)
                .concurrent(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_or_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .wait(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_or_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitOrNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_or_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_xor")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(BitXorNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_xor_concurrent")
                .next(AddNode.class)
                .concurrent(BitXorNode.class, ReduceNode.class, MultiplyNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_xor_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .wait(ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_xor_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitXorNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_bit_xor_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_and_reduce").next(ReduceNode.class).next(BitAndNode.class).next(RemainderNode.class).register();
        flowEngine.builder().id("demo_branch_bit_and_multiply").next(MultiplyNode.class).next(RemainderNode.class).register();

        flowEngine.builder().id("demo_branch_bit_and")
                .next(AddNode.class)
                .next("demo_branch_bit_and_reduce")
                .next("demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_and_concurrent")
                .next(AddNode.class)
                .concurrent("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_and_future")
                .next(AddNode.class)
                .future("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .wait("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_and_all")
                .next(AddNode.class)
                .all("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_and_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_or_reduce").next(ReduceNode.class).next(BitOrNode.class).next(RemainderNode.class).register();
        flowEngine.builder().id("demo_branch_bit_or_multiply").next(MultiplyNode.class).next(RemainderNode.class).register();

        flowEngine.builder().id("demo_branch_bit_or")
                .next(AddNode.class)
                .all("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_or_concurrent")
                .next(AddNode.class)
                .concurrent("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_or_future")
                .next(AddNode.class)
                .future("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .wait("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_or_all")
                .next(AddNode.class)
                .all("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_or_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_xor_reduce").next(ReduceNode.class).next(BitXorNode.class).next(RemainderNode.class).register();
        flowEngine.builder().id("demo_branch_bit_xor_multiply").next(MultiplyNode.class).next(RemainderNode.class).register();

        flowEngine.builder().id("demo_branch_bit_xor")
                .next(AddNode.class)
                .next("demo_branch_bit_xor_reduce")
                .next("demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_xor_concurrent")
                .next(AddNode.class)
                .concurrent("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_xor_future")
                .next(AddNode.class)
                .future("demo_branch_bit_xor_multiply", "demo_branch_bit_xor_reduce")
                .wait("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(StopFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_xor_all")
                .next(AddNode.class)
                .all("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_xor_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
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
