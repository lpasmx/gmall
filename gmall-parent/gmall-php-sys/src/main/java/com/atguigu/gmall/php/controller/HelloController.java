package com.atguigu.gmall.php.controller;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
@Controller
public class HelloController {


    @GetMapping("/shuai")
    public String shuaige(HttpSession session,
                            @RequestParam(value = "token",required = false) String token){
        if (session.getAttribute("loginUser") == null) {
            //没登陆，要登陆，还要看我们系统是否有这个令牌
            if(!StringUtils.isEmpty(token)){
                //认证中心认为已经登陆，带着令牌跳回来
                //1）、以这个令牌，去CAS查出我们这个用户的真正信息并保存在session中；JWT
                session.setAttribute("loginUser",token);
                return "protected";
            }
        }else {
            return "protected";
        }



//        Object loginUser = session.getAttribute("loginUser");
//        if(loginUser!=null){
//            return "protected";
//        }else{
//            return "redirect:http://www.gmallshop.com/login.html";
//        }
        return "redirect:http://www.gmallshop.com/login.html?url=http://www.php-sys.com/shuai";

    }
}
