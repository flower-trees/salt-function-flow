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
import org.salt.function.flow.TestApplication;
import org.salt.function.flow.demo.order.node.*;
import org.salt.function.flow.demo.order.param.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Demonstrates concurrent discount aggregation and async notification.
 *
 * Order(basePrice=500, vip=true, hasCoupon=true)
 * -> ItemPriceNode: 500
 * -> concurrent: MemberDiscountNode(500*0.85=425), CouponDiscountNode(500-30=470)
 * -> pick best (min): 425
 * -> TaxNode: 425 + 6% = 450
 * -> OrderCreateNode: create order
 * -> notify: async SMS (non-blocking)
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class OrderConcurrentTest {

    @Autowired
    FlowEngine flowEngine;

    @Test
    public void testOrderWithConcurrentDiscount() {
        Order order = Order.builder()
                .itemId("ITEM-001")
                .userId("user-123")
                .vip(true)
                .hasCoupon(true)
                .basePrice(500)
                .build();

        Order result = flowEngine.execute(
                flowEngine.builder()
                        .next(ItemPriceNode.class)
                        .concurrent(MemberDiscountNode.class, CouponDiscountNode.class)
                        .next(map -> ((Map<String, Object>) map).values().stream()
                                .filter(v -> v instanceof Integer)
                                .mapToInt(v -> (Integer) v)
                                .min().orElse(0))
                        .next(TaxNode.class)
                        .next(OrderCreateNode.class)
                        .notify(NotifyNode.class)
                        .build(),
                order);

        System.out.println("orderNo=" + result.getOrderNo() + ", finalPrice=" + result.getFinalPrice());
        Assert.assertNotNull(result.getOrderNo());
        Assert.assertEquals(450, result.getFinalPrice());
    }
}
