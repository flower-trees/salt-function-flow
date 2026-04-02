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
import org.salt.function.flow.demo.order.node.*;
import org.salt.function.flow.demo.order.param.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Demonstrates exclusive conditional routing and rollback compensation.
 *
 * VIP:     ItemPriceNode -> MemberDiscountNode(500*0.85=425) -> TaxNode(425+6%=450) -> OrderCreateNode -> InventoryDeductNode -> notify
 * Non-VIP: ItemPriceNode -> CouponDiscountNode(500-30=470)   -> TaxNode(470+6%=498) -> OrderCreateNode -> InventoryDeductNode -> notify
 *
 * InventoryDeductNode.rollback() restores inventory if flow is rolled back.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class OrderConditionTest {

    @Autowired
    FlowEngine flowEngine;

    @Test
    public void testVipOrder() {
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

        System.out.println("VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(450, result.getFinalPrice());
    }

    @Test
    public void testNonVipOrder() {
        Order order = Order.builder()
                .itemId("ITEM-002").userId("user-normal")
                .vip(false).hasCoupon(true).basePrice(500)
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
                Map.of("vip", false));

        System.out.println("Non-VIP finalPrice=" + result.getFinalPrice());
        Assert.assertEquals(498, result.getFinalPrice());
    }
}
