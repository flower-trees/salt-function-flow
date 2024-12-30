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

package org.salt.function.flow.node.structure.internal;

import lombok.extern.slf4j.Slf4j;
import org.salt.function.flow.Info;
import org.salt.function.flow.context.ContextBus;
import org.salt.function.flow.node.structure.FlowNodeStructure;

import java.util.List;
import java.util.concurrent.Future;

@Slf4j
public class FlowNodeFuture extends FlowNodeStructure<Future<?>> {

    @Override
    public Future<?> doProcessGateway(List<Info> infoList) {
        for (Info info : infoList) {
            Future<?> future = theadHelper.submit(() -> {
                try {
                    return execute(info);
                } catch (Exception e) {
                    ((ContextBus) getContextBus()).putException(info.getId(), e);
                }
                return null;
            });
            ((ContextBus) getContextBus()).putResult(info.getId(), future);
        }
        return null;
    }
}
