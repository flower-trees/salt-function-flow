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
import java.util.Map;

@Slf4j
public class FlowNodeWait extends FlowNodeStructure<Map<String, Object>> {

    @Override
    public Map<String, Object> doProcessGateway(List<Info> infoList) {
        IContextBus iContextBus = getContextBus();
        long lastTimeout = theadHelper.getTimeout();
        for (Info info : infoList) {
            if (lastTimeout <= 0) {
                ((ContextBus) iContextBus).putException(info.getIdOrAlias(), new RuntimeException("beyond maxTimeout"));
                return handle(infoList, true);
            }
            long start = System.currentTimeMillis();
            try {
                Object result = ((ContextBus) iContextBus).getResult(info.getIdOrAlias(), lastTimeout);
                if (result != null) {
                    ((ContextBus) iContextBus).putResult(info.getIdOrAlias(), result);
                } else {
                    ((ContextBus) iContextBus).removeResult(info.getIdOrAlias());
                }
            } catch (Exception e) {
                ((ContextBus) iContextBus).putException(info.getIdOrAlias(), e);
            }
            lastTimeout -= System.currentTimeMillis() - start;
        }
        if (isSuspend(iContextBus)) {
            return null;
        }
        mergeRunIds();
        return handle(infoList, lastTimeout <= 0);
    }
}
