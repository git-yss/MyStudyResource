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

Spring IOC 容器的核心是 `BeanFactory`（顶级接口）和 `ApplicationContext`（子接口，功能更全），底层创建 Bean 流程：

1. 加载配置（XML/注解），解析出 Bean 定义（BeanDefinition）；

2. 通过反射创建 Bean 实例；

3. 解析依赖关系，完成 DI 注入；

4. 初始化 Bean（执行 `init-method`/`@PostConstruct`）；

5. 将 Bean 存入容器（单例 Bean 缓存到 `singletonObjects` 集合）；

6. 容器销毁时执行销毁方法（`destroy-method`/`@PreDestroy`）。

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