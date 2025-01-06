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

package org.salt.function.flow.context;

import java.util.List;

public interface IContextBus {

    /**
     * Get flow execution parameters
     */
    <T> T getFlowParam();

    /**
     * Get flow execution result
     */
    <R> R getFlowResult();

    /**
     * Put additional transmission context information
     */
    <P> void putTransmit(String key, P content);

    /**
     * Get additional transmission context information
     */
    <P> P getTransmit(String key);

    /**
     * Add the parameters of node condition judgment
     */
    <P> void addCondition(String key, P value);

    /**
     * Get the execution result of the last node, which may return null
     */
    <P> P getPreResult();

    /**
     * Get the execution result of any node by node ID
     */
    <P> P getResult(String nodeId);

    /**
     * Get the execution result of any node by node class
     */
    <P> P getResult(Class<?> clazz);

    /**
     * Get the execution exception of any node by node ID
     */
    Exception getException(String nodeId);


    /**
     * Get the execution exception of any node by node class
     */
    Exception getException(Class<?> clazz);

    /**
     * Get flow execution Instance ID
     */
    String getRuntimeId();

    /**
     * Stop flow execution instance
     */
    void stopProcess();

    /**
     * Rollback flow execution instance
     */
    void rollbackProcess();

    /**
     * Get flow executing instance node ID or alias
     */
    String getNodeIdOrAlias();

    /**
     * Get the execution result of any node by node ID
     */
    String getRunId(String nodeId);

    /**
     * Get the execution result of the last node, which may return null
     */
    List<String> getPreRunIds();
}
