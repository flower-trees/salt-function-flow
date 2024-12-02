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

package org.salt.function.flow.node;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.util.FlowUtil;

@Slf4j
@Data
public abstract class FlowNode<O, I> {

    protected String nodeId = FlowUtil.id();

    protected IContextBus getContextBus() {
        return ContextBus.get();
    }

    public abstract O process(I input);
    public void rollback() {}
}
