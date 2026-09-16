package com.xuyan.fm.controller;

import com.xuyan.fm.common.interceptor.RequireAdmin;
import com.xuyan.fm.model.CarouselModel;
import com.xuyan.fm.service.CarouselService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 首页轮播图接口。
 * 整改点：原增删改无鉴权，任何游客可篡改首页轮播。现写操作要求管理员，查看保持公开。
 */
@RestController
@RequestMapping("/carousel")
public class CarouselController {

    private final CarouselService carouselService;

    public CarouselController(CarouselService carouselService) {
        this.carouselService = carouselService;
    }

    @RequireAdmin
    @PostMapping("/add")
    public ResultVo<String> addCarousel(@RequestBody CarouselModel carousel) {
        carouselService.addCarousel(carousel);
        return ResultVo.success();
    }

    @RequireAdmin
    @DeleteMapping("/delete/{id}")
    public ResultVo<String> deleteCarousel(@PathVariable Long id) {
        carouselService.deleteCarousel(id);
        return ResultVo.success();
    }

    @RequireAdmin
    @PostMapping("/update")
    public ResultVo updateCarousel(@RequestBody CarouselModel carousel) {
        carouselService.updateCarousel(carousel);
        return ResultVo.success();
    }

    /**
     * 轮播列表（公开，首页展示用）
     */
    @GetMapping("/all")
    public ResultVo<List<CarouselModel>> getAllCarousel() {
        return ResultVo.success(carouselService.getAllCarousels());
    }
}
