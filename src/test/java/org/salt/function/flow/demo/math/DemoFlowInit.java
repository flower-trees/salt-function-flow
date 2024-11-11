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
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.demo.math.node.*;
import org.salt.function.flow.node.IResult;

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
                .build();

        /**
         * Single flow with condition extend construction
         */
        flowEngine.builder().id("demo_flow_extend")
                .next(AddNode.class)
                .next(
                        Info.builder().include("param <= 30").node(ReduceNode.class).build(),
                        Info.builder().include("param > 30").node(RemainderNode.class).build()
                )
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        /**
         * Exclusive flow construction
         */
        flowEngine.builder().id("demo_flow_exclusive")
                .next(AddNode.class)
                .next(
                        Info.builder().include("param <= 30").node(ReduceNode.class).build(),
                        Info.builder().include("param > 30").node(MultiplyNode.class).build()
                )
                .next(DivisionNode.class).build();

        /**
         * Concurrent flow construction
         */
        flowEngine.builder().id("demo_flow_concurrent")
                .next(AddNode.class)
                .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        /**
         * Notify flow construction
         */
        flowEngine.builder().id("demo_flow_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        /**
         * Future/Wait flow construction
         */
        flowEngine.builder().id("demo_flow_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class)
                .wait(new AddResult(), ReduceNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_flow_future_1")
                .next(AddNode.class)
                .future(ReduceNode.class)
                .next(MultiplyNode.class)
                .wait(new AddResult(), ReduceNode.class)
                .next(DivisionNode.class)
                .build();

        /**
         * Inclusive flow construction
         */
        flowEngine.builder().id("demo_flow_inclusive")
                .next(AddNode.class)
                .all(
                        Info.builder().include("param > 30").node(ReduceNode.class).build(),
                        Info.builder().include("param < 50").node(MultiplyNode.class).build()
                )
                .next(DivisionNode.class).build();

        /**
         * Inclusive concurrent flow construction
         */
        flowEngine.builder().id("demo_flow_inclusive_concurrent")
                .next(AddNode.class)
                .concurrent(
                        new AddResult(),
                        Info.builder().include("param > 30").node(ReduceNode.class).build(),
                        Info.builder().include("param < 50").node(MultiplyNode.class).build()
                )
                .next(DivisionNode.class).build();

        //branch
        flowEngine.builder().id("demo_branch_reduce").next(ReduceNode.class).next(RemainderNode.class).build();
        flowEngine.builder().id("demo_branch_multiply").next(MultiplyNode.class).next(RemainderNode.class).build();

        /**
         * Exclusive branch flow construction
         */
        flowEngine.builder().id("demo_branch_exclusive")
                .next(AddNode.class)
                .next(
                        Info.builder().include("param <= 30").id("demo_branch_reduce").build(),
                        Info.builder().include("param > 30").id("demo_branch_multiply").build()
                )
                .next(DivisionNode.class)
                .build();

        /**
         * Concurrent branch flow construction
         */
        flowEngine.builder().id("demo_branch_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBranchResult(), "demo_branch_reduce", "demo_branch_multiply")
                .next(DivisionNode.class)
                .build();

        /**
         * Notify branch flow construction
         */
        flowEngine.builder().id("demo_branch_notify")
                .next(AddNode.class)
                .notify("demo_branch_reduce")
                .next("demo_branch_multiply")
                .next(DivisionNode.class).build();

        /**
         * Future/Wait branch flow construction
         */
        flowEngine.builder().id("demo_branch_future")
                .next(AddNode.class)
                .future("demo_branch_reduce")
                .next("demo_branch_multiply")
                .wait(new AddBranchResult(), "demo_branch_reduce")
                .next(DivisionNode.class).build();

        /**
         * Inclusive branch flow construction
         */
        flowEngine.builder().id("demo_branch")
                .next(AddNode.class)
                .all("demo_branch_reduce", "demo_branch_multiply")
                .next(DivisionNode.class)
                .build();

        /**
         * Nested branch flow construction
         */
        flowEngine.builder().id("demo_branch_nested")
                .next(AddNode.class)
                .all(
                        flowEngine.builder().id("nested_1").next(ReduceNode.class).next(RemainderNode.class).build(),
                        flowEngine.builder().id("nested_2").next(MultiplyNode.class).next(RemainderNode.class).build())
                .next(DivisionNode.class)
                .build();

        /**
         * Nested branch flow with anonymous branch construction
         */
        flowEngine.builder().id("demo_branch_anonymous")
                .next(AddNode.class)
                .all(
                        flowEngine.branch().next(ReduceNode.class).next(RemainderNode.class).build(),
                        flowEngine.branch().next(MultiplyNode.class).next(RemainderNode.class).build())
                .next(DivisionNode.class)
                .build();
    }

    private static class AddResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer demoReduceResult = iContextBus.getPassResult(ReduceNode.class.getName()) != null ?  (Integer) iContextBus.getPassResult(ReduceNode.class.getName()) : 0;
            Integer demoMultiplyResult = iContextBus.getPassResult(MultiplyNode.class.getName()) != null ? (Integer) iContextBus.getPassResult(MultiplyNode.class.getName()): 0;
            Integer handleResult = demoReduceResult + demoMultiplyResult;
            System.out.println("Addresult " + demoReduceResult + "+" + demoMultiplyResult + "=" + handleResult);
            return handleResult;
        }
    }

    private static class AddBranchResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer branchReduce = iContextBus.getPassResult("demo_branch_reduce") != null ? (Integer) iContextBus.getPassResult("demo_branch_reduce") : 0;
            Integer branchMultiply = iContextBus.getPassResult("demo_branch_multiply") != null ? (Integer) iContextBus.getPassResult("demo_branch_multiply") : 0;
            Integer handleResult = branchReduce + branchMultiply;
            System.out.println("AddBranchresult " + branchReduce + "+" + branchMultiply + "=" + handleResult);
            return handleResult;
        }
    }
}
