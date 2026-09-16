package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.model.FavoriteModel;
import com.xuyan.fm.service.FavoriteService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 收藏接口。
 * 整改点：身份改用 JWT 上下文；删除收藏带属主校验
 * （原接口任何登录用户可删任意收藏记录）。
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/add")
    public ResultVo addFavorite(@RequestBody FavoriteModel favoriteModel) {
        favoriteModel.setUserId(UserContext.getUserId());
        favoriteModel.setCreateTime(new Date());
        if (favoriteService.addFavorite(favoriteModel)) {
            return ResultVo.success(favoriteModel.getId());
        }
        return ResultVo.fail(ErrorMsg.FAVORITE_EXIT);
    }

    @GetMapping("/delete")
    public ResultVo deleteFavorite(@RequestParam Long id) {
        if (favoriteService.deleteFavorite(UserContext.getUserId(), id)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @GetMapping("/check")
    public ResultVo checkFavorite(@RequestParam Long idleId) {
        return ResultVo.success(favoriteService.isFavorite(UserContext.getUserId(), idleId));
    }

    @GetMapping("/my")
    public ResultVo getMyFavorite() {
        return ResultVo.success(favoriteService.getAllFavorite(UserContext.getUserId()));
    }
}
