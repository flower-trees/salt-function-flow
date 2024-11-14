### Salt Function Flow

Salt Function Flow is a Spring Boot-based, ultra-lightweight, in-memory flow orchestration component that uses functional programming to perform node orchestration.

### Quick Start

Includes implementation of general-purpose function nodes, flow orchestration, and flow execution.

#### Maven Dependency

```xml
<dependency>
    <groupId>cn.fenglingsoftware</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.1.0</version>
</dependency>
```

#### Implementing Function Nodes

Extend the `FlowNode` class, implement the `doProcess` method, and annotate with `@NodeIdentity`. Below are four basic function nodes that perform addition, subtraction, multiplication, and division:

##### AddNode: Adds 123 to the previous node's return value and returns it:

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

##### ReduceNode: Subtracts 15 from the previous node's return value and returns it:

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

##### MultiplyNode: Multiplies the previous node's return value by 73 and returns it:

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

##### DivisionNode: Divides the previous node's return value by 12 and returns it:

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

### Orchestrating and Registering Flow Nodes

Inject `FlowEngine` and register a flow with ID "demo_flow". Use functional programming to orchestrate nodes for sequential execution:

```java
@Autowired
FlowEngine flowEngine;

// Sequential node execution
flowEngine.builder().id("demo_flow")
        .next(AddNode.class)
        .next(ReduceNode.class)
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```

### Executing a Flow

Inject `FlowEngine` and execute the "demo_flow" flow with an input parameter of 39:

```java
@Autowired
FlowEngine flowEngine;

Integer result = flowEngine.execute("demo_flow", 39);
System.out.println("demo_flow result: " + result);
```

#### Execution Result

```
AddNode: 39 + 123 = 162
ReduceNode: 162 - 15 = 147
MultiplyNode: 147 * 73 = 10731
DivisionNode: 10731 / 12 = 894
demo_flow result: 894
```

### Complex Gateway Orchestration

#### Exclusive Execution

Execute either `ReduceNode` or `MultiplyNode` based on the flow parameter:

```java
flowEngine.builder().id("demo_flow_exclusive")
        .next(AddNode.class)
        .next(
                Info.builder().include("param <= 30").node(ReduceNode.class).build(),
                Info.builder().include("param > 30").node(MultiplyNode.class).build()
        )
        .next(DivisionNode.class)
        .build();
```

#### Concurrent Execution

Execute `ReduceNode` and `MultiplyNode` in parallel (asynchronously), and sum their results:

```java
flowEngine.builder().id("demo_flow_concurrent")
        .next(AddNode.class)
        .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .build();

// Result summation handler
class AddResult implements IResult<Integer> {
    @Override
    public Integer handle(IContextBus iContextBus, boolean isTimeout) {
        return iContextBus.getPassResult(ReduceNode.class) + iContextBus.getPassResult(MultiplyNode.class);
    }
}
```

### Asynchronous Execution

Asynchronously execute `ReduceNode`, synchronously execute `MultiplyNode`, and sum the results:

```java
flowEngine.builder().id("demo_flow_future")
        .next(AddNode.class)
        .future(ReduceNode.class) // Asynchronous execution
        .next(MultiplyNode.class)
        .wait(new AddResult(), ReduceNode.class) // Wait and merge results
        .next(DivisionNode.class)
        .build();
```

### Notification Execution

Asynchronously notify the execution of `ReduceNode`, which will not affect the final result:

```java
flowEngine.builder().id("demo_flow_notify")
        .next(AddNode.class)
        .notify(ReduceNode.class) // Asynchronous notification
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```

### Inclusive Execution

Execute `ReduceNode` and `MultiplyNode` simultaneously if their conditions are satisfied:

```java
flowEngine.builder()
        .id("demo_flow_inclusive")
        .next(AddNode.class)
        .all(
                Info.builder().include("param > 30").node(ReduceNode.class).build(),
                Info.builder().include("param < 50").node(MultiplyNode.class).build()
        )
        .next(DivisionNode.class).build();
```

### Loop Execution

Loop the execution of `ReduceNode` and `MultiplyNode` until the result is less than 56,000,000:

```java
flowEngine.builder().id("demo_flow_loop")
        .next(AddNode.class)
        .loop(
                iContextBus -> (Integer) iContextBus.getPreResult() < 56000000,
                ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```

### Data Passing

The flow execution instance can pass context information via the `IContextBus` interface:

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

#### `IContextBus` Interface

```java
public interface IContextBus<T, R> {
    // Get flow execution parameter
    T getParam();

    // Get flow execution result
    R getResult();

    // Set flow execution result
    void setResult(R result);

    // Add additional context information for transmission
    <P> void putTransmitInfo(String key, P content);

    // Get additional context information for transmission
    <P> P getTransmitInfo(String key);

    // Get condition parameters for node evaluation
    Map<String, Object> getConditionMap();
    
    // Dynamically add condition parameters for nodes
    <P> void addCondition(String key, P value);

    // Get the last node's execution result, may return null, thread-isolated
    <P> P getPreResult();

    // Get any node's execution result by ID
    <P> P getPassResult(String nodeId);

    // Get any node's exception information by ID
    Exception getPassException(String nodeId);

    // Get flow ID
    String getFlowId();

    // Get flow execution instance ID
    String getRuntimeId();

    // Stop the flow
    void stopProcess();

    // Rollback the flow
    void rollbackProcess();
}
```

### Sub-Flow Support

#### Building Sub-Flows

```java
flowEngine.builder().id("demo_branch_reduce").next("demo_reduce").result("demo_remainder").build();
flowEngine.builder().id("demo_branch_multiply").next("demo_multiply").result("demo_remainder").build();
```

#### Exclusive Execution

Similar to regular node orchestration:

```java
flowEngine.builder().id("demo_branch_exclusive")
        .next("demo_add")
        .next(
                Info.builder().include("param <= 40").id("demo_branch_reduce").build(),
                Info.builder().include("param > 40").id("demo_branch_multiply").build()
        )
        .result("demo_division")
        .build();
```

#### Concurrent Execution

```java
flowEngine.builder().id("demo_branch_concurrent")
        .next("demo_add")
        .concurrent(new AddBranchResult(), "demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .build();
```

#### Asynchronous Execution

```java
flowEngine.builder().id("demo_branch_future")
        .next("demo_add")
        .future("demo_branch_reduce")
        .next("demo_branch_multiply")
        .wait(new AddBranchResult(), "demo_branch_reduce")
        .result("demo_division").build();
```

#### Notification Execution

```java
flowEngine.builder().id("demo_branch_notify")
        .next("demo_add")
        .notify("demo_branch_reduce")
        .next("demo_branch_multiply")
        .result("demo_division").build();
```

#### Inclusive Execution

```java
flowEngine.builder().id("demo_branch")
        .next("demo_add")
        .all("demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .build();
```

#### Loop Execution

```java
flowEngine.builder().id("demo_branch")
        .next("demo_add")
        .loop(
                iContextBus -> (Integer) iContextBus.getPreResult() < 56000000,
                "demo_branch_reduce", "demo_branch_multiply"
        )
        .result("demo_division")
        .build();
```

### Nested Execution

```java
flowEngine.builder().id("demo_branch_nested")
        .next("demo_add")
        .all(
                flowEngine.builder().id("nested_1").next("demo_reduce").result("demo_remainder").build(),
                flowEngine.builder().id("nested_2").next("demo_multiply").result("demo_remainder").build())
        .result("demo_division")
        .build();
```

### Anonymous Nested Execution

```java
flowEngine.builder().id("demo_branch_anonymous")
                .next("demo_add")
                .all(
                        flowEngine.branch().next("demo_reduce").result("demo_remainder").build(),
                        flowEngine.branch().next("demo_multiply").result("demo_remainder").build())
                .result("demo_division")
                .build();
```

### Condition Evaluation

#### Rule Script Evaluation

Use rule scripts for evaluation. For example: issue a child ticket for age < 14 and an adult ticket for age >= 14:

```java
flowEngine.builder().id("train_ticket")
        .next("base_price")
        .next(
                Info.builder().include("age < 14").id("child_ticket").build(),
                Info.builder().include("age >= 14").id("adult_ticket").build())
        .result("ticket_result")
        .build();
```

**Execution:**

```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```

#### Embedded Function Evaluation

Use embedded functions for condition evaluation:

```java
flowEngine.builder().id("train_ticket_1")
       .next("base_price")
       .next(
               Info.builder().match(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() < 14).id("child_ticket").build(),
               Info.builder().match(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() >= 14).id("adult_ticket").build())
       .result("ticket_result")
       .build();
```

#### Custom Condition Parameters

Custom condition parameters can be passed in during execution:

```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
// Custom condition parameters
Map<String, Object> condition = new HashMap();
condition.put("sex", "man");

Ticket ticket = flowEngine.execute("train_ticket", passenger, condition);
```

You can also dynamically add conditions in the context:

```java
icontextBus.addCondition("sex", "man");
```

### Parameter and Return Value Adapters

Nodes can have fixed input and return value types, and adapt functions can be embedded during orchestration.

#### Example: TrainBasePriceStation Node

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

When passing a `Passenger` as a parameter, convert it to a `Station` to be passed to the `TrainBasePriceStation` node:

```java
flowEngine.builder().id("train_ticket_input")
        .next(
                Info.builder().node(TrainBasePriceStation.class)
                        .input(iContextBus -> {
                            Passenger passenger = iContextBus.getParam();
                            return Station.builder().from(passenger.getFrom()).to(passenger.getTo()).build();
                        })
                        .output((iContextBus, result) -> {
                            System.out.println("base_price return " + result);
                            return result;
                        }).build())
        .next(
                Info.builder().include("age < 14").node(TrainChildTicket.class).build(),
                Info.builder().include("age >= 14").node(TrainAdultTicket.class).build())
        .next(TrainTicketResult.class)
        .build();
```

**Execution:**

```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```

### Flow Termination

#### Normal Termination

Use the `stopProcess` method of the `IContextBus` interface to stop the entire flow:

```java
@NodeIdentity
public class BitAndNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("BitAndNode: stop flow");
            getContextBus().stopProcess();
        } else {
            Integer result = num & 256;
            System.out.println("BitAndNode: " + num + "&256=" + result);
            return result;
        }
        return null;
    }
}
```

#### Exception Termination

Throw an exception in a node to terminate the flow, but this only works for synchronous nodes:

```java
@NodeIdentity
public class BitOrNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("BitOrNode: throw exception");
            throw new RuntimeException("BitOrNode Exception!");
        } else {
            Integer result = num | 128;
            System.out.println("BitOrNode: " + num + "|128=" + result);
            return result;
        }
    }
}
```

#### Flow Rollback

Use the `rollbackProcess` method of the `IContextBus` interface to actively rollback the entire flow. This will trigger the rollback method of each executed node in reverse order. The `rollback` method is empty by default, but nodes can implement it selectively as needed:

```java
@NodeIdentity
public class BitXorNode extends FlowNode<Integer, Integer> {

    @Override
    public Integer doProcess(Integer num) {
        if (num > 500) {
            System.out.println("BitXorNode: rollback flow");
            getContextBus().rollbackProcess();
        } else {
            Integer result = num ^ 128;
            System.out.println("BitXorNode: " + num + "^128=" + result);
            return result;
        }
        return null;
    }

    @Override
    public void rollback() {
        System.out.println("BitXorNode: rollback execute");
    }
}
```

### Thread Configuration

#### Timeout

For concurrent or wait asynchronous execution, you can set a maximum wait time (in milliseconds):

```java
flowEngine.builder().id("demo_flow_concurrent_timeout")
        .next("demo_add")
        .concurrent(new AddResult(), 10, "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .build();
```

The `handle` method of the parallel result handler can receive the `isTimeout` parameter to determine if a timeout occurred:

```java
private static class AddResult implements IResult<Integer> {
    @Override
    public Integer handle(IContextBus iContextBus, boolean isTimeout) {
        System.out.println("AddResult handle isTimeout: " + isTimeout);
    }
}
```

#### Thread Pool Configuration

Define thread pool configurations via the YAML file:

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

#### Custom Thread Pool

Redefine the `flowThreadPool` bean:

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

#### Parameter-Specific Thread Pool

Pass an independent thread pool via parameters:

```java
flowEngine.builder().id("demo_flow_concurrent_isolate")
        .next("demo_add")
        .concurrent(new AddResult(), Executors.newFixedThreadPool(3), "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .build();
```

#### ThreadLocal Handling

By implementing the `IThreadContent` interface, you can pass the necessary `ThreadLocal` information to the asynchronous thread:

```java
// Extend IThreadContent interface and define as a bean
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

### Dynamic Flow Construction

Use the `buildDynamic` method of the builder to dynamically construct flows. Dynamic flows are not registered and can be constructed repeatedly.

```java
int a = (new Random()).nextInt(20);
FlowEngine.Builder builder = flowEngine.builder().id("demo_flow_dynamic");
builder.next(AddNode.class);
if (a < 10) {
    builder.next(ReduceNode.class);
} else {
    builder.next(MultiplyNode.class);
}
builder.next(DivisionNode.class);
FlowInstance flowInstance = builder.buildDynamic();
Integer result = flowEngine.execute(flowInstance, 39);
```

