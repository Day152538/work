package com.xuyan.fm.service;

import com.xuyan.fm.model.CarouselModel;

import java.util.List;

public interface CarouselService {
    void addCarousel(CarouselModel carousel);

    void deleteCarousel(Long id);
//    PageVo<CarouselModel> getAllCarousels(int page , int nums);

    List<CarouselModel> getAllCarousels();

    void updateCarousel(CarouselModel carousel);
}