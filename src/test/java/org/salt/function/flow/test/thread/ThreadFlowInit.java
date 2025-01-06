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
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.demo.math.node.AddNode;
import org.salt.function.flow.demo.math.node.DivisionNode;
import org.salt.function.flow.demo.math.node.MultiplyNode;
import org.salt.function.flow.demo.math.node.ReduceNode;
import org.salt.function.flow.node.IResult;
import org.salt.function.flow.test.thread.node.BitLeftNode;
import org.salt.function.flow.test.thread.node.BitRightNode;

import java.util.concurrent.Executors;

public class ThreadFlowInit implements IFlowInit {

    @Override
    public void configure(FlowEngine flowEngine) {

        flowEngine.builder().id("demo_flow_concurrent_timeout")
                .next(AddNode.class)
                .concurrent(new AddResult(), 10, ReduceNode.class, BitRightNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_future_timeout")
                .next(AddNode.class)
                .future(ReduceNode.class, BitRightNode.class)
                .wait(new AddResult(), 10, ReduceNode.class, BitRightNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_concurrent_isolate")
                .next(AddNode.class)
                .concurrent(new AddResult(), Executors.newFixedThreadPool(3), ReduceNode.class, BitRightNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_flow_concurrent_threadlocal")
                .next(AddNode.class)
                .concurrent(new ReduceResult(), ReduceNode.class, BitLeftNode.class)
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_bit_right").next(ReduceNode.class).next(BitRightNode.class).register();
        flowEngine.builder().id("demo_branch_bit_left").next(MultiplyNode.class).next(BitLeftNode.class).register();

        flowEngine.builder().id("demo_branch_flow_concurrent_timeout")
                .next(AddNode.class)
                .concurrent(new AddBranchResult(), 10, "demo_branch_bit_right", "demo_branch_bit_left")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_future_timeout")
                .next(AddNode.class)
                .future("demo_branch_bit_right", "demo_branch_bit_left")
                .wait(new AddBranchResult(), 20,"demo_branch_bit_right", "demo_branch_bit_left")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_concurrent_isolate")
                .next(AddNode.class)
                .concurrent(new AddBranchResult(), Executors.newFixedThreadPool(3), "demo_branch_bit_right", "demo_branch_bit_left")
                .next(DivisionNode.class)
                .register();

        flowEngine.builder().id("demo_branch_flow_concurrent_threadlocal")
                .next(AddNode.class)
                .concurrent(new AddBranchResult(), "demo_branch_bit_right", "demo_branch_bit_left")
                .next(DivisionNode.class)
                .register();
    }

    private static class AddResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            System.out.println("AddResult handle isTimeout: " + isTimeout);
            Integer demoReduceResult = iContextBus.getResult(ReduceNode.class.getName()) != null && iContextBus.getResult(ReduceNode.class.getName()) instanceof Integer ?  (Integer) iContextBus.getResult(ReduceNode.class.getName()) : 0;
            Integer demoBitRightResult = iContextBus.getResult(BitRightNode.class.getName()) != null && iContextBus.getResult(BitRightNode.class.getName()) instanceof Integer ? (Integer) iContextBus.getResult(BitRightNode.class.getName()): 0;
            Integer handleResult = demoReduceResult + demoBitRightResult;
            System.out.println("Addresult " + demoReduceResult + "+" + demoBitRightResult + "=" + handleResult);
            return handleResult;
        }
    }

    private static class ReduceResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            System.out.println("AddResult handle isTimeout: " + isTimeout);
            Integer demoReduceResult = iContextBus.getResult(ReduceNode.class.getName()) != null && iContextBus.getResult(ReduceNode.class.getName()) instanceof Integer ?  (Integer) iContextBus.getResult(ReduceNode.class.getName()) : 0;
            Integer demoBitRightResult = iContextBus.getResult(BitLeftNode.class.getName()) != null && iContextBus.getResult(BitLeftNode.class.getName()) instanceof Integer ? (Integer) iContextBus.getResult(BitLeftNode.class.getName()): 0;
            Integer handleResult = demoReduceResult - demoBitRightResult;
            System.out.println("ReduceResult " + demoReduceResult + "-" + demoBitRightResult + "=" + handleResult);
            return handleResult;
        }
    }

    private static class AddBranchResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            System.out.println("AddBranchResult handle isTimeout: " + isTimeout);
            Integer demoBitRightResult = iContextBus.getResult("demo_branch_bit_right") != null && iContextBus.getResult("demo_branch_bit_right") instanceof Integer ?  (Integer) iContextBus.getResult("demo_branch_bit_right") : 0;
            Integer demoBitLeftResult = iContextBus.getResult("demo_branch_bit_left") != null && iContextBus.getResult("demo_branch_bit_left") instanceof Integer ? (Integer) iContextBus.getResult("demo_branch_bit_left"): 0;
            Integer handleResult = demoBitRightResult + demoBitLeftResult;
            System.out.println("AddBranchResult " + demoBitRightResult + "+" + demoBitLeftResult + "=" + handleResult);
            return handleResult;
        }
    }
}
