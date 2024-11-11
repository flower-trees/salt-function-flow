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
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.demo.math.node.*;
import org.salt.function.flow.node.IResult;
import org.salt.function.flow.test.stop.node.BitAndNode;
import org.salt.function.flow.test.stop.node.BitOrNode;
import org.salt.function.flow.test.stop.node.BitXorNode;

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
                .next(DivisionNode.class).build();

        flowEngine.builder().id("demo_bit_and_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBitAndResult(), ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_and_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .wait(new AddBitAndResult(), ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_and_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitAndNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_and_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitAndNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_or")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(BitOrNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class).build();

        flowEngine.builder().id("demo_bit_or_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBitOrResult(), ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_or_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .wait(new AddBitOrResult(), ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_or_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitOrNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_or_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitOrNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_xor")
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(BitXorNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class).build();

        flowEngine.builder().id("demo_bit_xor_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBitAndResult(), BitXorNode.class, ReduceNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_xor_future")
                .next(AddNode.class)
                .future(ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .wait(new AddBitAndResult(), ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_xor_all")
                .next(AddNode.class)
                .all(ReduceNode.class, BitXorNode.class, MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_bit_xor_notify")
                .next(AddNode.class)
                .notify(ReduceNode.class, MultiplyNode.class, BitXorNode.class)
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_and_reduce").next(ReduceNode.class).next(BitAndNode.class).next(RemainderNode.class).build();
        flowEngine.builder().id("demo_branch_bit_and_multiply").next(MultiplyNode.class).next(RemainderNode.class).build();

        flowEngine.builder().id("demo_branch_bit_and")
                .next(AddNode.class)
                .next("demo_branch_bit_and_reduce")
                .next("demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_and_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBranchBitAndResult(),"demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_and_future")
                .next(AddNode.class)
                .future("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .wait(new AddBranchBitAndResult(),"demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_and_all")
                .next(AddNode.class)
                .all("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_and_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_and_reduce", "demo_branch_bit_and_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_or_reduce").next(ReduceNode.class).next(BitOrNode.class).next(RemainderNode.class).build();
        flowEngine.builder().id("demo_branch_bit_or_multiply").next(MultiplyNode.class).next(RemainderNode.class).build();

        flowEngine.builder().id("demo_branch_bit_or")
                .next(AddNode.class)
                .all("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_or_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBranchBitOrResult(), "demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_or_future")
                .next(AddNode.class)
                .future("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .wait(new AddBranchBitOrResult(),"demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_or_all")
                .next(AddNode.class)
                .all("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_or_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_or_reduce", "demo_branch_bit_or_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_xor_reduce").next(ReduceNode.class).next(BitXorNode.class).next(RemainderNode.class).build();
        flowEngine.builder().id("demo_branch_bit_xor_multiply").next(MultiplyNode.class).next(RemainderNode.class).build();

        flowEngine.builder().id("demo_branch_bit_xor")
                .next(AddNode.class)
                .next("demo_branch_bit_xor_reduce")
                .next("demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_xor_concurrent")
                .next(AddNode.class)
                .concurrent(new AddBranchBitAndResult(),"demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_xor_future")
                .next(AddNode.class)
                .future("demo_branch_bit_xor_multiply", "demo_branch_bit_xor_reduce")
                .wait(new AddBranchBitAndResult(),"demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_xor_all")
                .next(AddNode.class)
                .all("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .build();

        flowEngine.builder().id("demo_branch_bit_xor_notify")
                .next(AddNode.class)
                .notify("demo_branch_bit_xor_reduce", "demo_branch_bit_xor_multiply")
                .next(DivisionNode.class)
                .build();
    }

    private static class AddBitAndResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer demoReduceResult = iContextBus.getPassResult(ReduceNode.class.getName()) != null ?  (Integer) iContextBus.getPassResult(ReduceNode.class.getName()) : 0;
            Integer demoMultiplyResult = iContextBus.getPassResult(MultiplyNode.class.getName()) != null ? (Integer) iContextBus.getPassResult(MultiplyNode.class.getName()): 0;
            Integer demoBitAndResult = iContextBus.getPassResult(BitAndNode.class.getName()) != null ? (Integer) iContextBus.getPassResult(BitAndNode.class.getName()): 0;
            Integer handleResult = demoReduceResult + demoMultiplyResult + demoBitAndResult;
            System.out.println("Addresult " + demoReduceResult + "+" + demoMultiplyResult + "+" + demoBitAndResult + "=" + handleResult);
            return handleResult;
        }
    }

    private static class AddBitOrResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer demoReduceResult = iContextBus.getPassResult(ReduceNode.class.getName()) != null ?  (Integer) iContextBus.getPassResult(ReduceNode.class.getName()) : 0;
            Integer demoMultiplyResult = iContextBus.getPassResult(MultiplyNode.class.getName()) != null ? (Integer) iContextBus.getPassResult(MultiplyNode.class.getName()): 0;
            Integer demoBitAndResult = iContextBus.getPassResult(BitOrNode.class.getName()) != null ? (Integer) iContextBus.getPassResult(BitOrNode.class.getName()): 0;
            Integer handleResult = demoReduceResult + demoMultiplyResult + demoBitAndResult;
            System.out.println("Addresult " + demoReduceResult + "+" + demoMultiplyResult + "+" + demoBitAndResult + "=" + handleResult);
            return handleResult;
        }
    }

    private static class AddBranchBitAndResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer branchReduce = iContextBus.getPassResult("demo_branch_bit_and_reduce") != null ? (Integer) iContextBus.getPassResult("demo_branch_bit_and_reduce") : 0;
            Integer branchMultiply = iContextBus.getPassResult("demo_branch_bit_and_multiply") != null ? (Integer) iContextBus.getPassResult("demo_branch_bit_and_multiply") : 0;
            Integer handleResult = branchReduce + branchMultiply;
            System.out.println("AddBranchresult " + branchReduce + "+" + branchMultiply + "=" + handleResult);
            return handleResult;
        }
    }
    private static class AddBranchBitOrResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            Integer branchReduce = iContextBus.getPassResult("demo_branch_bit_or_reduce") != null ? (Integer) iContextBus.getPassResult("demo_branch_bit_or_reduce") : 0;
            Integer branchMultiply = iContextBus.getPassResult("demo_branch_bit_or_multiply") != null ? (Integer) iContextBus.getPassResult("demo_branch_bit_or_multiply") : 0;
            Integer handleResult = branchReduce + branchMultiply;
            System.out.println("AddBranchresult " + branchReduce + "+" + branchMultiply + "=" + handleResult);
            return handleResult;
        }
    }
}
