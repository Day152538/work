package com.xuyan.fm.service.impl;

import com.xuyan.fm.dao.CarouselDao;
import com.xuyan.fm.dao.IdleItemDao;
import com.xuyan.fm.model.CarouselModel;
import com.xuyan.fm.model.IdleItemModel;
import com.xuyan.fm.service.CarouselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class CarouselServiceImpl implements CarouselService {

    @Autowired
    private CarouselDao carouselDao;
    @Autowired
    private IdleItemDao idleItemDao;
    @Override
//    public void addCarousel(CarouselModel carousel) {
//        carouselDao.save(carousel);
//    }
    public void addCarousel(CarouselModel carousel) {
        IdleItemModel good = idleItemDao.selectByPrimaryKey(carousel.getGoodId());
        if (good != null) {
            // 插入到 carousel 表
            carouselDao.save(carousel);
        } else {
            // good_id 不存在，可以抛出异常或进行其他处理
            throw new IllegalArgumentException("Invalid good_id");
        }
    }
    public void deleteCarousel(Long id) {
        carouselDao.deleteById(id);
    }

    public List<CarouselModel> getAllCarousels() {
        return carouselDao.findAll();
    }

    @Override
    public void updateCarousel(CarouselModel carousel) {
        carouselDao.updateCarousel(carousel);
    }
}
