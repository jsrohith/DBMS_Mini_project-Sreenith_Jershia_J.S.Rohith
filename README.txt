SHOPEASE SIMPLE COMPLETE PROJECT
================================

WHAT IS INCLUDED
- Responsive e-commerce website
- Product catalogue
- Register and login
- Add/remove cart items
- Checkout and order history
- Spring Boot REST API
- Oracle XE 11g SQL setup
- Example PL/SQL function and procedure

IMPORTANT
This is a clean standalone demo and uses SHOPDEMO_ table names. It does not alter or depend on the existing LOGIN/PRODUCT/CART/ORDERS tables. It is intended for a local college demo, not production. Passwords are stored as plain text in this demo; do not use real passwords.

REQUIREMENTS
- JDK 17
- Maven
- Oracle XE 11g running
- Schema login ECOMMERCE / Ecommerce123 at localhost:1521/XE

SETUP
1. Extract this ZIP into a NEW folder, for example:
   C:\Users\HP\Downloads\ShopEase_Simple_Complete

2. Open SQL*Plus:
   sqlplus ECOMMERCE/Ecommerce123@localhost:1521/XE

3. At the SQL> prompt, run the setup file using its full path, for example:
   @ "C:\Users\HP\Downloads\ShopEase_Simple_Complete\sql\setup.sql"
   If SQL*Plus rejects the space after @, use:
   @"C:\Users\HP\Downloads\ShopEase_Simple_Complete\sql\setup.sql"

   Run setup.sql only once. It creates SHOPDEMO_ tables and sequences.

4. Open the extracted project folder in VS Code.

5. Open Terminal > New Terminal and run:
   mvn clean spring-boot:run

6. Wait until Spring Boot says it started. Open:
   http://localhost:8090

7. Create an account from the website, sign in, add products to cart, and place a demo order.

TROUBLESHOOTING
- If port 8090 is already occupied, stop the other Spring Boot application with Ctrl+C first.
- If Oracle login fails, verify the username, password, and service name in src/main/resources/application.properties.
- If you already ran setup.sql, do not run it a second time; Oracle will report that the tables already exist.
- If Maven fails, check that JDK 17 and Maven are installed using:
  java -version
  mvn -version

DEMO FEATURES / ENDPOINTS
GET  /api/health
GET  /api/products
POST /api/auth/register
POST /api/auth/login
POST /api/cart
GET  /api/cart/{uid}
DELETE /api/cart/{uid}/{productId}
POST /api/orders
GET  /api/orders/by-user/{uid}
