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





## 方法一：自建一个 bridge 网络（通用）

*# 建网*

docker network create families-net

*# 已有容器：挂到这张网上（每个都要执行一次）*

docker network connect families-net springboot-docker_mysql_1

docker network connect families-net families-books-api

*# 新起的容器直接指定网络*

docker run -d --name families-books-api --network families-net 