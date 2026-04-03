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
import org.salt.function.flow.Info;
import org.salt.function.flow.TestApplication;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.demo.order.node.*;
import org.salt.function.flow.demo.order.param.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Demonstrates three styles of conditional routing using an e-commerce order flow.
 *
 * VIP:     ItemPriceNode(500) -> MemberDiscountNode(*0.85=425) -> TaxNode(+6%=450) -> OrderCreateNode -> InventoryDeductNode -> notify
 * Non-VIP: ItemPriceNode(500) -> CouponDiscountNode(-30=470)   -> TaxNode(+6%=498) -> OrderCreateNode -> InventoryDeductNode -> notify
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class OrderConditionTest {

    @Autowired
    FlowEngine flowEngine;

    /**
     * Style 1: string expression condition.
     * "vip" variable comes from the condition map passed at execution time.
     */
    @Test
    public void testConditionByMap() {
        Order order = Order.builder()
                .itemId("ITEM-002").userId("user-vip")
                .vip(true).hasCoupon(true).basePrice(500)
                .build();

        Order result = flowEngine.execute(
                flowEngine.builder()
                        .next(ItemPriceNode.class)
                        .next(
                                Info.c("vip == true", MemberDiscountNode.class),
                                Info.c("vip == false", CouponDiscountNode.class)
                        )
                        .next(TaxNode.class)
                        .next(OrderCreateNode.class)
                        .next(InventoryDeductNode.class)
                        .notify(NotifyNode.class)
                        .build(),
                order,
                Map.of("vip", true));

        System.out.println("conditionByMap VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(450, result.getFinalPrice());
    }

    /**
     * Style 2: function condition.
     * Reads flow param directly at runtime — no condition map needed.
     */
    @Test
    public void testConditionByFunction() {
        Order order = Order.builder()
                .itemId("ITEM-002").userId("user-normal")
                .vip(false).hasCoupon(true).basePrice(500)
                .build();

        Order result = flowEngine.execute(
                flowEngine.builder()
                        .next(ItemPriceNode.class)
                        .next(
                                Info.c(bus -> ((Order) ContextBus.get().getFlowParam()).isVip(), MemberDiscountNode.class),
                                Info.c(bus -> !((Order) ContextBus.get().getFlowParam()).isVip(), CouponDiscountNode.class)
                        )
                        .next(TaxNode.class)
                        .next(OrderCreateNode.class)
                        .next(InventoryDeductNode.class)
                        .notify(NotifyNode.class)
                        .build(),
                order);

        System.out.println("conditionByFunction non-VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(498, result.getFinalPrice());
    }

    /**
     * Style 3: condition injected dynamically inside a node.
     * ItemPriceNode calls addCondition("vip", ...), making "vip" available for
     * downstream string expressions without passing a condition map at call site.
     */
    @Test
    public void testConditionByNodeInject() {
        Order order = Order.builder()
                .itemId("ITEM-002").userId("user-vip")
                .vip(true).hasCoupon(true).basePrice(500)
                .build();

        Order result = flowEngine.execute(
                flowEngine.builder()
                        .next(ItemPriceNode.class)          // ItemPriceNode calls addCondition("vip", order.isVip())
                        .next(
                                Info.c("vip == true", MemberDiscountNode.class),
                                Info.c("vip == false", CouponDiscountNode.class)
                        )
                        .next(TaxNode.class)
                        .next(OrderCreateNode.class)
                        .next(InventoryDeductNode.class)
                        .notify(NotifyNode.class)
                        .build(),
                order);

        System.out.println("conditionByNodeInject VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(450, result.getFinalPrice());
    }

    /**
     * Style 4: node return value as condition.
     * When a node returns a Map, the framework automatically adds all key-value pairs
     * into the condition context. Downstream string expressions can use them directly.
     *
     * ItemPriceWithTagNode returns Map.of("price", 500, "vip", true),
     * so "vip == true" and "price >= 300" are available as condition expressions downstream.
     */
    @Test
    public void testConditionByReturnValue() {
        Order order = Order.builder()
                .itemId("ITEM-002").userId("user-vip")
                .vip(true).hasCoupon(true).basePrice(500)
                .build();

        Order result = flowEngine.execute(
                flowEngine.builder()
                        .next(ItemPriceWithTagNode.class)    // returns Map{"price":500, "vip":true} -> auto-injected into condition context
                        .next(
                                Info.c("vip == true", MemberDiscountNode.class)    // "vip" comes from the returned Map
                                        .cInput(map -> ((java.util.Map) map).get("price")),
                                Info.c("vip == false", CouponDiscountNode.class)
                                        .cInput(map -> ((java.util.Map) map).get("price"))
                        )
                        .next(TaxNode.class)
                        .next(OrderCreateNode.class)
                        .notify(NotifyNode.class)
                        .build(),
                order);

        System.out.println("conditionByReturnValue VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(450, result.getFinalPrice());
    }
}
