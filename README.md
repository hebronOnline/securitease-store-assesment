# Store Application
The Store application keeps track of customers and orders in a database.

# Assumptions
This README assumes you're using a posix environment. It's possible to run this on Windows as well:
* Instead of `./gradlew` use `gradlew.bat`
* The syntax for creating the Docker container is different. You could also install PostgreSQL on bare metal if you prefer


# Prerequisites
This service assumes the presence of a postgresql 16.2 database server running on localhost:5433 (note the non-standard port)
It assumes a username and password `admin:admin` can be used.
It assumes there's already a database called `store`

You can start the PostgreSQL instance like this:
```shell
docker run -d \
  --name postgres \
  --restart always \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=store \
  -v postgres:/var/lib/postgresql/data \
  -p 5433:5432 \
  postgres:16.2 \
  postgres -c wal_level=logical
```

This service also requires a Redis server running on localhost:6379. Redis backs the
application cache (see the Caching section under Performance below); the GET endpoints will
fail to serve if it is unavailable.

You can start the Redis instance like this:
```shell
docker run -d \
  --name redis \
  --restart always \
  -p 6379:6379 \
  redis:7
```

# Running the application
You should be able to run the service using
```shell
./gradlew bootRun
```

The application uses Liquibase to migrate the schema. Some sample data is provided. You can create more data by reading the documentation in utils/README.md

# Data model
An order has an ID, a description, and is associated with the customer which made the order.
A customer has an ID, a name, and 0 or more orders.

# API
The service exposes three resources:

### `/order`
   * `GET /order` — list all orders (each with its customer and the products it contains)
   * `GET /order/{id}` — fetch a single order by ID (404 if not found)
   * `POST /order` — create an order (requires `description`, `customerId`, and a non-empty `productIds`)

### `/customer`
   * `GET /customer` — list all customers (each with its orders)
   * `GET /customer/search?name={substring}` — find customers whose name contains the given substring (case-insensitive)
   * `POST /customer` — create a customer (requires `name`)

### `/products`
   * `GET /products` — list all products, each with the IDs of the orders that contain it
   * `GET /products/{id}` — fetch a single product by ID, with its order IDs (404 if not found)
   * `POST /products` — create a product (requires `description`)

Invalid request bodies return `400` with per-field validation messages, and unknown IDs
return `404` — both served as JSON by a global exception handler.

The data model is circular - a customer owns a number of orders, and that order necessarily refers back to the customer which owns it.
To avoid loops in the serializer, when writing out a Customer or an Order, they're mapped to CustomerDTO and OrderDTO which contain truncated versions of the dependent object - CustomerOrderDTO and OrderCustomerDTO respectively. Likewise, an order lists its products as truncated OrderProductDTOs, and a product lists only the IDs of the orders that contain it.

The API is documented in the OpenAPI file OpenAPI.yaml.

# Caching

The GET endpoints are backed by a Redis-backed cache (see Prerequisites) to reduce load and
latency on repeat reads. Reads populate the cache; writes (creating an order, customer, or
product) evict the affected entries so stale data isn't served. 

# Tasks

1. Extend the order endpoint to find a specific order, by ID
2. Extend the customer endpoint to find customers based on a query string to match a substring of one of the words in their name
3. Users have complained that in production the GET endpoints can get very slow. The database is unfortunately not co-located with the application server, and there's high latency between the two. Identify if there are any optimisations that can improve performance
4. Add a new endpoint /products to model products which appear in an order:
      * A single order contains 1 or more products. 
      * A product has an ID and a description. 
      * Add a POST endpoint to create a product
      * Add a GET endpoint to return all products, and a specific product by ID
        * In both cases, also return a list of the order IDs which contain those products
      * Change the orders endpoint to return a list of products contained in the order

# Bonus points
1. Implement a CI pipeline on the platform of your choice to build the project and deliver it as a Dockerized image

# Notes on the tasks
Assume that the project represents a production application.
Think carefully about the impact on performance when implementing your changes
The specifications of the tasks have been left deliberately vague. You will be required to exercise judgement about what to deliver - in a real world environment, you would clarify these points in refinement, but since this is a project to be completed without interaction, feel free to make assumptions - but be prepared to defend them when asked.
There's no CI pipeline associated with this project, but in reality there would be. Consider the things that you would expect that pipeline to verify before allowing your code to be promoted
Feel free to refactor the codebase if necessary. Bad choices were deliberately made when creating this project.
