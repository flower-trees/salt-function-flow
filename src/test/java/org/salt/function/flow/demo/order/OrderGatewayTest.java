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
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.demo.order.node.*;
import org.salt.function.flow.demo.order.param.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Demonstrates all 7 gateway types in a single e-commerce order flow.
 *
 * Order(basePrice=500, vip=true, hasCoupon=true)
 *
 * future     UserProfileNode                        start async profile query early
 * next       ItemPriceNode                          500
 * all        PlatformPromotionNode(-20)             500 -> 480   both promotions apply
 *            ShopPromotionNode(-10)                 480 -> 470
 * concurrent MemberDiscountNode(*0.85=399)          pick best discount -> 399
 *            CouponDiscountNode(-30=440)
 * wait       UserProfileNode(-5)                    399 + (-5) = 394
 * next       TaxNode(+6%=417)                       417
 * loop       LockInventoryNode                      retry until lock succeeds (max 3x)
 * next       OrderCreateNode                        create order, finalPrice=417
 * notify     NotifyNode                             async SMS, non-blocking
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class OrderGatewayTest {

    @Autowired
    FlowEngine flowEngine;

    @Test
    public void testFullOrderFlow() {
        Order order = Order.builder()
                .itemId("ITEM-001").userId("user-vip")
                .vip(true).hasCoupon(true).basePrice(500)
                .build();

        FlowInstance flow = flowEngine.builder()
                .future(UserProfileNode.class)                                          // future:      start async profile query early
                .next(ItemPriceNode.class)                                              // next:        query item base price
                .all(                                                                   // all:         apply all matched promotions
                        Info.c("basePrice >= 300", PlatformPromotionNode.class),        //   platform promotion -20 if price >= 300
                        Info.c("basePrice >= 200", ShopPromotionNode.class)             //   shop promotion    -10 if price >= 200
                )
                .concurrent(MemberDiscountNode.class, CouponDiscountNode.class)         // concurrent:  calc both discounts in parallel
                .next(Info.c(
                            map -> ((Map<String, Object>) map).values().stream()              // next:        pick the best (lowest) price
                            .filter(v -> v instanceof Integer)
                            .mapToInt(v -> (Integer) v).min().orElse(0)
                        ).cAlias("discount_price"))
                .wait(UserProfileNode.class)                                            // wait:        join profile result
                .next(ignored -> (Integer) ContextBus.get().getResult("discount_price") // next: price + profile adjustment
                        + (Integer) ((Map) ignored).get(UserProfileNode.class.getName()))
                .next(TaxNode.class)                                                    // next:        add tax
                .loop(i -> (Integer) ContextBus.get().getPreResult() < 0,              // loop:        retry lock until succeeded
                        LockInventoryNode.class)
                .next(OrderCreateNode.class)                                            // next:        create order
                .notify(NotifyNode.class)                                               // notify:      async SMS, non-blocking
                .build();

        Order result = flowEngine.execute(flow, order, Map.of("basePrice", 500));

        System.out.println("OrderGatewayTest: orderNo=" + result.getOrderNo() + ", finalPrice=" + result.getFinalPrice());
        Assert.assertNotNull(result.getOrderNo());
        Assert.assertEquals(417, result.getFinalPrice());
    }
}
