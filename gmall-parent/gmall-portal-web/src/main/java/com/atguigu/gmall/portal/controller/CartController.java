package com.atguigu.gmall.portal.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.rpc.RpcContext;
import com.atguigu.gmall.cart.CartService;
import com.atguigu.gmall.cart.bean.Cart;
import com.atguigu.gmall.cart.bean.SkuResponse;
import com.atguigu.gmall.to.CommonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

@Api(tags = "购物车")
@RequestMapping("/cart")
@RestController
public class CartController {
    @Reference
    CartService cartService;

    @ApiOperation(value = "添加商品到购物车")
    @PostMapping("/add")
    public CommonResult addToCart(
            @ApiParam(value = "需要添加的商品的skuId")
            @RequestParam("skuId") Long skuId,
            @ApiParam(value = "需要添加的商品的数量")
            @RequestParam("num") Integer num,
            @ApiParam(value = "用户登录后传递自己的token，没有可以不传递")
            @RequestParam("token") String token,
            @ApiParam(value = "传递之前后台返回的购物车的标识，没有可以不传递")
            @RequestParam("cartKey") String cartKey){
        //注意rpc上下文要隐式传参，参数的名字一定不能用如下关键字
        //token ，timeout，async
        RpcContext.getContext().setAttachment("gmallusertoken",token);

        //给用户都返回一个购物车标识
        SkuResponse skuResponse = cartService.addToCart(skuId, num, cartKey);
        return new CommonResult().success(skuResponse);
    }
    @ApiOperation(value = "修改商品数量")
    @PostMapping("/update")
    public CommonResult updateCart(
            @ApiParam(value = "需要添加的商品的skuId")
            @RequestParam("skuId") Long skuId,
            @ApiParam(value = "需要添加的商品的数量")
            @RequestParam("num") Integer num,
            @ApiParam(value = "用户登录后传递自己的token，没有可以不传递")
            @RequestParam("token") String token,
            @ApiParam(value = "传递之前后台返回的购物车的标识，没有可以不传递")
            @RequestParam("cartKey") String cartKey){
        //注意rpc上下文要隐式传参，参数的名字一定不能用如下关键字
        //token ，timeout，async
        RpcContext.getContext().setAttachment("gmallusertoken",token);

        //给用户都返回一个购物车标识
       boolean update = cartService.updateCount(skuId, num, cartKey);
        return new CommonResult().success(update);
    }
    @ApiOperation(value = "删除商品")
    @PostMapping("/delete")
    public CommonResult removeToCart(
            @ApiParam(value = "需要添加的商品的skuId")
            @RequestParam("skuId") Long skuId,
            @ApiParam(value = "用户登录后传递自己的token，没有可以不传递")
            @RequestParam("token") String token,
            @ApiParam(value = "传递之前后台返回的购物车的标识，没有可以不传递")
            @RequestParam("cartKey") String cartKey){
        //注意rpc上下文要隐式传参，参数的名字一定不能用如下关键字
        //token ，timeout，async
        RpcContext.getContext().setAttachment("gmallusertoken",token);

        //给用户都返回一个购物车标识
        boolean delete = cartService.deleteCart(skuId,cartKey);
        return new CommonResult().success(delete);
    }

    @PostMapping("/check")
    public CommonResult checkToCart(
            @ApiParam(value = "需要添加的商品的skuId")
            @RequestParam("skuId") Long skuId,
            @ApiParam(value = "需要选中的商品，0不选中，1选中")
            @RequestParam("flag") Integer flag,
            @ApiParam(value = "用户登录后传递自己的token，没有可以不传递")
            @RequestParam("token") String token,
            @ApiParam(value = "传递之前后台返回的购物车的标识，没有可以不传递")
            @RequestParam("cartKey") String cartKey){
        //注意rpc上下文要隐式传参，参数的名字一定不能用如下关键字
        //token ，timeout，async
        RpcContext.getContext().setAttachment("gmallusertoken",token);

        //给用户都返回一个购物车标识
        boolean check = cartService.checkCart(skuId, flag, cartKey);
        return new CommonResult().success(check);
    }
   @GetMapping("/list")
    public CommonResult list(
            @ApiParam(value = "用户登录后传递自己的token，没有可以不传递")
            @RequestParam("token") String token,
            @ApiParam(value = "传递之前后台返回的购物车的标识，没有可以不传递")
            @RequestParam("cartKey") String cartKey){

        RpcContext.getContext().setAttachment("gmallusertoken",token);

        //给用户都返回一个购物车标识
        Cart cart = cartService.cartItemsList(cartKey);
        return new CommonResult().success(cart);
    }

}
