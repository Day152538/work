package com.xuyan.fm.dao;

import com.xuyan.fm.model.CarouselModel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CarouselDao {
    @Update("UPDATE sh_carousel SET show_order = #{showOrder} WHERE id = #{id}")
    void updateCarousel(CarouselModel carousel);

    @Insert("INSERT INTO sh_carousel (good_id, show_order) VALUES (#{goodId}, #{showOrder})")
    void save(CarouselModel carousel);

    @Delete("DELETE FROM sh_carousel WHERE id = #{id}")
    void deleteById(Long id);

    List<CarouselModel> findAll();
}