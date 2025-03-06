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

package org.salt.function.flow.demo.math;

import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.Info;
import org.salt.function.flow.config.IFlowInit;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.demo.math.node.*;

import java.util.Map;

public class DemoFlowInit implements IFlowInit {

    /**
     * Build and register all flow
     */
    @Override
    public void configure(FlowEngine flowEngine) {

        //simple
        /**
         * Single flow construction
         */
        flowEngine.builder().id("demo_flow")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        /**
         * Single flow with condition extend construction
         */
        flowEngine.builder().id("demo_flow_extend")
                .next(AddNode.class)
                .next(
                        Info.c("param <= 30", ReduceNode.class),
                        Info.c("param > 30", RemainderNode.class)
                )
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        /**
         * Exclusive flow construction
         */
        flowEngine.builder().id("demo_flow_exclusive")
                .next(AddNode.class)
                .next(
                        Info.c("param <= 30", ReduceNode.class),
                        Info.c("param > 30", MultiplyNode.class)
                )
                .next(DivisionNode.class).register();

        /**
         * Concurrent flow construction
         */
        flowEngine.builder().id("demo_flow_concurrent")
                .next(AddNode.class)
                .concurrent(ReduceNode.class, MultiplyNode.class)
                .next(DemoFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        /**
         * Notify flow construction
         */
        flowEngine.builder().id("demo_flow_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        /**
         * Future/Wait flow construction
         */
        flowEngine.builder().id("demo_flow_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class)
                .wait(ReduceNode.class, MultiplyNode.class)
                .next(DemoFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_future_1")
                .next(AddNode.class)
                .future(ReduceNode.class)
                .next(MultiplyNode.class)
                .wait(ReduceNode.class)
                .next(input -> (Integer) ContextBus.get().getResult(MultiplyNode.class) + addResult(input))
                .next(DivisionNode.class)
                .register();

        /**
         * Inclusive flow construction
         */
        flowEngine.builder().id("demo_flow_inclusive")
                .next(AddNode.class)
                .all(
                        Info.c("param > 30",ReduceNode.class),
                        Info.c("param < 50", MultiplyNode.class)
                )
                .next(DivisionNode.class).register();

        /**
         * Inclusive concurrent flow construction
         */
        flowEngine.builder().id("demo_flow_inclusive_concurrent")
                .next(AddNode.class)
                .concurrent(
                        Info.c("param > 30",ReduceNode.class),
                        Info.c("param < 50", MultiplyNode.class)
                )
                .next(DemoFlowInit::addResult)
                .next(DivisionNode.class).register();

        /**
         * loop flow construction
         */
        flowEngine.builder().id("demo_flow_loop")
                .next(AddNode.class)
                .loop(i -> (Integer) ContextBus.get().getPreResult() < 56000000, ReduceNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .register();

        //branch
        flowEngine.builder().id("demo_branch_reduce").next(ReduceNode.class).next(RemainderNode.class).register();
        flowEngine.builder().id("demo_branch_multiply").next(MultiplyNode.class).next(RemainderNode.class).register();

        /**
         * Exclusive branch flow construction
         */
        flowEngine.builder().id("demo_branch_exclusive")
                .next(AddNode.class)
                .next(
                        Info.c("param <= 30", "demo_branch_reduce"),
                        Info.c("param > 30", "demo_branch_multiply")
                )
                .next(DivisionNode.class)
                .register();

        /**
         * Concurrent branch flow construction
         */
        flowEngine.builder().id("demo_branch_concurrent")
                .next(AddNode.class)
                .concurrent("demo_branch_reduce", "demo_branch_multiply")
                .next(DemoFlowInit::addResult)
                .next(DivisionNode.class)
                .register();

        /**
         * Notify branch flow construction
         */
        flowEngine.builder().id("demo_branch_notify")
                .next(AddNode.class)
                .notify("demo_branch_reduce")
                .next("demo_branch_multiply")
                .next(DivisionNode.class).register();

        /**
         * Future/Wait branch flow construction
         */
        flowEngine.builder().id("demo_branch_future")
                .next(AddNode.class)
                .future("demo_branch_reduce")
                .next("demo_branch_multiply")
                .wait("demo_branch_reduce")
                .next(input -> (Integer) ContextBus.get().getResult("demo_branch_multiply") + addResult(input))
                .next(DivisionNode.class).register();

        /**
         * Inclusive branch flow construction
         */
        flowEngine.builder().id("demo_branch")
                .next(AddNode.class)
                .next("demo_branch_reduce")
                .next("demo_branch_multiply")
                .next(DivisionNode.class)
                .register();

        /**
         * Nested branch flow construction
         */
        flowEngine.builder().id("demo_branch_nested")
                .next(AddNode.class)
                .all(
                        flowEngine.builder().id("nested_1").next(ReduceNode.class).next(RemainderNode.class).register(),
                        flowEngine.builder().id("nested_2").next(MultiplyNode.class).next(RemainderNode.class).register())
                .next(DivisionNode.class)
                .register();

        /**
         * Nested branch flow with anonymous branch construction
         */
        flowEngine.builder().id("demo_branch_anonymous")
                .next(AddNode.class)
                .all(
                        flowEngine.builder().next(ReduceNode.class).next(RemainderNode.class).build(),
                        flowEngine.builder().next(MultiplyNode.class).next(RemainderNode.class).build())
                .next(DivisionNode.class)
                .register();
    }



    @SuppressWarnings("unchecked")
    public static Integer addResult(Object map) {
        assert map instanceof Map;
        return ((Map<String, Object>) map).values().stream()
                .filter(value -> value instanceof Integer)
                .mapToInt(value -> (Integer) value)
                .sum();
    }
}
