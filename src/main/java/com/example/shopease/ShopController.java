package com.example.shopease;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ShopController {
    private final JdbcTemplate db;
    public ShopController(JdbcTemplate db) { this.db = db; }

    @GetMapping("/health")
    public Map<String,Object> health() {
        return Map.of("status","ok","application","ShopEase");
    }

    @GetMapping("/products")
    public List<Map<String,Object>> products() {
        return db.queryForList("SELECT PRODUCT_ID AS id, NAME AS name, DESCRIPTION AS description, PRICE AS price, CATEGORY AS category, IMAGE_URL AS imageUrl FROM SHOPDEMO_PRODUCTS ORDER BY PRODUCT_ID");
    }

    @PostMapping("/auth/register")
    public Map<String,Object> register(@RequestBody Map<String,String> b) {
        String username = required(b,"username");
        String email = required(b,"email");
        String password = required(b,"password");
        String name = Optional.ofNullable(b.get("name")).orElse(username);
        String contact = Optional.ofNullable(b.get("contact")).orElse("");
        if (password.length() < 4) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at least 4 characters.");
        try {
            Number uid = db.queryForObject("SELECT SHOPDEMO_USER_SEQ.NEXTVAL FROM DUAL", Number.class);
            db.update("INSERT INTO SHOPDEMO_USERS (USER_ID, USERNAME, EMAIL, PASSWORD_TEXT, FULL_NAME, CONTACT) VALUES (?,?,?,?,?,?)",
                uid.intValue(), username, email, password, name, contact);
            return Map.of("success",true,"uid",uid.intValue(),"username",username,"name",name);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"That username or email is already registered.");
        }
    }

    @PostMapping("/auth/login")
    public Map<String,Object> login(@RequestBody Map<String,String> b) {
        String username = required(b,"username");
        String password = required(b,"password");
        List<Map<String,Object>> rows = db.queryForList(
            "SELECT USER_ID AS uid, USERNAME AS username, EMAIL AS email, FULL_NAME AS name FROM SHOPDEMO_USERS WHERE LOWER(USERNAME)=LOWER(?) AND PASSWORD_TEXT=?",
            username, password);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Incorrect username or password.");
        Map<String,Object> user = new LinkedHashMap<>(rows.get(0));
        user.put("success",true);
        return user;
    }

    @GetMapping("/cart/{uid}")
    public List<Map<String,Object>> cart(@PathVariable int uid) {
        return db.queryForList("SELECT c.PRODUCT_ID AS productId, p.NAME AS name, p.PRICE AS price, c.QUANTITY AS quantity, (p.PRICE*c.QUANTITY) AS subtotal FROM SHOPDEMO_CART c JOIN SHOPDEMO_PRODUCTS p ON p.PRODUCT_ID=c.PRODUCT_ID WHERE c.USER_ID=? ORDER BY p.NAME", uid);
    }

    @PostMapping("/cart")
    public Map<String,Object> addCart(@RequestBody Map<String,Object> b) {
        int uid = intRequired(b,"uid"), productId = intRequired(b,"productId");
        int quantity = b.get("quantity")==null ? 1 : Integer.parseInt(b.get("quantity").toString());
        if (quantity < 1 || quantity > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quantity must be between 1 and 99.");
        if (db.queryForObject("SELECT COUNT(*) FROM SHOPDEMO_USERS WHERE USER_ID=?",Integer.class,uid)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Please log in again.");
        if (db.queryForObject("SELECT COUNT(*) FROM SHOPDEMO_PRODUCTS WHERE PRODUCT_ID=?",Integer.class,productId)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Product not found.");
        List<Integer> existing = db.query("SELECT QUANTITY FROM SHOPDEMO_CART WHERE USER_ID=? AND PRODUCT_ID=?",
            (rs,n)->rs.getInt(1),uid,productId);
        if (existing.isEmpty()) db.update("INSERT INTO SHOPDEMO_CART (USER_ID,PRODUCT_ID,QUANTITY) VALUES (?,?,?)",uid,productId,quantity);
        else db.update("UPDATE SHOPDEMO_CART SET QUANTITY=? WHERE USER_ID=? AND PRODUCT_ID=?",Math.min(99,existing.get(0)+quantity),uid,productId);
        return Map.of("success",true,"message","Added to cart.");
    }

    @DeleteMapping("/cart/{uid}/{productId}")
    public Map<String,Object> removeCart(@PathVariable int uid,@PathVariable int productId) {
        db.update("DELETE FROM SHOPDEMO_CART WHERE USER_ID=? AND PRODUCT_ID=?",uid,productId);
        return Map.of("success",true);
    }

    @PostMapping("/orders")
    @Transactional
    public Map<String,Object> order(@RequestBody Map<String,String> b) {
        int uid;
        try { uid = Integer.parseInt(required(b,"uid")); }
        catch (NumberFormatException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid user."); }
        String address=required(b,"address"), city=required(b,"city");
        List<Map<String,Object>> cart = db.queryForList(
            "SELECT c.PRODUCT_ID AS productId,c.QUANTITY AS quantity,p.PRICE AS price FROM SHOPDEMO_CART c JOIN SHOPDEMO_PRODUCTS p ON p.PRODUCT_ID=c.PRODUCT_ID WHERE c.USER_ID=?",uid);
        if (cart.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Your cart is empty.");
        Number orderNum = db.queryForObject("SELECT SHOPDEMO_ORDER_SEQ.NEXTVAL FROM DUAL",Number.class);
        int orderId=orderNum.intValue();
        BigDecimal total=BigDecimal.ZERO;
        for (Map<String,Object> item:cart) {
            BigDecimal price = new BigDecimal(item.get("PRICE").toString());
            int qty = ((Number)item.get("QUANTITY")).intValue();
            total=total.add(price.multiply(BigDecimal.valueOf(qty)));
        }
        db.update("INSERT INTO SHOPDEMO_ORDERS (ORDER_ID,USER_ID,ORDER_DATE,TOTAL_AMOUNT,ADDRESS,CITY,STATUS) VALUES (?, ?, SYSDATE, ?, ?, ?, 'PLACED')",
            orderId,uid,total,address,city);
        for (Map<String,Object> item:cart) {
            BigDecimal price = new BigDecimal(item.get("PRICE").toString());
            int qty = ((Number)item.get("QUANTITY")).intValue();
            db.update("INSERT INTO SHOPDEMO_ORDER_ITEMS (ORDER_ITEM_ID,ORDER_ID,PRODUCT_ID,QUANTITY,UNIT_PRICE) VALUES (SHOPDEMO_ITEM_SEQ.NEXTVAL,?,?,?,?)",
                orderId,((Number)item.get("PRODUCTID")).intValue(),qty,price);
        }
        db.update("DELETE FROM SHOPDEMO_CART WHERE USER_ID=?",uid);
        return Map.of("success",true,"orderId",orderId,"total",total,"message","Order placed successfully.");
    }

    @GetMapping("/orders/by-user/{uid}")
    public List<Map<String,Object>> orderHistory(@PathVariable int uid) {
        return db.queryForList("SELECT ORDER_ID AS orderId, ORDER_DATE AS orderDate, TOTAL_AMOUNT AS total, ADDRESS AS address, CITY AS city, STATUS AS status FROM SHOPDEMO_ORDERS WHERE USER_ID=? ORDER BY ORDER_DATE DESC",uid);
    }

    private String required(Map<String,String> b,String key) {
        String v=b.get(key);
        if(v==null || v.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing field: "+key);
        return v.trim();
    }
    private int intRequired(Map<String,Object> b,String key) {
        Object v=b.get(key);
        if(v==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Missing field: "+key);
        try{return Integer.parseInt(v.toString());}
        catch(NumberFormatException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid field: "+key);}
    }
}
