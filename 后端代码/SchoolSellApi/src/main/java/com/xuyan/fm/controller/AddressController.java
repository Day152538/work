package com.xuyan.fm.controller;

import com.xuyan.fm.common.context.UserContext;
import com.xuyan.fm.common.enums.ErrorMsg;
import com.xuyan.fm.model.AddressModel;
import com.xuyan.fm.service.AddressService;
import com.xuyan.fm.vo.ResultVo;
import org.springframework.web.bind.annotation.*;

/**
 * 收货地址控制器。
 * 整改点：
 * 1. 鉴权方式由「明文 Cookie shUserId」改为 JWT + ThreadLocal（UserContext），
 *    用户无法再通过伪造 Cookie 冒充他人（原实现 shUserId 就是用户 ID，直接可伪造）。
 * 2. 所有写操作强制以登录态覆盖 userId，防止水平越权修改/删除他人地址。
 */
@RestController
@RequestMapping("/address")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/info")
    public ResultVo getAddress(@RequestParam(value = "id", required = false) Long id) {
        Long userId = UserContext.getUserId();
        if (null == id) {
            return ResultVo.success(addressService.getAddressByUser(userId));
        }
        return ResultVo.success(addressService.getAddressById(id, userId));
    }

    @PostMapping("/add")
    public ResultVo addAddress(@RequestBody AddressModel addressModel) {
        // 以服务端登录态为准，忽略客户端传入的 userId
        addressModel.setUserId(UserContext.getUserId());
        if (addressService.addAddress(addressModel)) {
            return ResultVo.success(addressModel);
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @PostMapping("/update")
    public ResultVo updateAddress(@RequestBody AddressModel addressModel) {
        addressModel.setUserId(UserContext.getUserId());
        if (addressService.updateAddress(addressModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }

    @PostMapping("/delete")
    public ResultVo deleteAddress(@RequestBody AddressModel addressModel) {
        addressModel.setUserId(UserContext.getUserId());
        if (addressService.deleteAddress(addressModel)) {
            return ResultVo.success();
        }
        return ResultVo.fail(ErrorMsg.SYSTEM_ERROR);
    }
}
