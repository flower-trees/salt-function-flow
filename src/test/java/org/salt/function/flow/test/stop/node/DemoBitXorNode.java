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

package org.salt.function.flow.test.stop.node;

import org.salt.function.flow.node.FlowNodeWithReturn;
import org.salt.function.flow.node.register.NodeIdentity;

@NodeIdentity(nodeId = "demo_bit_xor")
public class DemoBitXorNode extends FlowNodeWithReturn<Integer> {

    @Override
    public Integer doProcess() {
        Integer preResult = (Integer) getContextBus().getPreResult();
        if (preResult > 500) {
            System.out.println("DemoBitOrNode: rollback flow");
            getContextBus().rollbackProcess();
        } else {
            Integer result = preResult | 128;
            System.out.println("DemoBitOrNode: " + preResult + "|128=" + result);
            return result;
        }
        return null;
    }

    @Override
    public void rollback() {
        System.out.println("DemoBitOrNode: rollback execute");
    }
}
