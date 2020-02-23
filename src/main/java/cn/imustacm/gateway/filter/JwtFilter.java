package cn.imustacm.gateway.filter;

import cn.imustacm.common.consts.GlobalConst;
import cn.imustacm.common.domain.Resp;
import cn.imustacm.common.enums.ErrorCodeEnum;
import cn.imustacm.common.utils.JwtUtils;
import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

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

        // 获取token 并去掉token
        String token = request.getHeader(GlobalConst.JWT_HEADER);
        if (StringUtils.isEmpty(token)) {
            tokenNullHandler();
            return null;
        }
        token = token.substring(GlobalConst.JWT_PREFIX.length());
        log.info("[jwtFilter] servletPath:{} token:{}", servletPath, token);

        boolean expiredStatus = jwtUtils.tokenExpiredStatus(token);
        if (!expiredStatus) {
            tokenExpiredHandler();
            return null;
        }

        String userId = jwtUtils.getUserId(token);

        currentContext.addZuulRequestHeader(GlobalConst.USER_ID_HEADER, userId);
        return null;
    }

    private void tokenExpiredHandler(){
        exceptionHandler(new Resp(ErrorCodeEnum.USER_TOKEN_EXPIRED), OK);
    }

    /**
     * token为null
     */
    private void tokenNullHandler() {
        exceptionHandler(new Resp(ErrorCodeEnum.USER_TOKEN_NULL), OK);
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
