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

package org.salt.function.flow.demo.order;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.FlowInstance;
import org.salt.function.flow.Info;
import org.salt.function.flow.TestApplication;
import org.salt.function.flow.demo.order.node.PricingMemberDiscountNode;
import org.salt.function.flow.node.FlowNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Demonstrates 7 node reference styles using a pricing pipeline.
 *
 * input=200
 * -> +20 service fee          = 220  (node id)
 * -> *0.85 member discount    = 187  (class)
 * -> +6% tax                  = 198  (new instance)
 * -> -1                       = 197  (lambda)
 * -> /10*10 rounding          = 190  (method reference)
 * -> +5                       = 195  (nested flow)
 * -> -10 if service_fee > 100 = 185  (Info condition)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class NodeStyleTest {

    @Autowired
    FlowEngine flowEngine;

    @Test
    public void testPricingFlow() {
        FlowNode<Integer, Integer> taxNode = new FlowNode<>() {
            @Override
            public Integer process(Integer price) {
                return price + (int) (price * 0.06);
            }
        };

        FlowInstance flow = flowEngine.builder()
                .next("service_fee_node")                            // 1. node id       +20 service fee
                .next(PricingMemberDiscountNode.class)               // 2. class         *0.85 member discount
                .next(taxNode)                                       // 3. new instance  +6% tax
                .next(price -> (Integer) price - 1)                  // 4. lambda        -1 adjustment
                .next(NodeStyleTest::applyRounding)                  // 5. method ref    round down to tens
                .next(flowEngine.builder()                           // 6. nested flow   +5 subsidy
                        .next(price -> (Integer) price + 5)
                        .build())
                .next(Info.c("service_fee_node > 100",               // 7. Info cond     -10 rebate if fee > 100
                        price -> (Integer) price - 10))
                .build();

        Integer result = flowEngine.execute(flow, 200);
        System.out.println("NodeStyleTest result: " + result);
        Assert.assertEquals(185, (int) result);
    }

    public static Object applyRounding(Object price) {
        return (Integer) price / 10 * 10;
    }
}
