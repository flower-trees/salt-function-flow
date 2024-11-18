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
import org.salt.function.flow.context.IContextBus;
import org.salt.function.flow.node.structure.FlowNodeStructure;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class FlowNodeLoop<O> extends FlowNodeStructure<O> {

    Function<IContextBus, Boolean> loopCondition;

    public FlowNodeLoop(Function<IContextBus, Boolean> loopCondition) {
        this.loopCondition = loopCondition;
    }

    @Override
    public O doProcessGateway(List<Info> infoList) {
        IContextBus iContextBus = getContextBus();
        while(loopCondition.apply(iContextBus)) {
            for (Info info : infoList) {
                theadHelper.getDecoratorSync(() -> {
                    try {
                        execute(info);
                    } catch (Exception e) {
                        ((ContextBus) iContextBus).putPassException(info.getId(), e);
                    }
                }, info).run();
                if (isSuspend(iContextBus)) {
                    return null;
                }
            }
        }
        return null;
    }
}
