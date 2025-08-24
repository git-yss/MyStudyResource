# JVM前言

>聊聊常见面试题

1、请你谈谈你对jvm的理解？Java虚拟机有什么更新？

2、什么时OOM，请你说说OOM产生的原因？如何分析？

3、JVM的常见调优参数有哪些？

4、内存快照抓取，如何分析,命令是什么？

5、堆里分区：eden、survial(from、to)、老年区

6、GC垃圾收集算法有哪几个？谈谈利弊？

>BAT难度的面试题

1、JVM垃圾回收的时候如何确定垃圾，GCRoot?

2、-X、-XX参数你用过哪些？

3、常见的项目发布后配置过JVM调优参数吗？

4、引用、强引用、弱引用、虚引用？

5、GC垃圾回收器和GC回收算法关系？分别有哪些？

6、谈谈默认的垃圾回收器？

7、垃圾回收器的特点？

8、OOM你看过几种？

# JVM类加载器ClassLoader

>类的加载、连接和初始化将car.class转为car Class

加载：查询.class文件并加载类的二进制数据

连接：

- 验证：保证被记载的类的正确性；
- 准备：给类的静态变量分配空间，赋值默认初始值；
- 解析：把类中的符号引用转换为直接引用；

初始化：给类的静态变量赋值正确的值；

![image-20250821214933805](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250821214933805.png)

>类加载器分类

1、java虚拟机自带的加载器

- bootStrap 根加载器（加载系统的包，jdk核心库中的rt.jar）
- Ext              扩展类加载器（加载一些扩展jar包的类）
- Sys/App     系统/应用加载器 （我们编写的类）

2、用户自己定义的加载器

- ClassLaoder只需要继承这个抽象类即可，自定义自己的类加载器

双亲委派机制：可以保护java核心类不会被自定义的类替换

# 类的加载

```
/**
 * JVM参数有： -XX:+TraceClassLoading //1、用于追踪类的加载信息行打印 2、 分析项目为啥启动慢，可以快速定位自己类有没有被加载
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(MyChild.str);
        //结果
        //MyParent static
        //MyChild static
        //hello MyChild
    }
}
class MyParent{
    static {
        System.out.println("MyParent static");
    }
    public static String str = "hello MyParent";
}

class MyChild extends MyParent{
    static {
        System.out.println("MyChild static");
    }
    public static String str = "hello MyChild";
}
```

# final加载分析

```
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(MyParent.str);
        /**
         *fianl常量编译阶段  常量池
         * 这个代码将常量放到了Main的常量池中，之后访问的str与MyParent.str都无关了
         * 所以返回结果虽然是hello MyParent ，但是不会触发System.out.println("MyParent static");
         */
    }
}
class MyParent{
    static {
        System.out.println("MyParent static");
    }
    public static final String str = "hello MyParent";
}
```

```
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(MyParent.str);
        /**
         *当一个常量并非在编译时就可以确定的，那么这个常量就不会保存在方法调用类的静态常量池中。
         * 程序运行期间的时候会主动使用常用的类
         */
    }
}
class MyParent{
    static {
        System.out.println("MyParent static");
    }
    public static final  String str = UUID.randomUUID().toString();
}
```

**编译优化过程：**
在编译阶段，`MyParent.str`  会被直接替换为字面量  `"hello MyParent"`。生成的 `Main.class` 字节码中**不会出现对 `MyParent` 类的符号引用**。

# Native方法

Native：只要带了这个关键字就说明java作用范围达不到，只能调用底层C语言的库！

```
public class Main {

    public static void main(String[] args) throws InterruptedException, AWTException {
        Robot robot = new Robot();
        robot.mouseMove(100,100);//控制鼠标位置
    }
}
```

# 程序计数器

线程私有，每个线程都由一个程序计数器；

程序计数器占有一个十分小的内存空间作用为：控制字节码执行位置

分支、循环、跳转、异常处理都需要依赖程序计数器来完成！

![image-20250822222625230](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250822222625230.png)

# 方法区的今生前世

Method Area方法区是Java虚拟机规范中定义的运行时数据区域一直和堆一样可以线程共享！

jdk1.7之前

永久代：用于存储一些虚拟机加载类的信息、常量、字符串、静态变量、符号引用、方法代码等。。。这些东西都放在永久代中；

但是永久代空间有限，满了后会报错`outOfMemery:java PermGen`

jdk1.8之后

将永久代改名为元空间 ，原来的东西放在堆中或者元空间Metaspace空间；

元空间就是方法区在hotspot jvm的实现；

元空间和永久代都是jvm规范中方法区的实现。

区别：元空间不在虚拟机内存，而是本地内存！

`-XX:MetaspaceSize10m`

如果元空间满了报错：`outOfMemery: Metaspace`

# 栈Stack

>栈和队列

程序=数据结构+算法

栈和队列都是基本的数据结构

栈的优势：存取速度比堆快！仅次于寄存器，栈的数据不可以共享

栈里面一定不存在垃圾回收问题，只要线程结束该栈就回收

>栈的原理

java栈的组成元素--栈帧

![image-20250823194454793](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250823194454793.png)

![image-20250823194025363](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250823194025363.png)

谈谈你认识几种JVM？

- SUN公司 Hotspot
- BEA公司 JRockit
- IBM公司 J9VM

# 堆（heap）

**java7之前：**

Heap堆：一个jvm实例中只存在一个堆，堆的内存大小时可以调节的。

可以存的内容：类、方法、常量、保留了类型引用的真实信息；

**分为三个部分**

- 新生区：Young   (Eden-s0-s1)

- 养老区：Old Tenure

- 永久区：Perm

  堆内存在逻辑上分为三个部分：新生、养老、永久（JDK1.8以后叫元空间）

  物理上只有新生区和养老区i，元空间在本地内存中！不在jvm中！

**垃圾回收主要是在新生区和养老区，又分为 普通GC和FULL GC，如果堆满了，就会报错OOM**

>新生区

新生区就是一个类诞生、成长、消亡的地方！、

*新生区细分*：Eden、s0/s1  ,所有的类都在Eden被new出来的，慢慢的当Eden满了，程序还需要创建对象时程序就会触发轻量级的GC，清理完一次垃圾之后，会将活着的对象放入幸存区。99%信息都在Eden区。

Sun Hotspot虚拟机中内存管理采用分代管理机制，即不同的区域采用不同的算法！

>养老区

超过15次对象还没被清理则将送到养老区;运行几个月后养老区如果也满了就触发重FULL GC ;

> 永久区

放一些jdk自带的Class、interface的元数据；

几乎不会被垃圾回收的;

`OutofMemoryError:PermGen` 在项目启动的时候永久代不够用？可能是加载了太多第三方包！

JDK1.6之前：有永久代、常量池在方法区；

JDK1.7：有永久代，但是开始尝试去掉永久代，常量池在堆中；

JDK1.8之后：永久代被取代为元空间；常量池在元空间中；

闲聊：方法区和堆一样，是共享的区域，是JVM规范中的一个逻辑的部分，但是记住它的别名`非堆`

元空间：它是本地内存！

口诀：关于垃圾回收：分代收集算法 即不同的区域采用不同的算法

Young: GC频繁区域

Old:GC次数较少

Perm:不会产生GC

**特点：**

普通GC:只针对新生代 [GC]

全局GC:主要是针对老年代，偶尔伴随新生代 [FULL GC]

# 堆内存调优（初识）

![image-20250823202202598](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250823202202598.png)

```
/**
 * 默认内存配置
 * maxMemory：7193MB（虚拟机试图获取的最大内存量 一般是本机内存的1/4）
 * totalMemory：485MB（虚拟机试图默认的内存总量 一般是本机内存的1/64）
 *
 * 我们可以自定义堆的内存总量
 * -XX:+PringGCDetails;//输出详细的垃圾回收信息
 * -Xmx:最大分配内存 1/4
 * -Xms:初始分配内存 1/64
 */
public class Main {

    public static void main(String[] args) throws InterruptedException, AWTException {
       //获取堆内存的初始大小和最大大小
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        System.out.println(maxMemory/1024/1024+"MB");
        System.out.println(totalMemory/1024/1024+"MB");
    }
}
```

# Dump内存快照及JProfiler

请你说说工作中怎么排查OOM

1、运行前操作

2、监控！

在Java程序运行的时候，想要测试运行的情况！

使用一些工具来查看；

1. Jconsole
2. idea debug
3. IDEA(JProfiler插件)

>JProfile插件

一款性能瓶颈分析插件

```
/**
 * 默认内存配置
 -Xmx1m -Xms1m -XX:+HeapDumpOnOutOfMemoryError
 */
public class Main {
    byte[] bytes = new byte[1024 * 1024 * 100];
    private ArrayList<Main> list = new ArrayList<>();
   public void test() throws InterruptedException {
       Main main = new Main();
       list.add(main);
   }
    public static void main(String[] args) throws InterruptedException, AWTException {
        Main main = new Main();
        main.test();
    }
}

```

![image-20250823214623183](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250823214623183.png)



# GC四大算法

**1、JVM垃圾回收的时候如何确定垃圾，GCRoot?**

什么是垃圾:简单来说就是不再被引用的对象！

>引用计数法（了解即可）

![image-20250824212912658](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250824212912658.png)

特点：每个对象都有一个引用计数器，每引用一次计数器+1，为0则直接垃圾回收

缺点：

- 计数器维护麻烦
- 循环引用无法处理！

JVM一般不采用这种方式！

>可达性算法，GC Root(普遍使用)

![image-20250824221501437](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250824221501437.png)

一切都是从GC Root这个对象开始遍历的，只有和GC Root这个对象关联就不是垃圾！

**什么是GC Root？**

- 虚拟机栈中引用的对象！
- 类中静态属性引用的对象
- 方法区的常量
- 本地方法栈Native引用的对象





>复制算法

年轻代中就是使用复制算法！因为年轻代对象存活率低，适合全部复制过去，而不是检索某一部分对象复制过去！

![image-20250824213946416](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250824213946416.png)

1、一般普通GC之后，差不多Eden几乎都是空的！

2、每次存活的对象都会被From区和Eden区等复制到to区，from和to会发生一次交换；说白了，谁空谁就是to，每当幸存一次就会导致这个对象年龄加一；如果这年龄大于15则会进入老年代

优点：没有标记和清除的过程，效率高！没有内存碎片！

缺点：需要浪费双倍的空间！

>标记清除算法

![image-20250824214949163](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250824214949163.png)

优点：不需要额外的空间

缺点: 两次扫描，内存活得对象耗时则比较多，会产生内存碎片，只能适合存活率高低的区域；

>标记压缩算法

![image-20250824215415694](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250824215415694.png)

减少了上面标记清除的缺点，没有内存碎片但是再次加了一次扫描导致耗时更加严重！

那我们什么时候考虑使用标记压缩算法呢？

在我们这个要是有算法的空间种中，假设空间很少，不经常使用GC,那么可以考虑这个算法！

>**小总结**

内存效率：复制算法>标记清除>标记压缩算法

内存整齐度：复制算法=标记压缩>标记清除

内存利用率：标记压缩算法=标记清除>复制算法

**从效率上说，复制算法最好，空间但是浪费的比较多！为了兼顾所有指标，标记压缩算法会平滑一些，只是效率上不太行！**





**难道没有一种最优的算大吗？思考一下：**

没有！分代收集算法：不同的区域使用不同的算法！没有最好的只有最合适的！



**年轻代：**

相对于老年区，对象存活率低！

Eden区对象存活率极低！统计99%对象基本第一次使用后都会失效！推荐使用复制算法



**老年代：**

区域比较大，对象存活率较高！

推荐使用：标记清除/压缩！







