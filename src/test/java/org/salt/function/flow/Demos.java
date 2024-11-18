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

package org.salt.function.flow;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.salt.function.flow.demo.math.node.AddNode;
import org.salt.function.flow.demo.math.node.DivisionNode;
import org.salt.function.flow.demo.math.node.MultiplyNode;
import org.salt.function.flow.demo.math.node.ReduceNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class Demos {

    @Autowired
    FlowEngine flowEngine;

    /**
     * Single flow exe
     */
    @Test
    public void testDemo() {

        FlowInstance flowInstance = flowEngine.builder()
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .build();

        System.out.println("demo_flow test: ");
        Integer result = flowEngine.execute(flowInstance, 39);
        System.out.println("demo_flow result: " + result);
        Assert.assertTrue(result != null && result == 894);
    }
}
