1、打jar包

mvn -q -B package -DskipTests

2、重新构建镜像

docker build -t families-books-api:latest .



2、停止原来的容器和删除容器

docker stop families-books-api

docker rm families-books-api



3、启动容器

docker run -d --name families-books-api --network families-net -p 8080:8080 families-books-api:latest

4、跟随查看启动日志

docker logs -f families-books-api

5、redis启动

docker run -d --name springboot-docker_mysql_1 -e MYSQL_ROOT_PASSWORD=rootpassword -p 3306:3306 mysql:8.0



## 方法一：自建一个 bridge 网络（通用）

*# 建网*

docker network create families-net

*# 已有容器：挂到这张网上（每个都要执行一次）*

docker network connect families-net springboot-docker_mysql_1

docker network connect families-net families-books-api

*# 新起的容器直接指定网络*

docker run -d --name families-books-api --network families-net 

### 方法二：挂在ssl证书

    server {
        listen 80;
        server_name www.yangshun.top;
            # 强制 HTTP 跳 HTTPS（正确）
    return 301 https://$host$request_uri;
    }
    
    server {
        listen 443 ssl;
        server_name www.yangshun.top;
    # ✅ 已修正证书路径
    ssl_certificate /ssl/www.yangshun.top.pem;
    ssl_certificate_key /ssl/www.yangshun.top.key;
    
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
    }
    
    # ✅ API 代理（推荐用容器名访问）
    location /api/ {
        proxy_pass http://families-books-api:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
     }
    }




## 第一步：先删除旧容器（必须执行）

```
docker rm -f my-nginx
```

## 第二步：再运行启动命令（复制这个单行）

```
docker run -d --name my-nginx --network families-net -p 80:80 -p 443:443 -v /nginx/conf.d:/etc/nginx/conf.d -v /usr/local/ssl:/ssl nginx
```

## 第三步：重新加载配置

```
docker exec my-nginx nginx -s reload
```



