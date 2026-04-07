# Spring 全家桶核心知识点全解析

Spring 全家桶是 Java 后端开发的核心框架体系，核心包括 Spring Core（核心容器）、Spring Boot（快速开发）、Spring Cloud（微服务）等，其中**IOC、AOP** 是基础，Spring Boot 是简化开发的核心，下面从核心概念、生命周期、启动流程等维度逐一拆解。

## 一、核心基石：Spring Core 核心概念

### 1. IOC（控制反转）—— 核心设计思想

#### （1）定义与本质

IOC（Inversion of Control）即「控制反转」，核心是**将对象的创建、依赖注入的控制权从代码本身转移到 Spring 容器**。

- 传统开发：程序员手动 `new` 对象，管理依赖（如 `UserService service = new UserService();`）；

- Spring 开发：只需定义对象，依赖关系由 Spring 容器（ApplicationContext）统一创建、注入，程序员仅需使用对象。

#### （2）DI（依赖注入）—— IOC 的实现方式

DI（Dependency Injection）是 IOC 的具体落地手段，Spring 支持 3 种注入方式：

- 构造器注入（推荐）：通过构造方法注入依赖，保证对象创建时依赖已初始化；

```Java

// 构造器注入示例
@Service
public class UserServiceImpl implements UserService {
    private final UserDao userDao;
    
    // 构造器注入（Spring 5+ 推荐，强制依赖不可为空）
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

- Setter 注入：通过 Setter 方法注入，支持可选依赖；

- 字段注入（不推荐）：通过 `@Autowired` 直接标注字段，耦合度高，不利于测试。

#### （3）IOC 容器底层原理

1. Spring IoC 本质就干一件事：

   **你不用 new 对象了，交给 Spring 容器去创建、管理、装配、销毁。**

   整个流程分为 4 大阶段：

   1. **启动容器（加载配置）**
   2. **扫描 + 解析 Bean 定义（BeanDefinition）**
   3. **实例化 Bean（createBeanInstance）**
   4. **初始化 Bean（填充属性 + 初始化方法 + 后置处理）**
   5. **Bean 就绪，供使用**
   6. **容器关闭，销毁 Bean**

   下面我一步一步拆开讲，每一步干什么、有啥用。

   ------

   # 0. 准备知识

   - **Bean**：被 Spring 管理的对象
   - **BeanDefinition**：Bean 的 “设计图纸”，包含类名、作用域、懒加载、依赖等
   - **BeanFactory**：Spring 容器顶层接口，负责创建、管理 Bean
   - **ApplicationContext**：功能更强的容器（开发基本都用它）

   ------

   # 第 1 步：启动 Spring 容器

   **入口：启动 main 方法 / 项目启动加载 Spring**

   你写的代码类似：

   

   ```
   AnnotationConfigApplicationContext context = 
       new AnnotationConfigApplicationContext(AppConfig.class);
   ```

   ## 这一步干了啥？

   1. 初始化容器环境
   2. 加载配置类（或 xml）
   3. 准备好**BeanFactory**（底层真正干活的工厂）

   ## 作用

   告诉 Spring：

   “我要启动容器了，你准备好开始干活，去扫描、创建对象。”

   ------

   # 第 2 步：扫描包，收集 Bean 定义（BeanDefinition）

   Spring 开始根据你的配置：

   - `@ComponentScan`
   - `@Configuration`
   - `@Bean`
   - xml 中的 bean 标签

   去**扫描所有标注了 @Component/@Service/@Controller/@Repository 的类**。

   ## 这一步干了啥？

   1. 遍历包路径下所有 class 文件

   2. 判断类上是否有 Bean 注解

   3. **把每个 Bean 封装成一个 BeanDefinition 对象**

   4. 把所有 BeanDefinition 存到一个 Map 里

      ```
      beanDefinitionMap：key=beanName，value=BeanDefinition
      ```

      

   ## BeanDefinition 里有什么？

   - 全类名（class）
   - 是否单例 / 多例
   - 是否懒加载
   - 依赖哪些 Bean
   - 初始化方法、销毁方法

   ## 作用

   **这一步不创建对象，只收集 “图纸”。**

   Spring 先把所有要管理的 Bean 信息记下来，后面统一创建。

   ------

   # 第 3 步：BeanFactoryPostProcessor 处理（扩展点）

   在**真正创建 Bean 之前**，Spring 允许你修改 BeanDefinition。

   比如：

   - 动态修改 Bean 的类
   - 动态修改作用域
   - 加属性
   - MyBatis 就是在这里把 Mapper 变成 Bean

   ## 作用

   给开发者一个**修改 Bean 定义**的机会。

   这是 Spring 强大扩展性的关键一步。

   ------

   # 第 4 步：开始实例化 Bean（真正 new 对象）

   Spring 遍历 beanDefinitionMap，**开始创建对象**。

   ## 这一步干了啥？

   1. 根据 BeanDefinition 找到类

   2. 通过

      反射调用构造方法

      创建实例

   3. 创建出一个**原始对象**（还没填充属性，非常简陋）

   ## 注意

   此时对象只是**空壳**：

   - @Autowired 依赖还没注入
   - 初始化方法还没执行
   - 还不是完整可用 Bean

   ## 作用

   把对象在内存中创建出来，占个坑。

   ------

   # 第 5 步：填充属性（依赖注入 DI）

   这就是 **DI（依赖注入）** 阶段。

   ## 干了啥？

   Spring 扫描这个 Bean 里面：

   - @Autowired
   - @Resource
   - @Value

   然后：

   1. 去容器里找对应的依赖 Bean
   2. 通过**反射 set 方法 / 字段注入**给当前 Bean

   ```
   userService.setOrderService(orderService);
   ```

   ## 作用

   **把依赖的对象自动塞进来，不用你手动 set。**

   这就是 IoC 控制反转的核心体现：

   你不用自己找依赖，Spring 主动喂给你。

   ------

   # 第 6 步：执行 Aware 接口（感知容器）

   如果 Bean 实现了这些接口，Spring 会回调：

   - BeanNameAware
   - BeanFactoryAware
   - ApplicationContextAware

   

   ```
   public class MyBean implements ApplicationContextAware {
       @Override
       public void setApplicationContext(ApplicationContext ctx) {
           // 拿到容器
       }
   }
   ```

   ## 作用

   让 Bean 能**感知到 Spring 容器本身**，获取容器信息。

   ------

   # 第 7 步：BeanPostProcessor 前置处理

   ```
   postProcessBeforeInitialization()
   ```

   ## 作用

   在**初始化方法执行前**做一些增强。

   比如：

   - 处理 @PostConstruct
   - AOP 早期代理
   - 依赖检查

   ------

   # 第 8 步：执行初始化方法

   分三种：

   1. **@PostConstruct**（优先）
   2. 实现 **InitializingBean** 的 afterPropertiesSet ()
   3. xml / 注解中指定的 **init-method**

   ## 作用

   让你在**对象就绪前做自定义初始化**：

   - 加载配置
   - 建立连接
   - 初始化数据

   ------

   # 第 9 步：BeanPostProcessor 后置处理

   ```
   postProcessAfterInitialization()
   ```

   ## 这一步超级重要！

   **AOP 就是在这里生成代理对象！**

   Spring 判断：

   - 这个类有没有切点？
   - 要不要创建代理？
   - 用 JDK 动态代理还是 Cglib？

   然后把原始对象**包装成代理对象**，放回容器。

   ## 作用

   - AOP 实现
   - 动态代理
   - 对 Bean 做最终包装

   ------

   # 第 10 步：Bean 放入单例池，就绪使用

   创建好、初始化完、代理完的 Bean，放入：

   ```
   singletonObjects：一级缓存，完整成熟的单例 Bean
   ```

   之后你：

   ```
   context.getBean(XXService.class)
   ```

   就从这里拿。

   ## 作用

   Bean 正式就绪，随时可以使用。

   ------

   # 第 11 步：容器关闭，销毁 Bean

   容器关闭时执行：

   1. @PreDestroy
   2. DisposableBean 接口
   3. destroy-method

   ## 作用

   释放资源：

   - 关闭连接
   - 清理线程池
   - 保存数据

   

   1. 启动容器
   2. 扫描类 → 生成 BeanDefinition
   3. BeanFactory 后置处理器修改定义
   4. 反射实例化对象（原始对象）
   5. DI 依赖注入（填充 @Autowired）
   6. 执行 Aware 感知容器
   7. 前置处理器
   8. 执行初始化方法
   9. 后置处理器（生成 AOP 代理）
   10. 放入单例池，供使用
   11. 关闭容器 → 销毁 Bean

### 2. AOP（面向切面编程）—— 横向扩展能力

#### （1）定义与场景

AOP（Aspect Oriented Programming）即「面向切面编程」，核心是**将日志、事务、权限等通用逻辑（切面）与业务逻辑解耦**，实现横向复用。

- 核心场景：日志记录、事务管理、权限校验、异常统一处理。

#### （2）AOP 核心术语

|术语|含义|
|---|---|
|切面（Aspect）|通用逻辑的封装（如日志切面、事务切面）|
|连接点（JoinPoint）|程序执行过程中的任意节点（如方法调用、字段赋值），Spring 中仅支持方法连接点|
|切入点（Pointcut）|匹配连接点的规则（如指定包下的所有方法），决定哪些方法会被增强|
|通知（Advice）|切面的具体执行逻辑（如前置通知、后置通知）|
|目标对象（Target）|被增强的业务对象|
#### （3）AOP 底层实现（Spring 两种方式）

- JDK 动态代理：基于接口实现，目标类必须实现接口，通过 `Proxy.newProxyInstance()` 创建代理对象；

- CGLIB 动态代理：基于继承实现，目标类无需实现接口，通过生成子类重写方法实现增强（Spring 默认优先 JDK，无接口则用 CGLIB）。

### AOP 的执行时机：不止编译 / 运行，分 3 类核心场景

AOP（面向切面编程）的执行时机**并非单一的编译期或运行期**，而是根据实现方式不同，分布在**编译期、类加载期、运行期**三个阶段。下面我会用新手易懂的方式拆解每种场景，重点讲清最常用的 Spring AOP 的时机。

### 一、先明确核心结论

表格







|   AOP 实现方式   | 执行时机 |         典型框架 / 工具         |                核心原理                 |
| :--------------: | :------: | :-----------------------------: | :-------------------------------------: |
|     静态 AOP     |  编译期  |      AspectJ（编译时织入）      | 直接修改 Java 字节码，生成新 class 文件 |
| 静态 AOP（扩展） | 类加载期 |      AspectJ（加载时织入）      |       在类加载到 JVM 前修改字节码       |
|     动态 AOP     |  运行期  | Spring AOP、JDK 动态代理、CGLIB |      通过代理对象动态织入切面逻辑       |

### 二、逐类拆解：从易到难理解

### 1. 编译期织入（静态 AOP）—— AspectJ 原生方式

这是最 “早” 的 AOP 执行时机，**在你执行`javac`编译.java 文件为.class 文件时就完成切面织入**。

- 过程：AspectJ 编译器（ajc）会扫描你的切面代码和业务代码，直接把切面逻辑（如日志、权限校验）编译到目标类的字节码中；
- 特点：织入后生成的.class 文件已经包含切面逻辑，运行时无需额外处理，性能最优；
- 示例：如果你用 AspectJ 的编译期织入，编译后的`UserService.class`里，`addUser()`方法会直接包含日志打印的字节码，和手写代码完全一样。

### 2. 类加载期织入（静态 AOP）—— AspectJ 的 LTW（Load Time Weaving）

时机介于编译期和运行期之间：**在 JVM 加载.class 文件到内存时，通过类加载器修改字节码，织入切面**。

- 过程：需要通过 JVM 参数指定 AspectJ 的类加载器代理（`-javaagent:aspectjweaver.jar`），JVM 加载类时会触发字节码修改；
- 特点：无需修改原.class 文件，但仍属于 “静态织入”（织入后类的结构固定），性能接近编译期织入；
- 适用场景：不想重新编译代码，又想获得静态 AOP 的高性能（如对第三方 jar 包织入切面）。

### 3. 运行期织入（动态 AOP）—— Spring AOP 的核心方式

这是 Spring 开发者最常接触的方式，**在程序运行时，通过动态代理创建目标对象的代理类，调用方法时织入切面逻辑**。

- 核心原理：

  1. Spring 容器启动时，扫描所有`@Aspect`注解的切面类，解析切点和通知；
  2. 当获取目标 Bean（如`UserService`）时，Spring 不会直接返回原对象，而是生成一个**代理对象**（JDK 动态代理或 CGLIB）；
  3. 调用代理对象的方法时，先执行切面逻辑（如`@Before`通知），再调用原对象的方法；

  

- 特点：

  - 织入过程完全在运行时完成，无需修改字节码文件，灵活性最高；
  - 性能略低于静态 AOP（每次调用都要走代理逻辑），但日常开发中差异可忽略；

#### （4）AOP 实战示例（注解版）

```Java

// 1. 定义切面
@Aspect
@Component
public class LogAspect {
    // 切入点：匹配 com.example.service 包下所有方法
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}

    // 前置通知：方法执行前执行
    @Before("servicePointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("方法 " + methodName + " 开始执行");
    }

    // 后置通知：方法执行后执行（无论是否异常）
    @After("servicePointcut()")
    public void afterAdvice(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("方法 " + methodName + " 执行结束");
    }
}

// 2. 业务类（目标对象）
@Service
public class UserService {
    public void addUser() {
        System.out.println("执行添加用户业务逻辑");
    }
}
```

### 3. Bean 生命周期（Spring 核心）

Spring Bean 的完整生命周期是 IOC 容器管理对象的核心，共 8 个核心步骤：

1. **实例化**：通过反射创建 Bean 实例（调用无参构造/指定构造）；

2. **属性赋值**：将容器中的依赖注入到 Bean 的字段/属性（DI）；

3. **初始化前**：执行 `BeanPostProcessor` 的 `postProcessBeforeInitialization`（前置处理器）；

4. **初始化**：执行 `@PostConstruct` → 执行 `init-method`（XML 配置）→ 执行自定义初始化逻辑；

5. **初始化后**：执行 `BeanPostProcessor` 的 `postProcessAfterInitialization`（后置处理器，AOP 代理创建在此阶段）；

6. **Bean 就绪**：Bean 存入容器，可被程序使用；

7. **销毁前**：容器关闭时，执行 `@PreDestroy` → 执行 `destroy-method`；

8. **销毁**：Bean 被销毁，释放资源。

#### 生命周期验证示例

```Java

@Component
public class UserBean implements InitializingBean, DisposableBean {
    // 1. 实例化（构造器）
    public UserBean() {
        System.out.println("1. Bean 构造器执行（实例化）");
    }

    // 2. 属性赋值（DI）
    @Autowired
    private UserDao userDao;

    // 3. 初始化前（BeanPostProcessor）→ 外部定义，此处省略

    // 4. 初始化（@PostConstruct）
    @PostConstruct
    public void postConstruct() {
        System.out.println("4. @PostConstruct 执行");
    }

    // 4. 初始化（InitializingBean 接口）
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("4. afterPropertiesSet 执行");
    }

    // 4. 初始化（自定义 init-method，需在配置类指定）
    public void initMethod() {
        System.out.println("4. init-method 执行");
    }

    // 5. 初始化后（BeanPostProcessor）→ 外部定义，此处省略

    // 7. 销毁前（@PreDestroy）
    @PreDestroy
    public void preDestroy() {
        System.out.println("7. @PreDestroy 执行");
    }

    // 7. 销毁（DisposableBean 接口）
    @Override
    public void destroy() throws Exception {
        System.out.println("7. destroy 执行");
    }
}
```

## 二、Spring Boot 核心知识点

### 1. Spring Boot 核心优势

- 自动装配（AutoConfiguration）：无需手动配置 XML，自动加载默认配置；

- 起步依赖（Starter）：如 `spring-boot-starter-web` 整合 Tomcat、Spring MVC 等依赖，无需手动管理版本；

- 嵌入式容器：内置 Tomcat/Jetty/Undertow，直接打包为 JAR 运行；

- 简化配置：通过 `application.yml/application.properties` 统一配置，支持外部化配置。

### 2. Spring Boot 启动流程

Spring Boot 启动的核心入口是 `SpringApplication.run()`，完整流程如下：

1. **初始化 SpringApplication**：
    - 推断应用类型（Servlet/Reactive）；
    
    - 加载初始化器（ApplicationContextInitializer）；
    
    - 加载监听器（ApplicationListener）；
    
    - 推断主类（包含 `main` 方法的类）。
    
2. **执行 run 方法核心逻辑**：

    - 启动计时、加载环境（Environment）；

    - 创建 ApplicationContext（应用上下文）；

    - 刷新上下文（refresh）：核心步骤，完成 Bean 加载、自动装配；

    - 执行启动器（CommandLineRunner/ApplicationRunner）；

    - 启动完成，输出启动日志（如启动耗时）。

#### 启动流程核心代码（简化版）

```Java

// 入口类
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        // 核心启动方法
        ConfigurableApplicationContext context = SpringApplication.run(MyApplication.class, args);
    }
}

// SpringApplication.run 底层核心逻辑（简化）
public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
    return run(new Class<?>[] { primarySource }, args);
}

public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
    SpringApplication application = new SpringApplication(primarySources);
    // 执行启动逻辑
    return application.run(args);
}

public ConfigurableApplicationContext run(String... args) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start(); // 启动计时
    ConfigurableApplicationContext context = null;
    
    // 1. 加载环境（配置、系统变量等）
    ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
    
    // 2. 创建应用上下文（根据应用类型选择 AnnotationConfigServletWebServerApplicationContext 等）
    context = createApplicationContext();
    
    // 3. 准备上下文（加载初始化器、监听器）
    prepareContext(context, environment, listeners, applicationArguments, printedBanner);
    
    // 4. 刷新上下文（核心：加载 Bean、自动装配、启动容器）
    refreshContext(context);
    
    // 5. 执行启动器（CommandLineRunner/ApplicationRunner）
    afterRefresh(context, applicationArguments);
    
    stopWatch.stop(); // 结束计时
    return context;
}
```

### 3. 自动装配（AutoConfiguration）核心原理

自动装配是 Spring Boot 最核心的特性，底层逻辑：

#### （1）核心注解：@SpringBootApplication

`@SpringBootApplication` 是组合注解，核心包含 3 个注解：

- `@SpringBootConfiguration`：本质是 `@Configuration`，标记配置类；

- `@ComponentScan`：扫描当前包及子包下的 `@Component`/`@Service`/`@Repository` 等注解；

- `@EnableAutoConfiguration`：开启自动装配（核心）。

#### （2）@EnableAutoConfiguration 底层逻辑

1. **加载自动配置类**：Spring Boot 启动时，通过 `SpringFactoriesLoader` 读取 `META-INF/spring.factories` 文件，加载所有 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 对应的配置类（如 `WebMvcAutoConfiguration`、`DataSourceAutoConfiguration`）；

2. **条件过滤**：自动配置类上标注 `@Conditional` 系列注解（如 `@ConditionalOnClass`、`@ConditionalOnMissingBean`），只有满足条件才会生效；

    - 例：`DataSourceAutoConfiguration` 仅当类路径下有 `DataSource` 类且容器中无 `DataSource` Bean 时生效；

3. **注入默认配置**：生效的自动配置类会向容器中注入默认 Bean（如 `DispatcherServlet`、`DataSource`），实现「零配置」。

#### 自动装配自定义（常用）

- 排除自动配置：`@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)`；

- 自定义配置：通过 `@Configuration` 手动配置 Bean，覆盖默认配置（Spring 优先使用自定义 Bean）；

- 配置属性：通过 `application.yml` 配置 `spring.datasource.url` 等属性，自动装配时读取这些属性初始化 Bean。

## 三、Spring 全家桶其他核心组件

|组件|作用|
|---|---|
|Spring MVC|基于 MVC 模式的 Web 框架，处理 HTTP 请求（核心：DispatcherServlet 前端控制器）|
|Spring Data JPA|简化数据库操作，基于 JPA 实现 ORM，无需手动写 SQL|
|Spring Security|安全框架，处理认证、授权、防跨站请求等|
|Spring Cloud|微服务框架，提供服务注册（Eureka/Nacos）、配置中心（Config/Nacos）、网关（Gateway）等|
|Spring Boot Actuator|监控 Spring Boot 应用，提供健康检查、指标收集、接口审计等功能|
### 总结

1. **核心基础**：Spring Core 的 IOC 实现了对象控制权反转（容器管理 Bean），AOP 实现了通用逻辑与业务逻辑解耦，Bean 生命周期是容器管理对象的完整流程；

2. **Spring Boot 核心**：启动流程以 `SpringApplication.run()` 为入口，核心是上下文刷新；自动装配通过 `@EnableAutoConfiguration` 加载 `spring.factories` 中的配置类，结合条件注解实现「零配置」；

3. **核心优势**：Spring 全家桶通过「约定大于配置」简化开发，IOC/AOP 是底层基石，Spring Boot 是快速开发的载体，Spring Cloud 是微服务落地的解决方案。