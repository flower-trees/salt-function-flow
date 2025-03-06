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

package org.salt.function.flow.test.thread;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.salt.function.flow.FlowEngine;
import org.salt.function.flow.TestApplication;
import org.salt.function.flow.thread.TheadHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class)
@SpringBootConfiguration
public class ThreadTest {

    @Autowired
    FlowEngine flowEngine;

    static {
        TheadHelper.initThreadLocal(UserThreadUtil.getThreadLocal());
    }

    @Test
    public void testConcurrentTimeoutDemo() {
        System.out.println("demo_flow_concurrent_timeout test: ");
        Integer result = flowEngine.execute("demo_flow_concurrent_timeout", 39);
        System.out.println("demo_flow_concurrent_timeout result: " + result);
        Assert.assertTrue(result != null && result == 0);
    }

    @Test
    public void testFutureTimeoutDemo() {
        System.out.println("demo_flow_future_timeout test: ");
        Integer result = flowEngine.execute("demo_flow_future_timeout", 39);
        System.out.println("demo_flow_future_timeout result: " + result);
        Assert.assertTrue(result != null && result == 0);
    }

    @Test
    public void testFutureIsolateDemo() {
        System.out.println("demo_flow_concurrent_isolate test: ");
        Integer result = flowEngine.execute("demo_flow_concurrent_isolate", 39);
        System.out.println("demo_flow_concurrent_isolate result: " + result);
        Assert.assertTrue(result != null && result == 19);
    }

    @Test
    public void testConcurrentLocalhostDemo() {
        UserThreadUtil.put("test", 2);
        System.out.println("demo_flow_concurrent_threadlocal test: ");
        Integer result = flowEngine.execute("demo_flow_concurrent_threadlocal", 39);
        System.out.println("demo_flow_concurrent_threadlocal result: " + result);
        Assert.assertTrue(result != null && result == 66);
    }

    @Test
    public void testBranchConcurrentTimeoutDemo() {
        System.out.println("demo_branch_flow_concurrent_timeout test: ");
        Integer result = flowEngine.execute("demo_branch_flow_concurrent_timeout", 39);
        System.out.println("demo_branch_flow_concurrent_timeout result: " + result);
        Assert.assertTrue(result != null && result == 0);
    }

    @Test
    public void testBranchFutureTimeoutDemo() {
        UserThreadUtil.put("test", 1);
        System.out.println("demo_branch_flow_future_timeout test: ");
        Integer result = flowEngine.execute("demo_branch_flow_future_timeout", 39);
        System.out.println("demo_branch_flow_future_timeout result: " + result);
        Assert.assertTrue(result != null && (result == 6 || result == 1977));
    }

    @Test
    public void testBranchIsolateTimeoutDemo() {
        System.out.println("demo_branch_flow_concurrent_isolate test: ");
        Integer result = flowEngine.execute("demo_branch_flow_concurrent_isolate", 39);
        System.out.println("demo_branch_flow_concurrent_isolate result: " + result);
        Assert.assertTrue(result != null && result == 1977);
    }

    @Test
    public void testBranchConcurrentLocalhostDemo() {
        UserThreadUtil.put("test", 2);
        System.out.println("demo_branch_flow_concurrent_threadlocal test: ");
        Integer result = flowEngine.execute("demo_branch_flow_concurrent_threadlocal", 39);
        System.out.println("demo_branch_flow_concurrent_threadlocal result: " + result);
        Assert.assertTrue(result != null && result == 3948);
    }
}
