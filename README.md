# Salt Function Flow

Salt Function Flow is a SpringBoot-based, ultra-lightweight, in-memory flow orchestration component that leverages functional programming to implement node orchestration and routing.

## Quick Start
This guide covers implementing general-purpose flow nodes, flow orchestration, and flow execution.

### Maven Dependency
```xml
<dependency>
    <groupId>io.github.flower-trees</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.1.3</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.flower-trees:salt-function-flow:1.1.3'
```

### Implementing Functional Nodes
Extend the `FlowNode` class, implement the `process` method, and annotate the class with `@NodeIdentity`. Below are examples of four basic functional nodes: addition, subtraction, multiplication, and division.

- **Addition**: Gets the output of the previous node, adds 123, and returns the result:
    ```java
    @NodeIdentity
    public class AddNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num + 123;
        }
    }
    ```
- **Subtraction**: Gets the output of the previous node, subtracts 15, and returns the result:
    ```java
    @NodeIdentity
    public class ReduceNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num - 15;
        }
    }
    ```
- **Multiplication**: Gets the output of the previous node, multiplies it by 73, and returns the result:
    ```java
    @NodeIdentity
    public class MultiplyNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num * 73;
        }
    }
    ```
- **Division**: Gets the output of the previous node, divides it by 12, and returns the result:
    ```java
    @NodeIdentity
    public class DivisionNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num / 12;
        }
    }
    ```

💡 **Notes:**
- The `@NodeIdentity` annotation defaults to using the class name as the node identifier. Custom identifiers can also be specified, e.g., `@NodeIdentity("add_node")`.
- In `FlowNode<O, I>`, `O` and `I` represent the input and output types of the node. Use `Void` as a placeholder if no input or output is needed.

### Orchestrating & Executing Flows
Inject `FlowEngine` and use functional programming to orchestrate and execute nodes sequentially:

- **Orchestration**
  ```java
  @Autowired
  FlowEngine flowEngine;
  
  FlowInstance flowInstance = flowEngine.builder()
                  .next(AddNode.class)
                  .next(ReduceNode.class)
                  .next(MultiplyNode.class)
                  .next(DivisionNode.class)
                  .build(); // Build the flow instance
  
  Integer result = flowEngine.execute(flowInstance, 39);
  System.out.println("demo_flow result: " + result);
  ```

- **Execution Result**
  ```
  demo_flow result: 894
  ```

💡 **Notes:**
- Use `.next()` to orchestrate nodes, passing the node type like `AddNode.class` or a custom identifier such as `next("add_node")`.
- Use `.build()` to construct a flow instance and `.execute()` to run the flow instance.
- The parameter passed to `.execute()` (e.g., `39`) is used as the input for the first node.
- By default, the output of the previous node is used as the input for the next node. If this is not suitable, you can adapt it (details in later sections).
- Anonymous dynamic classes can also be passed directly to `.next()`:
    ```java
    FlowInstance flowInstance = flowEngine.builder()
                    .next(new FlowNode<Integer, Integer>() {
                        @Override
                        public Integer process(Integer input) {
                            return input + 1;
                        }
                    })
                    .build();
    ```
- `.next()` also supports lambda expressions:
    ```java
    FlowInstance flowInstance = flowEngine.builder()
                   .next((num) -> num + 1)
                   .build();
    ```

## Registering Flows
For reusable flows, register a global flow that can be executed by its ID in specific scenarios.

- **Registration**
  ```java
  flowEngine.builder().id("demo_flow") // Set a global flow ID
          .next(AddNode.class)
          .next(ReduceNode.class)
          .next(MultiplyNode.class)
          .next(DivisionNode.class)
          .register(); // Register the flow instance
  ```

- **Execution**
  ```java
  Integer result = flowEngine.execute("demo_flow", 39);
  ```

💡 **Notes:**
- Use `.id()` to specify a global ID for the flow.
- Use `.register()` to register the flow instance. A flow ID can only be registered once.

---

## Conditional Judgments

Salt Function Flow allows adding conditions before nodes to control their execution. There are two ways to add conditions:

### 1. Rule-Based Script Conditions
You can use rule scripts for conditions. For example, issue a child ticket if age < 14 and an adult ticket if age >= 14:

- **Orchestration**
  ```java
  FlowInstance flow = flowEngine.builder()
          .next(TrainBasePrice.class)
          .next(
                  Info.c("age < 14", TrainChildTicket.class),
                  Info.c("age >= 14", TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```

- **Execution**
  ```java
  Passenger passenger = Passenger.builder().name("Jack").age(12).build();
  Ticket ticket = flowEngine.execute(flow, passenger);
  ```

### 2. Embedded Function Conditions
You can use embedded functions to define conditions dynamically:

- **Orchestration**
  ```java
  flowEngine.builder()
          .next(TrainBasePrice.class)
          .next(
                  Info.c(iContextBus -> ((Passenger) iContextBus.getFlowParam()).getAge() < 14, TrainChildTicket.class),
                  Info.c(iContextBus -> ((Passenger) iContextBus.getFlowParam()).getAge() >= 14, TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```

💡 **Notes:**
- Use `Info.c()` for multi-parameter passing in `next()`.
- `iContextBus` is used for context transmission during the flow, including input parameters, conditional parameters, and node results. Details are covered in later sections.

### Conditional Parameters
By default, the input parameters of the flow are used for condition evaluation. Additional parameters can also be passed:

- **Passing Additional Conditional Parameters During Execution**
  ```java
  Map<String, Object> condition = new HashMap<>();
  condition.put("sex", "male");
  
  Ticket ticket = flowEngine.execute(flow, passenger, condition);
  ```

- **Adding Conditional Parameters Dynamically Within a Node**
  ```java
  iContextBus.addCondition("sex", "male");
  ```

---

## Input and Output Adaptation for Nodes
Nodes require fixed input and output types. To enhance generality, functions can be embedded during orchestration for adaptation.

- **Adapting Input for `TrainBasePrice` Node**
  ```java
  @NodeIdentity
  public class TrainBasePrice extends FlowNode<Integer, Station> {
  
      @Override
      public Integer process(Station station) {
          System.out.println("Passengers travel from " + station.getFrom() + " to " + station.getTo());
          return 300;
      }
  }
  ```

- **Transforming Flow Input Parameter to Node-Specific Input**
  ```java
  FlowInstance flow = flowEngine.builder()
          .next(
                  Info.c(TrainBasePrice.class)
                          .cInput(iContextBus -> {
                              Passenger passenger = iContextBus.getFlowParam();
                              return Station.builder().from(passenger.getFrom()).to(passenger.getTo()).build();
                          })
                          .cOutput((iContextBus, result) -> {
                              System.out.println("base_price return " + result);
                              return result;
                          }))
          .next(
                  Info.c("age < 14", TrainChildTicket.class),
                  Info.c("age >= 14", TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```

- **Execution**
  ```java
  Passenger passenger = Passenger.builder().name("Jack").age(12).from("Beijing").to("Shanghai").build();
  Ticket ticket = flowEngine.execute(flow, passenger);
  ```

💡 **Notes:**
- Use `Info.cInput()` to specify the input transformation function.
- Use `Info.cOutput()` to specify the output transformation function.

---

## Data Transmission
The flow execution instance uses the `IContextBus` interface to transmit context information. Use `getContextBus()` to retrieve it.

- **Retrieve `IContextBus` and Node Results**
  ```java
  @NodeIdentity
  public class ResultNode extends FlowNode<Integer, Void> {
  
      @Override
      public Integer process(Void v) {
          IContextBus iContextBus = getContextBus();
          Integer pResult = iContextBus.getResult(AddNode.class);
          System.out.println("Add result: " + pResult);
          return pResult;
      }
  }
  ```

- **`IContextBus` Interface Functions**
  ```java
  public interface IContextBus {

    // Retrieve flow execution parameters
    <T> T getFlowParam();

    // Retrieve flow execution results
    <R> R getFlowResult();

    // Store additional context information
    <P> void putTransmit(String key, P content);

    // Retrieve additional context information
    <P> P getTransmit(String key);

    // Add parameters for node condition evaluation
    <P> void addCondition(String key, P value);

    // Retrieve the result of the previous node execution, which may return null
    <P> P getPreResult();

    // Retrieve the execution result of any node by its ID
    <P> P getResult(String nodeId);

    // Retrieve the execution result of any node by its class
    <P> P getResult(Class<?> clazz);

    // Retrieve exceptions for any node by its ID
    Exception getException(String nodeId);

    // Retrieve exceptions for any node by its class
    Exception getException(Class<?> clazz);

    // Retrieve the execution instance ID
    String getRuntimeId();

    // Stop the flow execution instance
    void stopProcess();

    // Rollback the flow execution instance
    void rollbackProcess();
  }
  ```
---

## Complex Gateway Orchestration

### Exclusive Execution
- Execute either the `ReduceNode` or `MultiplyNode` based on flow input parameters:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .next(
                  Info.c("param <= 30", ReduceNode.class),
                  Info.c("param > 30", MultiplyNode.class)
          )
          .next(DivisionNode.class)
          .build();
  ```

### Parallel Execution
- Execute `ReduceNode` and `MultiplyNode` concurrently (asynchronously) and sum their results as input to the next node:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();

  class AddResult implements IResult<Integer> {
      @Override
      public Integer handle(IContextBus iContextBus, boolean isTimeout) {
          return iContextBus.getResult(ReduceNode.class) + iContextBus.getResult(MultiplyNode.class);
      }
  }
  ```

### Asynchronous Execution
- Execute `ReduceNode` asynchronously and `MultiplyNode` synchronously, then combine their results:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .future(ReduceNode.class) // Asynchronous execution
          .next(MultiplyNode.class) 
          .wait(new AddResult(), ReduceNode.class) // Wait and merge results
          .next(DivisionNode.class)
          .build();
  ```

### Notification Execution
- Notify `ReduceNode` to execute asynchronously without affecting the final result:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .notify(ReduceNode.class) // Asynchronous notification
          .next(MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```

### Compatible Execution
- Synchronously execute both `ReduceNode` and `MultiplyNode` if conditions are met:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .all(
                  Info.c("param > 30", ReduceNode.class),
                  Info.c("param < 50", MultiplyNode.class)
          )
          .next(DivisionNode.class)
          .build();
  ```

### Loop Execution
- Execute `ReduceNode` and `MultiplyNode` repeatedly until the result is less than 56,000,000:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .loop(
                  (iContextBus, i) -> (Integer) iContextBus.getPreResult() < 56000000, 
                  ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```

---

## Sub-Flow Support
The orchestration of sub-flows is similar to individual nodes.

### Exclusive Execution
- Execute one of two sub-flows based on flow input parameters:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .next(
                  Info.c("param <= 30", flowEngine.builder().next("demo_reduce").next("demo_remainder").build()),
                  Info.c("param > 30", flowEngine.builder().next("demo_multiply").next("demo_remainder").build()))
          .next(DivisionNode.class)
          .build();
  ```

### Parallel Execution
- Execute two sub-flows concurrently (asynchronously) and sum their results as input to the next node:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddBranchResult(), 
                      flowEngine.builder().next("demo_reduce").next("demo_remainder").build(),
                      flowEngine.builder().next("demo_multiply").next("demo_remainder").build())
          .next(DivisionNode.class)
          .build();
  ```

### Asynchronous Execution
- Execute one sub-flow asynchronously and combine results:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .future(flowEngine.builder().id("demo_branch_reduce").next(ReduceNode.class).next(RemainderNode.class).build())
          .next(flowEngine.builder().next(MultiplyNode.class).next(RemainderNode.class).build())
          .wait(new AddBranchResult(), "demo_branch_reduce")
          .next(DivisionNode.class)
          .build();
  ```

### Notification Execution
- Notify one sub-flow to execute asynchronously without affecting the final result:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .notify(flowEngine.builder().next("demo_reduce").next("demo_remainder").build())
          .next(flowEngine.builder().next("demo_multiply").next("demo_remainder").build())
          .next(DivisionNode.class)
          .build();
  ```

### Compatible Execution
- Synchronously execute both sub-flows if conditions are met:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .all(
               Info.c("param > 30", flowEngine.builder().next("demo_reduce").next("demo_remainder").build()),
               Info.c("param < 50", flowEngine.builder().next("demo_multiply").next("demo_remainder").build()))
          .next(DivisionNode.class)
          .build();
  ```

### Loop Execution
- Execute two sub-flows repeatedly until the result is less than 56,000,000:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .loop(
                  (iContextBus, i) -> (Integer) iContextBus.getPreResult() < 56000000,
                  flowEngine.builder().next("demo_reduce").next("demo_remainder").build(),
                  flowEngine.builder().next("demo_multiply").next("demo_remainder").build()
          )
          .next(DivisionNode.class)
          .build();
  ```

---

## Flow Termination

### Normal Termination
- Use the `stopProcess` method from the `IContextBus` interface to terminate the flow:
  ```java
  @NodeIdentity
  public class BitAndNode extends FlowNode<Integer, Integer> {
  
      @Override
      public Integer process(Integer num) {
          if (num > 500) {
              getContextBus().stopProcess();
          }
          return num & 256;
      }
  }
  ```

### Exception Termination
- Throw an exception within a node to terminate the flow (only applies to synchronously executed nodes):
  ```java
  @NodeIdentity
  public class BitOrNode extends FlowNode<Integer, Integer> {
  
      @Override
      public Integer process(Integer num) {
          if (num > 500) {
              throw new RuntimeException("DemoBitOrNode Exception!");
          }
          return num | 128;
      }
  }
  ```

### Flow Rollback
- Use the `rollbackProcess` method from the `IContextBus` interface to roll back the flow. This will trigger the `rollback` method in executed nodes in reverse order. Nodes can optionally implement the `rollback` method:
  ```java
  @NodeIdentity
  public class BitXorNode extends FlowNode<Integer, Integer> {
  
      @Override
      public Integer process(Integer num) {
          if (num > 500) {
              getContextBus().rollbackProcess();
          }
          return num | 128;
      }
  
      @Override
      public void rollback() {
          System.out.println("DemoBitOrNode: rollback execute");
      }
  }
  ```

---

## Threads

### Timeout Settings
- The `concurrent()` and `wait()` asynchronous execution functions allow setting a maximum wait time in milliseconds, the default is 3000 milliseconds:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddResult(), 10, ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```
- The `handle()` method in the parallel result processor can check if a timeout occurred via the `isTimeout` flag:
  ```java
  private static class AddResult implements IResult<Integer> {
      @Override
      public Integer handle(IContextBus iContextBus, boolean isTimeout) {
          System.out.println("AddResult handle isTimeout: " + isTimeout);
      }
  }
  ```

### Thread Pool Configuration

#### Configure in YML
Define thread pool settings in the YML file:
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

##### Redefine the `flowThreadPool` Bean
- Redefine a custom thread pool named `flowThreadPool`:
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
      Runtime.getRuntime().addShutdownHook(new Thread(() -> threadPoolTaskExecutor.shutdown()));
      return threadPoolTaskExecutor;
  }
  ```

##### Use a Separate Thread Pool
- Pass an independent thread pool via parameters:
  ```java
  flowEngine.builder().id("demo_flow_concurrent_isolate")
          .next(AddNode.class)
          .concurrent(new AddResult(), Executors.newFixedThreadPool(3), ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```

---

### ThreadLocal Handling

In multithreaded scenarios, ThreadLocal data inheritance can be handled in two ways:

1. **TaskDecorator Implementation**
   - Implement the `TaskDecorator` interface in the custom `ThreadPoolTaskExecutor` to set `ThreadLocal` data:
     ```java
     @Bean
     @ConditionalOnMissingBean(name = "flowThreadPool")
     public ThreadPoolTaskExecutor flowThreadPool() {
         ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
         threadPoolTaskExecutor.setTaskDecorator(runnable -> {
             Object context = UserThreadUtil.getThreadLocal();
             return () -> {
                 try {
                     UserThreadUtil.setThreadLocal(context);
                     runnable.run();
                 } finally {
                     UserThreadUtil.clean();
                 }
             };
         });
         return threadPoolTaskExecutor;
     }
     ```

2. **ThreadHelper Integration**
   - Use the `ThreadHelper.initThreadLocal(ThreadLocal<?>... threadLocals)` method to register custom `ThreadLocal` instances for the framework to manage:
     ```java
     static {
         ThreadHelper.initThreadLocal(UserThreadUtil.getThreadLocal());
     }
     ```