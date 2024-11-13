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

package org.salt.function.flow.demo.train.node;

import org.salt.function.flow.node.FlowNode;
import org.salt.function.flow.node.register.NodeIdentity;

@NodeIdentity(nodeId = "adult_tickt")
public class TrainAdultTicket extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer basePrice) {
        System.out.println("Issue adult ticket " + basePrice);
        return basePrice;
    }
}