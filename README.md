# Salt Function Flow

Salt Function Flow is an ultra-lightweight, in-memory, SpringBoot-based flow orchestration component that uses functional programming to achieve node orchestration.

## Quick Start
Includes implementation of general function nodes, flow orchestration, and flow execution.

### Maven
```xml
<dependency>
    <groupId>cn.fenglingsoftware</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Implementing Flow Function Nodes
Inherit from the `FlowNode` class, implement the `doProcess` method, and declare with `@NodeIdentity`. Below are four basic functional nodes (beans) for addition, subtraction, multiplication, and division:

- Gets the return value from the previous node, adds 123, and returns it:
```java
@NodeIdentity
public class AddNode extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer num) {
        Integer result = num + 123;
        System.out.println("Add: " + num + "+123=" + result);
        return result;
    }
}
```
- Gets the return value from the previous node, subtracts 15, and returns it:
```java
@NodeIdentity
public class ReduceNode extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer num) {
        Integer result = num - 15;
        System.out.println("Reduce: " + num + "-15=" + result);
        return result;
    }
}
```
- Gets the return value from the previous node, multiplies by 73, and returns it:
```java
@NodeIdentity
public class MultiplyNode extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer num) {
        Integer result = num * 73;
        System.out.println("Multiply: " + num + "*73=" + result);
        return result;
    }
}
```
- Gets the return value from the previous node, divides by 12, and returns it:
```java
@NodeIdentity
public class DivisionNode extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer num) {
        Integer result = num / 12;
        System.out.println("Division: " + num + "/12=" + result);
        return result;
    }
}
```

### Orchestrating & Executing Flows
Inject `FlowEngine` to use functional programming for orchestrating nodes in sequence:
![sequence](https://img-blog.csdnimg.cn/eb3598ae4db145858e7e47a190235af6.png)
```java
@Autowired
FlowEngine flowEngine;

......

FlowInstance flowInstance = flowEngine.builder()
                .next(AddNode.class)
                .next(ReduceNode.class)
                .next(MultiplyNode.class)
                .next(DivisionNode.class)
                .build(); // Build flow instance

Integer result = flowEngine.execute(flowInstance, 39);
System.out.println("demo_flow result: " + result);
```

Execution Result:
```
Add: 39+123=162
Reduce: 162-15=147
Multiply: 147*73=10731
Division: 10731/12=894
demo_flow result: 894
```

## Registering Flows
To avoid duplicate creation or conflicts, register flows first before executing.

### Register
```java
@Autowired
FlowEngine flowEngine;

flowEngine.builder().id("demo_flow") //set flow id
        .next(AddNode.class)
        .next(ReduceNode.class)
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .register(); //Register flow instance
```

### Execution
```java
@Autowired
FlowEngine flowEngine;

//exe flow instance by id
Integer result = flowEngine.execute("demo_flow", 39);
System.out.println("demo_flow result: " + result);
```

## Complex Gateway Orchestration
### Exclusive Execution
![image](https://img-blog.csdnimg.cn/585b8101369546fbb750e5ba6d0b85d1.png)
Execute `ReduceNode` or `MultiplyNode` based on input parameters:
```java
flowEngine.builder().id("demo_flow_exclusive")
        .next(AddNode.class)
        .next(
                Info.c("param <= 30", ReduceNode.class),
                Info.c("param > 30", MultiplyNode.class)
        )
        .next(DivisionNode.class)
        .register();
```

### Parallel Execution
![image](https://img-blog.csdnimg.cn/59d92327b03e4f56a1488e093129895f.png)
Execute `ReduceNode` and `MultiplyNode` concurrently (asynchronously) and sum their results.
```java
flowEngine.builder().id("demo_flow_concurrent")
        .next(AddNode.class)
        .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .register();
```
```java
class AddResult implements IResult<Integer> {
    @Override
    public Integer handle(IContextBus iContextBus, boolean isTimeout) {
        return iContextBus.getPassResult(ReduceNode.class) + iContextBus.getPassResult(ReduceNode.class);
    }
}
```

### Asynchronous Execution
![image](https://img-blog.csdnimg.cn/ce1d5d54fc3a48d18be054bbb2c114ff.png)
Execute `ReduceNode` asynchronously and `MultiplyNode` synchronously, then sum the results.
```java
flowEngine.builder().id("demo_flow_future")
        .next(AddNode.class)
        .future(ReduceNode.class) // Asynchronous execution
        .next(MultiplyNode.class) 
        .wait(new AddResult(), ReduceNode.class) // Wait and merge results
        .next(DivisionNode.class)
        .register();
```

### Notify Execution
![image](https://img-blog.csdnimg.cn/fe9c502ada5449fb8e5884c011041f89.png)
Execute `ReduceNode` asynchronously as a notification, which does not affect the final result.
```java
flowEngine.builder().id("demo_flow_notify")
        .next(AddNode.class)
        .notify(ReduceNode.class) // Asynchronous notify execution
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .register();
```

### Inclusive Execution
![image](https://img-blog.csdnimg.cn/9e28c6b39a134cba9fc889d9c247382d.png)
Execute both `ReduceNode` and `MultiplyNode` if conditions are met.
```java
flowEngine.builder().id("demo_flow_inclusive")
        .next(AddNode.class)
        .all(
                Info.c("param > 30",ReduceNode.class),
                Info.c("param < 50", MultiplyNode.class)
        )
        .next(DivisionNode.class)
        .register();
```

### Loop Execution
Loop through `ReduceNode` and `MultiplyNode` until the result is less than 56,000,000.
```java
flowEngine.builder().id("demo_flow_loop")
        .next(AddNode.class)
        .loop(
                iContextBus -> (Integer) iContextBus.getPreResult() < 56000000, 
                ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .register();
```

## Data Transfer
Context information is passed using the `IContextBus` interface:
```java
@NodeIdentity
public class ResultNode extends FlowNode<Integer, Void> {

    @Override
    public Integer doProcess(Void v) {
        IContextBus iContextBus = getContextBus();
        Integer pResult = iContextBus.getPassResult(AddNode.class);
        System.out.println("Add result: " + pResult);
        return pResult;
    }
}
```
The specific interface is as follows:
```java
public interface IContextBus<T, R> {

    // Get the flow execution parameter
    T getParam();

    // Get the flow execution result
    R getResult();

    // Set the flow execution result
    void setResult(R result);

    // Add additional transmission context information
    <P> void putTransmitInfo(String key, P content);

    // Get additional transmission context information
    <P> P getTransmitInfo(String key);

    // Get the parameters for node condition judgment
    Map<String, Object> getConditionMap();
    
    // Dynamically add parameters for node condition judgment
    <P> void addCondition(String key, P value);

   	// Get the result of the last node, which may return null; thread-isolated
    <P> P getPreResult();

    // Get the result of any node by ID
    <P> P getPassResult(String nodeId);

    // Get the exception information of any node by ID
    Exception getPassException(String nodeId);

    // Get the flow ID
    String getFlowId();

    // Get the flow execution instance ID
    String getRuntimeId();

    // Stop the flow
    void stopProcess();

    // Rollback the flow
    void rollbackProcess();
}
```

## Sub-flow Support
Construct sub-flows:
```java
flowEngine.builder().id("demo_branch_reduce").next("demo_reduce").result("demo_remainder").build();
flowEngine.builder().id("demo_branch_multiply").next("demo_multiply").result("demo_remainder").build();
```

### Exclusive Execution
Similar to node orchestration.
```java
flowEngine.builder().id("demo_branch_exclusive")
        .next("demo_add")
        .next(
                Info.c("param <= 30", "demo_branch_reduce"),
                Info.c("param > 30", "demo_branch_multiply")
        )
        .result("demo_division")
        .register();
```

### Parallel Execution
```java
flowEngine.builder().id("demo_branch_concurrent")
        .next("demo_add")
        .concurrent(new AddBranchResult(), "demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .register();
```

### Asynchronous Execution
```java
flowEngine.builder().id("demo_branch_future")
        .next("demo_add")
        .future("demo_branch_reduce")
        .next("demo_branch_multiply")
        .wait(new AddBranchResult(), "demo_branch_reduce")
        .result("demo_division").build();
```

### Notify Execution
```java
flowEngine.builder().id("demo_branch_notify")
        .next("demo_add")
        .notify("demo_branch_reduce")
        .next("demo_branch_multiply")
        .result("demo_division").build();
```

### Inclusive Execution
```java
flowEngine.builder().id("demo_branch")
        .next("demo_add")
        .all("demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .register();
```

### Loop Execution
```java
flowEngine.builder().id("demo_branch")
        .next("demo_add")
        .loop(
                iContextBus -> (Integer) iContextBus.getPreResult() < 56000000,
                "demo_branch_reduce", "demo_branch_multiply"
        )
        .result("demo_division")
        .register();
```

### Nested Execution
```java
flowEngine.builder().id("demo_branch_nested")
        .next("demo_add")
        .all(
                flowEngine.builder().id("nested_1").next("demo_reduce").result("demo_remainder").build(),
                flowEngine.builder().id("nested_2").next("demo_multiply").result("demo_remainder").build())
        .result("demo_division")
        .register();
```

### Anonymous Nested Execution
```java
flowEngine.builder().id("demo_branch_anonymous")
        .next("demo_add")
        .all(
                flowEngine.builder().next("demo_reduce").result("demo_remainder").build(),
                flowEngine.builder().next("demo_multiply").result("demo_remainder").build())
        .result("demo_division")
        .register();
```

## Conditional Judgments
### Rule-Based Script Judgments
You can use rule-based scripts for condition evaluation. For example, issuing a child ticket if the age is less than 14, or an adult ticket if the age is 14 or older:
```java
flowEngine.builder().id("train_ticket")
        .next("base_price")
        .next(
                Info.c("age < 14", TrainChildTicket.class),
                Info.c("age >= 14", TrainAdultTicket.class)
        .result("ticket_result")
        .register();
```
Execution:
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```

### Embedded Function Judgments
You can use embedded functions for conditional evaluation:
```java
flowEngine.builder().id("train_ticket_match")
        .next(TrainBasePrice.class)
        .next(
                Info.c(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() < 14, TrainChildTicket.class),
                Info.c(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() >= 14, TrainAdultTicket.class))
        .next(TrainTicketResult.class)
        .register();
```

### Custom Condition Parameters
You can extend conditional parameters by customizing the input:
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();

// Custom condition parameters
Map<String, Object> condition = new HashMap();
condition.put("sex", "man");

Ticket ticket = flowEngine.execute("train_ticket", passenger, condition);
```
Or dynamically add them within the context:
```java
icontextBus.addCondition("sex", "man");
```

## Node Input and Output Adaptation
Nodes can have fixed input and output types. During orchestration, you can embed functions to adapt inputs and outputs.

```java
@NodeIdentity
public class TrainBasePriceStation extends FlowNode<Integer, Station> {

    @Override
    public Integer doProcess(Station station) {
        if (station != null) {
            System.out.println("Passengers travel from " + station.getFrom() + " to " + station.getTo());
        }
        System.out.println("Calculate the basic train ticket price 300");
        return 300;
    }
}
```

In this flow, the input parameter is `Passenger`, which is converted to `Station` and passed to the `TrainBasePriceStation` node:
```java
flowEngine.builder().id("train_ticket_input")
        .next(
                Info.c(TrainBasePriceStation.class)
                        .cInput(iContextBus -> {
                            Passenger passenger = iContextBus.getParam();
                            return Station.builder().from(passenger.getFrom()).to(passenger.getTo()).build();
                        })
                        .cOutput((iContextBus, result) -> {
                            System.out.println("base_price return " + result);
                            return result;
                        }))
        .next(
                Info.c("age < 14", TrainChildTicket.class),
                Info.c("age >= 14", TrainAdultTicket.class)
        )
        .next(TrainTicketResult.class)
        .register();
```

Execution:
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```

## Process Termination
### Normal Termination
Terminate the entire process using the `

stopProcess` method in the `IContextBus` interface.
```java
@NodeIdentity
public class BitAndNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("DemoBitAndNode: stop flow");
            getContextBus().stopProcess();
        } else {
            Integer result = num & 256;
            System.out.println("DemoBitAndNode: " + num + "&256=" + result);
            return result;
        }
        return null;
    }
}
```

### Exception Termination
You can terminate the process by throwing an exception within a node, which only applies to synchronously executed nodes.
```java
@NodeIdentity
public class BitOrNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("DemoBitOrNode: throw exception");
            throw new RuntimeException("DemoBitOrNode Exception!");
        } else {
            Integer result = num | 128;
            System.out.println("DemoBitOrNode: " + num + "|128=" + result);
            return result;
        }
    }
}
```

### Process Rollback
Use the `rollbackProcess` method in the `IContextBus` interface to manually rollback the entire process. This will trigger the `rollback` method of each node in reverse order. By default, `rollback` is an empty method, which can be selectively implemented by nodes as needed.
```java
@NodeIdentity
public class BitXorNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("DemoBitOrNode: rollback flow");
            getContextBus().rollbackProcess();
        } else {
            Integer result = num | 128;
            System.out.println("DemoBitOrNode: " + num + "|128=" + result);
            return result;
        }
        return null;
    }

    @Override
    public void rollback() {
        System.out.println("DemoBitOrNode: rollback execute");
    }
}
```

## Threads
### Timeout Settings
For concurrent and `wait` asynchronous executions, you can set a maximum wait time in milliseconds using a timeout parameter:
```java
flowEngine.builder().id("demo_flow_concurrent_timeout")
        .next("demo_add")
        .concurrent(new AddResult(), 10, "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .register();
```

The `handle` method of the parallel result processor can take `isTimeout` to determine if there was a timeout:
```java
private static class AddResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            System.out.println("AddResult handle isTimeout: " + isTimeout);
        }
}
```

### Thread Pool Configuration
Define thread pool settings in the `yml` file:
```yaml
salt:
  function:
    flow:
      threadpool:
        coreSize: 50
        maxSize: 150
        queueCapacity: 256
        keepAlive: 30
```

### Custom Thread Pool
#### Redefine the Bean
Redefine a thread pool Bean named `flowThreadPool`:
```java
@Bean
@ConditionalOnMissingBean(name = "flowThreadPool")
public ThreadPoolTaskExecutor flowThreadPool() {
    ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
    threadPoolTaskExecutor.setCorePoolSize(50);
    threadPoolTaskExecutor.setMaxPoolSize(150);
    threadPoolTaskExecutor.setQueueCapacity(256);
    threadPoolTaskExecutor.setKeepAliveSeconds(30);
    threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    threadPoolTaskExecutor.setThreadNamePrefix("thread-pool-flow");
    threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        threadPoolTaskExecutor.shutdown();
    }));
    return threadPoolTaskExecutor;
}
```

#### Passing Parameters
Use an independent thread pool by passing it as a parameter:
```java
flowEngine.builder().id("demo_flow_concurrent_isolate")
        .next("demo_add")
        .concurrent(new AddResult(), Executors.newFixedThreadPool(3), "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .register();
```

### ThreadLocal Handling
Use the `IThreadContent` interface to transfer `ThreadLocal` information to an asynchronous thread.
```java
// Extend the IThreadContent interface and define it as a bean
@Component
public class TestThreadContent implements IThreadContent {

    private static ThreadLocal<Map<String, Object>> threadLocal = new ThreadLocal<>();

    @Override
    public Object getThreadContent() {
        return threadLocal.get();
    }

    @Override
    public void setThreadContent(Object content) {
        threadLocal.set((Map<String, Object>) content);
    }

    @Override
    public void cleanThreadContent() {
        threadLocal.remove();
    }
}
```

## Dynamic Flow Construction
Use the `builder.buildDynamic` method to dynamically build flows. Dynamic flows are not registered and can be repeatedly built.
```java
int a = (new Random()).nextInt(20);
FlowEngine.Builder builder = flowEngine.builder();
builder.next(AddNode.class);
if (a < 10) {
    builder.next(ReduceNode.class);
} else {
    builder.next(MultiplyNode.class);
}
builder.next(DivisionNode.class);
FlowInstance flowInstance = builder.build();
Integer result = flowEngine.execute(flowInstance, 39);
```