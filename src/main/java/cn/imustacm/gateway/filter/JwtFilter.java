package cn.imustacm.gateway.filter;

import cn.imustacm.common.consts.GlobalConst;
import cn.imustacm.gateway.client.InterfacePermissionClient;
import cn.imustacm.gateway.domain.Path;
import cn.imustacm.common.domain.Resp;
import cn.imustacm.common.enums.ErrorCodeEnum;
import cn.imustacm.common.utils.JwtUtils;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import java.util.Objects;
import java.util.Set;

import static org.springframework.http.HttpStatus.OK;

@Slf4j
@Component
public class JwtFilter extends ZuulFilter {


    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${jwt.expire-time}")
    private String jwtExpireTime;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private Path path;

    @Autowired
    private InterfacePermissionClient interfacePermissionClient;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(jwtSecretKey, Long.parseLong(jwtExpireTime));
    }


    /**
     * 过滤器类型
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器执行顺序
     * 数字越小 优先执行
     *
     * @return
     */
    @Override
    public int filterOrder() {
        return 0;
    }

    /**
     * 是否经过过滤器逻辑
     *
     * @return
     */
    @Override
    public boolean shouldFilter() {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        String servletPath = request.getServletPath();
        for(String p : path.getExclusionPath()) {
            if(p.equals(servletPath))
                return false;
        }
        return true;
    }

    /**
     * 过滤器逻辑
     *
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        String servletPath = request.getServletPath();

        // 1 获取token
        String token = request.getHeader(GlobalConst.JWT_HEADER);
        if (StringUtils.isEmpty(token)) {
            tokenNullHandler();
            return null;
        }
        String userId = null;
        try {
            log.info("[jwtFilter] servletPath:{} token:{}", servletPath, token);
            // 2 校验token是否过期
            boolean expiredStatus = jwtUtils.tokenExpiredStatus(token);
            if (!expiredStatus) {
                tokenExpiredHandler();
                return null;
            }
            userId = jwtUtils.getUserId(token);
            // 3 权限校验
            boolean permissionFlag = checkPermission(servletPath, token);
            if(!permissionFlag){
                noPermissionHandler();
            }
        } catch (Exception e) {
            tokenIllegal();
        }
        if (Objects.isNull(userId)) {
            tokenIllegal();
        }
        currentContext.addZuulRequestHeader(GlobalConst.USER_ID_HEADER, userId);
        return null;
    }

    /**
     * 接口权限校验
     *
     * @param servletPath
     * @return
     */
    private boolean checkPermission(String servletPath, String token) {
        // 1 获取能访问当前接口的权限set
        Set permissionSet = interfacePermissionClient.getInterfacePermissionSet(servletPath);
        log.info("checkPermission interface permissionSet:{}",permissionSet);
        // 2 获取用户权限列表
        Claims claimFromToken = jwtUtils.getClaimFromToken(token);
        String permissionNameListStr = (String) claimFromToken.get(GlobalConst.PERMISSION_NAME_LIST);
        String[] userPermissionNameArray = permissionNameListStr.split(",");
        log.info("checkPermission user permissionList:{}",userPermissionNameArray);
        for (String userPermission : userPermissionNameArray) {
            if (permissionSet.contains(userPermission)) {
                return true;
            }
        }
        return false;
    }


    /**
     * token失效
     */
    private void tokenExpiredHandler(){
        exceptionHandler(new Resp(ErrorCodeEnum.FORBIDDEN), OK);
    }

    /**
     * token失效
     */
    private void noPermissionHandler(){
        exceptionHandler(new Resp(ErrorCodeEnum.FORBIDDEN), OK);
    }

    /**
     * token为null
     */
    private void tokenNullHandler() {
        exceptionHandler(new Resp(ErrorCodeEnum.FORBIDDEN), OK);
    }

    private void tokenIllegal(){
        exceptionHandler(new Resp(ErrorCodeEnum.FORBIDDEN),OK);
    }

    /**
     * 异常返回
     *
     * @param resp
     * @param status
     */
    private void exceptionHandler(Resp resp, HttpStatus status) {
        RequestContext currentContext = RequestContext.getCurrentContext();
        currentContext.setSendZuulResponse(false);
        currentContext.getResponse().setContentType("text/html; charset=UTF-8");
        currentContext.setResponseBody(JSON.toJSONString(resp));
        if (status != null) {
            currentContext.setResponseStatusCode(status.value());
        }
    }

}
