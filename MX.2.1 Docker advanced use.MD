# Docker - advanced use
In this file we will analyze a little more advanced aspects of Docker, such as the different types of volumes, the networking between containers and the replicas if the service is replicated.

____
## Docker resource limitation
A container has no resource constraints by default configuration; but Docker provides ways to control how much memory or CPU a container can use - to ensure that no single container can monopolize system resources, lead to performance issues or service disruptions for other containers or host services.

This is extremely important because if the kernel detects insufficient memory for critical system functions, it throws an **Out Of Memory** Exception (OOM), and can potentially kill processes (including Docker) - Docker daemon can manage this problem.
These options are essential for maintaining the stability and performance of **both containers and the host system**.

We can specify the memory limitations in the `docker run` command or put **directly** the option **in the Docker Compose**.
In the first case we add the `--memory` flag as we can see here:
```
docker run --name container_name --memory=512m image_name
```
But we can also manage the limitation here (example seen in class):
```yml
services:
  producer:
    build: producer-end
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      lavinmq:
        condition: service_healthy
```

In this case we use `mem_limit`, to set a basic memory limit for a container - **maximum amount of memory** that the container can use; but we can also use `mem_reservation`: it specify a **soft memory limit**, indicating the minimum amount of memory that should be reserved for the container. We can also combine both flags, for example (change a file seen in class):
```yml
services:
  product:
    build: microservices/product-service
    mem_limit: 512m
    mem_reservation: 256m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      mongodb:
        condition: service_healthy
```

We can manage also the CPUs, as we said in the first part.
```yml
deploy:
  resources:
    limits:
      cpus: '0.5'
      memory: 512M
    reservations:
      cpus: '0.25'
      memory: 256M
```
For example here we limit the container to use up to 50% of a single CPU core and that is guarantees that the container will have at least 25% of a single CPU core available for him.\
This is done for example to ensure resources in a fair way, or handling high availability even under high load.


___
## Docker volumes
Docker volumes are a mechanism for persisting data generated by and used by Docker containers. There are different types of volumes: named, anonymous and bind. In this paragraph we want to analyze the characteristics of each type providing some examples.

_Is also important to say that if a service doesn't specify a volumes section, it means that the service won't have any volumes mounted. Essentially, **no data will be persisted outside the container** for that service._


### Named Volumes
Named Volumes are created and managed by Docker, allowing you to persist data across container restarts and removals.
Defined in the Docker-compose file or created by using command line with: `docker volume create`.\
Named volumes are not tied to a specific container instance and can be reused! **Perfects for scenarios where data persistence is required**.
```yml
services:
  mongodb:
    image: mongo:latest
    restart: always
    ports:
      - '27017:27017'
    environment:
      MONGO_INITDB_ROOT_USERNAME: admin
      MONGO_INITDB_ROOT_PASSWORD: secret
      MONGO_INITDB_DATABASE: projects
    volumes:
      - mongo-data:/data/db
```
As we can see in the example, the volume is set with [...] **:** [...].\
The first part is the name of the volume (in out case `mongo-data`), the second one is the path in the container.

### Anonymous Volumes
Anonymous volumes are temporary volumes created by Docker when a container starts. They are typically used for temporary data, and they don't have a specific name, just because they are temporary.\
These volumes are ephemeral and are removed when the container is removed.
```yml
services:
  web:
    build: ./web
    volumes:
      - /app/temp
    environment:
      - FLASK_ENV=development
  [...]
```
As we can see, we don't have the name, but only the path. It means that Docker Compose will create an anonymous volume without a specific name.


### Bind Mounts
Bind mounts allow you to mount a directory or file from the host machine into the container. Bind Mounts allow you to map a host file or directory to a container file or directory. This is particularly useful during development when you want changes made on the host to be immediately reflected in the container.\
In a nutshell: _we want to use the bind mounts when we need direct access to the host’s filesystem for development, testing, or sharing configuration files._
```yml
services:
  config:
  build: config-server-end
  mem_limit: 512m
  environment:
    - SPRING_PROFILES_ACTIVE=docker,native
    [...]
  volumes:
    - ./config-repo:/config-repo
```
This means that a directory from the host machine (`./config-repo`) is mounted into the container at `/config-repo`.


___
## Docker networking
Communication Between Containers\
Allows containers to communicate with each other and with the external world; with the networking we can create complex applications composed of multiple services running in different containers.


### Same network
Containers on the same Docker network can easily communicate with each other using their container names as hostnames.
```yml
services:
  app:
    build: ./app
    networks:
      - backend
  db:
    image: postgres
    networks:
      - backend
  [...]
```
As we see in this example the same network (called `backend`) is shared between `app` and `db` containers.

### Different networks
...
If we want to extend the previous example, we can add for example the proxy part, that can be in another network (because it doesn't have to communicate with the db for example).
```yml
services:
  app:
    build: ./app
    networks:
      - frontend
      - backend
  db:
    image: postgres
    networks:
      - backend
  proxy:
    build: ./proxy
    networks:
      - frontend
```
So we can see that `db` is in a different network of `proxy`, but there is with one service connected to both networks: `app` is in the middle!


#### Why have multiple networks?
Using multiple networks instead of a single network can provide benefits in terms of security, scalability and organizational clarity.\
The reasons are always the same: service separation, and so we can apply and manage different network policies and configure with different resources but also reduce the risk of network congestion and much more.

___
## Docker replicas
Docker replicas helps ensure high availability, load balancing (Docker Swarm provides it built-in) and fault tolerance of applications by having number of identical replicas (instances) of a service that should be running across your cluster.
But let's analyze it more deeply.

A replica is a running instance of a service; when you define a service in Docker Compose, you can specify the desired number of replicas (for scaling up or down) as we can see in the last line -deploy section- of the following example.
```yml
services:
  web:
    image: nginx
    ports:
      - "8080:8080"
    deploy:
      replicas: 3
```
An important feature is the **rolling updates** (for a deployment update with zero downtime).\
This is done by updating replicas incrementally to avoid downtime.
```yml
services:
  web:
    image: nginx
    ports:
      - "80:80"
    deploy:
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
```
In the `update_config` section we specify that only one replica should be updated at a time -not all instances simultaneously- which helps avoid downtime; moreover is sets a delay of 10 seconds between updating each replica - this is important because it gives the time to the new replica to start up and stabilize before the next one is updated.

___

Here we want to provide some examples that shows all this features in the Docker Compose file.
```yml
services:
  web:
    image: nginx:latest
    ports:
      - "80:80"
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
      replicas: 3
      update_config:
        parallelism: 1
        delay: 10s
    networks:
      - my-overlay-network
    volumes:
      - web-content:/usr/share/nginx/html

  db:
    image: mysql:5.7
    environment:
      MYSQL_ROOT_PASSWORD: example
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 2048M
        reservations:
          cpus: '0.25'
          memory: 1024M
    networks:
      - my-overlay-network
    volumes:
      - db-data:/var/lib/mysql
```
In this example we have two services.
The first one has `nginx` as base image (`latest` version), the second one instead has `mysql` (version `5.7`).
Both has memory and resource limitation and reservation.
The first service has 3 replicas with a single parallelism for update of 10 seconds.
Both services are in the same network, so they can communicate each other without any problems, and they both have a named volume (`web-content` and `db-data`).

In this example instead we have three services, connected to two different networks.

```yml
services:
  frontend:
    image: nginx:latest
    ports:
      - "80:80"
    deploy:
      replicas: 3
      resources:
        limits:
          cpus: '0.5'
          memory: 256M
        reservations:
          cpus: '0.25'
          memory: 128M
      update_config:
        parallelism: 1
        delay: 10s
    networks:
      - frontend-network
    volumes:
      - frontend-data:/usr/share/nginx/html

  backend:
    image: mybackend:latest
    build: ./backend
    deploy:
      replicas: 2
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
      update_config:
        parallelism: 1
        delay: 5s
    networks:
      - frontend-network
      - backend-network
    volumes:
      - backend-data:/app/data

  database:
    image: postgres:13
    environment:
      POSTGRES_DB: mydb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 1024M
        reservations:
          cpus: '0.25'
          memory: 512M
    networks:
      - backend-network
    volumes:
      - db-data:/var/lib/postgresql/data
```
The `frontend` service is connected with the `frontend-network`; the `backend` instead is connected with the `frontend-network` but also with the `backend-network`; the `database` service is in the `backend-network` obviously. The `backend` service acts as a bridge to the three services, communicating to each one.\
There are many replicas in this case too but with different update configurations (as we can see in the backend, with 5s).\
It is important to note that all volumes are named, "persistence oriented", so we can immagine that the data are crucial for the system.

