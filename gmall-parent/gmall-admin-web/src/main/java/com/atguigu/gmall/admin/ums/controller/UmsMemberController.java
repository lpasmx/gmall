package com.atguigu.gmall.admin.ums.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.to.CommonResult;
import com.atguigu.gmall.ums.entity.Member;
import com.atguigu.gmall.ums.entity.MemberLevel;
import com.atguigu.gmall.ums.service.MemberLevelService;
import io.swagger.annotations.Api;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@CrossOrigin
@Api(tags = "UmsMemberController",description = "会员服务管理")
@RestController
public class UmsMemberController {
    @Reference
    MemberLevelService memberLevelService;
    @GetMapping("/memberLevel/list")
    public Object memberLevel(@RequestParam(value = "defaultStatus",defaultValue = "0") Integer defaultStatus) {

      List<MemberLevel> memberLevels = memberLevelService.getMemberLevelByStatus(defaultStatus);
        return new CommonResult().success(memberLevels);
    }
}
