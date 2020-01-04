package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gmall.cart.CartService;
import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.CartItem;
import com.atguigu.gmall.cart.bean.SkuResponse;
import com.atguigu.gmall.constant.RedisCacheConstant;
import com.atguigu.gmall.pms.entity.Product;
import com.atguigu.gmall.pms.entity.SkuStock;
import com.atguigu.gmall.pms.service.ProductService;
import com.atguigu.gmall.ums.entity.Member;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@Component
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Reference
    ProductService productService;
    @Autowired
    RedissonClient redissonClient;
    /**
     *
     * 登陆不登录都带cart-key，没登陆我们会返回给cart-key，以后就用这个
     * 登陆了就额外加上自己的访问令牌：token
     * @param skuId
     * @param num
     * @param cartKey
     * @return
     */
    @Override
    public SkuResponse addToCart(Long skuId, Integer num, String cartKey) {
        SkuResponse skuResponse = new SkuResponse();
        String token = RpcContext.getContext().getAttachment("gmallusertoken");
        System.out.println("有没有token？？"+token);
        String memberJson= redisTemplate.opsForValue().get(RedisCacheConstant.USER_INFO_CACHE_KEY + token);
        Member member = JSON.parseObject(memberJson, Member.class);
        Long memberId = member == null ? 0L : member.getId();
        String memberName = member == null ? "" : member.getNickname();
        //1,查询这个sku信息，把他添加到购物车
        SkuStock skuStock = productService.getSkuInfo(skuId);
        //2查询出这个对应的spu信息
        Product product = productService.getProductByIdFromCache(skuStock.getProductId());
        //3查询优惠卷系统

        //4,封装成一个cartItem
        CartItem item = new CartItem(product.getId(),
                        skuStock.getId(),
                        memberId,
                        num,
                        skuStock.getPrice(),
                        skuStock.getPrice(),
                        num,
                        skuStock.getSp1(),skuStock.getSp2(),skuStock.getSp3(),
                        product.getPic(),
                        product.getName(),
                        memberName,
                        product.getProductCategoryId(),
                        product.getBrandName(),
                         false,"满111减11"
                         );
        //前端把key 都带过来    gmall:cart:tmep:uuid
        if (StringUtils.isEmpty(memberJson)) {
            //这个令牌没数据，没登陆：离线购物车流程
            if(!StringUtils.isEmpty(cartKey)){
                skuResponse.setCartKey(cartKey);
                //用户有老购物车
                cartKey = RedisCacheConstant.CART_TEMP+cartKey;
                
                addItemToCart(item,num,cartKey);
            }else {
                //新建一个购物车，以后用这个
                String replace = UUID.randomUUID().toString().replace("-", "");
                String newCartKey = RedisCacheConstant.CART_TEMP + replace;
                skuResponse.setCartKey(replace);
                addItemToCart(item,num,newCartKey);
            }
        }else{
            //在线购物车流程
            String loginCartKey = RedisCacheConstant.USER_CART + member.getId();
            //合并购物车
            mergeCart(RedisCacheConstant.CART_TEMP+cartKey,loginCartKey);
            //5、放入购物车
            addItemToCart(item,num,loginCartKey);
        }
        skuResponse.setItem(item);
        return skuResponse;
    }

    private void mergeCart(String oldCartKey, String newCartKey) {
        //1,获取新购物车
        RMap<String, String> map = redissonClient.getMap(oldCartKey);
        if (map != null&&map.entrySet()!=null) {
            map.entrySet().forEach((entry)->{
                String value = entry.getValue();
                CartItem item = JSON.parseObject(value, CartItem.class);
                //将老购物车的数据转移过去
                addItemToCart(item,item.getNum(),newCartKey);
                map.remove(item.getProductSkuId()+"");
            });
        }

    }
    //给购物车添加一项
    //1）、第一次用购物车的时候都必须合并
    //1、查看购物车数据
    //2、加入购物车需要合并
    private void addItemToCart(CartItem item, Integer num, String cartKey) {
        //1，拿到购物车
        RMap<String, String> map = redissonClient.getMap(cartKey);
        //2,先看看这个有没有
        boolean b = map.containsKey(item.getProductSkuId() + "");
        if(b){
            //3,购物车已经有此项
            String json = map.get(item.getProductSkuId()+"");
            CartItem cartItem = JSON.parseObject(json, CartItem.class);
            cartItem.setNum(cartItem.getNum()+num);
            String string = JSON.toJSONString(cartItem);
            map.put(item.getProductSkuId()+"",string);
        }else {
            String string = JSON.toJSONString(item);
            map.put(item.getProductSkuId()+"",string);
        }
    }

    @Override
    public boolean updateCount(Long skuId, Integer num, String cartKey) {
        String token = RpcContext.getContext().getAttachment("gmallusertoken");
        String memberJson = redisTemplate.opsForValue().get(RedisCacheConstant.USER_INFO_CACHE_KEY + token);
        Member member = JSON.parseObject(memberJson, Member.class);
        RMap<String,String> map = null;
        if (member == null) {
            //用户未登录
            map = redissonClient.getMap(RedisCacheConstant.CART_TEMP+cartKey);
        }else {
            //用户登录
            map = redissonClient.getMap(RedisCacheConstant.USER_CART+member.getId());
        }
        String s = map.get(skuId + "");
        CartItem item = JSON.parseObject(s, CartItem.class);
        item.setNum(num);
        String json = JSON.toJSONString(item);
        map.put(skuId+"",json);
        return true;
    }

    @Override
    public boolean deleteCart(Long skuId, String cartKey) {
        String token = RpcContext.getContext().getAttachment("gmallusertoken");
        String memberJson = redisTemplate.opsForValue().get(RedisCacheConstant.USER_INFO_CACHE_KEY + token);
        Member member = JSON.parseObject(memberJson, Member.class);
        RMap<String,String> map = null;
        if (member == null) {
            //用户未登录
            map = redissonClient.getMap(RedisCacheConstant.CART_TEMP+cartKey);
        }else {
            //用户登录
            map = redissonClient.getMap(RedisCacheConstant.USER_CART+member.getId());
        }
        map.remove(skuId+"");
        return true;
    }

    @Override
    public boolean checkCart(Long skuId, Integer flag, String cartKey) {
        String token = RpcContext.getContext().getAttachment("gmallusertoken");
        String memberJson = redisTemplate.opsForValue().get(RedisCacheConstant.USER_INFO_CACHE_KEY + token);
        Member member = JSON.parseObject(memberJson, Member.class);
        RMap<String,String> map = null;
        if (member == null) {
            //用户未登录
            map = redissonClient.getMap(RedisCacheConstant.CART_TEMP+cartKey);
        }else {
            //用户登录
            map = redissonClient.getMap(RedisCacheConstant.USER_CART+member.getId());
        }
        String s = map.get(skuId + "");
        CartItem item = JSON.parseObject(s, CartItem.class);
        item.setChecked(flag==0?false:true);
        String json = JSON.toJSONString(item);
        map.put(skuId+"",json);

        //维护checked 字段的set
        String checked = map.get("checked");
        Set<String> checkedSkuIds = new HashSet<>();
        //复杂的泛型数据转换
        if(!StringUtils.isEmpty(checked)){
            //有
            Set<String> strings = JSON.parseObject(checked,new TypeReference<Set<String>>(){
            });
            if (flag == 0) {
                //不勾中
                strings.remove(skuId+"");
            }else {
                strings.add(skuId+"");
            }
            String s1 = JSON.toJSONString(strings);
            map.put("checked",s1);
        }else {
            //没有
            checkedSkuIds.add(skuId+"");
            String s1 = JSON.toJSONString(checkedSkuIds);
            map.put("checked",s1);
        }
        return true;
    }

    @Override
    public Cart cartItemsList(String cartKey) {
        String token = RpcContext.getContext().getAttachment("gmallusertoken");
        String memberJson = redisTemplate.opsForValue().get(RedisCacheConstant.USER_INFO_CACHE_KEY + token);
        Member member = JSON.parseObject(memberJson, Member.class);
        RMap<String,String> map = null;
        if (member == null) {
            //用户未登录
            map = redissonClient.getMap(RedisCacheConstant.CART_TEMP+cartKey);
        }else {
            //用户登录
            //尝试合并购物车
            mergeCart(RedisCacheConstant.CART_TEMP+cartKey,RedisCacheConstant.USER_CART+member.getId());
            //合并完成后在操作
            map = redissonClient.getMap(RedisCacheConstant.USER_CART+member.getId());
        }
        if (map != null) {
            Cart cart = new Cart();
            cart.setItems(new ArrayList<CartItem>());
            map.entrySet().forEach((o)->{
                String json = o.getValue();
                CartItem item = JSON.parseObject(json, CartItem.class);
                cart.getItems().add(item);
            });

            return cart;
        }else {
            return null;
        }

    }
}
