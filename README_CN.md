# Salt Function Flow
Salt Function Flow是一款基于SpringBoot、内存级别、超轻量级流编排组件，它使用函数式编程来实现节点编排。

## 快速开始
包括流程通用功能节点实现、流程编排、流程运行。

### Maven
```xml
<dependency>
    <groupId>cn.fenglingsoftware</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.1.0</version>
</dependency>
```

### 实现流程功能节点
继承FlowNode类，实现doProcess方法，并声明@NodeIdentity，下面实现四个基本功能节点Bean，分别实现加、减、乘、除运算：

- 获取上一个节点返回值，并加123，返回：
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
- 获取上一个节点返回值，并减15，返回：
```java
@NodeIdentity
public class ReduceNode extends FlowNode<Integer, Integer> {
    @Override
    public Integer doProcess(Integer num) {
        Integer result = num - 15;
        System.out.println("Reduce: " + num + "-15=" + result) ;
        return result;
    }
}
```
- 获取上一个节点返回值，并乘73，返回：
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
- 获取上一个节点返回值，并除12，返回：
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

### 编排注册流程节点
注入FlowEngine，注册 ID 为 demo_flow 流程，使用函数式编排节点，顺序执行：
![顺序执行](https://img-blog.csdnimg.cn/eb3598ae4db145858e7e47a190235af6.png)

```java
@Autowired
FlowEngine flowEngine;

//节点顺序执行
flowEngine.builder().id("demo_flow")
        .next(AddNode.class)
        .next(ReduceNode.class)
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```

### 执行流程
注入FlowEngine，执行流程 demo_flow，输入参数为39：
```java
@Autowired
FlowEngine flowEngine;

Integer result = flowEngine.execute("demo_flow", 39);
System.out.println("demo_flow result: " + result);
```

### 执行结果

```
DemoAddNode: 39+123=162
DemoReduceNode: 162-15=147
DemoMultiplyNode: 147*73=10731
DemoDivisionNode: 10731/12=894
demo_flow result: 894
```

## 复杂网关编排
### 排他执行
根据流程入参执行 ReduceNode 或 MultiplyNode 节点
![在这里插入图片描述](https://img-blog.csdnimg.cn/585b8101369546fbb750e5ba6d0b85d1.png)
```java
flowEngine.builder().id("demo_flow_exclusive")
        .next(AddNode.class)
        .next(
                Info.c("param <= 30", ReduceNode.class),
                Info.c("param > 30", MultiplyNode.class)
        )
        .next(DivisionNode.class)
        .build();
```
### 并行执行
并行（异步并发）执行 ReduceNode、MultiplyNode节点，并结果相加。
![在这里插入图片描述](https://img-blog.csdnimg.cn/59d92327b03e4f56a1488e093129895f.png)
```java
flowEngine.builder().id("demo_flow_concurrent")
        .next(AddNode.class)
        .concurrent(new AddResult(), ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```
```java
//结果相加handle
class AddResult implements IResult<Integer> {
    @Override
    public Integer handle(IContextBus iContextBus, boolean isTimeout) {
        return iContextBus.getPassResult(ReduceNode.class) + iContextBus.getPassResult(ReduceNode.class);
    }
}
```
### 异步执行
异步执行ReduceNode节点，同步执行MultiplyNode，并结果相加。
![在这里插入图片描述](https://img-blog.csdnimg.cn/ce1d5d54fc3a48d18be054bbb2c114ff.png)
```java
flowEngine.builder().id("demo_flow_future")
        .next(AddNode.class)
        .future(ReduceNode.class) //异步执行
        .next(MultiplyNode.class) 
        .wait(new AddResult(), ReduceNode.class) //等待，合并结果
        .next(DivisionNode.class)
        .build();
```
### 通知执行
异步通知执行ReduceNode，ReduceNode将不影响最终结果。
![在这里插入图片描述](https://img-blog.csdnimg.cn/fe9c502ada5449fb8e5884c011041f89.png)
```java
flowEngine.builder().id("demo_flow_notify")
        .next(AddNode.class)
        .notify(ReduceNode.class) //异步通知执行
        .next(MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```
### 相容执行
同步相容执行ReduceNode、MultiplyNode，满足条件都会执行。
![在这里插入图片描述](https://img-blog.csdnimg.cn/9e28c6b39a134cba9fc889d9c247382d.png)
```java
flowEngine.builder().id("demo_flow_inclusive")
        .next(AddNode.class)
        .all(
                Info.c("param > 30",ReduceNode.class),
                Info.c("param < 50", MultiplyNode.class)
        )
        .next(DivisionNode.class)
        .build();
```
### 循环执行
循环执行ReduceNode、MultiplyNode，直到结果小于56000000。
```java
flowEngine.builder().id("demo_flow_loop")
        .next(AddNode.class)
        .loop(
                iContextBus -> (Integer) iContextBus.getPreResult() < 56000000, 
                ReduceNode.class, MultiplyNode.class)
        .next(DivisionNode.class)
        .build();
```

## 数据传递
流程执行实例通过IContextBus接口传递上下文信息：
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
具体接口如下：
```
public interface IContextBus<T, R> {

    //获取流执行参数
    T getParam();

    //获取流执行结果
    R getResult();

    //设置流执行结果
    void setResult(R result);

    //添加其他传输上下文信息
    <P> void putTransmitInfo(String key, P content);

    //获取其他传输上下文信息
    <P> P getTransmitInfo(String key);

    //获取节点条件判断参数
    Map<String, Object> getConditionMap();
    
    //动态增加节点条件判断参数
    <P> void addCondition(String key, P value);

   	//获取最后一个节点的执行结果，可能返回null，线程隔离
    <P> P getPreResult();

    //通过ID获取任意节点的执行结果
    <P> P getPassResult(String nodeId);

    //通过ID获取任意节点的异常信息
    Exception getPassException(String nodeId);

    //获取流程ID
    String getFlowId();

    //获取流程执行实例ID
    String getRuntimeId();

    //停止流程
    void stopProcess();

    //回滚流程
    void rollbackProcess();
}
```

## 子流程支持
构建子流程:
```java
flowEngine.builder().id("demo_branch_reduce").next("demo_reduce").result("demo_remainder").build();
flowEngine.builder().id("demo_branch_multiply").next("demo_multiply").result("demo_remainder").build();
```
### 排他执行
基本与同节点编排相同。
```java
flowEngine.builder().id("demo_branch_exclusive")
        .next("demo_add")
        .next(
                Info.c("param <= 30", "demo_branch_reduce"),
                Info.c("param > 30", "demo_branch_multiply")
        )
        .result("demo_division")
        .build();
```
### 并行执行
```java
flowEngine.builder().id("demo_branch_concurrent")
        .next("demo_add")
        .concurrent(new AddBranchResult(), "demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .build();
```
### 异步执行
```java
flowEngine.builder().id("demo_branch_future")
        .next("demo_add")
        .future("demo_branch_reduce")
        .next("demo_branch_multiply")
        .wait(new AddBranchResult(), "demo_branch_reduce")
        .result("demo_division").build();
```
### 通知执行
```java
flowEngine.builder().id("demo_branch_notify")
        .next("demo_add")
        .notify("demo_branch_reduce")
        .next("demo_branch_multiply")
        .result("demo_division").build();
```
### 相容执行
```java
flowEngine.builder().id("demo_branch")
        .next("demo_add")
        .all("demo_branch_reduce", "demo_branch_multiply")
        .result("demo_division")
        .build();
```
### 循环执行
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
### 嵌套执行
```java
flowEngine.builder().id("demo_branch_nested")
        .next("demo_add")
        .all(
                flowEngine.builder().id("nested_1").next("demo_reduce").result("demo_remainder").build(),
                flowEngine.builder().id("nested_2").next("demo_multiply").result("demo_remainder").build())
        .result("demo_division")
        .build();
```
### 匿名嵌套执行
```java
flowEngine.builder().id("demo_branch_anonymous")
        .next("demo_add")
        .all(
                flowEngine.branch().next("demo_reduce").result("demo_remainder").build(),
                flowEngine.branch().next("demo_multiply").result("demo_remainder").build())
        .result("demo_division")
        .build();
```
## 条件判断
![在这里插入图片描述](https://img-blog.csdnimg.cn/7565eae3a50842eb9ea6f9ced7bbff81.png)
### 规则脚本判断
可以使用规则脚本来判断，如：年龄<14岁签发儿童票，年龄>=14岁签发成人票:
```java
flowEngine.builder().id("train_ticket")
        .next("base_price")
        .next(
                Info.c("age < 14", TrainChildTicket.class),
                Info.c("age >= 14",TrainAdultTicket.class)
        .result("ticket_result")
        .build();
```
执行：
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```
### 嵌入函数判断
可以使用嵌入函数进行条件判断:
```java
flowEngine.builder().id("train_ticket_match")
        .next(TrainBasePrice.class)
        .next(
                Info.c(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() < 14, TrainChildTicket.class),
                Info.c(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() >= 14, TrainAdultTicket.class))
        .next(TrainTicketResult.class)
        .build();
```
### 自定义条件参数
可以扩展条件判断参数，自定义传入：
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();

//自定义条件参数
Map<String, Object> condition = new HashMap();
condition.put("sex", "man");

Ticket ticket = flowEngine.execute("train_ticket", passenger, condition);
```
也可在上下文中动态添加：
```java
icontextBus.addCondition("sex", "man");
```

## 节点入参和返回值适配
节点可以有固定的入参和返回值类型，在编排时，再嵌入函数转换和适配。
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
流程传入参数为Passenger，转成Station传入TrainBasePriceStation节点：
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
        .build();
```
执行：
```java
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
```

## 流程终止
### 正常终止
通过IContextBus接口的stopProcess，停止整个流程。
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
### 异常终止
通过节点中抛出异常，终止流程，但仅限同步执行节点。
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
### 回滚流程
通过IContextBus接口的rollbackProcess方法，主动回滚整个流程，会按逆序触发已执行节点rollback方法，rollback默认为空方法，节点可根据需要选择性实现。
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
## 线程
### 超时时间
concurrent、wait异步执行函数，可通过超时参数，设置最大等待时间，单位为毫秒:
```java
flowEngine.builder().id("demo_flow_concurrent_timeout")
        .next("demo_add")
        .concurrent(new AddResult(), 10, "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .build();
```
并行结果处理handle可接收isTimeout，判断是否有超时:
```java
private static class AddResult implements IResult<Integer> {
        @Override
        public Integer handle(IContextBus iContextBus, boolean isTimeout) {
            System.out.println("AddResult handle isTimeout: " + isTimeout);
        }
}
```
### 线程池配置
通过yml文件定义线程池配置
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
重新定义名为flowThreadPool线程池Bean:
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
通过参数传入独立的线程池:
```java
flowEngine.builder().id("demo_flow_concurrent_isolate")
        .next("demo_add")
        .concurrent(new AddResult(), Executors.newFixedThreadPool(3), "demo_reduce", "demo_bit_right")
        .result("demo_division")
        .build();
```
### ThreadLocal处理
通过实现IThreadContent接口，将需要的ThreadLocal信息传递至异步线程。
```
//继承IThreadContent接口，并定义成bean
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
## 动态构建流程
通过builder.buildDynamic方法，动态构建流程，动态流程不注册，可多次重复构建。
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