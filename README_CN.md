# Salt Function Flow
Salt Function Flow是一款基于SpringBoot、内存级别、超轻量级流编排组件，它使用函数式编程来实现节点编排和路由。

## 快速开始
包括流程通用功能节点实现、流程编排、流程运行。

### Maven
```xml
<dependency>
    <groupId>io.github.flower-trees</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.1.1</version>
</dependency>
```

### 实现流程功能节点
继承FlowNode类，实现process方法，并声明@NodeIdentity，下面实现四个基本功能节点Bean，分别实现加、减、乘、除运算：

- 获取上一个节点返回值，并加123，返回：
    ```java
    @NodeIdentity
    public class AddNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num + 123;
        }
    }
    ```
- 获取上一个节点返回值，并减15，返回：
    ```java
    @NodeIdentity
    public class ReduceNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num - 15;
        }
    }
    ```
- 获取上一个节点返回值，并乘73，返回：
    ```java
    @NodeIdentity
    public class MultiplyNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num * 73;
        }
    }
    ```
- 获取上一个节点返回值，并除12，返回：
    ```java
    @NodeIdentity
    public class DivisionNode extends FlowNode<Integer, Integer> {
        @Override
        public Integer process(Integer num) {
            return num / 12;
        }
    }
    ```
💡 **注：** 
- `@NodeIdentity` 默认使用class名称作为节点标识，也可以自定义标识，如：`@NodeIdentity("add_node")`。
- `FlowNode<O, I>` 中O、I分别代表节点需要的的入参和返回值，如果为空可使用 `Void` 类型占位符。

### 编排&执行流程
注入FlowEngine，使用函数式编排节点，顺序执行：

- 编排
  ```java
  @Autowired
  FlowEngine flowEngine;
  
  ......
  
  FlowInstance flowInstance = flowEngine.builder()
                  .next(AddNode.class)
                  .next(ReduceNode.class)
                  .next(MultiplyNode.class)
                  .next(DivisionNode.class)
                  .build(); //构建流程实例
  
  Integer result = flowEngine.execute(flowInstance, 39);
  System.out.println("demo_flow result: " + result);
  ```

- 执行结果

  ```
  demo_flow result: 894
  ```

💡 **注：**
- 通过`.next()` 函数编排流程，函数默认传入节点类型 `AddNode.class` ，也可传入自定义标识，如：`next("add_node")`。
- 通过`.build()` 构建流程实例，通过 `.execute()` 执行流程实例。
- 通过`.execute()` 执行传入参数 `39`，并作为第一个节点的执行入参。
- 流程执行时，默认上一个节点的返回值作为下一个节点的入参，如不符合节点要求，可进行适配，详见下面章节。
- `.next()` 函数也可以直接传入动态匿名类，如：
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

## 注册流程
如果是可复用流程，可注册全局流程，在具体业务中通过ID执行。
- 注册
  ```java
  flowEngine.builder().id("demo_flow") //设置流程ID
          .next(AddNode.class)
          .next(ReduceNode.class)
          .next(MultiplyNode.class)
          .next(DivisionNode.class)
          .register(); //注册流程实例
  ```

- 执行
  ```java
  Integer result = flowEngine.execute("demo_flow", 39);
  ```

💡 **注：**
- 通过`.id()`指定流程全局ID。
- 通过`.register()` 注册流程实例，一个流程ID只能注册一次。

## 条件判断

通过在节点前添加条件，来控制节点执行，有两种形式添加条件：

### 1.规则脚本判断
可以使用规则脚本来判断，如：年龄<14岁签发儿童票，年龄>=14岁签发成人票:
- 编排
  ```java
  FlowInstance flow = flowEngine.builder()
          .next(TrainBasePrice.class)
          .next(
                  Info.c("age < 14", TrainChildTicket.class),
                  Info.c("age >= 14",TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```
- 执行
  ```java
  Passenger passenger = Passenger.builder().name("jack").age(12).build();
  Ticket ticket = flowEngine.execute(flow, passenger);
  ```

### 2.嵌入函数判断
可以使用嵌入函数进行条件判断:
- 编排
  ```java
  flowEngine.builder()
          .next(TrainBasePrice.class)
          .next(
                  Info.c(iContextBus -> ((Passenger) iContextBus.getFlowParam()).getAge() < 14, TrainChildTicket.class),
                  Info.c(iContextBus -> ((Passenger) iContextBus.getFlowParam()).getAge() >= 14, TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```

💡 **注：**
- `next()` 多参数传递需要使用`Info.c()`函数。
- 流程通过 `iContextBus` 传递上下文信息，包括流程入参、条件判断参数、节点返回值等，详见下面章节。

### 条件参数
程序默认使用流程入参作为条件判断参数，也可以增加其他条件参数传入：

- 调用时增加条件参数：
  ```java
  Map<String, Object> condition = new HashMap();
  condition.put("sex", "man");
  
  Ticket ticket = flowEngine.execute(flow, passenger, condition);
  ```
- 节点中动态添加条件参数
  ```java
  iContextBus.addCondition("sex", "man");
  ```

## 节点入参和返回值适配
节点需要有固定的入参和返回值类型，为增加通用性，如果不符合要求，可在编排时，嵌入函数转换和适配。
- 节点 `TrainBasePrice` 入参改为 `Station`
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
- 流程传入参数为Passenger，转成Station传入TrainBasePrice节点
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
                  Info.c("age >= 14",TrainAdultTicket.class))
          .next(TrainTicketResult.class)
          .build();
  ```
执行：
```java
Passenger passenger = Passenger.builder().name("jack").age(12).from("Beijing").to("Shanghai").build();
Ticket ticket = flowEngine.execute(flow, passenger);
```

💡 **注：**
- 通过`Info.cInput()`指定节点入参转换函数，`Info.cOutput()`指定节点返回值转换函数。

## 数据传递
流程执行实例通过IContextBus接口传递上下文信息，通过`getContextBus()`获取。
- 获取 `IContextBus`，并获取响应及节点的结果值：
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
- `IContextBus` 具体接口功能：
  ```java
  public interface IContextBus {

    // 获取流程执行参数
    <T> T getFlowParam();

    // 获取流程执行结果
    <R> R getFlowResult();

    // 存储额外的传输上下文信息
    <P> void putTransmit(String key, P content);

    // 获取额外的传输上下文信息
    <P> P getTransmit(String key);

    // 添加节点条件判断的参数
    <P> void addCondition(String key, P value);

    // 获取上一个节点的执行结果，可能返回 null
    <P> P getPreResult();

    // 通过节点ID获取任意节点的执行结果
    <P> P getResult(String nodeId);

    // 通过节点类获取任意节点的执行结果
    <P> P getResult(Class<?> clazz);

    // 通过节点ID获取任意节点的执行异常
    Exception getException(String nodeId);

    // 通过节点类获取任意节点的执行异常
    Exception getException(Class<?> clazz);

    // 获取流程执行实例ID
    String getRuntimeId();

    // 停止流程执行实例
    void stopProcess();

    // 回滚流程执行实例
    void rollbackProcess();
  }
  ```

## 复杂网关编排
### 排他执行
- 根据流程入参执行 ReduceNode 或 MultiplyNode 节点其中一个:
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
### 并行执行
- 并行（异步并发）执行 ReduceNode、MultiplyNode节点，并结果相加，作为下一个节点的入参:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();

  class AddResult implements IResult<Integer> {
      @Override
      public Integer handle(IContextBus iContextBus, boolean isTimeout) {
          return iContextBus.getResult(ReduceNode.class) + iContextBus.getResult(ReduceNode.class);
      }
  }
  ```
### 异步执行
- 异步执行ReduceNode节点，同步执行MultiplyNode，并结果相加:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .future(ReduceNode.class) //异步执行
          .next(MultiplyNode.class) 
          .wait(new AddResult(), ReduceNode.class) //等待，合并结果
          .next(DivisionNode.class)
          .build();
  ```
### 通知执行
- 异步通知执行ReduceNode，ReduceNode将不影响最终结果:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .notify(ReduceNode.class) //异步通知执行
          .next(MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```
### 相容执行
- 同步相容执行ReduceNode、MultiplyNode，满足条件都会执行:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .all(
                  Info.c("param > 30",ReduceNode.class),
                  Info.c("param < 50", MultiplyNode.class)
          )
          .next(DivisionNode.class)
          .build();
  ```
### 循环执行
- 循环执行ReduceNode、MultiplyNode，直到结果小于56000000:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .loop(
                  (iContextBus, i) -> (Integer) iContextBus.getPreResult() < 56000000, 
                  ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```

## 子流程支持
基本与同节点编排相同。
### 排他执行
- 根据流程入参执行两个子流程其中一个:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .next(
                  Info.c("param <= 30", flowEngine.builder().next("demo_reduce").next("demo_remainder").build()),
                  Info.c("param > 30", flowEngine.builder().next("demo_multiply").next("demo_remainder").build()))
          .next(DivisionNode.class)
          .build();
  ```
### 并行执行
- 并行（异步并发）执行两个子流程，并结果相加，作为下一个节点的入参:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddBranchResult(), 
                      flowEngine.builder().next("demo_reduce").next("demo_remainder").build(),
                      flowEngine.builder().next("demo_multiply").next("demo_remainder").build())
          .next(DivisionNode.class)
          .build();
  ```
### 异步执行
- 异步执行其中一个子流程，并结果相加:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .future(flowEngine.builder().id("demo_branch_reduce").next(ReduceNode.class).next(RemainderNode.class).build())
          .next(flowEngine.builder().next(MultiplyNode.class).next(RemainderNode.class).build())
          .wait(new AddBranchResult(), "demo_branch_reduce")
          .next(DivisionNode.class)
          .build();
  ```
### 通知执行
- 异步通知执行其中一个子流程，不影响最终结果:
```java
  flowEngine.builder()
          .next(AddNode.class)
          .notify(flowEngine.builder().next("demo_reduce").next("demo_remainder").build())
          .next(flowEngine.builder().next("demo_multiply").next("demo_remainder").build())
          .next(DivisionNode.class)
          .build();
  ```
### 相容执行
- 同步相容执行两个子流程，满足条件都会执行:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .all(
                  flowEngine.builder().next("demo_reduce").next("demo_remainder").build(),
                  flowEngine.builder().next("demo_multiply").next("demo_remainder").build())
          .next(DivisionNode.class)
          .build();
  ```
### 循环执行
- 循环执行两个子流程，直到结果小于56000000:
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

## 流程终止
### 正常终止
- 通过IContextBus接口的stopProcess，停止整个流程：
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
### 异常终止
- 通过节点中抛出异常，终止流程，但仅限同步执行节点：
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
### 回滚流程
- 通过IContextBus接口的rollbackProcess方法，主动回滚整个流程，会按逆序触发已执行节点rollback方法，rollback默认为空方法，节点可根据需要选择性实现。
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
## 线程
### 超时时间
- concurrent()、wait()异步执行函数，可通过超时参数，设置最大等待时间，单位为毫秒，默认为3000毫秒:
  ```java
  flowEngine.builder()
          .next(AddNode.class)
          .concurrent(new AddResult(), 10, ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```
- 并行结果处理handle()可接收isTimeout，判断是否有超时:
  ```java
  private static class AddResult implements IResult<Integer> {
          @Override
          public Integer handle(IContextBus iContextBus, boolean isTimeout) {
              System.out.println("AddResult handle isTimeout: " + isTimeout);
          }
  }
  ```
### 线程池配置
- 通过yml文件定义线程池配置
  ```
  salt:
    function:
      flow:
        threadpool:
          coreSize: 50
          maxSize: 150
          queueCapacity: 256
          keepAlive: 30
  ```
### 自定义线程池
#### 重新定义Bean
- 重新定义名为 `flowThreadPool` 线程池Bean:
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
#### 参数传入
- 通过参数传入独立的线程池:
  ```java
  flowEngine.builder().id("demo_flow_concurrent_isolate")
          .next(AddNode.class)
          .concurrent(new AddResult(), Executors.newFixedThreadPool(3), ReduceNode.class, MultiplyNode.class)
          .next(DivisionNode.class)
          .build();
  ```
### ThreadLocal处理
在多线程情况下，可以通过两种方式进行ThreadLocal数据继承处理：
- 在自定 `ThreadPoolTaskExecutor` 时，实现 `TaskDecorator` 接口，设置ThreadLocal。
  ```java
  @Bean
  @ConditionalOnMissingBean(name = "flowThreadPool")
  public ThreadPoolTaskExecutor flowThreadPool() {
      ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
      ......
      threadPoolTaskExecutor.setTaskDecorator(runnable -> {
              Object o = UserThreadUtil.getThreadLocal();
              return () -> {
                  try {
                      UserThreadUtil.setThreadLocal(o);
                      runnable.run();
                  } finally {
                      UserThreadUtil.clean();
                  }
              };
          });
      return threadPoolTaskExecutor;
  }
  ```
- 通过 `ThreadHelper.initThreadLocal(ThreadLocal<?>... threadLocals)` 方法将用户自定义ThreadLocal，设置到框架中，通过框架处理。
  ```java
  static {
      TheadHelper.initThreadLocal(UserThreadUtil.getThreadLocal());
  }
  ```