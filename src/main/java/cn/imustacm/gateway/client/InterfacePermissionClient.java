package cn.imustacm.gateway.client;

import cn.imustacm.common.consts.ServiceIdConst;
import cn.imustacm.user.service.IInterfacePermissionService;
import org.springframework.cloud.openfeign.FeignClient;


@FeignClient(ServiceIdConst.IMUSTACM_USER_SERVICE)
public interface InterfacePermissionClient extends IInterfacePermissionService {

}
