# Salt Function Flow
Salt Function Flow是内存级别的轻量级流编排组件，它使用函数API来实现编排。

## 快速开始
包括流程通用功能节点实现、流程编排、流程运行。

### Maven
```
<dependency>
    <groupId>cn.fenglingsoftware</groupId>
    <artifactId>salt-function-flow</artifactId>
    <version>1.0.2</version>
</dependency>
```

### 实现流程功能节点
继承FlowNodeWithReturn类，实现doProcess方法，并声明@NodeIdentity，指定节点ID
```
//获取上一个节点返回值，并加123，返回
@NodeIdentity(nodeId = "demo_add")
public class DemoAddNode extends FlowNodeWithReturn<Integer> {
    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        Integer result = preResult + 123;
        System.out.println("DemoAddNode: " + preResult + "+123=" + result);
        return result;
    }
}
```
```
//获取上一个节点返回值，并减15，返回
@NodeIdentity(nodeId = "demo_reduce")
public class DemoReduceNode extends FlowNodeWithReturn<Integer> {
    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        Integer result = preResult - 15;
        System.out.println("DemoReduceNode: " + preResult + "-15=" + result) ;
        return result;
    }
}
```
```
//获取上一个节点返回值，并乘73，返回
@NodeIdentity(nodeId = "demo_multiply")
public class DemoMultiplyNode extends FlowNodeWithReturn<Integer> {
    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        Integer result = preResult * 73;
        System.out.println("DemoMultiplyNode: " + preResult + "*73=" + result);
        return result;
    }
}
```
```
//获取上一个节点返回值，并除12，返回
@NodeIdentity(nodeId = "demo_division")
public class DemoDivisionNode extends FlowNodeWithReturn<Integer> {
    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        Integer result = preResult / 12;
        System.out.println("DemoDivisionNode: " + preResult + "/12=" + result);
        return result;
    }
}
```

### 编排注册流程节点
注入FlowEngine类型Bean，注册ID为demo_flow流程，使用API编排节点顺序执行
![顺序执行](https://img-blog.csdnimg.cn/eb3598ae4db145858e7e47a190235af6.png)

```
@Autowired
FlowEngine flowEngine;

//节点顺序执行
flowEngine.builder().id("demo_flow").next("demo_add").next("demo_reduce").next("demo_multiply").result("demo_division").build();
```

### 执行流程

```
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

## 节点功能扩展
扩展demo_reduce节点，如果传入参数 > 40，执行扩展demo_remainder取余节点。
![在这里插入图片描述](https://img-blog.csdnimg.cn/1e89dc3d74984e9ea4401e1bcbbe50d5.png)
```
flowEngine.builder().id("demo_flow_extend")
                .next("demo_add")
                .next(
                        Info.builder().include("param <= 40").id("demo_reduce").build(),
                        Info.builder().include("param > 40").id("demo_remainder").build()
                )
                .next("demo_multiply")
                .result("demo_division").build();
```
## 复杂网关编排
### 排他执行
根据传入条件执行demo_reduce或demo_multiply节点
![在这里插入图片描述](https://img-blog.csdnimg.cn/585b8101369546fbb750e5ba6d0b85d1.png)
```
flowEngine.builder().id("demo_flow_exclusive")
                .next("demo_add")
                .next(
                        Info.builder().include("param <= 40").id("demo_reduce").build(),
                        Info.builder().include("param > 40").id("demo_multiply").build()
                )
                .result("demo_division").build();
```
### 并行执行
#### 并行形式一
并行（异步并发）执行demo_reduce、demo_multiply节点，并结果相加。
![在这里插入图片描述](https://img-blog.csdnimg.cn/59d92327b03e4f56a1488e093129895f.png)
```
flowEngine.builder().id("demo_flow_concurrent")
                .next("demo_add")
                .concurrent(new AddResult(), "demo_reduce", "demo_multiply")
                .result("demo_division")
                .build();
```
```
//结果相加handle
private static class AddResult implements IResult<Integer> {
    @Override
    public Integer handle(IContextBus iContextBus, boolean isTimeout) {
        Integer demoReduceResult = iContextBus.getPassResult("demo_reduce") != null ?  (Integer) iContextBus.getPassResult("demo_reduce") : 0;
        Integer demoMultiplyResult = iContextBus.getPassResult("demo_multiply") != null ? (Integer) iContextBus.getPassResult("demo_multiply"): 0;
        Integer handleResult = demoReduceResult + demoMultiplyResult;
        System.out.println("Addresult " + demoReduceResult + "+" + demoMultiplyResult + "=" + handleResult);
        return handleResult;
    }
}
```
#### 并行形式二
异步执行demo_reduce、 demo_multiply节点，并结果相加。
![在这里插入图片描述](https://img-blog.csdnimg.cn/2adc436c9a994e55a0234dc21fb8de3c.png)
```
flowEngine.builder().id("demo_flow_future")
                .next("demo_add")
                .future("demo_reduce", "demo_multiply")
                .wait(new AddResult(), "demo_reduce", "demo_multiply")
                .result("demo_division")
                .build();
```
异步执行demo_reduce节点，同步执行demo_multiply，并结果相加。
![在这里插入图片描述](https://img-blog.csdnimg.cn/ce1d5d54fc3a48d18be054bbb2c114ff.png)
```
flowEngine.builder().id("demo_flow_future_1")
                .next("demo_add")
                .future("demo_reduce")
                .next("demo_multiply")
                .wait(new AddResult(), "demo_reduce")
                .result("demo_division")
                .build();
```
### 通知执行
通知执行demo_reduce。
![在这里插入图片描述](https://img-blog.csdnimg.cn/fe9c502ada5449fb8e5884c011041f89.png)
```
flowEngine.builder().id("demo_flow_notify")
                .next("demo_add")
                .notify("demo_reduce")
                .next("demo_multiply")
                .result("demo_division")
                .build();
```
### 相容执行
#### 同步相容
同步相容执行demo_reduce、demo_multiply。
![在这里插入图片描述](https://img-blog.csdnimg.cn/9e28c6b39a134cba9fc889d9c247382d.png)
```
flowEngine.builder().id("demo_flow_inclusive")
                .next("demo_add")
                .all(
                        Info.builder().include("param > 30").id("demo_reduce").build(),
                        Info.builder().include("param < 50").id("demo_multiply").build()
                )
                .result("demo_division").build();
```
#### 异步相容
异步相容执行demo_reduce、demo_multiply。
![在这里插入图片描述](https://img-blog.csdnimg.cn/39063d6689794b7bbaba78f2ae5efa6a.png)
```
flowEngine.builder().id("demo_flow_inclusive_concurrent")
                .next("demo_add")
                .concurrent(
                        new AddResult(),
                        Info.builder().include("param > 30").id("demo_reduce").build(),
                        Info.builder().include("param < 50").id("demo_multiply").build()
                )
                .result("demo_division").build();
```
## 子流程支持
构建子流程。
```
flowEngine.builder().id("demo_branch_reduce").next("demo_reduce").result("demo_remainder").build();
flowEngine.builder().id("demo_branch_multiply").next("demo_multiply").result("demo_remainder").build();

```
### 排他执行
基本与同节点编排相同。
```
flowEngine.builder().id("demo_branch_exclusive")
                .next("demo_add")
                .next(
                        Info.builder().include("param <= 40").id("demo_branch_reduce").build(),
                        Info.builder().include("param > 40").id("demo_branch_multiply").build()
                )
                .result("demo_division")
                .build();
```
### 并行执行
```
flowEngine.builder().id("demo_branch_concurrent")
                .next("demo_add")
                .concurrent(new AddBranchResult(), "demo_branch_reduce", "demo_branch_multiply")
                .result("demo_division")
                .build();
```
```
flowEngine.builder().id("demo_branch_future")
                .next("demo_add")
                .future("demo_branch_reduce")
                .next("demo_branch_multiply")
                .wait(new AddBranchResult(), "demo_branch_reduce")
                .result("demo_division").build();
```
### 通知执行
```
flowEngine.builder().id("demo_branch_notify")
                .next("demo_add")
                .notify("demo_branch_reduce")
                .next("demo_branch_multiply")
                .result("demo_division").build();
```
### 相容执行
```
flowEngine.builder().id("demo_branch")
                .next("demo_add")
                .all("demo_branch_reduce", "demo_branch_multiply")
                .result("demo_division")
                .build();
```
### 嵌套执行
```
flowEngine.builder().id("demo_branch_nested")
                .next("demo_add")
                .all(
                        flowEngine.builder().id("nested_1").next("demo_reduce").result("demo_remainder").build(),
                        flowEngine.builder().id("nested_2").next("demo_multiply").result("demo_remainder").build())
                .result("demo_division")
                .build();
```
### 匿名子流程
```
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
条件判断参数默认为执行流程入参，<14岁签发儿童票，>=14岁签发成人票。
```
flowEngine.builder().id("train_ticket")
                .next("base_price")
                .next(
                        Info.builder().include("age < 14").id("child_ticket").build(),
                        Info.builder().include("age >= 14").id("adult_tickt").build())
                .result("ticket_result")
                .build();
```
```
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket", passenger);
System.out.println("train_ticket result: " + ticket.getPrice());
```
#### 自定义条件参数
不使用执行流程入参作为条件判断参数，自定义传入。
```
Passenger passenger = Passenger.builder().name("jack").age(12).build();
//自定义条件参数
Map<String, Object> condition = new HashMap();
condition.put("age", 12);
Ticket ticket = flowEngine.execute("train_ticket", passenger, condition);
System.out.println("train_ticket result: " + ticket.getPrice());
```
### 嵌入函数判断
使用函数进行条件判断。
```
flowEngine.builder().id("train_ticket_1")
               .next("base_price")
               .next(
                       Info.builder().match(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() < 14).id("child_ticket").build(),
                       Info.builder().match(iContextBus -> ((Passenger) iContextBus.getParam()).getAge() >= 14).id("adult_tickt").build())
               .result("ticket_result")
               .build();
```
```
Passenger passenger = Passenger.builder().name("jack").age(12).build();
Ticket ticket = flowEngine.execute("train_ticket_1", passenger);
System.out.println("train_ticket_1 result: " + ticket.getPrice());
```
## 节点入参和返回值
节点可以有固定的入参和返回值类型，在编排时，再嵌入函数使用上下文构建或转换。
```
//基础票价计算入参为路程信息，继承FlowNodeWithReturnAndInput抽象类，定义入参
@NodeIdentity(nodeId = "base_price")
public class TrainBasePrice extends FlowNodeWithReturnAndInput<Integer, Station> {

    @Override
    public Integer doProcessWithInput(IContextBus iContextBus, Station station) {
        if (station != null) {
            System.out.println("Passengers travel from " + station.getFrom() + " to " + station.getTo());
        }
        System.out.println("Calculate the basic train ticket price 300");
        return 300;
    }
}
```
```
//编排时嵌入input和output函数，通过流程下文构建处理入参数和返回值
flowEngine.builder().id("train_ticket_2")
                .next(
                        Info.builder().id("base_price")
                                .input(iContextBus -> {
                                    Passenger passenger = (Passenger) iContextBus.getParam();
                                    return Station.builder().from(passenger.getFrom()).to(passenger.getTo()).build();
                                })
                                .output((iContextBus, result) -> {
                                    System.out.println("base_price return " + result);
                                }).build())
                .next(
                        Info.builder().include("age < 14").id("child_ticket").build(),
                        Info.builder().include("age >= 14").id("adult_tickt").build())
                .result("ticket_result")
                .build();
```
## 数据传递
流程执行实例上下文数据通过IContextBus接口进行传递
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
## 流程终止
### 正常终止
通过IContextBus接口的stopProcess，停止整个流程。
```
@NodeIdentity(nodeId = "demo_bit_and")
public class DemoBitAndNode extends FlowNodeWithReturn<Integer> {

    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        if (preResult > 500) {
            System.out.println("DemoBitAndNode: stop flow");
            iContextBus.stopProcess();
        } else {
            Integer result = preResult & 256;
            System.out.println("DemoBitAndNode: " + preResult + "&256=" + result);
            return result;
        }
        return null;
    }
}
```
### 异常终止
通过节点中抛出异常，终止流程，但仅限同步执行节点。
```
@NodeIdentity(nodeId = "demo_bit_or")
public class DemoBitOrNode extends FlowNodeWithReturn<Integer> {

    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        if (preResult > 500) {
            System.out.println("DemoBitOrNode: throw exception");
            throw new RuntimeException("DemoBitOrNode Exception!");
        } else {
            Integer result = preResult | 128;
            System.out.println("DemoBitOrNode: " + preResult + "|128=" + result);
            return result;
        }
    }
}
```
### 回滚流程
通过IContextBus接口的rollbackProcess方法，主动回滚整个流程，会按逆序触发已执行节点rollback方法，rollback默认为空方法，节点可根据需要选择性实现。
```
default <T, R> void rollback(IContextBus<T, R> iContextBus) {}
```
```
@NodeIdentity(nodeId = "demo_bit_xor")
public class DemoBitXorNode extends FlowNodeWithReturn<Integer> {

    @Override
    public Integer doProcess(IContextBus iContextBus) {
        Integer preResult = (Integer) iContextBus.getPreResult();
        if (preResult > 500) {
            System.out.println("DemoBitOrNode: rollback flow");
            iContextBus.rollbackProcess();
        } else {
            Integer result = preResult | 128;
            System.out.println("DemoBitOrNode: " + preResult + "|128=" + result);
            return result;
        }
        return null;
    }

    @Override
    public <T, R> void rollback(IContextBus<T, R> iContextBus) {
        System.out.println("DemoBitOrNode: rollback execute");
    }
}
```
## 线程
### 超时时间
concurrent、wait异步执行函数，可通过超时参数，设置最大等待时间
```
//concurrent添加超时等待参数，单位为毫秒
flowEngine.builder().id("demo_flow_concurrent_timeout")
                .next("demo_add")
                .concurrent(new AddResult(), 10, "demo_reduce", "demo_bit_right")
                .result("demo_division")
                .build();
```
```
//并行结果处理handle可接收isTimeout，判断是否有超时
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
重新定义名为flowThreadPool线程池Bean。
```
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
通过参数传入独立的线程池
```
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
通过builder.buildDynamic方法，动态构建流程，动态流程不注册，可多次构建。
```
for (int i=0; i<5; i++) {
	int a = (new Random()).nextInt(20);
	String flowID = "demo_flow_dynamic_" + i;
	FlowEngine.Builder builder = flowEngine.builder().id(flowID);
	builder.next("demo_add");
	if (a < 10) {
	    builder.next("demo_reduce");
	} else {
	    builder.next("demo_multiply");
	}
	builder.result("demo_division");
	FlowInstance flowInstance = builder.buildDynamic();
	System.out.println(flowID + " a: " + a);
	System.out.println(flowID + " test: ");
	Integer result = flowEngine.execute(flowInstance, 39);
	System.out.println(flowID + " result: " + result);
}
```