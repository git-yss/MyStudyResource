



#### 1、juc是什么:

就是我们java原生的并发包，和一些常用的工具类。

#### 2、进程和线程

Java程序至少两个线程：GC、Main

线程有6种状态：面向源码学习！

NEW //新建

RUNNABABLE //运行

BLOKED //阻塞

WATTIING //等待

TIMED_WAITTING //延时等待

TERMINATED//终止

#### 3、wait/sleep区别

1、类不同！wait是object类的方法 sleep是thred类的方法

在juc编程里，线程休眠实现方式！Thread.sleep()

//时间单位TimeUnit。SECONDS.sleep(2);

2、会不会释放资源？sleep:抱着锁睡觉，不会释放锁！wait会释放锁！

3、使用的范围是不同的；wait和notify一起使用，一般在线程通信的时候使用；sleep就是单独的方法，在任何时候都可以用！

4、关于异常：sleep需要捕获异常！ 

#### 4、Lock锁

```
synchronized传统的锁
```

代码

```
public class Main {
    public static void main(String[] args) {

        Ticket ticket = new Ticket();
        //所有函数式接口都可以用lambda表达式简化
        new Thread(() -> {for (int i = 0; i < 50; i++) ticket.getNum();},"a").start();
        new Thread(() -> {for (int i = 0; i < 50; i++) ticket.getNum();},"b").start();
        new Thread(() -> {for (int i = 0; i < 50; i++) ticket.getNum();},"c").start();


    }

}

/**
 * 资源类
 */
class Ticket {

    //ReentrantLock默认非公平锁 后面线程可以插队，也是可重入锁
    private Lock lock =new ReentrantLock();
    private static volatile   int num = 50;


    public synchronized void getNum() {
        lock.lock();
        lock.trylock(1,timeUnit.SECONDS)
        try {
            if(num > 0){
                System.out.println(Thread.currentThread().getName() + "正在卖第" + (num--) + "张票");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}
```

```
Synchronized和Lock区别
```

1、Synchronized是一个关键字，Lock是一个对象

2、Synchronized无法尝试获取锁，Lock可以调用tryLock来尝试获取；

3、Synchronized会自动释放锁（a线程执行完毕后b如果异常也会释放锁），lock锁是手动释放锁！如果不释放则会死锁！

4、Synchronized（线程A（获取锁，如果阻塞），线程B（等待，一直等待）),lock可以尝试获取锁，失败了之后就放弃；

![image-20250812204933056](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250812204933056.png)

5、Synchronized一定是非公平锁，但是lock可以是给公平的，通过入参控制；

6、代码量比较大时候，我们一般使用lock实现精准控制吗，Synchronized适合代码量比较小的同步问题；

#### 5、生产者消费者问题

面试手写题：单例模式、排序算法、死锁、生产者消费者

线程和线程之间本来是不能通信的，但是有时候我们需要线程之间可以协调操作；

```
Synchronized版
```

```Java
//目的：有两个线程，实现一个+1一个-1交替十次
//传统的wait和notify不能实现精准的唤醒和通知
public class Main {
    public static void main(String[] args) {

        Ticket ticket = new Ticket();
        new Thread(()->{
            for (int i = 0; i <10 ; i++) {
                try {
                    ticket.increment();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        },"A").start();
        new Thread(()->{
            for (int i = 0; i <10 ; i++) {
                try {
                    ticket.decrement();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        },"B").start();


    }

}

/**
 * 资源类 线程之间通信就三个固定步骤：判断、执行、通知
 */
class Ticket {


    private    int num = 0;


    public synchronized   void increment() throws InterruptedException {
        if(num!=0){
            //判断是否需要等待
            this.wait();
        }
        num++;
        System.out.println(Thread.currentThread().getName() + "加了第" + num + "张票");
        this.notifyAll();
    }
    public synchronized   void decrement() throws InterruptedException {
        if(num ==0){
            //判断是否需要等待
            this.wait();
        }
        num--;
        System.out.println(Thread.currentThread().getName() + "减了第" +num  + "张票");
        this.notifyAll();
    }
}
```

```java
四条线程可以实现交替吗？
不能，会产生虚假唤醒问题！这里加减方法都是使用if来判断是否等待，正确的方法应该是使用while来循环判断;
        while(num!=0){
            //判断是否需要等待
            this.wait();
        }
        num++;
```

```
Lock版
```

```
/**
 * 用lock方式现实线程通信
 * 例如 abc三线程，希望依次实现：A打印10张，B打印15张，C打印20张
 */
public class Main {
    public static void main(String[] args) {
        Ticket ticket = new Ticket();
        new Thread(()-> {
            try {
                ticket.print10();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },"A").start();
        new Thread(()-> {
            try {
                ticket.print15();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },"B").start();
        new Thread(()-> {
            try {
                ticket.print20();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },"C").start();

    }

}

/**
 * 资源类 线程之间通信就三个固定步骤：判断、执行、通知
 */
class Ticket {

    private Lock lock =new ReentrantLock();
    private  int num = 1;
    private Condition condition1 = lock.newCondition();
    private Condition condition2 = lock.newCondition();
    private Condition condition3 = lock.newCondition();

    public  void print10() throws InterruptedException {
        lock.lock();
        try {
            while(num!=1){
                //判断是否需要等待
                condition1.await();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + i);
            }
            num=2;
            condition2.signal();//唤醒
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
    public  void print15() throws InterruptedException {
        lock.lock();
        try {
            while(num!=2){
                //判断是否需要等待
                condition2.await();
            }
            for (int i = 0; i < 15; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + i);
            }
            num=3;
            condition3.signal();//唤醒
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    public  void print20() throws InterruptedException {
        lock.lock();
        try {
            while(num!=3){
                //判断是否需要等待
                condition3.await();
            }
            for (int i = 0; i < 20; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + i);
            }
            num=1;
            //最后一个c线程执行完毕可以继续唤醒C的lock的condition3，反正num值已经改变不能在执行了
            condition3.signal();//唤醒
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }
}
```

#### 6、锁的本质

```
synchronized本质
```

```
/**
 * 1、标准情况下限制性sendEmail还是先执行sendSms
 * 答案：sendEmail
 * 被synchronized修饰的方式，锁的对象是方法的调用者，所以说这两个线程用的同一个调用对象
 * 如果方法不仅使用synchronized还使用了static,则为静态同步方法，锁的对象不再是实例，而是类对象，所以属于同一个锁
 */
public class Main {
    public static void main(String[] args) {
        Ticket ticket = new Ticket();
        Ticket ticket1 = new Ticket();
        new Thread(()-> {
            try {
                ticket.sendEmail();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },"A").start();
        new Thread(()-> {
            ticket1.sendSms();
        },"B").start();


    }

}
/**
 * 资源类 线程之间通信就三个固定步骤：判断、执行、通知
 */
class Ticket {

    public synchronized   void sendEmail() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
        System.out.println("sendEmail");
    }
    public synchronized   void sendSms() {
        System.out.println("sendSms");
    }

    public synchronized static  void hello1() {
        TimeUnit.SECONDS.sleep(3);
        System.out.println("hello1");
    }
    public synchronized static  void hello2() {
        System.out.println("hello2");
    }

}
```

#### 7、不安全的集合类

只要是并发环境，你的集合类都不安全（List,Map,Set）

>list不安全

```java
    public static void main(String[] args) {
        ArrayList<Object> list = new ArrayList<>();//不安全集合
        List<Object> list1 = Collections.synchronizedList(list);//安全集合
        CopyOnWriteArrayList<Object> list2 = new CopyOnWriteArrayList<>();//安全集合
        for (int i = 0; i < 30; i++) {
             new Thread(() -> {
                 list2.add(UUID.randomUUID().toString().substring(0,3));
                 System.out.println(list2);
             }).start();
        }


    }
```

>CopyOnWriteArrayList

```java
  public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
```

>Set不安全

```java
  public static void main(String[] args) {
       // Set<String> set = new HashSet<>();//不安全 ConcurrentModificationException
       // Set<String> set = Collections.synchronizedSet(new HashSet<>());//安全
        CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();//安全
        for (int i = 0; i < 30; i++) {
            new Thread(() -> {
                set.add(UUID.randomUUID().toString().substring(0,3));
                System.out.println(set);
            }).start();
        }

    }
```

```java
    private boolean addIfAbsent(E e, Object[] snapshot) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] current = getArray();
            int len = current.length;
            if (snapshot != current) {
                // Optimize for lost race to another addXXX operation
                int common = Math.min(snapshot.length, len);
                for (int i = 0; i < common; i++)
                    if (current[i] != snapshot[i] && eq(e, current[i]))
                        return false;
                if (indexOf(e, current, common, len) >= 0)
                        return false;
            }
            Object[] newElements = Arrays.copyOf(current, len + 1);
            newElements[len] = e;
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
```

>Map不安全

```java
    public static void main(String[] args) {
        //加载因子 0.75f 表示容量3/4则开始提前扩容，扩容到2倍
        //容量 16
        //结构 数组+链表+红黑树
       // Map<String, Object> map = new HashMap<>();//不安全 ConcurrentModificationException
        Map<String, Object> map = new ConcurrentHashMap<>();//安全
        for (int i = 0; i < 30; i++) {
            new Thread(() -> {
                map.put(UUID.randomUUID().toString().substring(0,3),"11");
                System.out.println(map);
            }).start();
        }

    }
```

>```
>ConcurrentHashMap安全的原因
>```

```java
 final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                             new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key,
                                                              value, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                           value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (oldVal != null)
                        return oldVal;
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }
```

#### 8、读写锁

>独占锁（写锁）：一次只能被一个线程使用
>
>共享锁（读锁）：该锁可以被多个线程占用

```java
public class Main {

    public static void main(String[] args) {
        MyCache myCache = new MyCache();
        //模拟读写线程
        for (int i = 0; i < 10; i++) {
            new Thread(()->{
                myCache.setMap("a","123");
            },String.valueOf(i)).start();
        }

        for (int i = 0; i < 10; i++) {
            new Thread(()->{
                myCache.getMap("a");
            },String.valueOf(i)).start();
        }
    }

}
//读写锁
class MyCache {
       private volatile Map<String,Object> map = new HashMap<>();
       //读写
      private ReadWriteLock lock =new ReentrantReadWriteLock();
       public void getMap(String key) {
           lock.readLock().lock();
           try {
               System.out.println(Thread.currentThread().getName()+"读取"+key);
               Object o = map.get(key);
               System.out.println(Thread.currentThread().getName()+"读ok"+o);
           } finally {
               lock.readLock().unlock();
           }
       }

       public void setMap(String key,String value) {
           lock.writeLock().lock();
           try {
               System.out.println(Thread.currentThread().getName()+"写入"+key);
               map.put(key,value);
               System.out.println(Thread.currentThread().getName()+"写入ok");
           } finally {
               lock.writeLock().unlock();
           }

       }
 }

```

![image-20250813205719260](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250813205719260.png)

>小结：使用ReentrantReadWriteLock后可以精确精致读写，让读操作可以多线程不分顺序，而写操作严格同步，一次只能一个线程占用，实现读写互斥 写写互斥 读读共享！

#### 9、阻塞队列

队列：FIFO 先进先出

栈：Stack 先进后出

>为什么我们需要使用阻塞队列？
>
>因为有些线程通讯的场景我们不关心唤醒！只需要利用阻塞队列特性！

Queue和Set、List一样都属于Collction下分支，其中Queue又分Dqeue属于双端队列；AbstractQueue属于非阻塞队列；BlockingQueue属于阻塞队列；

>ArrayBlockingQueue四组api

| 方法                 | 阻塞会抛出异常 | 插入阻塞返回布尔值false，取出返回null | 延时阻塞（规定时间内等待获取） | 一直阻塞（一直等待获取）  |
| -------------------- | -------------- | ------------------------------------- | ------------------------------ | ------------------------- |
| 插入                 | add()          | offer()                               | offer(e,time)                  | put()                     |
| 移除                 | remove()       | poll()                                | poll(time)                     | take()                    |
| 检查并获得第一个元素 | element()      | peek()                                | 等待特性的方法没有检查api      | 等待特性的方法没有检查api |



>SynchronousQueue同步队列（特殊的阻塞队列，设计目的是为了线程间的同步传递而不是为了储存数据）

SynchronousQueue 容量为1！

每一个put操作后就会进入阻塞状态，就需要另一个线程调用take操作才能打开上一个线程的阻塞，并且当前线程又进入阻塞状态等待下一个线程put方法唤醒！

>new SynchronousQueue()和 new ArrayBlockingQueue(1) 的主要区别?
>
>答案：
>
>SynchronousQueue 特点：
>没有内部容量：它是一个真正的"零容量"队列
>直接传递：生产者线程直接将元素传递给消费者线程
>必须配对：插入操作必须等待对应的移除操作，反之亦然
>无存储：队列本身不存储任何元素
>ArrayBlockingQueue(1) 特点：
>有固定容量：虽然容量只有 1，但确实可以存储一个元素
>有存储空间：元素被存储在队列内部的数组中
>先插入后消费：生产者先放入元素，消费者随后取出

```
    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<String>  queue = new SynchronousQueue<>();
        // 启动生产者线程
        Thread producer = new Thread(() -> {
            try {
                System.out.println("生产者准备放入元素...");
                queue.put("Hello");
                System.out.println("生产者已放入元素");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 启动消费者线程
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(1000); // 等待1秒再消费
                System.out.println("消费者准备取元素...");
                String element = queue.take();
                System.out.println("消费者取到元素: " + element);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();
        producer.join();
    }
```

#### 9、线程池

>程序运行的本质：占用系统资源！提高程序的使用率，降低我们的一个性能消耗

线程池、连接池、内存池、对象池。。。。。

为啥要用线程池：线程复用

>关于我们线程池

三大方法、七大参数、4种拒绝策略

>三大方法

```
   public static void main(String[] args) throws InterruptedException {
        ExecutorService threadPool = Executors.newCachedThreadPool();//根据服务器硬件能力，遇强则强自适应最高线程数

       // ExecutorService threadPool = Executors.newSingleThreadExecutor();//最多一个线程

        // ExecutorService threadPool = Executors.newFixedThreadPool(5); //固定线程数量
      try {

          for (int i = 0; i < 30; i++) {
              threadPool.submit(()->{
                  System.out.println(Thread.currentThread().getName());
              });
          }
      } catch (Exception e) {
          e.printStackTrace();
      }finally {
          threadPool.shutdown();//用完一定要手动关闭
      }



    }
```

>七大参数

```java
 public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }

 public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
    }

 public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>(),
                                      threadFactory);
    }

  public ThreadPoolExecutor(int corePoolSize,//核心线程数
                              int maximumPoolSize,//最大线程数
                              long keepAliveTime,//超时等待时间
                              TimeUnit unit,//时间单位
                              BlockingQueue<Runnable> workQueue,//阻塞队列
                              ThreadFactory threadFactory//线程工厂
                              RejectedExecutionHandler handler//拒绝策略
                           ) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }
```

>线程池工作流程示意图

![image-20250815213526879](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250815213526879.png)

>4种拒绝策略

![image-20250815214543022](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250815214543022.png)

>ThreadPoolExecuter.AbortPolicy();   抛出异常
>
>ThreadPoolExecuter.CallerRunsPolicy() ;哪里来的找对用的线程执行！
>
>ThreadPoolExecuter.DiscardOldestPolicy() ;尝试获取任务，不一定执行
>
>ThreadPoolExecuter.DiscardPolicy() ;不报错，直接丢弃任务
>
>

>线程池如何设置最大线程数？

CPU密集型：根据cpu处理器数量来定！保证最大效率 

```
Runtime.getRuntime().availableProcessors()//最大线程数
```

IO密集型：场景：50个线程都是进程操作大文件资源，比较耗时！

​       ->这种最大线程数需要>常用的io任务数!

#### 10、四种函数式接口

java.util.function

>所有的函数式接口都可以用来简化编程模型，都可以使用lambda表达式简化！

```
/**
 *函数式接口是必须要掌握和精通的
 * 四个
 * java8
 * Fucntion:有一个输入参数有一个输出参数
 * Consumer有一个输入参数，没有输出参数
 * Supplier:没有输入参数，只有输出参数
 * Predicate:有一个输入参数，判断是否自正确
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        Runnable runnable=()->{};//函数式接口
        Function<String,String> function =s-> s;//函数式接口(简写)
        Function<String,String> function1 = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s;
            }
        };//函数式接口(完整写)
         //执行
        System.out.println(function1.apply("3"));

        //Predicate
        Predicate<String> predicate1 = o -> o.isEmpty();
        Predicate<String> predicate =new Predicate<String>() {
            @Override
            public boolean test(String o) {
                return o.isEmpty();
            }
        };
        System.out.println(predicate.test(""));

        //Supplier
        Supplier<String> supplier=new Supplier<String>() {
            @Override
            public String get() {
                return "aaa";
            }
        };
        System.out.println(supplier.get());

        //Consumer
        Consumer<String> consumer=new Consumer<String>() {
            @Override
            public void accept(String s) {
                System.out.println(s);
            }
        };

        consumer.accept(supplier.get());
    }

}
```

#### 11、Stream流式计算

#### 12、分支合并（了解）

在Java中，`ForkJoinModel` 适合用于以下场景：

1. **任务分割**：将一个大任务分解为多个较小的独立子任务，并在后台线程执行。
2. **无顺序依赖的任务**：任务之间没有依赖关系，可以同时处理多个子任务。
3. **并行处理高效场景**：需要将主线程释放出来处理大量子任务的情况，提高系统的多任务处理能力。

**示例使用场景**：

- 大数目标计算：如计算多个数值的平方和。
- 数据处理流水线：如解析日志、加密数据等不依赖顺序的任务。
- 图形渲染或多媒体处理：需要同时处理多个图形或媒体流。

#### 13、异步回调

>CompletableFuture实战，同步异步处理多个并发任务，可以获得执行结果

```
public class Main {

    // 自定义结果包装类
    static class TaskResult {
        private final String taskName;
        private final Object result;
        private final Throwable error;

        public TaskResult(String taskName, Object result, Throwable error) {
            this.taskName = taskName;
            this.result = result;
            this.error = error;
        }

        public boolean isSuccess() {
            return error == null;
        }

        @Override
        public String toString() {
            if (error != null) {
                return taskName + " failed: " + error.getMessage();
            }
            return taskName + " succeeded: " + result;
        }
    }

    public static void main(String[] args) {
        // 创建自定义线程池（避免使用公共线程池）
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            // 1. 创建并启动三个异步任务
            long startTime = System.currentTimeMillis();
            CompletableFuture<TaskResult> taskD = CompletableFuture.supplyAsync(
                            () -> executeTask("D", 1000), executor)
                    .handle((res, ex) -> new TaskResult("D", res, ex));


            CompletableFuture<TaskResult> taskA = CompletableFuture.supplyAsync(
                            () -> executeTask("A", 800), executor)
                    .handle((res, ex) -> new TaskResult("A", res, ex));

            CompletableFuture<TaskResult> taskB = CompletableFuture.supplyAsync(
                            () -> executeTask("B", 1200), executor)
                    .handle((res, ex) -> new TaskResult("B", res, ex));

            // 任务C异步触发（稍后提交但几乎同时执行）
            CompletableFuture<TaskResult> taskC = CompletableFuture.supplyAsync(
                            () -> executeTask("C", 1500), executor)
                    .handle((res, ex) -> new TaskResult("C", res, ex));

            // 2. 等待所有任务完成
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(taskA, taskB, taskC,taskD);
            // 3. 收集所有任务结果
            List<TaskResult> results = allTasks.thenApply(v ->
                    Arrays.asList(taskA.join(), taskB.join(), taskC.join())
            ).join();

            // 4. 计算总耗时
            long duration = System.currentTimeMillis() - startTime;

            // 5. 分析任务执行结果
            analyzeResults(results, duration);

        } finally {
            // 关闭线程池
            executor.shutdown();
        }
    }

    // 模拟任务执行（可能成功/失败）
    private static String executeTask(String name, int delay) {
        try {
            // 模拟任务执行时间
            Thread.sleep(delay);

            // 模拟随机失败（20%概率）
            if (Math.random() < 0.2) {
                throw new RuntimeException(name + " task failed intentionally");
            }

            return name + "-result";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(name + " task interrupted", e);
        }
    }

    // 结果分析处理方法
    private static void analyzeResults(List<TaskResult> results, long duration) {
        System.out.println("\n===== 任务执行完成 =====");
        System.out.printf("总耗时: %d ms\n", duration);

        // 打印所有任务结果
        results.forEach(System.out::println);

        // 检查是否有失败任务
        List<TaskResult> failedTasks = results.stream()
                .filter(r -> !r.isSuccess()).collect(Collectors.toList());


        if (failedTasks.isEmpty()) {
            System.out.println("✅ 所有任务执行成功");
            // 这里可以处理所有成功的业务逻辑
            processSuccessfulResults(results);
        } else {
            System.err.println("❌ 有任务执行失败: " + failedTasks.size() + "/" + results.size());
            // 这里可以处理失败情况的业务逻辑
            handleFailedTasks(failedTasks);
        }
    }

    private static void processSuccessfulResults(List<TaskResult> results) {
        System.out.println("处理成功结果：");
        results.forEach(r ->
                System.out.println("  " + r.taskName + " 结果: " + r.result)
        );
    }

    private static void handleFailedTasks(List<TaskResult> failedTasks) {
        System.err.println("失败任务处理：");
        failedTasks.forEach(f ->
                System.err.println("  " + f.taskName + " 错误: " + f.error.getMessage())
        );
    }

}
```

#### 14、常用辅助类

###### 1、CountDownLatch

```
/**
 *CountDownLatch使用  场景：控制多个线程执行完成后再执行其他线程
 */
public class Main {


    public static void main(String[] args) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName()  + " come in");
                countDownLatch.countDown();
            }, String.valueOf(i)).start();
        }
        countDownLatch.await();
        System.out.println(Thread.currentThread().getName()+"主线程" + " 离开");
    }
}
```

>小结：awit()会等待计数器归零再执行后面的方法

###### 2、CyclicBarrier

```
/**
 *CyclicBarrier使用  场景：控制线程量达到某个数就会放开拦截进入自己的方法
 */
public class Main {


    public static void main(String[] args) throws InterruptedException {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(7,()-> System.out.println("7个线程都执行完毕"));
        for (int i = 1; i <= 7; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName()  + " come in");
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            }, String.valueOf(i)).start();
        }


    }
}
```

>和CountDownLatch相反，是加法计数器，主要判断线程数
>
>### 选择 CountDownLatch 当：
>
>- 需要主线程等待多个工作线程完成初始化
>- 一次性事件等待（如服务启动、资源加载）
>- 不需要循环使用的场景
>- 典型应用：
>  - 启动服务前的资源检查
>  - 并行计算最终汇总
>  - 测试用例中的并发控制
>
>### 选择 CyclicBarrier 当：
>
>- 多个线程需要相互等待
>- 需要分阶段执行任务
>- 需要在屏障点执行特定操作
>- 需要重复使用的同步点
>- 典型应用：
>  - 并行迭代算法
>  - 多阶段数据处理
>  - 周期性系统状态同步
>  - 模拟压力测试
>
>## 7. 性能与注意事项
>
>### 性能对比：
>
>- **CountDownLatch**：轻量级，基于AQS实现，适合一次性同步
>
>- ##### **CyclicBarrier**：相对较重，支持复杂场景，适合多阶段任务

###### 3、Semaphore

```
/**
 *Semaphore ：信号量
 * CyclicBarrier ：循环栅栏
 * CountDownLatch ：倒计时
 * 模拟：三个停车位   6个车
 *
 */
public class Main {


    public static void main(String[] args) throws InterruptedException {
        Semaphore semaphore = new Semaphore(3);
        for (int i = 1; i <=6 ; i++) {
            new Thread(()->{
                try {
                    semaphore.acquire();//得到车位
                    System.out.println(Thread.currentThread().getName()+"抢到车位");
                    TimeUnit.SECONDS.sleep(3);
                    System.out.println(Thread.currentThread().getName()+"离开车位");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    semaphore.release();//释放车位
                }
            },"car-"+i).start();
        }


    }
}
```

>```
>acquire():d当一个线程调用这个方法就是获取到一个信号量，当如果为信号量为0，则一直等待其他线程释放信号量
>release():信号量+1，唤醒等待的线程！
>场景：多线程共享资源互斥！并发线程的控制（限流）
>```

#### 15、JMM

>Java Memory Model

模型：理论！不是真实存在的！

所有线程如何工作的？

八大操作：

内存交互有8种，虚拟机实现必须保证每一个操作都是原子性的，不可分割的（对于double和long类型的变量来说，load、store、read和write操作在某些平台上允许例外）

lock（锁定）:作用于主内存的变量，把一个变量标识为线程独占状态

unlock(解锁)：作用于主内存的变量，她把一个处于锁定状态的变量释放出来，释放出来的变量才可以被其他线程锁定；

read(读取)：作用于主内存变量，他把一个变量的值从主内存传输到线程的工作内存中以便随后的load动作使用；

load(载入)：作用于工作内存的变量，他把read操作从主内存变量放入到工作内存中

use(使用)：作用于工作内存的变量，他把工作内存中的变量传输到执行引擎，每当虚拟机遇到一个需要使用变量的值就会使用这个指令

assign(赋值)：作用于工作内存的变量，他把一个从执行引擎中接收到的值放入到工作内存的变量副本中

store(储存)：作用于主内存的变量，，他把一个从工作内存中的一个变量的值传送到主内存中，以便后续write使用

 write(写入)：作用于主内存的变量，他把store操作从工作内存中得到的变量的值放入主内存的变量中

JMM对于八种指令使用制定了如下规则：

1. 不许read和load、store和write操作之一单独出现，即 使用了read必须load，使用store必须write
2. 不许线程丢弃他最近的assgin操作，即工作变量的数据改变后必须告知主内存
3. 不许一个线程将没有assgin的数据从工作内存同步到主内存
4. 一个新的变量必须在主内存中诞生，不许工作内存直接使用一个未初始化的变量，就是对变量使用use、store或者assgin操作初始化变量的值
5. 如果对一个变量进行lock操作，会清空所有工作内存中此变量的值，在执行引擎使用这个变量前必须重新load或者assgin操作初始化变量的值
6. 如果一个变量没有被lock,就不能对其进行unlock，也不能unlock一个被其他线程锁住的变量
7. 对一个变量进行unlock操作之前必须把此变量同步到主内存

#### 16、Volatile

1、保证可见性（JMM）

2、禁止指令重排(原子类)

3、不保证原子性

>保证可见性论证

```
/**
 * volatile  如果不加volatile，方法永远不会结束，因为线程A修改了num的值，但是线程B没有及时获取到num的值，所以线程B会一直循环，方法never end
 */
public class Main {
    private volatile static Integer num = 0;
    public static void main(String[] args) throws InterruptedException {
        new Thread(()->{
        while (num == 0){

        }}).start();
        TimeUnit.SECONDS.sleep(1);
        num=1;
        System.out.println("num="+num);

    }

}
```

>不保证原子性验证

原子性：不可分割！

但是如何不让你加锁如何让保证原子性？

可以使用AtomicInteger！等原子类

>禁止指令重排论证（理论）

指令重排：就i是你写的程序不一定是按照你的程序来跑？

源代码-》编译器（优化重排）-》指令并行重排-》内存系统的重排-》最终执行！

1、单线程一定安全！（但是也不能避免指令重排）

处理器在进行重排时候会考虑指令之间的依赖性！

尝试理解多线程下的指令重排问题：

```
int x,y,a,b=0;
线程1                     线程2
x=a                       y=b
b=1                       a=2
理想结果：x=0  y=0;
指令重排：可能先做第二步骤操作在做第一步操作
线程1                     线程2
b=1                       a=2
x=a                       y=b
指令重排后可能的结果：x=2  y=1;
```

指令重排小结：

volatile可以禁止指令重排！

内存屏障：cpu的指令；两个作用：

1、保证特定的执行顺序！

2、保证某些变量的内存可见性

![image-20250819213642072](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250819213642072.png)

请你谈谈指令重排最经典的应用！DCL单例模式

#### 17、单例模式

>懒汉式DCL

```
public class LazyMan {

    //构造器私有
    private LazyMan() {}
    private volatile   static LazyMan instance = null;//关键是Volatile防止赋值是出现指令重排
    public static LazyMan getInstance() {

            if (instance == null) {
                synchronized (LazyMan.class) {
                    if (instance == null) {
                    instance = new LazyMan();//请你也谈谈这个操作！他不是原子性的
                        //java创建对象的过程
                        //1、分配内存空间
                        //2、执行构造方法
                        //3、将对象指定空间

                        //存在指令重排情况导致不安全，可以对实例添加Volatile
                    }
                }
            }
        return instance;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                System.out.println(LazyMan.getInstance().hashCode());
            }).start();
        }
    }
}
```



>饿汉式(不推荐)

```
public class Hungry {
    //构造器私有
    private Hungry() {}

    private final static Hungry instance = new Hungry();

    public static Hungry getInstance() {
        return instance;
    }
}
```

小结：

```
    public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //反射安全吗？官方真的推荐DCL吗？
        //结果：都不安全都会破i坏单例模式，更安全的方式是使用枚举类来编写单例（枚举类只要不被修改jdk源码就是安全的，反射都不能破坏它）
        Constructor<LazyMan> declaredConstructors = LazyMan.class.getDeclaredConstructor();
        LazyMan lazyMan =declaredConstructors.newInstance();
        LazyMan lazyMan1 =declaredConstructors.newInstance();
        System.out.println(lazyMan.hashCode() );
        System.out.println(LazyMan.getInstance().hashCode() );
        System.out.println(lazyMan1.hashCode() );
    }
```

#### 18、CAS

>什么是cas?   比较并交换

在更新变量之前比较这个值是否是期望的值，如果中途被别人修改了自然也就和期望值不匹配，自然也就修改值失败

>小结：
>
>缺点：
>
>1、循环开销太大，会一直循环判断值是否是期望值一直到和期望值相同才会停止
>
>2、内存操作，每次只能保证一个共享变量的原子性！
>
>3、出现ABA问题！

#### 19、原子引用

>什么是ABA问题？狸猫换太子

期望的值和变量值相同，但是变量的值可能已经被改变后再次变回来了，值虽然相同，本质确实被替换了！

```
public class Main {
    private static AtomicStampedReference<Integer> reference = new AtomicStampedReference<>(100,1);//初始值为100，初始版本号1
    public static void main(String[] args) throws InterruptedException {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        atomicInteger.compareAndSet(0,1);
        atomicInteger.compareAndSet(1,0);

        atomicInteger.compareAndSet(0,3);
        System.out.println("ABA问题导致结果为："+atomicInteger);

//        --------如何解决ABA问题？---原子引用-----------------
        reference.getReference()//获取版本号
        reference.compareAndSet(100,101,
                reference.getReference(),reference.getStamp()+1);

        reference.compareAndSet(101,100,
                reference.getReference(),reference.getStamp()+1);

        reference.compareAndSet(100,102,
                reference.getReference(),reference.getStamp()+1);
        System.out.println("解决ABA问题，结果为："+reference.getReference());
    }

}
```

![image-20250820212217915](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250820212217915.png)

#### 20、探究锁（开阔思路）

1、自旋锁

unsafe类的源码：

```
    public final int getAndAddInt(Object var1, long var2, int var4) {
        int var5;
        do {//自旋锁一直判断
            var5 = this.getIntVolatile(this, valueOffset);//获取当前对象内存地址中的值
        } while(!this.compareAndSwapInt(var1, var2, var5, var5 + var4));
        //比较并交换值

        return var5;
    }
```

1、如何自制一把锁？

```
public class Main {
    private static AtomicReference<Thread> reference = new AtomicReference<>();
    public void lock(){
        Thread thread = Thread.currentThread();
        System.out.println("当前线程："+thread);
        //上锁自旋
        while(!reference.compareAndSet(null,thread)){

        }
        System.out.println(thread.getName()+"lock...");
    }
    public void unlck(){
        Thread thread = Thread.currentThread();

        //上锁自旋
        reference.compareAndSet(thread,null);
        System.out.println(thread.getName()+"unlock...");
    }
    public static void main(String[] args) throws InterruptedException {
        Main main = new Main();
        new Thread(()->{
            main.lock();
            try{
                TimeUnit.SECONDS.sleep(5);
            }catch (Exception e){
                e.printStackTrace();
            }
            main.unlck();
        },"R1").start();

        new Thread(()->{
            main.lock();
            try{
                TimeUnit.SECONDS.sleep(1);
            }catch (Exception e){
                e.printStackTrace();
            }
            main.unlck();
        },"R2").start();
    }

}
```

![image-20250820214524037](C:\Users\81157\AppData\Roaming\Typora\typora-user-images\image-20250820214524037.png)

#### 21、什么是死锁？

```

/**
 * 形成死锁怎么办？
 * 日志
 * 查询堆栈信息！ jvm的知识
 * 1、获取当前运行的Java进程号 jps -l
 * 2、查看信息 jstack +进程号
 * 3、jconsole查询线程信息是否死锁（可视化工具）
 */
public class Main {

    public void speak() {
        System.out.println("speak");
    }

    public static void main(String[] args) throws InterruptedException {
        Main main1 = new Main();
        Main main2 = new Main();
        new Thread(()->{
            synchronized (main1){
                try{
                    TimeUnit.SECONDS.sleep(2);//保证两线程都获取到一把锁而没有获取到第二把锁，在获取第二把锁时才会形成竞争
                }catch (Exception e){
                    e.printStackTrace();
                }
                synchronized (main2){
                    main2.speak();
                }
            }
        },"R1").start();

        new Thread(()->{
            synchronized (main2){
                try{
                    TimeUnit.SECONDS.sleep(2);//保证两线程都获取到一把锁而没有获取到第二把锁，在获取第二把锁时才会形成竞争
                }catch (Exception e){
                    e.printStackTrace();
                }
                synchronized (main1){
                    main1.speak();
                }
            }
        },"R2").start();
    }

}
```

